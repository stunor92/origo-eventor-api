package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.Eventor
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import javax.sql.DataSource

private object EventorTable : Table("eventor") {
    val id = text("id")
    val name = text("name")
    val federation = text("federation")
    val baseUrl = text("base_url")
    val eventorApiKey = text("eventor_api_key")
}


open class EventorRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toEventor(row: ResultRow): Eventor {
        return Eventor(
            id = row[EventorTable.id],
            name = row[EventorTable.name],
            federation = row[EventorTable.federation],
            baseUrl = row[EventorTable.baseUrl],
            eventorApiKey = row[EventorTable.eventorApiKey]
        )
    }
    
    open fun findById(id: String): Eventor? {
        return transaction(database) {
            EventorTable
                .selectAll()
                .where { EventorTable.id eq id }
                .limit(1)
                .map(::toEventor)
                .singleOrNull()
        }
    }
    
    open fun findAll(): List<Eventor> {
        return transaction(database) {
            EventorTable
                .selectAll()
                .map(::toEventor)
        }
    }

}