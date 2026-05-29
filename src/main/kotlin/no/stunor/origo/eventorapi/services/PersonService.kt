package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.util.UUID

class PersonService(
    private val eventorRepository: EventorRepository,
    private val personRepository: PersonRepository,
    private val membershipRepository: MembershipRepository,
    private val userPersonRepository: UserPersonRepository,
    private val eventorService: EventorService,
    private val personConverter: PersonConverter
) {

    suspend fun authenticate(eventorId: String, username: String, password: String, userId: UUID): Person {
        val eventor = withContext(Dispatchers.IO) {
            eventorRepository.findById(eventorId)
        } ?: throw EventorNotFoundException()

        val eventorPerson = try {
            eventorService.authenticatePerson(eventor, username, password)
        } catch (e: EventorAuthException) {
            throw e
        } catch (e: EventorConnectionException) {
            throw e
        } catch (e: Exception) {
            throw EventorConnectionException()
        }

        val person = personConverter.convertPerson(eventorPerson, eventor)
        val existingPerson = withContext(Dispatchers.IO) {
            personRepository.findByEventorIdAndEventorRef(eventorId, person.eventorRef)
        }
        if (existingPerson != null) {
            person.id = existingPerson.id
            withContext(Dispatchers.IO) { membershipRepository.deleteByPersonId(existingPerson.id) }
        }

        val userPerson = UserPerson(id = UserPersonKey(userId = userId, personId = person.id), person = person)
        person.users.add(userPerson)
        withContext(Dispatchers.IO) { personRepository.save(person) }
        return person
    }
}
