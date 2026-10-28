package com.vauth.foxyvpn.ui.screens

import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.vauth.foxyvpn.data.RedeemClient
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.i18n.trf
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import com.vauth.foxyvpn.ui.components.softGlowRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Confetto(
    val x: Float, val phase: Float, val speed: Float, val size: Float,
    val sway: Float, val spin: Float, val colorIdx: Int, val round: Boolean,
)

/**
 * صفحه‌ی «حساب شما ساخته شد» بعد از ورود: لوگوی فنری، کاغذرنگی‌های رنگ تم و دکمه‌ی ادامه.
 * زدن «ادامه» → [onContinue] (اجازه‌ی نوتیف خواسته می‌شه و بعد صفحه‌ی اصلی).
 */
@Composable
fun WelcomeScreen(upgrade: Boolean = false, onContinue: () -> Unit) {
    val c = LocalFoxExtras.current
    val context = LocalContext.current
    // ورود کد دعوت (فقط کاربر تازه‌ی فری که هنوز دعوتی استفاده نکرده)
    val scope = rememberCoroutineScope()
    val refClient = remember { RedeemClient(context) }
    val uiPrefs = remember { context.getSharedPreferences("foxvpn_ui", android.content.Context.MODE_PRIVATE) }
    var refUsed by remember { mutableStateOf(uiPrefs.getBoolean("ref_used", false)) }
    var inviteOpen by remember { mutableStateOf(false) }
    var friend by remember { mutableStateOf("") }
    var inviteBusy by remember { mutableStateOf(false) }
    var inviteMsg by remember { mutableStateOf<String?>(null) }
    var inviteOk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { LicenseState.load(context) }
    val license by LicenseState.status.collectAsState()
    val planKey = license?.plan?.takeIf { it == "pro" || it == "max" } ?: "free"
    val planName = planKey.replaceFirstChar { it.uppercase() }
    val planIcon = when (planKey) { "max" -> Icons.Filled.Bolt; "pro" -> Icons.Filled.Star; else -> Icons.Filled.CardGiftcard }
    // هر چی پلن بالاتر، جشن پرشورتر
    val count = when (planKey) { "max" -> 160; "pro" -> 110; else -> 70 }
    val summary = license?.let { l ->
        val parts = mutableListOf<String>()
        parts += if (l.totalBytes == null) tr("Unlimited data") else trf("%d GB", (l.totalBytes / 1_073_741_824L).toInt())
        l.expiresAt?.let { parts += trf("%d days", kotlin.math.ceil((it - System.currentTimeMillis() / 1000) / 86400.0).toLong().coerceAtLeast(0)) }
        parts.joinToString(" · ")
    }
    val palette = remember(c, planKey) {
        listOf(c.accent, c.accentLight, c.accentText, Color(if (planKey == "max") 0xFFFFD700 else 0xFFFFD166), Color(0xFF06D6A0), Color(0xFFEF476F), Color.White)
    }
    val confetti = remember(count) { val r = Random(7); List(count) { Confetto(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.9f, 6f + r.nextFloat() * 8f, 0.02f + r.nextFloat() * 0.05f, r.nextFloat() * 360f, r.nextInt(7), r.nextBoolean()) } }

    val t by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val logoScale by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "logo",
    )
    val textAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(900, delayMillis = 350), label = "text")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // هاله‌ی رنگ تم پشت لوگو
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(c.accent.copy(alpha = 0.22f), Color.Transparent), center = Offset(540f, 700f), radius = 900f),
            ),
        )
        Canvas(Modifier.fillMaxSize()) {
            confetti.forEach { p ->
                val prog = (p.phase + t * p.speed) % 1f
                val y = -30f + prog * (size.height + 60f)
                val x = p.x * size.width + sin((prog * 6f + p.phase * 6f) * PI.toFloat()) * p.sway * size.width
                val col = palette[p.colorIdx].copy(alpha = 0.9f)
                if (p.round) {
                    drawCircle(col, p.size / 2f * 1.4f, Offset(x, y))
                } else {
                    rotate(p.spin + prog * 540f, Offset(x, y)) {
                        drawRect(col, Offset(x - p.size / 2f, y - p.size / 4f), Size(p.size * 1.3f, p.size * 0.6f))
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(190.dp).scale(pulse * logoScale).background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.30f), Color.Transparent)), RoundedCornerShape(50)))
                Image(painterResource(R.drawable.ic_fox_logo), contentDescription = null, modifier = Modifier.size(120.dp).scale(logoScale))
            }
            Spacer(Modifier.height(26.dp))
            Text(if (upgrade) "🎆" else "🎉", fontSize = 40.sp, modifier = Modifier.alpha(textAlpha))
            Spacer(Modifier.height(8.dp))
            // قرص پلن
            val pill = RoundedCornerShape(20.dp)
            Row(
                modifier = Modifier.alpha(textAlpha).clip(pill).background(c.accent.copy(alpha = 0.16f)).border(1.dp, c.accent.copy(alpha = 0.45f), pill)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(planIcon, null, tint = c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(tr(planName), color = c.accentText, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                when {
                    upgrade -> tr("Congratulations!")
                    planKey == "free" -> tr("Your account is ready!")
                    else -> trf("Welcome to %s!", tr(planName))
                },
                color = c.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.alpha(textAlpha),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (upgrade) trf("You're now on %s", tr(planName)) else tr("Welcome to Fox-VPN. Let's get you connected."),
                color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.alpha(textAlpha),
            )
            if (summary != null) {
                Spacer(Modifier.height(6.dp))
                Text(summary, color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.alpha(textAlpha))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (upgrade) tr("Your upgrade is active. Enjoy!") else tr("Next, allow notifications so you never miss news and updates."),
                color = c.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.alpha(textAlpha),
            )
            // کارت نرمِ «کد دعوت دارم؟»: بسته = یک قرص کوچک؛ باز = فیلد + اعمال
            if (!upgrade && planKey == "free" && (!refUsed || inviteOk)) {
                Spacer(Modifier.height(18.dp))
                val iShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier.alpha(textAlpha).fillMaxWidth().animateContentSize().clip(iShape)
                        .background(c.accent.copy(alpha = 0.10f)).border(1.dp, c.accent.copy(alpha = 0.35f), iShape)
                        .then(if (!inviteOpen && !inviteOk) Modifier.clickable { inviteOpen = true } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (inviteOk) {
                        Text(inviteMsg.orEmpty(), color = c.accentText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center)
                    } else if (!inviteOpen) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CardGiftcard, null, tint = c.accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Have an invite code?"), color = c.accentText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    } else {
                        Text(tr("Enter your friend's code. You both get free days."), color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = friend,
                            onValueChange = { friend = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(18); inviteMsg = null },
                            singleLine = true, enabled = !inviteBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            placeholder = { Text(tr("Friend's code"), color = c.textSecondary.copy(alpha = 0.6f)) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                                focusedContainerColor = c.innerCard, unfocusedContainerColor = c.innerCard, cursorColor = c.accent,
                            ),
                        )
                        inviteMsg?.let { Spacer(Modifier.height(6.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        Spacer(Modifier.height(8.dp))
                        PlanButton(if (inviteBusy) tr("Checking…") else tr("Apply"), c, Modifier.fillMaxWidth(), enabled = friend.length > 2 && !inviteBusy, onClick = {
                            inviteBusy = true; inviteMsg = null
                            scope.launch {
                                val r = refClient.referral(friend)
                                inviteBusy = false
                                when {
                                    r == null -> inviteMsg = tr("Could not reach the server. Try again.")
                                    r.ok -> {
                                        uiPrefs.edit().putBoolean("ref_used", true).apply()
                                        refUsed = true; inviteOk = true
                                        inviteMsg = trf("Done! +%d days added to your plan.", r.gained)
                                        LicenseState.load(context)
                                    }
                                    else -> {
                                        if (r.error == "already") { uiPrefs.edit().putBoolean("ref_used", true).apply(); refUsed = true }
                                        inviteMsg = when (r.error) {
                                            "self" -> tr("That's your own code.")
                                            "already" -> tr("You've already used an invite.")
                                            "full" -> tr("This friend has reached the invite limit.")
                                            "same_network" -> tr("This invite can't be used from the same network.")
                                            "too_old" -> tr("Invites only work in your first days with the app.")
                                            "disabled" -> tr("Invites are turned off right now.")
                                            else -> tr("This code isn't valid.")
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .alpha(textAlpha)
                    .fillMaxWidth()
                    .height(56.dp)
                    .softGlowRect(c.accent, 20f, 10f, 0.16f)
                    .background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)), shape)
                    .clickable(onClick = onContinue),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (upgrade) tr("Let's go") else tr("Continue"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
