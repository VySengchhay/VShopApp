package com.vshop.security

import com.vshop.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey
import org.springframework.stereotype.Service

/**
 * Short-lived access tokens (JWT, default 15 minutes).
 * Long-lived sessions use opaque refresh tokens instead (see AuthService + refresh_tokens table).
 */
@Service
class JwtService(private val props: JwtProperties) {

    private val key: SecretKey by lazy {
        require(props.secret.length >= 32) { "app.jwt.secret (JWT_SECRET) must be at least 32 characters" }
        Keys.hmacShaKeyFor(props.secret.toByteArray())
    }

    fun generateAccessToken(userId: Long, email: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("purpose", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenExpirySeconds())))
            .signWith(key)
            .compact()
    }

    /** Sent to the app as expiresInSeconds. */
    fun accessTokenExpirySeconds(): Long = props.accessTokenExpiryMinutes * 60

    /** Returns the user if the token is a valid access token, or null if it's expired, tampered with, or not an access token. */
    fun parse(token: String): AuthUser? = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        if (claims["purpose"] != "access") null
        else AuthUser(claims.subject.toLong(), claims["email"] as String)
    } catch (e: Exception) {
        null
    }
}
