package com.vauth.foxyvpn.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.Bolt
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
        key = "free", title = "Free", tagline = "Try it out", icon = Icons.Filled.Public, badge = null,
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
                        Text("Plans", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary)
                        Text("Pick what fits you", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
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
                            Text(block.message, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                        }
                        if (block.needsNewCode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlanButton("Contact support", c, outlined = true, modifier = Modifier.weight(1f)) { contact() }
                                PlanButton("Enter new code", c, outlined = true, modifier = Modifier.weight(1f)) { onEnterNewCode() }
                            }
                        }
                    }
                }

                PLANS.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        c = c,
                        active = plan.key == currentKey,
                        lowerThanCurrent = rank(plan.key) < rank(currentKey),
                        hasPlan = rank(currentKey) > 0,
                        onGet = { contact(plan.title) },
                    )
                }

                Text(
                    "Payment and activation are handled in Telegram. Your new code works right after it's sent to you.",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
                PlanButton("Contact support", c, outlined = true, modifier = Modifier.fillMaxWidth()) { contact() }
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
    plan: PlanSpec,
    c: FoxExtras,
    active: Boolean,
    lowerThanCurrent: Boolean,
    hasPlan: Boolean,
    onGet: () -> Unit,
) {
    val highlighted = plan.key == "max"
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .planCard(
                c, 28, c.card,
                border = if (active || highlighted) c.accent else c.cardBorder,
                width = if (active || highlighted) 2 else 1,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
                contentAlignment = Alignment.Center,
            ) { Icon(plan.icon, null, tint = onAccent, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary)
                Text(plan.tagline, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
            val badge = if (active) "CURRENT" else plan.badge
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.accent.copy(alpha = 0.16f))
                        .border(1.dp, c.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text(badge, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = c.accentText) }
            }
        }

        plan.groups.forEach { (group, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    group.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = c.textSecondary,
                )
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = c.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(item, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    }
                }
            }
        }

        // دکمه‌ی پایین کارت
        when {
            active -> PlanButton("${plan.title} · Active", c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            lowerThanCurrent -> PlanButton("Included in your plan", c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            plan.key == "free" -> PlanButton("Free", c, enabled = false, outlined = true, modifier = Modifier.fillMaxWidth()) {}
            else -> PlanButton(if (hasPlan) "Upgrade to ${plan.title}" else "Buy ${plan.title}", c, modifier = Modifier.fillMaxWidth(), onClick = onGet)
        }
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
            .height(50.dp)
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                outlined && enabled -> c.accentText
                outlined -> c.textSecondary
                else -> onAccent
            },
        )
    }
}
