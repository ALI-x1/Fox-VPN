package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import com.vauth.foxyvpn.ui.theme.ThemeMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.GUARDIAN_ENDPOINT_DEFAULT
import com.vauth.foxyvpn.data.GuardianClient
import com.vauth.foxyvpn.data.LicenseBlock
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.scale
import android.content.Context
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.platform.LocalUriHandler
import com.vauth.foxyvpn.notice.NoticeClient
import com.vauth.foxyvpn.notice.NoticeResult
import com.vauth.foxyvpn.data.TrafficMonitor
import com.vauth.foxyvpn.data.TrafficState
import com.vauth.foxyvpn.data.formatBytes
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.Entitlement
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.vpn.ExitCheck
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.IpInfo
import com.vauth.foxyvpn.vpn.PingUtil
import com.vauth.foxyvpn.widget.WidgetQuotaCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding

// ───────────────────────── تایمر اتصال ─────────────────────────

/** زمان شروع اتصال؛ در سطح process نگه داشته می‌شه تا با رفتن به تنظیمات و برگشتن، تایمر صفر نشه. */
/** انیمیشن ورود فقط یک‌بار در هر اجرای برنامه پخش می‌شه (نه هر بار که از تنظیمات برمی‌گردی). */
private object EntryGate {
    var played = false
}

@Composable
private fun rememberEntrance(entered: Boolean, index: Int): State<Float> = animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 380, delayMillis = 60 + index * 70, easing = FastOutSlowInEasing),
    label = "entrance-$index",
)

/** محو شدن + کمی بالا اومدن؛ فقط در فاز draw خونده می‌شه، پس recompose نداره. */
private fun Modifier.entrance(progress: State<Float>): Modifier = graphicsLayer {
    val p = progress.value
    alpha = p
    translationY = (1f - p) * 20.dp.toPx()
}

/** تایمر جدا از HomeScreen: با هر ثانیه فقط همین متن recompose می‌شه، نه کل صفحه. */
@Composable
private fun TimerText(elapsed: () -> Long, color: Color) {
    Text(
        text = formatElapsed(elapsed()),
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color
    )
}

/** سرعت با حرکت نرم بین مقدارها (به‌جای پرش هر ثانیه) و ارقام هم‌عرض تا عرض عدد نپره. */
@Composable
private fun SmoothSpeedText(speedBytes: Long, color: Color) {
    val animated by animateFloatAsState(
        targetValue = speedBytes.toFloat(),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "speed-number",
    )
    Text(
        text = formatBytesPerSecond(animated.toLong()),
        style = TextStyle(fontFeatureSettings = "tnum"),
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        maxLines = 1
    )
}

private object SessionClock {
    var startedAt: Long = 0L
}

/** کد دو حرفی کشور (مثل "DE") → ایموجی پرچم. برای کد نامعتبر null برمی‌گردونه. */
private fun flagEmoji(code: String): String? {
    val cc = code.trim().uppercase().let { if (it == "UK") "GB" else it }
    if (cc.length != 2 || !cc.all { it in 'A'..'Z' }) return null
    val base = 0x1F1E6
    return String(Character.toChars(base + (cc[0] - 'A'))) +
        String(Character.toChars(base + (cc[1] - 'A')))
}

/** اسم کامل کشور به انگلیسی (DE → Germany)؛ اگه کد معتبر نبود null. */
private fun countryDisplayName(code: String): String? {
    val cc = code.trim().uppercase()
    if (cc.length != 2 || !cc.all { it in 'A'..'Z' }) return null
    val name = Locale("", cc).getDisplayCountry(com.vauth.foxyvpn.i18n.Lang.locale)
    return name.takeIf { it.isNotBlank() && !it.equals(cc, ignoreCase = true) }
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val sec = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
}

// ───────────────────────── دیتای باقی‌مانده ─────────────────────────

/** اگه کل حجم به‌جای 50 مثلاً 46.6 نشون داده شد، این رو بکن 1_000_000_000.0 */
private const val BYTES_PER_GB = 1_073_741_824.0

// ───────────────────────── پینگ و چک آی‌پی ─────────────────────────
// همان سنجش TCP صفحه‌ی انتخاب سرور، بعد از اتصال روی سرور انتخاب‌شده اجرا می‌شود.

private const val PING_REFRESH_MS = 5_000L

/** برای اینکه وصل شدن خودکار فقط یک بار در هر اجرای برنامه انجام بشه. */
private object AutoConnectGate {
    @Volatile var consumed = false
}

// همان رنگ‌های کیفیت پینگِ لیست سرورها: زیر ۳۰۰ سبز، زیر ۵۰۰ نارنجی، بالاتر قرمز
private val PingGood = Color(0xFF22B573)
private val PingMedium = Color(0xFFF2A81D)
private val PingBad = Color(0xFFEF5350)
// در حال اتصال: نارنجی، هم‌رنگ نقطه‌ی وضعیت
private val PingConnecting = Color(0xFFFF9F1C)
private const val EXIT_CHECK_ATTEMPTS = 3
private const val EXIT_CHECK_RETRY_MS = 2_000L

// ───────────────────────── کارت (رنگ‌ها از تم فعال) ─────────────────────────

private fun Modifier.foxCard(c: FoxExtras, radius: Dp, color: Color = c.card): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(6.dp, shape, ambientColor = c.accent.copy(alpha = 0.15f), spotColor = c.accent.copy(alpha = 0.15f))
        .clip(shape)
        .background(color)
        .border(1.dp, c.cardBorder, shape)
}

// ───────────────────────── صفحه اصلی ─────────────────────────

