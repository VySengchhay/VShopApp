package com.vshop.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

enum class PaymentStatus { PENDING, APPROVED, DECLINED, CANCELLED, EXPIRED, FAILED }

@Entity
@Table(name = "payments")
class Payment(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    var order: ShopOrder,

    @Column(name = "tran_id", nullable = false, unique = true)
    var tranId: String,

    @Column(name = "payment_option") var paymentOption: String,
    var amount: BigDecimal,
    var currency: String,

    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "expires_at") var expiresAt: Instant,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var apv: String? = null
    @Column(name = "qr_string") var qrString: String? = null
    @Column(name = "abapay_deeplink") var abapayDeeplink: String? = null
    @Column(name = "checkout_qr_url") var checkoutQrUrl: String? = null

    @Column(name = "request_payload") var requestPayload: String? = null
    @Column(name = "response_payload") var responsePayload: String? = null
    @Column(name = "callback_payload") var callbackPayload: String? = null

    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now()
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now()
    @Column(name = "paid_at") var paidAt: Instant? = null

    fun isExpired(now: Instant = Instant.now()) = now.isAfter(expiresAt)
}
