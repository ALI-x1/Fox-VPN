package com.vauth.foxyvpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.BuildConfig
import com.vauth.foxyvpn.data.ContactInfo
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.data.formatMoney
import com.vauth.foxyvpn.data.gbLabel
import com.vauth.foxyvpn.data.rememberPlanConfig
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras

// ───────────────────────── کمکی‌ها (مشترک بین پشتیبانی و خرید) ─────────────────────────

/** آیدی/لینک تلگرام → لینک کامل. */
internal fun tgUrl(v: String): String {
    val t = v.trim()
    return when {
        t.startsWith("http", true) -> t
        t.startsWith("t.me/", true) -> "https://$t"
        else -> "https://t.me/${t.removePrefix("@")}"
    }
}

internal fun copyText(context: Context, text: String, label: String) {
    runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
    Toast.makeText(context, trf("%s copied", label), Toast.LENGTH_SHORT).show()
}

/** اطلاعات برای پشتیبانی: نسخه، پلن و شناسه‌ی کوتاه دستگاه (بدون خودِ کد). */
internal fun supportInfo(context: Context): String {
    val plan = LicenseState.status.value?.plan ?: "-"
    val dev = RedeemClient(context).deviceId.take(8)
    return "Fox-VPN ${BuildConfig.VERSION_NAME} · plan: $plan · device: $dev"
}

/** یک گزینه‌ی ارتباط/پرداخت با آیکون مخصوص خودش. [enabled] = false یعنی هنوز آدرسش گذاشته نشده («به‌زودی»). */
@Composable
internal fun ContactOption(
    c: FoxExtras,
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    actionIcon: ImageVector,
    tint: Color = c.accent,
    index: Int = 0,
    onClick: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450, delayMillis = 70 * index), label = "opt-in")
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(a * (if (enabled) 1f else 0.55f))
            .scale(0.96f + 0.04f * a)
            .clip(shape)
            .background(c.card)
            .border(1.dp, if (enabled) tint.copy(alpha = 0.35f) else c.cardBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            Text(
                if (enabled) subtitle else tr("Coming soon"),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (enabled) {
            Spacer(Modifier.width(8.dp))
            Icon(actionIcon, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ScreenHeader(c: FoxExtras, title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(46.dp).clip(CircleShape).background(c.card).border(1.dp, c.cardBorder, CircleShape),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.textPrimary.copy(alpha = 0.75f)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
        }
    }
}

@Composable
private fun HeroBadge(c: FoxExtras, icon: ImageVector, text: String) {
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = c.accent.copy(alpha = 0.3f), spotColor = c.accent.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(c.accentLight, c.accent)))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = onAccent, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = onAccent)
    }
}

/** گزینه‌های «ارتباط با من»: ربات پاسخگو + آیدی تلگرام. */
@Composable
private fun ContactOptions(c: FoxExtras, contacts: ContactInfo, startIndex: Int, message: String) {
    val uri = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val tg = contacts.tg.ifBlank { RedeemClient.SUPPORT_URL }
    ContactOption(
        c, Icons.Filled.SmartToy, tr("Telegram bot"), tr("Fast answers, any time"),
        enabled = contacts.bot.isNotBlank(), actionIcon = Icons.AutoMirrored.Filled.OpenInNew, index = startIndex,
    ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); runCatching { uri.openUri(tgUrl(contacts.bot)) } }
    ContactOption(
        c, Icons.Filled.AccountCircle, tr("Admin"), tr("Telegram admin"),
        enabled = true, actionIcon = Icons.AutoMirrored.Filled.Send, index = startIndex + 1,
    ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); runCatching { uri.openUri("${tgUrl(tg)}?text=${Uri.encode(message)}") } }
}

// ───────────────────────── صفحه‌ی پشتیبانی ─────────────────────────

@Composable
fun SupportScreen(onBack: () -> Unit, onOpenPlans: () -> Unit = {}) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val cfg by rememberPlanConfig()
    val msg = "Hi, I need help with Fox-VPN.\n${supportInfo(context)}"
    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ScreenHeader(c, tr("Support"), tr("We're here to help"), onBack)
                HeroBadge(c, Icons.Filled.SupportAgent, tr("Have a question or a problem? Pick the fastest way to reach us."))
                ContactOption(
                    c, Icons.Filled.LocalOffer, tr("Plans & prices"), tr("See what each plan includes"),
                    enabled = true, actionIcon = Icons.AutoMirrored.Filled.OpenInNew, index = 0,
                ) { onOpenPlans() }
                ContactOptions(c, cfg.contacts, 1, msg)
                ContactOption(
                    c, Icons.Filled.Receipt, tr("Copy support info"), tr("Version, plan and device ID"),
                    enabled = true, actionIcon = Icons.Filled.ContentCopy, index = 3,
                ) { copyText(context, supportInfo(context), tr("Support info")) }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.innerCard).padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Info, null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        tr("Tip: send your code name (not the code itself) and the support info — it helps us answer faster."),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ───────────────────────── صفحه‌ی خرید ─────────────────────────

