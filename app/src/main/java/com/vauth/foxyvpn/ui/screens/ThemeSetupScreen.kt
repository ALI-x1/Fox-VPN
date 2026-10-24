package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import com.vauth.foxyvpn.ui.components.softGlow
import com.vauth.foxyvpn.ui.components.softGlowRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.ThemeController
import com.vauth.foxyvpn.ui.theme.ThemeModeSelector
import com.vauth.foxyvpn.ui.theme.ThemeSwatchRow

/**
 * قدم دوم اولین اجرا (بعد از زبان): انتخاب تم با پیش‌نمایش زنده.
 * کل صفحه از تم انتخاب‌شده رنگ می‌گیره؛ پس با هر انتخاب، پیش‌نمایش و دکمه همون لحظه عوض می‌شن.
 */
@Composable
fun ThemeSetupScreen(themeController: ThemeController, onContinue: () -> Unit) {
    val c = LocalFoxExtras.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    // ورود پله‌ای بخش‌ها
    val ease = androidx.compose.animation.core.FastOutSlowInEasing
    val a1 by animateFloatAsState(if (shown) 1f else 0f, tween(380, easing = ease), label = "a1")
    val a2 by animateFloatAsState(if (shown) 1f else 0f, tween(420, delayMillis = 90, easing = ease), label = "a2")
    val a3 by animateFloatAsState(if (shown) 1f else 0f, tween(420, delayMillis = 180, easing = ease), label = "a3")
    val a4 by animateFloatAsState(if (shown) 1f else 0f, tween(420, delayMillis = 270, easing = ease), label = "a4")
    var done by remember { mutableStateOf(false) } // دوبار لمس سریع دوبار ناوبری نکنه
    val haptics = LocalHapticFeedback.current
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).widthIn(max = 520.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                tr("Make it yours"), color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = a1 },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                tr("Pick a theme. You can change it any time in Settings."), color = c.textSecondary, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = a1 },
            )
            Spacer(Modifier.height(22.dp))

            ThemePreviewCard(Modifier.graphicsLayer { alpha = a2; translationY = (1f - a2) * 24f })

            Spacer(Modifier.height(22.dp))
            Column(Modifier.graphicsLayer { alpha = a3; translationY = (1f - a3) * 24f }) {
                ThemeModeSelector(themeController)
                Spacer(Modifier.height(14.dp))
                ThemeSwatchRow(themeController)
            }
            Spacer(Modifier.height(16.dp))
        }
        val shape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = a4 }
                .padding(top = 8.dp, bottom = 18.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(56.dp)
                .softGlowRect(c.accent, 20f, 10f, 0.16f)
                .background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)), shape)
                .clickable {
                    if (!done) {
                        done = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onContinue()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(tr("Continue"), color = onAccent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** مینیاتور صفحه‌ی اصلی: لوگو، حلقه‌ی اتصال و کارت دیتا؛ همه با رنگ‌های تم فعلی. */
@Composable
private fun ThemePreviewCard(modifier: Modifier = Modifier) {
    val c = LocalFoxExtras.current
    val shape = RoundedCornerShape(28.dp)
    val fill by animateFloatAsState(0.72f, tween(700, delayMillis = 350), label = "fill")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softGlowRect(c.accent, 28f, 12f, 0.10f)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.cardBorder, shape)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_fox_logo), contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.textPrimary)) { append("FOX-") }
                    withStyle(SpanStyle(color = c.accent)) { append("VPN") }
                },
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(92.dp)
                .softGlow(c.accent, 14f, 0.30f)
                .clip(CircleShape)
                .background(c.accent.copy(alpha = 0.12f))
                .border(3.dp, c.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_fox_logo), contentDescription = null, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("READY TO CONNECT"), color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Data remaining"), color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            val pill = RoundedCornerShape(12.dp)
            Box(
                Modifier.clip(pill).background(c.accent.copy(alpha = 0.14f)).border(1.dp, c.accent.copy(alpha = 0.35f), pill)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { Text("72%", color = c.accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.accent.copy(alpha = 0.15f))) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fill).clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0.55f), c.accent))),
            )
        }
    }
}
