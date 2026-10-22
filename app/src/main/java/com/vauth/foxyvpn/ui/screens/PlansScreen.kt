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
    /** دسته‌بندی ویژگی‌ها: عنوان دسته به همراه لیست مزیت‌ها. */
    val groups: List<Pair<String, List<String>>>,
)

private val PLANS = listOf(
    PlanSpec(
        key = "free", title = "Free", tagline = "Try it out", icon = Icons.Filled.CardGiftcard, badge = null,
        groups = listOf(
            "Data" to listOf("5 GB in total", "1 GB per day"),
            "Access" to listOf("7 days", "1 device"),
            "Support" to listOf("Telegram support"),
        ),
    ),
    PlanSpec(
        key = "pro", title = "Pro", tagline = "For everyday use", icon = Icons.Filled.Star, badge = "POPULAR",
        groups = listOf(
            "Data" to listOf("50 GB in total", "5 GB per day"),
            "Access" to listOf("30 days", "Up to 3 devices"),
            "Support" to listOf("Priority support"),
        ),
    ),
    PlanSpec(
        key = "max", title = "Max", tagline = "No limits", icon = Icons.Filled.Bolt, badge = "BEST VALUE",
        groups = listOf(
            "Data" to listOf("Unlimited data", "No daily limit"),
            "Access" to listOf("30 days", "Up to 5 devices"),
            "Support" to listOf("Priority support"),
        ),
    ),
)

/** آیکون اختصاصی هر ویژگی (بر اساس دسته و متن). */
private fun featureIcon(group: String, index: Int, item: String): ImageVector = when {
    item.contains("Unlimited") || item.contains("No daily") -> Icons.Filled.AllInclusive
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
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var codeMsg by remember { mutableStateOf<String?>(null) }
    var codeOk by remember { mutableStateOf(false) }

    // کد جدید: اگه پلن بالاتر بشه صفحه‌ی تبریک باز می‌شه، وگرنه فقط پیام «اعمال شد»
    fun applyCode() {
        val repo = authRepository ?: return
        if (!RedeemClient.isComplete(code) || busy) return
        val before = rank(currentKey)
        val oldCode = redeem.activeCode
        val releaseOld = redeem.releaseSnapshot()
        busy = true; codeMsg = null
        scope.launch {
            val grant = redeem.redeem(code).getOrElse {
                busy = false; codeOk = false; codeMsg = it.message ?: tr("Could not verify the code"); return@launch
            }
            onDisconnect()
            val r = repo.loginWithRefreshToken(grant.refreshToken)
            busy = false
            if (r.isSuccess) {
                code = ""
                if (redeem.activeCode != oldCode) releaseOld() // جای دستگاه روی کد قبلی آزاد بشه
                if (rank(LicenseState.status.value?.plan) > before) onUpgraded()
                else { codeOk = true; codeMsg = tr("Your code was applied.") }
            } else {
                codeOk = false; codeMsg = tr("This code can't sign in right now. Please contact support.")
            }
        }
    }

    fun contact(plan: String? = null) {
        val text = if (plan == null) "Hi, I need help with my Fox-VPN plan." else "Hi, I want to get the $plan plan on Fox-VPN."
        uriHandler.openUri("${RedeemClient.SUPPORT_URL}?text=${Uri.encode(text)}")
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
                        onGet = { contact(plan.title) },
                    )
                }

                if (!isAdmin && authRepository != null) {
                    RedeemBox(
                        c = c, code = code, busy = busy, message = codeMsg, ok = codeOk,
                        onCode = { code = RedeemClient.formatCode(it); codeMsg = null },
                        onApply = ::applyCode,
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

private fun Modifier.planCard(c: FoxExtras, radius: Int, fill: Color, border: Color, width: Int = 1): Modifier {
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
    onGet: () -> Unit,
) {
    val highlighted = plan.key == "max"
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

        plan.groups.forEach { (group, items) ->
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
private fun RedeemBox(
    c: FoxExtras,
    code: String,
    busy: Boolean,
    message: String?,
    ok: Boolean,
    onCode: (String) -> Unit,
    onApply: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val err = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier.fillMaxWidth().planCard(c, 28, c.card, c.accent.copy(alpha = 0.45f), 2).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.VpnKey, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tr("Have a code?"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(tr("Enter it and your plan upgrades instantly."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            singleLine = true,
            enabled = !busy,
            placeholder = { Text("FOX-XXXX-XXXX-XXXX", color = c.textSecondary.copy(alpha = 0.6f)) },
            shape = RoundedCornerShape(18.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onApply() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                focusedContainerColor = c.innerCard, unfocusedContainerColor = c.innerCard,
                cursorColor = c.accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (message != null) {
            Text(message, color = if (ok) c.accentText else err, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        PlanButton(
            if (busy) tr("Checking…") else tr("Redeem"), c,
            enabled = RedeemClient.isComplete(code) && !busy, modifier = Modifier.fillMaxWidth(), onClick = onApply,
        )
    }
}

@Composable
private fun PlanButton(
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
