package no.stunor.origo.eventorapi

import io.ktor.server.netty.*
import io.ktor.server.application.*
import no.stunor.origo.eventorapi.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val deps = Dependencies(environment.config)
    configureDatabase(deps)
    configureSerialization()
    configureAuth(environment.config)
    configureStatusPages()
    configureHealth(deps)
    configureRouting(deps)
    configureShutdownHook(deps)
}
