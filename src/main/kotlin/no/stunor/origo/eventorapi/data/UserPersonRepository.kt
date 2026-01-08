package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class UserPersonRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        UserPerson(
            id = UserPersonKey(
                userId = rs.getObject("user_id", UUID::class.java),
                personId = rs.getObject("person_id", UUID::class.java)
            ),
            person = null // Avoid circular dependency
        )
    }

    open fun findAllByPersonId(personId: UUID): List<UserPerson> {
        return jdbcTemplate.query(
            "SELECT * FROM user_person WHERE person_id = ?",
            rowMapper,
            personId
        )
    }

    open fun save(userPerson: UserPerson): UserPerson {
        jdbcTemplate.update(
            "INSERT INTO user_person (user_id, person_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            userPerson.id.userId, userPerson.id.personId
        )
        return userPerson
    }

}