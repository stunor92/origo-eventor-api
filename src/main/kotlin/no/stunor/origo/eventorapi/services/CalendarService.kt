package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
import java.util.UUID

class CalendarService(
    private val personRepository: PersonRepository,
    private val eventorRepository: EventorRepository,
    private val organisationRepository: OrganisationRepository,
    private val regionRepository: RegionRepository,
    private val eventorService: EventorService,
    private val calendarConverter: CalendarConverter,
    private val batchTimeoutMs: Long = 30_000L
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun getEventList(
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventorList = withContext(Dispatchers.IO) { eventorRepository.findAll() }

        val results = coroutineScope {
            eventorList.map { eventor ->
                async {
                    withTimeoutOrNull(batchTimeoutMs) {
                        try {
                            val persons = resolvePersonsForEventor(eventor.id, userId)
                            getEventListInternal(eventor, from, to, null, classifications, persons)
                        } catch (e: Exception) {
                            log.warn("Failed to fetch events for eventor {}: {}", eventor.id, e.message)
                            PartialResult(emptyList<CalendarRace>(), isPartial = false)
                        }
                    } ?: run {
                        log.warn("Timeout fetching events from eventor {} after {} ms", eventor.id, batchTimeoutMs)
                        PartialResult(emptyList<CalendarRace>(), isPartial = true)
                    }
                }
            }.awaitAll()
        }

        val allRaces = results.flatMap { it.data }
        val isPartial = results.any { it.isPartial }
        return PartialResult(filterRacesByDateRange(allRaces, from, to), isPartial)
    }

    suspend fun getEventList(
        eventorId: String,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventor = withContext(Dispatchers.IO) { eventorRepository.findById(eventorId) }
            ?: throw EventorNotFoundException()
        val persons = resolvePersonsForEventor(eventor.id, userId)
        val races = getEventListInternal(eventor, from, to, organisations, classifications, persons)
        return PartialResult(filterRacesByDateRange(races.data, from, to), isPartial = races.isPartial)
    }

    private suspend fun resolvePersonsForEventor(eventorId: String, userId: UUID?): List<Person> {
        return if (userId != null) {
            withContext(Dispatchers.IO) {
                personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventorId)
            }
        } else {
            emptyList()
        }
    }

    private suspend fun getEventListInternal(
        eventor: Eventor,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        persons: List<Person>
    ): PartialResult<List<CalendarRace>> {
        val eventList = eventorService.getEventList(eventor, from, to, organisations, classifications)
            ?: return PartialResult(emptyList(), isPartial = false)
        val events = eventList.event.map { it.eventId.content }

        val personIds = persons.map { it.eventorRef }
        val organisationIds = persons.flatMap { person ->
            person.memberships.mapNotNull { it.organisation?.eventorRef }
        }.distinct()

        log.info("Fetching competitor-count for persons {} and organisations {}.", personIds, organisationIds)
        val competitorCountList = eventorService.getCompetitorCounts(eventor, events, organisationIds, personIds)

        log.info("Fetching event classes for {} events", events.size)
        val eventClassesMap = coroutineScope {
            events.map { eventId ->
                async {
                    runCatching { eventId to eventorService.getEventClasses(eventor, eventId) }
                        .onFailure { log.warn("Failed to fetch event classes for event {}: {}", eventId, it.message) }
                        .getOrNull() ?: (eventId to null)
                }
            }.awaitAll()
        }.toMap()

        val races = withContext(Dispatchers.IO) { calendarConverter.convertEvents(eventList, eventor, competitorCountList, eventClassesMap) }
        return PartialResult(races, isPartial = false)
    }

    private fun filterRacesByDateRange(races: List<CalendarRace>, from: LocalDate, to: LocalDate): List<CalendarRace> {
        return races.filter { race ->
            val raceLocalDate = race.raceDate.toLocalDateTime().toLocalDate()
            !raceLocalDate.isBefore(from) && !raceLocalDate.isAfter(to)
        }
    }
}
