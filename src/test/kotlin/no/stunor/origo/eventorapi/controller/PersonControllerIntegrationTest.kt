package no.stunor.origo.eventorapi.controller

import io.mockk.mockk
import no.stunor.origo.eventorapi.services.PersonService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Integration tests require Ktor testApplication setup – to be implemented")
class PersonControllerIntegrationTest {

    private val personService: PersonService = mockk()

    @Test
    fun `authenticate should call service authenticate method and return OK`() {
        // TODO: Implement with Ktor's testApplication { client.post(...) }
    }
}
