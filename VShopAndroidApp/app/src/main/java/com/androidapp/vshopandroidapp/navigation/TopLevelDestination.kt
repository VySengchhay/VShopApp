package com.androidapp.vshopandroidapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

enum class TopLevelDestination(
    val route: NavKey,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(HomeRoute, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    SHOP(ShopRoute, "Shop", Icons.Outlined.Storefront, Icons.Rounded.Storefront),
    CART(CartRoute, "Cart", Icons.Outlined.ShoppingCart, Icons.Rounded.ShoppingCart),
    ORDERS(OrdersRoute, "Orders", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong),
    PROFILE(ProfileRoute, "Profile", Icons.Outlined.Person, Icons.Rounded.Person);

    companion object {
        fun of(key: NavKey?): TopLevelDestination? = entries.firstOrNull { it.route == key }
    }
}