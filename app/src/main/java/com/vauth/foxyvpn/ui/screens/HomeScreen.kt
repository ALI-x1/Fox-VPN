package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.TrafficMonitor
import com.vauth.foxyvpn.data.ByteFormat
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.vpn.ExitCheck
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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

    // وضعیت ترافیک و IP
    val trafficState by TrafficMonitor.trafficState.collectAsState()
    var ipAddress by remember { mutableStateOf("در حال بررسی...") }
    var countryCode by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // مدیریت شروع/پایان مانیتورینگ ترافیک و استعلام خودکار IP هنگام تغییر وضعیت اتصال
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            TrafficMonitor.start()
        } else {
            TrafficMonitor.stop()
        }

        // استعلام IP (اگر متصل باشد از طریق پروکسی پورت 1080 وگرنه به صورت مستقیم)
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

    val statusColors = LocalFoxyStatusColors.current
    val targetRingColor = when (state) {
        ConnectionState.CONNECTED -> statusColors.connected
        ConnectionState.CONNECTING -> statusColors.connecting
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }

    val ringColor by animateColorAsState(targetValue = targetRingColor, label = "connect-ring-color")
    val scale by animateFloatAsState(
        targetValue = if (state == ConnectionState.CONNECTED) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "connect-scale",
    )

    val systemInDarkTheme = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoxyVPN") },
                actions = {
                    val mode = themeController.mode
                    val showingDark = themeController.resolveDark(systemInDarkTheme)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { themeController.toggle(systemInDarkTheme) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    themeController.set(ThemeMode.SYSTEM)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Filled.LightMode
                                ThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            contentDescription = "Theme Toggle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // دکمه بزرگ اتصال / قطع اتصال
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.15f))
                    .clickable {
                        if (state == ConnectionState.DISCONNECTED) onRequestConnect() else onDisconnect()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size((140 * scale).dp)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Power,
                        contentDescription = if (state == ConnectionState.DISCONNECTED) "Connect" else "Disconnect",
                        tint = ringColor,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            Spacer(Modifier.padding(top = 24.dp))
            Text(
                text = when (state) {
                    ConnectionState.CONNECTED -> "Connected"
                    ConnectionState.CONNECTING -> "Connecting…"
                    ConnectionState.DISCONNECTED -> "Disconnected"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            if (state == ConnectionState.CONNECTING) {
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    "Tap to cancel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val error = lastError
            if (error != null && state != ConnectionState.CONNECTING) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.padding(top = 24.dp))

            // کارت اطلاعات شبکه، IP، پرچم (شیر و خورشید برای ایران) و سرعت آپلود/دانلود
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Alignment.SpaceBetween.let { Arrangement.SpaceBetween },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // شرط نمایش پرچم شیر و خورشید برای ایران
                            if (countryCode.uppercase() == "IR") {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_lion_sun_flag),
                                    contentDescription = "Iran Lion & Sun Flag",
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.padding(start = 10.dp))
                            Column {
                                Text(
                                    text = "IP: $ipAddress",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (countryCode.uppercase() == "IR") "ایران (Iran)" else countryCode.ifBlank { "Location" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = countryCode.uppercase().ifBlank { "---" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // بخش نمایش سرعت آپلود و دانلود در صورت اتصال
                    if (state == ConnectionState.CONNECTED) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⬆️ ${ByteFormat.format(trafficState.uploadSpeed.toLong())}/s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "⬇️ ${ByteFormat.format(trafficState.downloadSpeed.toLong())}/s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.padding(top = 12.dp))

            // کارت انتخاب سرور و لوکیشن
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenServers),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Server Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Public, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(
                            selectedProxy?.let { it.countryName.ifBlank { it.countryCode } } ?: "Recommended",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
