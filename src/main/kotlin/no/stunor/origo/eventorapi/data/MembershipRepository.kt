package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.Membership
import no.stunor.origo.eventorapi.model.person.MembershipKey
import no.stunor.origo.eventorapi.model.person.MembershipType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class MembershipRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val organisationRepository: OrganisationRepository
) {
    
    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        val personId = rs.getObject("person_id", UUID::class.java)
        val organisationId = rs.getObject("organisation_id", UUID::class.java)
        val organisation = organisationId?.let { organisationRepository.findById(it) }
        
        Membership(
            id = MembershipKey(personId = personId, organisationId = organisationId),
            person = null, // Avoid circular dependency
            organisation = organisation,
            type = MembershipType.valueOf(rs.getString("type"))
        )
    }
    
    open fun findAllByPersonId(personId: UUID?): List<Membership> {
        if (personId == null) return emptyList()
        return jdbcTemplate.query(
            "SELECT * FROM membership WHERE person_id = ?",
            rowMapper,
            personId
        )
    }
    
    open fun getOrganisationById(organisationId: UUID) = organisationRepository.findById(organisationId)
    
    open fun getOrganisationsByIds(organisationIds: List<UUID>) = organisationRepository.findByIds(organisationIds)

    open fun save(membership: Membership): Membership {
        jdbcTemplate.update(
            """
            INSERT INTO membership (person_id, organisation_id, type) 
            VALUES (?, ?, ?::membership_type)
            ON CONFLICT (person_id, organisation_id) DO UPDATE SET 
                type = EXCLUDED.type
            """,
            membership.id.personId, membership.id.organisationId, membership.type.name
        )
        return membership
    }

    open fun deleteByPersonId(personId: UUID?) {
        if (personId != null) {
            jdbcTemplate.update("DELETE FROM membership WHERE person_id = ?", personId)
        }
    }
}
