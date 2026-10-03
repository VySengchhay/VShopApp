package com.vshop.dto

import com.vshop.entity.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

// DTOs (JSON field names are part of the contract with the Android app)

private const val PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$"

private const val PASSWORD_PATTERN_MESSAGE = "Password must contain at least one letter and one number"

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 150)   // users.email is VARCHAR(150)
    val email: String,

    // 8-72 chars (BCrypt only uses the first 72 bytes), at least one letter and one number.
    @field:NotBlank
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @field:Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_MESSAGE)
    val password: String,
)

data class RegisterResponse(val id: String, val email: String)

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
)

data class RefreshRequest(@field:NotBlank val refreshToken: String)

data class LogoutRequest(@field:NotBlank val refreshToken: String)

data class PasswordResetRequestDto(@field:NotBlank @field:Email val email: String)

data class PasswordResetVerifyDto(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 6, max = 6) val otp: String,
)

data class PasswordResetConfirmDto(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 6, max = 6) val otp: String,
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    @field:Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_PATTERN_MESSAGE)
    val newPassword: String,
)

data class MeResponse(val id: String, val email: String)

data class GenericMessageResponse(val message: String)

// Profile (/api/me). Not used by the auth flow; fullName and phone are sent to PayWay at checkout.
data class UpdateProfileRequest(
    @field:NotBlank @field:Size(max = 100) val fullName: String,
    @field:Pattern(regexp = "^(\\+855|0)[1-9][0-9]{7,8}$", message = "Enter a valid Cambodian phone number")
    val phone: String?,
)

data class UserResponse(val id: Long, val fullName: String, val email: String, val phone: String?, val createdAt: Instant)

fun User.toResponse() = UserResponse(id!!, fullName, email, phone, createdAt)
