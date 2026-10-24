package com.vauth.foxyvpn.notice

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.components.FxOverlay
import com.vauth.foxyvpn.ui.components.softGlowRect
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Bit(val x: Float, val phase: Float, val speed: Float, val size: Float, val sway: Float, val spin: Float, val color: Int, val round: Boolean)

/**
 * صفحه‌ی تمام‌صفحه‌ی اعلان/آپدیت/تبریک؛ هم‌سبک «ارتقا به پرو» (WelcomeScreen).
 * داخل خود درخت Compose کشیده می‌شه (نه Dialog) پس تا بالای صفحه و پشت ناچ می‌ره و لبه‌به‌لبه‌ست.
 * فقط alpha/translation/scale با graphicsLayer انیمیت می‌شه (بدون recompose در هر فریم).
 */
@Composable
fun NoticeScreen(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    code: String = "",
    fx: String = "confetti",
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    canDismiss: Boolean = true,
    onDismiss: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val c = LocalFoxExtras.current
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    BackHandler(enabled = true) { if (canDismiss) onDismiss() }

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val ease = FastOutSlowInEasing
    val logo by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "n-logo")
    val a1 by animateFloatAsState(if (shown) 1f else 0f, tween(450, 150, ease), label = "n-a1")
    val a2 by animateFloatAsState(if (shown) 1f else 0f, tween(450, 260, ease), label = "n-a2")
    val a3 by animateFloatAsState(if (shown) 1f else 0f, tween(450, 370, ease), label = "n-a3")
    val a4 by animateFloatAsState(if (shown) 1f else 0f, tween(450, 480, ease), label = "n-a4")
    val inf = rememberInfiniteTransition(label = "n-inf")
    val pulse by inf.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1600, easing = ease), RepeatMode.Reverse), label = "n-pulse")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "n-t")

    val emojiFx = fx == "fireworks" || fx == "hearts" || fx == "snow"
    val bits = remember(fx) {
        if (emojiFx || fx == "none" || fx.isBlank()) emptyList()
        else { val r = Random(7); List(90) { Bit(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.9f, 6f + r.nextFloat() * 8f, 0.02f + r.nextFloat() * 0.05f, r.nextFloat() * 360f, r.nextInt(6), r.nextBoolean()) } }
    }
    val palette = remember(c) { listOf(c.accent, c.accentLight, c.accentText, Color(0xFFFFD166), Color(0xFF06D6A0), Color.White) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            .pointerInput(Unit) { detectTapGestures { } }, // لمس به صفحه‌ی زیرش نرسه
    ) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.24f), Color.Transparent), center = Offset(540f, 650f), radius = 950f)))
        if (bits.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                bits.forEach { p ->
                    val prog = (p.phase + t * p.speed) % 1f
                    val y = -30f + prog * (size.height + 60f)
                    val x = p.x * size.width + sin((prog * 6f + p.phase * 6f) * PI.toFloat()) * p.sway * size.width
                    val col = palette[p.color].copy(alpha = 0.9f)
                    if (p.round) drawCircle(col, p.size * 0.7f, Offset(x, y))
                    else rotate(p.spin + prog * 540f, Offset(x, y)) { drawRect(col, Offset(x - p.size / 2f, y - p.size / 4f), Size(p.size * 1.3f, p.size * 0.6f)) }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { scaleX = logo; scaleY = logo; alpha = logo.coerceIn(0f, 1f) }) {
                Box(Modifier.size(190.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.30f), Color.Transparent)), CircleShape))
                Image(painterResource(R.drawable.ic_fox_logo), contentDescription = null, modifier = Modifier.size(112.dp))
            }
            Spacer(Modifier.height(22.dp))
            if (badge != null) {
                val pill = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier.graphicsLayer { alpha = a1; translationY = (1f - a1) * 20f }
                        .clip(pill).background(c.accent.copy(alpha = 0.16f)).border(1.dp, c.accent.copy(alpha = 0.45f), pill)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(badge, color = c.accentText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Spacer(Modifier.height(14.dp))
            }
            Text(
                title, color = c.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = a2; translationY = (1f - a2) * 24f },
            )
            if (body.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f, fill = false).widthIn(max = 520.dp).verticalScroll(rememberScrollState()).graphicsLayer { alpha = a3; translationY = (1f - a3) * 24f }) {
                    Text(body, color = c.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            if (code.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                val chip = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier.graphicsLayer { alpha = a3 }
                        .clip(chip).background(c.accent.copy(alpha = 0.16f)).border(1.dp, c.accent.copy(alpha = 0.55f), chip)
                        .clickable {
                            runCatching { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("code", code)) }
                            Toast.makeText(ctx, tr("Code copied"), Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(code, color = c.accentText, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Tap to copy"), color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(30.dp))
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier.graphicsLayer { alpha = a4; translationY = (1f - a4) * 20f }
                    .fillMaxWidth().widthIn(max = 520.dp).height(56.dp)
                    .softGlowRect(c.accent, 20f, 10f, 0.16f)
                    .background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)), shape)
                    .clickable(onClick = onPrimary),
                contentAlignment = Alignment.Center,
            ) { Text(primary, color = onAccent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
            if (secondary != null && onSecondary != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onSecondary, modifier = Modifier.graphicsLayer { alpha = a4 }) { Text(secondary, color = c.textSecondary, fontWeight = FontWeight.Bold) }
            }
        }
        if (emojiFx) FxOverlay(fx, durationMs = 5200)
    }
}
