package no.stunor.origo.eventorapi

import io.ktor.server.application.*
import io.ktor.server.netty.*
import no.stunor.origo.eventorapi.di.appModule
import no.stunor.origo.eventorapi.plugins.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment.config))
    }
    configureDatabase(environment.config)
    configureSerialization()
    configureAuth(environment.config)
    configureStatusPages()
    configureRouting()
    configureShutdownHook()
}