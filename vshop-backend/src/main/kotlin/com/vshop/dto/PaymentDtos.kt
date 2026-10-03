package com.vshop.dto

import com.vshop.entity.OrderStatus
import com.vshop.entity.PaymentStatus
import java.math.BigDecimal
import java.time.Instant

/** What the Android payment screen needs. */
data class PaymentResponse(
    val orderId: Long,
    val orderNo: String,
    val tranId: String,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val currency: String,
    val qrString: String?,        // draw this as a QR code on the phone (e.g. with ZXing)
    val abapayDeeplink: String?,  // open with Intent.ACTION_VIEW to launch ABA Mobile
    val checkoutQrUrl: String?,
    val expiresAt: Instant,
)

/** What the app gets when it polls. */
data class PaymentStatusResponse(
    val orderId: Long,
    val orderNo: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus?,
    val tranId: String?,
    val amount: BigDecimal,
    val currency: String,
    val apv: String?,
    val paidAt: Instant?,
    val expiresAt: Instant?,
)
