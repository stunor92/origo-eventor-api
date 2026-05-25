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
        eventorRepository    = mockk()
        personRepository     = mockk()
        membershipRepository = mockk()
        userPersonRepository = mockk()
        eventorService       = mockk()
        personConverter      = mockk()

        personService = PersonService(
            eventorRepository    = eventorRepository,
            personRepository     = personRepository,
            membershipRepository = membershipRepository,
            userPersonRepository = userPersonRepository,
            eventorService       = eventorService,
            personConverter      = personConverter
        )
    }

    @Test
    fun `authenticate should successfully authenticate and save new person`() {
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

        val result = personService.authenticate(eventorId, username, password, userId)

        assertNotNull(result)
        assertEquals(convertedPerson.eventorRef, result.eventorRef)
        verify { eventorService.authenticatePerson(eventor, username, password) }
        verify { personRepository.save(any()) }
    }

    @Test
    fun `authenticate should update existing person and clear old memberships`() {
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

        val result = personService.authenticate(eventorId, username, password, userId)

        assertNotNull(result)
        assertEquals(existingPerson.id, result.id)
        verify { membershipRepository.deleteByPersonId(existingPerson.id) }
        verify { personRepository.save(any()) }
    }

    @Test
    fun `authenticate should throw EventorNotFoundException when eventor not found`() {
        every { eventorRepository.findById("INVALID") } returns null

        assertThrows<EventorNotFoundException> {
            personService.authenticate("INVALID", "testuser", "testpass", UUID.randomUUID())
        }
    }

    @Test
    fun `authenticate should throw EventorAuthException when credentials are invalid`() {
        val eventor = EventorFactory.createEventorNorway()
        every { eventorRepository.findById("NOR") } returns eventor
        every { eventorService.authenticatePerson(eventor, "testuser", "wrongpass") } throws EventorAuthException()

        assertThrows<EventorAuthException> {
            personService.authenticate("NOR", "testuser", "wrongpass", UUID.randomUUID())
        }
    }

    @Test
    fun `authenticate should throw EventorConnectionException on connection error`() {
        val eventor = EventorFactory.createEventorNorway()
        every { eventorRepository.findById("NOR") } returns eventor
        every { eventorService.authenticatePerson(eventor, any(), any()) } throws EventorConnectionException()

        assertThrows<EventorConnectionException> {
            personService.authenticate("NOR", "testuser", "testpass", UUID.randomUUID())
        }
    }
}
