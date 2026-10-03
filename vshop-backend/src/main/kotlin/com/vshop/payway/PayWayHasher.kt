package com.vshop.payway

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PayWay signs every request the same way:
 *   hash = Base64( HMAC-SHA512( value1 + value2 + ... , apiKey ) )
 *
 * The values are joined with NO separator, in the exact order PayWay's docs list.
 * Fields you don't send count as empty strings, so they add nothing to the string.
 * If one field is out of order you get "Wrong hash" from PayWay.
 */
object PayWayHasher {

    private val REQ_TIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC)

    /** req_time must be UTC, format yyyyMMddHHmmss, e.g. 20260925124501 */
    fun reqTime(now: Instant = Instant.now()): String = REQ_TIME_FORMAT.format(now)

    fun hash(apiKey: String, values: List<String?>): String {
        val data = values.joinToString(separator = "") { it ?: "" }
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(apiKey.toByteArray(Charsets.UTF_8), "HmacSHA512"))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    fun base64(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    /** Field order for the Purchase API hash (from PayWay docs). */
    val PURCHASE_HASH_ORDER = listOf(
        "req_time", "merchant_id", "tran_id", "amount", "items", "shipping",
        "firstname", "lastname", "email", "phone", "type", "payment_option",
        "return_url", "cancel_url", "continue_success_url", "return_deeplink",
        "currency", "custom_fields", "return_params", "payout", "lifetime",
        "additional_params", "google_pay_token", "skip_success_page",
    )

    /** Field order for the Check Transaction API hash. */
    val CHECK_HASH_ORDER = listOf("req_time", "merchant_id", "tran_id")

    fun hashFields(apiKey: String, fields: Map<String, String>, order: List<String>): String =
        hash(apiKey, order.map { fields[it] })
}
