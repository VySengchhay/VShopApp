package com.vshop.controller

import com.vshop.payway.CheckResult
import com.vshop.payway.MockPayWayClient
import com.vshop.service.PaymentService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Developer helpers. Only exist in mock mode (PAYWAY_MOCK=true).
 * Lets you finish a payment from Postman while testing the Android app.
 */
@RestController
@RequestMapping("/api/dev/payments")
@ConditionalOnProperty(name = ["payway.mock"], havingValue = "true", matchIfMissing = true)
class DevPaymentController(
    private val mock: MockPayWayClient,
    private val payments: PaymentService,
) {
    @PostMapping("/{tranId}/approve")
    fun approve(@PathVariable tranId: String) = set(tranId, CheckResult.State.APPROVED)

    @PostMapping("/{tranId}/decline")
    fun decline(@PathVariable tranId: String) = set(tranId, CheckResult.State.DECLINED)

    private fun set(tranId: String, state: CheckResult.State): ResponseEntity<Map<String, String>> {
        if (!mock.setState(tranId, state)) {
            return ResponseEntity.status(404).body(mapOf("message" to "Unknown tran_id in mock (did the server restart?)"))
        }
        payments.handleCallback(tranId, """{"mock_callback":true,"state":"$state"}""")   // same path as a real callback
        return ResponseEntity.ok(mapOf("tranId" to tranId, "state" to state.name))
    }
}
