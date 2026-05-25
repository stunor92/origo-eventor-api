package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.person.*

import java.util.*


class PersonConverter(
    private val organisationRepository: OrganisationRepository,
) {

    fun convertPerson(eventorPerson: org.iof.eventor.Person, eventor: Eventor): Person {
        val person = Person(
            eventorId = eventor.id,
            eventorRef = eventorPerson.personId.content,
            name = convertPersonName(eventorPerson.personName),
            birthYear = eventorPerson.birthDate.date.content.substring(0, 4).toInt(),
            nationality = eventorPerson.nationality.country.alpha3.value,
            gender = convertGender(eventorPerson.sex),
            mobilePhone = ContactConverter.convertPhone(eventorPerson.tele),
            email = ContactConverter.convertEmail(eventorPerson.tele),
        )
        person.memberships  = convertMemberships(person,eventorPerson.role, eventor.id)
        return person
    }

    fun convertGender(sex: String?): Gender {
        if (sex == null) {
            return Gender.Other
        }
        return when (sex) {
            "M" -> Gender.Man
            "F" -> Gender.Woman
            else -> Gender.Other
        }
    }

    fun convertPersonName(personName: org.iof.eventor.PersonName): PersonName {
        val given = StringBuilder()
        for (i in personName.given.indices) {
            for (j in 1..personName.given.size) {
                if (personName.given[i].sequence.toInt() == j) {
                    if (given.toString() != "") {
                        given.append(" ")
                    }
                    given.append(personName.given[i].content)
                }
            }
        }
        return PersonName(personName.family.content, given.toString())
    }

    private fun convertMemberships(
        person: Person,
        roles: List<org.iof.eventor.Role>,
        eventorId: String
    ): MutableList<Membership> {
        val highestRole: MutableMap<String, Int> = HashMap()

        for (role in roles) {
            if (role.roleTypeId.content == "2") {
                role.roleTypeId.content = "10"
            }

            if (!highestRole.containsKey(role.organisationId.content)
                || role.roleTypeId.content.toInt() > highestRole.getValue(role.organisationId.content)
            ) {
                highestRole[role.organisationId.content] = role.roleTypeId.content.toInt()
            }
        }

        val memberships: MutableList<Membership> = mutableListOf()

        for (orgRef in highestRole.keys.stream().toList()) {
            val organisation = organisationRepository.findByEventorRefAndEventorId(orgRef, eventorId) ?: continue
            when {
                highestRole[orgRef] == 1 -> {
                    memberships.add(
                        Membership(
                            id = MembershipKey(organisationId = organisation.id, personId = person.id),
                            person = person,
                            organisation = organisation,
                            type = MembershipType.Member
                        )
                    )
                }

                highestRole[orgRef] == 3 || highestRole[orgRef] == 5 -> {
                    memberships.add(
                        Membership(
                            id = MembershipKey(organisationId = organisation.id, personId = person.id),
                            person = person,
                            organisation = organisation,
                            type = MembershipType.Organiser
                        )
                    )
                }

                highestRole[orgRef] == 10 -> {
                    memberships.add(
                        Membership(
                            id = MembershipKey(organisationId = organisation.id, personId = person.id),
                            person = person,
                            organisation = organisation,
                            type = MembershipType.Admin
                        )
                    )
                }
            }
        }


        return memberships
    }
}
