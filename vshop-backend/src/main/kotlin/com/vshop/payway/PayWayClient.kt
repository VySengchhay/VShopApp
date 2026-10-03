package com.vshop.payway

import java.math.BigDecimal

/**
 * Everything the rest of the app needs from ABA PayWay.
 * Two implementations:
 *  - RealPayWayClient  -> talks to the PayWay sandbox (payway.mock=false)
 *  - MockPayWayClient  -> fake PayWay inside this app (payway.mock=true)
 */
interface PayWayClient {
    fun createPurchase(req: PurchaseRequest): PurchaseResult
    fun checkTransaction(tranId: String): CheckResult
}

data class PurchaseItem(val name: String, val quantity: Int, val price: BigDecimal)

data class PurchaseRequest(
    val tranId: String,
    val amount: BigDecimal,
    val currency: String,
    val items: List<PurchaseItem>,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val lifetimeMinutes: Int,
)

data class PurchaseResult(
    val success: Boolean,
    val qrString: String?,
    val abapayDeeplink: String?,
    val checkoutQrUrl: String?,
    val errorMessage: String?,
    val requestLog: String,    // what we sent, with secrets removed
    val rawResponse: String,
)

/** Normalised result of PayWay "check transaction". */
data class CheckResult(
    val state: State,
    val amount: BigDecimal?,
    val currency: String?,
    val apv: String?,
    val rawResponse: String,
) {
    enum class State { APPROVED, PENDING, DECLINED, CANCELLED, REFUNDED, NOT_FOUND, ERROR }
}
