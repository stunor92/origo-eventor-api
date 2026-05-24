package no.stunor.origo.eventorapi.services.converter

import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimeStampConverter {
    companion object {

        fun parseDate(time: String): Timestamp {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return Timestamp.from(Instant.from(formatter.withZone(ZoneOffset.UTC).parse(time)))
        }

        fun parseDate(time: String, eventorId: String): Timestamp {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return Timestamp.from(Instant.from(formatter.withZone(getTimeZone(eventorId)).parse(time)))
        }

        private fun getTimeZone(eventorId: String): ZoneId {
            if (eventorId == "AUS") {
                return ZoneId.of("Australia/Sydney")
            }
            return ZoneId.of("Europe/Paris")
        }
    }
}
