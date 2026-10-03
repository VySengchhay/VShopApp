package com.vshop.dto

import java.time.Instant

/** Error body for every non-2xx response. fieldErrors is only present for validation errors (400). */
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null,
)
