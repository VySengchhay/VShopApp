package com.vshop.exception

import org.springframework.http.HttpStatus

/** Throw this anywhere to return a clean JSON error to the app. Open so specific errors can extend it. */
open class ApiException(val status: HttpStatus, override val message: String) : RuntimeException(message)

fun notFound(what: String) = ApiException(HttpStatus.NOT_FOUND, "$what not found")

fun badRequest(message: String) = ApiException(HttpStatus.BAD_REQUEST, message)

fun conflict(message: String) = ApiException(HttpStatus.CONFLICT, message)

fun unauthorized(message: String) = ApiException(HttpStatus.UNAUTHORIZED, message)

fun tooManyRequests(message: String) = ApiException(HttpStatus.TOO_MANY_REQUESTS, message)

/** 400. A wrong code must not roll back the attempts counter, so services list it in noRollbackFor. */
class InvalidOtpException(message: String = "Invalid or expired OTP") : ApiException(HttpStatus.BAD_REQUEST, message)
