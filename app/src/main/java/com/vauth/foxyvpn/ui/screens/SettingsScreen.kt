package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.i18n.Lang
import android.content.Context
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.luminance
import com.vauth.foxyvpn.notice.NoticeClient
import com.vauth.foxyvpn.notice.UpdateInfo
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.platform.LocalUriHandler
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.data.RedeemClient
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.vauth.foxyvpn.data.AppliedConfig
import com.vauth.foxyvpn.data.SystemVpnState
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.BuildConfig
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.ui.theme.AppIconPickerSection
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemeModeSelector
import com.vauth.foxyvpn.ui.theme.ThemePickerSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TELEGRAM_URL = "https://t.me/Ali_jahangirr"

/** صفحه‌های داخلی تنظیمات؛ بدون نیاز به route جدید تو NavGraph. */
private enum class SettingsPage(val title: String, val subtitle: String) {
    MAIN("Settings", "Tune your connection"),
    APPEARANCE("Appearance", "Theme and colors"),
    CONNECTION("Connection", "Exit location check"),
    DNS("DNS", "Encrypted and custom DNS"),
    LOCAL_PROXY("Local proxy", "SOCKS5 and HTTP servers"),
    ADVANCED("Advanced", "Edge address and upstream proxy"),
}

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
    // پشتیبانی: اگه NavGraph چیزی نداد، تلگرام باز می‌شه (قبلاً ردیف Support بی‌عمل بود)
    onOpenSupport: (() -> Unit)? = null,
    // صفحه‌ی «کد و ارتقا»: اگه NavGraph چیزی نداد، مثل قبل صفحه‌ی حساب باز می‌شه
    onOpenPlans: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current

    // آپدیت جدید؟ (از ربات یا گیت‌هاب)
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checking by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    val updateScope = rememberCoroutineScope()
    // force = true وقتی کاربر خودش روی «Version» می‌زنه؛ وگرنه تا ۶۰ دقیقه از نتیجه‌ی قبلی استفاده می‌شه
    fun checkUpdate(force: Boolean = false) {
        if (checking) return
        if (!force && UpdateCheckCache.isFresh()) {
            updateInfo = UpdateCheckCache.info
            checked = true
            return
        }
        checking = true
        updateScope.launch {
            val result = runCatching { NoticeClient.fetchStatus(context).update }
            if (result.isSuccess) UpdateCheckCache.put(result.getOrNull())
            updateInfo = result.getOrNull() ?: UpdateCheckCache.info
            checking = false; checked = true
        }
    }
    LaunchedEffect(Unit) { checkUpdate() }

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
    var autoConnect by remember { mutableStateOf(settingsStore.autoConnect) }
    var showKillSwitchDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    // زبان برنامه: کشوی انتخاب (سیستم / فارسی / English)
    var languageChoice by remember { mutableStateOf(Lang.choice(context)) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val redeemClient = remember { RedeemClient(context.applicationContext) }
    val isAdmin = remember { redeemClient.adminMode }
    val uriHandler = LocalUriHandler.current
    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    val adminScope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    val scrollState = rememberScrollState()
    LaunchedEffect(page) { if (scrollState.value != 0) scrollState.scrollTo(0) }
    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }
    var showDohProviderDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomDnsDialog by rememberSaveable { mutableStateOf(false) }
    var showSocksBindDialog by rememberSaveable { mutableStateOf(false) }
    var showSocksPortDialog by rememberSaveable { mutableStateOf(false) }
    var showHttpPortDialog by rememberSaveable { mutableStateOf(false) }
    var showEdgeAddressDialog by rememberSaveable { mutableStateOf(false) }
    var showUpstreamProxyTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showUpstreamProxyAddressDialog by rememberSaveable { mutableStateOf(false) }
    var showUpstreamProxyCredentialsDialog by rememberSaveable { mutableStateOf(false) }
    var showSplitTunnelDialog by rememberSaveable { mutableStateOf(false) }

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
        PortPickerDialog(
            title = tr("Local port"),
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
        PortPickerDialog(
            title = tr("HTTP port"),
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

    if (showLanguageSheet) {
        LanguagePickerSheet(
            current = languageChoice,
            onSelect = { choice ->
                showLanguageSheet = false
                if (choice != languageChoice) {
                    languageChoice = choice
                    Lang.setChoice(context, choice)
                    Lang.markChosen(context)
                    context.findActivity()?.recreate() // زبان و جهت صفحه عوض می‌شه
                }
            },
            onDismiss = { showLanguageSheet = false },
        )
    }

    if (showResetDialog) {
        FoxDialog(
            title = tr("Reset connection settings?"),
            onDismiss = { showResetDialog = false },
            onConfirm = {
                showResetDialog = false
                settingsStore.resetConnectionDefaults()
                exitCheckEnabled = settingsStore.exitCheckEnabled
                dohProvider = settingsStore.dohProvider
                customDnsEnabled = settingsStore.customDnsEnabled
                customDnsServer = settingsStore.customDnsServer
                socksBindAddress = settingsStore.socksBindAddress
                socksPort = settingsStore.socksPort
                httpProxyEnabled = settingsStore.httpProxyEnabled
                httpProxyPort = settingsStore.httpProxyPort
                proxyOnlyMode = settingsStore.proxyOnlyMode
                customEdgeAddress = settingsStore.customEdgeAddress
                upstreamProxyEnabled = settingsStore.upstreamProxyEnabled
                upstreamProxyType = settingsStore.upstreamProxyType
                upstreamProxyHost = settingsStore.upstreamProxyHost
                upstreamProxyPort = settingsStore.upstreamProxyPort
                upstreamProxyUsername = settingsStore.upstreamProxyUsername
                upstreamProxyPassword = settingsStore.upstreamProxyPassword
                excludedApps = settingsStore.excludedApps
            },
            confirmLabel = tr("Reset"),
        ) {
            Text(
                text = tr("DNS, local proxy, advanced and split tunnel settings go back to their defaults. Theme, language and your account stay as they are."),
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showAddAccountDialog) {
        AddAccountToServerDialog(
            onDismiss = { showAddAccountDialog = false },
            onSubmit = { label, adminKey, server ->
                val key = (context.applicationContext as? FoxyVpnApp)?.authRepository?.currentRefreshToken()
                showAddAccountDialog = false
                if (key.isNullOrBlank()) {
                    Toast.makeText(context, tr("No access key found. Sign in again."), Toast.LENGTH_LONG).show()
                } else {
                    adminScope.launch {
                        RedeemClient(context.applicationContext).addAccountToServer(adminKey, label, key, server)
                            .onSuccess { Toast.makeText(context, trf("Added to server: %s", it), Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(context, it.message ?: tr("Failed"), Toast.LENGTH_LONG).show() }
                    }
                }
            },
        )
    }
    if (showSignOutDialog) {
        SignOutConfirmDialog(
            onDismiss = { showSignOutDialog = false },
            onConfirm = {
                showSignOutDialog = false
                onSignOut()
            },
        )
    }

    if (showKillSwitchDialog) {
        FoxDialog(
            title = tr("Kill switch"),
            onDismiss = { showKillSwitchDialog = false },
            onConfirm = {
                showKillSwitchDialog = false
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure {
                    // بعضی گوشی‌ها صفحه VPN را ندارند؛ حداقل تنظیمات شبکه را باز کن
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            },
            confirmLabel = tr("Open settings"),
        ) {
            Text(
                text = tr("Android's own kill switch blocks all traffic if the VPN drops, so nothing leaks. In the VPN settings, tap the gear next to Fox-VPN and turn on \"Always-on VPN\" and \"Block connections without VPN\"."),
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    // ── وضعیت واقعی کیل‌سوییچ اندروید (با برگشتن از تنظیمات سیستم تازه می‌شه) ──
    var sysVpn by remember { mutableStateOf(SystemVpnState.read(context)) }
    val lifecycleOwner = context.findActivity() as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) sysVpn = SystemVpnState.read(context)
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    // ── تغییرِ تنظیماتِ مؤثر بر اتصال، وقتی VPN وصله ──
    val vpnState by FoxyVpnService.state.collectAsState()
    val settingsFingerprint = remember(
        dohProvider, customDnsEnabled, customDnsServer, socksBindAddress, socksPort,
        httpProxyEnabled, httpProxyPort, proxyOnlyMode, customEdgeAddress, upstreamProxyEnabled,
        upstreamProxyType, upstreamProxyHost, upstreamProxyPort, upstreamProxyUsername,
        upstreamProxyPassword, excludedApps,
    ) { settingsStore.connectionFingerprint() }
    val connectionChanged = remember(settingsFingerprint, exitCheckEnabled) { !settingsStore.isConnectionDefault() }
    var dismissedFingerprint by rememberSaveable { mutableStateOf<Int?>(null) }
    val needsReconnect = vpnState == ConnectionState.CONNECTED &&
        AppliedConfig.fingerprint != null &&
        settingsFingerprint != AppliedConfig.fingerprint &&
        settingsFingerprint != dismissedFingerprint
    val reconnectScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = needsReconnect,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ApplyChangesBar(
                    c = c,
                    onLater = { dismissedFingerprint = settingsFingerprint },
                    onReconnect = {
                        dismissedFingerprint = settingsFingerprint
                        reconnectScope.launch {
                            FoxyVpnService.stop(context)
                            withTimeoutOrNull(10_000) {
                                FoxyVpnService.state.first { it == ConnectionState.DISCONNECTED }
                            }
                            delay(400)
                            FoxyVpnService.start(context)
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // ───── هدر: برگشت + عنوان + لوگو ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { if (page != SettingsPage.MAIN) page = SettingsPage.MAIN else onBack() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .border(1.dp, c.cardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Back"),
                            tint = c.textPrimary.copy(alpha = 0.75f)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(page.title),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                        Text(
                            text = tr(page.subtitle),
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

                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState != SettingsPage.MAIN
                        (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { if (forward) it / 8 else -it / 8 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { if (forward) -it / 8 else it / 8 })
                    },
                    label = "settings-page",
                ) { target ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (target) {
                            SettingsPage.MAIN -> {
                                // ───── اکانت (اول صفحه)؛ زیرش: وارد کردن کد، پشتیبانی ─────
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .settingsCard(c, 24.dp)
                                ) {
                                    AccountHeaderRow(c = c, onClick = onOpenAccount)
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.CardGiftcard,
                                        title = tr("Redeem a code"),
                                        subtitle = tr("Upgrade your plan with a code"),
                                        // صفحه‌ی «کد و ارتقا»، نه صفحه‌ی اشتراک حساب
                                        onClick = { (onOpenPlans ?: onOpenAccount).invoke() },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.SupportAgent,
                                        title = tr("Support"),
                                        subtitle = tr("Bot, Telegram and help"),
                                        onClick = { onOpenSupport?.invoke() ?: openTelegram(context) },
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                SettingsSection(c, tr("Theme & language")) {
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Palette,
                                        title = tr("Appearance"),
                                        subtitle = tr("Theme and colors"),
                                        divider = false,
                                        onClick = { page = SettingsPage.APPEARANCE },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Language,
                                        title = tr("Language"),
                                        subtitle = languageLabel(languageChoice),
                                        onClick = { showLanguageSheet = true },
                                    )
                                }

                                // ───── سه ابزار سریع کنار هم ─────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    QuickTile(
                                        modifier = Modifier.weight(1f),
                                        c = c,
                                        icon = Icons.Filled.Bolt,
                                        title = tr("Auto-connect"),
                                        status = if (autoConnect) tr("On") else tr("Off"),
                                        active = autoConnect,
                                        onClick = {
                                            autoConnect = !autoConnect
                                            settingsStore.autoConnect = autoConnect
                                        },
                                    )
                                    QuickTile(
                                        modifier = Modifier.weight(1f),
                                        c = c,
                                        icon = Icons.Filled.Shield,
                                        title = tr("Kill switch"),
                                        status = when {
                                            sysVpn.lockdown -> tr("On")
                                            sysVpn.alwaysOn -> tr("Always-on")
                                            else -> tr("Off")
                                        },
                                        active = sysVpn.alwaysOn || sysVpn.lockdown,
                                        onClick = { showKillSwitchDialog = true },
                                    )
                                    QuickTile(
                                        modifier = Modifier.weight(1f),
                                        c = c,
                                        icon = Icons.Filled.Apps,
                                        title = tr("Split tunnel"),
                                        status = when (excludedApps.size) {
                                            0 -> tr("None")
                                            1 -> tr("1 app")
                                            else -> trf("%d apps", excludedApps.size)
                                        },
                                        active = excludedApps.isNotEmpty(),
                                        onClick = { showSplitTunnelDialog = true },
                                    )
                                }
                                Spacer(Modifier.height(20.dp))

                                // ───── گروه‌ها؛ هرکدام وارد صفحه‌ی خودش می‌شه ─────
                                SettingsSection(c, tr("Connection")) {
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.VerifiedUser,
                                        title = tr("Connection"),
                                        subtitle = if (exitCheckEnabled) tr("Exit location check on") else tr("Exit location check off"),
                                        divider = false,
                                        onClick = { page = SettingsPage.CONNECTION },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Dns,
                                        title = tr("DNS"),
                                        subtitle = if (customDnsEnabled) trf("Custom • %s", customDnsServer) else tr(dohProvider.label),
                                        onClick = { page = SettingsPage.DNS },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Router,
                                        title = tr("Local proxy"),
                                        subtitle = (if (proxyOnlyMode) tr("Proxy-only • ") else "") + "SOCKS5 $socksPort",
                                        onClick = { page = SettingsPage.LOCAL_PROXY },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Tune,
                                        title = tr("Advanced"),
                                        subtitle = if (upstreamProxyEnabled) trf("Chained via %s", upstreamProxyType.name) else tr("Edge address and upstream proxy"),
                                        onClick = { page = SettingsPage.ADVANCED },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Description,
                                        title = tr("View logs"),
                                        subtitle = tr("Recent connection activity"),
                                        onClick = onOpenLogs,
                                    )
                                }

                                SettingsSection(c, tr("About")) {
                                    val upd = updateInfo
                                    SettingRow(
                                        c = c,
                                        icon = if (upd != null) Icons.Filled.SystemUpdate else Icons.Filled.Info,
                                        divider = false,
                                        title = if (upd != null) tr("Update available") else tr("Version"),
                                        subtitle = when {
                                            upd != null -> trf("%s → %s · tap to update", BuildConfig.VERSION_NAME, upd.versionName.ifBlank { upd.versionCode.toString() })
                                            checking -> BuildConfig.VERSION_NAME + " · " + tr("Checking…")
                                            checked -> BuildConfig.VERSION_NAME + " · " + tr("You're up to date")
                                            else -> BuildConfig.VERSION_NAME
                                        },
                                        onClick = { if (upd != null && upd.url.isNotBlank()) runCatching { uriHandler.openUri(upd.url) } else checkUpdate(force = true) },
                                        trailing = {
                                            if (upd != null) {
                                                Box(
                                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.accent).padding(horizontal = 12.dp, vertical = 6.dp),
                                                ) { Text(tr("Update"), fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White) }
                                            }
                                        },
                                    )
                                    if (isAdmin) {
                                        SettingRow(
                                            c = c,
                                            icon = Icons.Filled.VpnKey,
                                            title = tr("Copy access key (admin)"),
                                            subtitle = tr("For creating redeem codes. Keep it private."),
                                            onClick = {
                                                val key = (context.applicationContext as? FoxyVpnApp)
                                                    ?.authRepository?.currentRefreshToken()
                                                if (key.isNullOrBlank()) {
                                                    Toast.makeText(context, tr("No access key found. Sign in again."), Toast.LENGTH_LONG).show()
                                                } else {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("fox-access-key", key))
                                                    Toast.makeText(context, tr("Access key copied"), Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        )
                                        SettingRow(
                                            c = c,
                                            icon = Icons.Filled.VpnKey,
                                            title = tr("Add this account to server (admin)"),
                                            subtitle = tr("Sends the access key straight to the bot's pool"),
                                            onClick = { showAddAccountDialog = true },
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = connectionChanged) {
                                    Column {
                                        TextButton(
                                            onClick = { showResetDialog = true },
                                            colors = foxTextButtonColors(c),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(tr("Reset connection settings"), fontWeight = FontWeight.SemiBold)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                if (isAdmin) {
                                val signOutShape = RoundedCornerShape(20.dp)
                                Button(
                                    onClick = { showSignOutDialog = true },
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
                                    Text(tr("Sign out"), fontWeight = FontWeight.SemiBold)
                                }
                                }
                            }

                            SettingsPage.APPEARANCE -> {
                                SettingsSection(c, tr("Mode")) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        ThemeModeSelector(themeController = themeController)
                                    }
                                    if (themeController.palette.supportsLightDark) {
                                        SettingSwitchRow(
                                            c = c,
                                            icon = Icons.Filled.Palette,
                                            title = tr("Show light/dark button on home"),
                                            subtitle = tr("A quick sun/moon button next to Settings"),
                                            checked = themeController.showHomeToggle,
                                            onCheckedChange = { themeController.setShowHomeToggle(it) },
                                        )
                                    }
                                }
                                SettingsSection(c, tr("Themes")) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        ThemePickerSection(
                                            themeController = themeController,
                                            showTitle = false,
                                        )
                                    }
                                }
                                SettingsSection(c, tr("App icon")) {
                                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                        AppIconPickerSection()
                                    }
                                }
                            }

                            SettingsPage.CONNECTION -> {
                                SettingsSection(c, tr("Connection")) {
                                    SettingSwitchRow(
                                        c = c,
                                        icon = Icons.Filled.VerifiedUser,
                                        title = tr("Verify exit location"),
                                        subtitle = tr("Check that the tunnel exits in the selected country"),
                                        checked = exitCheckEnabled,
                                        divider = false,
                                        onCheckedChange = {
                                            exitCheckEnabled = it
                                            settingsStore.exitCheckEnabled = it
                                        },
                                    )
                                }
                            }

                            SettingsPage.DNS -> {
                                SettingsSection(c, tr("DNS")) {
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Lock,
                                        title = tr("Encrypted DNS"),
                                        subtitle = tr(dohProvider.label),
                                        divider = false,
                                        onClick = { showDohProviderDialog = true },
                                    )
                                    SettingSwitchRow(
                                        c = c,
                                        icon = Icons.Filled.Dns,
                                        title = tr("Use a custom DNS server"),
                                        subtitle = tr("Send your apps' DNS queries to a server you choose"),
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
                                            title = tr("DNS server"),
                                            subtitle = presetLabel ?: customDnsServer,
                                            onClick = { showCustomDnsDialog = true },
                                        )
                                    }
                                }
                            }

                            SettingsPage.LOCAL_PROXY -> {
                                SettingsSection(c, tr("Local proxy")) {
                                    SettingSwitchRow(
                                        c = c,
                                        icon = Icons.Filled.Router,
                                        title = tr("Proxy-only mode"),
                                        subtitle = tr("Run local proxy servers (SOCKS5 & HTTP), without a VPN interface"),
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
                                        title = tr("Local address"),
                                        subtitle = socksBindAddress,
                                        onClick = { showSocksBindDialog = true },
                                    )
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Hub,
                                        title = tr("SOCKS5 port"),
                                        subtitle = socksPort.toString(),
                                        onClick = { showSocksPortDialog = true },
                                    )
                                    SettingSwitchRow(
                                        c = c,
                                        icon = Icons.Filled.Language,
                                        title = tr("Enable HTTP proxy"),
                                        subtitle = tr("Run local HTTP proxy alongside SOCKS5"),
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
                                            title = tr("HTTP port"),
                                            subtitle = httpProxyPort.toString(),
                                            onClick = { showHttpPortDialog = true },
                                        )
                                    }
                                }
                            }

                            SettingsPage.ADVANCED -> {
                                SettingsSection(c, tr("Advanced")) {
                                    SettingRow(
                                        c = c,
                                        icon = Icons.Filled.Link,
                                        title = tr("Custom edge address"),
                                        subtitle = customEdgeAddress.ifBlank { tr("Not set") },
                                        divider = false,
                                        onClick = { showEdgeAddressDialog = true },
                                    )
                                    SettingSwitchRow(
                                        c = c,
                                        icon = Icons.Filled.AltRoute,
                                        title = tr("Chain through upstream proxy"),
                                        subtitle = tr("Connect to the VPN server through another proxy first"),
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
                                            title = tr("Proxy type"),
                                            subtitle = upstreamProxyType.name,
                                            onClick = { showUpstreamProxyTypeDialog = true },
                                        )
                                        SettingRow(
                                            c = c,
                                            icon = Icons.Filled.Router,
                                            title = tr("Proxy address"),
                                            subtitle = if (upstreamProxyHost.isBlank()) {
                                                tr("Not set")
                                            } else {
                                                "$upstreamProxyHost:$upstreamProxyPort"
                                            },
                                            onClick = { showUpstreamProxyAddressDialog = true },
                                        )
                                        SettingRow(
                                            c = c,
                                            icon = Icons.Filled.Person,
                                            title = tr("Proxy credentials"),
                                            subtitle = if (upstreamProxyUsername.isBlank()) tr("None") else upstreamProxyUsername,
                                            onClick = { showUpstreamProxyCredentialsDialog = true },
                                        )
                                    }
                                }
                            }
                        }
                    }
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

/** ردیف اکانت بالای صفحه. */
@Composable
private fun AccountHeaderRow(c: FoxExtras, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, CircleShape, ambientColor = c.accent, spotColor = c.accent)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr("Account"),
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
            )
            Text(
                text = tr("Manage account"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = c.textPrimary,
            )
            Text(
                text = tr("Subscription status and data usage"),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
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

/** کاشی سریع: آیکون، عنوان و وضعیت (به‌شکل قرص)؛ وقتی فعاله با رنگ accent پررنگ می‌شه. */
// ───────────────────────── نوار «اعمال تغییرات» ─────────────────────────

@Composable
private fun ApplyChangesBar(
    c: FoxExtras,
    onLater: () -> Unit,
    onReconnect: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = tr("Settings changed"),
            color = c.textPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tr("Reconnect to apply your changes to the current connection."),
            color = c.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onLater) { Text(tr("Later"), color = c.textSecondary) }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onReconnect, shape = RoundedCornerShape(14.dp)) { Text(tr("Reconnect")) }
        }
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier,
    c: FoxExtras,
    icon: ImageVector,
    title: String,
    status: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val activeBrush = Brush.verticalGradient(
        listOf(c.accent.copy(alpha = 0.22f), c.accent.copy(alpha = 0.08f))
    )
    Column(
        modifier = modifier
            .heightIn(min = 140.dp)
            .shadow(
                if (active) 8.dp else 4.dp, shape,
                ambientColor = c.accent.copy(alpha = 0.18f),
                spotColor = c.accent.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(if (active) activeBrush else SolidColor(c.card))
            .border(1.dp, if (active) c.accent.copy(alpha = 0.65f) else c.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .then(
                    if (active) Modifier.shadow(10.dp, CircleShape, ambientColor = c.accent, spotColor = c.accent)
                    else Modifier
                )
                .clip(CircleShape)
                .background(
                    if (active) Brush.linearGradient(listOf(c.accentLight, c.accent))
                    else SolidColor(c.accent.copy(alpha = 0.12f))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color.White else c.accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (active) c.accent.copy(alpha = 0.18f) else c.innerCard)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) c.accentText else c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** لوگوی هواپیمای کاغذی تلگرام به‌صورت وکتور (در آیکون‌های Material وجود ندارد). */
private val TelegramPlane: ImageVector by lazy {
    ImageVector.Builder(
        name = "TelegramPlane",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M9.78,18.65L10.06,14.42L17.74,7.5C18.08,7.19 17.67,7.04 17.22,7.31L7.74,13.3L3.64,12" +
                "C2.76,11.75 2.75,11.14 3.84,10.7L19.81,4.54C20.54,4.21 21.24,4.72 20.96,5.84L18.24,18.65" +
                "C18.05,19.56 17.5,19.78 16.74,19.36L12.6,16.3L10.61,18.23C10.39,18.45 10.21,18.63 9.78,18.65Z"
        ).toNodes(),
        fill = SolidColor(Color.White),
    ).build()
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

/** (ادمین) نام حساب + رمز ادمین سرور؛ رمز ذخیره نمی‌شه. */
@Composable
private fun AddAccountToServerDialog(
    onDismiss: () -> Unit,
    onSubmit: (label: String, adminKey: String, server: Int) -> Unit,
) {
    val c = LocalFoxExtras.current
    var label by remember { mutableStateOf("") }
    var adminKey by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(0) }
    FoxDialog(
        title = tr("Add account to server"),
        onDismiss = onDismiss,
        onConfirm = { onSubmit(label.trim(), adminKey.trim(), server) },
        confirmEnabled = label.isNotBlank() && adminKey.isNotBlank(),
        confirmLabel = tr("Add"),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(tr("Account name")) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        OutlinedTextField(
            value = adminKey,
            onValueChange = { adminKey = it },
            label = { Text(tr("Server admin key")) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.padding(top = 8.dp),
        )
        if (RedeemClient.serverCount > 1) {
            // هر Worker استخر حساب خودش رو داره؛ انتخاب کن این حساب به کدوم بره
            Text(
                tr("Server"),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until RedeemClient.serverCount) {
                    RadioButton(
                        selected = server == i,
                        onClick = { server = i },
                        colors = foxRadioColors(c),
                    )
                    Text("${i + 1}", color = c.textPrimary, modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        Text(
            tr("The admin key is not stored on this device."),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** قالب مشترک همه‌ی دیالوگ‌ها با استایل Fox. */
@Composable
private fun FoxDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmLabel: String = tr("Save"),
    wide: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalFoxExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = if (wide) Modifier.fillMaxWidth(0.94f) else Modifier,
        properties = DialogProperties(usePlatformDefaultWidth = !wide),
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
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = foxTextButtonColors(c)) { Text(tr("Cancel")) }
        },
    )
}

@Composable
private fun SignOutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val c = LocalFoxExtras.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.innerCard,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = SxDanger,
            )
        },
        title = {
            Text(tr("Sign out?"), fontWeight = FontWeight.ExtraBold, color = c.textPrimary)
        },
        text = {
            Text(
                tr("You'll need to sign in again with your Firefox account to use FoxyVPN."),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = SxDanger),
            ) { Text(tr("Sign out"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = foxTextButtonColors(c)) { Text(tr("Cancel")) }
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
        title = tr("Encrypted DNS"),
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
                    Text(tr(provider.label))
                    val detail = when (provider) {
                        SettingsStore.DohProvider.AUTOMATIC -> tr("Use whichever answers first")
                        SettingsStore.DohProvider.OFF -> tr("Use the network's own DNS")
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
        title = tr("DNS server"),
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
            label = { Text(tr("IPv4 address")) },
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
        title = tr("Custom edge address"),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(trimmed) },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(tr("Host or IP")) },
            singleLine = true,
            isError = !isValid,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        Text(
            tr("Leave empty to resolve the server hostname normally."),
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
        title = tr("Local address"),
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
private fun PortPickerDialog(
    title: String,
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val c = LocalFoxExtras.current
    var portText by remember { mutableStateOf(current.toString()) }
    val parsedPort = portText.toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65_535

    FoxDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { parsedPort?.let { onConfirm(it) } },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter(Char::isDigit).take(5) },
            label = { Text(tr("Port (1\u201365535)")) },
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
        title = tr("Proxy type"),
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
        title = tr("Proxy address"),
        onDismiss = onDismiss,
        onConfirm = { parsedPort?.let { onConfirm(host, it) } },
        confirmEnabled = isValid,
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it.trim() },
            label = { Text(tr("Host or IP")) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter(Char::isDigit).take(5) },
            label = { Text(tr("Port (1\u201365535)")) },
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
        title = tr("Proxy credentials"),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(username, password) },
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(tr("Username (optional)")) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(tr("Password (optional)")) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private class InstalledAppEntry(val packageName: String, val label: String) {
    val labelLower: String = label.lowercase()
}

/** نتیجه‌ی آخرین چک آپدیت؛ ۶۰ دقیقه معتبره (فقط تو حافظه، با بستن برنامه پاک می‌شه). */
private object UpdateCheckCache {
    @Volatile var info: UpdateInfo? = null
        private set
    @Volatile private var checkedAt = 0L
    fun isFresh(): Boolean = checkedAt != 0L && System.currentTimeMillis() - checkedAt < 60 * 60_000L
    fun put(update: UpdateInfo?) { info = update; checkedAt = System.currentTimeMillis() }
}

/** فهرست برنامه‌های نصب‌شده؛ ۵ دقیقه کش می‌شه تا باز کردن دوباره‌ی Split tunneling فوری باشه. */
private object InstalledAppsCache {
    @Volatile private var apps: List<InstalledAppEntry>? = null
    @Volatile private var loadedAt = 0L
    fun get(): List<InstalledAppEntry>? =
        apps?.takeIf { System.currentTimeMillis() - loadedAt < 5 * 60_000L }
    fun put(list: List<InstalledAppEntry>) { apps = list; loadedAt = System.currentTimeMillis() }
}

/** برنامه‌های انتخاب‌شده‌ی قبلی بالا، بقیه الفبایی. */
private fun sortInstalledApps(apps: List<InstalledAppEntry>, selected: Set<String>): List<InstalledAppEntry> =
    apps.sortedWith(
        compareByDescending<InstalledAppEntry> { it.packageName in selected }.thenBy { it.labelLower }
    )

/** کش کوچک آیکون برنامه‌ها تا اسکرول لیست روان بماند. */
private object AppIconCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(160)
    fun get(pkg: String): ImageBitmap? = cache.get(pkg)
    fun put(pkg: String, bmp: ImageBitmap) { cache.put(pkg, bmp) }
}

@Composable
private fun InstalledAppIcon(packageName: String, c: FoxExtras, size: Dp = 42.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = AppIconCache.get(packageName), packageName) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) {
                AppIconCache.put(packageName, loaded)
                value = loaded
            }
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(c.accent.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        val img = bitmap
        if (img != null) {
            Image(bitmap = img, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.Apps, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
    }
}

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
    val initiallySelected = remember { current }

    val installedApps by produceState<List<InstalledAppEntry>?>(
        initialValue = InstalledAppsCache.get()?.let { sortInstalledApps(it, initiallySelected) },
    ) {
        if (value == null) {
            val base = withContext(Dispatchers.IO) {
                val packageManager = context.packageManager
                packageManager.getInstalledApplications(0)
                    .asSequence()
                    .filter { it.packageName != context.packageName }
                    .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                    .map { info: android.content.pm.ApplicationInfo ->
                        InstalledAppEntry(info.packageName, info.loadLabel(packageManager).toString())
                    }
                    .toList()
            }
            InstalledAppsCache.put(base)
            // برنامه‌هایی که از قبل انتخاب شده بالا می‌آیند، بقیه الفبایی
            value = sortInstalledApps(base, initiallySelected)
        }
    }
    val filteredApps = remember(installedApps, searchQuery) {
        val all = installedApps.orEmpty()
        if (searchQuery.isBlank()) all
        else all.filter {
            val q = searchQuery.trim()
            it.labelLower.contains(q.lowercase()) || it.packageName.contains(q, ignoreCase = true)
        }
    }

    FoxDialog(
        title = tr("Split tunneling"),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(selected) },
        wide = true,
    ) {
        Text(
            tr("Selected apps bypass the VPN and use your normal connection."),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(tr("Search apps")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.accent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = tr("Clear"), tint = c.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = foxFieldColors(c),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected.isEmpty()) c.innerCard else c.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (selected.isEmpty()) tr("No apps selected") else trf("%d selected", selected.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected.isEmpty()) c.textSecondary else c.accentText,
                )
            }
            Spacer(Modifier.weight(1f))
            if (selected.isNotEmpty()) {
                TextButton(onClick = { selected = emptySet() }, colors = foxTextButtonColors(c)) {
                    Text(tr("Clear all"), fontSize = 13.sp)
                }
            }
        }
        val apps = installedApps
        if (apps == null) {
            CircularProgressIndicator(
                color = c.accent,
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally),
            )
        } else if (filteredApps.isEmpty()) {
            Text(
                trf("No apps match \"%s\".", searchQuery),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selected.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isChecked) c.accent.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable {
                                selected = if (isChecked) selected - app.packageName else selected + app.packageName
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        InstalledAppIcon(packageName = app.packageName, c = c)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 11.sp,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
                    }
                }
            }
        }
    }
}
