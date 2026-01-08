package no.stunor.origo.eventorapi.controller

import jakarta.servlet.http.HttpServletRequest
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.services.PersonService
import no.stunor.origo.eventorapi.validation.InputValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("person")
internal class PersonController {

    @Autowired
    private lateinit var personService: PersonService

    @Autowired
    private lateinit var inputValidator: InputValidator

    @PostMapping("/{eventorId}")
    fun HttpServletRequest.authenticate(
        @PathVariable eventorId: String,
        @RequestHeader(value = "username") username: String,
        @RequestHeader(value = "password") password: String
    ): ResponseEntity<Person> {
        val uid = UUID.fromString(getAttribute("uid") as String)

        // Validate inputs to prevent SSRF attacks
        val validatedEventorId = inputValidator.validateEventorId(eventorId)
        val validatedUsername = inputValidator.validateUsername(username)

        return ResponseEntity(
            personService.authenticate(
                eventorId = validatedEventorId,
                username = validatedUsername,
                password = password,
                userId = uid
            ), HttpStatus.OK
        )
    }
}
