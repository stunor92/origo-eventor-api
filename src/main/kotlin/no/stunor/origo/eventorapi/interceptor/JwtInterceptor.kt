package no.stunor.origo.eventorapi.interceptor

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor



@Component
class JwtInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Value($$"${config.jwt.secret}")
    private lateinit var jwtSecret: String

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val authorizationHeader = request.getHeader("Authorization")
        
        // Since this interceptor is only applied to protected paths, we require authentication
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for ${request.method} ${request.requestURI}")
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.write("Authentication required")
            return false
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
                log.warn("JWT token missing required claim for ${request.method} ${request.requestURI}")
                response.status = HttpStatus.UNAUTHORIZED.value()
                response.writer.write("Invalid authentication token")
                return false
            }
            
            request.setAttribute("uid", uid)
            return true
        } catch (e: JwtException) {
            log.warn("JWT validation failed for ${request.method} ${request.requestURI}: ${e.javaClass.simpleName}")
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.write("Authentication failed")
            return false
        } catch (e: Exception) {
            log.error("Unexpected error processing JWT for ${request.method} ${request.requestURI}: ${e.javaClass.simpleName}", e)
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.writer.write("Authentication failed")
            return false
        }
    }
}