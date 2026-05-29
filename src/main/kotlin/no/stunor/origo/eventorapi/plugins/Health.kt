package no.stunor.origo.eventorapi.plugins

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.stunor.origo.eventorapi.Dependencies
import org.slf4j.LoggerFactory
import java.sql.Connection

private val log = LoggerFactory.getLogger("HealthPlugin")

/**
 * Configures health check endpoints for monitoring and orchestration.
 *
 * Endpoints:
 * - GET /actuator/health - Overall health status (includes DB check)
 * - GET /actuator/health/liveness - Liveness probe (app is running)
 * - GET /actuator/health/readiness - Readiness probe (app is ready to serve traffic)
 */
fun Application.configureHealth(deps: Dependencies) {
    routing {
        route("/actuator/health") {
            // Overall health - checks all dependencies
            get {
                val health = checkHealth(deps)
                val status = if (health["status"] == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                call.respond(status, health)
            }

            // Liveness probe - is the app alive?
            get("/liveness") {
                call.respond(mapOf("status" to "UP"))
            }

            // Readiness probe - is the app ready to serve traffic?
            get("/readiness") {
                val dbHealth = checkDatabaseHealth(deps)
                val status = if (dbHealth["status"] == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                call.respond(status, mapOf(
                    "status" to dbHealth["status"],
                    "components" to mapOf("database" to dbHealth)
                ))
            }
        }
    }
}

private fun checkHealth(deps: Dependencies): Map<String, Any?> {
    val dbHealth = checkDatabaseHealth(deps)
    val overallStatus = if (dbHealth["status"] == "UP") "UP" else "DOWN"

    return mapOf(
        "status" to overallStatus,
        "components" to mapOf(
            "database" to dbHealth
        )
    )
}

private fun checkDatabaseHealth(deps: Dependencies): Map<String, Any?> {
    return try {
        val dataSource = deps.dataSource as HikariDataSource

        // Try to get a connection and execute a simple query
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT 1").use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next() && rs.getInt(1) == 1) {
                        mapOf(
                            "status" to "UP",
                            "details" to mapOf(
                                "database" to "PostgreSQL",
                                "activeConnections" to dataSource.hikariPoolMXBean.activeConnections,
                                "totalConnections" to dataSource.hikariPoolMXBean.totalConnections,
                                "maxPoolSize" to dataSource.maximumPoolSize
                            )
                        )
                    } else {
                        mapOf(
                            "status" to "DOWN",
                            "error" to "Query returned unexpected result"
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        log.error("Database health check failed", e)
        mapOf(
            "status" to "DOWN",
            "error" to (e.message ?: "Unknown error")
        )
    }
}


