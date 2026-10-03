package com.vshop.controller

import com.vshop.dto.UpdateProfileRequest
import com.vshop.security.AuthUser
import com.vshop.service.AuthService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Profile: name and phone are used as the payer details sent to PayWay. */
@RestController
@RequestMapping("/api/me")
class MeController(private val auth: AuthService) {
    @GetMapping
    fun me(@AuthenticationPrincipal user: AuthUser) = auth.profile(user.id)

    @PutMapping
    fun update(@AuthenticationPrincipal user: AuthUser, @Valid @RequestBody req: UpdateProfileRequest) =
        auth.updateProfile(user.id, req)
}
