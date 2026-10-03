package com.vshop.security

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Runs without a database: ./gradlew test */
class SecureTokensTest {

    @Test
    fun `sha256 is standard Base64 of the digest`() {
        // Known SHA-256 test vectors ("abc" and the empty string).
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", SecureTokens.sha256("abc"))
        assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", SecureTokens.sha256(""))
    }

    @Test
    fun `refresh token is 64 random bytes in base64url without padding`() {
        val token = SecureTokens.newRefreshToken()
        assertEquals(86, token.length)
        assertTrue(token.matches(Regex("^[A-Za-z0-9_-]+$")), "not base64url: $token")
        assertNotEquals(token, SecureTokens.newRefreshToken())
        assertEquals(44, SecureTokens.sha256(token).length)   // fits token_hash VARCHAR(255)
    }

    @Test
    fun `otp is always 6 digits`() {
        repeat(1_000) {
            val otp = SecureTokens.newOtp()
            assertTrue(otp.matches(Regex("^\\d{6}$")), "bad otp: $otp")
        }
    }
}
