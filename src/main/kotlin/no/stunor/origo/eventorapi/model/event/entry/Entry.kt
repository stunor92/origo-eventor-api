package no.stunor.origo.eventorapi.model.event.entry

import java.sql.Timestamp
import java.util.UUID

abstract class Entry(
    open var id: UUID = UUID.randomUUID(),
    open var raceEventorRef: String,
    open var classEventorRef: String,
    open var bib: String?,
    open var status: EntryStatus,
    open var startTime: Timestamp?,
    open var finishTime: Timestamp?,
    open var result: Result?
)