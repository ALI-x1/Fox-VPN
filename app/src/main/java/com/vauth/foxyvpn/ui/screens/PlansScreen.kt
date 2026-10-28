package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf

import android.net.Uri
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import com.vauth.foxyvpn.data.FxaAuthRepository
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.border
import androidx.compose.runtime.State
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.draw.rotate
import com.vauth.foxyvpn.ui.components.softGlow
import com.vauth.foxyvpn.ui.components.softGlowRect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.PlanConfigStore
import com.vauth.foxyvpn.data.PlanInfo
import com.vauth.foxyvpn.data.formatMoney
import com.vauth.foxyvpn.data.gbLabel
import com.vauth.foxyvpn.data.rememberPlanConfig
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Apps
import androidx.compose.ui.text.style.TextDecoration
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras

// ───────────────────────── مشخصات پلن‌ها ─────────────────────────
// این اعداد باید با PLANS تو worker.js یکی باشن.

private data class PlanSpec(
    val key: String,
    val title: String,
    val tagline: String,
    val icon: ImageVector,
    val badge: String?,
)

/** از جاهای دیگه (مثل نوار صفحه‌ی اصلی) می‌شه خواست که پلنِ مشخصی همین لحظه در صفحه‌ی پلن‌ها باز بشه. */
object PlanFocus {
    var key by mutableStateOf<String?>(null)
}

private val PLANS = listOf(
    PlanSpec("free", "Free", "Try it out", Icons.Filled.CardGiftcard, null),
    PlanSpec("pro", "Pro", "For everyday use", Icons.Filled.Star, "POPULAR"),
    PlanSpec("max", "Max", "For power users", Icons.Filled.Bolt, "BEST VALUE"),
)

/** ویژگی‌های هر پلن از روی اعداد سرور (ربات: /plan) ساخته می‌شه تا همیشه با سرور یکی باشه. */
private fun groupsFor(key: String, p: PlanInfo): List<Pair<String, List<String>>> = listOf(
    "Data" to listOf(trf("%s per month", gbLabel(p.gb)), trf("%s per day", gbLabel(p.daily))),
    "Access" to listOf(if (p.days > 0) trf("%d days", p.days) else tr("No expiry"), if (p.devices > 1) trf("Up to %d devices", p.devices) else tr("1 device")),
    "Extras" to (if (key == "free") listOf(tr("5 free themes"), tr("5 app icons"), tr("5 locations"))
    else listOf(tr("All themes"), tr("All app icons"), tr("All locations incl. VIP servers"))),
    "Support" to listOf(if (key == "free") tr("Telegram support") else tr("Priority support")),
)

/** آیکون اختصاصی هر ویژگی (بر اساس دسته و متن). */
private fun featureIcon(group: String, index: Int, item: String): ImageVector = when {
    item.startsWith("Unlimited") || item.startsWith("نامحدود") -> Icons.Filled.AllInclusive
    group == "Data" && index == 0 -> Icons.Filled.DataUsage
    group == "Data" -> Icons.Filled.Today
    group == "Access" && index == 0 -> Icons.Filled.Timer
    group == "Access" -> Icons.Filled.PhoneAndroid
    group == "Extras" && index == 0 -> Icons.Filled.Palette
    group == "Extras" && index == 1 -> Icons.Filled.Apps
    group == "Extras" -> Icons.Filled.Public
    else -> Icons.Filled.SupportAgent
}

internal fun rank(key: String?): Int = when (key) {
    "free" -> 1
    "pro" -> 2
    "max" -> 3
    else -> 0
}

// ───────────────────────── قیمت، تایمر، پیشنهاد ─────────────────────────

@Composable
internal fun rememberNowSec(enabled: Boolean): State<Long> {
    val st = remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(enabled) {
        if (enabled) while (true) { st.value = System.currentTimeMillis() / 1000; delay(1000) }
    }
    return st
}

