package com.vshop.payway

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

/** Runs without a database: ./gradlew test */
class PayWayHasherTest {

    private val key = "test-api-key"

    @Test
    fun `req_time is UTC yyyyMMddHHmmss`() {
        assertEquals("20260925120000", PayWayHasher.reqTime(Instant.parse("2026-09-25T12:00:00Z")))
    }

    @Test
    fun `check-transaction hash matches reference value`() {
        val fields = mapOf("req_time" to "20260925120000", "merchant_id" to "ec478904", "tran_id" to "2609251200001234")
        assertEquals(
            "K0aLNgrTYWYtxMsPJ6v0xZpeefZKuxnWZyPo9jPNv1qgWkRXr41XxRQHGc35er5Z8/IU9jkvaT4iPYLFkS0cqA==",
            PayWayHasher.hashFields(key, fields, PayWayHasher.CHECK_HASH_ORDER),
        )
    }

    @Test
    fun `purchase hash uses PayWay field order and skips unused fields`() {
        // Deliberately inserted in a random order: the hasher must sort them by PURCHASE_HASH_ORDER.
        val fields = mapOf(
            "lifetime" to "15", "currency" to "USD", "payment_option" to "abapay_khqr_deeplink",
            "type" to "purchase", "phone" to "012345678", "email" to "a@b.com",
            "lastname" to "Sengchhay", "firstname" to "Vy", "items" to "ITEMS", "amount" to "12.50",
            "tran_id" to "2609251200001234", "merchant_id" to "ec478904", "req_time" to "20260925120000",
        )
        assertEquals(
            "tpjEZWFJIBz9Un3D8meOqc91dSiIS3qsk9kJO07psutDqNjWtIPhZyN0v4c/IBOZAVamS7Gy1f7KNLph2eip9A==",
            PayWayHasher.hashFields(key, fields, PayWayHasher.PURCHASE_HASH_ORDER),
        )
    }
}
