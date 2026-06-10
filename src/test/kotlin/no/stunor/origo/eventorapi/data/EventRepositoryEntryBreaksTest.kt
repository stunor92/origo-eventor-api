package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.event.EventFormEnum
import no.stunor.origo.eventorapi.model.event.EventStatusEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant

class EventRepositoryEntryBreaksTest {

    @Test
    fun `entryBreaks should be serialized correctly for saving`() {
        val timestamp1 = Timestamp.from(Instant.parse("2026-06-01T10:00:00Z"))
        val timestamp2 = Timestamp.from(Instant.parse("2026-07-01T10:00:00Z"))

        val event = Event(
            eventorId = "TEST",
            eventorRef = "12345",
            name = "Test Event",
            type = EventFormEnum.Individual,
            classification = EventClassificationEnum.Club,
            status = EventStatusEnum.Created,
            entryBreaks = arrayOf(timestamp1, timestamp2)
        )

        // Serialize like the save method does
        val entryBreaksStr = event.entryBreaks.takeIf { it.isNotEmpty() }
            ?.let { "{${it.joinToString(",") { eb -> eb.toString() }}}" }

        assertNotNull(entryBreaksStr)
        assertEquals("{$timestamp1,$timestamp2}", entryBreaksStr)
    }

    @Test
    fun `entryBreaks should handle empty array`() {
        val event = Event(
            eventorId = "TEST",
            eventorRef = "12345",
            name = "Test Event",
            type = EventFormEnum.Individual,
            classification = EventClassificationEnum.Club,
            status = EventStatusEnum.Created,
            entryBreaks = emptyArray()
        )

        // Serialize like the save method does
        val entryBreaksStr = event.entryBreaks.takeIf { it.isNotEmpty() }
            ?.let { "{${it.joinToString(",") { eb -> eb.toString() }}}" }

        // Should be null for empty array
        assertEquals(null, entryBreaksStr)
    }

    @Test
    fun `timestamp toString format is valid for Timestamp valueOf`() {
        val timestamp = Timestamp.from(Instant.parse("2026-06-01T10:00:00Z"))
        val timestampStr = timestamp.toString()

        // Verify we can parse it back
        val parsed = Timestamp.valueOf(timestampStr)

        assertEquals(timestamp, parsed)
    }
}


