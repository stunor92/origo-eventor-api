package no.stunor.origo.eventorapi.model.event.entry

import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp
import java.util.UUID


class TeamEntry (
    override var id: UUID = UUID.randomUUID(),
    override var entryId: String = UUID.randomUUID().toString(),
    override var raceEventorRef: String = "",
    var name: String = "",
    var organisations: MutableList<Organisation> = mutableListOf(),
    var teamMembers: MutableList<TeamMember> = mutableListOf(),
    override var classEventorRef: String = "",
    override var bib: String? = null,
    override var status: EntryStatus,
    override var startTime: Timestamp? = null,
    override var finishTime: Timestamp? = null,
    override var result: Result? = null
) :  Entry(
    id, entryId, raceEventorRef, classEventorRef, bib, status, startTime, finishTime, result
)