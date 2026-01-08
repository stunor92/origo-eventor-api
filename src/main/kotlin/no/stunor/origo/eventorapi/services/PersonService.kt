package no.stunor.origo.eventorapi.services

import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.MembershipRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.UserPersonRepository
import no.stunor.origo.eventorapi.exception.EventorAuthException
import no.stunor.origo.eventorapi.exception.EventorConnectionException
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import no.stunor.origo.eventorapi.services.converter.PersonConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.*

@Service
class PersonService {

    @Autowired
    private lateinit var eventorRepository: EventorRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var userPersonRepository: UserPersonRepository

    @Autowired
    private lateinit var eventorService: EventorService

    @Autowired
    private lateinit var personConverter: PersonConverter

    fun authenticate(eventorId: String, username: String, password: String, userId: UUID): Person {
        try {
            val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()

            val eventorPerson = eventorService.authenticatePerson(eventor, username, password)?: throw EventorAuthException()

            val person = personConverter.convertPerson(eventorPerson, eventor)
            val existingPerson = personRepository.findByEventorIdAndEventorRef(eventorId, person.eventorRef)
            if (existingPerson != null) {
                person.id = existingPerson.id
                membershipRepository.deleteByPersonId(existingPerson.id)
            }

            val userPerson = UserPerson(id = UserPersonKey(userId = userId, personId = person.id), person = person)
            person.users.add(userPerson)
            personRepository.save(person)
            return person
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == 401) {
                throw EventorAuthException()
            }
            throw EventorConnectionException()
        }
    }
}