@Composable
fun HomeScreen(
    app: FoxyVpnApp,
    themeController: ThemeController,
    onRequestConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
    authRepository: FxaAuthRepository? = null,
    onOpenPlans: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
) {
    val state by FoxyVpnService.state.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()

    // وضعیت کد (Redeem): مصرف، سقف روزانه، اعتبار؛ ادمین (ورود با ایمیل) محدودیتی نداره
    val redeemClient = remember { RedeemClient(app) }
    val isAdmin = remember { redeemClient.adminMode }
    LaunchedEffect(Unit) { LicenseState.load(app) }
    val license by LicenseState.status.collectAsState()
    val useLicense = !isAdmin && license != null
    val block = if (isAdmin) null else LicenseState.blockOf(license)

    // وقتی برنامه تو پس‌زمینه‌ست (یا صفحه خاموشه) حلقه‌های تکراری زیر متوقف می‌شن تا باتری هدر نره.
    val lifecycleOwner = LocalContext.current.findActivity() as? LifecycleOwner
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        if (lifecycle == null) {
            onDispose { }
        } else {
            isForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> isForeground = true
                    Lifecycle.Event.ON_STOP -> isForeground = false
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    // وضعیت کد رو هر یک دقیقه از سرور می‌گیره (وقتی وصله، خود UsageReporter این کار رو می‌کنه)
    LaunchedEffect(isForeground, state, isAdmin) {
        if (isAdmin || !isForeground || redeemClient.activeCode == null) return@LaunchedEffect
        while (true) {
            if (state != ConnectionState.CONNECTED) redeemClient.reportUsage(0)
            delay(60_000)
        }
    }

    // State رو به TrafficCard می‌دیم (نه مقدارش رو) تا هر ثانیه فقط همون باکس recompose بشه
    val trafficState = TrafficMonitor.trafficState.collectAsState()
    var ipAddress by remember { mutableStateOf("-") }
    var countryCode by remember { mutableStateOf("") }

    // وصل شدن خودکار (اگه تو تنظیمات روشن باشه): فقط یک بار در هر بار اجرای برنامه
    LaunchedEffect(Unit) {
        if (!AutoConnectGate.consumed) {
            AutoConnectGate.consumed = true
            if (app.settingsStore.autoConnect &&
                app.proxyStateStore.load() != null &&
                FoxyVpnService.state.value == ConnectionState.DISCONNECTED
            ) {
                onRequestConnect()
            }
        }
    }

    // مانیتور ترافیک فقط به وضعیت اتصال وابسته‌ست (نه به عوض شدن سرور)
    LaunchedEffect(state, isForeground) {
        if (state == ConnectionState.CONNECTED && isForeground) {
            TrafficMonitor.start()
        } else {
            TrafficMonitor.stop()
        }
    }

    // آی‌پی و کشور خروجی: وقتی وصلیم از طریق SOCKS تونل، وگرنه مستقیم (آی‌پی واقعی خودمون).
    //  - با هر تغییر وضعیت/سرور مقدار قبلی پاک می‌شه تا پرچم و آی‌پی سرور قبلی نمونه.
    //  - اگه چک شکست خورد (مثلاً تونل هنوز آماده نیست) چند بار با فاصله دوباره امتحان می‌شه.
    //  - تو حالت CONNECTING چک نمی‌کنیم؛ نتیجه‌اش بی‌معنیه.
    val exitKey = if (state == ConnectionState.CONNECTED) "${selectedProxy?.host}:${selectedProxy?.port}" else ""
    LaunchedEffect(state, exitKey) {
        ipAddress = "-"
        countryCode = ""
        if (state == ConnectionState.CONNECTING) return@LaunchedEffect

        val proxyPort = if (state == ConnectionState.CONNECTED) 1080 else null
        repeat(EXIT_CHECK_ATTEMPTS) { attempt ->
            val result: Result<IpInfo> = try {
                ExitCheck().verifyExitCountry(socksPort = proxyPort)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            val info = result.getOrNull()
            if (info != null) {
                ipAddress = info.ip.ifBlank { "-" }
                countryCode = info.countryCode
                return@LaunchedEffect
            }
            if (attempt < EXIT_CHECK_ATTEMPTS - 1) delay(EXIT_CHECK_RETRY_MS)
        }
    }

    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(state, isForeground) {
        if (state == ConnectionState.CONNECTED) {
            if (SessionClock.startedAt == 0L) SessionClock.startedAt = SystemClock.elapsedRealtime()
            if (isForeground) {
                while (true) {
                    elapsedSeconds = (SystemClock.elapsedRealtime() - SessionClock.startedAt) / 1000
                    delay(1000)
                }
            }
        } else {
            SessionClock.startedAt = 0L
            elapsedSeconds = 0L
        }
    }

    // دیتای اکانت (همون منبع صفحه‌ی Account)؛ بعد از هر تغییر وضعیت اتصال و هر یک دقیقه رفرش می‌شه
    var entitlement by remember { mutableStateOf<Entitlement?>(null) }
    LaunchedEffect(authRepository, state, isForeground) {
        if (authRepository == null || !isForeground || useLicense) return@LaunchedEffect
        // وسط «در حال اتصال» درخواست نزن: با دریافت پروکسی‌پس سرویس رقابت می‌کرد و اتصال اول را کند می‌کرد
        if (state == ConnectionState.CONNECTING) return@LaunchedEffect
        while (true) {
            try {
                val token = authRepository.currentAccessToken()
                if (token != null) {
                    entitlement = GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, token)
                    // آخرین حجم باقی‌مانده برای ویجت بزرگ (بدون درخواست شبکه‌ی جدا)
                    WidgetQuotaCache.save(app, entitlement)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // آخرین مقدار معتبر نگه داشته می‌شه
            }
            delay(60_000)
        }
    }
    val ent = entitlement
    val lic = license
    val dataUnlimited = if (useLicense) lic?.totalBytes == null else (ent != null && !ent.limitedBandwidth)
    val dataTotalGb = if (useLicense) {
        (lic?.totalBytes ?: 0L) / BYTES_PER_GB
    } else {
        ent?.maxBytes?.let { it.toDouble() / BYTES_PER_GB }?.takeIf { it > 0.0 } ?: 50.0
    }
    val dataRemainingGb = if (useLicense) {
        lic?.let { l -> l.totalBytes?.let { (it - l.usedBytes).coerceAtLeast(0L) / BYTES_PER_GB } }
    } else {
        ent?.quotaRemaining?.let { it.toDouble() / BYTES_PER_GB }
    }
    // برچسب دکمه‌ی پلن: Free / Pro / Max (برای ادمین یا بدون کد: Plans)
    val planLabel = if (isAdmin) tr("Plans")
        else license?.plan?.takeIf { it.isNotBlank() && it != "custom" }?.replaceFirstChar { it.uppercase() } ?: tr("Plans")
    val planName = if (useLicense) lic?.plan?.takeIf { it.isNotBlank() && it != "custom" }
        ?.replaceFirstChar { it.uppercase() } else null
    val dailyTotalGb = if (useLicense) (lic?.dailyBytes ?: 0L) / BYTES_PER_GB else 0.0
    val dailyRemainingGb = if (useLicense) lic?.dailyBytes?.let { ((it - lic.dailyUsed).coerceAtLeast(0L)) / BYTES_PER_GB } else null
    val daysLeftCard: Long? = if (useLicense) lic?.expiresAt?.let {
        kotlin.math.ceil((it - System.currentTimeMillis() / 1000) / 86400.0).toLong().coerceAtLeast(0)
    } else null
    val licenseNote: String? = if (useLicense && lic != null) {
        val parts = mutableListOf<String>()
        lic.dailyBytes?.let {
            parts += trf("Today %.1f / %.1f GB", lic.dailyUsed / BYTES_PER_GB, it / BYTES_PER_GB)
        }
        lic.expiresAt?.let {
            val days = kotlin.math.ceil((it - System.currentTimeMillis() / 1000) / 86400.0).toLong().coerceAtLeast(0)
            parts += if (days == 1L) tr("1 day left") else trf("%d days left", days)
        }
        parts.joinToString(" · ").ifBlank { null }
    } else null

    // ───── نوار وضعیت بالای صفحه: فقط وقتی چیزی برای گفتن هست ─────
    val uriHandler = LocalUriHandler.current
    var serverStatus by remember { mutableStateOf<NoticeResult?>(null) }
    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        while (true) {
            serverStatus = NoticeClient.fetchStatus(app)
            delay(300_000)
        }
    }
    val bannerWarn = LocalFoxyStatusColors.current.connecting
    val bannerAccent = LocalFoxExtras.current.accent
    val bannerErr = MaterialTheme.colorScheme.error
    val daysLeft = if (useLicense) license?.expiresAt?.let { kotlin.math.ceil((it - System.currentTimeMillis() / 1000) / 86400.0).toLong() } else null
    val remainingPct = if (useLicense && !dataUnlimited && dataRemainingGb != null && dataTotalGb > 0.0) (dataRemainingGb / dataTotalGb * 100).toInt() else null
    val blockMsg = block?.message ?: ""
    val okTone = LocalFoxyStatusColors.current.connected
    val serverBars = serverStatus?.banners.orEmpty().ifEmpty { listOfNotNull(serverStatus?.banner) }
    val localBanner: HomeBanner? = when {
        block == LicenseBlock.MAINT -> HomeBanner(Icons.Filled.Build, blockMsg, bannerWarn, null)
        block == LicenseBlock.FROZEN -> HomeBanner(Icons.Filled.AcUnit, blockMsg, bannerAccent) { onOpenSupport() }
        block == LicenseBlock.DAILY -> HomeBanner(Icons.Filled.Schedule, blockMsg, bannerWarn) { onOpenPlans() }
        block == LicenseBlock.TOTAL -> HomeBanner(Icons.Filled.DataUsage, blockMsg, bannerErr) { onOpenPlans() }
        block != null -> HomeBanner(Icons.Filled.Block, blockMsg, bannerErr) { onOpenPlans() }
        daysLeft != null && daysLeft in 1..3 -> HomeBanner(Icons.Filled.Timer, trf("Your plan ends in %d days. Renew it to stay connected.", daysLeft), bannerWarn) { onOpenPlans() }
        remainingPct != null && remainingPct <= 10 -> HomeBanner(Icons.Filled.DataUsage, trf("Only %d%% of your data is left.", remainingPct), bannerWarn) { onOpenPlans() }
        serverStatus?.update != null -> serverStatus?.update?.let { u ->
            HomeBanner(Icons.Filled.SystemUpdate, tr("A new version is available. Tap to update."), bannerAccent) { if (u.url.isNotBlank()) uriHandler.openUri(u.url) }
        }
        else -> null
    }
    // نوارهای سرور (چندتا می‌تونه باشه) + پیام محلی (حجم/انقضا/بلاک)؛ همه به‌نوبت نشون داده می‌شن
    // آفر/تخفیفِ زمان‌دار (فقط وقتی از ربات با تایمر گذاشته شده): همان ظاهر نوار صفحه‌ی پلن‌ها + لمس = باز شدن همان پلن
    val planCfg by com.vauth.foxyvpn.data.rememberPlanConfig()
    val nowOffer = System.currentTimeMillis() / 1000
    val offerKey = listOf("pro", "max").filter { k ->
        val i = planCfg.plan(k); i.disc > 0 && i.dend > nowOffer && rank(k) >= rank(license?.plan)
    }.minByOrNull { planCfg.plan(it).dend }
    val offerBanner: HomeBanner? = offerKey?.let { k ->
        val i = planCfg.plan(k)
        HomeBanner(Icons.Filled.LocalFireDepartment, trf("%s · %d%% off", tr(k.replaceFirstChar { it.uppercase() }), i.disc), bannerAccent) { PlanFocus.key = k; onOpenPlans() }
            .also { it.offerEnd = i.dend }
    }
    val homeBanners: List<HomeBanner> = serverBars.map { sb ->
        val (ic, tone) = when (sb.kind) {
            "good" -> Icons.Filled.CheckCircle to okTone
            "alert" -> Icons.Filled.ErrorOutline to bannerErr
            "info" -> Icons.Filled.Info to bannerAccent
            else -> Icons.Filled.Build to bannerWarn // maint / warn
        }
        HomeBanner(ic, sb.text.ifBlank { tr("The server is being updated. Please try again in a few minutes.") }, tone, null)
    }.let { listOfNotNull(offerBanner) + it } + listOfNotNull(localBanner)
    // افکت تمام‌صفحه‌ی نوار (برای هر نوار فقط یک بار)
    var fxPlaying by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(serverBars) {
        val pending = serverBars.firstOrNull { it.fx.isNotBlank() && it.fx != "none" && it.id.isNotBlank() && !NoticeClient.fxSeen(app, it.id) }
        if (pending != null && fxPlaying == null) { NoticeClient.markFxSeen(app, pending.id); fxPlaying = pending.fx }
    }

    // پینگ فقط «بعد از اتصال» اندازه‌گیری می‌شه؛ قبل از اتصال (و تو حالت CONNECTING) null می‌مونه
    // و قرص پینگ خاکستری و بدون عدد ("---") نشون داده می‌شه.
    // سنجش TCP مستقیم به سرور انتخابی از شبکه‌ی واقعی گوشی انجام می‌شه (نه از دل تونل)؛ PingUtil.init لازمه.
    var pingMs by remember { mutableStateOf<Int?>(null) }
    // با ضربه روی قرص پینگ زیاد می‌شه تا سنجش فوراً از نو شروع بشه؛ pingBusy تا رسیدن جواب قرص رو کم‌رنگ نگه می‌داره
    var pingRefreshTick by remember { mutableStateOf(0) }
    var pingBusy by remember { mutableStateOf(false) }
    LaunchedEffect(state, selectedProxy?.host, selectedProxy?.port, isForeground, pingRefreshTick) {
        if (state != ConnectionState.CONNECTED) {
            pingMs = null
            pingBusy = false
        }
        if (state != ConnectionState.CONNECTED || !isForeground) return@LaunchedEffect
        val proxy = selectedProxy ?: return@LaunchedEffect
        var misses = 0
        while (true) {
            val ms = PingUtil.measureTcpLatencyMs(proxy.host, proxy.port, samples = 3)
            pingBusy = false
            if (ms != null) {
                pingMs = ms
                misses = 0
            } else if (++misses >= 2) {
                pingMs = null // دو بار پشت‌سرهم جواب نداد: واقعاً در دسترس نیست
            }
            delay(PING_REFRESH_MS)
        }
    }

    val c = LocalFoxExtras.current

    // کشوری که نمایش می‌دیم: وصل → کشور خروجی (تا چک تموم نشده کشور سرور انتخابی)،
    // در حال اتصال → کشور سرور انتخابی، قطع → کشور واقعی خودمون (اگه چک نشد، ایران).
    val shownCountry = when (state) {
        ConnectionState.CONNECTED -> countryCode.ifBlank { selectedProxy?.countryCode.orEmpty() }
        ConnectionState.CONNECTING -> selectedProxy?.countryCode.orEmpty()
        ConnectionState.DISCONNECTED -> countryCode.ifBlank { "IR" }
    }

    val statusColors = LocalFoxyStatusColors.current
    val ping = pingMs
    // رنگ پینگ مثل لیست سرورها: سبز / زرد / قرمز بر اساس کیفیت؛ بدون عدد (قبل از اتصال) خاکستری
    val pingLevel = ping?.let { PingUtil.level(it) } ?: 0
    val pingColor = when (pingLevel) {
        3 -> PingGood
        2 -> PingMedium
        1 -> PingBad
        // قبل از اتصال خاکستری؛ وقتی در حال اتصال هست نارنجی
        else -> if (state == ConnectionState.CONNECTING) PingConnecting else c.textSecondary
    }
    val view = LocalView.current

    // لرزش ریز موقع وصل شدنِ موفق (فقط وقتی از «در حال اتصال» به «وصل» می‌ره، نه موقع باز شدن برنامه)
    var previousState by remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        if (previousState == ConnectionState.CONNECTING && state == ConnectionState.CONNECTED) hapticSuccess(view)
        previousState = state
    }
    // دلیل خطا: با ✕ بسته می‌شه؛ خطای جدید (متن متفاوت) دوباره نشون داده می‌شه
    var dismissedError by remember { mutableStateOf<String?>(null) }
    val visibleError = lastError?.takeIf { it.isNotBlank() && it != dismissedError }
    // لرزش خطا فقط برای خطای تازه؛ نه هر بار که صفحه از نو ساخته می‌شه و خطای قدیمی هنوز تو سرویس مونده
    val initialError = remember { lastError }
    LaunchedEffect(lastError) {
        if (!lastError.isNullOrBlank() && lastError != initialError) hapticError(view)
    }
    // با شروع اتصال تازه، خطای قبلی (حتی با همون متن) دوباره می‌تونه نشون داده بشه
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTING) dismissedError = null
    }
    val targetRingColor = when (state) {
        ConnectionState.CONNECTED -> statusColors.connected
        ConnectionState.CONNECTING -> statusColors.connecting
        ConnectionState.DISCONNECTED -> if (block != null) MaterialTheme.colorScheme.error else c.accent
    }
    val ringColor by animateColorAsState(targetValue = targetRingColor, label = "connect-ring-color")
    val scale by animateFloatAsState(
        targetValue = if (state == ConnectionState.CONNECTED) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "connect-scale",
    )

    val layoutDirection = LocalLayoutDirection.current

    // ───── انیمیشن ورود (یک‌بار در هر اجرا) ─────
    var entered by remember { mutableStateOf(EntryGate.played) }
    LaunchedEffect(Unit) {
        if (!EntryGate.played) {
            EntryGate.played = true
            entered = true
        }
    }
    val entranceHeader = rememberEntrance(entered, 0)
    val entranceMain = rememberEntrance(entered, 1)
    val entranceData = rememberEntrance(entered, 3)
    // دکمه‌ی اتصال: پرش فنری + حلقه‌ی یک‌باره
    val buttonPop = animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "button-pop",
    )
    val buttonBurst = animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 250, easing = LinearOutSlowInEasing),
        label = "button-burst",
    )

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
        ) {
            // بدون اسکرول: بالا (هدر + کارت اصلی) ثابته، وسط فضای باقی‌مانده رو برای دکمه‌ی اتصال
            // می‌گیره و کارت دیتا چسبیده به لبه‌ی پایین صفحه‌ست.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        top = padding.calculateTopPadding(),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ───── هدر: لوگو + نام + تنظیمات ─────
                    Row(
                        modifier = Modifier.fillMaxWidth().entrance(entranceHeader),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_fox_logo),
                                contentDescription = null,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = c.textPrimary)) { append("FOX-") }
                                        withStyle(SpanStyle(color = c.accent)) { append("VPN") }
                                    },
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = tr("Secure & Private"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // کلید خورشید/ماه: فقط برای تم‌های لایت/دارک‌دار و وقتی از تنظیمات خاموشش نکرده باشن
                            val systemDark = isSystemInDarkTheme()
                            if (themeController.showHomeToggle && themeController.palette.supportsLightDark) {
                                val isDarkNow = themeController.resolveDark(systemDark)
                                ThemeToggleButton(
                                    isDark = isDarkNow,
                                    c = c,
                                    onClick = {
                                        themeController.set(if (isDarkNow) ThemeMode.LIGHT else ThemeMode.DARK)
                                    },
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            HeartHeaderButton(c)
                            Spacer(Modifier.width(10.dp))
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(c.card)
                                    .border(1.dp, c.cardBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = tr("Settings"),
                                    tint = c.textPrimary.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }


                    Spacer(Modifier.height(10.dp))

                    AnimatedVisibility(
                        visible = homeBanners.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                    ) {
                        Column {
                            BannerCarousel(homeBanners, c)
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    // ───── کارت اصلی: وضعیت، تایمر، ترافیک، سرور ─────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .entrance(entranceMain)
                            .foxCard(c, 28.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                // قطع: خاکستری · در حال اتصال: نارنجی · وصل: سبز
                                                when (state) {
                                                    ConnectionState.DISCONNECTED -> c.textSecondary.copy(alpha = 0.45f)
                                                    ConnectionState.CONNECTING -> Color(0xFFFF9F1C)
                                                    ConnectionState.CONNECTED -> Color(0xFF2ECC71)
                                                }
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Crossfade(targetState = state, animationSpec = tween(300), label = "status-label") { st ->
                                        Text(
                                            text = when (st) {
                                                ConnectionState.CONNECTED -> tr("CONNECTED")
                                                ConnectionState.CONNECTING -> tr("CONNECTING…")
                                                ConnectionState.DISCONNECTED -> if (block != null) tr("LOCKED") else tr("READY TO CONNECT")
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp,
                                            color = c.textSecondary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                // تایمر بزرگ
                                // وقتی وصل نیست کم‌رنگ می‌شه و موقع اتصال نرم پررنگ می‌شه
                                val timerColor by animateColorAsState(
                                    targetValue = if (state == ConnectionState.CONNECTED) c.textPrimary
                                    else c.textPrimary.copy(alpha = 0.45f),
                                    animationSpec = tween(450),
                                    label = "timer-color",
                                )
                                TimerText(elapsed = { elapsedSeconds }, color = timerColor)
                            }

                            // پینگ (بعد از اتصال اندازه‌گیری می‌شه؛ قبلش خاکستری "---")
                            PingPill(
                                pingMs = ping,
                                color = pingColor,
                                busy = pingBusy,
                                // فقط وقتی وصله: لرزش ریز + سنجش دوباره‌ی پینگ
                                onClick = if (state == ConnectionState.CONNECTED && !pingBusy) {
                                    {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        pingBusy = true
                                        pingRefreshTick++
                                    }
                                } else null,
                            )
                        }

                        // سرعت لحظه‌ای + مصرف کل (دانلود و آپلود کنار هم در یک باکس)
                        TrafficCard(c = c, traffic = trafficState, connected = state == ConnectionState.CONNECTED)

                        // سطر کشور / IP سرور + دکمه‌ی تغییر سرور
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .foxCard(c, 18.dp, color = c.innerCard.copy(alpha = 0.6f))
                                .clickable(onClick = onOpenServers)
                                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isIranShown = shownCountry.equals("IR", ignoreCase = true)
                            val shownCountryName = remember(shownCountry) { countryDisplayName(shownCountry) }
                            val locationTitle = when {
                                isIranShown -> tr("Iran")
                                shownCountryName != null -> shownCountryName
                                state == ConnectionState.CONNECTING -> tr("Connecting…")
                                else -> tr("Location")
                            }
                            Box(
                                modifier = Modifier.size(width = 52.dp, height = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // با عوض شدن کشور، پرچم نرم جایگزین می‌شه (به‌جای پرش ناگهانی)
                                Crossfade(targetState = shownCountry, animationSpec = tween(350), label = "flag") { code ->
                                    val isIran = code.equals("IR", ignoreCase = true)
                                    val emoji = flagEmoji(code)
                                    when {
                                        isIran -> Image(
                                            painter = painterResource(id = R.drawable.ic_flag_lion_sun),
                                            contentDescription = tr("Iran flag"),
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        emoji != null -> Text(text = emoji, fontSize = 30.sp)
                                        else -> Icon(
                                            imageVector = Icons.Filled.Public,
                                            contentDescription = tr("Server location"),
                                            tint = if (state == ConnectionState.CONNECTED) c.accent else c.textSecondary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state == ConnectionState.DISCONNECTED) tr("YOUR LOCATION") else tr("SERVER LOCATION"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = c.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Crossfade(targetState = locationTitle, animationSpec = tween(350), label = "location-title") { title ->
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Crossfade(targetState = ipAddress, animationSpec = tween(350), label = "ip-address") { ip ->
                                    Text(
                                        text = trf("IP  %s", ip),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            ChangeServerButton(c = c, onClick = onOpenServers)
                        }
                    }

                    // ───── دلیل خطا / هشدار اتصال (فقط وقتی هست) ─────
                    AnimatedVisibility(
                        visible = visibleError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        // آخرین متن رو نگه می‌داریم تا موقع محو شدن، متن نپره
                        var shown by remember { mutableStateOf("") }
                        if (visibleError != null) shown = visibleError
                        Column {
                            Spacer(Modifier.height(10.dp))
                            ErrorBanner(message = shown, onDismiss = { dismissedError = lastError })
                        }
                    }
                }

                // ───── دکمه بزرگ اتصال با هاله؛ اندازه‌اش با فضای خالی تنظیم می‌شه ─────
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val buttonArea = minOf(maxWidth, maxHeight, 270.dp)
                    ConnectButton(
                        size = buttonArea,
                        ringColor = ringColor,
                        scale = scale,
                        connected = state != ConnectionState.DISCONNECTED,
                        labelColor = if (state == ConnectionState.DISCONNECTED && block == null) c.accentText else ringColor,
                        c = c,
                        onClick = {
                            hapticPress(view)
                            if (state == ConnectionState.DISCONNECTED) {
                                if (block != null) onOpenPlans() else onRequestConnect()
                            } else {
                                onDisconnect()
                            }
                        },
                        connecting = state == ConnectionState.CONNECTING,
                        animate = isForeground,
                        entryPop = buttonPop,
                        entryBurst = buttonBurst,
                    )
                }

                // ───── پنل پایین: Data remaining (چسبیده به لبه‌ی پایین) ─────
                DataRemainingCard(
                    remainingGb = dataRemainingGb,
                    totalGb = dataTotalGb,
                    unlimited = dataUnlimited,
                    planLabel = planLabel,
                    onPlanClick = onOpenPlans,
                    onCardClick = onOpenAccount,
                    dailyRemainingGb = dailyRemainingGb,
                    dailyTotalGb = dailyTotalGb,
                    daysLeft = daysLeftCard,
                    note = if (useLicense) null else licenseNote,
                    c = c,
                    lowColor = statusColors.connecting,
                    criticalColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().entrance(entranceData),
                    bottomInset = padding.calculateBottomPadding(),
                )
            }
            fxPlaying?.let { fx -> com.vauth.foxyvpn.ui.components.FxOverlay(fx, onDone = { fxPlaying = null }) }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

@Composable
private fun ConnectButton(
    size: Dp,
    ringColor: Color,
    scale: Float,
    connected: Boolean,
    labelColor: Color,
    c: FoxExtras,
    onClick: () -> Unit,
    connecting: Boolean = false,
    animate: Boolean = true,
    entryPop: State<Float>,
    entryBurst: State<Float>,
) {
    // همه‌ی اندازه‌ها نسبت به size هستن (نسبت‌ها همون طراحی قبلی با size = 250)
    val buttonSize = size * 0.824f * scale
    val logoTint = c.accent
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                // ورود فنری: از کوچیک و محو شروع می‌شه و کمی بزرگ‌تر از اندازه‌ی نهایی می‌پره و می‌شینه
                val p = entryPop.value
                val sc = 0.55f + 0.45f * p
                scaleX = sc
                scaleY = sc
                alpha = (p * 2f).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        // حلقه‌های هاله بیرونی
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(ringColor.copy(alpha = 0.14f), ringColor.copy(alpha = 0.03f))
                    )
                )
                .border(1.dp, ringColor.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size * 0.912f)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.07f))
                .border(1.dp, ringColor.copy(alpha = 0.18f), CircleShape)
        )

        // انیمیشن‌های ریز: فقط وقتی لازمه و برنامه جلوی چشمه ساخته می‌شن (مصرف باتری ~صفر در حالت قطع)
        if (animate && connecting) {
            // در حال اتصال: ضربان تندتر و کوتاه‌تر (دوره‌ی ۱ ثانیه، دامنه‌ی کم)
            PulseRing(
                size = buttonSize,
                maxScale = 1.12f * size.value / buttonSize.value,
                color = ringColor,
                period = 1000,
                peakAlpha = 0.6f,
            )
        } else if (animate && connected) {
            // وصل: ضربان نرم و آروم و پخش‌تر
            PulseRing(size = buttonSize, maxScale = 1.3f * size.value / buttonSize.value, color = ringColor)
        }

        // حلقه‌ی ورود: فقط یک‌بار موقع باز شدن برنامه از لبه‌ی دکمه پخش می‌شه و محو می‌شه
        Box(
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer {
                    val p = entryBurst.value
                    if (p <= 0f || p >= 1f) {
                        alpha = 0f
                    } else {
                        val sc = 1f + 0.9f * p
                        scaleX = sc
                        scaleY = sc
                        alpha = (1f - p) * 0.55f
                    }
                }
                .border(2.dp, ringColor, CircleShape)
        )

        // دکمه اصلی
        Box(
            modifier = Modifier
                .size(buttonSize)
                .shadow(28.dp, CircleShape, ambientColor = ringColor, spotColor = ringColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(c.buttonFillCenter, c.buttonFillEdge))
                )
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(listOf(c.accentLight, ringColor)),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fox_mono),
                    contentDescription = when {
                        connecting -> tr("Connecting…")
                        connected -> tr("Disconnect")
                        else -> tr("Connect")
                    },
                    tint = logoTint,
                    modifier = Modifier.size(size * 0.256f)
                )
                Spacer(Modifier.height(size * 0.056f))
                Text(
                    text = when {
                        connecting -> tr("CONNECTING…")
                        connected -> tr("DISCONNECT")
                        else -> tr("CONNECT")
                    },
                    maxLines = 1,
                    fontSize = (size.value * 0.072f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (size.value * 0.016f).sp,
                    color = labelColor
                )
            }
        }
    }
}

/**
 * موج‌های ضربان: از لبه‌ی دکمه شروع می‌شن، از هاله‌ی بیرونی هم رد می‌شن و آروم محو می‌شن.
 * دو موج با فاصله‌ی نیم‌دوره پشت هم می‌آن تا پیوسته و پخش‌تر دیده بشه.
 */
@Composable
private fun PulseRing(size: Dp, maxScale: Float, color: Color, period: Int = 3200, peakAlpha: Float = 0.5f) {
    val transition = rememberInfiniteTransition(label = "pulse-ring")
    val first = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulse-progress-1",
    )
    val second = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(period, easing = LinearOutSlowInEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset(period / 2, StartOffsetType.FastForward),
        ),
        label = "pulse-progress-2",
    )
    listOf(first, second).forEach { p ->
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val t = p.value
                    val sc = 1f + (maxScale - 1f) * t
                    scaleX = sc
                    scaleY = sc
                    alpha = peakAlpha * (1f - t) * (1f - t * 0.3f)
                }
                .border(2.dp, color, CircleShape),
        )
    }
}

