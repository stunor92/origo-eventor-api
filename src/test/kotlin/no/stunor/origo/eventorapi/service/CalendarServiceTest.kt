package no.stunor.origo.eventorapi.service

import io.mockk.every
import io.mockk.mockk
import jakarta.xml.bind.JAXBContext
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.services.CalendarService
import no.stunor.origo.eventorapi.testdata.EventorFactory
import no.stunor.origo.eventorapi.testdata.OrganisationFactory
import no.stunor.origo.eventorapi.testdata.PersonFactory
import org.iof.eventor.EntryList
import org.iof.eventor.EventClassList
import org.iof.eventor.ResultListList
import org.iof.eventor.StartListList
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class CalendarServiceTest {
    var eventorRepository = mockk<EventorRepository>()
    var personRepository = mockk<PersonRepository>()
    var organisationRepository = mockk<OrganisationRepository>()
    var regionRepository = mockk<RegionRepository>()
    var eventorService = mockk<EventorService>()
    val calendarService = CalendarService(
        personRepository = personRepository,
        eventorRepository = eventorRepository,
        organisationRepository = organisationRepository,
        regionRepository = regionRepository,
        eventorService = eventorService
    )

    val entryList = JAXBContext.newInstance(EntryList::class.java).createUnmarshaller().unmarshal(File("src/test/resources/calendarService/OrganisationEntries.xml")) as EntryList
    val eventClass = JAXBContext.newInstance(EventClassList::class.java).createUnmarshaller().unmarshal(File("src/test/resources/calendarService/EventClass.xml")) as EventClassList
    val startList = JAXBContext.newInstance(StartListList::class.java).createUnmarshaller().unmarshal(File("src/test/resources/calendarService/PersonalStart.xml")) as StartListList
    val resultList = JAXBContext.newInstance(ResultListList::class.java).createUnmarshaller().unmarshal(File("src/test/resources/calendarService/PersonalResult.xml")) as ResultListList

    @Test
    fun testGetRacesForUser() {
        every { personRepository.findAllByUsers(any()) } returns listOf(PersonFactory.createTestPerson())
        every { eventorRepository.findById(any()) } returns EventorFactory.createEventorNorway()
        every { eventorService.getGetOrganisationEntries(any(), any(), any(), any(), any()) } returns entryList
        every { eventorService.getEventClasses(any(), any()) } returns eventClass
        every { eventorService.getGetPersonalStarts(any(), any(), any(), any(), any()) } returns startList
        every { eventorService.getGetPersonalResults(any(), any(), any(), any(), any()) } returns resultList
        every { organisationRepository.findByEventorRefAndEventorId(any(),any()) } returns OrganisationFactory.createTestOrganisation()
        val result = calendarService.getEventList(UUID.randomUUID())

        assert(result.data.size == 27)
        val dm = result.data.first { it.eventId == "17544" }
        assert(dm.userEntries.size == 1)
        assert(dm.organisationEntries.first { it.organisation.eventorRef == "141" }.entries == 95)
    }
}