private fun discLive(info: PlanInfo, nowS: Long) = info.disc > 0 && (info.dend == 0L || info.dend > nowS)

/** (درصد تخفیف، قیمت نهایی): بهترین بین تخفیف پلن (تا پایان تایمر) و کد تخفیف. */
private fun effPrice(info: PlanInfo, coupon: Pair<Int, Long>?, nowS: Long): Pair<Int, Long> {
    val live = discLive(info, nowS)
    val planFinal = if (live && info.final > 0) info.final else info.price
    val planPct = if (live) info.disc else 0
    val couponBetter = coupon != null && coupon.first >= planPct
    return maxOf(planPct, coupon?.first ?: 0) to (if (couponBetter) coupon!!.second else planFinal)
}

internal fun countdownText(sec: Long): String {
    val s = sec.coerceAtLeast(0)
    val d = s / 86400
    val hms = String.format(java.util.Locale.US, "%02d:%02d:%02d", (s % 86400) / 3600, (s % 3600) / 60, s % 60)
    return if (d > 0) com.vauth.foxyvpn.i18n.bidi((trf("%d days", d) + " " + hms).replace("\u202B", "").replace("\u202C", "")) else hms
}

// ───────────────────────── صفحه ─────────────────────────

@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onEnterNewCode: () -> Unit,
    authRepository: FxaAuthRepository? = null,
    onDisconnect: () -> Unit = {},
    onUpgraded: () -> Unit = {},
    onBuy: (plan: String, discountCode: String) -> Unit = { _, _ -> },
    onOpenSupport: () -> Unit = {},
) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val uriHandler = LocalUriHandler.current
    val redeem = remember { RedeemClient(context) }
    val isAdmin = remember { redeem.adminMode }
    LaunchedEffect(Unit) { LicenseState.load(context) }
    val license by LicenseState.status.collectAsState()
    val currentKey = if (isAdmin) null else license?.plan
    val block = if (isAdmin) null else LicenseState.blockOf(license)

    val scope = rememberCoroutineScope()

    fun contact() = onOpenSupport()

    // تنظیمات سرور (محدودیت‌ها، قیمت، تخفیف) + کد تخفیف واردشده
    val cfg by rememberPlanConfig()
    var discCode by remember { mutableStateOf("") }
    var discMsg by remember { mutableStateOf<String?>(null) }
    var discOk by remember { mutableStateOf(false) }
    var discBusy by remember { mutableStateOf(false) }
    // آکاردئون: فقط یک کارت باز؛ اول پلن فعلی (یا Pro) باز است و با لمس هدر عوض می‌شود
    var expanded by remember { mutableStateOf<String?>(null) }
    // همیشه بسته شروع می‌شه؛ کاربر خودش باز می‌کنه
    var autoOpen by remember { mutableStateOf(false) }
    LaunchedEffect(PlanFocus.key) { PlanFocus.key?.let { k -> autoOpen = false; expanded = k; PlanFocus.key = null } }
    val nowBase = System.currentTimeMillis() / 1000
    val anyTimed = listOf("pro", "max").any { cfg.plan(it).dend > nowBase && cfg.plan(it).disc > 0 }
    val nowState = rememberNowSec(anyTimed)
    // پیشنهاد هوشمند: حجم رو به پایان / سقف خورده / پلن در آستانه‌ی انقضا
    val lic0 = license
    val fracLeft = lic0?.totalBytes?.takeIf { it > 0 }?.let { t -> ((t - (lic0?.usedBytes ?: 0L)).toFloat() / t.toFloat()).coerceIn(0f, 1f) }
    val lowData = !isAdmin && fracLeft != null && fracLeft <= 0.2f
    val renewOffer = cfg.renew
    val expSoon = !isAdmin && (lic0?.expiresAt ?: Long.MAX_VALUE) - nowBase in 0L..((renewOffer?.days ?: 3) * 86400L)
    val nextKey = when (currentKey) { "free" -> "pro"; "pro" -> "max"; else -> null }
    val suggestKey: String? = when {
        isAdmin || lic0 == null -> null
        expSoon && (currentKey == "pro" || currentKey == "max") -> currentKey
        (lowData || block != null) && nextKey != null -> nextKey
        else -> null
    }
    val suggestReason: String? = when {
        suggestKey == null -> null
        expSoon -> tr("Your plan ends soon — renew to keep your access.")
        block != null -> tr("You've reached your limit — upgrade to keep going.")
        else -> trf("Your data is running low (%d%% left).", ((fracLeft ?: 0f) * 100).toInt())
    }
    LaunchedEffect(currentKey, suggestKey) { if (autoOpen) expanded = suggestKey ?: currentKey ?: "pro" }
    var applied by remember { mutableStateOf<Map<String, Pair<Int, Long>>>(emptyMap()) }
    var appliedCode by remember { mutableStateOf("") }
    fun applyDiscount() {
        val code = discCode.trim()
        if (code.isEmpty() || discBusy) return
        discBusy = true; discMsg = null
        scope.launch {
            val r = listOf("pro", "max").mapNotNull { k -> PlanConfigStore.checkDiscount(code, k)?.let { k to it } }.toMap()
            discBusy = false
            if (r.isEmpty()) { discOk = false; discMsg = tr("This discount code isn't valid."); applied = emptyMap(); appliedCode = "" }
            else { discOk = true; applied = r; appliedCode = code.uppercase(); discMsg = trf("Discount applied: %d%% off", r.values.maxOf { it.first }) }
        }
    }

    // تخفیف خودکار تمدید برای کاربری که نزدیک انقضاست (کد RENEW از سرور)
    LaunchedEffect(expSoon, renewOffer?.code, currentKey) {
        val ro = renewOffer
        if (expSoon && ro != null && currentKey != null && currentKey != "free" && appliedCode.isEmpty()) {
            val r = listOf("pro", "max").mapNotNull { k -> PlanConfigStore.checkDiscount(ro.code, k)?.let { k to it } }.toMap()
            if (r.isNotEmpty()) { discOk = true; applied = r; appliedCode = ro.code; discMsg = trf("Renewal discount applied: %d%% off", r.values.maxOf { it.first }) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .border(1.dp, c.cardBorder, CircleShape),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.textPrimary.copy(alpha = 0.75f))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(tr("Plans"), fontSize = 30.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                        Text(tr("Pick what fits you"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                    }
                }

                // دلیل قفل شدن (سقف روزانه/کل، انقضا، ...)
                if (block != null) {
                    val err = MaterialTheme.colorScheme.error
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .planCard(c, 24, err.copy(alpha = 0.10f), err.copy(alpha = 0.45f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = err, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(block.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                        }
                        if (block.needsNewCode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlanButton(tr("Contact support"), c, outlined = true, modifier = Modifier.weight(1f)) { contact() }
                                PlanButton(tr("Enter new code"), c, outlined = true, modifier = Modifier.weight(1f)) { onEnterNewCode() }
                            }
                        }
                    }
                }

                if (suggestKey != null && suggestReason != null) {
                    SuggestStrip(c, suggestReason, suggestKey == currentKey, tr(PLANS.first { it.key == suggestKey }.title)) { autoOpen = false; expanded = suggestKey }
                }
                // تخفیف زمان‌دار فعال: نوار جذاب با شمارش معکوس
                val promo = listOf("pro", "max").filter { discLive(cfg.plan(it), nowBase) && cfg.plan(it).dend > nowBase }.minByOrNull { cfg.plan(it).dend }
                if (promo != null) {
                    val pi = cfg.plan(promo)
                    PromoStrip(c, trf("%s · %d%% off", tr(PLANS.first { it.key == promo }.title), pi.disc), nowState, pi.dend) { autoOpen = false; expanded = promo }
                }

                if (!isAdmin && authRepository != null) {
                    RedeemCard(authRepository = authRepository, onDisconnect = onDisconnect, onUpgraded = onUpgraded, inline = true)
                }

                DiscountBox(
                    c = c, code = discCode, busy = discBusy, message = discMsg, ok = discOk,
                    onCode = { discCode = it.uppercase(); discMsg = null },
                    onApply = ::applyDiscount,
                    appliedLabel = if (appliedCode.isNotEmpty()) appliedCode else null,
                )

                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                PLANS.forEachIndexed { i, plan ->
                    val a by animateFloatAsState(if (shown) 1f else 0f, tween(550, delayMillis = 120 * i), label = "plan-in")
                    PlanCard(
                        modifier = Modifier.alpha(a).scale(0.94f + 0.06f * a),
                        plan = plan,
                        c = c,
                        active = plan.key == currentKey,
                        lowerThanCurrent = rank(plan.key) < rank(currentKey),
                        hasPlan = rank(currentKey) > 0,
                        info = cfg.plan(plan.key),
                        coupon = applied[plan.key],
                        expanded = expanded == plan.key,
                        nowState = nowState,
                        onToggle = { autoOpen = false; expanded = if (expanded == plan.key) null else plan.key },
                        onGet = { onBuy(plan.key, if (applied.containsKey(plan.key)) appliedCode else "") },
                    )
                }

                Text(
                    tr("Payment and activation are handled in Telegram. Your new code works right after it's sent to you."),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                PlanButton(tr("Contact support"), c, outlined = true, modifier = Modifier.fillMaxWidth()) { contact() }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

internal fun Modifier.planCard(c: FoxExtras, radius: Int, fill: Color, border: Color, width: Int = 1): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .shadow(6.dp, shape, ambientColor = c.accent.copy(alpha = 0.15f), spotColor = c.accent.copy(alpha = 0.15f))
        .clip(shape)
        .background(fill)
        .border(width.dp, border, shape)
}

@Composable
private fun PlanCard(
    modifier: Modifier = Modifier,
    plan: PlanSpec,
    c: FoxExtras,
    active: Boolean,
    lowerThanCurrent: Boolean,
    hasPlan: Boolean,
    info: PlanInfo,
    coupon: Pair<Int, Long>?,
    expanded: Boolean,
    nowState: State<Long>,
    onToggle: () -> Unit,
    onGet: () -> Unit,
) {
    val highlighted = true // همه‌ی کارت‌ها دور هماهنگ با تم دارن (قبلاً Pro نداشت)
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

    val cardShape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softGlowRect(c.accent, 28f, 10f, if (active) 0.14f else 0.08f)
            .clip(cardShape)
            .background(c.card)
            .border(
                if (active || highlighted) 2.dp else 1.dp,
                if (active || highlighted) Brush.linearGradient(listOf(c.accentLight, c.accent)) else Brush.linearGradient(listOf(c.cardBorder, c.cardBorder)),
                cardShape,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .softGlow(c.accent, 6f, 0.35f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
                contentAlignment = Alignment.Center,
            ) { Icon(plan.icon, null, tint = onAccent, modifier = Modifier.size(30.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tr(plan.title), fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(tr(plan.tagline), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
            val badge = if (active) "CURRENT" else plan.badge
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.accent.copy(alpha = 0.16f))
                        .border(1.dp, c.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (active) Icons.Filled.Check else Icons.Filled.Star, null, tint = c.accentText, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr(badge), fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.accentText)
                    }
                }
            }
            val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(260), label = "chev")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(26.dp).rotate(rot))
        }

        // قیمت (اگه از ربات گذاشته شده): تخفیف پلن یا کد تخفیف → نشان OFF و قیمت خط‌خورده
        if (info.price > 0 && plan.key != "free") {
            val nowS = if (info.dend > 0) nowState.value else 0L
            val (pct, finalPrice0) = effPrice(info, coupon, nowS)
            val finalPrice = if (finalPrice0 > 0) finalPrice0 else info.price
            val cur = com.vauth.foxyvpn.data.PlanConfigStore.cached(LocalContext.current).contacts.cur.ifBlank { tr("Toman") }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatMoney(finalPrice), fontSize = 30.sp, fontWeight = FontWeight.Black, color = c.accentText)
                Spacer(Modifier.width(6.dp))
                Text(cur, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
                if (pct > 0) {
                    Spacer(Modifier.width(10.dp))
                    OffBadge(c, pct)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatMoney(info.price), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.textSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        if (info.dend > 0 && info.disc > 0 && plan.key != "free" && info.dend > nowState.value) {
            DiscountTimer(c, info.dend - nowState.value, Modifier.offset(y = (-8).dp))
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(240)) + fadeOut(tween(160)),
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groupsFor(plan.key, info).forEach { (group, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 4.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr(group).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                        color = c.accentText,
                    )
                }
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.innerCard)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(featureIcon(group, index, item), null, tint = c.accent, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(tr(item), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
                    }
                }
            }
        }

        // دکمه‌ی پایین هر کارت: خرید / ارتقا / تمدید
        when {
            active && plan.key == "free" -> PlanButton(com.vauth.foxyvpn.i18n.bidi("${tr(plan.title)} · ${tr("Active")}".replace("\u202B", "").replace("\u202C", "")), c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            active -> PlanButton(trf("Renew %s", tr(plan.title)), c, modifier = Modifier.fillMaxWidth(), onClick = onGet)
            lowerThanCurrent -> PlanButton(tr("Included in your plan"), c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            plan.key == "free" -> PlanButton(tr("Free"), c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            hasPlan -> PlanButton(trf("Upgrade to %s", tr(plan.title)), c, modifier = Modifier.fillMaxWidth(), onClick = onGet)
            else -> PlanButton(trf("Buy %s", tr(plan.title)), c, modifier = Modifier.fillMaxWidth(), onClick = onGet)
        }
        }
        }
    }
}