/** کلید لایت/دارک: آیکن خورشید و ماه با چرخش و محو/بزرگ‌شدن عوض می‌شن. */
@Composable
private fun ThemeToggleButton(isDark: Boolean, c: FoxExtras, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 360f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "theme-toggle-rotation",
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(c.card)
            .border(1.dp, c.cardBorder, CircleShape)
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 90)) + scaleIn(tween(240, delayMillis = 90), initialScale = 0.3f)) togetherWith
                    (fadeOut(tween(110)) + scaleOut(tween(110), targetScale = 0.3f))
            },
            label = "theme-toggle-icon",
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = if (dark) tr("Light") else tr("Dark"),
                tint = c.accentText,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun SpeedCard(
    modifier: Modifier,
    colors: FoxExtras,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
) {
    Row(
        modifier = modifier
            .foxCard(colors, 20.dp, color = colors.innerCard.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .border(2.dp, iconColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                color = colors.textSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IconBubble(icon: androidx.compose.ui.graphics.vector.ImageVector, c: FoxExtras) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.accent.copy(alpha = 0.10f))
            .border(1.dp, c.cardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** میله‌های سیگنال؛ level = تعداد میله‌های روشن (۰ تا ۳). */
@Composable
private fun SignalBars(
    color: Color,
    level: Int = 3,
    heights: List<Dp> = listOf(10.dp, 16.dp, 22.dp),
    barWidth: Dp = 5.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .size(width = barWidth, height = h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < level) color else color.copy(alpha = 0.25f))
            )
        }
    }
}

