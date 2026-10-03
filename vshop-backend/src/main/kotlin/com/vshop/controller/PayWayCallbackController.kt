package com.vshop.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.vshop.exception.badRequest
import com.vshop.service.PaymentService
import jakarta.servlet.http.HttpServletRequest
import java.net.URLDecoder
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PayWay calls this URL (return_url) when a payment finishes.
 * It must be reachable from the internet -> use ngrok while developing.
 *
 * We only use the callback as a signal. The real status always comes from
 * PayWay's check-transaction API, so a fake callback can't mark an order paid.
 */
@RestController
@RequestMapping("/api/payway")
class PayWayCallbackController(
    private val payments: PaymentService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/callback")
    fun callback(req: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val contentType = req.contentType.orEmpty().lowercase()
        val values = mutableMapOf<String, String>()
        val raw: String

        if (contentType.startsWith("multipart/") || contentType.startsWith("application/x-www-form-urlencoded")) {
            req.parameterMap.forEach { (k, v) -> values[k] = v.firstOrNull().orEmpty() }
            raw = objectMapper.writeValueAsString(values)
        } else {
            raw = String(req.inputStream.readAllBytes(), Charsets.UTF_8)
            try {
                objectMapper.readTree(raw).fields().forEach { (k, v) -> values[k] = if (v.isValueNode) v.asText() else v.toString() }
            } catch (e: Exception) {
                // Not JSON; try key=value&key=value
                raw.split("&").mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 } }
                    .forEach { (k, v) -> values[k] = URLDecoder.decode(v, Charsets.UTF_8) }
            }
            req.parameterMap.forEach { (k, v) -> values.putIfAbsent(k, v.firstOrNull().orEmpty()) }
        }

        log.info("PayWay callback: {}", raw.take(1000))
        val tranId = values["tran_id"] ?: values["merchant_ref"] ?: values["tranId"]
        if (tranId.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to "tran_id missing"))
        }

        payments.handleCallback(tranId, raw)
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
