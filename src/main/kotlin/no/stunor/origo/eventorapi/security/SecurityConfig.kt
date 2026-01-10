package no.stunor.origo.eventorapi.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security configuration for JWT-based authentication with Supabase.
 *
 * Uses Spring Boot's auto-configuration for OAuth2 Resource Server with JWT.
 * The JWT decoder is automatically configured from application.yml properties:
 * - spring.security.oauth2.resourceserver.jwt.jwk-set-uri (RSA)
 *
 * Falls back to HybridJwtDecoder for legacy HMAC support during migration.
 *
 * Authentication requirements:
 * - Public: GET /event endpoints (no authentication required)
 * - Optional: GET /event-list endpoints (authentication optional, provides personalized data if authenticated)
 * - Required: /person, /user, and all other /event-list methods
 * - Public: Actuator, API docs, Swagger UI
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

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
                oauth2.jwt { jwt ->
                    // Spring Boot auto-configures the JWT decoder from application.yml
                    // HybridJwtDecoder is used as @Primary bean if legacy HMAC support is needed
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
                oauth2.authenticationEntryPoint { _, response, authException ->
                    response.status = 401
                    response.contentType = "application/json"
                    response.writer.write("""{"error":"Unauthorized","message":"${authException.message}"}""")
                }
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { _ ->
            listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        }
        return converter
    }
}

