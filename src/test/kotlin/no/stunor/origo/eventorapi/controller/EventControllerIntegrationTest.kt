package no.stunor.origo.eventorapi.controller

import io.mockk.mockk
import no.stunor.origo.eventorapi.services.EventService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Integration tests require Ktor testApplication setup – to be implemented")
class EventControllerIntegrationTest {

    private val eventService: EventService = mockk()

    @Test
    fun `getEvent should return event when valid eventorId and eventId provided`() {
        // TODO: Implement with Ktor's testApplication { client.get(...) }
    }

    @Test
    fun `getEventEntryList should return entry list when valid parameters provided`() {
        // TODO: Implement with Ktor's testApplication { client.get(...) }
    }
}
