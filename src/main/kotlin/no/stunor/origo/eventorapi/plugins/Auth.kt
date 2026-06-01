package no.stunor.origo.eventorapi.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("Auth")

fun Application.configureAuth(config: ApplicationConfig) {
    val projectRef = config.property("app.supabaseProjectRef").getString()
    val jwksUri    = "https://$projectRef.supabase.co/auth/v1/.well-known/jwks.json"
    val issuer     = "https://$projectRef.supabase.co/auth/v1"

    log.info("JWT issuer: {}", issuer)
    log.info("JWT JWKS:   {}", jwksUri)

    val jwkProvider = JwkProviderBuilder(URI(jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        // jwt-optional: missing or invalid token → principal is null, request proceeds
        jwt("jwt-optional") {
            realm = "origo-eventor-api"
            verifier(jwkProvider, issuer) { acceptLeeway(3) }
            validate { credential -> JWTPrincipal(credential.payload) }
            // Silent challenge: do not respond with 401; route sees null principal
            challenge { _, _ -> }
        }

        // jwt-required: missing or invalid token → 401 Unauthorized
        jwt("jwt-required") {
            realm = "origo-eventor-api"
            verifier(jwkProvider, issuer) { acceptLeeway(3) }
            validate { credential -> JWTPrincipal(credential.payload) }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
    }
}
