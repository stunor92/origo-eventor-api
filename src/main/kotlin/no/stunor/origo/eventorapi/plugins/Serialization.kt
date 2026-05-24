package no.stunor.origo.eventorapi.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import java.io.IOException
import java.sql.Timestamp
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerModule(kotlinModule())

            val javaTimeModule = JavaTimeModule()
            javaTimeModule.addSerializer(Timestamp::class.java, TimestampSerializer())
            registerModule(javaTimeModule)

            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

private class TimestampSerializer : StdSerializer<Timestamp>(Timestamp::class.java) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    @Throws(IOException::class)
    override fun serialize(value: Timestamp, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.toInstant().atZone(ZoneOffset.UTC).format(formatter))
    }
}
