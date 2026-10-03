package com.vshop.payway

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * A fake PayWay that lives inside this app. Active when payway.mock=true (the default).
 *
 * Use it to build the Android app without real keys or internet:
 *  1. App calls POST /api/orders/{id}/pay  -> gets a fake QR + deeplink
 *  2. You call  POST /api/dev/payments/{tranId}/approve  (Postman / curl)
 *  3. App's next status poll sees PAID
 */
@Component
@ConditionalOnProperty(name = ["payway.mock"], havingValue = "true", matchIfMissing = true)
class MockPayWayClient : PayWayClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class Tx(val amount: BigDecimal, val currency: String, var state: CheckResult.State)
    private val transactions = ConcurrentHashMap<String, Tx>()

    init {
        log.warn("PayWay MOCK mode is ON. No real payments. Approve with POST /api/dev/payments/{tranId}/approve")
    }

    override fun createPurchase(req: PurchaseRequest): PurchaseResult {
        transactions[req.tranId] = Tx(req.amount, req.currency, CheckResult.State.PENDING)
        return PurchaseResult(
            success = true,
            // Not a real KHQR. Bank apps will reject it, but it renders as a QR code for UI testing.
            qrString = "MOCK-KHQR|${req.tranId}|${req.amount.toPlainString()}|${req.currency}",
            abapayDeeplink = "abamobilebank://ababank.com?type=payway&qrcode=MOCK-${req.tranId}",
            checkoutQrUrl = null,
            errorMessage = null,
            requestLog = """{"mock":true,"tran_id":"${req.tranId}"}""",
            rawResponse = """{"status":{"code":"00","message":"Mock success"}}""",
        )
    }

    override fun checkTransaction(tranId: String): CheckResult {
        val tx = transactions[tranId] ?: return CheckResult(CheckResult.State.NOT_FOUND, null, null, null, "{}")
        return CheckResult(tx.state, tx.amount, tx.currency, if (tx.state == CheckResult.State.APPROVED) "MOCK01" else null,
            """{"mock":true,"state":"${tx.state}"}""")
    }

    /** Dev helper: pretend the customer paid (or declined) in ABA Mobile. */
    fun setState(tranId: String, state: CheckResult.State): Boolean {
        val tx = transactions[tranId] ?: return false
        tx.state = state
        return true
    }
}
