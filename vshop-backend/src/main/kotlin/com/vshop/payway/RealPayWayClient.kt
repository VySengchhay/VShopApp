package com.vshop.payway

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vshop.config.PayWayProperties
import java.math.BigDecimal
import java.math.RoundingMode
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * Talks to the real ABA PayWay sandbox.
 * Active when payway.mock=false (PAYWAY_MOCK=false in .env).
 */
@Component
@ConditionalOnProperty(name = ["payway.mock"], havingValue = "false")
class RealPayWayClient(
    private val props: PayWayProperties,
    private val objectMapper: ObjectMapper,
) : PayWayClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val http: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(10_000)
            setReadTimeout(20_000)
        })
        .build()

    init {
        require(props.merchantId.isNotBlank()) { "PAYWAY_MERCHANT_ID is missing. Add it to .env or set PAYWAY_MOCK=true." }
        require(props.apiKey.isNotBlank()) { "PAYWAY_API_KEY is missing. Add it to .env or set PAYWAY_MOCK=true." }
        if (props.callbackUrl.isBlank()) {
            log.warn("PAYWAY_CALLBACK_URL is empty. PayWay can't notify this server; the app will rely on status polling.")
        }
    }

    // ------------------------------------------------------------------
    // Purchase: POST /api/payment-gateway/v1/payments/purchase (multipart/form-data)
    // payment_option = abapay_khqr_deeplink -> PayWay answers with JSON that has
    // the KHQR string and the ABA Mobile deeplink instead of an HTML checkout page.
    // ------------------------------------------------------------------
    override fun createPurchase(req: PurchaseRequest): PurchaseResult {
        val itemsJson = objectMapper.writeValueAsString(
            req.items.map { mapOf("name" to it.name, "quantity" to it.quantity, "price" to money(it.price)) }
        )

        // Only the fields we use. Unused fields are "" and don't change the hash.
        val fields = linkedMapOf(
            "req_time" to PayWayHasher.reqTime(),
            "merchant_id" to props.merchantId,
            "tran_id" to req.tranId,
            "amount" to money(req.amount),
            "items" to PayWayHasher.base64(itemsJson),
            "firstname" to req.firstName,
            "lastname" to req.lastName,
            "email" to req.email,
            "phone" to req.phone,
            "type" to "purchase",
            "payment_option" to "abapay_khqr_deeplink",
            "return_url" to props.callbackUrl.takeIf { it.isNotBlank() }?.let { PayWayHasher.base64(it) }.orEmpty(),
            "currency" to req.currency,
            "lifetime" to req.lifetimeMinutes.toString(),
        ).filterValues { it.isNotEmpty() }

        val hash = PayWayHasher.hashFields(props.apiKey, fields, PayWayHasher.PURCHASE_HASH_ORDER)

        val form = LinkedMultiValueMap<String, Any>()
        fields.forEach { (k, v) -> form.add(k, v) }
        form.add("hash", hash)

        val requestLog = objectMapper.writeValueAsString(fields)   // never log the api key; the hash is fine to omit too
        log.info("PayWay purchase -> tran_id={} amount={} {}", req.tranId, fields["amount"], req.currency)

        val raw = try {
            http.post()
                .uri("/api/payment-gateway/v1/payments/purchase")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .exchange(RestClient.RequestHeadersSpec.ExchangeFunction { _, res ->
                    String(res.body.readAllBytes(), Charsets.UTF_8)
                })
        } catch (e: Exception) {
            log.error("PayWay purchase call failed for tran_id={}", req.tranId, e)
            return PurchaseResult(false, null, null, null, "Can't reach PayWay: ${e.message}", requestLog, "")
        }

        log.info("PayWay purchase <- {}", raw.take(500))
        val json = parse(raw)
            ?: return PurchaseResult(false, null, null, null, "PayWay returned a non-JSON response", requestLog, raw)

        val code = json.path("status").path("code").asText("")
        val qr = json.text("qr_string", "qrString")
        val deeplink = json.text("abapay_deeplink")
        val ok = (code == "00" || code == "0") && (qr != null || deeplink != null)

        return PurchaseResult(
            success = ok,
            qrString = qr,
            abapayDeeplink = deeplink,
            checkoutQrUrl = json.text("checkout_qr_url"),
            errorMessage = if (ok) null else json.path("status").path("message").asText("PayWay rejected the request (code $code)"),
            requestLog = requestLog,
            rawResponse = raw,
        )
    }

    // ------------------------------------------------------------------
    // Check transaction: POST /api/payment-gateway/v1/payments/check-transaction-2 (JSON)
    // This is the source of truth for "did the customer really pay?".
    // ------------------------------------------------------------------
    override fun checkTransaction(tranId: String): CheckResult {
        val fields = linkedMapOf(
            "req_time" to PayWayHasher.reqTime(),
            "merchant_id" to props.merchantId,
            "tran_id" to tranId,
        )
        val body = fields + ("hash" to PayWayHasher.hashFields(props.apiKey, fields, PayWayHasher.CHECK_HASH_ORDER))

        val raw = try {
            http.post()
                .uri("/api/payment-gateway/v1/payments/check-transaction-2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange(RestClient.RequestHeadersSpec.ExchangeFunction { _, res ->
                    String(res.body.readAllBytes(), Charsets.UTF_8)
                })
        } catch (e: Exception) {
            log.warn("PayWay check-transaction failed for tran_id={}: {}", tranId, e.message)
            return CheckResult(CheckResult.State.ERROR, null, null, null, e.message ?: "")
        }

        val json = parse(raw) ?: return CheckResult(CheckResult.State.ERROR, null, null, null, raw)
        val statusCode = json.path("status").path("code").asText("")
        val data = json.path("data")

        if (statusCode != "00" || data.isMissingNode || data.isNull) {
            // PayWay doesn't know this tran_id yet (customer hasn't opened / scanned it) or another error.
            return CheckResult(CheckResult.State.NOT_FOUND, null, null, null, raw)
        }

        val state = when (data.path("payment_status").asText("").uppercase()) {
            "APPROVED" -> CheckResult.State.APPROVED
            "PENDING", "PRE-AUTH" -> CheckResult.State.PENDING
            "DECLINED" -> CheckResult.State.DECLINED
            "CANCELLED" -> CheckResult.State.CANCELLED
            "REFUNDED" -> CheckResult.State.REFUNDED
            else -> if (data.path("payment_status_code").asInt(-1) == 0) CheckResult.State.APPROVED else CheckResult.State.PENDING
        }
        val amount = data.path("payment_amount").takeIf { it.isNumber }?.decimalValue()
            ?: data.path("total_amount").takeIf { it.isNumber }?.decimalValue()

        return CheckResult(
            state = state,
            amount = amount,
            currency = data.text("payment_currency"),
            apv = data.text("apv"),
            rawResponse = raw,
        )
    }

    private fun money(v: BigDecimal) = v.setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun parse(raw: String): JsonNode? = try {
        objectMapper.readTree(raw)
    } catch (e: Exception) {
        null
    }

    private fun JsonNode.text(vararg names: String): String? =
        names.firstNotNullOfOrNull { n -> path(n).takeIf { it.isTextual && it.asText().isNotBlank() }?.asText() }
}
