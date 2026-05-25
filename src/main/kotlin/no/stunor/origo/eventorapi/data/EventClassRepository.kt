package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.ClassGender
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.EventClassTypeEnum
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.sql.DataSource

open class EventClassRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toEventClass(row: ResultRow): EventClass {
        return EventClass(
            id = row[ClassTable.id],
            eventorRef = row[ClassTable.eventorRef],
            name = row[ClassTable.name],
            shortName = row[ClassTable.shortName],
            type = EventClassTypeEnum.valueOf(row[ClassTable.type]),
            minAge = row[ClassTable.minAge],
            maxAge = row[ClassTable.maxAge],
            gender = ClassGender.valueOf(row[ClassTable.gender]),
            presentTime = row[ClassTable.presentTime],
            orderedResult = row[ClassTable.orderedResult],
            legs = row[ClassTable.legs],
            minAverageAge = row[ClassTable.minAverageAge],
            maxAverageAge = row[ClassTable.maxAverageAge]
        )
    }

    open fun findByEventId(eventId: UUID?): List<EventClass> {
        if (eventId == null) return emptyList()
        return transaction(database) {
            ClassTable
                .selectAll()
                .where { ClassTable.eventId eq eventId }
                .map(::toEventClass)
        }
    }
}

