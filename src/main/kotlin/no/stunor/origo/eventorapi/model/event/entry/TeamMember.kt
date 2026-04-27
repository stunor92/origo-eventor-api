package no.stunor.origo.eventorapi.model.event.entry

import no.stunor.origo.eventorapi.model.event.PunchingUnit
import no.stunor.origo.eventorapi.model.person.Gender
import no.stunor.origo.eventorapi.model.person.PersonName
import java.sql.Timestamp

data class TeamMember(
        val leg: Int = 1,
        var personEventorRef: String? = null,
        var competitorEventorRef: String? = null,
        var name: PersonName? = PersonName(),
        var birthYear: Int? = null,
        var nationality: String? = null,
        var gender: Gender? = null,
        var punchingUnits: MutableList<PunchingUnit> = mutableListOf(),
        var startTime: Timestamp? = null,
        var finishTime: Timestamp? = null,
        var legResult: Result? = null,
        var overallResult: Result? = null,
        var splitTimes: MutableList<SplitTime> = mutableListOf(),
)