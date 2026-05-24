package no.stunor.origo.eventorapi.plugins

import io.ktor.server.application.*
import io.ktor.server.config.*
import com.zaxxer.hikari.HikariDataSource
import no.stunor.origo.eventorapi.services.CalendarService
import no.stunor.origo.eventorapi.services.EventService
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("DatabasePlugin")

fun Application.configureDatabase(config: ApplicationConfig) {
    // Eagerly resolve the DataSource so connection pool is created on startup
    val dataSource: DataSource by inject()
    log.info("Database connection pool initialized: ${(dataSource as? HikariDataSource)?.jdbcUrl}")
}

fun Application.configureShutdownHook() {
    val calendarService: CalendarService by inject()
    val eventService:    EventService    by inject()
    val dataSource:      DataSource      by inject()

    environment.monitor.subscribe(ApplicationStopped) {
        log.info("Application stopping – shutting down executor pools and DB pool")
        calendarService.shutdownExecutor()
        eventService.shutdownExecutor()
        (dataSource as? HikariDataSource)?.close()
    }
}
