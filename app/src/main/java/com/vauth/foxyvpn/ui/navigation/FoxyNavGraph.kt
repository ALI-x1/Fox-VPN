package com.vauth.foxyvpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.ui.screens.AccountScreen
import com.vauth.foxyvpn.i18n.AppLanguage
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.ui.screens.HomeScreen
import com.vauth.foxyvpn.ui.screens.LanguageScreen
import com.vauth.foxyvpn.ui.screens.findActivity
import com.vauth.foxyvpn.ui.screens.LoginScreen
import com.vauth.foxyvpn.ui.screens.LogsScreen
import com.vauth.foxyvpn.ui.screens.ServerListScreen
import com.vauth.foxyvpn.ui.screens.SettingsScreen
import com.vauth.foxyvpn.ui.screens.SplashScreen
import com.vauth.foxyvpn.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object FoxyRoutes {
    const val LANGUAGE = "language"
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
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
    val context = LocalContext.current
    // اولین اجرا: اول انتخاب زبان، بعد بقیه‌ی مسیر
    val startDestination = remember { if (Lang.hasChosen(context)) FoxyRoutes.SPLASH else FoxyRoutes.LANGUAGE }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(FoxyRoutes.LANGUAGE) {
            LanguageScreen(
                onConfirm = { choice ->
                    Lang.setChoice(context, choice)
                    Lang.markChosen(context)
                    navController.navigate(FoxyRoutes.SPLASH) { popUpTo(FoxyRoutes.LANGUAGE) { inclusive = true } }
                    // با زبان/جهت جدید دوباره ساخته می‌شه (بعد از رفتن به مسیر بعدی، تا وضعیت ذخیره‌شده درست باشه)
                    if (choice != AppLanguage.SYSTEM) {
                        scope.launch {
                            delay(150)
                            context.findActivity()?.recreate()
                        }
                    }
                },
            )
        }
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
