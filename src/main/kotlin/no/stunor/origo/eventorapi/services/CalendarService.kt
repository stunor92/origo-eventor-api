package no.stunor.origo.eventorapi.services

import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.calendar.CalendarRace
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.services.converter.CalendarConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import jakarta.annotation.PreDestroy

@Service
class CalendarService(
    var personRepository: PersonRepository,
    var eventorRepository: EventorRepository,
    var organisationRepository: OrganisationRepository,
    var regionRepository: RegionRepository,
    var eventorService: EventorService
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // SynchronousQueue + high max-pool prevents the nested-parallelism deadlock that a bounded
    // fixed pool causes (outer person tasks hold threads while waiting for inner event-class tasks
    // that are stuck in the same pool's queue).  The cap of 500 threads guards against runaway
    // resource use; each thread is I/O-bound and lives at most 6 s (Eventor HTTP timeout).
    private val executor = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 4,
        500,
        60L, TimeUnit.SECONDS,
        SynchronousQueue()
    )

    // Timeout for waiting on a batch of parallel Eventor API calls (HTTP timeout is 6s, so 30s is a safe upper bound)
    private val batchTimeoutSeconds = 30L

    @PreDestroy
    fun shutdownExecutor() {
        executor.shutdown()
        try {
            // Allow up to 60 seconds for graceful shutdown of ongoing tasks
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate within 60 seconds, forcing shutdown")
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            log.warn("Shutdown interrupted, forcing shutdown")
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private var calendarConverter = CalendarConverter(
        organisationRepository = organisationRepository,
        regionRepository = regionRepository
    )

    @Value($$"${config.personalEntries.start}")
    private val personalEntriesStart = 0L

    @Value($$"${config.personalEntries.end}")
    private val personalEntriesEnd = 0L

    @Value($$"${config.personalStarts.start}")
    private val personalStartsStart = 0L

    @Value($$"${config.personalStarts.end}")
    private val personalStartsEnd = 0L

    @Value($$"${config.personalResults.start}")
    private val personalResultsStart = 0L

    @Value($$"${config.personalResults.end}")
    private val personalResultsEnd = 0L

    fun getEventList(userId: UUID): List<CalendarRace> {
        val persons = personRepository.findAllByUsers(userId)

        val futures = persons.mapNotNull { person ->
            val eventor = eventorRepository.findById(person.eventorId) ?: return@mapNotNull null
            CompletableFuture
                .supplyAsync({ processPersonEntries(person, eventor) }, executor)
                .exceptionally { ex ->
                    log.warn("Failed to fetch calendar entries for person {} on eventor {}: {}", person.eventorRef, person.eventorId, ex.message)
                    emptyList()
                }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)

        val raceList = mutableListOf<CalendarRace>()
        futures.forEach { mergeRaces(raceList, it.join()) }
        return raceList
    }

    private fun processPersonEntries(person: Person, eventor: Eventor): List<CalendarRace> {
        val organisationIds = person.memberships.mapNotNull { it.organisation?.eventorRef }

        val entriesFuture = CompletableFuture.supplyAsync({
            eventorService.getGetOrganisationEntries(
                eventor = eventor,
                organisations = organisationIds,
                eventId = null,
                fromDate = LocalDate.now().minusDays(personalEntriesStart),
                toDate = LocalDate.now().plusDays(personalEntriesEnd)
            )
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch organisation entries for person {} on eventor {}: {}", person.eventorRef, person.eventorId, ex.message)
            org.iof.eventor.EntryList()
        }

        val startsFuture = CompletableFuture.supplyAsync({
            eventorService.getGetPersonalStarts(
                eventor = eventor,
                personId = person.eventorRef,
                eventId = null,
                fromDate = LocalDate.now().minusDays(personalStartsStart),
                toDate = LocalDate.now().plusDays(personalStartsEnd)
            )
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch personal starts for person {} on eventor {}: {}", person.eventorRef, person.eventorId, ex.message)
            null
        }

        val resultsFuture = CompletableFuture.supplyAsync({
            eventorService.getGetPersonalResults(
                eventor = eventor,
                personId = person.eventorRef,
                eventId = null,
                fromDate = LocalDate.now().minusDays(personalResultsStart),
                toDate = LocalDate.now().plusDays(personalResultsEnd)
            )
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch personal results for person {} on eventor {}: {}", person.eventorRef, person.eventorId, ex.message)
            null
        }

        // Wait for entries first — it drives the event-class map.
        // buildEventClassMap fires its own parallel API calls and waits for them, so starting
        // it as soon as entries arrive lets class-map fetching overlap with the still-running
        // starts/results futures.
        val entryList = entriesFuture.get(batchTimeoutSeconds, TimeUnit.SECONDS)
        val eventClassMap = buildEventClassMap(entryList, eventor)

        // Collect starts and results (likely already done or nearly done by now)
        CompletableFuture.allOf(startsFuture, resultsFuture).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        val startListList = startsFuture.join()
        val resultListList = resultsFuture.join()

        return eventClassMap.generateCalendarRaceForPerson(
            eventor,
            person,
            entryList,
            startListList,
            resultListList
        )
    }

    private fun buildEventClassMap(
        entryList: org.iof.eventor.EntryList,
        eventor: Eventor
    ): MutableMap<String, org.iof.eventor.EventClassList> {
        // Map raceId -> eventId, deduplicating by eventId to avoid redundant API calls
        val raceToEventId = mutableMapOf<String, String>()
        for (entry in entryList.entry) {
            val eventId = entry.event.eventId.content
            for (raceId in entry.eventRaceId) {
                raceToEventId[raceId.content] = eventId
            }
        }

        if (raceToEventId.isEmpty()) return mutableMapOf()

        // Fetch all unique event class lists in parallel, skipping any that fail
        val uniqueEventIds = raceToEventId.values.distinct()
        val classFutures: List<Pair<String, CompletableFuture<org.iof.eventor.EventClassList?>>> = uniqueEventIds.map { eventId ->
            eventId to CompletableFuture
                .supplyAsync({ eventorService.getEventClasses(eventor, eventId) }, executor)
                .exceptionally { ex ->
                    log.warn("Failed to fetch event classes for event {} on eventor {}: {}", eventId, eventor.id, ex.message)
                    null
                }
        }

        CompletableFuture.allOf(*classFutures.map { it.second }.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)

        val classesByEventId: Map<String, org.iof.eventor.EventClassList> = classFutures
            .mapNotNull { (eventId, future) -> future.join()?.let { eventId to it } }
            .toMap()

        // Map raceId -> EventClassList
        val eventClassMap = mutableMapOf<String, org.iof.eventor.EventClassList>()
        for ((raceId, eventId) in raceToEventId) {
            classesByEventId[eventId]?.let { eventClassMap[raceId] = it }
        }
        return eventClassMap
    }

    private fun mergeRaces(
        raceList: MutableList<CalendarRace>,
        personRaces: List<CalendarRace>
    ) {
        // Build a map for O(1) lookup instead of O(n) find operation
        val raceMap = raceList.associateBy { "${it.eventor}_${it.raceId}" }.toMutableMap()

        for (race in personRaces) {
            val key = "${race.eventor}_${race.raceId}"
            val existingRace = raceMap[key]
            if (existingRace != null) {
                existingRace.userEntries.addAll(race.userEntries)
                existingRace.organisationEntries.addAll(race.organisationEntries)
            } else {
                raceMap[key] = race
                raceList.add(race)
            }
        }
    }

    fun getEventList(from: LocalDate, to: LocalDate, classifications: List<EventClassificationEnum>?, userId: UUID?): List<CalendarRace> {
        val eventorList: List<Eventor> = eventorRepository.findAll()

        // Process all eventors in parallel for better performance
        val futures = eventorList.map { eventor ->
            CompletableFuture.supplyAsync({
                val persons: List<Person> = if (userId != null) {
                    personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventor.id)
                } else {
                    emptyList()
                }
                getEventList(eventor = eventor, from = from, to = to, organisations = null, classifications = classifications, persons = persons)
            }, executor).exceptionally { ex ->
                log.warn("Failed to fetch events for eventor {}: {}", eventor.id, ex.message)
                emptyList()
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)

        val result = futures.flatMap { it.join() }
        return filterRacesByDateRange(result, from, to)
    }

    fun getEventList(eventorId: String, from: LocalDate, to: LocalDate, organisations: List<String>?, classifications: List<EventClassificationEnum>?, userId: UUID?): List<CalendarRace> {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()
        // If no userId, fetch events without personal entries
        val persons: List<Person> = if (userId != null) {
            personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventor.id)
        } else {
            emptyList()
        }
        val races = getEventList(eventor = eventor, from = from, to = to, organisations = organisations, classifications = classifications, persons = persons)
        return filterRacesByDateRange(races, from, to)
    }

    private fun getEventList(eventor: Eventor, from: LocalDate, to: LocalDate, organisations: List<String>?, classifications: List<EventClassificationEnum>?, persons: List<Person>): List<CalendarRace> {
        val eventList = eventorService.getEventList(eventor, from, to, organisations, classifications)

        // Use map instead of loop for better performance
        val events = eventList!!.event.map { it.eventId.content }

        // Extract person IDs and organization IDs in one pass
        val personIds = persons.map { it.eventorRef }
        val organisationIds = persons.flatMap { person ->
            person.memberships.mapNotNull { it.organisation?.eventorRef }
        }.distinct()

        log.info("Fetching competitor-count for persons {} and organisations {}.", personIds, organisationIds)
        val competitorCountList = eventorService.getCompetitorCounts(eventor, events, organisationIds, personIds)
        return calendarConverter.convertEvents(eventList, eventor, competitorCountList)
    }

    private fun MutableMap<String, org.iof.eventor.EventClassList>.generateCalendarRaceForPerson(
        eventor: Eventor,
        person: Person,
        entryList: org.iof.eventor.EntryList?,
        startListList: org.iof.eventor.StartListList?,
        resultListList: org.iof.eventor.ResultListList?
    ): List<CalendarRace> {
        var result = calendarConverter.convertEntryList(eventor, entryList, person, this)
        result = calendarConverter.convertStartListList(eventor, startListList, person, result)
        result = calendarConverter.convertResultList(eventor, resultListList, person, result)
        return result.values.toList()
    }

    private fun filterRacesByDateRange(races: List<CalendarRace>, from: LocalDate, to: LocalDate): List<CalendarRace> {
        return races.filter { race ->
            val raceLocalDate = race.raceDate.toLocalDateTime().toLocalDate()
            !raceLocalDate.isBefore(from) && !raceLocalDate.isAfter(to)
        }
    }
}

