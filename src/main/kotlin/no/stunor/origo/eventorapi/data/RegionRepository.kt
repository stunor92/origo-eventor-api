package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.Region
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.*

open class RegionRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        Region(
            id = rs.getObject("id", UUID::class.java),
            eventorId = rs.getString("eventor_id"),
            eventorRef = rs.getString("eventor_ref"),
            name = rs.getString("name")
        )
    }
    
    open fun findByEventorRefAndEventorId(eventorRef: String, eventorId: String): Region? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM region WHERE eventor_ref = ? AND eventor_id = ?",
                rowMapper,
                eventorRef, eventorId
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun findById(id: UUID): Region? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM region WHERE id = ?",
                rowMapper,
                id
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun save(region: Region): Region {
        if (region.id == null) {
            region.id = UUID.randomUUID()
            jdbcTemplate.update(
                "INSERT INTO region (id, eventor_id, eventor_ref, name) VALUES (?, ?, ?, ?)",
                region.id, region.eventorId, region.eventorRef, region.name
            )
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO region (id, eventor_id, eventor_ref, name) 
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET 
                    eventor_id = EXCLUDED.eventor_id,
                    eventor_ref = EXCLUDED.eventor_ref,
                    name = EXCLUDED.name
                """,
                region.id, region.eventorId, region.eventorRef, region.name
            )
        }
        return region
    }
}