package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.event.ClassGender
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClass
import no.stunor.origo.eventorapi.model.event.EventClassTypeEnum
import org.iof.eventor.EventClassList
import org.iof.eventor.HashTableEntry

class EventClassConverter {
    companion object {
        /**
         * Safely parse a string to integer, handling European decimal format (comma separator).
         * Rounds decimal values to nearest integer.
         * @param value String value to parse (may contain comma as decimal separator)
         * @return Parsed integer value or null if parsing fails
         */
        private fun safeParseInt(value: String?): Int? {
            if (value == null) return null
            return try {
                // Replace comma with period for European decimal format, then round to int
                value.replace(',', '.').toDouble().toInt()
            } catch (_: NumberFormatException) {
                null
            }
        }

        fun convertEventClasses(
            eventCLassList: EventClassList?,
            event: Event
        ): List<EventClass> {
            if (eventCLassList == null)
                return mutableListOf()
            val result = mutableListOf<EventClass>()
            for (eventClass in eventCLassList.eventClass) {
                if (eventClass != null) {
                    result.add(convertEventClass(event, eventClass))
                }
            }
            return result
        }

        fun convertEventClass(event: Event, eventClass: org.iof.eventor.EventClass): EventClass {
            return EventClass(
                eventorRef = eventClass.eventClassId.content,
                name = eventClass.name.content,
                shortName = eventClass.classShortName.content,
                type = getClassTypeFromId(if (eventClass.classType != null) eventClass.classType.classTypeId.content else eventClass.classTypeId.content),
                minAge = safeParseInt(eventClass.lowAge),
                maxAge = safeParseInt(eventClass.highAge),
                gender = convertGender(eventClass.sex),
                presentTime = getTimePresentation(eventClass.hashTableEntry),
                orderedResult = getResultListMode(eventClass.hashTableEntry),
                legs = safeParseInt(eventClass.numberOfLegs) ?: 1,
                minAverageAge = safeParseInt(eventClass.minAverageAge),
                maxAverageAge = safeParseInt(eventClass.maxAverageAge),
                event = event
            )
        }

        private fun convertGender(sex: String): ClassGender {
            return when (sex) {
                "M" -> ClassGender.Men
                "F" -> ClassGender.Women
                else -> ClassGender.Both
            }
        }

        private fun getClassTypeFromId(classTypeId: String): EventClassTypeEnum {
            return when (classTypeId) {
                "1" -> EventClassTypeEnum.Elite
                "3" -> EventClassTypeEnum.Open
                else -> EventClassTypeEnum.Normal
            }
        }

        private fun getResultListMode(hashTableEntryList: List<HashTableEntry>): Boolean {
            for (hashTableEntry in hashTableEntryList) {
                if (hashTableEntry.key.content == "Eventor_ResultListMode"
                    && (hashTableEntry.value.content == "UnorderedNoTimes"
                            || hashTableEntry.value.content == "Unordered")
                ) {
                    return false
                }
            }
            return true
        }

        private fun getTimePresentation(hashTableEntryList: List<HashTableEntry>): Boolean {
            for (hashTableEntry in hashTableEntryList) {
                if (hashTableEntry.key.content == "Eventor_ResultListMode"
                    && hashTableEntry.value.content == "UnorderedNoTimes"
                ) {
                    return false
                }
            }
            return true
        }

    }
}
