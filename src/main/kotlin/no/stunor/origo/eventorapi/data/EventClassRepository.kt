package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.ClassGender
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.EventClassTypeEnum
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.*

open class EventClassRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        EventClass(
            id = rs.getObject("id", UUID::class.java),
            eventorRef = rs.getString("eventor_ref"),
            name = rs.getString("name"),
            shortName = rs.getString("short_name"),
            type = EventClassTypeEnum.valueOf(rs.getString("type")),
            minAge = rs.getInt("min_age"),
            maxAge = rs.getInt("max_age"),
            gender = ClassGender.valueOf(rs.getString("gender")),
            presentTime = rs.getBoolean("present_time"),
            orderedResult = rs.getBoolean("ordered_result"),
            legs = rs.getInt("legs"),
            minAverageAge = rs.getInt("min_average_age"),
            maxAverageAge = rs.getInt("max_average_age")
        )
    }

    open fun findByEventId(eventId: UUID?): List<EventClass> {
        return jdbcTemplate.query(
            "SELECT * FROM class WHERE event_id = ?",
            rowMapper,
            eventId
        )
    }
}


