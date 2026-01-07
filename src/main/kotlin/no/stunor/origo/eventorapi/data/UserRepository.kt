package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.User
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
open class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        User(id = rs.getString("id"))
    }
    
    open fun findById(id: String): User? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM auth.users WHERE id = ?",
                rowMapper,
                id
            )
        } catch (_: Exception) {
            null
        }
    }
    
    open fun save(user: User): User {
        jdbcTemplate.update(
            "INSERT INTO auth.users (id) VALUES (?) ON CONFLICT (id) DO NOTHING",
            user.id
        )
        return user
    }
}