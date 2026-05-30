package no.stunor.origo.eventorapi

import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.netty.*
import io.ktor.server.application.*
import no.stunor.origo.eventorapi.plugins.*

fun main(args: Array<String>) {
    dotenv { ignoreIfMissing = true }.entries().forEach { System.setProperty(it.key, it.value) }
    EngineMain.main(args)
}

fun Application.module() {
    val deps = Dependencies(environment.config)
    configureMetrics()
    configureDatabase(deps)
    configureSerialization()
    configureAuth(environment.config)
    configureStatusPages()
    configureHealth(deps)
    configureRouting(deps)
    configureShutdownHook(deps)
}
