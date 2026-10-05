package com.androidapp.vshopandroidapp.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

private fun NavKey?.isPublic(): Boolean =
    this == null || this is OnboardingRoute || this is LoginRoute || this is RegisterRoute ||
            this == ForgotPasswordRoute || this is OtpVerifyRoute || this is ResetPasswordRoute

@Composable
fun AppNavDisplay(
    startRoute: NavKey,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    cartCount: Int = 0,
) {
    val backStack = rememberNavBackStack(startRoute)
    var pickedAddressId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isProductListView by rememberSaveable { mutableStateOf(false) }

    fun resetTo(vararg routes: NavKey) {
        backStack.clear()
        backStack.addAll(routes)
    }

    fun back() {
        backStack.removeLastOrNull()
    }

    fun replaceTop(route: NavKey) {
        backStack.removeLastOrNull()
        backStack.add(route)
    }

    fun navigateToTab(tap: TopLevelDestination) {
        if (backStack.lastOrNull() == tap.route) return
        if (tap == TopLevelDestination.HOME) resetTo(HomeRoute) else resetTo(HomeRoute, tap.route)
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && backStack.lastOrNull().isPublic()) {
            resetTo(LoginRoute)
        }
    }

    val currentTab = TopLevelDestination.of(backStack.lastOrNull())

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                VShopBottomBar(
                    selected = currentTab,
                    cartCount = cartCount,
                    onTabClick = ::navigateToTab
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding()),
            onBack = { back() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {

                //----------------- Auth -------------------
                entry<RegisterRoute> {

                }
            }
        )
    }
}