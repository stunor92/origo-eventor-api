package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.Eventor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

open class EventorRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        Eventor(
            id = rs.getString("id"),
            name = rs.getString("name"),
            federation = rs.getString("federation"),
            baseUrl = rs.getString("base_url"),
            eventorApiKey = rs.getString("eventor_api_key")
        )
    }
    
    open fun findById(id: String): Eventor? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM eventor WHERE id = ?",
                rowMapper,
                id
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun findAll(): List<Eventor> {
        return jdbcTemplate.query("SELECT * FROM eventor", rowMapper)
    }

}