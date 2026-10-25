package com.vauth.foxyvpn.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.i18n.tr

// نام انگلیسی تم‌ها (وقتی زبان برنامه فارسی نیست)
private fun ThemePalette.displayTitle(): String = if (Lang.isFa) title else when (this) {
    ThemePalette.ORANGE_WHITE -> "Orange & White"
    ThemePalette.OCEAN_BLUE -> "Ocean Blue"
    ThemePalette.LIME_FRESH -> "Fresh Lime"
    ThemePalette.FUCHSIA_POP -> "Fuchsia Pop"
    ThemePalette.CORAL_SUNSET -> "Coral Sunset"
    ThemePalette.GRAPHITE_MONO -> "Graphite"
    ThemePalette.BLUE_VIOLET -> "Blue & Violet"
    ThemePalette.BLACK_RED -> "Black & Red"
    ThemePalette.TURQUOISE_WHITE -> "Turquoise & White"
    ThemePalette.GRANITE_GOLD -> "Granite & Gold"
    ThemePalette.ROSE_WHITE -> "Rose & White"
    ThemePalette.FOREST_GREEN -> "Forest Green"
    ThemePalette.ORCHID_PURPLE -> "Orchid Purple"
    ThemePalette.SKY_BLUE -> "Sky Blue"
    ThemePalette.CYBER_CYAN -> "Neon Cyan"
    ThemePalette.LIQUID_GLASS -> "Liquid Glass"
    ThemePalette.MOCHA_LATTE -> "Mocha Latte"
}

private fun ThemePalette.displaySubtitle(): String = if (Lang.isFa) subtitle else when (this) {
    ThemePalette.ORANGE_WHITE -> "Warm and minimal"
    ThemePalette.OCEAN_BLUE -> "Deep and calm"
    ThemePalette.LIME_FRESH -> "Fresh and energetic"
    ThemePalette.FUCHSIA_POP -> "Bold and modern"
    ThemePalette.CORAL_SUNSET -> "Warm and lively"
    ThemePalette.GRAPHITE_MONO -> "Simple and serious"
    ThemePalette.BLUE_VIOLET -> "Cool modern gradient"
    ThemePalette.BLACK_RED -> "Dark, high contrast"
    ThemePalette.TURQUOISE_WHITE -> "Calm and clean"
    ThemePalette.GRANITE_GOLD -> "Soft and glassy"
    ThemePalette.ROSE_WHITE -> "Gentle and elegant"
    ThemePalette.FOREST_GREEN -> "Natural and calm"
    ThemePalette.ORCHID_PURPLE -> "Night and dreamy"
    ThemePalette.SKY_BLUE -> "Bright and clear"
    ThemePalette.CYBER_CYAN -> "Cyber and glowing"
    ThemePalette.LIQUID_GLASS -> "White and translucent"
    ThemePalette.MOCHA_LATTE -> "Warm and cozy"
}

/**
 * انتخاب حالت نمایش: سیستم / روشن / تیره.
 * فقط برای تم‌هایی که هر دو حالت رو دارن اثر داره؛ برای بقیه توضیح کوتاهی نشون می‌ده.
 */
@Composable
fun ThemeModeSelector(
    themeController: ThemeController,
    modifier: Modifier = Modifier,
) {
    val c = LocalFoxExtras.current
    if (!themeController.palette.supportsLightDark) {
        Text(
            text = tr("This theme has its own colors, so light/dark mode doesn't apply."),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            modifier = modifier.fillMaxWidth().padding(4.dp),
        )
        return
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.innerCard)
            .border(1.dp, c.cardBorder, shape)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeSegment(Icons.Filled.SettingsBrightness, tr("System"), themeController.mode == ThemeMode.SYSTEM, Modifier.weight(1f)) {
            themeController.set(ThemeMode.SYSTEM)
        }
        ModeSegment(Icons.Filled.LightMode, tr("Light"), themeController.mode == ThemeMode.LIGHT, Modifier.weight(1f)) {
            themeController.set(ThemeMode.LIGHT)
        }
        ModeSegment(Icons.Filled.DarkMode, tr("Dark"), themeController.mode == ThemeMode.DARK, Modifier.weight(1f)) {
            themeController.set(ThemeMode.DARK)
        }
    }
}

