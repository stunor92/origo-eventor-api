package no.stunor.origo.eventorapi.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.xml.bind.JAXBContext
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventClassRepository
import no.stunor.origo.eventorapi.data.EventRepository
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.FeeRepository
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.event.entry.EntryStatus
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.services.converter.*
import no.stunor.origo.eventorapi.testdata.EventorFactory
import org.iof.eventor.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.*

class EventServiceTest {
    private lateinit var eventorRepository: EventorRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var eventConverter: EventConverter
    private lateinit var feeRepository: FeeRepository
    private lateinit var eventClassRepository: EventClassRepository
    private lateinit var eventorService: EventorService
    private lateinit var organisationConverter: OrganisationConverter
    private lateinit var entryListConverter: EntryListConverter
    private lateinit var startListConverter: StartListConverter
    private lateinit var resultListConverter: ResultListConverter
    private lateinit var eventService: EventService

    // Load test data
    private val oneDayEvent = JAXBContext.newInstance(Event::class.java)
        .createUnmarshaller()
        .unmarshal(File("src/test/resources/eventorResponse/eventService/oneDayEvent/Event.xml")) as Event

    private val oneDayEventClasses = JAXBContext.newInstance(EventClassList::class.java)
        .createUnmarshaller()
        .unmarshal(File("src/test/resources/eventorResponse/eventService/oneDayEvent/EventClassList.xml")) as EventClassList

    private val oneDayDocuments = JAXBContext.newInstance(DocumentList::class.java)
        .createUnmarshaller()
        .unmarshal(File("src/test/resources/eventorResponse/eventService/oneDayEvent/DocumentList.xml")) as DocumentList

    @BeforeEach
    fun setup() {
        eventorRepository    = mockk()
        eventRepository      = mockk()
        eventConverter       = mockk()
        eventClassRepository = mockk()
        feeRepository        = mockk()
        eventorService       = mockk()
        organisationConverter = mockk()
        entryListConverter   = mockk()
        startListConverter   = mockk()
        resultListConverter  = mockk()

        eventService = EventService(
            eventorRepository    = eventorRepository,
            eventRepository      = eventRepository,
            eventConverter       = eventConverter,
            feeRepository        = feeRepository,
            eventClassRepository = eventClassRepository,
            eventorService       = eventorService,
            organisationConverter = organisationConverter,
            entryListConverter   = entryListConverter,
            startListConverter   = startListConverter,
            resultListConverter  = resultListConverter
        )
    }

    @Test
    fun `getEvent should retrieve and convert one-day event successfully`() {
        val eventorId = "NOR"
        val eventId = "17535"
        val eventor = EventorFactory.createEventorNorway()
        val convertedEvent = mockk<no.stunor.origo.eventorapi.model.event.Event>()

        every { eventorRepository.findById(eventorId) } returns eventor
        every { eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns oneDayEvent
        every { eventorService.getEventClasses(eventor, eventId) } returns oneDayEventClasses
        every { eventorService.getEventDocuments(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns oneDayDocuments
        every { eventRepository.findByEventorIdAndEventorRef(eventorId, eventId) } returns null
        every { organisationConverter.convertOrganisations(any(), any()) } returns mutableListOf()
        every { eventConverter.convertEvent(any(), any(), any(), any(), any(), any()) } returns convertedEvent
        every { eventRepository.save(any()) } returns convertedEvent
        every { convertedEvent.eventorRef } returns eventId
        every { convertedEvent.id } returns UUID.randomUUID()
        every { eventorService.getEventEntryFees(eventor, eventId) } returns null
        every { feeRepository.findAllByEventId(any()) } returns emptyList()
        every { feeRepository.saveAll(any<List<no.stunor.origo.eventorapi.model.event.Fee>>()) } returns emptyList()
        every { eventClassRepository.findByEventId(any()) } returns emptyList()

        val result = eventService.getEvent(eventorId, eventId)

        assertNotNull(result)
        verify { eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventId) }
        verify { eventorService.getEventClasses(eventor, eventId) }
        verify { eventRepository.save(any()) }
    }

    @Test
    fun `getEvent should throw EventorNotFoundException when eventor not found`() {
        every { eventorRepository.findById("INVALID") } returns null

        assertThrows<EventorNotFoundException> {
            eventService.getEvent("INVALID", "17535")
        }
    }

    @Test
    fun `getEntryList should return result entries when available`() {
        val eventorId = "NOR"
        val eventId = "17535"
        val eventor = EventorFactory.createEventorNorway()
        val resultList = mockk<ResultList>()
        val entryList = mockk<EntryList>()

        val mockEntries = listOf(
            PersonEntry(personEventorRef = "person1", classEventorRef = "class1", raceEventorRef = "race1", status = EntryStatus.Finished),
            PersonEntry(personEventorRef = "person2", classEventorRef = "class2", raceEventorRef = "race2", status = EntryStatus.Started)
        )

        every { eventorRepository.findById(eventorId) } returns eventor
        every { eventorService.getEventEntryList(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns entryList
        every { entryList.entry } returns emptyList()
        every { eventorService.getEventStartList(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns null
        every { eventorService.getEventResultList(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns resultList
        every { resultListConverter.convertEventResultList(eventor, resultList) } returns mockEntries

        val result = eventService.getEntryList(eventorId, eventId)

        assertEquals(2, result.size)
        verify { eventorService.getEventResultList(eventor.baseUrl, eventor.eventorApiKey, eventId) }
    }

    @Test
    fun `getEntryList should throw EventorNotFoundException when eventor not found`() {
        every { eventorRepository.findById("INVALID") } returns null

        assertThrows<EventorNotFoundException> {
            eventService.getEntryList("INVALID", "17535")
        }
    }
}
