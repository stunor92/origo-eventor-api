package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.*
import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp
import java.text.ParseException

class EventConverter(
    private val entryListConverter: EntryListConverter
) {
    fun convertEventClassification(eventForm: String?): EventClassificationEnum {
        return when (eventForm) {
            "1" -> EventClassificationEnum.Championship
            "2" -> EventClassificationEnum.National
            "3" -> EventClassificationEnum.Regional
            "4" -> EventClassificationEnum.Local
            else -> EventClassificationEnum.Club
        }
    }

    fun convertEventStatus(eventStatusId: String?): EventStatusEnum {
        return when (eventStatusId) {
            "2" -> EventStatusEnum.RegionApproved
            "3" -> EventStatusEnum.Approved
            "4" -> EventStatusEnum.Created
            "5" -> EventStatusEnum.EntryOpen
            "6" -> EventStatusEnum.EntryPaused
            "7" -> EventStatusEnum.EntryClosed
            "8" -> EventStatusEnum.Live
            "9" -> EventStatusEnum.Completed
            "10" -> EventStatusEnum.Canceled
            "11" -> EventStatusEnum.Reported
            else -> EventStatusEnum.Applied
        }
    }

    fun convertEventDisciplines(disciplineIds: List<Any>): List<Discipline> {
        return disciplineIds.map { convertEventDiscipline(it) }
    }

    fun convertEventForm(eventForm: String?): EventFormEnum {
        return when (eventForm) {
            "IndSingleDay", "IndMultiDay" -> EventFormEnum.Individual
            "RelaySingleDay", "RelayMultiDay" -> EventFormEnum.Relay
            "PatrolSingleDay", "PatrolMultiDay" -> EventFormEnum.Patrol
            "TeamSingleDay", "TeamMultiDay" -> EventFormEnum.Team
            else -> EventFormEnum.Individual
        }
    }

    private fun convertEventDiscipline(disciplineId: Any): Discipline {
        return when ((disciplineId as org.iof.eventor.DisciplineId).content) {
            "1" -> Discipline.FootO
            "2" -> Discipline.MtbO
            "3" -> Discipline.SkiO
            "4" -> Discipline.PreO
            else -> Discipline.FootO
        }
    }

    private fun updateBasicFields(existing: Event, eventorEvent: org.iof.eventor.Event, eventor: Eventor, organisations: List<Organisation>) {
        existing.eventorId = eventor.id
        existing.eventorRef = eventorEvent.eventId.content
        existing.name = eventorEvent.name.content
        existing.type = convertEventForm(eventorEvent.eventForm)
        existing.classification = convertEventClassification(eventorEvent.eventClassificationId.content)
        existing.status = convertEventStatus(eventorEvent.eventStatusId.content)
        existing.disciplines = convertEventDisciplines(eventorEvent.disciplineIdOrDiscipline).toTypedArray()
        existing.startDate = TimeStampConverter.parseDate("${eventorEvent.startDate.date.content} ${eventorEvent.startDate.clock.content}", eventor.id)
        existing.finishDate = TimeStampConverter.parseDate("${eventorEvent.finishDate.date.content} ${eventorEvent.finishDate.clock.content}", eventor.id)
        existing.organisers = mergeOrganisers(existing.organisers, organisations)
        existing.entryBreaks = convertEntryBreaks(eventorEvent.entryBreak, eventor).toTypedArray()
        existing.punchingUnitTypes = entryListConverter.convertPunchingUnitTypes(eventorEvent.punchingUnitType).toTypedArray()
        existing.webUrls = emptyList()
        existing.message = convertHostMessage(eventorEvent.hashTableEntry)
        existing.email = null
        existing.phone = null
    }

    private fun <T, K> mergeByKey(existing: List<T>, incoming: List<T>, keySelector: (T) -> K, updater: (T, T) -> Unit): List<T> {
        val existingMap = existing.associateBy { keySelector(it) }.toMutableMap()
        for (inc in incoming) {
            val key = keySelector(inc)
            val match = existingMap[key]
            if (match != null) updater(match, inc) else existingMap[key] = inc
        }
        return existingMap.values.toList()
    }

    private fun mergeEventClasses(existing: Event, classes: org.iof.eventor.EventClassList?) {
        val incoming = EventClassConverter.convertEventClasses(eventCLassList = classes, event = existing)
        val merged = mergeByKey(existing.classes, incoming, { it.eventorRef }) { target, src ->
            target.name = src.name; target.shortName = src.shortName; target.type = src.type
            target.minAge = src.minAge; target.maxAge = src.maxAge; target.gender = src.gender
            target.presentTime = src.presentTime; target.orderedResult = src.orderedResult
            target.legs = src.legs; target.minAverageAge = src.minAverageAge; target.maxAverageAge = src.maxAverageAge
        }
        existing.classes.clear()
        existing.classes.addAll(merged)
    }

    private fun mergeRaces(existing: Event, eventorEvent: org.iof.eventor.Event, eventor: Eventor) {
        val incoming = convertRaces(existing, eventorEvent.hashTableEntry, eventorEvent.eventRace, eventor)
        val merged = mergeByKey(existing.races, incoming, { it.eventorRef }) { target, src ->
            target.name = src.name; target.lightCondition = src.lightCondition; target.distance = src.distance
            target.date = src.date; target.position = src.position; target.startList = src.startList
            target.resultList = src.resultList; target.livelox = src.livelox
        }
        existing.races.clear()
        existing.races.addAll(merged)
    }

    private fun mergeDocuments(existing: Event, documents: org.iof.eventor.DocumentList?) {
        val incoming = convertEventDocument(documents, event = existing)
        val merged = mergeByKey(existing.documents, incoming, { it.eventorRef }) { target, src ->
            target.name = src.name; target.url = src.url; target.type = src.type
        }
        existing.documents.clear()
        existing.documents.addAll(merged)
    }

    private fun mergeOrganisers(existing: List<Organisation>, incoming: List<Organisation>): MutableList<Organisation> {
        val result = existing.associateBy { it.eventorRef }.toMutableMap()
        for (org in incoming) result.putIfAbsent(org.eventorRef, org)
        return result.values.toMutableList()
    }

    private fun createNewEvent(eventorEvent: org.iof.eventor.Event, classes: org.iof.eventor.EventClassList?, documents: org.iof.eventor.DocumentList?, organisations: MutableList<Organisation>, eventor: Eventor): Event {
        val event = Event(
            id = null,
            eventorId = eventor.id,
            eventorRef = eventorEvent.eventId.content,
            name = eventorEvent.name.content,
            type = convertEventForm(eventorEvent.eventForm),
            classification = convertEventClassification(eventorEvent.eventClassificationId.content),
            status = convertEventStatus(eventorEvent.eventStatusId.content),
            disciplines = convertEventDisciplines(eventorEvent.disciplineIdOrDiscipline).toTypedArray(),
            startDate = TimeStampConverter.parseDate("${eventorEvent.startDate.date.content} ${eventorEvent.startDate.clock.content}", eventor.id),
            finishDate = TimeStampConverter.parseDate("${eventorEvent.finishDate.date.content} ${eventorEvent.finishDate.clock.content}", eventor.id),
            organisers = organisations,
            entryBreaks = convertEntryBreaks(eventorEvent.entryBreak, eventor).toTypedArray(),
            punchingUnitTypes = entryListConverter.convertPunchingUnitTypes(eventorEvent.punchingUnitType).toTypedArray(),
            webUrls = emptyList(),
            message = convertHostMessage(eventorEvent.hashTableEntry),
            email = null,
            phone = null
        )
        event.classes = EventClassConverter.convertEventClasses(eventCLassList = classes, event = event).toMutableList()
        event.races = convertRaces(event, eventorEvent.hashTableEntry, eventorEvent.eventRace, eventor).toMutableList()
        event.documents = convertEventDocument(documents, event = event).toMutableList()
        return event
    }

    @Throws(ParseException::class)
    fun convertEvent(
        existingEvent: Event?,
        eventorEvent: org.iof.eventor.Event,
        classes: org.iof.eventor.EventClassList?,
        documents: org.iof.eventor.DocumentList?,
        organisations: MutableList<Organisation>,
        eventor: Eventor
    ): Event {
        return if (existingEvent != null) {
            updateBasicFields(existingEvent, eventorEvent, eventor, organisations)
            mergeEventClasses(existingEvent, classes)
            mergeRaces(existingEvent, eventorEvent, eventor)
            mergeDocuments(existingEvent, documents)
            existingEvent
        } else {
            createNewEvent(eventorEvent, classes, documents, organisations, eventor)
        }
    }

    private fun convertRaces(event: Event, hashTableEntries: List<org.iof.eventor.HashTableEntry>, eventRaces: List<org.iof.eventor.EventRace>, eventor: Eventor): List<Race> {
        return eventRaces.map { convertRace(event, hashTableEntries, it, eventor) }
    }

    private fun convertRace(event: Event, hashTableEntries: List<org.iof.eventor.HashTableEntry>, eventRace: org.iof.eventor.EventRace, eventor: Eventor): Race {
        return Race(
            eventorRef = eventRace.eventRaceId.content,
            name = eventRace.name.content,
            lightCondition = convertLightCondition(eventRace.raceLightCondition),
            distance = convertRaceDistance(eventRace.raceDistance),
            date = if (eventRace.raceDate != null) TimeStampConverter.parseDate("${eventRace.raceDate.date.content} ${eventRace.raceDate.clock.content}", eventor.id) else null,
            position = if (eventRace.eventCenterPosition != null) convertPosition(eventRace.eventCenterPosition) else null,
            startList = hasStartList(hashTableEntries, eventRace.eventRaceId.content),
            resultList = hasResultList(hashTableEntries, eventRace.eventRaceId.content),
            livelox = hasLivelox(hashTableEntries),
            event = event,
        )
    }

    fun convertLightCondition(lightCondition: String?): LightConditionEnum = when (lightCondition) {
        "Day" -> LightConditionEnum.Day; "Night" -> LightConditionEnum.Night; "DayAndNight" -> LightConditionEnum.DayAndNight; else -> LightConditionEnum.Day
    }

    fun convertRaceDistance(raceDistance: String?): DistanceEnum = when (raceDistance) {
        null -> DistanceEnum.Middle; "Long" -> DistanceEnum.Long; "Middle" -> DistanceEnum.Middle
        "Sprint", "SprintRelay" -> DistanceEnum.Sprint; "Ultralong" -> DistanceEnum.UltraLong
        "Pre-O" -> DistanceEnum.PreO; "Temp-O" -> DistanceEnum.TempO; else -> DistanceEnum.Middle
    }

    fun hasStartList(hashTableEntries: List<org.iof.eventor.HashTableEntry>, eventRaceId: String): Boolean =
        hashTableEntries.any { it.key.content == "startList_$eventRaceId" }

    fun hasResultList(hashTableEntries: List<org.iof.eventor.HashTableEntry>, eventRaceId: String): Boolean =
        hashTableEntries.any { it.key.content == "officialResult_$eventRaceId" }

    fun hasLivelox(hashTableEntries: List<org.iof.eventor.HashTableEntry>): Boolean =
        hashTableEntries.any { it.key.content == "Eventor_LiveloxEventConfigurations" }

    fun convertPosition(eventCenterPosition: org.iof.eventor.EventCenterPosition): RacePosition =
        RacePosition(eventCenterPosition.y.toDouble(), eventCenterPosition.x.toDouble())

    private fun convertHostMessage(hashTableEntries: List<org.iof.eventor.HashTableEntry>): String? =
        hashTableEntries.firstOrNull { it.key.content == "Eventor_Message" }?.value?.content

    private fun convertEventDocument(documentList: org.iof.eventor.DocumentList?, event: Event): List<Document> {
        if (documentList == null) return mutableListOf()
        return documentList.document.map { document ->
            Document(eventorRef = document.id.toString(), name = document.name, url = document.url, type = document.type, event = event)
        }
    }

    fun convertEntryBreaks(entryBreaks: List<org.iof.eventor.EntryBreak>, eventor: Eventor): List<Timestamp> {
        return entryBreaks.mapNotNull { entryBreak ->
            if (entryBreak.validToDate != null)
                TimeStampConverter.parseDate("${entryBreak.validToDate.date.content} ${entryBreak.validToDate.clock.content}", eventor.id)
            else null
        }
    }
}
