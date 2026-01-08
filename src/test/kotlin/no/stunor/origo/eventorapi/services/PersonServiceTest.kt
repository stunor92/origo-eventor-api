package no.stunor.origo.eventorapi.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.MembershipRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.UserPersonRepository
import no.stunor.origo.eventorapi.exception.EventorAuthException
import no.stunor.origo.eventorapi.exception.EventorConnectionException
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.services.converter.PersonConverter
import no.stunor.origo.eventorapi.testdata.EventorFactory
import no.stunor.origo.eventorapi.testdata.PersonFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.*
import org.iof.eventor.Person as EventorPerson

class PersonServiceTest {
    private lateinit var eventorRepository: EventorRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var membershipRepository: MembershipRepository
    private lateinit var userPersonRepository: UserPersonRepository
    private lateinit var eventorService: EventorService
    private lateinit var personConverter: PersonConverter
    private lateinit var personService: PersonService

    @BeforeEach
    fun setup() {
        eventorRepository = mockk()
        personRepository = mockk()
        membershipRepository = mockk()
        userPersonRepository = mockk()
        eventorService = mockk()
        personConverter = mockk()
        
        personService = PersonService()
        
        // Use reflection to inject mocks
        PersonService::class.java.getDeclaredField("eventorRepository").apply {
            isAccessible = true
            set(personService, eventorRepository)
        }
        PersonService::class.java.getDeclaredField("personRepository").apply {
            isAccessible = true
            set(personService, personRepository)
        }
        PersonService::class.java.getDeclaredField("membershipRepository").apply {
            isAccessible = true
            set(personService, membershipRepository)
        }
        PersonService::class.java.getDeclaredField("userPersonRepository").apply {
            isAccessible = true
            set(personService, userPersonRepository)
        }
        PersonService::class.java.getDeclaredField("eventorService").apply {
            isAccessible = true
            set(personService, eventorService)
        }
        PersonService::class.java.getDeclaredField("personConverter").apply {
            isAccessible = true
            set(personService, personConverter)
        }
    }

    @Test
    fun `authenticate should successfully authenticate and save new person`() {
        // Given
        val eventorId = "NOR"
        val username = "testuser"
        val password = "testpass"
        val userId = UUID.randomUUID()
        val eventor = EventorFactory.createEventorNorway()
        val eventorPerson = mockk<EventorPerson>()
        val convertedPerson = PersonFactory.createTestPerson()
        
        every { eventorRepository.findById(eventorId) } returns eventor
        every { eventorService.authenticatePerson(eventor, username, password) } returns eventorPerson
        every { personConverter.convertPerson(eventorPerson, eventor) } returns convertedPerson
        every { personRepository.findByEventorIdAndEventorRef(eventorId, convertedPerson.eventorRef) } returns null
        every { personRepository.save(any()) } returns convertedPerson

        // When
        val result = personService.authenticate(eventorId, username, password, userId)

        // Then
        assertNotNull(result)
        assertEquals(convertedPerson.eventorRef, result.eventorRef)
        verify { eventorService.authenticatePerson(eventor, username, password) }
        verify { personRepository.save(any()) }
    }

    @Test
    fun `authenticate should update existing person and clear old memberships`() {
        // Given
        val eventorId = "NOR"
        val username = "testuser"
        val password = "testpass"
        val userId = UUID.randomUUID()
        val eventor = EventorFactory.createEventorNorway()
        val eventorPerson = mockk<EventorPerson>()
        val convertedPerson = PersonFactory.createTestPerson()
        val existingPerson = PersonFactory.createTestPerson()
        
        every { eventorRepository.findById(eventorId) } returns eventor
        every { eventorService.authenticatePerson(eventor, username, password) } returns eventorPerson
        every { personConverter.convertPerson(eventorPerson, eventor) } returns convertedPerson
        every { personRepository.findByEventorIdAndEventorRef(eventorId, convertedPerson.eventorRef) } returns existingPerson
        every { membershipRepository.deleteByPersonId(existingPerson.id) } returns Unit
        every { personRepository.save(any()) } returns convertedPerson

        // When
        val result = personService.authenticate(eventorId, username, password, userId)

        // Then
        assertNotNull(result)
        assertEquals(existingPerson.id, result.id)
        verify { membershipRepository.deleteByPersonId(existingPerson.id) }
        verify { personRepository.save(any()) }
    }

    @Test
    fun `authenticate should throw EventorNotFoundException when eventor not found`() {
        // Given
        val eventorId = "INVALID"
        val username = "testuser"
        val password = "testpass"
        val userId = UUID.randomUUID()
        
        every { eventorRepository.findById(eventorId) } returns null

        // When & Then
        assertThrows<EventorNotFoundException> {
            personService.authenticate(eventorId, username, password, userId)
        }
    }

    @Test
    fun `authenticate should throw EventorAuthException when credentials are invalid`() {
        // Given
        val eventorId = "NOR"
        val username = "testuser"
        val password = "wrongpass"
        val userId = UUID.randomUUID()
        val eventor = EventorFactory.createEventorNorway()
        
        every { eventorRepository.findById(eventorId) } returns eventor
        every { eventorService.authenticatePerson(eventor, username, password) } returns null

        // When & Then
        assertThrows<EventorAuthException> {
            personService.authenticate(eventorId, username, password, userId)
        }
    }

    @Test
    fun `authenticate should throw EventorAuthException on 401 HTTP error`() {
        // Given
        val eventorId = "NOR"
        val username = "testuser"
        val password = "wrongpass"
        val userId = UUID.randomUUID()
        val eventor = EventorFactory.createEventorNorway()
        val httpException = mockk<HttpClientErrorException>()
        
        every { eventorRepository.findById(eventorId) } returns eventor
        every { httpException.statusCode } returns HttpStatus.UNAUTHORIZED
        every { eventorService.authenticatePerson(eventor, username, password) } throws httpException

        // When & Then
        assertThrows<EventorAuthException> {
            personService.authenticate(eventorId, username, password, userId)
        }
    }

    @Test
    fun `authenticate should throw EventorConnectionException on other HTTP errors`() {
        // Given
        val eventorId = "NOR"
        val username = "testuser"
        val password = "testpass"
        val userId = UUID.randomUUID()
        val eventor = EventorFactory.createEventorNorway()
        val httpException = mockk<HttpClientErrorException>()
        
        every { eventorRepository.findById(eventorId) } returns eventor
        every { httpException.statusCode } returns HttpStatus.INTERNAL_SERVER_ERROR
        every { eventorService.authenticatePerson(eventor, username, password) } throws httpException

        // When & Then
        assertThrows<EventorConnectionException> {
            personService.authenticate(eventorId, username, password, userId)
        }
    }

}