/** لرزش‌های ریز: فشردن دکمه / اتصال موفق / خطا. اگه کاربر لرزش لمسی سیستم رو خاموش کرده باشه، اتفاقی نمی‌افته. */
private fun hapticPress(view: android.view.View) {
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

private fun hapticSuccess(view: android.view.View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK,
    )
}

private fun hapticError(view: android.view.View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )
}

/** نوار دلیل خطا / هشدار اتصال زیر کارت اصلی؛ با ضربه روی ✕ بسته می‌شه. */
@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tone = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(16.dp)
    val c = LocalFoxExtras.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.35f), shape)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = c.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = tr("Dismiss"), tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/** قرص پینگ: عدد + میله‌های سیگنال؛ رنگ مثل لیست سرورها (سبز / نارنجی / قرمز). بدون عدد: خاکستری و "---". */
@Composable
private fun PingPill(
    pingMs: Int?,
    color: Color,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val level = pingMs?.let { PingUtil.level(it) } ?: 0
    val contentAlpha by animateFloatAsState(targetValue = if (busy) 0.5f else 1f, label = "ping-busy")
    Row(
        modifier = modifier
            .widthIn(min = 104.dp)
            .alpha(contentAlpha)
            .clip(shape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignalBars(color = color, level = level)
        Spacer(Modifier.width(8.dp))
        if (pingMs != null) {
            Text(
                text = pingMs.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "ms",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.85f),
                maxLines = 1
            )
        } else {
            Text(
                text = "---",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = color,
                maxLines = 1
            )
        }
    }
}

