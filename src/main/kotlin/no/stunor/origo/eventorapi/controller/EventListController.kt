package no.stunor.origo.eventorapi.controller

import jakarta.servlet.http.HttpServletRequest
import no.stunor.origo.eventorapi.model.calendar.CalendarRace
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.services.CalendarService
import no.stunor.origo.eventorapi.validation.InputValidator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("event-list")
internal class EventListController {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var calendarService: CalendarService

    @Autowired
    private lateinit var inputValidator: InputValidator


    @GetMapping("/{eventorId}")
    fun HttpServletRequest.getEventList(
        @PathVariable eventorId: String,
        @RequestParam("from") from: LocalDate,
        @RequestParam("to") to: LocalDate,
        @RequestParam(
            value = "organisations",
            required = false
        ) organisations: List<String>?,
        @RequestParam(
            value = "classifications",
            required = false,
            defaultValue = "Championship, National, Regional, Local"
        ) classifications: List<EventClassificationEnum>?
    ): ResponseEntity<List<CalendarRace>> {
        log.info("Start to get event-list from eventor-{}.", eventorId)
        val uidString = getAttribute("uid") as String?
        val uid = uidString?.let { UUID.fromString(it) }

        // Validate input to prevent SSRF attacks
        val validatedEventorId = inputValidator.validateEventorId(eventorId)
        val validatedOrganisations = inputValidator.validateOrganisationIds(organisations)
        return ResponseEntity(
                calendarService.getEventList(
                        eventorId = validatedEventorId,
                        from = from,
                        to = to,
                        organisations = validatedOrganisations,
                        classifications = classifications,
                        userId = uid
                ),
                HttpStatus.OK
        )
    }

    @GetMapping
    fun HttpServletRequest.getEventList(
        @RequestParam("from") from: LocalDate,
        @RequestParam("to") to: LocalDate,
        @RequestParam(
            value = "classifications",
            required = false,
            defaultValue = "Championship, National, Regional, Local"
        ) classifications: List<EventClassificationEnum>?
    ): ResponseEntity<List<CalendarRace>> {
        log.info("Start to get event-list from all eventors.")
        val uidString = getAttribute("uid") as String?
        val uid = uidString?.let { UUID.fromString(it) }
        return ResponseEntity(
            calendarService.getEventList(
                from = from,
                to = to,
                classifications = classifications,
                userId = uid
            ),
            HttpStatus.OK
        )
    }

    @GetMapping("/me")
    fun HttpServletRequest.getUserEntries(): ResponseEntity<List<CalendarRace>> {
        val uid = UUID.fromString(getAttribute("uid") as String?)

        // /me endpoint requires authentication
        if (uid == null) {
            log.warn("Attempted to access /me endpoint without authentication")
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

        return ResponseEntity(
            calendarService.getEventList(
                userId = uid
            ),
            HttpStatus.OK
        )
    }
}
