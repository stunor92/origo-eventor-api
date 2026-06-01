package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.Membership
import no.stunor.origo.eventorapi.model.person.MembershipKey
import no.stunor.origo.eventorapi.model.person.MembershipType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.*
import javax.sql.DataSource

internal object MembershipTable : Table("membership") {
    val personId = javaUUID("person_id").references(PersonTable.id)
    val organisationId = javaUUID("organisation_id").references(OrganisationTable.id)
    val type = text("type")

    override val primaryKey = PrimaryKey(personId, organisationId)
}

open class MembershipRepository(
    dataSource: DataSource,
    private val organisationRepository: OrganisationRepository
) {
    
    private val database = Database.connect(dataSource)

    private fun toMembership(row: ResultRow): Membership {
        val personId = row[MembershipTable.personId]
        val organisationId = row[MembershipTable.organisationId]
        val organisation = organisationRepository.findById(organisationId)

        return Membership(
            id = MembershipKey(personId = personId, organisationId = organisationId),
            person = null, // Avoid circular dependency
            organisation = organisation,
            type = MembershipType.valueOf(row[MembershipTable.type])
        )
    }
    
    open fun findAllByPersonId(personId: UUID?): List<Membership> {
        if (personId == null) return emptyList()
        return transaction(database) {
            MembershipTable
                .selectAll()
                .where { MembershipTable.personId eq personId }
                .map(::toMembership)
        }
    }
    
    open fun getOrganisationById(organisationId: UUID) = organisationRepository.findById(organisationId)

    open fun save(membership: Membership): Membership {
        transaction(database) {
            MembershipTable.upsert {
                it[MembershipTable.personId] = membership.id.personId!!
                it[MembershipTable.organisationId] = membership.id.organisationId!!
                it[MembershipTable.type] = membership.type.name
            }
        }
        return membership
    }

    open fun deleteByPersonId(personId: UUID?) {
        if (personId != null) {
            transaction(database) {
                MembershipTable.deleteWhere { MembershipTable.personId eq personId }
            }
        }
    }
}
