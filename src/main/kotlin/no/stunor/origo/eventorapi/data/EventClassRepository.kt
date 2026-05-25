package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.ClassGender
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.EventClassTypeEnum
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.sql.DataSource

private object EventClassTable : Table("class") {
    val id = uuid("id")
    val eventId = uuid("event_id")
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val shortName = text("short_name")
    val type = text("type")
    val minAge = integer("min_age")
    val maxAge = integer("max_age")
    val gender = text("gender")
    val presentTime = bool("present_time")
    val orderedResult = bool("ordered_result")
    val legs = integer("legs")
    val minAverageAge = integer("min_average_age")
    val maxAverageAge = integer("max_average_age")
}

open class EventClassRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toEventClass(row: ResultRow): EventClass {
        return EventClass(
            id = row[EventClassTable.id],
            eventorRef = row[EventClassTable.eventorRef],
            name = row[EventClassTable.name],
            shortName = row[EventClassTable.shortName],
            type = EventClassTypeEnum.valueOf(row[EventClassTable.type]),
            minAge = row[EventClassTable.minAge],
            maxAge = row[EventClassTable.maxAge],
            gender = ClassGender.valueOf(row[EventClassTable.gender]),
            presentTime = row[EventClassTable.presentTime],
            orderedResult = row[EventClassTable.orderedResult],
            legs = row[EventClassTable.legs],
            minAverageAge = row[EventClassTable.minAverageAge],
            maxAverageAge = row[EventClassTable.maxAverageAge]
        )
    }

    open fun findByEventId(eventId: UUID?): List<EventClass> {
        if (eventId == null) return emptyList()
        return transaction(database) {
            EventClassTable
                .selectAll()
                .where { EventClassTable.eventId eq eventId }
                .map(::toEventClass)
        }
    }
}

