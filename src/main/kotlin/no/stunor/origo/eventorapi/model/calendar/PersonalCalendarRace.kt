package no.stunor.origo.eventorapi.model.calendar

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.*
import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp

data class PersonalCalendarRace(
    val eventor: Eventor,
    val eventId: String,
    val eventName: String,
    val raceId: String,
    val raceName: String?,
    val raceDate: Timestamp,
    val type: EventFormEnum,
    val classification: EventClassificationEnum,
    val lightCondition: LightConditionEnum,
    val distance: DistanceEnum,
    val position: RacePosition?,
    val status: EventStatusEnum,
    val disciplines: List<Discipline>,
    val organisers: List<Organisation>,
    val entries: Int,
    val startList: Boolean,
    val resultList: Boolean,
    val livelox: Boolean,
    val eventClasses: List<EventClass>,
    val competitors: List<CalendarCompetitor>
)
