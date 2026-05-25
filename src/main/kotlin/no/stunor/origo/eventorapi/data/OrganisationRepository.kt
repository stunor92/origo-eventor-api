package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.organisation.OrganisationType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.*
import javax.sql.DataSource

internal object OrganisationTable : Table("organisation") {
    val id = uuid("id")
    val eventorId = text("eventor_id")
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val type = text("type")
    val country = text("country")
    val regionId = uuid("region_id").references(RegionTable.id).nullable()
}

open class OrganisationRepository(
    dataSource: DataSource,
    private val regionRepository: RegionRepository
) {
    
    private val database = Database.connect(dataSource)

    private fun toOrganisation(row: ResultRow): Organisation {
        val regionId = row[OrganisationTable.regionId]
        val region = regionId?.let { regionRepository.findById(it) }
        
        return Organisation(
            id = row[OrganisationTable.id],
            eventorId = row[OrganisationTable.eventorId],
            eventorRef = row[OrganisationTable.eventorRef],
            name = row[OrganisationTable.name],
            type = OrganisationType.valueOf(row[OrganisationTable.type]),
            country = row[OrganisationTable.country],
            region = region
        )
    }
    
    open fun findByEventorRefAndEventorId(eventorRef: String, eventorId: String): Organisation? {
        return transaction(database) {
            OrganisationTable
                .selectAll()
                .where { (OrganisationTable.eventorRef eq eventorRef) and (OrganisationTable.eventorId eq eventorId) }
                .limit(1)
                .map(::toOrganisation)
                .singleOrNull()
        }
    }
    
    open fun findById(id: UUID): Organisation? {
        return transaction(database) {
            OrganisationTable
                .selectAll()
                .where { OrganisationTable.id eq id }
                .limit(1)
                .map(::toOrganisation)
                .singleOrNull()
        }
    }
    
    open fun save(organisation: Organisation): Organisation {
        transaction(database) {
            if (organisation.id == null) {
                organisation.id = UUID.randomUUID()
            }

            OrganisationTable.upsert {
                it[OrganisationTable.id] = organisation.id!!
                it[OrganisationTable.eventorId] = organisation.eventorId
                it[OrganisationTable.eventorRef] = organisation.eventorRef
                it[OrganisationTable.name] = organisation.name
                it[OrganisationTable.type] = organisation.type.name
                it[OrganisationTable.country] = organisation.country
                it[OrganisationTable.regionId] = organisation.region?.id
            }
        }
        return organisation
    }
}