/** باکس ترافیک: دانلود و آپلود کنار هم؛ هرکدام سرعت لحظه‌ای + مصرف کل از شروع اتصال. */
@Composable
private fun TrafficCard(c: FoxExtras, traffic: State<TrafficState>, connected: Boolean) {
    val t = traffic.value
    // مثل تایمر: وقتی وصل نیست اعداد سرعت کم‌رنگ‌ان و موقع اتصال نرم پررنگ می‌شن
    val numberColor by animateColorAsState(
        targetValue = if (connected) c.textPrimary else c.textPrimary.copy(alpha = 0.45f),
        animationSpec = tween(450),
        label = "traffic-number-color",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .foxCard(c, 20.dp, color = c.innerCard.copy(alpha = 0.6f))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrafficColumn(
            modifier = Modifier.weight(1f),
            c = c,
            label = tr("DOWNLOAD"),
            icon = Icons.Filled.ArrowDownward,
            tint = c.download,
            speedBytes = t.downloadSpeed.toLong(),
            active = connected && t.downloadSpeed > 0,
            connected = connected,
            down = true,
            numberColor = numberColor,
            total = formatBytes(t.totalDownload),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(54.dp)
                .background(c.cardBorder)
        )
        TrafficColumn(
            modifier = Modifier.weight(1f),
            c = c,
            label = tr("UPLOAD"),
            icon = Icons.Filled.ArrowUpward,
            tint = c.upload,
            speedBytes = t.uploadSpeed.toLong(),
            active = connected && t.uploadSpeed > 0,
            connected = connected,
            down = false,
            numberColor = numberColor,
            total = formatBytes(t.totalUpload),
        )
    }
}

