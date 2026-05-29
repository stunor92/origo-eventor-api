package no.stunor.origo.eventorapi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.stunor.origo.eventorapi.Dependencies
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import java.time.LocalDate
import java.util.UUID

fun Application.configureRouting(deps: Dependencies) {
    routing {
        // ── Health ─────────────────────────────────────────────────────────────
        get("/actuator/health") {
            call.respond(mapOf("status" to "UP"))
        }

        route("/rest") {
            authenticate("jwt-optional", optional = true) {
                get("/event/{eventorId}/{eventorRef}") {
                    val eventorId  = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventorRef = deps.inputValidator.validateEventId(call.parameters["eventorRef"]!!)
                    call.respond(deps.eventService.getEvent(eventorId, eventorRef))
                }

                get("/event/{eventorId}/{eventId}/entry-list") {
                    val eventorId = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventId   = deps.inputValidator.validateEventId(call.parameters["eventId"]!!)
                    call.respond(deps.eventService.getEntryList(eventorId, eventId))
                }

                get("/event-list/{eventorId}") {
                    val eventorId       = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
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
                    val validatedOrgs = deps.inputValidator.validateOrganisationIds(organisations)
                    val result = deps.calendarService.getEventList(
                        eventorId       = eventorId,
                        from            = from,
                        to              = to,
                        organisations   = validatedOrgs,
                        classifications = classifications,
                        userId          = uid
                    )
                    call.respond(if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK, result.data)
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
                    val result = deps.calendarService.getEventList(
                        from            = from,
                        to              = to,
                        classifications = classifications,
                        userId          = uid
                    )
                    call.respond(if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK, result.data)
                }
            }

            authenticate("jwt-required") {
                post("/person/{eventorId}") {
                    val eventorId = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val username  = deps.inputValidator.validateUsername(
                        call.request.headers["username"] ?: throw IllegalArgumentException("Missing username header")
                    )
                    val password  = call.request.headers["password"]
                        ?: throw IllegalArgumentException("Missing password header")
                    val uid = UUID.fromString(
                        call.principal<JWTPrincipal>()?.subject
                            ?: throw IllegalStateException("Authentication required")
                    )
                    call.respond(deps.personService.authenticate(eventorId, username, password, uid))
                }
            }
        }
    }
}