@Composable
private fun DiscountBox(
    c: FoxExtras,
    code: String,
    busy: Boolean,
    message: String?,
    ok: Boolean,
    onCode: (String) -> Unit,
    onApply: () -> Unit,
    appliedLabel: String? = null,
) {
    val err = MaterialTheme.colorScheme.error
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 180f else 0f, tween(260), label = "disc-chev")
    Column(
        modifier = Modifier.fillMaxWidth().planCard(c, 28, c.card, c.cardBorder).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.LocalOffer, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Discount code"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(
                    if (appliedLabel != null && !open) appliedLabel else tr("Got a promo code? Apply it to see the new price."),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = if (appliedLabel != null && !open) c.accentText else c.textSecondary,
                )
            }
            Icon(Icons.Filled.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(26.dp).rotate(rot))
        }
        AnimatedVisibility(visible = open, enter = expandVertically(tween(280)) + fadeIn(tween(280)), exit = shrinkVertically(tween(220)) + fadeOut(tween(140))) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = onCode,
                singleLine = true,
                enabled = !busy,
                placeholder = { Text("CODE", color = c.textSecondary.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(18.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onApply() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                    focusedContainerColor = c.innerCard, unfocusedContainerColor = c.innerCard,
                    cursorColor = c.accent,
                ),
                modifier = Modifier.weight(1f),
            )
            PlanButton(if (busy) "…" else tr("Apply"), c, enabled = code.isNotBlank() && !busy, modifier = Modifier.width(100.dp), onClick = onApply)
        }
        if (message != null) {
            Text(message, color = if (ok) c.accentText else err, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        }
        }
    }
}

