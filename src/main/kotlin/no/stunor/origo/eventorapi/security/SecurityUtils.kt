package no.stunor.origo.eventorapi.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Utility to extract authenticated user information from the security context.
 */
@Component
class SecurityUtils {

    companion object {
        /**
         * Get the currently authenticated user's ID (from JWT 'sub' claim).
         * Returns null if the user is not authenticated or authentication is anonymous.
         */
        fun getCurrentUserId(): String? {
            val authentication = SecurityContextHolder.getContext().authentication

            return when {
                authentication == null -> null
                !authentication.isAuthenticated -> null
                authentication.principal == "anonymousUser" -> null
                authentication is JwtAuthenticationToken -> authentication.name // 'sub' claim
                else -> authentication.name
            }
        }

    }
}

