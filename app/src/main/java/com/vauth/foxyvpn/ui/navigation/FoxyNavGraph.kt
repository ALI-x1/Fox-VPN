package com.vauth.foxyvpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.ui.screens.AccountScreen
import com.vauth.foxyvpn.i18n.AppLanguage
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.ui.screens.HomeScreen
import com.vauth.foxyvpn.ui.screens.LanguageScreen
import com.vauth.foxyvpn.ui.screens.findActivity
import com.vauth.foxyvpn.ui.screens.LoginScreen
import com.vauth.foxyvpn.ui.screens.LogsScreen
import com.vauth.foxyvpn.ui.screens.ServerListScreen
import com.vauth.foxyvpn.ui.screens.PlansScreen
import com.vauth.foxyvpn.ui.screens.SettingsScreen
import com.vauth.foxyvpn.ui.screens.SplashScreen
import com.vauth.foxyvpn.ui.screens.SupportScreen
import com.vauth.foxyvpn.ui.screens.PurchaseScreen
import com.vauth.foxyvpn.ui.screens.ThemeSetupScreen
import com.vauth.foxyvpn.ui.screens.WelcomeScreen
import com.vauth.foxyvpn.ui.theme.ThemeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private fun planRankOf(plan: String?): Int = when (plan) { "free" -> 1; "pro" -> 2; "max" -> 3; else -> 0 }
private const val UI_PREFS = "foxvpn_ui"
private const val KEY_LAST_PLAN = "last_plan_rank"

object FoxyRoutes {
    const val LANGUAGE = "language"
    const val THEME_SETUP = "theme_setup"
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val WELCOME = "welcome"
    const val UPGRADED = "upgraded"
    const val HOME = "home"
    const val SERVERS = "servers"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val PLANS = "plans"
    const val LOGS = "logs"
    const val SUPPORT = "support"
    const val PURCHASE = "purchase/{plan}?code={code}"
    fun purchase(plan: String, code: String) = "purchase/$plan?code=${android.net.Uri.encode(code)}"
}

