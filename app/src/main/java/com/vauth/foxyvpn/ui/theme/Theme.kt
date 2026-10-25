package com.vauth.foxyvpn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

internal val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    inversePrimary = md_theme_light_inversePrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceTint = md_theme_light_surfaceTint,
    surfaceBright = md_theme_light_surfaceBright,
    surfaceDim = md_theme_light_surfaceDim,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    scrim = md_theme_light_scrim,
)

internal val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    inversePrimary = md_theme_dark_inversePrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceTint = md_theme_dark_surfaceTint,
    surfaceBright = md_theme_dark_surfaceBright,
    surfaceDim = md_theme_dark_surfaceDim,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    scrim = md_theme_dark_scrim,
)

@Composable
fun FoxyVpnTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.ORANGE_WHITE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // کاربر فری با تم قفل‌دار (مثلاً بعد از تموم شدن پلن) موقتاً تم پیش‌فرض رو می‌بینه؛ انتخابش پاک نمی‌شه و با تمدید برمی‌گرده
    val unlocked = com.vauth.foxyvpn.ui.components.Perks.rememberUnlocked()
    val palette = if (palette.isFree || unlocked) palette else ThemePalette.ORANGE_WHITE
    val systemInDarkTheme = isSystemInDarkTheme()
    // بعضی پالت‌ها ذاتاً تیره/روشنن (fixedDark) و ThemeMode روشون اثر نداره
    val darkTheme = palette.fixedDark ?: when (themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val targetSpec = remember(palette, darkTheme) { palette.spec(darkTheme) }
    // انیمیشن تغییر تم: محو شدن عکس قبلی (ThemeTransition)؛ بدون lerp و recompose در هر فریم
    val spec = targetSpec

    val context = LocalContext.current
    // رنگ داینامیک فقط وقتی کاربر خودش روشنش کنه و پالت پیش‌فرض باشه
    val colorScheme = when {
        dynamicColor && palette == ThemePalette.ORANGE_WHITE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> spec.scheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            // بدون این، اندروید روی نوار ناوبری یه پوشش نیمه‌شفاف سفید/مشکی می‌ندازه (تو حالت روشن، پایین صفحه سفید می‌شد)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    CompositionLocalProvider(
        LocalFoxyStatusColors provides spec.status,
        LocalFoxExtras provides spec.extras,
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = FoxyTypography,
            motionScheme = MotionScheme.expressive(),
            content = { ThemeTransitionHost(content) },
        )
    }
}

// ───────────────────────── انیمیشن تغییر تم ─────────────────────────

private const val THEME_FADE_MS = 520

@Composable
private fun rememberAnimatedSpec(target: FoxThemeSpec, key: Any): FoxThemeSpec {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    var lastKey by remember { mutableStateOf(key) }

    LaunchedEffect(key) {
        if (key == lastKey) return@LaunchedEffect
        lastKey = key
        // اگه وسط انیمیشن قبلی هستیم، از رنگ فعلی شروع کن
        from = lerpSpec(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(THEME_FADE_MS, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    return if (t >= 1f) to else lerpSpec(from, to, t)
}

private fun lerpSpec(a: FoxThemeSpec, b: FoxThemeSpec, t: Float): FoxThemeSpec {
    if (t <= 0f) return a
    if (t >= 1f) return b
    return FoxThemeSpec(
        scheme = lerpScheme(a.scheme, b.scheme, t),
        extras = lerpExtras(a.extras, b.extras, t),
        status = FoxyStatusColors(
            connected = lerp(a.status.connected, b.status.connected, t),
            connecting = lerp(a.status.connecting, b.status.connecting, t),
            disconnected = lerp(a.status.disconnected, b.status.disconnected, t),
        ),
    )
}

private fun lerpExtras(a: FoxExtras, b: FoxExtras, t: Float) = FoxExtras(
    bgTop = lerp(a.bgTop, b.bgTop, t), bgBottom = lerp(a.bgBottom, b.bgBottom, t),
    card = lerp(a.card, b.card, t), cardBorder = lerp(a.cardBorder, b.cardBorder, t),
    innerCard = lerp(a.innerCard, b.innerCard, t),
    textPrimary = lerp(a.textPrimary, b.textPrimary, t), textSecondary = lerp(a.textSecondary, b.textSecondary, t),
    accent = lerp(a.accent, b.accent, t), accentLight = lerp(a.accentLight, b.accentLight, t),
    accentText = lerp(a.accentText, b.accentText, t),
    upload = lerp(a.upload, b.upload, t), download = lerp(a.download, b.download, t),
    buttonFillCenter = lerp(a.buttonFillCenter, b.buttonFillCenter, t),
    buttonFillEdge = lerp(a.buttonFillEdge, b.buttonFillEdge, t),
)

private fun lerpScheme(a: ColorScheme, b: ColorScheme, t: Float): ColorScheme = b.copy(
    primary = lerp(a.primary, b.primary, t), onPrimary = lerp(a.onPrimary, b.onPrimary, t),
    primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
    onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, t),
    inversePrimary = lerp(a.inversePrimary, b.inversePrimary, t),
    secondary = lerp(a.secondary, b.secondary, t), onSecondary = lerp(a.onSecondary, b.onSecondary, t),
    secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, t),
    onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, t),
    tertiary = lerp(a.tertiary, b.tertiary, t),
    background = lerp(a.background, b.background, t), onBackground = lerp(a.onBackground, b.onBackground, t),
    surface = lerp(a.surface, b.surface, t), onSurface = lerp(a.onSurface, b.onSurface, t),
    surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, t),
    onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
    surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
    inverseSurface = lerp(a.inverseSurface, b.inverseSurface, t),
    inverseOnSurface = lerp(a.inverseOnSurface, b.inverseOnSurface, t),
    outline = lerp(a.outline, b.outline, t), outlineVariant = lerp(a.outlineVariant, b.outlineVariant, t),
    surfaceBright = lerp(a.surfaceBright, b.surfaceBright, t), surfaceDim = lerp(a.surfaceDim, b.surfaceDim, t),
    surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
    surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, t),
    surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, t),
    surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
    surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
)
