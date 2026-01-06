package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.Gender
import no.stunor.origo.eventorapi.model.person.Membership
import no.stunor.origo.eventorapi.model.person.MembershipKey
import no.stunor.origo.eventorapi.model.person.MembershipType
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.model.person.PersonName
import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.*

@Repository
open class PersonRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val membershipRepository: MembershipRepository,
    private val userPersonRepository: UserPersonRepository
) {
    
    // Simple row mapper without nested queries (for batch loading)
    private val simpleRowMapper = RowMapper { rs: ResultSet, _: Int ->
        Person(
            id = rs.getObject("id", UUID::class.java),
            eventorId = rs.getString("eventor_id"),
            eventorRef = rs.getString("eventor_ref"),
            name = PersonName(
                family = rs.getString("family_name") ?: "",
                given = rs.getString("given_name") ?: ""
            ),
            birthYear = rs.getInt("birth_year"),
            nationality = rs.getString("nationality") ?: "",
            gender = Gender.valueOf(rs.getString("gender") ?: "Other"),
            mobilePhone = rs.getString("mobile_phone"),
            email = rs.getString("email"),
            memberships = mutableListOf(), // Loaded separately
            users = mutableListOf(), // Loaded separately
            lastUpdated = rs.getTimestamp("last_updated")?.toInstant() ?: Instant.now()
        )
    }

    // Row mapper with nested queries (only for single person lookups)
    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        val id = rs.getObject("id", UUID::class.java)
        Person(
            id = id,
            eventorId = rs.getString("eventor_id"),
            eventorRef = rs.getString("eventor_ref"),
            name = PersonName(
                family = rs.getString("family_name") ?: "",
                given = rs.getString("given_name") ?: ""
            ),
            birthYear = rs.getInt("birth_year"),
            nationality = rs.getString("nationality") ?: "",
            gender = Gender.valueOf(rs.getString("gender") ?: "Other"),
            mobilePhone = rs.getString("mobile_phone"),
            email = rs.getString("email"),
            memberships = membershipRepository.findAllByPersonId(id).toMutableList(),
            users = userPersonRepository.findAllByPersonId(id).toMutableList(),
            lastUpdated = rs.getTimestamp("last_updated")?.toInstant() ?: Instant.now()
        )
    }
    
    /**
     * Helper method to batch load memberships for multiple persons.
     * This method modifies the persons parameter in-place by populating the memberships field.
     *
     * @param persons List of persons to load memberships for. The memberships field will be populated for each person.
     */
    private fun loadMembershipsForPersons(persons: List<Person>) {
        if (persons.isEmpty()) return

        val personIds = persons.mapNotNull { it.id }
        if (personIds.isEmpty()) return

        // Batch size to avoid overly large IN clauses / parameter lists
        val batchSize = 1000
        val sql = "SELECT * FROM membership WHERE person_id IN (:personIds)"

        // Accumulate memberships across all batches
        val allMemberships = mutableListOf<Membership>()

        personIds.chunked(batchSize).forEach { batch ->
            val params = mapOf("personIds" to batch)

            val batchMemberships = namedParameterJdbcTemplate.query(sql, params) { rs: ResultSet, _: Int ->
                val personId = rs.getObject("person_id", UUID::class.java)
                val organisationId = rs.getObject("organisation_id", UUID::class.java)

                Membership(
                    id = MembershipKey(personId = personId, organisationId = organisationId),
                    person = null,
                    organisation = null, // Organisations loaded in batch below
                    type = MembershipType.valueOf(rs.getString("type"))
                )
            }

            allMemberships.addAll(batchMemberships)
        }

        // Batch-load organisations for all memberships to avoid N+1 queries
        val organisationIds = allMemberships.mapNotNull { it.id.organisationId }.distinct()
        val organisationsById =
            if (organisationIds.isEmpty()) {
                emptyMap()
            } else {
                organisationIds.chunked(batchSize).flatMap { batch ->
                    batch.mapNotNull { organisationId ->
                        membershipRepository.getOrganisationById(organisationId)?.let { organisationId to it }
                    }
                }.toMap()
            }

        val membershipsWithOrganisations = allMemberships.map { membership ->
            val organisation = membership.id.organisationId?.let { organisationsById[it] }
            membership.copy(organisation = organisation)
        }

        // Group memberships by person_id
        val membershipsByPersonId = membershipsWithOrganisations.groupBy { it.id.personId }

        // Assign memberships to persons
        persons.forEach { person ->
            person.memberships = membershipsByPersonId[person.id]?.toMutableList() ?: mutableListOf()
        }
    }

    /**
     * Helper method to batch load user associations for multiple persons.
     * This method modifies the persons parameter in-place by populating the users field.
     *
     * @param persons List of persons to load user associations for. The users field will be populated for each person.
     */
    private fun loadUsersForPersons(persons: List<Person>) {
        if (persons.isEmpty()) return

        val personIds = persons.mapNotNull { it.id }
        if (personIds.isEmpty()) return

        // Batch the IN-clause to avoid too many parameters in a single query
        val batchSize = 1000
        val sql = "SELECT * FROM user_person WHERE person_id IN (:personIds)"
        val allUserPersons = mutableListOf<UserPerson>()

        for (chunk in personIds.chunked(batchSize)) {
            val params = mapOf("personIds" to chunk)
            val batchResult = namedParameterJdbcTemplate.query(sql, params) { rs: ResultSet, _: Int ->
                UserPerson(
                    id = UserPersonKey(
                        userId = rs.getObject("user_id", UUID::class.java),
                        personId = rs.getObject("person_id", UUID::class.java)
                    ),
                    person = null
                )
            }
            allUserPersons.addAll(batchResult)
        }

        // Group user associations by person_id
        val usersByPersonId = allUserPersons.groupBy { it.id.personId }

        // Assign user associations to persons
        persons.forEach { person ->
            person.users = usersByPersonId[person.id]?.toMutableList() ?: mutableListOf()
        }
    }

    open fun findByEventorIdAndEventorRef(eventorId: String, eventorRef: String): Person? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM person WHERE eventor_id = ? AND eventor_ref = ?",
                rowMapper,
                eventorId, eventorRef
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun findAllByUsers(userId: UUID): List<Person> {
        val persons = jdbcTemplate.query(
            """
            SELECT p.* FROM person p
            INNER JOIN user_person up ON p.id = up.person_id
            WHERE up.user_id = ?
            """,
            simpleRowMapper, // Use simpleRowMapper instead of rowMapper
            userId
        )

        // Batch load memberships and user associations
        loadMembershipsForPersons(persons)
        loadUsersForPersons(persons)

        return persons
    }
    
    open fun findAllByUsersAndEventorId(userId: UUID, eventorId: String): List<Person> {
        val persons = jdbcTemplate.query(
            """
            SELECT p.* FROM person p
            INNER JOIN user_person up ON p.id = up.person_id
            WHERE up.user_id = ? AND p.eventor_id = ?
            """,
            simpleRowMapper, // Use simpleRowMapper instead of rowMapper
            userId, eventorId
        )

        // Batch load memberships and user associations
        loadMembershipsForPersons(persons)
        loadUsersForPersons(persons)

        return persons
    }
    
    open fun save(person: Person): Person {
        if (person.id == null) {
            person.id = UUID.randomUUID()
            jdbcTemplate.update(
                """
                INSERT INTO person (id, eventor_id, eventor_ref, family_name, given_name, 
                    birth_year, nationality, gender, mobile_phone, email, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::gender, ?, ?, ?)
                """,
                person.id, person.eventorId, person.eventorRef, person.name.family, person.name.given,
                person.birthYear, person.nationality, person.gender.name, person.mobilePhone, 
                person.email, java.sql.Timestamp.from(person.lastUpdated)
            )
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO person (id, eventor_id, eventor_ref, family_name, given_name, 
                    birth_year, nationality, gender, mobile_phone, email, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::gender, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    eventor_id = EXCLUDED.eventor_id,
                    eventor_ref = EXCLUDED.eventor_ref,
                    family_name = EXCLUDED.family_name,
                    given_name = EXCLUDED.given_name,
                    birth_year = EXCLUDED.birth_year,
                    nationality = EXCLUDED.nationality,
                    gender = EXCLUDED.gender,
                    mobile_phone = EXCLUDED.mobile_phone,
                    email = EXCLUDED.email,
                    last_updated = EXCLUDED.last_updated
                """,
                person.id, person.eventorId, person.eventorRef, person.name.family, person.name.given,
                person.birthYear, person.nationality, person.gender.name, person.mobilePhone, 
                person.email, java.sql.Timestamp.from(person.lastUpdated)
            )
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