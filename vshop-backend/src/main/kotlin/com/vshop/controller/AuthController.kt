package com.vshop.controller

import com.vshop.dto.AuthTokensResponse
import com.vshop.dto.GenericMessageResponse
import com.vshop.dto.LoginRequest
import com.vshop.dto.LogoutRequest
import com.vshop.dto.MeResponse
import com.vshop.dto.PasswordResetConfirmDto
import com.vshop.dto.PasswordResetRequestDto
import com.vshop.dto.PasswordResetVerifyDto
import com.vshop.dto.RefreshRequest
import com.vshop.dto.RegisterRequest
import com.vshop.dto.RegisterResponse
import com.vshop.security.AuthUser
import com.vshop.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/** Public except /me (see SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ResponseEntity<RegisterResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req))

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<AuthTokensResponse> =
        ResponseEntity.ok(auth.login(req))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshRequest): ResponseEntity<AuthTokensResponse> =
        ResponseEntity.ok(auth.refresh(req))

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody req: LogoutRequest): ResponseEntity<GenericMessageResponse> {
        auth.logout(req)
        return ResponseEntity.ok(GenericMessageResponse("Logged out"))
    }

    @PostMapping("/password-reset/request")
    fun requestPasswordReset(@Valid @RequestBody req: PasswordResetRequestDto): ResponseEntity<GenericMessageResponse> {
        auth.requestPasswordReset(req)
        // Same answer whether or not the email exists (no user enumeration).
        return ResponseEntity.ok(GenericMessageResponse("If that email is registered, a reset code has been sent"))
    }

    @PostMapping("/password-reset/verify")
    fun verifyPasswordResetOtp(@Valid @RequestBody req: PasswordResetVerifyDto): ResponseEntity<GenericMessageResponse> {
        auth.verifyPasswordResetOtp(req)
        return ResponseEntity.ok(GenericMessageResponse("Code verified"))
    }

    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(@Valid @RequestBody req: PasswordResetConfirmDto): ResponseEntity<GenericMessageResponse> {
        auth.confirmPasswordReset(req)
        return ResponseEntity.ok(GenericMessageResponse("Password has been reset successfully"))
    }

    /** Needs a valid access token. */
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AuthUser): ResponseEntity<MeResponse> =
        ResponseEntity.ok(auth.me(user.id))
}
