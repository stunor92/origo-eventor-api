package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.calendar.CalendarCompetitor
import no.stunor.origo.eventorapi.model.calendar.CalendarRace
import no.stunor.origo.eventorapi.model.calendar.PersonalCalendarRace
import no.stunor.origo.eventorapi.model.organisation.Organisation

class CalendarConverter(
    private val eventConverter: EventConverter,
    private val organisationConverter: OrganisationConverter
) {

    fun convertEvents(
        eventList: org.iof.eventor.EventList?,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList?
    ): List<CalendarRace> = eventList?.event?.flatMap { convertEvent(it, eventor, competitorCountList) } ?: emptyList()

    private fun convertEvent(
        event: org.iof.eventor.Event,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList?
    ): List<CalendarRace> = event.eventRace.map { generateRace(event, it, eventor, competitorCountList) }

    private fun generateRace(
        event: org.iof.eventor.Event,
        eventRace: org.iof.eventor.EventRace,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList?
    ): CalendarRace {
        return CalendarRace(
            eventor = eventor,
            eventId = event.eventId.content,
            eventName = event.name.content,
            raceId = eventRace.eventRaceId.content,
            raceName = eventRace.name.content,
            raceDate = TimeStampConverter.parseDate("${eventRace.raceDate.date.content} 00:00:00"),
            type = eventConverter.convertEventForm(event.eventForm),
            classification = eventConverter.convertEventClassification(event.eventClassificationId.content),
            lightCondition = eventConverter.convertLightCondition(eventRace.raceLightCondition),
            distance = eventConverter.convertRaceDistance(eventRace.raceDistance),
            position = eventRace.eventCenterPosition?.let { eventConverter.convertPosition(it) },
            status = eventConverter.convertEventStatus(event.eventStatusId.content),
            disciplines = eventConverter.convertEventDisciplines(event.disciplineIdOrDiscipline),
            organisers = event.organiser?.let { organisationConverter.convertOrganisations(it.organisationIdOrOrganisation, eventor.id) } ?: listOf(),
            entryBreaks = eventConverter.convertEntryBreaks(event.entryBreak, eventor),
            entries = getEntries(event.eventId.content, eventRace.eventRaceId.content, competitorCountList),
            signedUp = isSignedUp(event.eventId.content, competitorCountList),
            startList = eventConverter.hasStartList(event.hashTableEntry, eventRace.eventRaceId.content),
            resultList = eventConverter.hasResultList(event.hashTableEntry, eventRace.eventRaceId.content),
            livelox = eventConverter.hasLivelox(event.hashTableEntry)
        )
    }

    private fun getEntries(eventId: String, eventRaceId: String, competitorCountList: org.iof.eventor.CompetitorCountList?): Int =
        competitorCountList?.competitorCount?.firstOrNull { it.eventId == eventId && (it.eventRaceId == null || it.eventRaceId == eventRaceId) }?.numberOfEntries?.toInt() ?: 0

    private fun isSignedUp(eventId: String, competitorCountList: org.iof.eventor.CompetitorCountList?): Boolean =
        competitorCountList?.competitorCount?.any { it.eventId == eventId && !it.classCompetitorCount.isNullOrEmpty() } ?: false

    fun buildPersonalRace(
        event: org.iof.eventor.Event,
        eventRace: org.iof.eventor.EventRace,
        eventor: Eventor,
        orgCache: Map<String, Organisation>,
        competitors: List<CalendarCompetitor>
    ): PersonalCalendarRace = PersonalCalendarRace(
        eventor        = eventor,
        eventId        = event.eventId.content,
        eventName      = event.name.content,
        raceId         = eventRace.eventRaceId.content,
        raceName       = eventRace.name?.content,
        raceDate       = TimeStampConverter.parseDate("${eventRace.raceDate.date.content} 00:00:00"),
        type           = eventConverter.convertEventForm(event.eventForm),
        classification = eventConverter.convertEventClassification(event.eventClassificationId.content),
        lightCondition = eventConverter.convertLightCondition(eventRace.raceLightCondition),
        distance       = eventConverter.convertRaceDistance(eventRace.raceDistance),
        position       = eventRace.eventCenterPosition?.let { eventConverter.convertPosition(it) },
        status         = eventConverter.convertEventStatus(event.eventStatusId.content),
        disciplines    = eventConverter.convertEventDisciplines(event.disciplineIdOrDiscipline),
        organisers     = event.organiser?.let {
            organisationConverter.convertOrganisations(it.organisationIdOrOrganisation, eventor.id, orgCache)
        } ?: listOf(),
        competitors    = competitors
    )
}