@Composable
internal fun PlanButton(
    text: String,
    c: FoxExtras,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .then(
                if (outlined) Modifier.background(c.innerCard).border(1.dp, c.cardBorder, shape)
                else Modifier.background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                outlined && enabled -> c.accentText
                outlined -> c.textSecondary
                else -> onAccent
            },
        )
    }
}


// ───────────────────────── اجزای جدید ─────────────────────────

@Composable
private fun DiscountTimer(c: FoxExtras, remainSec: Long, modifier: Modifier = Modifier) {
    val err = MaterialTheme.colorScheme.error
    val urgent = remainSec < 3600
    val tint = if (urgent) err else c.accentText
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier.clip(shape).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Timer, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr("Offer ends in"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textSecondary)
        Spacer(Modifier.width(8.dp))
        Text(
            countdownText(remainSec), fontSize = 14.sp, fontWeight = FontWeight.Black, color = tint,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
internal fun PromoStrip(c: FoxExtras, title: String, nowState: State<Long>, end: Long, label: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    val left = (end - nowState.value).coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = onAccent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        // حداکثر دو خط: برچسب کوچک + عنوان تک‌خطی (اگر بلند بود با … کوتاه می‌شه)
        Column(Modifier.weight(1f)) {
            Text(label ?: tr("Limited-time offer"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onAccent.copy(alpha = 0.85f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = onAccent, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            countdownText(left), fontSize = 13.sp, fontWeight = FontWeight.Black, color = onAccent, maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun SuggestStrip(c: FoxExtras, reason: String, renew: Boolean, planTitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.10f)).border(1.dp, c.accent.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(if (renew) Icons.Filled.Autorenew else Icons.Filled.TrendingUp, null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(reason, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
            Text(if (renew) trf("Renew %s", planTitle) else trf("Recommended: %s", planTitle), fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentText)
        }
        Icon(Icons.Filled.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(22.dp).rotate(-90f))
    }
}

/** مقایسه‌ی سریع سه پلن کنار هم (بدون باز کردن کارت‌ها). */
@Composable
private fun CompareTable(c: FoxExtras, cfg: com.vauth.foxyvpn.data.PlanConfig, currentKey: String?, applied: Map<String, Pair<Int, Long>>, nowState: State<Long>) {
    val context = LocalContext.current
    val cur = PlanConfigStore.cached(context).contacts.cur.ifBlank { tr("Toman") }
    val nowS = nowState.value
    fun priceOf(k: String): String {
        val i = cfg.plan(k)
        if (k == "free") return tr("Free")
        if (i.price <= 0) return "—"
        val (_, fin) = effPrice(i, applied[k], nowS)
        return formatMoney(if (fin > 0) fin else i.price)
    }
    val rows: List<Pair<String, (String) -> String>> = listOf(
        tr("Per month") to { k -> cfg.plan(k).gb.let { if (it <= 0) "∞" else gbLabel(it) } },
        tr("Per day") to { k -> cfg.plan(k).daily.let { if (it <= 0) "∞" else gbLabel(it) } },
        tr("Devices") to { k -> cfg.plan(k).devices.toString() },
        tr("Duration") to { k -> cfg.plan(k).days.let { if (it > 0) trf("%d days", it) else tr("No expiry") } },
        trf("Price (%s)", cur) to { k -> priceOf(k) },
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(c.card).border(1.dp, c.cardBorder, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(tr("Compare plans").uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, color = c.accentText)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1.2f))
            PLANS.forEach { p ->
                val cur0 = p.key == currentKey
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(if (cur0) c.accent.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) { Text(tr(p.title), fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (cur0) c.accentText else c.textPrimary) }
                }
            }
        }
        rows.forEachIndexed { i, (label, value) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (i % 2 == 0) c.innerCard else Color.Transparent).padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary, modifier = Modifier.weight(1.2f))
                PLANS.forEach { p ->
                    Text(value(p.key), fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (p.key == currentKey) c.accentText else c.textPrimary, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
