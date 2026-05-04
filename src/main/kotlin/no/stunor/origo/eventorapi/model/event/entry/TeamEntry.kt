package no.stunor.origo.eventorapi.model.event.entry

import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp
import java.util.UUID


class TeamEntry (
    override var id: UUID = UUID.randomUUID(),
    override var raceEventorRef: String = "",
    var name: String = "",
    var organisations: MutableList<Organisation> = mutableListOf(),
    var teamMembers: MutableList<TeamMember> = mutableListOf(),
    override var classEventorRef: String = "",
    override var eventClass: EventClass = EventClass(),
    override var bib: String? = null,
    override var status: EntryStatus,
    override var startTime: Timestamp? = null,
    override var finishTime: Timestamp? = null,
    override var result: Result? = null
) :  Entry(
    id, raceEventorRef, classEventorRef, eventClass, bib, status, startTime, finishTime, result
)