@Composable
fun FoxyNavGraph(
    navController: NavHostController = rememberNavController(),
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onHomeShown: () -> Unit = {},
    // درخواست باز شدن لیست سرورها (از دکمه‌ی «Change server» ویجت بزرگ)
    openServersRequest: Boolean = false,
    onOpenServersHandled: () -> Unit = {},
) {
    // scope سطح NavGraph؛ بعد از برگشت از صفحه‌ی سرورها هم زنده می‌مونه (scope خود صفحه لغو می‌شد)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // اولین اجرا: اول انتخاب زبان، بعد بقیه‌ی مسیر
    val startDestination = remember { if (Lang.hasChosen(context)) FoxyRoutes.SPLASH else FoxyRoutes.LANGUAGE }

    // لیست سرورها بعد از Splash باز می‌شه؛ اگه هنوز وارد نشده‌اید (Login) درخواست نادیده گرفته می‌شه
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(openServersRequest, currentRoute) {
        if (!openServersRequest || currentRoute == null || currentRoute == FoxyRoutes.SPLASH) return@LaunchedEffect
        if (currentRoute != FoxyRoutes.LOGIN && currentRoute != FoxyRoutes.SERVERS && currentRoute != FoxyRoutes.LANGUAGE) {
            navController.navigate(FoxyRoutes.SERVERS) { popUpTo(FoxyRoutes.HOME) }
        }
        onOpenServersHandled()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(FoxyRoutes.LANGUAGE) {
            LanguageScreen(
                onConfirm = { choice ->
                    Lang.setChoice(context, choice)
                    Lang.markChosen(context)
                    // بعد از زبان، یک قدم انتخاب تم (فقط اولین اجرا)
                    navController.navigate(FoxyRoutes.THEME_SETUP) { popUpTo(FoxyRoutes.LANGUAGE) { inclusive = true } }
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
        composable(FoxyRoutes.THEME_SETUP) {
            ThemeSetupScreen(
                themeController = themeController,
                onContinue = {
                    navController.navigate(FoxyRoutes.SPLASH) { popUpTo(FoxyRoutes.THEME_SETUP) { inclusive = true } }
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
                    navController.navigate(FoxyRoutes.WELCOME) { popUpTo(FoxyRoutes.LOGIN) { inclusive = true } }
                },
                onOpenSupport = { navController.navigate(FoxyRoutes.SUPPORT) },
            )
        }
        composable(FoxyRoutes.WELCOME) {
            // «ادامه»: اول اجازه‌ی نوتیف پرسیده می‌شه، بعد صفحه‌ی اصلی
            WelcomeScreen(
                onContinue = {
                    onHomeShown()
                    navController.navigate(FoxyRoutes.HOME) { popUpTo(FoxyRoutes.WELCOME) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.UPGRADED) {
            // بعد از ارتقا: تبریک، و برگشت به همون صفحه‌ی اصلی
            LaunchedEffect(Unit) {
                // رتبه‌ی فعلی ثبت می‌شه تا هوم دوباره همین ارتقا رو جشن نگیره
                context.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE).edit()
                    .putInt(KEY_LAST_PLAN, planRankOf(LicenseState.status.value?.plan)).apply()
            }
            WelcomeScreen(
                upgrade = true,
                onContinue = { navController.popBackStack(FoxyRoutes.HOME, false) },
            )
        }
        composable(FoxyRoutes.HOME) {
            // بعد از ورود به حساب (رسیدن به صفحه‌ی اصلی) یک بار اجازه‌ی نوتیفیکیشن پرسیده می‌شه
            LaunchedEffect(Unit) { onHomeShown() }
            // ارتقا از ربات (ادمین پلن کد رو عوض کرده): با اولین به‌روزرسانی وضعیت، صفحه‌ی تبریک نشون داده می‌شه
            val homeLicense by LicenseState.status.collectAsState()
            LaunchedEffect(homeLicense?.plan) {
                val rank = planRankOf(homeLicense?.plan)
                if (rank > 0) {
                    val prefs = context.getSharedPreferences(UI_PREFS, android.content.Context.MODE_PRIVATE)
                    val prev = prefs.getInt(KEY_LAST_PLAN, -1)
                    prefs.edit().putInt(KEY_LAST_PLAN, rank).apply()
                    if (prev in 1 until rank) navController.navigate(FoxyRoutes.UPGRADED)
                }
            }
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
                onOpenAccount = { navController.navigate(FoxyRoutes.ACCOUNT) },
                onOpenSupport = { navController.navigate(FoxyRoutes.SUPPORT) },
            )
        }
        composable(FoxyRoutes.SUPPORT) {
            SupportScreen(
                onBack = { navController.popBackStack() },
                onOpenPlans = { navController.navigate(FoxyRoutes.PLANS) },
            )
        }
        composable(
            FoxyRoutes.PURCHASE,
            arguments = listOf(
                navArgument("plan") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            PurchaseScreen(
                planKey = entry.arguments?.getString("plan") ?: "pro",
                discountCode = entry.arguments?.getString("code").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.PLANS) {
            PlansScreen(
                onBack = { navController.popBackStack() },
                authRepository = app.authRepository,
                onDisconnect = onDisconnect,
                onUpgraded = { navController.navigate(FoxyRoutes.UPGRADED) },
                onBuy = { plan, code -> navController.navigate(FoxyRoutes.purchase(plan, code)) },
                onOpenSupport = { navController.navigate(FoxyRoutes.SUPPORT) },
                // کد منقضی/لغو/تموم‌شده: دوباره می‌شه کد جدید وارد کرد
                onEnterNewCode = {
                    onDisconnect()
                    app.tokenStore.clear()
                    RedeemClient(app).apply { releaseSnapshot()(); adminMode = false; activeCode = null }
                    LicenseState.clear(app)
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(FoxyRoutes.SERVERS) {
            ServerListScreen(
                serverListClient = app.serverListClient,
                proxyStateStore = app.proxyStateStore,
                onOpenPlans = { navController.navigate(FoxyRoutes.PLANS) },
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
                onOpenSupport = { navController.navigate(FoxyRoutes.SUPPORT) },
                onOpenPlans = { navController.navigate(FoxyRoutes.PLANS) },
                authRepository = app.authRepository,
                onDisconnect = onDisconnect,
                onUpgraded = { navController.navigate(FoxyRoutes.UPGRADED) },
                onSignOut = {
                    onDisconnect()
                    app.tokenStore.clear()
                    RedeemClient(app).apply { releaseSnapshot()(); adminMode = false; activeCode = null }
                    LicenseState.clear(app)
                    navController.navigate(FoxyRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.ACCOUNT) {
            AccountScreen(
                authRepository = app.authRepository,
                onDisconnect = onDisconnect,
                onUpgraded = { navController.navigate(FoxyRoutes.UPGRADED) },
                onOpenPlans = { navController.navigate(FoxyRoutes.PLANS) },
                onOpenSupport = { navController.navigate(FoxyRoutes.SUPPORT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(FoxyRoutes.LOGS) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
