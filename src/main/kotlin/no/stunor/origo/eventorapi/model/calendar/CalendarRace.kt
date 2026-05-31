package no.stunor.origo.eventorapi.model.calendar

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.*
import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp

data class CalendarRace(
    var eventor: Eventor = Eventor(),
    var eventId: String = "",
    var eventName: String = "",
    var raceId: String = "",
    var raceName: String? = null,
    var raceDate: Timestamp,
    var type: EventFormEnum = EventFormEnum.Individual,
    var classification: EventClassificationEnum = EventClassificationEnum.Club,
    var lightCondition: LightConditionEnum = LightConditionEnum.Day,
    var distance: DistanceEnum = DistanceEnum.Middle,
    var position: RacePosition? = null,
    var status: EventStatusEnum = EventStatusEnum.Applied,
    var disciplines: List<Discipline> = listOf(),
    var organisers: List<Organisation> = listOf(),
    var entryBreaks: List<Timestamp> = listOf(),
    var entries: Int = 0,
    var userEntries: MutableList<CalendarCompetitor> = mutableListOf(),
    var organisationEntries: MutableList<OrganisationEntries> = mutableListOf(),
    var signedUp: Boolean = false,
    var startList: Boolean = false,
    var resultList: Boolean = false,
    var livelox: Boolean = false,
    var eventClasses: List<EventClass> = listOf()
)