@Composable
private fun TrafficColumn(
    modifier: Modifier,
    c: FoxExtras,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    speedBytes: Long,
    active: Boolean,
    connected: Boolean,
    down: Boolean,
    numberColor: Color,
    total: String,
) {
    Column(modifier = modifier.padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowArrow(icon = icon, tint = tint, down = down, active = active)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = c.textSecondary,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        SmoothSpeedText(speedBytes = speedBytes, color = numberColor)
        ActivityMeter(speedBytes = speedBytes, connected = connected, tint = tint, track = c.cardBorder)
        Spacer(Modifier.height(4.dp))
        Text(
            text = trf("Total  %s", total),
            fontSize = 12.sp,
            color = c.textSecondary,
            maxLines = 1
        )
    }
}

/** فلش دانلود/آپلود: وقتی داده در جریانه، فلش از داخل حباب «رد می‌شه» (فقط alpha/translation؛ بدون recompose). */
@Composable
private fun FlowArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, down: Boolean, active: Boolean) {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            val ph by rememberInfiniteTransition(label = "flow").animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(1100, easing = androidx.compose.animation.core.LinearEasing)),
                label = "flow-phase",
            )
            Icon(
                imageVector = icon, contentDescription = null, tint = tint,
                modifier = Modifier.size(14.dp).graphicsLayer {
                    val dir = if (down) 1f else -1f
                    translationY = dir * (ph - 0.5f) * 14.dp.toPx()
                    val k = 1f - (2f * kotlin.math.abs(ph - 0.5f)).let { it * it }
                    alpha = k.coerceIn(0.1f, 1f)
                },
            )
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * نوار نازک فعالیت زیر سرعت (مقیاس لگاریتمی؛ ~۰ تا ۲۰ MB/s).
 * وصل باشی و ترافیک نباشه هم یه ته‌رنگ کم‌رنگ پیداست (خالی دیده نمی‌شه)؛ قطع باشی نوار خیلی کم‌رنگ و خالیه.
 */
