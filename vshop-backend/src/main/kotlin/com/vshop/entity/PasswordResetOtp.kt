package com.vshop.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// Password reset OTP (POST /api/auth/password-reset/{request,verify,confirm}).
// Logic: service/AuthService. Sender: service/OtpSender.

/**
 * One row per "password-reset/request" for a registered email.
 * The 6-digit code itself is never stored, only its BCrypt hash.
 * "Used" = used_at is set (password changed, or replaced by a newer code).
 */
@Entity
@Table(name = "password_reset_otps")
class PasswordResetOtp(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User,

    @Column(name = "code_hash", nullable = false)
    var codeHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /** Wrong codes entered so far. */
    @Column(nullable = false)
    var attempts: Int = 0

    /** Last time the right code was checked with /verify (informational; verify doesn't consume the code). */
    @Column(name = "verified_at") var verifiedAt: Instant? = null
    @Column(name = "used_at") var usedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    fun isExpired(now: Instant = Instant.now()) = now.isAfter(expiresAt)
}
