package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.Fee
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.*
import javax.sql.DataSource

private object FeeTable : Table("fee") {
    val id = uuid("id")
    override val primaryKey = PrimaryKey(id)
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val currency = text("currency").nullable()
    val amount = double("amount").nullable()
    val externalFee = double("external_fee").nullable()
    val percentageSurcharge = integer("percentage_surcharge").nullable()
    val validFrom = timestamp("valid_from").nullable()
    val validTo = timestamp("valid_to").nullable()
    val fromBirthYear = integer("from_birth_year").nullable()
    val toBirthYear = integer("to_birth_year").nullable()
    val taxIncluded = bool("tax_included")
    val eventId = uuid("event_id").references(EventTable.id).nullable()
}

private object ClassFeeTable : Table("class_fee") {
    val feeId = uuid("fee_id").references(FeeTable.id)
    val classId = uuid("class_id").references(ClassTable.id)
    override val primaryKey = PrimaryKey(feeId, classId)
}

class FeeRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toFee(row: ResultRow): Fee {
        return Fee(
            id = row[FeeTable.id],
            eventorRef = row[FeeTable.eventorRef],
            name = row[FeeTable.name],
            currency = row[FeeTable.currency],
            amount = row[FeeTable.amount],
            externalFee = row[FeeTable.externalFee],
            percentageSurcharge = row[FeeTable.percentageSurcharge],
            validFrom = row[FeeTable.validFrom]?.let { java.sql.Timestamp.from(it) },
            validTo = row[FeeTable.validTo]?.let { java.sql.Timestamp.from(it) },
            fromBirthYear = row[FeeTable.fromBirthYear],
            toBirthYear = row[FeeTable.toBirthYear],
            taxIncluded = row[FeeTable.taxIncluded],
            classes = mutableListOf(), // Load separately if needed
            eventId = row[FeeTable.eventId]
        )
    }
    
    fun findAllByEventId(eventId: UUID?): List<Fee> {
        if (eventId == null) return emptyList()
        return transaction(database) {
            FeeTable
                .selectAll()
                .where { FeeTable.eventId eq eventId }
                .map(::toFee)
        }
    }
    
    fun save(fee: Fee): Fee {
        transaction(database) {
            if (fee.id == null) {
                fee.id = UUID.randomUUID()
            }

            FeeTable.upsert {
                it[FeeTable.id] = fee.id!!
                it[FeeTable.eventorRef] = fee.eventorRef
                it[FeeTable.name] = fee.name
                it[FeeTable.currency] = fee.currency
                it[FeeTable.amount] = fee.amount
                it[FeeTable.externalFee] = fee.externalFee
                it[FeeTable.percentageSurcharge] = fee.percentageSurcharge
                it[FeeTable.validFrom] = fee.validFrom?.toInstant()
                it[FeeTable.validTo] = fee.validTo?.toInstant()
                it[FeeTable.fromBirthYear] = fee.fromBirthYear
                it[FeeTable.toBirthYear] = fee.toBirthYear
                it[FeeTable.taxIncluded] = fee.taxIncluded
                it[FeeTable.eventId] = fee.eventId
            }

            // Save class associations
            if (fee.classes.isNotEmpty()) {
                // First delete existing associations
                ClassFeeTable.deleteWhere { ClassFeeTable.feeId eq fee.id!! }
                // Then insert new ones
                for (eventClass in fee.classes) {
                    ClassFeeTable.upsert {
                        it[ClassFeeTable.feeId] = fee.id!!
                        it[ClassFeeTable.classId] = eventClass.id
                    }
                }
            }
        }
        
        return fee
    }
    
    fun saveAll(fees: List<Fee>): List<Fee> {
        fees.forEach { save(it) }
        return fees
    }
    
    fun deleteAll(fees: List<Fee>) {
        transaction(database) {
            fees.forEach { fee ->
                fee.id?.let { id ->
                    ClassFeeTable.deleteWhere { ClassFeeTable.feeId eq id }
                    FeeTable.deleteWhere { FeeTable.id eq id }
                }
            }
        }
    }
}