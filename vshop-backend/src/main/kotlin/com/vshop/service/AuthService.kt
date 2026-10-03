package com.vshop.service

import com.vshop.config.JwtProperties
import com.vshop.config.OtpProperties
import com.vshop.dto.AuthTokensResponse
import com.vshop.dto.LoginRequest
import com.vshop.dto.LogoutRequest
import com.vshop.dto.MeResponse
import com.vshop.dto.PasswordResetConfirmDto
import com.vshop.dto.PasswordResetRequestDto
import com.vshop.dto.PasswordResetVerifyDto
import com.vshop.dto.RefreshRequest
import com.vshop.dto.RegisterRequest
import com.vshop.dto.RegisterResponse
import com.vshop.dto.UpdateProfileRequest
import com.vshop.dto.UserResponse
import com.vshop.dto.toResponse
import com.vshop.entity.PasswordResetOtp
import com.vshop.entity.RefreshToken
import com.vshop.entity.User
import com.vshop.exception.InvalidOtpException
import com.vshop.exception.conflict
import com.vshop.exception.notFound
import com.vshop.exception.tooManyRequests
import com.vshop.exception.unauthorized
import com.vshop.repository.PasswordResetOtpRepository
import com.vshop.repository.RefreshTokenRepository
import com.vshop.repository.UserRepository
import com.vshop.security.JwtService
import com.vshop.security.SecureTokens
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val otps: PasswordResetOtpRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
    private val otpSender: OtpSender,
    private val jwtProps: JwtProperties,
    private val otpProps: OtpProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ---------- Register (no login: the app calls /login next) ----------

    @Transactional
    fun register(req: RegisterRequest): RegisterResponse {
        val email = normalize(req.email)
        if (users.existsByEmailIgnoreCase(email)) throw conflict("An account with email '$email' already exists")

        // The app no longer asks for a name. Use the email's local part until the user edits their profile.
        val fullName = email.substringBefore('@').ifBlank { email }.take(100)
        val user = users.save(User(fullName, email, null, encoder.encode(req.password)))
        log.info("Registered new user {}", user.id)
        return RegisterResponse(user.id!!.toString(), user.email)
    }

    // ---------- Login ----------

    @Transactional
    fun login(req: LoginRequest): AuthTokensResponse {
        val user = users.findByEmailIgnoreCase(normalize(req.email))
        // Same message for wrong email or wrong password, so nobody can guess which emails exist.
        if (user == null || !encoder.matches(req.password, user.passwordHash)) {
            throw unauthorized("Invalid email or password")
        }
        return issueTokens(user)
    }

    // ---------- Refresh (rotating) ----------

    @Transactional
    fun refresh(req: RefreshRequest): AuthTokensResponse {
        val stored = refreshTokens.findByTokenHash(SecureTokens.sha256(req.refreshToken))
        if (stored == null || stored.revoked || stored.expiryDate.isBefore(Instant.now())) {
            throw unauthorized("Refresh token is invalid, expired, or revoked")
        }
        // Rotate: the old refresh token stops working, a brand new pair is issued.
        stored.revoked = true
        return issueTokens(stored.user)
    }

    // ---------- Logout (idempotent) ----------

    @Transactional
    fun logout(req: LogoutRequest) {
        refreshTokens.findByTokenHash(SecureTokens.sha256(req.refreshToken))?.let { it.revoked = true }
        // No error if the token is unknown or already revoked.
    }

    // ---------- Me ----------

    @Transactional(readOnly = true)
    fun me(userId: Long): MeResponse {
        val user = users.findById(userId).orElseThrow { notFound("User") }
        return MeResponse(user.id!!.toString(), user.email)
    }

    // ---------- Password reset: request ----------

    /** Returns normally for unknown emails, so nobody can find out who has an account. */
    @Transactional
    fun requestPasswordReset(req: PasswordResetRequestDto) {
        val email = normalize(req.email)
        val user = users.findByEmailIgnoreCase(email) ?: run {
            log.info("Password reset requested for an email with no account")
            return
        }
        val now = Instant.now()
        if (otps.countCreatedSince(user.id!!, now.minus(1, ChronoUnit.HOURS)) >= otpProps.maxRequestsPerHour) {
            throw tooManyRequests("Too many password reset requests. Please try again later")
        }

        otps.findActive(user.id!!).forEach { it.usedAt = now }   // only the newest code works
        val code = SecureTokens.newOtp()
        otps.save(PasswordResetOtp(user, encoder.encode(code), now.plus(otpProps.expiryMinutes, ChronoUnit.MINUTES)))
        otpSender.send(user.email, user.fullName, code)
    }

    // ---------- Password reset: verify ----------

    /**
     * Checks a code WITHOUT consuming it, so the app can say "wrong code" before asking for the new password.
     * noRollbackFor: a wrong guess increments attempts and then throws; without it the increment
     * would be rolled back and the attempt limit would never apply.
     */
    @Transactional(noRollbackFor = [InvalidOtpException::class])
    fun verifyPasswordResetOtp(req: PasswordResetVerifyDto) {
        val otp = checkOtp(normalize(req.email), req.otp)
        otp.verifiedAt = Instant.now()
    }

    // ---------- Password reset: confirm ----------

    @Transactional(noRollbackFor = [InvalidOtpException::class])
    fun confirmPasswordReset(req: PasswordResetConfirmDto) {
        val otp = checkOtp(normalize(req.email), req.otp)
        val user = otp.user
        val now = Instant.now()

        user.passwordHash = encoder.encode(req.newPassword)
        otp.usedAt = now
        // Resetting the password logs the user out everywhere.
        refreshTokens.revokeAllForUser(user.id!!)
        log.info("Password reset completed for user {}", user.id)
    }

    // ---------- Helpers ----------

    /**
     * Returns the newest unused code for [email] if [code] matches it, otherwise throws InvalidOtpException.
     * A mismatch counts as a failed attempt. Never marks the code used: the caller decides.
     */
    private fun checkOtp(email: String, code: String): PasswordResetOtp {
        val user = users.findByEmailIgnoreCase(email) ?: throw InvalidOtpException()
        val otp = otps.findActive(user.id!!).firstOrNull() ?: throw InvalidOtpException()

        if (otp.isExpired()) throw InvalidOtpException()
        if (otp.attempts >= otpProps.maxVerifyAttempts) {
            throw InvalidOtpException("Too many incorrect attempts. Please request a new code")
        }
        if (!encoder.matches(code, otp.codeHash)) {
            otp.attempts += 1
            otps.save(otp)
            throw InvalidOtpException()
        }
        return otp
    }

    private fun issueTokens(user: User): AuthTokensResponse {
        val userId = user.id ?: throw IllegalStateException("User has no id")
        val rawRefreshToken = SecureTokens.newRefreshToken()
        refreshTokens.save(
            RefreshToken(
                user = user,
                tokenHash = SecureTokens.sha256(rawRefreshToken),
                expiryDate = Instant.now().plus(jwtProps.refreshTokenExpiryDays, ChronoUnit.DAYS),
            )
        )
        return AuthTokensResponse(
            accessToken = jwt.generateAccessToken(userId, user.email),
            refreshToken = rawRefreshToken,
            expiresInSeconds = jwt.accessTokenExpirySeconds(),
        )
    }

    private fun normalize(email: String) = email.trim().lowercase()

    // ---------- Profile (/api/me) ----------

    @Transactional(readOnly = true)
    fun profile(userId: Long) = users.findById(userId).orElseThrow { notFound("User") }.toResponse()

    @Transactional
    fun updateProfile(userId: Long, req: UpdateProfileRequest): UserResponse {
        val user = users.findById(userId).orElseThrow { notFound("User") }
        user.fullName = req.fullName.trim()
        user.phone = req.phone?.trim()
        return user.toResponse()
    }
}
