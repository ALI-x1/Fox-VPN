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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
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
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.Entitlement
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.vpn.ExitCheck
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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

/** اسم کامل کشور به انگلیسی (DE → Germany)؛ اگه شناخته نشد null. */
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

// ───────────────────────── پینگ ─────────────────────────
// همان سنجش TCP صفحه‌ی انتخاب سرور روی سرور انتخاب‌شده اجرا می‌شود.

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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            TrafficMonitor.start()
        } else {
            TrafficMonitor.stop()
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val proxyPort = if (state == ConnectionState.CONNECTED) 1080 else null
                val result = ExitCheck().verifyExitCountry(socksPort = proxyPort)
                result.onSuccess { info ->
                    ipAddress = info.ip
                    countryCode = info.countryCode
                }.onFailure {
                    // فقط عدد نشون می‌دیم؛ اگه آی‌پی نیومد، به‌جای متن خطا خط تیره
                    ipAddress = "-"
                    countryCode = ""
                }
            } catch (e: Exception) {
                ipAddress = "-"
                countryCode = ""
            }
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

    var pingMs by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state, selectedProxy?.host, selectedProxy?.port) {
        // هنگام قطع یا اتصال، پینگ اجرا نمی‌شود.
        pingMs = null
        if (state != ConnectionState.CONNECTED) return@LaunchedEffect

        val proxy = selectedProxy ?: return@LaunchedEffect
        while (true) {
            pingMs = PingUtil.measureTcpLatencyMs(proxy.host, proxy.port)
            delay(5_000)
        }
    }

    val c = LocalFoxExtras.current

    val statusColors = LocalFoxyStatusColors.current
    val ping = pingMs
    // رنگ پینگ بر اساس وضعیت VPN است: خاکستری در حالت قطع/اتصال، سبز پس از اتصال.
    val pingColor = if (state == ConnectionState.CONNECTED) {
        statusColors.connected
    } else {
        c.textSecondary
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

    val isIran = countryCode.uppercase() == "IR"

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.width(14.dp))
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

                Spacer(Modifier.height(20.dp))

                // ───── کارت اصلی: وضعیت، تایمر، سرعت، سرور ─────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .foxCard(c, 28.dp)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state == ConnectionState.DISCONNECTED) statusColors.connected else ringColor
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
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

                        // پینگ
                        PingPill(pingMs = ping, color = pingColor)
                    }

                    // تایمر بزرگ
                    Text(
                        text = formatElapsed(elapsedSeconds),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.textPrimary
                    )

                    // آپلود / دانلود
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpeedCard(
                            modifier = Modifier.weight(1f),
                            colors = c,
                            label = "UPLOAD",
                            value = formatBytesPerSecond(trafficState.uploadSpeed.toLong()),
                            icon = Icons.Filled.ArrowUpward,
                            iconColor = c.upload
                        )
                        SpeedCard(
                            modifier = Modifier.weight(1f),
                            colors = c,
                            label = "DOWNLOAD",
                            value = formatBytesPerSecond(trafficState.downloadSpeed.toLong()),
                            icon = Icons.Filled.ArrowDownward,
                            iconColor = c.download
                        )
                    }

                    // سطر کشور / IP سرور
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .foxCard(c, 18.dp, color = c.innerCard.copy(alpha = 0.6f))
                            .clickable(onClick = onOpenServers)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isIran) {
                            // پرچم رسمی شیر و خورشید (res/drawable-nodpi/ic_flag_lion_sun.webp)، با نسبت اصلی پرچم
                            Image(
                                painter = painterResource(id = R.drawable.ic_flag_lion_sun),
                                contentDescription = "Iran Flag",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(width = 56.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            val flag = flagEmoji(countryCode)
                            Box(
                                modifier = Modifier.size(width = 56.dp, height = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (flag != null) {
                                    Text(text = flag, fontSize = 30.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = c.accent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isIran) "Iran" else (countryDisplayName(countryCode) ?: countryCode.ifBlank { "Location" }),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c.textPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = c.textSecondary
                                )
                            }
                            Text(
                                text = ipAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        SignalBars(color = statusColors.connected)
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = c.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                // ───── دکمه بزرگ اتصال با هاله نارنجی ─────
                ConnectButton(
                    ringColor = ringColor,
                    scale = scale,
                    connected = state != ConnectionState.DISCONNECTED,
                    labelColor = if (state == ConnectionState.DISCONNECTED) c.accentText else ringColor,
                    c = c,
                    onClick = {
                        if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                    }
                )

                Spacer(Modifier.height(36.dp))

                // ───── کارت پایین: Data remaining ─────
                DataRemainingCard(
                    remainingGb = dataRemainingGb,
                    totalGb = dataTotalGb,
                    unlimited = dataUnlimited,
                    c = c,
                    lowColor = statusColors.connecting,
                    criticalColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

@Composable
private fun ConnectButton(
    ringColor: Color,
    scale: Float,
    connected: Boolean,
    labelColor: Color,
    c: FoxExtras,
    onClick: () -> Unit,
) {
    val buttonSize = (206 * scale).dp
    Box(
        modifier = Modifier.size(290.dp),
        contentAlignment = Alignment.Center
    ) {
        // حلقه‌های هاله بیرونی
        Box(
            modifier = Modifier
                .size(290.dp)
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
                .size(246.dp)
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
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (connected) "DISCONNECT" else "CONNECT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
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
            .size(46.dp)
            .clip(CircleShape)
            .background(c.accent.copy(alpha = 0.10f))
            .border(1.dp, c.cardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(26.dp)
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

/** قرص پینگ کنار وضعیت اتصال (سبز = خوب، زرد = متوسط، قرمز = ضعیف). */
@Composable
private fun PingPill(pingMs: Int?, color: Color) {
    val shape = RoundedCornerShape(24.dp)
    val level = when {
        pingMs == null -> 0
        pingMs <= 120 -> 3
        pingMs <= 300 -> 2
        else -> 1
    }
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.5.dp, color.copy(alpha = 0.7f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignalBars(
            color = color,
            level = level,
            heights = listOf(8.dp, 14.dp, 20.dp),
            barWidth = 4.dp
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "PING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = color
            )
            Text(
                text = pingMs?.let { "$it ms" } ?: "—",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

/**
 * کارت «Data remaining» با خط پیشرفت.
 * خط و متن هم‌رنگ و هماهنگ‌اند: رنگ تم، و با کم شدن دیتا (≤۲۰٪ زرد، ≤۱۰٪ قرمز).
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

    Column(
        modifier = modifier
            .foxCard(c, 26.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Filled.Public, c)
            Spacer(Modifier.width(12.dp))
            Column {
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
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(animatedTone.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(animatedTone.copy(alpha = 0.55f), animatedTone)
                        )
                    )
            )
        }
    }
}
