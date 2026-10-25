package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.GUARDIAN_ENDPOINT_DEFAULT
import com.vauth.foxyvpn.data.GuardianClient
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.LicenseStatus
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.data.formatBytes
import com.vauth.foxyvpn.data.gbLabel
import com.vauth.foxyvpn.data.model.Entitlement
import com.vauth.foxyvpn.data.rememberPlanConfig
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun onAccentColor(c: FoxExtras): Color = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

private fun Modifier.accCard(c: FoxExtras, radius: Int, borderColor: Color = c.cardBorder, width: Int = 1): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .shadow(6.dp, shape, ambientColor = c.accent.copy(alpha = 0.15f), spotColor = c.accent.copy(alpha = 0.15f))
        .clip(shape)
        .background(c.card)
        .border(width.dp, borderColor, shape)
}

private fun planIcon(key: String?): ImageVector = when (key) {
    "free" -> Icons.Filled.CardGiftcard
    "pro" -> Icons.Filled.Star
    "max" -> Icons.Filled.Bolt
    else -> Icons.Filled.WorkspacePremium
}

/** «۳ ساعت و ۱۲ دقیقه» / «۲ روز و ۴ ساعت». */
private fun durationText(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val d = s / 86400; val h = (s % 86400) / 3600; val m = (s % 3600) / 60
    return when {
        d > 0 -> trf("%dd %dh", d, h)
        h > 0 -> trf("%dh %dm", h, m)
        else -> trf("%dm", maxOf(m, 1L))
    }
}

// ───────────────────────── صفحه‌ی حساب ─────────────────────────

