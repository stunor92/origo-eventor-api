package no.stunor.origo.eventorapi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.services.CalendarService
import no.stunor.origo.eventorapi.services.EventService
import no.stunor.origo.eventorapi.services.PersonService
import no.stunor.origo.eventorapi.validation.InputValidator
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

fun Application.configureRouting() {
    val eventService:    EventService    by inject()
    val calendarService: CalendarService by inject()
    val personService:   PersonService   by inject()
    val inputValidator:  InputValidator  by inject()

    routing {
        // ── Health / actuator ──────────────────────────────────────────────────
        get("/actuator/health") {
            call.respond(mapOf("status" to "UP"))
        }

        // All application routes live under /rest (matching the Spring context-path)
        route("/rest") {

            // ── Event endpoints (no auth required) ────────────────────────────
            authenticate("jwt-optional", optional = true) {
                get("/event/{eventorId}/{eventorRef}") {
                    val eventorId  = inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventorRef = inputValidator.validateEventId(call.parameters["eventorRef"]!!)
                    val event = withContext(Dispatchers.IO) {
                        eventService.getEvent(eventorId, eventorRef)
                    }
                    call.respond(event)
                }

                get("/event/{eventorId}/{eventId}/entry-list") {
                    val eventorId = inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventId   = inputValidator.validateEventId(call.parameters["eventId"]!!)
                    val entries = withContext(Dispatchers.IO) {
                        eventService.getEntryList(eventorId, eventId)
                    }
                    call.respond(entries)
                }

                // ── Event-list endpoints (auth optional) ──────────────────────
                get("/event-list/{eventorId}") {
                    val eventorId       = inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val from            = LocalDate.parse(call.request.queryParameters["from"]!!)
                    val to              = LocalDate.parse(call.request.queryParameters["to"]!!)
                    val organisations   = call.request.queryParameters.getAll("organisations")
                    val classifications = call.request.queryParameters.getAll("classifications")
                        ?.flatMap { it.split(",") }
                        ?.mapNotNull { runCatching { EventClassificationEnum.valueOf(it.trim()) }.getOrNull() }
                        ?: listOf(
                            EventClassificationEnum.Championship,
                            EventClassificationEnum.National,
                            EventClassificationEnum.Regional,
                            EventClassificationEnum.Local
                        )
                    val uid = call.principal<JWTPrincipal>()?.subject?.let { UUID.fromString(it) }
                    val validatedOrgs = inputValidator.validateOrganisationIds(organisations)

                    val result = withContext(Dispatchers.IO) {
                        calendarService.getEventList(
                            eventorId       = eventorId,
                            from            = from,
                            to              = to,
                            organisations   = validatedOrgs,
                            classifications = classifications,
                            userId          = uid
                        )
                    }
                    val status = if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK
                    call.respond(status, result.data)
                }

                get("/event-list") {
                    val from            = LocalDate.parse(call.request.queryParameters["from"]!!)
                    val to              = LocalDate.parse(call.request.queryParameters["to"]!!)
                    val classifications = call.request.queryParameters.getAll("classifications")
                        ?.flatMap { it.split(",") }
                        ?.mapNotNull { runCatching { EventClassificationEnum.valueOf(it.trim()) }.getOrNull() }
                        ?: listOf(
                            EventClassificationEnum.Championship,
                            EventClassificationEnum.National,
                            EventClassificationEnum.Regional,
                            EventClassificationEnum.Local
                        )
                    val uid = call.principal<JWTPrincipal>()?.subject?.let { UUID.fromString(it) }

                    val result = withContext(Dispatchers.IO) {
                        calendarService.getEventList(
                            from            = from,
                            to              = to,
                            classifications = classifications,
                            userId          = uid
                        )
                    }
                    val status = if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK
                    call.respond(status, result.data)
                }
            }

            // ── Person endpoints (auth required) ──────────────────────────────
            authenticate("jwt-required") {
                post("/person/{eventorId}") {
                    val eventorId         = inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val username          = inputValidator.validateUsername(call.request.headers["username"]
                        ?: throw IllegalArgumentException("Missing username header"))
                    val password          = call.request.headers["password"]
                        ?: throw IllegalArgumentException("Missing password header")
                    val uid = UUID.fromString(
                        call.principal<JWTPrincipal>()?.subject
                            ?: throw IllegalStateException("Authentication required")
                    )
                    val person = withContext(Dispatchers.IO) {
                        personService.authenticate(eventorId, username, password, uid)
                    }
                    call.respond(person)
                }
            }
        }
    }
}
