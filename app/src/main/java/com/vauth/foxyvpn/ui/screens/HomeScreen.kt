package com.vauth.foxyvpn.ui.screens

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// ───────────────────────── تایمر اتصال ─────────────────────────

/** زمان شروع اتصال؛ در سطح process نگه داشته می‌شه تا با رفتن به تنظیمات و برگشتن، تایمر صفر نشه. */
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
    val name = Locale("", cc).getDisplayCountry(Locale.ENGLISH)
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
) {
    val state by FoxyVpnService.state.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()

    val trafficState by TrafficMonitor.trafficState.collectAsState()
    var ipAddress by remember { mutableStateOf("-") }
    var countryCode by remember { mutableStateOf("") }

    // وصل شدن خودکار (اگه تو تنظیمات روشن باشه): فقط یک بار در هر بار اجرای برنامه
    LaunchedEffect(Unit) {
        if (!AutoConnectGate.consumed) {
            AutoConnectGate.consumed = true
            if (app.settingsStore.autoConnect && FoxyVpnService.state.value == ConnectionState.DISCONNECTED) {
                onRequestConnect()
            }
        }
    }

    // مانیتور ترافیک فقط به وضعیت اتصال وابسته‌ست (نه به عوض شدن سرور)
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
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
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            if (SessionClock.startedAt == 0L) SessionClock.startedAt = SystemClock.elapsedRealtime()
            while (true) {
                elapsedSeconds = (SystemClock.elapsedRealtime() - SessionClock.startedAt) / 1000
                delay(1000)
            }
        } else {
            SessionClock.startedAt = 0L
            elapsedSeconds = 0L
        }
    }

    // دیتای اکانت (همون منبع صفحه‌ی Account)؛ بعد از هر تغییر وضعیت اتصال و هر یک دقیقه رفرش می‌شه
    var entitlement by remember { mutableStateOf<Entitlement?>(null) }
    LaunchedEffect(authRepository, state) {
        if (authRepository == null) return@LaunchedEffect
        while (true) {
            try {
                val token = authRepository.currentAccessToken()
                if (token != null) {
                    entitlement = GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, token)
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
    val dataUnlimited = ent != null && !ent.limitedBandwidth
    val dataTotalGb = ent?.maxBytes?.let { it.toDouble() / BYTES_PER_GB }?.takeIf { it > 0.0 } ?: 50.0
    val dataRemainingGb = ent?.quotaRemaining?.let { it.toDouble() / BYTES_PER_GB }

    // پینگ فقط «بعد از اتصال» اندازه‌گیری می‌شه؛ قبل از اتصال (و تو حالت CONNECTING) null می‌مونه
    // و قرص پینگ خاکستری و بدون عدد ("---") نشون داده می‌شه.
    // سنجش TCP مستقیم به سرور انتخابی از شبکه‌ی واقعی گوشی انجام می‌شه (نه از دل تونل)؛ PingUtil.init لازمه.
    var pingMs by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state, selectedProxy?.host, selectedProxy?.port) {
        pingMs = null
        if (state != ConnectionState.CONNECTED) return@LaunchedEffect
        val proxy = selectedProxy ?: return@LaunchedEffect
        var misses = 0
        while (true) {
            val ms = PingUtil.measureTcpLatencyMs(proxy.host, proxy.port, samples = 3)
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
        else -> c.textSecondary
    }
    val targetRingColor = when (state) {
        ConnectionState.CONNECTED -> statusColors.connected
        ConnectionState.CONNECTING -> statusColors.connecting
        ConnectionState.DISCONNECTED -> c.accent
    }
    val ringColor by animateColorAsState(targetValue = targetRingColor, label = "connect-ring-color")
    val scale by animateFloatAsState(
        targetValue = if (state == ConnectionState.CONNECTED) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "connect-scale",
    )

    val layoutDirection = LocalLayoutDirection.current

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
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "Secure & Private",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

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
                                contentDescription = "Settings",
                                tint = c.textPrimary.copy(alpha = 0.75f)
                            )
                        }
                    }


                    Spacer(Modifier.height(10.dp))

                    // ───── کارت اصلی: وضعیت، تایمر، ترافیک، سرور ─────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                                if (state == ConnectionState.DISCONNECTED) statusColors.connected else ringColor
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when (state) {
                                            ConnectionState.CONNECTED -> "CONNECTED"
                                            ConnectionState.CONNECTING -> "CONNECTING…"
                                            ConnectionState.DISCONNECTED -> "READY TO CONNECT"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.sp,
                                        color = c.textSecondary
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                // تایمر بزرگ
                                Text(
                                    text = formatElapsed(elapsedSeconds),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = c.textPrimary
                                )
                            }

                            // پینگ (بعد از اتصال اندازه‌گیری می‌شه؛ قبلش خاکستری "---")
                            PingPill(pingMs = ping, color = pingColor)
                        }

                        // سرعت لحظه‌ای + مصرف کل (دانلود و آپلود کنار هم در یک باکس)
                        TrafficCard(c = c, traffic = trafficState)

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
                            val flag = flagEmoji(shownCountry)
                            val locationTitle = when {
                                isIranShown -> "Iran"
                                countryDisplayName(shownCountry) != null -> countryDisplayName(shownCountry).orEmpty()
                                state == ConnectionState.CONNECTING -> "Connecting…"
                                else -> "Location"
                            }
                            Box(
                                modifier = Modifier.size(width = 52.dp, height = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isIranShown -> Image(
                                        painter = painterResource(id = R.drawable.ic_flag_lion_sun),
                                        contentDescription = "Iran flag",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    flag != null -> Text(text = flag, fontSize = 30.sp)
                                    else -> Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = "Server location",
                                        tint = if (state == ConnectionState.CONNECTED) c.accent else c.textSecondary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state == ConnectionState.DISCONNECTED) "YOUR LOCATION" else "SERVER LOCATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = c.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = locationTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "IP  $ipAddress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            ChangeServerButton(c = c, onClick = onOpenServers)
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
                        labelColor = if (state == ConnectionState.DISCONNECTED) c.accentText else ringColor,
                        c = c,
                        onClick = {
                            if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                        }
                    )
                }

                // ───── پنل پایین: Data remaining (چسبیده به لبه‌ی پایین) ─────
                DataRemainingCard(
                    remainingGb = dataRemainingGb,
                    totalGb = dataTotalGb,
                    unlimited = dataUnlimited,
                    c = c,
                    lowColor = statusColors.connecting,
                    criticalColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    bottomInset = padding.calculateBottomPadding(),
                )
            }
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
) {
    // همه‌ی اندازه‌ها نسبت به size هستن (نسبت‌ها همون طراحی قبلی با size = 250)
    val buttonSize = size * 0.824f * scale
    Box(
        modifier = Modifier.size(size),
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
                Image(
                    painter = painterResource(id = R.drawable.ic_fox_logo),
                    contentDescription = if (connected) "Disconnect" else "Connect",
                    modifier = Modifier.size(size * 0.256f)
                )
                Spacer(Modifier.height(size * 0.056f))
                Text(
                    text = if (connected) "DISCONNECT" else "CONNECT",
                    fontSize = (size.value * 0.072f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (size.value * 0.016f).sp,
                    color = labelColor
                )
            }
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

/** قرص پینگ: عدد + میله‌های سیگنال؛ رنگ مثل لیست سرورها (سبز / نارنجی / قرمز). بدون عدد: خاکستری و "---". */
@Composable
private fun PingPill(pingMs: Int?, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val level = pingMs?.let { PingUtil.level(it) } ?: 0
    Row(
        modifier = modifier
            .widthIn(min = 104.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
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
private fun TrafficCard(c: FoxExtras, traffic: TrafficState) {
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
            label = "DOWNLOAD",
            icon = Icons.Filled.ArrowDownward,
            tint = c.download,
            speed = formatBytesPerSecond(traffic.downloadSpeed.toLong()),
            total = formatBytes(traffic.totalDownload),
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
            label = "UPLOAD",
            icon = Icons.Filled.ArrowUpward,
            tint = c.upload,
            speed = formatBytesPerSecond(traffic.uploadSpeed.toLong()),
            total = formatBytes(traffic.totalUpload),
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
    speed: String,
    total: String,
) {
    Column(modifier = modifier.padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
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
        Text(
            text = speed,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = c.textPrimary,
            maxLines = 1
        )
        Text(
            text = "Total  $total",
            fontSize = 12.sp,
            color = c.textSecondary,
            maxLines = 1
        )
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
            contentDescription = "Change server",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "CHANGE",
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
    c: FoxExtras,
    lowColor: Color,
    criticalColor: Color,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val fraction = if (unlimited) {
        1f
    } else if (remainingGb == null || totalGb <= 0.0) {
        0f
    } else {
        (remainingGb / totalGb).coerceIn(0.0, 1.0).toFloat()
    }
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "data-fraction")
    val tone = when {
        unlimited || remainingGb == null -> c.accent
        fraction <= 0.10f -> criticalColor
        fraction <= 0.20f -> lowColor
        else -> c.accent
    }
    val animatedTone by animateColorAsState(targetValue = tone, label = "data-tone")

    val totalText = if (totalGb % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", totalGb)
    } else {
        String.format(Locale.US, "%.1f", totalGb)
    }
    val valueText = if (unlimited) {
        "Unlimited"
    } else if (remainingGb == null) {
        "-- of $totalText GB"
    } else {
        String.format(Locale.US, "%.1f of %s GB", remainingGb, totalText)
    }
    val percentText = when {
        unlimited -> "∞"
        remainingGb == null -> "--%"
        else -> "${(fraction * 100).roundToInt()}%"
    }
    val usedText = if (!unlimited && remainingGb != null) {
        String.format(Locale.US, "Used %.1f GB", (totalGb - remainingGb).coerceAtLeast(0.0))
    } else {
        null
    }

    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val pillShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .shadow(14.dp, shape, ambientColor = c.accent.copy(alpha = 0.2f), spotColor = c.accent.copy(alpha = 0.2f))
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.cardBorder, shape)
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Filled.Public, c)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Data remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = animatedTone
                )
            }
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(animatedTone.copy(alpha = 0.14f))
                    .border(1.dp, animatedTone.copy(alpha = 0.35f), pillShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = percentText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = animatedTone,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(animatedTone.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(animatedTone.copy(alpha = 0.55f), animatedTone)
                        )
                    )
            )
        }

        if (usedText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = usedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary
                )
                Text(
                    text = "Total $totalText GB",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary
                )
            }
        }
    }
}