@Composable
fun AccountScreen(
    authRepository: FxaAuthRepository,
    onBack: () -> Unit,
    onDisconnect: () -> Unit = {},
    onUpgraded: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = LocalFoxExtras.current
    val statusColors = LocalFoxyStatusColors.current
    val onAccent = onAccentColor(c)
    val redeem = remember { RedeemClient(context) }
    val isAdmin = remember { redeem.adminMode }
    val cfg by rememberPlanConfig()

    LaunchedEffect(Unit) { LicenseState.load(context) }
    val license by LicenseState.status.collectAsState()
    var entitlement by remember { mutableStateOf<Entitlement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        loading = true; error = null
        scope.launch {
            if (!isAdmin && redeem.activeCode != null) runCatching { redeem.reportUsage(0) }
            val token = authRepository.currentAccessToken()
            if (token == null) { error = tr("Not signed in."); loading = false; return@launch }
            runCatching { GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, token) }
                .onSuccess { entitlement = it }
                .onFailure { if (license == null) error = it.message ?: tr("Failed to load account info") }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    // ساعت زنده برای شمارش معکوس
    var nowSec by remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); nowSec = System.currentTimeMillis() / 1000 } }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccRoundButton(c, onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back"), tint = c.textPrimary.copy(alpha = 0.75f)) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(tr("Account"), fontSize = 30.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                            Text(tr("Your plan and usage"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                        }
                    }
                    AccRoundButton(c, { if (!loading) refresh() }) {
                        if (loading) CircularProgressIndicator(color = c.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                        else Icon(Icons.Filled.Refresh, tr("Refresh"), tint = c.textPrimary.copy(alpha = 0.75f))
                    }
                }

                when {
                    isAdmin -> AdminCard(c, onAccent, entitlement, statusColors.connected)
                    license != null -> {
                        val lic = license!!
                        PlanHero(c, onAccent, lic, cfg.plan(lic.plan ?: "").days, nowSec, if (isAdmin) null else onOpenPlans)
                        LowAlert(c, lic, nowSec, onOpenPlans)
                        UsageRing(c, lic)
                        DailyAndTimeCards(c, lic, cfg.plan(lic.plan ?: "").days, nowSec)
                    }
                    loading -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.accent, strokeWidth = 3.dp)
                    }
                    error != null -> Row(Modifier.fillMaxWidth().accCard(c, 24).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // دکمه‌های سریع
                if (!isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAction(c, Icons.Filled.WorkspacePremium, tr("Upgrade"), Modifier.weight(1f), onOpenPlans)
                        QuickAction(c, Icons.Filled.SupportAgent, tr("Support"), Modifier.weight(1f), onOpenSupport)
                    }
                    RedeemCard(authRepository = authRepository, onDisconnect = onDisconnect, onUpgraded = onUpgraded)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ───────────────────────── اجزای صفحه ─────────────────────────

@Composable
private fun PlanHero(c: FoxExtras, onAccent: Color, lic: LicenseStatus, planDays: Int, nowSec: Long, onUpgrade: (() -> Unit)? = null) {
    val block = LicenseState.blockOf(lic, nowSec)
    val err = MaterialTheme.colorScheme.error
    val statusColors = LocalFoxyStatusColors.current
    val frozen = lic.state == "frozen"
    val (statusText, statusColor) = when {
        frozen -> tr("Frozen") to c.accentText
        block != null -> tr("Limited") to err
        else -> tr("Active") to statusColors.connected
    }
    val name = lic.plan?.let { tr(it.replaceFirstChar { ch -> ch.uppercase() }) } ?: tr("Custom plan")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(500), label = "hero")
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier.fillMaxWidth().alpha(a).scale(0.95f + 0.05f * a)
            .shadow(12.dp, shape, ambientColor = c.accent.copy(alpha = 0.3f), spotColor = c.accent.copy(alpha = 0.3f))
            .clip(shape).background(c.card).border(2.dp, Brush.linearGradient(listOf(c.accentLight, c.accent)), shape).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(62.dp).shadow(8.dp, CircleShape, ambientColor = c.accent.copy(alpha = 0.5f), spotColor = c.accent.copy(alpha = 0.5f))
                .clip(CircleShape).background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
            contentAlignment = Alignment.Center,
        ) { Icon(if (frozen) Icons.Filled.AcUnit else planIcon(lic.plan), null, tint = onAccent, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            val exp = lic.expiresAt
            Text(
                when {
                    exp == null -> tr("No expiry")
                    exp <= nowSec -> tr("Expired")
                    else -> com.vauth.foxyvpn.i18n.bidi(
                        trf("%s left", durationText(exp - nowSec)).replace("\u202B", "").replace("\u202C", "") + " · " +
                            java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(java.util.Date(exp * 1000)),
                    )
                },
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onUpgrade != null) {
                val pillShape = RoundedCornerShape(12.dp)
                if (lic.plan == "max") {
                    Box(
                        modifier = Modifier.clip(pillShape).background(c.accent.copy(alpha = 0.16f))
                            .border(1.dp, c.accent.copy(alpha = 0.45f), pillShape).padding(horizontal = 12.dp, vertical = 6.dp),
                    ) { Text(com.vauth.foxyvpn.i18n.bidi(("👑 " + tr("Top plan")).replace("\u202B", "").replace("\u202C", "")), fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentText) }
                } else {
                    Row(
                        modifier = Modifier.clip(pillShape).background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
                            .clickable(onClick = onUpgrade).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = onAccent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Upgrade"), fontSize = 13.sp, fontWeight = FontWeight.Black, color = onAccent)
                    }
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(statusColor.copy(alpha = 0.16f))
                    .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.Black, color = statusColor) }
        }
    }
    if (block != null) {
        Row(Modifier.fillMaxWidth().accCard(c, 20, err.copy(alpha = 0.45f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = err, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(block.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
        }
    }
    @Suppress("UNUSED_VARIABLE") val unused = planDays
}

/** هشدار پیشگیرانه: حجم ≤۱۰٪ یا ≤۳ روز تا پایان پلن، با دکمه‌ی تمدید/ارتقا. */
@Composable
private fun LowAlert(c: FoxExtras, lic: LicenseStatus, nowSec: Long, onOpenPlans: () -> Unit) {
    if (LicenseState.blockOf(lic, nowSec) != null) return // پیام سقف/انقضا همین بالا تو کارت پلن هست
    val total = lic.totalBytes?.takeIf { it > 0 }
    val fracLeft = total?.let { ((it - lic.usedBytes).toFloat() / it.toFloat()).coerceIn(0f, 1f) }
    val exp = lic.expiresAt
    val expSoon = exp != null && exp > nowSec && exp - nowSec <= 3 * 86400L
    val lowData = fracLeft != null && fracLeft <= 0.1f
    if (!expSoon && !lowData) return
    val warn = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(22.dp)
    val text = if (lowData) trf("Only %d%% of your data is left.", ((fracLeft ?: 0f) * 100).toInt())
    else trf("Your plan ends in %s.", durationText((exp ?: nowSec) - nowSec))
    val onAcc = onAccentColor(c)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(warn.copy(alpha = 0.10f)).border(1.dp, warn.copy(alpha = 0.45f), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, null, tint = warn, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
                .clickable(onClick = onOpenPlans).padding(horizontal = 14.dp, vertical = 9.dp),
        ) { Text(if (lic.plan == "free") tr("Upgrade") else tr("Renew"), fontSize = 13.sp, fontWeight = FontWeight.Black, color = onAcc) }
    }
}

/** حلقه‌ی بزرگِ دیتای باقی‌مانده (ماهانه/کل) با انیمیشن. */
@Composable
private fun UsageRing(c: FoxExtras, lic: LicenseStatus) {
    val total = lic.totalBytes
    val unlimited = total == null || total <= 0
    val remaining = if (unlimited) 0L else (total!! - lic.usedBytes).coerceAtLeast(0)
    val fraction = if (unlimited) 1f else (remaining.toFloat() / total!!.toFloat()).coerceIn(0f, 1f)
    // اولین باز شدن صفحه: حلقه و عددها از صفر پر می‌شن (شمارش نرم)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val anim by animateFloatAsState(if (started) fraction else 0f, tween(1300, easing = FastOutSlowInEasing), label = "ring")
    val shownRemaining by animateFloatAsState(if (started) remaining.toFloat() else 0f, tween(1300, easing = FastOutSlowInEasing), label = "ring-num")
    val shownUsed by animateFloatAsState(if (started) lic.usedBytes.toFloat() else 0f, tween(1300, easing = FastOutSlowInEasing), label = "used-num")
    val low = !unlimited && fraction <= 0.1f
    val err = MaterialTheme.colorScheme.error
    val track = c.cardBorder
    Column(
        Modifier.fillMaxWidth().accCard(c, 28).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (anim > 0.001f) {
                    drawArc(
                        brush = if (low) Brush.linearGradient(listOf(err, err)) else Brush.sweepGradient(listOf(c.accent, c.accentLight, c.accent)),
                        startAngle = 135f, sweepAngle = 270f * anim, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.DataUsage, null, tint = if (low) err else c.accent, modifier = Modifier.size(26.dp))
                Text(
                    if (unlimited) "∞" else formatBytes(shownRemaining.toLong()),
                    fontSize = if (unlimited) 44.sp else 30.sp, fontWeight = FontWeight.Black, color = c.textPrimary, textAlign = TextAlign.Center,
                )
                Text(tr("Data remaining"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
        if (!unlimited) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStat(c, tr("Used"), formatBytes(shownUsed.toLong()))
                MiniStat(c, tr("Left"), "${(anim * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun MiniStat(c: FoxExtras, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
    }
}

/** میله‌ی امروز + میله‌ی زمان پلن. */
@Composable
private fun DailyAndTimeCards(c: FoxExtras, lic: LicenseStatus, planDays: Int, nowSec: Long) {
    val daily = lic.dailyBytes
    if (daily != null && daily > 0) {
        val left = (daily - lic.dailyUsed).coerceAtLeast(0)
        BarCard(
            c, Icons.Filled.Today, tr("Today"),
            trf("%s left of %s", formatBytes(left), formatBytes(daily)),
            left.toFloat() / daily.toFloat(),
            if (lic.resetAt > nowSec) trf("Resets in %s", durationText(lic.resetAt - nowSec)) else null,
        )
    }
    val exp = lic.expiresAt
    if (exp != null && planDays > 0) {
        val total = planDays * 86400L
        val remain = (exp - nowSec).coerceAtLeast(0)
        BarCard(
            c, Icons.Filled.Schedule, tr("Plan time"),
            if (remain == 0L) tr("Expired") else trf("%s left", durationText(remain)),
            (remain.toFloat() / total.toFloat()).coerceIn(0f, 1f), null,
        )
    }
}

@Composable
private fun BarCard(c: FoxExtras, icon: ImageVector, title: String, value: String, fraction: Float, hint: String?) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "bar")
    val low = fraction <= 0.1f
    val err = MaterialTheme.colorScheme.error
    Column(Modifier.fillMaxWidth().accCard(c, 24).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.innerCard)) {
            Box(
                Modifier.fillMaxWidth(anim).height(12.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (low) Brush.horizontalGradient(listOf(err, err)) else Brush.horizontalGradient(listOf(c.accent, c.accentLight))),
            )
        }
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
    }
}

@Composable
private fun AdminCard(c: FoxExtras, onAccent: Color, info: Entitlement?, okColor: Color) {
    Row(Modifier.fillMaxWidth().accCard(c, 28, c.accent.copy(alpha = 0.5f), 2).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.accentLight, c.accent))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AccountCircle, null, tint = onAccent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Admin"), fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            Text(tr("Unlimited — no plan limits apply"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
        }
        if (info?.subscribed == true) Text(tr("Active"), fontWeight = FontWeight.Black, color = okColor)
    }
}

@Composable
private fun QuickAction(c: FoxExtras, icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier.clip(shape).background(c.card).border(1.dp, c.accent.copy(alpha = 0.35f), shape).clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(28.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary)
    }
}

@Composable
private fun DetailRow(c: FoxExtras, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
    }
}

@Composable
private fun AccRoundButton(c: FoxExtras, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp).clip(CircleShape).background(c.card).border(1.dp, c.cardBorder, CircleShape),
    ) { content() }
}
