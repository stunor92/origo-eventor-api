package no.stunor.origo.eventorapi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.stunor.origo.eventorapi.exception.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<EventorNotFoundException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Eventor is not found"))
        }
        exception<EventNotFoundException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Event is not found"))
        }
        exception<EntryListNotFoundException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Entry-list is not found"))
        }
        exception<EventorAuthException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Eventor authentication failed. Please check your credentials and try again.")
            )
        }
        exception<EventorConnectionException> { call, _ ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("error" to "We are currently not able to connect to Eventor. Please try again later.")
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to (cause.message ?: "Unauthorized")))
        }
    }
}
