package no.stunor.origo.eventorapi.controller

import com.ninjasquad.springmockk.MockkBean
import no.stunor.origo.eventorapi.services.PersonService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@Disabled("MockMvc not available in Spring Boot 4.0.0 - needs refactoring")
@SpringBootTest
@ActiveProfiles("test")
class PersonControllerIntegrationTest {


    @MockkBean
    private lateinit var personService: PersonService

    @Test
    fun `authenticate should call service authenticate method and return OK`() {
        // TODO: Refactor to use RestClient or TestRestTemplate instead of MockMvc
        /*
        val eventorId = "NOR"
        val username = "testuser"
        val password = "testpass"
        val userId = "user123"
        val mockPerson = PersonFactory.createTestPerson()
        
        every { personService.authenticate(eventorId, username, password, userId) } returns mockPerson
        */
    }

    @Test
    fun `delete should call service delete method`() {
        // TODO: Refactor to use RestClient or TestRestTemplate instead of MockMvc
        /*
        val eventorId = "NOR"
        val personId = "123"
        val userId = "user123"
        
        justRun { personService.delete(eventorId, personId, userId) }
        */
    }
}
