package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID
import javax.sql.DataSource

internal object PersonTable : Table("person") {
    val id = uuid("id")
    override val primaryKey = PrimaryKey(id)
    val eventorId = text("eventor_id")
    val eventorRef = text("eventor_ref")
    val familyName = text("family_name")
    val givenName = text("given_name")
    val birthYear = integer("birth_year")
    val nationality = text("nationality")
    val gender = text("gender")
    val mobilePhone = text("mobile_phone").nullable()
    val email = text("email").nullable()
    val lastUpdated = timestamp("last_updated")
}


open class PersonRepository(
    dataSource: DataSource,
    private val membershipRepository: MembershipRepository,
    private val userPersonRepository: UserPersonRepository
) {
    
    private val database = Database.connect(dataSource)

    private fun toPersonSimple(row: ResultRow): Person {
        return Person(
            id = row[PersonTable.id],
            eventorId = row[PersonTable.eventorId],
            eventorRef = row[PersonTable.eventorRef],
            name = PersonName(
                family = row[PersonTable.familyName],
                given = row[PersonTable.givenName]
            ),
            birthYear = row[PersonTable.birthYear],
            nationality = row[PersonTable.nationality],
            gender = Gender.valueOf(row[PersonTable.gender]),
            mobilePhone = row[PersonTable.mobilePhone],
            email = row[PersonTable.email],
            memberships = mutableListOf(), // Loaded separately
            users = mutableListOf(), // Loaded separately
            lastUpdated = row[PersonTable.lastUpdated]
        )
    }

    private fun toPerson(row: ResultRow): Person {
        val id = row[PersonTable.id]
        return Person(
            id = id,
            eventorId = row[PersonTable.eventorId],
            eventorRef = row[PersonTable.eventorRef],
            name = PersonName(
                family = row[PersonTable.familyName],
                given = row[PersonTable.givenName]
            ),
            birthYear = row[PersonTable.birthYear],
            nationality = row[PersonTable.nationality],
            gender = Gender.valueOf(row[PersonTable.gender]),
            mobilePhone = row[PersonTable.mobilePhone],
            email = row[PersonTable.email],
            memberships = membershipRepository.findAllByPersonId(id).toMutableList(),
            users = userPersonRepository.findAllByPersonId(id).toMutableList(),
            lastUpdated = row[PersonTable.lastUpdated]
        )
    }
    
    /**
     * Helper method to batch load memberships for multiple persons
     */
    private fun loadMembershipsForPersons(persons: List<Person>) {
        if (persons.isEmpty()) return

        val personIds = persons.mapNotNull { it.id }
        if (personIds.isEmpty()) return

        transaction(database) {
            val allMemberships = MembershipTable
                .selectAll()
                .where { MembershipTable.personId inList personIds }
                .map { row ->
                    val personId = row[MembershipTable.personId]
                    val organisationId = row[MembershipTable.organisationId]
                    val organisation = membershipRepository.getOrganisationById(organisationId)

                    Membership(
                        id = MembershipKey(personId = personId, organisationId = organisationId),
                        person = null,
                        organisation = organisation,
                        type = MembershipType.valueOf(row[MembershipTable.type])
                    )
                }

            // Group memberships by person_id
            val membershipsByPersonId = allMemberships.groupBy { it.id.personId }

            // Assign memberships to persons
            persons.forEach { person ->
                person.memberships = membershipsByPersonId[person.id]?.toMutableList() ?: mutableListOf()
            }
        }
    }

    /**
     * Helper method to batch load user associations for multiple persons
     */
    private fun loadUsersForPersons(persons: List<Person>) {
        if (persons.isEmpty()) return

        val personIds = persons.mapNotNull { it.id }
        if (personIds.isEmpty()) return

        transaction(database) {
            val allUserPersons = UserPersonTable
                .selectAll()
                .where { UserPersonTable.personId inList personIds }
                .map { row ->
                    UserPerson(
                        id = UserPersonKey(
                            userId = row[UserPersonTable.userId],
                            personId = row[UserPersonTable.personId]
                        ),
                        person = null
                    )
                }

            // Group user associations by person_id
            val usersByPersonId = allUserPersons.groupBy { it.id.personId }

            // Assign user associations to persons
            persons.forEach { person ->
                person.users = usersByPersonId[person.id]?.toMutableList() ?: mutableListOf()
            }
        }
    }

    open fun findByEventorIdAndEventorRef(eventorId: String, eventorRef: String): Person? {
        return transaction(database) {
            PersonTable
                .selectAll()
                .where { (PersonTable.eventorId eq eventorId) and (PersonTable.eventorRef eq eventorRef) }
                .limit(1)
                .map(::toPerson)
                .singleOrNull()
        }
    }
    open fun findAllByUsersAndEventorId(userId: UUID, eventorId: String): List<Person> {
        val persons = transaction(database) {
            PersonTable.innerJoin(UserPersonTable)
                .selectAll()
                .where { (UserPersonTable.userId eq userId) and (PersonTable.eventorId eq eventorId) }
                .map(::toPersonSimple)
        }

        // Batch load memberships and user associations
        loadMembershipsForPersons(persons)
        loadUsersForPersons(persons)

        return persons
    }
    
    open fun save(person: Person): Person {
        transaction(database) {
            if (person.id == null) {
                person.id = UUID.randomUUID()
            }

            PersonTable.upsert {
                it[PersonTable.id] = person.id!!
                it[PersonTable.eventorId] = person.eventorId
                it[PersonTable.eventorRef] = person.eventorRef
                it[PersonTable.familyName] = person.name.family
                it[PersonTable.givenName] = person.name.given
                it[PersonTable.birthYear] = person.birthYear
                it[PersonTable.nationality] = person.nationality
                it[PersonTable.gender] = person.gender.name
                it[PersonTable.mobilePhone] = person.mobilePhone
                it[PersonTable.email] = person.email
                it[PersonTable.lastUpdated] = person.lastUpdated
            }
        }
        
        // Save memberships
        person.memberships.forEach { membership ->
            membership.person = person
            membership.id.personId = person.id
            membershipRepository.save(membership)
        }
        
        // Save user associations
        person.users.forEach { userPerson ->
            userPerson.person = person
            userPerson.id.personId = person.id
            userPersonRepository.save(userPerson)
        }
        
        return person
    }
}