package no.stunor.origo.eventorapi.services

import jakarta.annotation.PreDestroy
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.PartialResult
import no.stunor.origo.eventorapi.model.calendar.CalendarRace
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.services.converter.CalendarConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class CalendarService(
    var personRepository: PersonRepository,
    var eventorRepository: EventorRepository,
    var organisationRepository: OrganisationRepository,
    var regionRepository: RegionRepository,
    var eventorService: EventorService
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    // - Core threads: 4x available processors (balanced approach)
    // - Max threads: 200 (reduced from 500 for better resource management)
    // - Queue: Bounded queue with 100 capacity (prevents unlimited queueing)
    // - Rejection policy: Abort (throws exception when queue is full)
    private val executor = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 4,
        200,
        60L, TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue(100),
        ThreadPoolExecutor.AbortPolicy()
    ).apply {
        // Allow core threads to time out when idle to free resources
        allowCoreThreadTimeOut(true)
    }

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

    fun getEventList(userId: UUID): PartialResult<List<CalendarRace>> {
        val persons = personRepository.findAllByUsers(userId)
        var timedOut = false

        val futures = persons.mapNotNull { person ->
            val eventor = eventorRepository.findById(person.eventorId) ?: return@mapNotNull null
            CompletableFuture
                .supplyAsync({ processPersonEntries(person, eventor) }, executor)
                .exceptionally { ex ->
                    log.warn("Failed to fetch calendar entries for person {} on eventor {}: {}", person.eventorRef, person.eventorId, ex.message)
                    PartialResult(emptyList(), isPartial = false)
                }
        }

        try {
            CompletableFuture.allOf(*futures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            timedOut = true
            log.warn("Timeout while fetching personal entries for user {} after {} seconds. Returning partial results.", userId, batchTimeoutSeconds)
        }

        val raceList = mutableListOf<CalendarRace>()
        futures.forEach { future ->
            try {
                val result = future.join()
                mergeRaces(raceList, result.data)
                if (result.isPartial) timedOut = true
            } catch (ex: Exception) {
                log.warn("Failed to process future result: {}", ex.message)
                timedOut = true
            }
        }
        return PartialResult(raceList, isPartial = timedOut)
    }

     private fun processPersonEntries(person: Person, eventor: Eventor): PartialResult<List<CalendarRace>> {
        var timedOut = false
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
         val entryList = try {
            entriesFuture.get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            timedOut = true
            log.warn("Timeout fetching organisation entries for person {} on eventor {}", person.eventorRef, person.eventorId)
            org.iof.eventor.EntryList()
        }
        val eventClassMap = buildEventClassMap(entryList, eventor)

        // Collect starts and results (likely already done or nearly done by now)
        try {
            CompletableFuture.allOf(startsFuture, resultsFuture).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            timedOut = true
            log.warn("Timeout fetching personal starts/results for person {} on eventor {}", person.eventorRef, person.eventorId)
        }
        val startListList = try {
            startsFuture.join()
        } catch (ex: Exception) {
            log.warn("Failed to retrieve starts for person {}: {}", person.eventorRef, ex.message)
            timedOut = true
            null
        }
        val resultListList = try {
            resultsFuture.join()
        } catch (ex: Exception) {
            log.warn("Failed to retrieve results for person {}: {}", person.eventorRef, ex.message)
            timedOut = true
            null
        }

        val races = eventClassMap.generateCalendarRaceForPerson(
            eventor,
            person,
            entryList,
            startListList,
            resultListList
        )
        return PartialResult(races, isPartial = timedOut)
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

        try {
            CompletableFuture.allOf(*classFutures.map { it.second }.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            log.warn("Timeout fetching event classes for eventor {} after {} seconds. Returning partial event class results.", eventor.id, batchTimeoutSeconds)
        }

        val classesByEventId: Map<String, org.iof.eventor.EventClassList> = classFutures
            .mapNotNull { (eventId, future) ->
                try {
                    future.join()?.let { eventId to it }
                } catch (ex: Exception) {
                    log.warn("Failed to retrieve event classes for event {}: {}", eventId, ex.message)
                    null
                }
            }
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

    fun getEventList(from: LocalDate, to: LocalDate, classifications: List<EventClassificationEnum>?, userId: UUID?): PartialResult<List<CalendarRace>> {
        val eventorList: List<Eventor> = eventorRepository.findAll()
        var timedOut = false

        // Process all eventors in parallel for better performance
        val futures = eventorList.map { eventor ->
            CompletableFuture.supplyAsync({
                val persons: List<Person> = if (userId != null) {
                    personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventor.id)
                } else {
                    emptyList()
                }
                getEventListInternal(eventor = eventor, from = from, to = to, organisations = null, classifications = classifications, persons = persons)
            }, executor).exceptionally { ex ->
                log.warn("Failed to fetch events for eventor {}: {}", eventor.id, ex.message)
                PartialResult(emptyList(), isPartial = false)
            }
        }

        try {
            CompletableFuture.allOf(*futures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            timedOut = true
            log.warn("Timeout fetching events from all eventors after {} seconds. Returning partial results.", batchTimeoutSeconds)
        }

        val result = futures.flatMap {
            try {
                val partialResult = it.join()
                if (partialResult.isPartial) timedOut = true
                partialResult.data
            } catch (ex: Exception) {
                log.warn("Failed to retrieve events from eventors: {}", ex.message)
                timedOut = true
                emptyList()
            }
        }
        return PartialResult(filterRacesByDateRange(result, from, to), isPartial = timedOut)
    }

    fun getEventList(eventorId: String, from: LocalDate, to: LocalDate, organisations: List<String>?, classifications: List<EventClassificationEnum>?, userId: UUID?): PartialResult<List<CalendarRace>> {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()
        // If no userId, fetch events without personal entries
        val persons: List<Person> = if (userId != null) {
            personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventor.id)
        } else {
            emptyList()
        }
        val races = getEventListInternal(eventor = eventor, from = from, to = to, organisations = organisations, classifications = classifications, persons = persons)
        return PartialResult(filterRacesByDateRange(races.data, from, to), isPartial = races.isPartial)
    }

    private fun getEventListInternal(eventor: Eventor, from: LocalDate, to: LocalDate, organisations: List<String>?, classifications: List<EventClassificationEnum>?, persons: List<Person>): PartialResult<List<CalendarRace>> {
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
        val races = calendarConverter.convertEvents(eventList, eventor, competitorCountList)
        return PartialResult(races, isPartial = false)
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

