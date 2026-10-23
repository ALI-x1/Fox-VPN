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

private val PLANS = listOf(
    PlanSpec("free", "Free", "Try it out", Icons.Filled.CardGiftcard, null),
    PlanSpec("pro", "Pro", "For everyday use", Icons.Filled.Star, "POPULAR"),
    PlanSpec("max", "Max", "For power users", Icons.Filled.Bolt, "BEST VALUE"),
)

/** ویژگی‌های هر پلن از روی اعداد سرور (ربات: /plan) ساخته می‌شه تا همیشه با سرور یکی باشه. */
private fun groupsFor(key: String, p: PlanInfo): List<Pair<String, List<String>>> = listOf(
    "Data" to listOf(trf("%s per month", gbLabel(p.gb)), trf("%s per day", gbLabel(p.daily))),
    "Access" to listOf(if (p.days > 0) trf("%d days", p.days) else tr("No expiry"), if (p.devices > 1) trf("Up to %d devices", p.devices) else tr("1 device")),
    "Support" to listOf(if (key == "free") tr("Telegram support") else tr("Priority support")),
)

/** آیکون اختصاصی هر ویژگی (بر اساس دسته و متن). */
private fun featureIcon(group: String, index: Int, item: String): ImageVector = when {
    item.startsWith("Unlimited") || item.startsWith("نامحدود") -> Icons.Filled.AllInclusive
    group == "Data" && index == 0 -> Icons.Filled.DataUsage
    group == "Data" -> Icons.Filled.Today
    group == "Access" && index == 0 -> Icons.Filled.Timer
    group == "Access" -> Icons.Filled.PhoneAndroid
    else -> Icons.Filled.SupportAgent
}

private fun rank(key: String?): Int = when (key) {
    "free" -> 1
    "pro" -> 2
    "max" -> 3
    else -> 0
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
                        onGet = { onBuy(plan.key, if (applied.containsKey(plan.key)) appliedCode else "") },
                    )
                }

                DiscountBox(
                    c = c, code = discCode, busy = discBusy, message = discMsg, ok = discOk,
                    onCode = { discCode = it.uppercase(); discMsg = null },
                    onApply = ::applyDiscount,
                )

                if (!isAdmin && authRepository != null) {
                    RedeemCard(authRepository = authRepository, onDisconnect = onDisconnect, onUpgraded = onUpgraded)
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
    onGet: () -> Unit,
) {
    val highlighted = true // همه‌ی کارت‌ها دور هماهنگ با تم دارن (قبلاً Pro نداشت)
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

    val cardShape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                if (active || highlighted) 12.dp else 6.dp, cardShape,
                ambientColor = c.accent.copy(alpha = if (active || highlighted) 0.35f else 0.15f),
                spotColor = c.accent.copy(alpha = if (active || highlighted) 0.35f else 0.15f),
            )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(8.dp, CircleShape, ambientColor = c.accent.copy(alpha = 0.5f), spotColor = c.accent.copy(alpha = 0.5f))
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
        }

        // قیمت (اگه از ربات گذاشته شده): تخفیف پلن یا کد تخفیف → نشان OFF و قیمت خط‌خورده
        if (info.price > 0 && plan.key != "free") {
            val pct = maxOf(info.disc, coupon?.first ?: 0)
            val couponFinal = if (coupon != null && coupon.first >= info.disc) coupon.second else null
            val finalPrice = couponFinal ?: info.final.takeIf { it > 0 } ?: info.price
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

        // دکمه‌ی پایین کارت
        when {
            active -> PlanButton("${tr(plan.title)} · ${tr("Active")}", c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            lowerThanCurrent -> PlanButton(tr("Included in your plan"), c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            plan.key == "free" -> PlanButton(tr("Free"), c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            else -> PlanButton(if (hasPlan) trf("Upgrade to %s", tr(plan.title)) else trf("Buy %s", tr(plan.title)), c, modifier = Modifier.fillMaxWidth(), onClick = onGet)
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
) {
    val err = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier.fillMaxWidth().planCard(c, 28, c.card, c.cardBorder).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.LocalOffer, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tr("Discount code"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(tr("Got a promo code? Apply it to see the new price."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
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
