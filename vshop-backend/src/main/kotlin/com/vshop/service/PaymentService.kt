package com.vshop.service

import com.vshop.config.PayWayProperties
import com.vshop.dto.PaymentResponse
import com.vshop.dto.PaymentStatusResponse
import com.vshop.dto.toResponse
import com.vshop.entity.OrderStatus
import com.vshop.entity.Payment
import com.vshop.entity.PaymentStatus
import com.vshop.exception.ApiException
import com.vshop.exception.conflict
import com.vshop.exception.notFound
import com.vshop.payway.CheckResult
import com.vshop.payway.PayWayClient
import com.vshop.payway.PurchaseItem
import com.vshop.payway.PurchaseRequest
import com.vshop.repository.OrderRepository
import com.vshop.repository.PaymentRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val orders: OrderRepository,
    private val payments: PaymentRepository,
    private val payway: PayWayClient,
    private val props: PayWayProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Step 2 of the flow: ask PayWay for a KHQR + ABA Pay deeplink for this order.
     * If there's already a fresh unpaid QR, return it instead of creating a second one.
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun startPayment(userId: Long, orderId: Long): PaymentResponse {
        val order = orders.findByIdAndUserId(orderId, userId) ?: throw notFound("Order")
        when (order.status) {
            OrderStatus.PENDING_PAYMENT -> Unit
            OrderStatus.CANCELLED -> throw conflict("This order was cancelled")
            else -> throw conflict("This order is already paid")
        }

        // Re-use the current QR if it's still valid for at least 30 more seconds.
        payments.findAllForOrder(orderId).firstOrNull { it.status == PaymentStatus.PENDING }?.let { existing ->
            refreshLocked(existing)
            if (order.status == OrderStatus.PAID) throw conflict("This order is already paid")
            if (existing.status == PaymentStatus.PENDING && !existing.isExpired(Instant.now().plusSeconds(30))) {
                return existing.toResponse()
            }
            if (existing.status == PaymentStatus.PENDING) {   // about to expire: retire it
                existing.status = PaymentStatus.EXPIRED
                existing.updatedAt = Instant.now()
            }
        }

        val user = order.user
        val (first, last) = splitName(user.fullName)
        val payment = payments.save(
            Payment(
                order = order,
                tranId = newTranId(),
                paymentOption = "abapay_khqr_deeplink",
                amount = order.total,
                currency = order.currency,
                expiresAt = Instant.now().plusSeconds(props.lifetimeMinutes * 60L),
            )
        )

        val result = payway.createPurchase(
            PurchaseRequest(
                tranId = payment.tranId,
                amount = order.total,
                currency = order.currency,
                items = order.items.map { PurchaseItem(it.productName.take(50), it.quantity, it.unitPrice) } +
                    listOfNotNull(order.deliveryFee.takeIf { it.signum() > 0 }?.let { PurchaseItem("Delivery", 1, it) }),
                firstName = first, lastName = last,
                email = user.email.take(50),
                phone = user.phone.orEmpty(),
                lifetimeMinutes = props.lifetimeMinutes,
            )
        )

        payment.requestPayload = result.requestLog
        payment.responsePayload = result.rawResponse
        payment.updatedAt = Instant.now()

        if (!result.success) {
            payment.status = PaymentStatus.FAILED
            log.warn("PayWay purchase failed for order {} tran {}: {}", order.orderNo, payment.tranId, result.errorMessage)
            throw ApiException(HttpStatus.BAD_GATEWAY, "Couldn't start the payment: ${result.errorMessage}")
        }

        payment.qrString = result.qrString
        payment.abapayDeeplink = result.abapayDeeplink
        payment.checkoutQrUrl = result.checkoutQrUrl
        return payment.toResponse()
    }

    /**
     * Step 6 of the flow: the app polls this every few seconds.
     * If the payment is still pending we ask PayWay directly, so it works even
     * when PayWay can't reach our callback URL (e.g. running on localhost without ngrok).
     */
    @Transactional
    fun status(userId: Long, orderId: Long): PaymentStatusResponse {
        val order = orders.findByIdAndUserId(orderId, userId) ?: throw notFound("Order")
        val latest = payments.findAllForOrder(orderId).firstOrNull()
        if (latest != null && latest.status == PaymentStatus.PENDING) refresh(latest.tranId)

        return PaymentStatusResponse(
            orderId = order.id!!, orderNo = order.orderNo, orderStatus = order.status,
            paymentStatus = latest?.status, tranId = latest?.tranId,
            amount = order.total, currency = order.currency,
            apv = latest?.apv, paidAt = order.paidAt, expiresAt = latest?.expiresAt,
        )
    }

    /** Step 4 of the flow: PayWay pushed a result to /api/payway/callback. */
    @Transactional
    fun handleCallback(tranId: String, rawPayload: String): Boolean {
        val p = payments.findByTranIdForUpdate(tranId) ?: run {
            log.warn("Callback for unknown tran_id={}", tranId)
            return false
        }
        p.callbackPayload = rawPayload.take(4000)
        // Never trust the callback body alone: confirm with PayWay's check-transaction API.
        refreshLocked(p)
        return true
    }

    /** Lock the payment row and sync its status with PayWay. Safe to call many times. */
    @Transactional
    fun refresh(tranId: String) {
        payments.findByTranIdForUpdate(tranId)?.let { refreshLocked(it) }
    }

    private fun refreshLocked(p: Payment) {
        if (p.status != PaymentStatus.PENDING) return   // already final: nothing to do (idempotent)

        val r = payway.checkTransaction(p.tranId)
        val now = Instant.now()
        when (r.state) {
            CheckResult.State.APPROVED -> {
                if (r.amount != null && r.amount.compareTo(p.amount) != 0) {
                    // Paid amount doesn't match the order. Don't ship; needs a human to check.
                    log.error("AMOUNT MISMATCH tran_id={} expected={} got={}", p.tranId, p.amount, r.amount)
                    p.status = PaymentStatus.FAILED
                } else {
                    markPaid(p, r.apv, now)
                }
            }
            CheckResult.State.DECLINED -> p.status = PaymentStatus.DECLINED
            CheckResult.State.CANCELLED, CheckResult.State.REFUNDED -> p.status = PaymentStatus.CANCELLED
            CheckResult.State.PENDING, CheckResult.State.NOT_FOUND -> if (p.isExpired(now)) p.status = PaymentStatus.EXPIRED
            CheckResult.State.ERROR -> Unit   // PayWay unreachable: keep PENDING, try again next poll
        }
        p.updatedAt = now
    }

    private fun markPaid(p: Payment, apv: String?, now: Instant) {
        p.status = PaymentStatus.APPROVED
        p.apv = apv
        p.paidAt = now

        val order = p.order
        if (order.status == OrderStatus.CANCELLED) {
            log.warn("Order {} was cancelled but then paid (tran {}). Marking PAID; consider a refund.", order.orderNo, p.tranId)
        }
        if (order.status == OrderStatus.PENDING_PAYMENT || order.status == OrderStatus.CANCELLED) {
            order.status = OrderStatus.PAID
            order.paidAt = now
            order.updatedAt = now
            // Take the items out of stock only after money is confirmed.
            order.items.forEach { it.product.stock = maxOf(0, it.product.stock - it.quantity) }
            log.info("Order {} PAID via tran {} (apv {})", order.orderNo, p.tranId, apv)
        }
    }

    private fun Payment.toResponse() = PaymentResponse(
        orderId = order.id!!, orderNo = order.orderNo, tranId = tranId, status = status,
        amount = amount, currency = currency, qrString = qrString, abapayDeeplink = abapayDeeplink,
        checkoutQrUrl = checkoutQrUrl, expiresAt = expiresAt,
    )

    private fun splitName(full: String): Pair<String, String> {
        val parts = full.trim().split(Regex("\\s+"), limit = 2)
        return parts[0].take(20) to (parts.getOrNull(1) ?: parts[0]).take(20)
    }

    /** PayWay tran_id: max 20 chars, unique per merchant. e.g. 260925124501-4821 -> "2609251245014821" */
    private fun newTranId(): String {
        val ts = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now())
        return ts + Random.nextInt(1000, 9999)
    }
}
