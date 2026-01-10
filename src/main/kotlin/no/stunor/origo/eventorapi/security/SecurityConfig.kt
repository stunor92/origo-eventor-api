package no.stunor.origo.eventorapi.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security configuration for JWT-based authentication with Supabase.
 *
 * Supports optional authentication:
 * - Invalid tokens on public endpoints are treated as NO token (anonymous access)
 * - Invalid tokens on protected endpoints result in 401 Unauthorized
 *
 * Authentication requirements:
 * - Public: GET /event endpoints (no auth required, invalid tokens ignored)
 * - Optional: GET /event-list endpoints (auth optional, invalid tokens ignored)
 * - Required: /person, /user, and all other /event-list methods (invalid tokens rejected)
 * - Public: Actuator, API docs, Swagger UI
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val optionalJwtAuthenticationManagerResolver: OptionalJwtAuthenticationManagerResolver
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.GET, "/event/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/event-list/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/api-docs/**", "/documentation.html", "/swagger-ui/**").permitAll()
                    .requestMatchers("/person/**", "/user/**").authenticated()
                    .requestMatchers("/event-list/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                // Use custom authentication manager resolver for optional JWT authentication
                // This treats invalid tokens on public endpoints as anonymous (same as no token)
                oauth2.authenticationManagerResolver(optionalJwtAuthenticationManagerResolver)
            }
            // Enable anonymous authentication
            .anonymous { }

        return http.build()
    }
}

