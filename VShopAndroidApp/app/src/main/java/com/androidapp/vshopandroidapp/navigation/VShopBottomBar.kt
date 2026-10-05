package com.androidapp.vshopandroidapp.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun VShopBottomBar(
    selected: TopLevelDestination,
    cartCount: Int,
    onTabClick: (TopLevelDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        TopLevelDestination.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabClick(tab) },
                label = { Text(text = tab.label) },
                icon = {
                    val icon = if (isSelected) tab.selectedIcon else tab.icon

                    if (tab == TopLevelDestination.CART && cartCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (cartCount > 99) "99+" else cartCount.toString()) } }) {
                            Icon(icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(icon, contentDescription = tab.label)
                    }
                }
            )
        }
    }
}