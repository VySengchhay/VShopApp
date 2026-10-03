package com.vshop.security

import com.vshop.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Runs without a database: ./gradlew test */
class JwtServiceTest {

    private val secret = "test-secret-test-secret-test-secret-1234"
    private val jwt = JwtService(JwtProperties(secret = secret, accessTokenExpiryMinutes = 15, refreshTokenExpiryDays = 30))

    @Test
    fun `access token round-trips to the same user`() {
        val token = jwt.generateAccessToken(42L, "vy@example.com")
        assertEquals(AuthUser(42L, "vy@example.com"), jwt.parse(token))
    }

    @Test
    fun `expiresInSeconds follows the configured minutes`() {
        assertEquals(900L, jwt.accessTokenExpirySeconds())
    }

    @Test
    fun `expired token is rejected`() {
        val expired = JwtService(JwtProperties(secret = secret, accessTokenExpiryMinutes = -1))
            .generateAccessToken(42L, "vy@example.com")
        assertNull(jwt.parse(expired))
    }

    @Test
    fun `token signed with another secret is rejected`() {
        val other = JwtService(JwtProperties(secret = "another-secret-another-secret-another-1"))
            .generateAccessToken(42L, "vy@example.com")
        assertNull(jwt.parse(other))
    }

    @Test
    fun `garbage and non-access tokens are rejected`() {
        assertNull(jwt.parse("not-a-jwt"))

        // e.g. an old password-reset token: right key, wrong purpose
        val resetToken = Jwts.builder()
            .subject("42")
            .claim("purpose", "reset")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()
        assertNull(jwt.parse(resetToken))
    }
}