@Composable
private fun ActivityMeter(speedBytes: Long, connected: Boolean, tint: Color, track: Color) {
    val live = (kotlin.math.ln(1.0 + speedBytes / 1024.0) / kotlin.math.ln(1.0 + 20480.0)).toFloat().coerceIn(0f, 1f)
    val target = if (connected) maxOf(live, 0.06f) else 0f
    val f by animateFloatAsState(target, tween(450, easing = FastOutSlowInEasing), label = "meter")
    val a by animateFloatAsState(if (connected) 1f else 0.5f, tween(450), label = "meter-a")
    Spacer(Modifier.height(5.dp))
    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(track.copy(alpha = 0.6f * a))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(2.dp)).background(tint.copy(alpha = if (speedBytes > 0) 1f else 0.45f)))
    }
}

/** دکمه‌ی تغییر سرور کنار سطر کشور. */
@Composable
private fun ChangeServerButton(c: FoxExtras, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .shadow(6.dp, shape, ambientColor = c.accent.copy(alpha = 0.5f), spotColor = c.accent.copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.accent, c.accent.copy(alpha = 0.82f))))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.SwapHoriz,
            contentDescription = tr("Change server"),
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = tr("CHANGE"),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * پنل «Data remaining»: چسبیده به لبه‌ی پایین صفحه، با درصد، خط پیشرفت و مصرف/کل.
 * خط و متن هم‌رنگ و هماهنگ‌اند: رنگ تم، و با کم شدن دیتا (≤۲۰٪ زرد، ≤۱۰٪ قرمز).
 * [bottomInset] فاصله‌ی نوار ناوبری سیستمه تا پنل زیرش ادامه پیدا کنه و فاصله‌ای نمونه.
 */
