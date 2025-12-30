package no.stunor.origo.eventorapi.interceptor

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Optional JWT interceptor that allows requests without authentication.
 * If a valid JWT token is provided, the user ID is extracted and set in the request.
 * If no token is provided or the token is invalid, the request continues with uid = null.
 */
@Component
class OptionalJwtInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Value($$"${config.jwt.secret}")
    private lateinit var jwtSecret: String

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val authorizationHeader = request.getHeader("Authorization")

        // If no authorization header is provided, continue without authentication
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header provided for ${request.method} ${request.requestURI}, continuing without authentication")
            request.setAttribute("uid", null)
            return true
        }

        val token = authorizationHeader.removePrefix("Bearer ")
        try {
            val secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
            val claimsJws = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            val uid = claimsJws.payload["sub"]?.toString()

            if (uid.isNullOrEmpty()) {
                log.debug("JWT token missing required claim for ${request.method} ${request.requestURI}, continuing without authentication")
                request.setAttribute("uid", null)
                return true
            }

            log.debug("Successfully authenticated user: $uid for ${request.method} ${request.requestURI}")
            request.setAttribute("uid", uid)
            return true
        } catch (e: JwtException) {
            log.debug("JWT validation failed for ${request.method} ${request.requestURI}: ${e.javaClass.simpleName}, continuing without authentication")
            request.setAttribute("uid", null)
            return true
        } catch (e: Exception) {
            log.warn("Unexpected error processing JWT for ${request.method} ${request.requestURI}: ${e.javaClass.simpleName}, continuing without authentication")
            request.setAttribute("uid", null)
            return true
        }
    }
}

