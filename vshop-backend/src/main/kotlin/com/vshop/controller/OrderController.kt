package com.vshop.controller

import com.vshop.dto.CreateOrderRequest
import com.vshop.entity.OrderStatus
import com.vshop.security.AuthUser
import com.vshop.service.OrderService
import com.vshop.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orders: OrderService,
    private val payments: PaymentService,
) {
    /** Step 1: turn the cart into an order (status PENDING_PAYMENT). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal u: AuthUser, @Valid @RequestBody req: CreateOrderRequest) =
        orders.create(u.id, req)

    /** GET /api/orders or /api/orders?status=PAID */
    @GetMapping
    fun list(@AuthenticationPrincipal u: AuthUser, @RequestParam(required = false) status: OrderStatus?) =
        orders.list(u.id, status)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long) = orders.get(u.id, id)

    @PostMapping("/{id}/cancel")
    fun cancel(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long) = orders.cancel(u.id, id)

    /** Step 2: get a KHQR string + ABA Pay deeplink for this order. */
    @PostMapping("/{id}/pay")
    fun pay(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long) = payments.startPayment(u.id, id)

    /** Step 6: the app polls this every 3 seconds while the payment screen is open. */
    @GetMapping("/{id}/payment-status")
    fun paymentStatus(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long) = payments.status(u.id, id)
}