@Composable
private fun DataRemainingCard(
    remainingGb: Double?,
    totalGb: Double,
    unlimited: Boolean = false,
    planLabel: String? = null,
    onPlanClick: () -> Unit = {},
    onCardClick: () -> Unit = {},
    dailyRemainingGb: Double? = null,
    dailyTotalGb: Double = 0.0,
    daysLeft: Long? = null,
    note: String? = null,
    c: FoxExtras,
    lowColor: Color,
    criticalColor: Color,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    fun toneFor(f: Float, none: Boolean) = when {
        none -> c.accent
        f <= 0.10f -> criticalColor
        f <= 0.20f -> lowColor
        else -> c.accent
    }
    val fraction = if (unlimited) 1f
    else if (remainingGb == null || totalGb <= 0.0) 0f
    else (remainingGb / totalGb).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(fraction, tween(900, easing = FastOutSlowInEasing), label = "data-fraction")
    val animatedTone by animateColorAsState(toneFor(fraction, unlimited || remainingGb == null), label = "data-tone")

    val dailyFraction = if (dailyRemainingGb != null && dailyTotalGb > 0.0) (dailyRemainingGb / dailyTotalGb).coerceIn(0.0, 1.0).toFloat() else null
    val animatedDaily by animateFloatAsState(dailyFraction ?: 0f, tween(900, easing = FastOutSlowInEasing), label = "daily-fraction")
    val dailyTone by animateColorAsState(toneFor(dailyFraction ?: 1f, dailyFraction == null), label = "daily-tone")

    // عدد هم همراه نوار از صفر بالا می‌ره؛ در حالت پایدار دقیقاً همون remainingGb می‌مونه
    val shownRemaining = remainingGb?.let { r -> if (fraction > 0f) r * (animatedFraction / fraction).coerceIn(0f, 1.5f) else r }
    val valueText = when {
        unlimited -> tr("Unlimited")
        remainingGb == null -> "--"
        else -> trf("%.1f GB left", shownRemaining ?: remainingGb)
    }
    val percentText = when {
        unlimited -> "∞"
        remainingGb == null -> "--%"
        else -> "${(animatedFraction * 100).roundToInt()}%"
    }

    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val pillShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .shadow(14.dp, shape, ambientColor = c.accent.copy(alpha = 0.2f), spotColor = c.accent.copy(alpha = 0.2f))
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.cardBorder, shape)
            .clickable(onClick = onCardClick)
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Filled.DataUsage, c)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tr("Data remaining"), style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                Text(text = valueText, fontSize = 20.sp, fontWeight = FontWeight.Black, color = animatedTone, maxLines = 1)
            }
            if (planLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(c.accent.copy(alpha = 0.14f))
                        .border(1.dp, c.accent.copy(alpha = 0.35f), pillShape)
                        .clickable(onClick = onPlanClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = c.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = planLabel, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.accent, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(animatedTone.copy(alpha = 0.14f))
                    .border(1.dp, animatedTone.copy(alpha = 0.35f), pillShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = percentText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = animatedTone, maxLines = 1)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(animatedTone.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(animatedTone.copy(alpha = 0.55f), animatedTone)))
            )
        }

        // امروز (سقف روزانه) + روزهای باقی‌مانده؛ جای «کل/مصرف‌شده»ی قبلی
        if (dailyFraction != null || daysLeft != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (dailyFraction != null && dailyRemainingGb != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trf("Today: %.1f GB left", dailyRemainingGb),
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = dailyTone, maxLines = 1,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.92f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(dailyTone.copy(alpha = 0.15f))) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(animatedDaily).clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(dailyTone.copy(alpha = 0.55f), dailyTone)))
                            )
                        }
                    }
                } else Spacer(Modifier.weight(1f))
                if (daysLeft != null) {
                    Spacer(Modifier.width(12.dp))
                    Row(
                        modifier = Modifier.clip(pillShape).background(c.innerCard).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (daysLeft == 1L) tr("1 day left") else trf("%d days left", daysLeft),
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary, maxLines = 1,
                        )
                    }
                }
            }
        }

        if (note != null) {
            Text(text = note, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
        }
    }
}

/** قلب کنار تنظیمات: فعلاً فقط واکنش نشون می‌ده (رنگ می‌گیره، می‌تپه و لرزش خفیف)؛ بعداً می‌شه بهش کار داد. */
@Composable
private fun HeartHeaderButton(c: FoxExtras) {
    val context = LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("foxvpn_ui", Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(prefs.getBoolean("home_heart", false)) }
    val pink = Color(0xFFFF5C8D)
    val tint by animateColorAsState(if (on) pink else c.textPrimary.copy(alpha = 0.75f), label = "heart-tint")
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(on) {
        if (on) {
            pop.animateTo(1.4f, tween(120))
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 380f))
        }
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (on) pink.copy(alpha = 0.16f) else c.card)
            .border(1.dp, if (on) pink.copy(alpha = 0.5f) else c.cardBorder, CircleShape)
            .clickable {
                on = !on
                prefs.edit().putBoolean("home_heart", on).apply()
                haptics.performHapticFeedback(if (on) androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress else androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (on) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = tr("Favorite"),
            tint = tint,
            modifier = Modifier.size(24.dp).scale(pop.value),
        )
    }
}


/** یک پیام وضعیت روی صفحه‌ی اصلی؛ [onClick] خالی یعنی فقط اطلاع‌رسانی. */
 {
    /** اگه > 0 باشه این نوار «آفر محدود» با شمارش معکوسه (epoch ثانیه‌ی پایان). */
    var offerEnd: Long = 0L
}

/** چند نوار به‌نوبت: هر ۵٫۵ ثانیه با اسلاید+محو عوض می‌شه؛ با کشیدن انگشت هم دستی عوض می‌شه؛ نقطه‌ها جای فعلی رو نشون می‌دن. */
@Composable
private fun BannerCarousel(items: List<HomeBanner>, c: FoxExtras) {
    if (items.isEmpty()) return
    var index by remember { mutableStateOf(0) }
    var touchTick by remember { mutableStateOf(0) }
    val cur = index.coerceIn(0, items.lastIndex)
    if (items.size > 1) {
        LaunchedEffect(items.size, touchTick, cur) {
            delay(5500)
            index = (cur + 1) % items.size
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = cur,
            transitionSpec = {
                val dir = if (targetState >= initialState) 1 else -1
                (androidx.compose.animation.slideInHorizontally(tween(380)) { it / 3 * dir } + fadeIn(tween(380))) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(tween(280)) { -it / 3 * dir } + fadeOut(tween(240)))
            },
            // کلید pointerInput باید cur هم باشد؛ وگرنه lambda قدیمی با index کهنه کار می‌کرد (فقط یک جهت جواب می‌داد)
            modifier = Modifier.pointerInput(items.size, cur) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragCancel = { acc = 0f },
                    onDragEnd = {
                        if (items.size > 1) {
                            if (acc < -50f) index = (cur + 1) % items.size
                            else if (acc > 50f) index = (cur - 1 + items.size) % items.size
                        }
                        touchTick++
                    },
                    onHorizontalDrag = { change, d -> change.consume(); acc += d },
                )
            },
            label = "banner-carousel",
        ) { i -> StatusBannerBar(items[i.coerceIn(0, items.lastIndex)], c) }
        if (items.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items.indices.forEach { i ->
                    Box(
                        Modifier.size(if (i == cur) 14.dp else 6.dp, 6.dp).clip(CircleShape)
                            .background(if (i == cur) c.accent else c.textSecondary.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBannerBar(b: HomeBanner, c: FoxExtras) {
    if (b.offerEnd > 0L) {
        val now = rememberNowSec(true)
        PromoStrip(c, b.text, now, b.offerEnd) { b.onClick?.invoke() }
        return
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(b.tone.copy(alpha = 0.14f))
            .border(1.dp, b.tone.copy(alpha = 0.45f), shape)
            .then(if (b.onClick != null) Modifier.clickable(onClick = b.onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(b.tone.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) { Icon(b.icon, contentDescription = null, tint = b.tone, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(
            text = com.vauth.foxyvpn.i18n.bidi(b.text),
            modifier = Modifier.weight(1f),
            color = c.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp,
        )
        if (b.onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = b.tone, modifier = Modifier.size(14.dp))
        }
    }
}
