package no.stunor.origo.eventorapi.services

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
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private typealias CalendarRaceResult = PartialResult<List<CalendarRace>>
private typealias CalendarRaceResultFuture = CompletableFuture<CalendarRaceResult>

class CalendarService(
    private val personRepository: PersonRepository,
    private val eventorRepository: EventorRepository,
    private val organisationRepository: OrganisationRepository,
    private val regionRepository: RegionRepository,
    private val eventorService: EventorService,
    private val calendarConverter: CalendarConverter
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val executor = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 4,
        500,
        60L, TimeUnit.SECONDS,
        SynchronousQueue()
    )
    private val batchTimeoutSeconds = 30L

    fun shutdownExecutor() {
        executor.shutdown()
        try {
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

    fun getEventList(
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventorList = eventorRepository.findAll()
        val futures: List<CalendarRaceResultFuture> = eventorList.map { eventor ->
            createEventorFetchFuture(eventor, from, to, classifications, userId)
        }
        val waitTimedOut = awaitBatchOrTimeout(futures)
        val completedResults = collectCompletedResults(futures)
        val result = completedResults.flatMap { it.data }
        val isPartial = computeIsPartial(waitTimedOut, futures, completedResults)
        return PartialResult(filterRacesByDateRange(result, from, to), isPartial)
    }

    fun getEventList(
        eventorId: String,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()
        val persons = resolvePersonsForEventor(eventor.id, userId)
        val races = getEventListInternal(
            eventor = eventor,
            from = from,
            to = to,
            organisations = organisations,
            classifications = classifications,
            persons = persons
        )
        return PartialResult(filterRacesByDateRange(races.data, from, to), isPartial = races.isPartial)
    }

    private fun createEventorFetchFuture(
        eventor: Eventor,
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): CalendarRaceResultFuture {
        return CompletableFuture.supplyAsync({
            val persons = resolvePersonsForEventor(eventor.id, userId)
            getEventListInternal(
                eventor = eventor,
                from = from,
                to = to,
                organisations = null,
                classifications = classifications,
                persons = persons
            )
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch events for eventor {}: {}", eventor.id, ex.message)
            PartialResult(emptyList(), isPartial = false)
        }
    }

    private fun awaitBatchOrTimeout(futures: List<CalendarRaceResultFuture>): Boolean {
        return try {
            CompletableFuture.allOf(*futures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)
            false
        } catch (_: java.util.concurrent.TimeoutException) {
            log.warn("Timeout fetching events from all eventors after {} seconds. Returning partial results.", batchTimeoutSeconds)
            true
        }
    }

    private fun collectCompletedResults(futures: List<CalendarRaceResultFuture>): List<CalendarRaceResult> {
        return futures.mapNotNull { future ->
            if (!future.isDone) return@mapNotNull null
            try {
                future.join()
            } catch (ex: Exception) {
                log.warn("Failed to retrieve events from eventors: {}", ex.message)
                PartialResult(emptyList(), isPartial = true)
            }
        }
    }

    private fun computeIsPartial(
        waitTimedOut: Boolean,
        futures: List<CalendarRaceResultFuture>,
        completedResults: List<CalendarRaceResult>
    ): Boolean = waitTimedOut || futures.any { !it.isDone } || completedResults.any { it.isPartial }

    private fun resolvePersonsForEventor(eventorId: String, userId: UUID?): List<Person> {
        return if (userId != null) {
            personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventorId)
        } else {
            emptyList()
        }
    }

    private fun getEventListInternal(
        eventor: Eventor,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        persons: List<Person>
    ): PartialResult<List<CalendarRace>> {
        val eventList = eventorService.getEventList(eventor, from, to, organisations, classifications)
        val events = eventList!!.event.map { it.eventId.content }

        val personIds = persons.map { it.eventorRef }
        val organisationIds = persons.flatMap { person ->
            person.memberships.mapNotNull { it.organisation?.eventorRef }
        }.distinct()

        log.info("Fetching competitor-count for persons {} and organisations {}.", personIds, organisationIds)
        val competitorCountList = eventorService.getCompetitorCounts(eventor, events, organisationIds, personIds)

        log.info("Fetching event classes for {} events", events.size)
        val eventClassesFutures = events.map { eventId ->
            CompletableFuture.supplyAsync({
                eventId to eventorService.getEventClasses(eventor, eventId)
            }, executor).exceptionally { ex ->
                log.warn("Failed to fetch event classes for event {}: {}", eventId, ex.message)
                eventId to null
            }
        }

        try {
            CompletableFuture.allOf(*eventClassesFutures.toTypedArray()).get(batchTimeoutSeconds, TimeUnit.SECONDS)
        } catch (_: java.util.concurrent.TimeoutException) {
            log.warn("Timeout fetching event classes after {} seconds", batchTimeoutSeconds)
        }

        val eventClassesMap = eventClassesFutures.mapNotNull { future ->
            if (future.isDone) {
                try { future.join() } catch (ex: Exception) {
                    log.warn("Failed to retrieve event classes: {}", ex.message)
                    null
                }
            } else null
        }.toMap()

        val races = calendarConverter.convertEvents(eventList, eventor, competitorCountList, eventClassesMap)
        return PartialResult(races, isPartial = false)
    }

    private fun filterRacesByDateRange(races: List<CalendarRace>, from: LocalDate, to: LocalDate): List<CalendarRace> {
        return races.filter { race ->
            val raceLocalDate = race.raceDate.toLocalDateTime().toLocalDate()
            !raceLocalDate.isBefore(from) && !raceLocalDate.isAfter(to)
        }
    }
}
