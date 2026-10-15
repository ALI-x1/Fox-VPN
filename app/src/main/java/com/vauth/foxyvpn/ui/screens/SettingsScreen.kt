package com.vauth.foxyvpn.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.BuildConfig
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemePickerSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TELEGRAM_URL = "https://t.me/Ali_jahangirr"
private const val TELEGRAM_HANDLE = "@Ali_jahangirr"

// رنگ «خطر» (Sign out) عمداً از تم مستقل است
private val SxDanger = Color(0xFFE5484D)

// ───────────────────────── رنگ‌ها از تم فعال (LocalFoxExtras) ─────────────────────────

private fun Modifier.settingsCard(c: FoxExtras, radius: Dp, color: Color = c.card): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(6.dp, shape, ambientColor = c.accent.copy(alpha = 0.15f), spotColor = c.accent.copy(alpha = 0.15f))
        .clip(shape)
        .background(color)
        .border(1.dp, c.cardBorder, shape)
}

private fun openTelegram(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
        // اگر هیچ برنامه‌ای برای باز کردن لینک نبود، کاری نکن
    }
}

// ───────────────────────── صفحه تنظیمات ─────────────────────────

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    themeController: ThemeController,
    onOpenLogs: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current

    var exitCheckEnabled by remember { mutableStateOf(settingsStore.exitCheckEnabled) }
    var dohProvider by remember { mutableStateOf(settingsStore.dohProvider) }
    var customDnsEnabled by remember { mutableStateOf(settingsStore.customDnsEnabled) }
    var customDnsServer by remember { mutableStateOf(settingsStore.customDnsServer) }
    var socksBindAddress by remember { mutableStateOf(settingsStore.socksBindAddress) }
    var socksPort by remember { mutableStateOf(settingsStore.socksPort) }
    var httpProxyEnabled by remember { mutableStateOf(settingsStore.httpProxyEnabled) }
    var httpProxyPort by remember { mutableStateOf(settingsStore.httpProxyPort) }
    var proxyOnlyMode by remember { mutableStateOf(settingsStore.proxyOnlyMode) }
    var customEdgeAddress by remember { mutableStateOf(settingsStore.customEdgeAddress) }
    var upstreamProxyEnabled by remember { mutableStateOf(settingsStore.upstreamProxyEnabled) }
    var upstreamProxyType by remember { mutableStateOf(settingsStore.upstreamProxyType) }
    var upstreamProxyHost by remember { mutableStateOf(settingsStore.upstreamProxyHost) }
    var upstreamProxyPort by remember { mutableStateOf(settingsStore.upstreamProxyPort) }
    var upstreamProxyUsername by remember { mutableStateOf(settingsStore.upstreamProxyUsername) }
    var upstreamProxyPassword by remember { mutableStateOf(settingsStore.upstreamProxyPassword) }
    var excludedApps by remember { mutableStateOf(settingsStore.excludedApps) }
    var showDohProviderDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var showSocksBindDialog by remember { mutableStateOf(false) }
    var showSocksPortDialog by remember { mutableStateOf(false) }
    var showHttpPortDialog by remember { mutableStateOf(false) }
    var showEdgeAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyTypeDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyAddressDialog by remember { mutableStateOf(false) }
    var showUpstreamProxyCredentialsDialog by remember { mutableStateOf(false) }
    var showSplitTunnelDialog by remember { mutableStateOf(false) }

    if (showDohProviderDialog) {
        DohProviderPickerDialog(
            current = dohProvider,
            onDismiss = { showDohProviderDialog = false },
            onConfirm = {
                dohProvider = it
                settingsStore.dohProvider = it
                showDohProviderDialog = false
            },
        )
    }
    if (showCustomDnsDialog) {
        CustomDnsPickerDialog(
            current = customDnsServer,
            onDismiss = { showCustomDnsDialog = false },
            onConfirm = {
                customDnsServer = it
                settingsStore.customDnsServer = it
                showCustomDnsDialog = false
            },
        )
    }
    if (showSocksBindDialog) {
        SocksBindAddressPickerDialog(
            current = socksBindAddress,
            onDismiss = { showSocksBindDialog = false },
            onConfirm = {
                socksBindAddress = it
                settingsStore.socksBindAddress = it
                showSocksBindDialog = false
            },
        )
    }
    if (showSocksPortDialog) {
        SocksPortPickerDialog(
            current = socksPort,
            onDismiss = { showSocksPortDialog = false },
            onConfirm = {
                socksPort = it
                settingsStore.socksPort = it
                showSocksPortDialog = false
            },
        )
    }
    if (showHttpPortDialog) {
        HttpPortPickerDialog(
            current = httpProxyPort,
            onDismiss = { showHttpPortDialog = false },
            onConfirm = {
                httpProxyPort = it
                settingsStore.httpProxyPort = it
                showHttpPortDialog = false
            },
        )
    }
    if (showEdgeAddressDialog) {
        CustomEdgeAddressDialog(
            current = customEdgeAddress,
            onDismiss = { showEdgeAddressDialog = false },
            onConfirm = {
                customEdgeAddress = it
                settingsStore.customEdgeAddress = it
                showEdgeAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyTypeDialog) {
        UpstreamProxyTypePickerDialog(
            current = upstreamProxyType,
            onDismiss = { showUpstreamProxyTypeDialog = false },
            onConfirm = {
                upstreamProxyType = it
                settingsStore.upstreamProxyType = it
                showUpstreamProxyTypeDialog = false
            },
        )
    }
    if (showUpstreamProxyAddressDialog) {
        UpstreamProxyAddressDialog(
            currentHost = upstreamProxyHost,
            currentPort = upstreamProxyPort,
            onDismiss = { showUpstreamProxyAddressDialog = false },
            onConfirm = { host, port ->
                upstreamProxyHost = host
                upstreamProxyPort = port
                settingsStore.upstreamProxyHost = host
                settingsStore.upstreamProxyPort = port
                showUpstreamProxyAddressDialog = false
            },
        )
    }
    if (showUpstreamProxyCredentialsDialog) {
        UpstreamProxyCredentialsDialog(
            currentUsername = upstreamProxyUsername,
            currentPassword = upstreamProxyPassword,
            onDismiss = { showUpstreamProxyCredentialsDialog = false },
            onConfirm = { username, password ->
                upstreamProxyUsername = username
                upstreamProxyPassword = password
                settingsStore.upstreamProxyUsername = username
                settingsStore.upstreamProxyPassword = password
                showUpstreamProxyCredentialsDialog = false
            },
        )
    }
    if (showSplitTunnelDialog) {
        SplitTunnelDialog(
            current = excludedApps,
            onDismiss = { showSplitTunnelDialog = false },
            onConfirm = {
                excludedApps = it
                settingsStore.excludedApps = it
                showSplitTunnelDialog = false
            },
        )
    }

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
            ) {
                // ───── هدر: برگشت + عنوان + لوگو ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .border(1.dp, c.cardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = c.textPrimary.copy(alpha = 0.75f)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                        Text(
                            text = "Tune your connection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                            letterSpacing = 1.sp,
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_fox_logo),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ───── Theme (اول صفحه) ─────
                SettingsSection(c, "Theme") {
                    Box(modifier = Modifier.padding(12.dp)) {
                        ThemePickerSection(
                            themeController = themeController,
                            showTitle = false,
                        )
                    }
                }

                // ───── Connection ─────
                SettingsSection(c, "Connection") {
                    SettingSwitchRow(
                        c = c,
                        icon = Icons.Filled.VerifiedUser,
                        title = "Verify exit location",
                        subtitle = "Check that the tunnel exits in the selected country",
                        checked = exitCheckEnabled,
                        divider = false,
                        onCheckedChange = {
                            exitCheckEnabled = it
                            settingsStore.exitCheckEnabled = it
                        },
                    )
                }

                // ───── DNS ─────
                SettingsSection(c, "DNS") {
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Lock,
                        title = "Encrypted DNS",
                        subtitle = dohProvider.label,
                        divider = false,
                        onClick = { showDohProviderDialog = true },
                    )
                    SettingSwitchRow(
                        c = c,
                        icon = Icons.Filled.Dns,
                        title = "Use a custom DNS server",
                        subtitle = "Send your apps' DNS queries to a server you choose",
                        checked = customDnsEnabled,
                        onCheckedChange = {
                            customDnsEnabled = it
                            settingsStore.customDnsEnabled = it
                        },
                    )
                    if (customDnsEnabled) {
                        val presetLabel = SettingsStore.CUSTOM_DNS_PRESETS
                            .firstOrNull { it.first == customDnsServer }
                            ?.second
                        SettingRow(
                            c = c,
                            icon = Icons.Filled.Tune,
                            title = "DNS server",
                            subtitle = presetLabel ?: customDnsServer,
                            onClick = { showCustomDnsDialog = true },
                        )
                    }
                }

                // ───── Local proxy ─────
                SettingsSection(c, "Local proxy") {
                    SettingSwitchRow(
                        c = c,
                        icon = Icons.Filled.Router,
                        title = "Proxy-only mode",
                        subtitle = "Run local proxy servers (SOCKS5 & HTTP), without a VPN interface",
                        checked = proxyOnlyMode,
                        divider = false,
                        onCheckedChange = {
                            proxyOnlyMode = it
                            settingsStore.proxyOnlyMode = it
                        },
                    )
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Public,
                        title = "Local address",
                        subtitle = socksBindAddress,
                        onClick = { showSocksBindDialog = true },
                    )
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Hub,
                        title = "SOCKS5 port",
                        subtitle = socksPort.toString(),
                        onClick = { showSocksPortDialog = true },
                    )
                    SettingSwitchRow(
                        c = c,
                        icon = Icons.Filled.Language,
                        title = "Enable HTTP proxy",
                        subtitle = "Run local HTTP proxy alongside SOCKS5",
                        checked = httpProxyEnabled,
                        onCheckedChange = {
                            httpProxyEnabled = it
                            settingsStore.httpProxyEnabled = it
                        },
                    )
                    if (httpProxyEnabled) {
                        SettingRow(
                            c = c,
                            icon = Icons.Filled.Tune,
                            title = "HTTP port",
                            subtitle = httpProxyPort.toString(),
                            onClick = { showHttpPortDialog = true },
                        )
                    }
                }

                // ───── Split tunneling ─────
                SettingsSection(c, "Split tunneling") {
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Apps,
                        title = "Excluded apps",
                        subtitle = if (excludedApps.isEmpty()) {
                            "None"
                        } else {
                            "${excludedApps.size} app(s) bypass the VPN"
                        },
                        divider = false,
                        onClick = { showSplitTunnelDialog = true },
                    )
                }

                // ───── Advanced ─────
                SettingsSection(c, "Advanced") {
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Link,
                        title = "Custom edge address",
                        subtitle = customEdgeAddress.ifBlank { "Not set" },
                        divider = false,
                        onClick = { showEdgeAddressDialog = true },
                    )
                    SettingSwitchRow(
                        c = c,
                        icon = Icons.Filled.AltRoute,
                        title = "Chain through upstream proxy",
                        subtitle = "Connect to the VPN server through another proxy first",
                        checked = upstreamProxyEnabled,
                        onCheckedChange = {
                            upstreamProxyEnabled = it
                            settingsStore.upstreamProxyEnabled = it
                        },
                    )
                    if (upstreamProxyEnabled) {
                        SettingRow(
                            c = c,
                            icon = Icons.Filled.Hub,
                            title = "Proxy type",
                            subtitle = upstreamProxyType.name,
                            onClick = { showUpstreamProxyTypeDialog = true },
                        )
                        SettingRow(
                            c = c,
                            icon = Icons.Filled.Router,
                            title = "Proxy address",
                            subtitle = if (upstreamProxyHost.isBlank()) {
                                "Not set"
                            } else {
                                "$upstreamProxyHost:$upstreamProxyPort"
                            },
                            onClick = { showUpstreamProxyAddressDialog = true },
                        )
                        SettingRow(
                            c = c,
                            icon = Icons.Filled.Person,
                            title = "Proxy credentials",
                            subtitle = if (upstreamProxyUsername.isBlank()) "None" else upstreamProxyUsername,
                            onClick = { showUpstreamProxyCredentialsDialog = true },
                        )
                    }
                }

                // ───── About ─────
                SettingsSection(c, "About") {
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Description,
                        title = "View logs",
                        subtitle = "Recent connection activity",
                        divider = false,
                        onClick = onOpenLogs,
                    )
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.Info,
                        title = "Version",
                        subtitle = BuildConfig.VERSION_NAME,
                    )
                    TelegramCard(c = c, onClick = { openTelegram(context) })
                }

                // ───── Account ─────
                SettingsSection(c, "Account") {
                    SettingRow(
                        c = c,
                        icon = Icons.Filled.AccountCircle,
                        title = "Manage account",
                        subtitle = "Subscription status and data usage",
                        divider = false,
                        onClick = onOpenAccount,
                    )
                }

                val signOutShape = RoundedCornerShape(20.dp)
                Button(
                    onClick = onSignOut,
                    shape = signOutShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SxDanger.copy(alpha = 0.12f),
                        contentColor = SxDanger,
                    ),
                    elevation = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(1.dp, SxDanger.copy(alpha = 0.35f), signOutShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ───────────────────────── کامپوننت‌های ظاهر ─────────────────────────

@Composable
private fun SettingsSection(
    c: FoxExtras,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = c.accentText,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .settingsCard(c, 24.dp),
        content = content,
    )
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SettingsIconBubble(icon: ImageVector, c: FoxExtras) {
    Box(
        modifier = Modifier
            .size(42.dp)
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

@Composable
private fun SettingRow(
    c: FoxExtras,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column {
        if (divider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp)
                    .height(1.dp)
                    .background(c.cardBorder)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBubble(icon, c)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            } else if (onClick != null) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    c: FoxExtras,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    divider: Boolean = true,
) {
    SettingRow(
        c = c,
        icon = icon,
        title = title,
        subtitle = subtitle,
        divider = divider,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = c.accent,
                    checkedBorderColor = c.accent,
                    uncheckedThumbColor = c.textSecondary,
                    uncheckedTrackColor = c.innerCard,
                    uncheckedBorderColor = c.cardBorder,
                ),
            )
        },
    )
}

