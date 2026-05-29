package no.stunor.origo.eventorapi.plugins

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import no.stunor.origo.eventorapi.Dependencies
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DatabasePlugin")

fun Application.configureDatabase(deps: Dependencies) {
    log.info("Database connection pool initialized: ${(deps.dataSource as? HikariDataSource)?.jdbcUrl}")
}

fun Application.configureShutdownHook(deps: Dependencies) {
    monitor.subscribe(ApplicationStopped) {
        log.info("Application stopping – closing HTTP client and DB pool")
        deps.close()
    }
}
