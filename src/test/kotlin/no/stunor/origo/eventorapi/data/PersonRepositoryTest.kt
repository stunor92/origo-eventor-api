package no.stunor.origo.eventorapi.data

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.organisation.OrganisationType
import no.stunor.origo.eventorapi.model.person.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.time.Instant
import java.util.*

class PersonRepositoryTest {
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var membershipRepository: MembershipRepository
    private lateinit var userPersonRepository: UserPersonRepository
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setup() {
        jdbcTemplate = mockk()
        namedParameterJdbcTemplate = mockk()
        membershipRepository = mockk()
        userPersonRepository = mockk()
        
        personRepository = PersonRepository(
            jdbcTemplate,
            namedParameterJdbcTemplate,
            membershipRepository,
            userPersonRepository
        )
    }

    @Test
    fun `loadMembershipsForPersons should handle empty person list`() {
        // Given
        val persons = emptyList<Person>()
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadMembershipsForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - no exception should be thrown and no database queries should be made
        verify(exactly = 0) { namedParameterJdbcTemplate.query(any<String>(), any<Map<String, Any>>(), any<RowMapper<Membership>>()) }
    }

    @Test
    fun `loadMembershipsForPersons should handle persons with null IDs`() {
        // Given
        val persons = listOf(
            Person(id = null, eventorId = "NOR", eventorRef = "123", name = PersonName("Test", "User"))
        )
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadMembershipsForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - no database queries should be made
        verify(exactly = 0) { namedParameterJdbcTemplate.query(any<String>(), any<Map<String, Any>>(), any<RowMapper<Membership>>()) }
    }

    @Test
    fun `loadMembershipsForPersons should correctly load and associate memberships`() {
        // Given
        val personId1 = UUID.randomUUID()
        val personId2 = UUID.randomUUID()
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        
        val person1 = Person(
            id = personId1,
            eventorId = "NOR",
            eventorRef = "123",
            name = PersonName("Person", "One")
        )
        val person2 = Person(
            id = personId2,
            eventorId = "NOR",
            eventorRef = "456",
            name = PersonName("Person", "Two")
        )
        val persons = listOf(person1, person2)
        
        val org1 = Organisation(
            id = orgId1,
            eventorId = "NOR",
            eventorRef = "ORG1",
            name = "Org 1",
            type = OrganisationType.Club,
            country = "NOR"
        )
        val org2 = Organisation(
            id = orgId2,
            eventorId = "NOR",
            eventorRef = "ORG2",
            name = "Org 2",
            type = OrganisationType.Club,
            country = "NOR"
        )
        
        // Mock the membership query
        every {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM membership WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<Membership>>()
            )
        } answers {
            val rowMapper = thirdArg<RowMapper<Membership>>()
            val rs1 = mockk<ResultSet>()
            val rs2 = mockk<ResultSet>()
            
            every { rs1.getObject("person_id", UUID::class.java) } returns personId1
            every { rs1.getObject("organisation_id", UUID::class.java) } returns orgId1
            every { rs1.getString("type") } returns "Member"
            
            every { rs2.getObject("person_id", UUID::class.java) } returns personId2
            every { rs2.getObject("organisation_id", UUID::class.java) } returns orgId2
            every { rs2.getString("type") } returns "Admin"
            
            listOf(
                rowMapper.mapRow(rs1, 0)!!,
                rowMapper.mapRow(rs2, 0)!!
            )
        }
        
        // Mock the organisation loading
        every { membershipRepository.getOrganisationById(orgId1) } returns org1
        every { membershipRepository.getOrganisationById(orgId2) } returns org2
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadMembershipsForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then
        assertEquals(1, person1.memberships.size)
        assertEquals(orgId1, person1.memberships[0].id.organisationId)
        assertEquals(org1.name, person1.memberships[0].organisation?.name)
        assertEquals(MembershipType.Member, person1.memberships[0].type)
        
        assertEquals(1, person2.memberships.size)
        assertEquals(orgId2, person2.memberships[0].id.organisationId)
        assertEquals(org2.name, person2.memberships[0].organisation?.name)
        assertEquals(MembershipType.Admin, person2.memberships[0].type)
        
