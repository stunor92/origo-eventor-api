package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.Fee
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.*

class FeeRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        Fee(
            id = rs.getObject("id", UUID::class.java),
            eventorRef = rs.getString("eventor_ref"),
            name = rs.getString("name"),
            currency = rs.getString("currency"),
            amount = rs.getDouble("amount"),
            externalFee = rs.getDouble("external_fee"),
            percentageSurcharge = rs.getInt("percentage_surcharge"),
            validFrom = rs.getTimestamp("valid_from"),
            validTo = rs.getTimestamp("valid_to"),
            fromBirthYear = rs.getInt("from_birth_year"),
            toBirthYear = rs.getInt("to_birth_year"),
            taxIncluded = rs.getBoolean("tax_included"),
            classes = mutableListOf(), // Load separately if needed
            eventId = rs.getObject("event_id", UUID::class.java)
        )
    }
    
    fun findAllByEventId(eventId: UUID?): List<Fee> {
        if (eventId == null) return emptyList()
        return jdbcTemplate.query(
            "SELECT * FROM fee WHERE event_id = ?",
            rowMapper,
            eventId
        )
    }
    
    fun save(fee: Fee): Fee {
        if (fee.id == null) {
            fee.id = UUID.randomUUID()
            jdbcTemplate.update(
                """
                INSERT INTO fee (id, eventor_ref, name, currency, amount, external_fee, 
                    percentage_surcharge, valid_from, valid_to, from_birth_year, to_birth_year, 
                    tax_included, event_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fee.id, fee.eventorRef, fee.name, fee.currency, fee.amount, fee.externalFee,
                fee.percentageSurcharge, fee.validFrom, fee.validTo, fee.fromBirthYear, 
                fee.toBirthYear, fee.taxIncluded, fee.eventId
            )
        } else {
            jdbcTemplate.update(
                """
                INSERT INTO fee (id, eventor_ref, name, currency, amount, external_fee, 
                    percentage_surcharge, valid_from, valid_to, from_birth_year, to_birth_year, 
                    tax_included, event_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    eventor_ref = EXCLUDED.eventor_ref,
                    name = EXCLUDED.name,
                    currency = EXCLUDED.currency,
                    amount = EXCLUDED.amount,
                    external_fee = EXCLUDED.external_fee,
                    percentage_surcharge = EXCLUDED.percentage_surcharge,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    from_birth_year = EXCLUDED.from_birth_year,
                    to_birth_year = EXCLUDED.to_birth_year,
                    tax_included = EXCLUDED.tax_included,
                    event_id = EXCLUDED.event_id
                """,
                fee.id, fee.eventorRef, fee.name, fee.currency, fee.amount, fee.externalFee,
                fee.percentageSurcharge, fee.validFrom, fee.validTo, fee.fromBirthYear, 
                fee.toBirthYear, fee.taxIncluded, fee.eventId
            )
        }
        
        // Save class associations
        if (fee.classes.isNotEmpty()) {
            // First delete existing associations
            jdbcTemplate.update("DELETE FROM class_fee WHERE fee_id = ?", fee.id)
            // Then insert new ones
            for (eventClass in fee.classes) {
                jdbcTemplate.update(
                    "INSERT INTO class_fee (fee_id, class_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    fee.id, eventClass.id
                )
            }
        }
        
        return fee
    }
    
    fun saveAll(fees: List<Fee>): List<Fee> {
        fees.forEach { save(it) }
        return fees
    }
    
    fun deleteAll(fees: List<Fee>) {
        fees.forEach { fee ->
            fee.id?.let { id ->
                jdbcTemplate.update("DELETE FROM class_fee WHERE fee_id = ?", id)
                jdbcTemplate.update("DELETE FROM fee WHERE id = ?", id)
            }
        }
    }
}