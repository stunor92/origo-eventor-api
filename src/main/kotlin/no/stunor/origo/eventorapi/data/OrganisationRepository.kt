package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.organisation.OrganisationType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OrganisationRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val regionRepository: RegionRepository
) {
    
    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        val regionId = rs.getObject("region_id", UUID::class.java)
        val region = regionId?.let { regionRepository.findById(it) }
        
        Organisation(
            id = rs.getObject("id", UUID::class.java),
            eventorId = rs.getString("eventor_id"),
            eventorRef = rs.getString("eventor_ref"),
            name = rs.getString("name"),
            type = OrganisationType.valueOf(rs.getString("type")),
            country = rs.getString("country"),
            region = region
        )
    }
    
    open fun findByEventorRefAndEventorId(eventorRef: String, eventorId: String): Organisation? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM organisation WHERE eventor_ref = ? AND eventor_id = ?",
                rowMapper,
                eventorRef, eventorId
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun findById(id: UUID): Organisation? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM organisation WHERE id = ?",
                rowMapper,
                id
            )
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * Batch load organisations by their IDs.
     * 
     * @param ids List of organisation IDs to load
     * @return Map of organisation ID to Organisation object
     */
    open fun findByIds(ids: List<UUID>): Map<UUID, Organisation> {
        if (ids.isEmpty()) return emptyMap()
        
        val params = mapOf("ids" to ids)
        val sql = "SELECT * FROM organisation WHERE id IN (:ids)"
        
        return namedParameterJdbcTemplate.query(sql, params, rowMapper)
            .mapNotNull { org -> org.id?.let { id -> id to org } }
            .toMap()
    }
    
    open fun save(organisation: Organisation): Organisation {
        if (organisation.id == null) {
            organisation.id = UUID.randomUUID()
            jdbcTemplate.update(
                "INSERT INTO organisation (id, eventor_id, eventor_ref, name, type, country, region_id) VALUES (?, ?, ?, ?, ?::organisation_type, ?, ?)",
                organisation.id, organisation.eventorId, organisation.eventorRef, organisation.name,
                organisation.type.name, organisation.country, organisation.region?.id
            )
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO organisation (id, eventor_id, eventor_ref, name, type, country, region_id) 
                VALUES (?, ?, ?, ?, ?::organisation_type, ?, ?)
                ON CONFLICT (id) DO UPDATE SET 
                    eventor_id = EXCLUDED.eventor_id,
                    eventor_ref = EXCLUDED.eventor_ref,
                    name = EXCLUDED.name,
                    type = EXCLUDED.type,
                    country = EXCLUDED.country,
                    region_id = EXCLUDED.region_id
                """,
                organisation.id, organisation.eventorId, organisation.eventorRef, organisation.name,
                organisation.type.name, organisation.country, organisation.region?.id
            )
        }
        return organisation
    }
}