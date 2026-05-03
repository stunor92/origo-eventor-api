package no.stunor.origo.eventorapi.model.event.entry

import no.stunor.origo.eventorapi.model.event.PunchingUnit
import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.person.Gender
import no.stunor.origo.eventorapi.model.person.PersonName
import java.sql.Timestamp
import java.util.UUID

data class PersonEntry (
    override var id: UUID = UUID.randomUUID(),
    override var entryId: String = UUID.randomUUID().toString(),
    override var raceEventorRef: String = "",
    var name: PersonName = PersonName(),
    var competitorEventorRef: String? = null,
    var personEventorRef: String? = null,
    var organisation: Organisation? = null,
    var birthYear: Int? = null,
    var nationality: String? = null,
    var gender: Gender = Gender.Other,
    override var classEventorRef: String = "",
    override var bib: String? = null,
    var punchingUnits: MutableList<PunchingUnit> = mutableListOf(),
    override var status: EntryStatus,
    override var startTime: Timestamp? = null,
    override var finishTime: Timestamp? = null,
    override var result: Result? = null,
    var splitTimes: MutableList<SplitTime> = mutableListOf(),
) : Entry(
        id, entryId, raceEventorRef, classEventorRef, bib, status, startTime, finishTime, result
)
