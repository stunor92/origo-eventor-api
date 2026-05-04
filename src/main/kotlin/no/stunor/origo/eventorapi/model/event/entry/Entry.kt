package no.stunor.origo.eventorapi.model.event.entry

import no.stunor.origo.eventorapi.model.event.EventClass
import java.sql.Timestamp
import java.util.UUID

abstract class Entry(
    open var id: UUID = UUID.randomUUID(),
    open var raceEventorRef: String,
    @Deprecated("use eventClass") open var classEventorRef: String,
    open var eventClass: EventClass = EventClass(),
    open var bib: String?,
    open var status: EntryStatus,
    open var startTime: Timestamp?,
    open var finishTime: Timestamp?,
    open var result: Result?
)