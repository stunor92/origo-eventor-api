package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.Fee

class FeeConverter {
    companion object {
        /**
         * Parses a decimal string that may use either comma or period as decimal separator.
         * Handles European format (12,5) and US/English format (12.5).
         *
         * @param value The string to parse
         * @return The parsed double value
         * @throws NumberFormatException if the string cannot be parsed
         */
        private fun parseDecimal(value: String): Double {
            // Replace comma with period to normalize decimal separator
            return value.replace(',', '.').toDouble()
        }

        fun convertEntryFees(
            entryFees: org.iof.eventor.EntryFeeList?,
            event: Event,
            eventClassList: List<org.iof.eventor.EventClass>
        ): MutableList<Fee> {
            val result = mutableListOf<Fee>()

            if (entryFees == null) return result

            for (entryFee in entryFees.entryFee) {
                result.add(convertEntryFee(entryFee, event, eventClassList))
            }
            return result
        }

        private fun convertEntryFee(
            entryFee: org.iof.eventor.EntryFee,
            event: Event,
            eventClassList: List<org.iof.eventor.EventClass>
        ): Fee {
            return Fee(
                eventorRef = entryFee.entryFeeId.content,
                name = entryFee.name.content,
                currency = if (entryFee.valueOperator == "fixed" && entryFee.amount != null) entryFee.amount.currency else null,
                amount = if (entryFee.valueOperator == "fixed" && entryFee.amount != null) parseDecimal(entryFee.amount.content) else null,
                externalFee = if (entryFee.externalFee != null) parseDecimal(entryFee.externalFee.content) else null,
                percentageSurcharge = if (entryFee.valueOperator == "percent" && entryFee.amount != null) entryFee.amount.content.toInt() else null,
                validFrom = if (entryFee.validFromDate != null) TimeStampConverter.parseDate(
                    "${entryFee.validFromDate.date.content} ${entryFee.validFromDate.clock.content}",
                    event.eventorId
                ) else null,
                validTo = if (entryFee.validToDate != null) TimeStampConverter.parseDate(
                    "${entryFee.validToDate.date.content} ${entryFee.validToDate.clock.content}",
                    event.eventorId
                ) else null,
                fromBirthYear = if (entryFee.fromDateOfBirth != null) entryFee.fromDateOfBirth.date.content.substring(
                    0,
                    4
                ).toInt() else null,
                toBirthYear = if (entryFee.toDateOfBirth != null) entryFee.toDateOfBirth.date.content.substring(0, 4)
                    .toInt() else null,
                taxIncluded = entryFee.taxIncluded == "Y",
                classes = findEventClasses(event.classes, entryFee, eventClassList),
                eventId = event.id
            )
        }

        private fun findEventClasses(
            classes: List<EventClass>,
            fee: org.iof.eventor.EntryFee,
            eventClassList: List<org.iof.eventor.EventClass>
        ): MutableList<EventClass> {
            val result = mutableListOf<EventClass>()

            for (eventClass in eventClassList) {
                for (classFee in eventClass.classEntryFee) {
                    if (classFee.entryFeeId.content == fee.entryFeeId.content) {
                        classes.find{ it.eventorRef == eventClass.eventClassId.content }?.let { result.add(it) }
                    }
                }
            }
            return result
        }
    }
}

