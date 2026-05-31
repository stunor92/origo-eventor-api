package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.PartialResult
import no.stunor.origo.eventorapi.model.calendar.*
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.entry.Entry
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamEntry
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.model.person.PersonName
import no.stunor.origo.eventorapi.services.converter.CalendarConverter
import no.stunor.origo.eventorapi.services.converter.EntryListConverter
import no.stunor.origo.eventorapi.services.converter.EventClassConverter
import no.stunor.origo.eventorapi.services.converter.ResultListConverter
import no.stunor.origo.eventorapi.services.converter.StartListConverter
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class CalendarService(
    private val personRepository: PersonRepository,
    private val eventorRepository: EventorRepository,
    private val organisationRepository: OrganisationRepository,
    private val eventorService: EventorService,
    private val calendarConverter: CalendarConverter,
    private val entryListConverter: EntryListConverter,
    private val startListConverter: StartListConverter,
    private val resultListConverter: ResultListConverter,
    private val batchTimeoutMs: Long = 16_000L,
    private val eventListTimeoutMs: Long = 10_000L,
    private val competitorCountTimeoutMs: Long = 4_000L
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun getEventList(
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventorList = eventorRepository.findAll()

        val results = coroutineScope {
            eventorList.map { eventor ->
                async {
                    withTimeoutOrNull(batchTimeoutMs) {
                        try {
                            val persons = resolvePersonsForEventor(eventor.id, userId)
                            getEventListInternal(eventor, from, to, null, classifications, persons)
                        } catch (e: CancellationException) {
                            throw e
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
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()
        val persons = resolvePersonsForEventor(eventor.id, userId)
        val races = withTimeoutOrNull(batchTimeoutMs) {
            try {
                getEventListInternal(eventor, from, to, organisations, classifications, persons)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Failed to fetch events for eventor {}: {}", eventorId, e.message)
                PartialResult(emptyList(), isPartial = false)
            }
        } ?: run {
            log.warn("Timeout fetching events from eventor {} after {} ms", eventorId, batchTimeoutMs)
            PartialResult(emptyList<CalendarRace>(), isPartial = true)
        }
        return PartialResult(filterRacesByDateRange(races.data, from, to), isPartial = races.isPartial)
    }

    suspend fun getPersonalEventList(
        eventorId: String,
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID
    ): List<CalendarRace> {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()
        val persons = personRepository.findAllByUsersAndEventorId(userId, eventorId)
        if (persons.isEmpty()) return emptyList()

        val personRefs = persons.map { it.eventorRef }.toSet()
        val orgRefs = persons.flatMap { p -> p.memberships.mapNotNull { it.organisation?.eventorRef } }.distinct()

        val eventList = eventorService.getEventList(eventor, from, to, null, classifications, timeoutMs = eventListTimeoutMs)
            ?: return emptyList()
        val eventIds = eventList.event.map { it.eventId.content }

        val competitorCountList = try {
            eventorService.getCompetitorCounts(eventor, eventIds, orgRefs, personRefs.toList(), timeoutMs = competitorCountTimeoutMs)
        } catch (e: Exception) {
            log.warn("Competitor count unavailable for personal event list, eventor {}: {}", eventorId, e.message)
            null
        }

        val allRaces = calendarConverter.convertEvents(eventList, eventor, competitorCountList)
        val signedUpRaces = filterRacesByDateRange(allRaces, from, to).filter { it.signedUp }
        if (signedUpRaces.isEmpty()) return emptyList()

        val orgCache = organisationRepository.findAllByEventorId(eventor.id)

        return coroutineScope {
            signedUpRaces.map { race ->
                async { enrichRaceWithPersonalData(eventor, race, personRefs, orgCache) }
            }.awaitAll()
        }
    }

    private suspend fun enrichRaceWithPersonalData(
        eventor: Eventor,
        race: CalendarRace,
        personRefs: Set<String>,
        orgCache: Map<String, Organisation>
    ): CalendarRace = coroutineScope {
        val classesDef = async {
            try { eventorService.getEventClasses(eventor, race.eventId) } catch (e: Exception) { null }
        }
        val entriesDef = async {
            try { fetchPersonalEntries(eventor, race, orgCache) } catch (e: Exception) { emptyList() }
        }

        val eventClassList = classesDef.await()
        val entries = entriesDef.await()

        val event = Event(eventorId = eventor.id, eventorRef = race.eventId)
        val convertedClasses = EventClassConverter.convertEventClasses(eventClassList, event)
        val classMap = convertedClasses.associateBy { it.eventorRef }

        race.eventClasses = convertedClasses
        race.userEntries = entries
            .filter { matchesPerson(it, personRefs) }
            .mapNotNull { toCalendarCompetitor(it, race, classMap) }
            .toMutableList()

        race
    }

    private suspend fun fetchPersonalEntries(
        eventor: Eventor,
        race: CalendarRace,
        orgCache: Map<String, Organisation>
    ): List<Entry> = when {
        race.resultList -> {
            val rl = eventorService.getEventResultList(eventor.baseUrl, eventor.eventorApiKey, race.eventId)
            rl?.let { resultListConverter.convertEventResultList(eventor, it, orgCache) } ?: emptyList()
        }
        race.startList -> {
            val sl = eventorService.getEventStartList(eventor.baseUrl, eventor.eventorApiKey, race.eventId)
            sl?.let { startListConverter.convertEventStartList(eventor, it, orgCache) } ?: emptyList()
        }
        else -> {
            val el = eventorService.getEventEntryList(eventor.baseUrl, eventor.eventorApiKey, race.eventId)
            el?.takeIf { !it.entry.isNullOrEmpty() }
                ?.let { entryListConverter.convertEventEntryList(eventor, it, orgCache) }
                ?: emptyList()
        }
    }

    private fun matchesPerson(entry: Entry, personRefs: Set<String>): Boolean = when (entry) {
        is PersonEntry -> entry.personEventorRef != null && entry.personEventorRef in personRefs
        is TeamEntry   -> entry.teamMembers.any { it.personEventorRef != null && it.personEventorRef in personRefs }
        else           -> false
    }

    private fun toCalendarCompetitor(entry: Entry, race: CalendarRace, classMap: Map<String, EventClass>): CalendarCompetitor? {
        val eventClass = classMap[entry.classEventorRef] ?: EventClass()
        return when {
            entry is PersonEntry && race.resultList -> CalendarCompetitor(
                personId = entry.personEventorRef ?: "",
                name = entry.name,
                personResult = entry.result?.let { CalendarPersonResult(result = it, bib = entry.bib, eventClass = eventClass) }
            )
            entry is PersonEntry && race.startList -> CalendarCompetitor(
                personId = entry.personEventorRef ?: "",
                name = entry.name,
                personStart = CalendarPersonStart(startTime = entry.startTime, bib = entry.bib, eventClass = eventClass)
            )
            entry is PersonEntry -> CalendarCompetitor(
                personId = entry.personEventorRef ?: "",
                name = entry.name,
                personEntry = CalendarEntry(eventClass = eventClass, punchingUnits = entry.punchingUnits)
            )
            entry is TeamEntry -> {
                val member = entry.teamMembers.firstOrNull { it.personEventorRef != null } ?: return null
                when {
                    race.resultList -> CalendarCompetitor(
                        personId = member.personEventorRef ?: "",
                        name = member.name ?: PersonName(),
                        teamResult = member.overallResult?.let {
                            CalendarTeamResult(
                                teamName = entry.name,
                                leg = member.leg,
                                result = it,
                                legResult = member.legResult ?: it,
                                bib = entry.bib,
                                eventClass = eventClass
                            )
                        }
                    )
                    race.startList -> CalendarCompetitor(
                        personId = member.personEventorRef ?: "",
                        name = member.name ?: PersonName(),
                        teamStart = CalendarTeamStart(
                            teamName = entry.name,
                            startTime = member.startTime,
                            bib = entry.bib,
                            leg = member.leg,
                            eventClass = eventClass
                        )
                    )
                    else -> null
                }
            }
            else -> null
        }
    }

    private suspend fun resolvePersonsForEventor(eventorId: String, userId: UUID?): List<Person> =
        if (userId != null) personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventorId)
        else emptyList()

    private suspend fun getEventListInternal(
        eventor: Eventor,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        persons: List<Person>
    ): PartialResult<List<CalendarRace>> {
        val eventList = eventorService.getEventList(eventor, from, to, organisations, classifications,
            timeoutMs = eventListTimeoutMs)
            ?: return PartialResult(emptyList(), isPartial = false)
        val events = eventList.event.map { it.eventId.content }

        val personIds = persons.map { it.eventorRef }
        val organisationIds = persons.flatMap { person ->
            person.memberships.mapNotNull { it.organisation?.eventorRef }
        }.distinct()

        val competitorCountList = try {
            eventorService.getCompetitorCounts(eventor, events, organisationIds, personIds,
                timeoutMs = competitorCountTimeoutMs)
        } catch (e: Exception) {
            log.warn("Competitor count unavailable for eventor {} ({}), returning events without counts", eventor.id, e.message)
            null
        }

        val races = calendarConverter.convertEvents(eventList, eventor, competitorCountList)
        return PartialResult(races, isPartial = false)
    }

    private fun filterRacesByDateRange(races: List<CalendarRace>, from: LocalDate, to: LocalDate): List<CalendarRace> =
        races.filter { race ->
            val raceLocalDate = race.raceDate.toLocalDateTime().toLocalDate()
            !raceLocalDate.isBefore(from) && !raceLocalDate.isAfter(to)
        }
}
