package com.vshop.dto

import com.vshop.entity.OrderStatus
import com.vshop.entity.PaymentStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class CreateOrderRequest(
    @field:NotEmpty(message = "Cart is empty") @field:Size(max = 30) @field:Valid
    val items: List<CartLine>,
    val addressId: Long,
)

/** The app sends only IDs and quantities. Prices always come from the database. */
data class CartLine(
    val productId: Long,
    @field:Min(1) @field:Max(20) val quantity: Int,
)

data class OrderItemResponse(
    val productId: Long, val name: String, val imageUrl: String?,
    val unitPrice: BigDecimal, val quantity: Int, val lineTotal: BigDecimal,
)

data class ShippingResponse(val fullName: String, val phone: String, val address: String)

data class PaymentSummary(val tranId: String, val status: PaymentStatus, val expiresAt: Instant, val paidAt: Instant?)

data class OrderResponse(
    val id: Long,
    val orderNo: String,
    val status: OrderStatus,
    val items: List<OrderItemResponse>,
    val itemCount: Int,
    val subtotal: BigDecimal,
    val deliveryFee: BigDecimal,
    val total: BigDecimal,
    val currency: String,
    val shipping: ShippingResponse,
    val createdAt: Instant,
    val paidAt: Instant?,
    val latestPayment: PaymentSummary?,
)