        // Verify organisations were loaded
        verify(exactly = 1) { membershipRepository.getOrganisationById(orgId1) }
        verify(exactly = 1) { membershipRepository.getOrganisationById(orgId2) }
    }

    @Test
    fun `loadMembershipsForPersons should batch queries for large person lists`() {
        // Given - create more than 1000 persons to trigger batching
        val persons = (1..1500).map { i ->
            Person(
                id = UUID.randomUUID(),
                eventorId = "NOR",
                eventorRef = "REF$i",
                name = PersonName("Person", "$i")
            )
        }
        
        // Mock the membership query to return empty results
        every {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM membership WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<Membership>>()
            )
        } returns emptyList()
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadMembershipsForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - should have made 2 queries (1000 + 500 persons)
        verify(exactly = 2) {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM membership WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<Membership>>()
            )
        }
    }

    @Test
    fun `loadUsersForPersons should handle empty person list`() {
        // Given
        val persons = emptyList<Person>()
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadUsersForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - no exception should be thrown and no database queries should be made
        verify(exactly = 0) { namedParameterJdbcTemplate.query(any<String>(), any<Map<String, Any>>(), any<RowMapper<UserPerson>>()) }
    }

    @Test
    fun `loadUsersForPersons should handle persons with null IDs`() {
        // Given
        val persons = listOf(
            Person(id = null, eventorId = "NOR", eventorRef = "123", name = PersonName("Test", "User"))
        )
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadUsersForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - no database queries should be made
        verify(exactly = 0) { namedParameterJdbcTemplate.query(any<String>(), any<Map<String, Any>>(), any<RowMapper<UserPerson>>()) }
    }

    @Test
    fun `loadUsersForPersons should correctly load and associate user associations`() {
        // Given
        val personId1 = UUID.randomUUID()
        val personId2 = UUID.randomUUID()
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        
        val person1 = Person(
            id = personId1,
            eventorId = "NOR",
            eventorRef = "123",
            name = PersonName("Person", "One")
        )
        val person2 = Person(
            id = personId2,
            eventorId = "NOR",
            eventorRef = "456",
            name = PersonName("Person", "Two")
        )
        val persons = listOf(person1, person2)
        
        // Mock the user_person query
        every {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM user_person WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<UserPerson>>()
            )
        } answers {
            val rowMapper = thirdArg<RowMapper<UserPerson>>()
            val rs1 = mockk<ResultSet>()
            val rs2 = mockk<ResultSet>()
            
            every { rs1.getObject("user_id", UUID::class.java) } returns userId1
            every { rs1.getObject("person_id", UUID::class.java) } returns personId1
            
            every { rs2.getObject("user_id", UUID::class.java) } returns userId2
            every { rs2.getObject("person_id", UUID::class.java) } returns personId2
            
            listOf(
                rowMapper.mapRow(rs1, 0)!!,
                rowMapper.mapRow(rs2, 0)!!
            )
        }
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadUsersForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then
        assertEquals(1, person1.users.size)
        assertEquals(userId1, person1.users[0].id.userId)
        assertEquals(personId1, person1.users[0].id.personId)
        
        assertEquals(1, person2.users.size)
        assertEquals(userId2, person2.users[0].id.userId)
        assertEquals(personId2, person2.users[0].id.personId)
    }

    @Test
    fun `loadUsersForPersons should batch queries for large person lists`() {
        // Given - create more than 1000 persons to trigger batching
        val persons = (1..1500).map { i ->
            Person(
                id = UUID.randomUUID(),
                eventorId = "NOR",
                eventorRef = "REF$i",
                name = PersonName("Person", "$i")
            )
        }
        
        // Mock the user_person query to return empty results
        every {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM user_person WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<UserPerson>>()
            )
        } returns emptyList()
        
        // When
        val method = PersonRepository::class.java.getDeclaredMethod(
            "loadUsersForPersons",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(personRepository, persons)
        
        // Then - should have made 2 queries (1000 + 500 persons)
        verify(exactly = 2) {
            namedParameterJdbcTemplate.query(
                eq("SELECT * FROM user_person WHERE person_id IN (:personIds)"),
                any<Map<String, Any>>(),
                any<RowMapper<UserPerson>>()
            )
        }
    }
}
