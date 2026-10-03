package com.vshop.config

import java.math.BigDecimal
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiryMinutes: Long = 15,   // short-lived access JWT
    val refreshTokenExpiryDays: Long = 30,     // opaque refresh token stored (hashed) in refresh_tokens
)

@ConfigurationProperties("app.shop")
data class ShopProperties(
    val deliveryFee: BigDecimal = BigDecimal("1.50"),
    val freeDeliveryOver: BigDecimal = BigDecimal("20.00"),
)

@ConfigurationProperties("app.otp")
data class OtpProperties(
    val logOnly: Boolean = true,          // true: print the code in the log instead of emailing it
    val mailFrom: String = "VShop <no-reply@vshop.local>",
    val expiryMinutes: Long = 10,
    val maxVerifyAttempts: Int = 5,       // wrong codes allowed per OTP
    val maxRequestsPerHour: Int = 5,      // new codes per email per hour, then 429
)

@ConfigurationProperties("payway")
data class PayWayProperties(
    val mock: Boolean = true,
    val baseUrl: String = "https://checkout-sandbox.payway.com.kh",
    val merchantId: String = "",
    val apiKey: String = "",
    val callbackUrl: String = "",
    val lifetimeMinutes: Int = 15,
)