@Composable
fun PurchaseScreen(planKey: String, discountCode: String = "", onBack: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val uri = LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    val cfg by rememberPlanConfig()
    val plan = cfg.plan(planKey)
    val name = tr(planKey.replaceFirstChar { it.uppercase() })
    val ct = cfg.contacts
    val cur = ct.cur.ifBlank { tr("Toman") }
    val code = discountCode.trim()
    val msg = "Hi, I want to get the ${planKey.uppercase()} plan on Fox-VPN." +
        (if (code.isNotEmpty()) "\nDiscount code: $code" else "") + "\n${supportInfo(context)}"

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ScreenHeader(c, trf("Buy %s", name), tr("Choose how you want to pay"), onBack)

                // خلاصه‌ی پلن + قیمت
                val shape = RoundedCornerShape(26.dp)
                Column(
                    modifier = Modifier.fillMaxWidth().clip(shape).background(c.card)
                        .border(2.dp, Brush.linearGradient(listOf(c.accentLight, c.accent)), shape).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                    Text(
                        trf("%s per month · %s per day · %d devices", gbLabel(plan.gb), gbLabel(plan.daily), plan.devices),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
                    )
                    if (plan.price > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(formatMoney(plan.final.takeIf { it > 0 } ?: plan.price), fontSize = 30.sp, fontWeight = FontWeight.Black, color = c.accentText)
                            Spacer(Modifier.width(6.dp))
                            Text(cur, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            if (plan.disc > 0) {
                                Spacer(Modifier.width(10.dp))
                                OffBadge(c, plan.disc)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatMoney(plan.price), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                    if (code.isNotEmpty()) Text(trf("Discount code: %s", code.uppercase()), fontWeight = FontWeight.Bold, color = c.accentText)
                }
                if (ct.note.isNotBlank()) {
                    Text(com.vauth.foxyvpn.i18n.bidi(ct.note), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                ContactOption(
                    c, Icons.Filled.SmartToy, tr("Buy with the bot"), tr("Automatic and fast"),
                    enabled = ct.bot.isNotBlank(), actionIcon = Icons.AutoMirrored.Filled.OpenInNew, index = 0,
                ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); runCatching { uri.openUri(tgUrl(ct.bot)) } }
                ContactOption(
                    c, Icons.Filled.CreditCard, tr("Card to card"), ct.card.ifBlank { "" },
                    enabled = ct.card.isNotBlank(), actionIcon = Icons.Filled.ContentCopy, index = 1,
                ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); copyText(context, ct.card.replace(" ", ""), tr("Card number")) }
                ContactOption(
                    c, Icons.Filled.CurrencyBitcoin, tr("Crypto"), ct.crypto,
                    enabled = ct.crypto.isNotBlank(), actionIcon = Icons.Filled.ContentCopy, index = 2,
                ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); copyText(context, ct.crypto, tr("Wallet address")) }
                ContactOption(
                    c, Icons.Filled.VolunteerActivism, tr("Donate"), tr("Support the project and get your plan"),
                    enabled = ct.donate.isNotBlank(), actionIcon = if (ct.donate.startsWith("http")) Icons.AutoMirrored.Filled.OpenInNew else Icons.Filled.ContentCopy, index = 3,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (ct.donate.startsWith("http")) runCatching { uri.openUri(ct.donate) } else copyText(context, ct.donate, tr("Donate link"))
                }
                ContactOption(
                    c, Icons.Filled.AccountCircle, tr("Message the owner"), ct.tg.ifBlank { RedeemClient.SUPPORT_URL }.removePrefix("https://"),
                    enabled = true, actionIcon = Icons.AutoMirrored.Filled.Send, index = 4,
                ) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); runCatching { uri.openUri("${tgUrl(ct.tg.ifBlank { RedeemClient.SUPPORT_URL })}?text=${Uri.encode(msg)}") } }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.innerCard).padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Shield, null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        tr("After paying, send the receipt. Your new code is delivered in Telegram and works right away."),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** نشان «٪ OFF» کنار قیمت تخفیف‌خورده. */
@Composable
internal fun OffBadge(c: FoxExtras, percent: Int) {
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    Row(
        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Brush.horizontalGradient(listOf(c.accentLight, c.accent))).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocalOffer, null, tint = onAccent, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text("$percent% OFF", fontSize = 11.sp, fontWeight = FontWeight.Black, color = onAccent)
    }
}
