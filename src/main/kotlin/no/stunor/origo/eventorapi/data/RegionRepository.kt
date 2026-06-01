package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.Region
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.*
import javax.sql.DataSource

internal object RegionTable : Table("region") {
    val id = javaUUID("id")
    override val primaryKey = PrimaryKey(id)
    val eventorId = text("eventor_id")
    val eventorRef = text("eventor_ref")
    val name = text("name")
}

open class RegionRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toRegion(row: ResultRow): Region {
        return Region(
            id = row[RegionTable.id],
            eventorId = row[RegionTable.eventorId],
            eventorRef = row[RegionTable.eventorRef],
            name = row[RegionTable.name]
        )
    }
    
    open fun findByEventorRefAndEventorId(eventorRef: String, eventorId: String): Region? {
        return transaction(database) {
            RegionTable
                .selectAll()
                .where { (RegionTable.eventorRef eq eventorRef) and (RegionTable.eventorId eq eventorId) }
                .limit(1)
                .map(::toRegion)
                .singleOrNull()
        }
    }
    
    open fun findById(id: UUID): Region? {
        return transaction(database) {
            RegionTable
                .selectAll()
                .where { RegionTable.id eq id }
                .limit(1)
                .map(::toRegion)
                .singleOrNull()
        }
    }
}