@Composable
private fun ModeSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = LocalFoxExtras.current
    val bg by animateColorAsState(if (selected) c.accent else Color.Transparent, tween(260), label = "mode-bg")
    val fg by animateColorAsState(
        if (selected) Color.White else c.textSecondary, tween(260), label = "mode-fg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * بخش «پالت تم‌ها»: اول تم‌های لایت/دارک‌دار، بعد تم‌هایی که رنگ اختصاصی خودشون رو دارن.
 */
@Composable
fun ThemePickerSection(
    themeController: ThemeController,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    unlocked: Boolean = true,
    onLocked: () -> Unit = {},
) {
    val c = LocalFoxExtras.current
    val selected = themeController.palette
    val systemDark = isSystemInDarkTheme()
    val isDark = themeController.resolveDark(systemDark)
    val dual = ThemePalette.values().filter { it.supportsLightDark }
    val fixed = ThemePalette.values().filter { !it.supportsLightDark }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showTitle) {
            Text(
                text = tr("Themes"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        GroupLabel(tr("Light & dark themes"), c)
        PaletteGrid(dual, selected, isDark, themeController, unlocked, onLocked)
        Spacer(Modifier.height(4.dp))
        GroupLabel(tr("Single-style themes"), c)
        PaletteGrid(fixed, selected, isDark, themeController, unlocked, onLocked)
    }
}

@Composable
private fun GroupLabel(text: String, c: FoxExtras) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = c.accentText,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun PaletteGrid(
    items: List<ThemePalette>,
    selected: ThemePalette,
    isDark: Boolean,
    themeController: ThemeController,
    unlocked: Boolean,
    onLocked: () -> Unit,
) {
    items.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowItems.forEach { palette ->
                PaletteCard(
                    palette = palette,
                    selected = palette == selected,
                    previewDark = isDark,
                    modifier = Modifier.weight(1f),
                    locked = !unlocked && !palette.isFree,
                    onClick = { if (!unlocked && !palette.isFree) onLocked() else themeController.setPalette(palette) }
                )
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PaletteCard(
    palette: ThemePalette,
    selected: Boolean,
    previewDark: Boolean,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onClick: () -> Unit,
) {
    val dual = palette.supportsLightDark
    val useDark = dual && previewDark
    val bg by animateColorAsState(
        if (useDark) palette.darkPreviewBg ?: palette.previewBg else palette.previewBg,
        tween(380), label = "palette-bg",
    )
    val text by animateColorAsState(
        if (useDark) palette.darkPreviewText ?: palette.previewText else palette.previewText,
        tween(380), label = "palette-text",
    )
    val sub by animateColorAsState(
        if (useDark) palette.darkPreviewSub ?: palette.previewSub else palette.previewSub,
        tween(380), label = "palette-sub",
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) palette.swatch else sub.copy(alpha = 0.25f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.swatch),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (locked) {
                com.vauth.foxyvpn.ui.components.ProBadge()
            } else if (dual) {
                // نشانه‌ی «هم روشن هم تیره»
                Icon(
                    imageVector = if (useDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = sub,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = palette.displayTitle(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = palette.displaySubtitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = sub,
        )
    }
}


/**
 * انتخاب سریع رنگ تم (برای صفحه‌ی راه‌اندازی اولیه): ردیف دایره‌های رنگی قابل اسکرول
 * (اول تم‌های لایت/دارک‌دار، بعد تم‌های تک‌حالته) و زیرش نام و توضیح تم انتخاب‌شده.
 */
@Composable
fun ThemeSwatchRow(
    themeController: ThemeController,
    modifier: Modifier = Modifier,
) {
    val c = LocalFoxExtras.current
    val selected = themeController.palette
    // اولین اجرا: فقط ۵ تم اصلی (لایت/دارک‌دار و آزاد برای همه)، همه توی یک ردیف و بدون اسکرول افقی
    val all = remember { ThemePalette.values().filter { it.isFree } }
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            all.forEach { p ->
                val sel = p == selected
                // اندازه‌ی ثابت ۵۶: انتخاب‌شده فقط داخل خودش بزرگ می‌شه، پس ارتفاع ردیف نمی‌پره
                val dia by animateDpAsState(if (sel) 52.dp else 42.dp, tween(220), label = "swatch-size")
                val ring by animateColorAsState(if (sel) c.textPrimary else c.cardBorder, tween(220), label = "swatch-ring")
                val checkTint = if (p.swatch.luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!sel) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                themeController.setPalette(p)
                            }
                        }
                        .semantics { contentDescription = p.displayTitle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(dia)
                            .clip(CircleShape)
                            .background(p.swatch)
                            .border(if (sel) 3.dp else 1.dp, ring, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sel) Icon(Icons.Filled.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(selected.displayTitle(), color = c.textPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(selected.displaySubtitle(), color = c.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
