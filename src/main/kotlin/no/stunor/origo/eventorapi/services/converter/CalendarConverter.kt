package no.stunor.origo.eventorapi.services.converter
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.calendar.*
import no.stunor.origo.eventorapi.model.event.Event
import org.springframework.stereotype.Component


@Component
class CalendarConverter(
    var organisationRepository: OrganisationRepository,
    var regionRepository: RegionRepository
) {
    var eventConverter = EventConverter()

    var organisationConverter = OrganisationConverter(
        organisationRepository = organisationRepository,
        regionRepository = regionRepository
    )

    /**
     * Convert an Eventor EventList into a list of CalendarRace domain objects.
     * Duplicates are not expected; races are generated per EventRace contained in each Event.
     * @param eventList Source list from Eventor (nullable – returns empty list if null)
     * @param eventor Context describing the Eventor instance/environment
     * @param competitorCountList Counts used for entries / organisation entries
     * @param eventClassesMap Map of eventId to EventClassList for populating event classes
     */
    fun convertEvents(
        eventList: org.iof.eventor.EventList?,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList,
        eventClassesMap: Map<String, org.iof.eventor.EventClassList?> = emptyMap()
    ): List<CalendarRace> = eventList?.event?.flatMap { convertEvent(it, eventor, competitorCountList, eventClassesMap) } ?: emptyList()

    private fun convertEvent(
        event: org.iof.eventor.Event,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList,
        eventClassesMap: Map<String, org.iof.eventor.EventClassList?>
    ): List<CalendarRace> = event.eventRace.map { generateRace(event, it, eventor, competitorCountList, eventClassesMap) }

    private fun generateRace(
        event: org.iof.eventor.Event,
        eventRace: org.iof.eventor.EventRace,
        eventor: Eventor,
        competitorCountList: org.iof.eventor.CompetitorCountList,
        eventClassesMap: Map<String, org.iof.eventor.EventClassList?>
    ): CalendarRace {
        val eventId = event.eventId.content
        val eventClassList = eventClassesMap[eventId]
        val convertedEvent = Event(eventorId = eventor.id, eventorRef = eventId)
        val eventClasses = EventClassConverter.convertEventClasses(eventClassList, convertedEvent)

        return CalendarRace(
            eventor = eventor,
            eventId = eventId,
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
            userEntries = mutableListOf(),
            organisationEntries = getOrganisationEntries(event.eventId.content, eventRace.eventRaceId.content, competitorCountList, eventor),
            signedUp = isSignedUp(event.eventId.content, competitorCountList),
            startList = eventConverter.hasStartList(event.hashTableEntry, eventRace.eventRaceId.content),
            resultList = eventConverter.hasResultList(event.hashTableEntry, eventRace.eventRaceId.content),
            livelox = eventConverter.hasLivelox(event.hashTableEntry),
            eventClasses = eventClasses
        )
    }

    private fun getEntries(eventId: String, eventRaceId: String, competitorCountList: org.iof.eventor.CompetitorCountList?): Int =
        competitorCountList?.competitorCount?.firstOrNull { it.eventId == eventId && (it.eventRaceId == null || it.eventRaceId == eventRaceId) }?.numberOfEntries?.toInt()
            ?: 0

    private fun getOrganisationEntries(
        eventId: String,
        eventRaceId: String,
        competitorCountList: org.iof.eventor.CompetitorCountList,
        eventor: Eventor
    ): MutableList<OrganisationEntries> = competitorCountList.competitorCount
        .filter { isRelevantCompetitorCount(it, eventId, eventRaceId) }
        .flatMap { it.organisationCompetitorCount ?: emptyList() }
        .mapNotNull { occ -> organisationConverter.convertOrganisation(occ.organisationId, eventor.id)?.let { OrganisationEntries(it, occ.numberOfEntries.toInt()) } }
        .toMutableList()

    private fun isRelevantCompetitorCount(competitorCount: org.iof.eventor.CompetitorCount, eventId: String, eventRaceId: String): Boolean =
        competitorCount.eventId == eventId && (competitorCount.eventRaceId == null || competitorCount.eventRaceId == eventRaceId) && competitorCount.organisationCompetitorCount != null

    private fun isSignedUp(eventId: String, competitorCountList: org.iof.eventor.CompetitorCountList?): Boolean =
        competitorCountList?.competitorCount?.any { it.eventId == eventId && !it.classCompetitorCount.isNullOrEmpty() } ?: false
}
