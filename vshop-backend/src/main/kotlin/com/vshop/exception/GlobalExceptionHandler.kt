package com.vshop.exception

import com.vshop.dto.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException, req: HttpServletRequest) =
        build(e.status, e.message, req)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val fields = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleBadJson(e: HttpMessageNotReadableException, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "Request body is missing or not valid JSON", req)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, MissingServletRequestParameterException::class)
    fun handleBadParam(e: Exception, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "A request parameter is missing or has the wrong type", req)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoRoute(e: NoResourceFoundException, req: HttpServletRequest) =
        build(HttpStatus.NOT_FOUND, "No endpoint at this URL", req)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethod(e: HttpRequestMethodNotSupportedException, req: HttpServletRequest) =
        build(HttpStatus.METHOD_NOT_ALLOWED, "${e.method} is not supported here", req)

    @ExceptionHandler(Exception::class)
    fun handleOther(e: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unexpected error on {} {}", req.method, req.requestURI, e)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server", req)
    }

    private fun build(
        status: HttpStatus,
        message: String,
        req: HttpServletRequest,
        fields: Map<String, String>? = null,
    ) = ResponseEntity.status(status).body(
        ApiError(status.value(), status.reasonPhrase, message, req.requestURI, fieldErrors = fields)
    )
}