/** کارت تلگرام: جایگزین ردیف Source code. */
@Composable
private fun TelegramCard(c: FoxExtras, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.innerCard.copy(alpha = 0.7f))
                .border(1.dp, c.accent.copy(alpha = 0.45f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, ambientColor = c.accent, spotColor = c.accent)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Telegram",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                )
                Text(
                    text = TELEGRAM_HANDLE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.accentText,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ───────────────────────── دیالوگ‌ها ─────────────────────────

@Composable
private fun foxTextButtonColors(c: FoxExtras) = ButtonDefaults.textButtonColors(
    contentColor = c.accentText,
    disabledContentColor = c.textSecondary.copy(alpha = 0.4f),
)

@Composable
private fun foxRadioColors(c: FoxExtras) = RadioButtonDefaults.colors(
    selectedColor = c.accent,
    unselectedColor = c.textSecondary,
)

@Composable
private fun foxFieldColors(c: FoxExtras) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = c.accent,
    unfocusedBorderColor = c.textSecondary.copy(alpha = 0.4f),
    focusedLabelColor = c.accentText,
    unfocusedLabelColor = c.textSecondary,
    cursorColor = c.accent,
    focusedTextColor = c.textPrimary,
    unfocusedTextColor = c.textPrimary,
)

/** قالب مشترک همه‌ی دیالوگ‌ها با استایل Fox. */
@Composable
private fun FoxDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalFoxExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.innerCard,
        shape = RoundedCornerShape(28.dp),
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Column(content = content) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = foxTextButtonColors(c),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = foxTextButtonColors(c)) { Text("Cancel") }
        },
    )
}

