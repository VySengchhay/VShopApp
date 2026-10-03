package com.vshop.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Random values for refresh tokens and reset codes, and the hash used to store refresh tokens. */
object SecureTokens {
    private val random = SecureRandom()

    /** 64 random bytes, base64url without padding (86 characters). Only its hash is stored. */
    fun newRefreshToken(): String {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** 6-digit numeric code, zero-padded ("004217"). */
    fun newOtp(): String = random.nextInt(1_000_000).toString().padStart(6, '0')

    /** SHA-256 of the UTF-8 text, standard Base64 (44 characters). */
    fun sha256(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}
