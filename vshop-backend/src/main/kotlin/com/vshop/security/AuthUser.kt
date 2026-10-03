package com.vshop.security

/** The logged-in user, available in controllers via @AuthenticationPrincipal. */
data class AuthUser(val id: Long, val email: String)
