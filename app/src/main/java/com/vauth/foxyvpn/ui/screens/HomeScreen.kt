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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.luminance
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
import com.vauth.foxyvpn.data.TrafficMonitor
import com.vauth.foxyvpn.data.formatBytesPerSecond
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.vpn.ExitCheck
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// ───────────────────────── تایمر اتصال ─────────────────────────

/** زمان شروع اتصال؛ در سطح process نگه داشته می‌شه تا با رفتن به تنظیمات و برگشتن، تایمر صفر نشه. */
private object SessionClock {
    var startedAt: Long = 0L
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val sec = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
}

// ───────────────────────── پالت رنگی طرح ─────────────────────────

private val FoxOrange = Color(0xFFF7941D)
private val FoxOrangeDeep = Color(0xFFC2570C)
private val FoxOrangeLight = Color(0xFFFFC857)
private val FoxBlue = Color(0xFF29B6F6)
private val FoxGreen = Color(0xFF22B573)

private data class FoxHomeColors(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val innerCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentText: Color,
)

private fun foxColors(dark: Boolean) = if (dark) {
    FoxHomeColors(
        bgTop = Color(0xFF16120E),
        bgBottom = Color(0xFF241B12),
        card = Color(0xFF2A221A),
        cardBorder = Color(0x33FFB066),
        innerCard = Color(0xFF33291F),
        textPrimary = Color(0xFFF5EEE6),
        textSecondary = Color(0xFFB8AB9C),
        accentText = FoxOrangeLight,
    )
} else {
    FoxHomeColors(
        bgTop = Color(0xFFFFFAF4),
        bgBottom = Color(0xFFFDEBD6),
        card = Color(0xB3FFFFFF),
        cardBorder = Color(0x33F7941D),
        innerCard = Color(0xFFFFF4E8),
        textPrimary = Color(0xFF14172B),
        textSecondary = Color(0xFF5B6275),
        accentText = FoxOrangeDeep,
    )
}

private fun Modifier.foxCard(c: FoxHomeColors, radius: Dp, color: Color = c.card): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(6.dp, shape, ambientColor = FoxOrange.copy(alpha = 0.15f), spotColor = FoxOrange.copy(alpha = 0.15f))
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
) {
    val state by FoxyVpnService.state.collectAsState()
    val lastError by FoxyVpnService.lastError.collectAsState()
    val selectedProxy by app.proxyStateStore.selectedProxyFlow.collectAsState()

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
                    ipAddress = "خطا در دریافت"
                    countryCode = ""
                }
            } catch (e: Exception) {
                ipAddress = "نامشخص"
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val c = foxColors(isDark)

    val statusColors = LocalFoxyStatusColors.current
    val targetRingColor = when (state) {
        ConnectionState.CONNECTED -> statusColors.connected
        ConnectionState.CONNECTING -> statusColors.connecting
        ConnectionState.DISCONNECTED -> FoxOrange
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
                                    withStyle(SpanStyle(color = FoxOrange)) { append("VPN") }
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
                                        if (state == ConnectionState.DISCONNECTED) FoxGreen else ringColor
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

                        // دکمه Auto / انتخاب سرور
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(FoxOrange.copy(alpha = 0.08f))
                                .border(1.dp, c.cardBorder, RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenServers)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = c.accentText
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = selectedProxy?.countryCode?.ifBlank { "Auto" } ?: "Auto",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = c.accentText
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = c.accentText
                            )
                        }
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
                            iconColor = FoxOrange
                        )
                        SpeedCard(
                            modifier = Modifier.weight(1f),
                            colors = c,
                            label = "DOWNLOAD",
                            value = formatBytesPerSecond(trafficState.downloadSpeed.toLong()),
                            icon = Icons.Filled.ArrowDownward,
                            iconColor = FoxBlue
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
                            Image(
                                painter = painterResource(id = R.drawable.ic_lion_sun_flag),
                                contentDescription = "Iran Flag",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 48.dp, height = 36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = FoxOrange,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isIran) "Iran (IR)" else countryCode.ifBlank { "Location" },
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
                        SignalBars(color = FoxGreen)
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
                    labelColor = if (state == ConnectionState.DISCONNECTED) FoxOrangeDeep else ringColor,
                    isDark = isDark,
                    onClick = {
                        if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                    }
                )

                Spacer(Modifier.height(36.dp))

                // ───── کارت پایین: Your IP / Location ─────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .foxCard(c, 26.dp)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Filled.Public, c)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your IP",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.textSecondary
                            )
                            Text(
                                text = ipAddress,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenServers)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBubble(Icons.Filled.LocationOn, c)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.textSecondary
                            )
                            Text(
                                text = if (isIran) "Iran" else countryCode.ifBlank { "Iran" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textPrimary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = c.textSecondary
                        )
                    }
                }

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
    isDark: Boolean,
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
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF4A3520), Color(0xFF2E2217))
                        } else {
                            listOf(Color(0xFFFFFCF8), Color(0xFFFBE3C4))
                        }
                    )
                )
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(listOf(FoxOrangeLight, ringColor)),
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
    colors: FoxHomeColors,
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
private fun IconBubble(icon: androidx.compose.ui.graphics.vector.ImageVector, c: FoxHomeColors) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(FoxOrange.copy(alpha = 0.10f))
            .border(1.dp, c.cardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FoxOrange,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** سه میله‌ی سیگنال (مثل طرح). */
@Composable
private fun SignalBars(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(10.dp, 16.dp, 22.dp).forEach { h ->
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
