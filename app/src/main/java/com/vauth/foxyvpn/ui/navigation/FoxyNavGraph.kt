package com.vauth.foxyvpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.ui.screens.AccountScreen
import com.vauth.foxyvpn.ui.screens.AdminLoginScreen
import com.vauth.foxyvpn.ui.screens.HomeScreen
import com.vauth.foxyvpn.ui.screens.LoginScreen
import com.vauth.foxyvpn.ui.screens.LogsScreen
import com.vauth.foxyvpn.ui.screens.ServerListScreen
import com.vauth.foxyvpn.ui.screens.PlansScreen
import com.vauth.foxyvpn.ui.screens.SettingsScreen
import com.vauth.foxyvpn.ui.screens.SplashScreen
import com.vauth.foxyvpn.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object FoxyRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ADMIN_LOGIN = "admin_login"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val PLANS = "plans"
    const val LOGS = "logs"
}

@Composable
fun FoxyNavGraph(
    navController: NavHostController = rememberNavController(),
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onHomeShown: () -> Unit = {},
) {
    // scope سطح NavGraph؛ بعد از برگشت از صفحه‌ی سرورها هم زنده می‌مونه (scope خود صفحه لغو می‌شد)
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = FoxyRoutes.SPLASH) {
        composable(FoxyRoutes.SPLASH) {
            SplashScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
                onNeedsLogin = {
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.LOGIN) {
            LoginScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.LOGIN) { inclusive = true } }
                },
                onOpenAdmin = { navController.navigate(FoxyRoutes.ADMIN_LOGIN) },
            )
        }
        composable(FoxyRoutes.ADMIN_LOGIN) {
            AdminLoginScreen(
                authRepository = app.authRepository,
                onSignedIn = {
                    RedeemClient(app).adminMode = true
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.HOME) {
            // بعد از ورود به حساب (رسیدن به صفحه‌ی اصلی) یک بار اجازه‌ی نوتیفیکیشن پرسیده می‌شه
            LaunchedEffect(Unit) { onHomeShown() }
            HomeScreen(
                app = app,
                themeController = themeController,
                authRepository = app.authRepository,
                // هنوز سروری انتخاب نشده: به‌جای وصل شدن خودکار، اول لیست سرورها باز می‌شه
                onRequestConnect = {
                    if (app.proxyStateStore.load() == null) {
                        navController.navigate(FoxyRoutes.SERVERS)
                    } else {
                        onRequestConnect()
                    }
                },
                onDisconnect = onDisconnect,
                onOpenServers = { navController.navigate(FoxyRoutes.SERVERS) },
                onOpenSettings = { navController.navigate(FoxyRoutes.SETTINGS) },
                onOpenPlans = { navController.navigate(FoxyRoutes.PLANS) },
            )
        }
        composable(FoxyRoutes.PLANS) {
            PlansScreen(
                onBack = { navController.popBackStack() },
                // کد منقضی/لغو/تموم‌شده: دوباره می‌شه کد جدید وارد کرد
                onEnterNewCode = {
                    onDisconnect()
                    app.tokenStore.clear()
                    RedeemClient(app).apply { adminMode = false; activeCode = null }
                    LicenseState.clear(app)
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.SERVERS) {
            ServerListScreen(
                serverListClient = app.serverListClient,
                proxyStateStore = app.proxyStateStore,
                onServerSelected = {
                    navController.popBackStack()
                    // با انتخاب سرور، خودکار وصل شو (اگه از قبل وصل بود، اول قطع و بعد به سرور جدید)
                    scope.launch {
                        if (FoxyVpnService.state.value != ConnectionState.DISCONNECTED) {
                            onDisconnect()
                            withTimeoutOrNull(5_000) {
                                FoxyVpnService.state.first { it == ConnectionState.DISCONNECTED }
                            }
                            delay(400)
                        }
                        onRequestConnect()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.SETTINGS) {
            SettingsScreen(
                settingsStore = app.settingsStore,
                themeController = themeController,
                onOpenLogs = { navController.navigate(FoxyRoutes.LOGS) },
                onOpenAccount = { navController.navigate(FoxyRoutes.ACCOUNT) },
                onSignOut = {
                    onDisconnect()
                    app.tokenStore.clear()
                    RedeemClient(app).apply { adminMode = false; activeCode = null }
                    LicenseState.clear(app)
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.ACCOUNT) {
            AccountScreen(
                authRepository = app.authRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.LOGS) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