@Composable
private fun DohProviderPickerDialog(
    current: SettingsStore.DohProvider,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.DohProvider) -> Unit,
) {
    val c = LocalFoxExtras.current
    var selected by remember { mutableStateOf(current) }

    FoxDialog(
        title = "Encrypted DNS",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selected) },
    ) {
        SettingsStore.DohProvider.entries.forEach { provider ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == provider, onClick = { selected = provider })
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = selected == provider,
                    onClick = { selected = provider },
                    colors = foxRadioColors(c),
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(provider.label)
                    val detail = when (provider) {
                        SettingsStore.DohProvider.AUTOMATIC -> "Use whichever answers first"
                        SettingsStore.DohProvider.OFF -> "Use the network's own DNS"
                        else -> provider.addresses.joinToString(", ")
                    }
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDnsPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = LocalFoxExtras.current
    var value by remember { mutableStateOf(current) }
    val isValid = SettingsStore.isValidDnsServer(value)

    FoxDialog(
        title = "DNS server",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(value) },
        confirmEnabled = isValid,
    ) {
        SettingsStore.CUSTOM_DNS_PRESETS.forEach { (address, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = value == address, onClick = { value = address })
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = value == address,
                    onClick = { value = address },
                    colors = foxRadioColors(c),
                )
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it.trim() },
            label = { Text("IPv4 address") },
            singleLine = true,
            isError = value.isNotBlank() && !isValid,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CustomEdgeAddressDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = LocalFoxExtras.current
    var value by remember { mutableStateOf(current) }
    val trimmed = value.trim()
    val isValid = trimmed.isEmpty() || SettingsStore.isValidEdgeHost(trimmed)

    FoxDialog(
        title = "Custom edge address",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(trimmed) },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Host or IP") },
            singleLine = true,
            isError = !isValid,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        Text(
            "Leave empty to resolve the server hostname normally.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SocksBindAddressPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = LocalFoxExtras.current
    var selected by remember { mutableStateOf(current) }

    FoxDialog(
        title = "Local address",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selected) },
    ) {
        SettingsStore.SOCKS_BIND_ADDRESS_PRESETS.forEach { (address, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == address, onClick = { selected = address })
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = selected == address,
                    onClick = { selected = address },
                    colors = foxRadioColors(c),
                )
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SocksPortPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val c = LocalFoxExtras.current
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    FoxDialog(
        title = "Local port",
        onDismiss = onDismiss,
        onConfirm = { parsedPort?.let { onConfirm(it) } },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter(Char::isDigit).take(5) },
            label = { Text("Port (1\u201365535)") },
            singleLine = true,
            isError = portText.isNotBlank() && !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
    }
}

