package com.androidapp.vshopandroidapp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// -------------- First Launch -----------------
@Serializable
data object OnboardingRoute : NavKey

// -------------- Auth -----------------
@Serializable
data object RegisterRoute : NavKey

@Serializable
data object LoginRoute : NavKey

@Serializable
data object ForgotPasswordRoute : NavKey

@Serializable
data class OtpVerifyRoute(val email: String) : NavKey

@Serializable
data class ResetPasswordRoute(
    val email: String,
    val otp: String
) : NavKey

// -------------- Bottom Tabs -----------------
@Serializable
data object HomeRoute : NavKey

@Serializable
data object ShopRoute : NavKey

@Serializable
data object CartRoute : NavKey

@Serializable
data object OrdersRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

// -------------- Shopping -----------------
@Serializable
data class ProductListRoute(
    val categoryId: Long,
    val title: String
) : NavKey

@Serializable
data class ProductDetail(val productId: Long) : NavKey

@Serializable
data object CheckoutRoute : NavKey

@Serializable
data class AddressListRoute(val selectMode: Boolean) : NavKey

@Serializable
data class AddressFormRoute(val addressId: Long? = null) : NavKey

// -------------- Payment + Order -----------------
@Serializable
data class PaymentRoute(
    val orderId: Long,
    val openAbaApp: Boolean = false
) : NavKey

@Serializable
data class PaymentResultRoute(
    val orderId: Long,
    val success: Boolean,
    val reason: String = ""
) : NavKey

@Serializable
data class OrderDetailRoute(
    val orderId: Long,
) : NavKey