package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.Region
import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.organisation.OrganisationType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.*
import javax.sql.DataSource
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal object OrganisationTable : Table("organisation") {
    val id = uuid("id")
    override val primaryKey = PrimaryKey(id)
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
        val regionId = row[OrganisationTable.regionId]?.toJavaUuid()
        val region = regionId?.let { regionRepository.findById(it) }

        return Organisation(
            id = row[OrganisationTable.id].toJavaUuid(),
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
    
    open fun findAllByEventorId(eventorId: String): Map<String, Organisation> {
        return transaction(database) {
            (OrganisationTable leftJoin RegionTable)
                .selectAll()
                .where { OrganisationTable.eventorId eq eventorId }
                .associate { row ->
                    val region = row[OrganisationTable.regionId]?.let {
                        Region(
                            id = row[RegionTable.id].toJavaUuid(),
                            eventorId = row[RegionTable.eventorId],
                            eventorRef = row[RegionTable.eventorRef],
                            name = row[RegionTable.name]
                        )
                    }
                    row[OrganisationTable.eventorRef] to Organisation(
                        id = row[OrganisationTable.id].toJavaUuid(),
                        eventorId = row[OrganisationTable.eventorId],
                        eventorRef = row[OrganisationTable.eventorRef],
                        name = row[OrganisationTable.name],
                        type = OrganisationType.valueOf(row[OrganisationTable.type]),
                        country = row[OrganisationTable.country],
                        region = region
                    )
                }
        }
    }

    open fun findById(id: UUID): Organisation? {
        return transaction(database) {
            OrganisationTable
                .selectAll()
                .where { OrganisationTable.id eq id.toKotlinUuid() }
                .limit(1)
                .map(::toOrganisation)
                .singleOrNull()
        }
    }
    
    open fun save(organisation: Organisation): Organisation {
        transaction(database) {
            OrganisationTable.upsert(OrganisationTable.eventorId, OrganisationTable.eventorRef,
                onUpdateExclude = listOf(OrganisationTable.id)
            ) {
                it[OrganisationTable.id] = (organisation.id ?: UUID.randomUUID()).toKotlinUuid()
                it[OrganisationTable.eventorId] = organisation.eventorId
                it[OrganisationTable.eventorRef] = organisation.eventorRef
                it[OrganisationTable.name] = organisation.name
                it[OrganisationTable.type] = organisation.type.name
                it[OrganisationTable.country] = organisation.country
                it[OrganisationTable.regionId] = organisation.region?.id?.toKotlinUuid()
            }
            organisation.id = findByEventorRefAndEventorId(organisation.eventorRef, organisation.eventorId)!!.id
        }
        return organisation
    }
}