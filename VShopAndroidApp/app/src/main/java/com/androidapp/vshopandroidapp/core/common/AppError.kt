package com.androidapp.vshopandroidapp.core.common

sealed class AppError {
    data class Validation(val fieldErrors: Map<String, String>) : AppError()
    data class Unauthorized(val message: String) : AppError()
    data class Conflict(val message: String) : AppError()
    data class NotFound(val message: String) : AppError()
    data class TooManyRequests(val message: String) : AppError()
    data class BadRequest(val message: String) : AppError()
    object NoInternet : AppError()
    object Timeout : AppError()
    data class Unknown(val message: String) : AppError()

    fun userMessage(): String = when (this) {
        is AppError.Validation -> fieldErrors.values.firstOrNull() ?: "Please check your input"
        is AppError.Unauthorized -> message
        is AppError.Conflict -> message
        is AppError.NotFound -> message
        is AppError.TooManyRequests -> message
        is AppError.BadRequest -> message
        is AppError.NoInternet -> "No internet connection. Please check your network"
        is AppError.Timeout -> "Request timed out. Please try again"
        is AppError.Unknown -> message
    }
}