package com.vauth.foxyvpn.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.vauth.foxyvpn.ui.components.softGlow
import com.vauth.foxyvpn.ui.components.softGlowRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.i18n.AppLanguage
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras

/** Activity زیر یه زنجیره از ContextWrapper (برای recreate بعد از عوض شدن زبان). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** نام نمایشی گزینه‌ی زبان (برای ردیف تنظیمات). */
fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> tr("System default")
    AppLanguage.PERSIAN -> "فارسی"
    AppLanguage.ENGLISH -> "English"
}

// ───────────────────────── صفحه‌ی انتخاب زبان (اولین اجرا) ─────────────────────────

/**
 * اولین بار که برنامه باز می‌شه، قبل از صفحه‌ی ورود. متن‌ها دوزبانه‌ان چون هنوز زبانی انتخاب نشده.
 * پیش‌فرض «زبان سیستم» انتخاب شده: گوشی فارسی ← برنامه فارسی، غیر از اون ← انگلیسی.
 */
@Composable
fun LanguageScreen(onConfirm: (AppLanguage) -> Unit) {
    val c = LocalFoxExtras.current
    // گوشی فارسی ← «فارسی» از اول انتخاب شده؛ بقیه ← زبان سیستم (انگلیسی)
    var selected by remember { mutableStateOf(if (Lang.systemIsPersian()) AppLanguage.PERSIAN else AppLanguage.SYSTEM) }
    val effFa = when (selected) { AppLanguage.PERSIAN -> true; AppLanguage.ENGLISH -> false; AppLanguage.SYSTEM -> Lang.systemIsPersian() }
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

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
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                // آیکون بزرگ با هاله‌ی نرم
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(c.accent.copy(alpha = 0.10f))
                            .border(1.dp, c.accent.copy(alpha = 0.18f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .softGlow(c.accent, 12f, 0.30f)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        LanguageGlyph(onAccent = onAccent, accent = c.accent)
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    "Choose your language",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "زبان خود را انتخاب کنید",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(26.dp))

                LanguageOptions(selected = selected, onSelect = { selected = it }, bilingual = true, c = c)

                Spacer(Modifier.weight(1.3f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .softGlowRect(c.accent, 20f, 10f, 0.16f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
                        .clickable { onConfirm(selected) },
                    contentAlignment = Alignment.Center,
                ) {
                    // متن دکمه همان لحظه با زبان انتخاب‌شده عوض می‌شه
                    androidx.compose.animation.Crossfade(targetState = effFa, animationSpec = androidx.compose.animation.core.tween(200), label = "lang-continue") { fa ->
                        Text(
                            if (fa) "ادامه" else "Continue",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = onAccent,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ───────────────────────── کشوی انتخاب زبان (تنظیمات) ─────────────────────────

/** کشوی پایین صفحه با سه گزینه؛ با انتخاب، زبان اعمال می‌شه. */
@Composable
fun LanguagePickerSheet(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalFoxExtras.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgTop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(tr("Language"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary)
            Text(tr("Choose the app language"), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Spacer(Modifier.height(16.dp))
            LanguageOptions(selected = current, onSelect = onSelect, bilingual = false, c = c)
        }
    }
}

// ───────────────────────── گزینه‌ها (مشترک) ─────────────────────────

@Composable
private fun LanguageOptions(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    bilingual: Boolean,
    c: FoxExtras,
) {
    val systemName = if (Lang.systemIsPersian()) "فارسی" else "English"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LanguageOptionCard(
            title = if (bilingual) "System  ·  سیستم" else tr("System default"),
            subtitle = if (bilingual) "$systemName" else tr("Follows your phone's language") + " ($systemName)",
            icon = { Icon(Icons.Filled.PhoneAndroid, null, tint = c.accent, modifier = Modifier.size(24.dp)) },
            selected = selected == AppLanguage.SYSTEM,
            c = c,
            onClick = { onSelect(AppLanguage.SYSTEM) },
        )
        LanguageOptionCard(
            title = "فارسی",
            subtitle = "Persian",
            icon = { LangBadge("فا", c) },
            selected = selected == AppLanguage.PERSIAN,
            c = c,
            onClick = { onSelect(AppLanguage.PERSIAN) },
        )
        LanguageOptionCard(
            title = "English",
            subtitle = if (bilingual) "انگلیسی" else "English",
            icon = { LangBadge("EN", c) },
            selected = selected == AppLanguage.ENGLISH,
            c = c,
            onClick = { onSelect(AppLanguage.ENGLISH) },
        )
    }
}

@Composable
private fun LangBadge(text: String, c: FoxExtras) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = c.accent)
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    c: FoxExtras,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val onAccent = if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.softGlowRect(c.accent, 22f, 8f, 0.12f) else Modifier)
            .clip(shape)
            .background(if (selected) c.accent.copy(alpha = 0.10f) else c.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (selected) c.accent else Color.Transparent)
                .border(2.dp, if (selected) c.accent else c.cardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = onAccent, modifier = Modifier.size(16.dp))
        }
    }
}

/** نماد زبان: دو کاشی هم‌پوشان با «A» و «آ» (انگلیسی و فارسی). */
@Composable
private fun LanguageGlyph(onAccent: Color, accent: Color) {
    Box(modifier = Modifier.size(60.dp)) {
        // کاشی پر: A
        Box(
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(11.dp))
                .background(onAccent),
            contentAlignment = Alignment.Center,
        ) {
            Text("A", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        }
        // کاشی خطی: آ
        Box(
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(11.dp))
                .background(onAccent.copy(alpha = 0.16f))
                .border(1.5.dp, onAccent.copy(alpha = 0.75f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("آ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = onAccent)
        }
    }
}
