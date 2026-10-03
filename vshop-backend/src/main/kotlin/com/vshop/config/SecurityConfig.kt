package com.vshop.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.vshop.dto.ApiError
import com.vshop.security.JwtAuthFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtFilter: JwtAuthFilter,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }                       // Stateless JWT API, no cookies
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // Only the public auth endpoints. /api/auth/me falls through to authenticated() below.
                it.requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/password-reset/**",
                ).permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                it.requestMatchers("/api/payway/callback").permitAll()   // PayWay calls this, it has no JWT
                it.requestMatchers("/api/dev/**").permitAll()            // Only exists when payway.mock=true
                it.requestMatchers("/actuator/health").permitAll()
                it.requestMatchers("/error").permitAll()                 // don't turn server errors into 401s
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                // 401 = no / expired / invalid access token. The Android app refreshes and retries only on 401.
                it.authenticationEntryPoint { req, res, _ -> writeError(req, res, HttpStatus.UNAUTHORIZED, "Authentication required") }
                // 403 = logged in, but not allowed.
                it.accessDeniedHandler { req, res, _ -> writeError(req, res, HttpStatus.FORBIDDEN, "Access denied") }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /** Same JSON shape as GlobalExceptionHandler, so the app parses every error the same way. */
    private fun writeError(req: HttpServletRequest, res: HttpServletResponse, status: HttpStatus, message: String) {
        res.status = status.value()
        res.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(res.outputStream, ApiError(status.value(), status.reasonPhrase, message, req.requestURI))
    }
}