@Composable
private fun HttpPortPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val c = LocalFoxExtras.current
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    FoxDialog(
        title = "HTTP port",
        onDismiss = onDismiss,
        onConfirm = { parsedPort?.let { onConfirm(it) } },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter(Char::isDigit).take(5) },
            label = { Text("Port (1\u201365535)") },
            singleLine = true,
            isError = portText.isNotBlank() && !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
    }
}

@Composable
private fun UpstreamProxyTypePickerDialog(
    current: SettingsStore.UpstreamProxyType,
    onDismiss: () -> Unit,
    onConfirm: (SettingsStore.UpstreamProxyType) -> Unit,
) {
    val c = LocalFoxExtras.current
    var selected by remember { mutableStateOf(current) }

    FoxDialog(
        title = "Proxy type",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selected) },
    ) {
        SettingsStore.UpstreamProxyType.entries.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == type, onClick = { selected = type })
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = selected == type,
                    onClick = { selected = type },
                    colors = foxRadioColors(c),
                )
                Text(type.name, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun UpstreamProxyAddressDialog(
    currentHost: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
) {
    val c = LocalFoxExtras.current
    var host by remember { mutableStateOf(currentHost) }
    var portText by remember { mutableStateOf(currentPort.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = host.isNotBlank() && parsedPort != null && parsedPort in 1..65_535

    FoxDialog(
        title = "Proxy address",
        onDismiss = onDismiss,
        onConfirm = { parsedPort?.let { onConfirm(host, it) } },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it.trim() },
            label = { Text("Host or IP") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter(Char::isDigit).take(5) },
            label = { Text("Port (1\u201365535)") },
            singleLine = true,
            isError = portText.isNotBlank() && (parsedPort == null || parsedPort !in 1..65_535),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun UpstreamProxyCredentialsDialog(
    currentUsername: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val c = LocalFoxExtras.current
    var username by remember { mutableStateOf(currentUsername) }
    var password by remember { mutableStateOf(currentPassword) }

    FoxDialog(
        title = "Proxy credentials",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(username, password) },
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (optional)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private data class InstalledAppEntry(val packageName: String, val label: String)

@Composable
private fun SplitTunnelDialog(
    current: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val c = LocalFoxExtras.current
    val context = LocalContext.current
    var selected by remember { mutableStateOf(current) }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps by produceState<List<InstalledAppEntry>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .map { info: android.content.pm.ApplicationInfo ->
                    InstalledAppEntry(info.packageName, info.loadLabel(packageManager).toString())
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    FoxDialog(
        title = "Split tunneling",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selected) },
    ) {
        Text(
            "Selected apps bypass the VPN and use your normal connection.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.accent) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        if (installedApps.isEmpty()) {
            CircularProgressIndicator(
                color = c.accent,
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally),
            )
        } else if (filteredApps.isEmpty()) {
            Text(
                "No apps match \"$searchQuery\".",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selected.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selected = if (isChecked) {
                                    selected - app.packageName
                                } else {
                                    selected + app.packageName
                                }
                            }
                            .padding(vertical = 2.dp),
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                selected = if (it) selected + app.packageName else selected - app.packageName
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = c.accent,
                                uncheckedColor = c.textSecondary,
                                checkmarkColor = Color.White,
                            ),
                        )
                        Text(app.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
