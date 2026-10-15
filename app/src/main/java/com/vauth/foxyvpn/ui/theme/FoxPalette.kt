package com.vauth.foxyvpn.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * پالت‌های تم. fixedDark:
 *  - null  → از ThemeMode (سیستم/روشن/تیره) پیروی می‌کنه
 *  - true/false → خود تم ذاتاً تیره/روشنه و ThemeMode روش اثر نداره
 */
enum class ThemePalette(
    val title: String,
    val subtitle: String,
    val fixedDark: Boolean?,
    val swatch: Color,
    val previewBg: Color,
    val previewText: Color,
    val previewSub: Color,
) {
    ORANGE_WHITE(
        "نارنجی و سفید", "روشن و مینیمال", null,
        Color(0xFFF59E0B), Color(0xFFFFF8EE), Color(0xFF2B1D10), Color(0xFF8A7560)
    ),
    BLUE_VIOLET(
        "آبی و بنفش", "گرادیان سرد و مدرن", true,
        Color(0xFF4A63FF), Color(0xFF12162E), Color(0xFFFFFFFF), Color(0xFF8E9BFF)
    ),
    BLACK_RED(
        "مشکی و قرمز", "تیره و پرکنتراست", true,
        Color(0xFFE63946), Color(0xFF111111), Color(0xFFFFFFFF), Color(0xFFD9A3A8)
    ),
    TURQUOISE_WHITE(
        "فیروزه‌ای و سفید", "آرام و تمیز", false,
        Color(0xFF14B8A6), Color(0xFFF1FFFB), Color(0xFF0F3B37), Color(0xFF5F8A85)
    ),
    GRANITE_GOLD(
        "گرانیت و طلایی", "نرم و شیشه‌ای", false,
        Color(0xFFB89B5E), Color(0xFFF5EFE2), Color(0xFF3A2F1B), Color(0xFF8A7A5C)
    );

    companion object {
        fun fromName(name: String?): ThemePalette =
            values().firstOrNull { it.name == name } ?: ORANGE_WHITE
    }
}

/** رنگ‌های اضافه‌ی مخصوص UI برند (فراتر از ColorScheme متریال). */
@Immutable
data class FoxExtras(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val innerCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,        // رنگ اصلی: دکمه، هاله، آیکون‌ها
    val accentLight: Color,   // شروع گرادیان حاشیه‌ی دکمه
    val accentText: Color,    // متن‌های رنگی (CONNECT، Auto)
    val upload: Color,
    val download: Color,
    val buttonFillCenter: Color,
    val buttonFillEdge: Color,
)

@Immutable
data class FoxThemeSpec(
    val scheme: ColorScheme,
    val extras: FoxExtras,
    val status: FoxyStatusColors,
)

val OrangeLightExtras = FoxExtras(
    bgTop = Color(0xFFFFFAF4), bgBottom = Color(0xFFFDEBD6),
    card = Color(0xB3FFFFFF), cardBorder = Color(0x33F7941D), innerCard = Color(0xFFFFF4E8),
    textPrimary = Color(0xFF14172B), textSecondary = Color(0xFF5B6275),
    accent = Color(0xFFF7941D), accentLight = Color(0xFFFFC857), accentText = Color(0xFFC2570C),
    upload = Color(0xFFF7941D), download = Color(0xFF29B6F6),
    buttonFillCenter = Color(0xFFFFFCF8), buttonFillEdge = Color(0xFFFBE3C4),
)

val OrangeDarkExtras = FoxExtras(
    bgTop = Color(0xFF16120E), bgBottom = Color(0xFF241B12),
    card = Color(0xFF2A221A), cardBorder = Color(0x33FFB066), innerCard = Color(0xFF33291F),
    textPrimary = Color(0xFFF5EEE6), textSecondary = Color(0xFFB8AB9C),
    accent = Color(0xFFF7941D), accentLight = Color(0xFFFFC857), accentText = Color(0xFFFFC857),
    upload = Color(0xFFF7941D), download = Color(0xFF29B6F6),
    buttonFillCenter = Color(0xFF4A3520), buttonFillEdge = Color(0xFF2E2217),
)

private val BlueExtras = FoxExtras(
    bgTop = Color(0xFF10142B), bgBottom = Color(0xFF1B1F4D),
    card = Color(0xCC1E2552), cardBorder = Color(0x445B6CFF), innerCard = Color(0xFF232A5E),
    textPrimary = Color(0xFFEEF0FF), textSecondary = Color(0xFFA4A9D6),
    accent = Color(0xFF5B6CFF), accentLight = Color(0xFF8B7BFF), accentText = Color(0xFF9FA8FF),
    upload = Color(0xFF8B7BFF), download = Color(0xFF4FC3F7),
    buttonFillCenter = Color(0xFF2E3580), buttonFillEdge = Color(0xFF1A1F52),
)

private val RedExtras = FoxExtras(
    bgTop = Color(0xFF0A0A0A), bgBottom = Color(0xFF1A0B0D),
    card = Color(0xCC1C1C1C), cardBorder = Color(0x44E63946), innerCard = Color(0xFF262626),
    textPrimary = Color(0xFFF5F5F5), textSecondary = Color(0xFFA8A8A8),
    accent = Color(0xFFE63946), accentLight = Color(0xFFFF6B6B), accentText = Color(0xFFFF8A93),
    upload = Color(0xFFFF5D6C), download = Color(0xFFFFA94D),
    buttonFillCenter = Color(0xFF2E1316), buttonFillEdge = Color(0xFF190A0C),
)

private val TealExtras = FoxExtras(
    bgTop = Color(0xFFF4FFFC), bgBottom = Color(0xFFDDF6F0),
    card = Color(0xB3FFFFFF), cardBorder = Color(0x3314B8A6), innerCard = Color(0xFFE6FAF6),
    textPrimary = Color(0xFF0F2A2A), textSecondary = Color(0xFF4A6A68),
    accent = Color(0xFF14B8A6), accentLight = Color(0xFF5EEAD4), accentText = Color(0xFF0B7F73),
    upload = Color(0xFF14B8A6), download = Color(0xFF38BDF8),
    buttonFillCenter = Color(0xFFFFFFFF), buttonFillEdge = Color(0xFFCFF3EC),
)

private val GoldExtras = FoxExtras(
    bgTop = Color(0xFFF7F2E8), bgBottom = Color(0xFFE9DEC6),
    card = Color(0xB3FFFBF3), cardBorder = Color(0x55B89B5E), innerCard = Color(0xFFF1E9D8),
    textPrimary = Color(0xFF2B2418), textSecondary = Color(0xFF6E624C),
    accent = Color(0xFFB89B5E), accentLight = Color(0xFFE0C98E), accentText = Color(0xFF7A5F2B),
    upload = Color(0xFFB89B5E), download = Color(0xFF7F93A3),
    buttonFillCenter = Color(0xFFFCF8EE), buttonFillEdge = Color(0xFFE6D8B8),
)

private fun buildScheme(
    dark: Boolean,
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color,
    background: Color, onSurface: Color,
    surfaceLow: Color, surfaceMid: Color, surfaceHigh: Color, surfaceHighest: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    outline: Color,
): ColorScheme = if (dark) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primaryContainer,
        secondary = secondary, onSecondary = onPrimary,
        secondaryContainer = primaryContainer, onSecondaryContainer = onPrimaryContainer,
        tertiary = secondary,
        background = background, onBackground = onSurface,
        surface = background, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        surfaceBright = surfaceHighest, surfaceDim = background,
        surfaceContainerLowest = background, surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceMid, surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = outline, outlineVariant = surfaceVariant,
        inverseSurface = onSurface, inverseOnSurface = background,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primaryContainer,
        secondary = secondary, onSecondary = onPrimary,
        secondaryContainer = primaryContainer, onSecondaryContainer = onPrimaryContainer,
        tertiary = secondary,
        background = background, onBackground = onSurface,
        surface = background, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        surfaceBright = background, surfaceDim = surfaceHighest,
        surfaceContainerLowest = Color.White, surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceMid, surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = outline, outlineVariant = surfaceVariant,
        inverseSurface = onSurface, inverseOnSurface = background,
    )
}

private val BlueScheme = buildScheme(
    dark = true,
    primary = Color(0xFF8E9BFF), onPrimary = Color(0xFF0B1066),
    primaryContainer = Color(0xFF2D3A9E), onPrimaryContainer = Color(0xFFDDE0FF),
    secondary = Color(0xFFB39DFF),
    background = Color(0xFF10142B), onSurface = Color(0xFFEEF0FF),
    surfaceLow = Color(0xFF151A38), surfaceMid = Color(0xFF1B2147),
    surfaceHigh = Color(0xFF232A5E), surfaceHighest = Color(0xFF2B336F),
    surfaceVariant = Color(0xFF2A3068), onSurfaceVariant = Color(0xFFA4A9D6),
    outline = Color(0xFF6F76B0),
)

private val RedScheme = buildScheme(
    dark = true,
    primary = Color(0xFFFF5A68), onPrimary = Color(0xFF4D0008),
    primaryContainer = Color(0xFF8C1220), onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFFF8A93),
    background = Color(0xFF0F0F0F), onSurface = Color(0xFFF5F5F5),
    surfaceLow = Color(0xFF161616), surfaceMid = Color(0xFF1C1C1C),
    surfaceHigh = Color(0xFF262626), surfaceHighest = Color(0xFF303030),
    surfaceVariant = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF777777),
)

private val TealScheme = buildScheme(
    dark = false,
    primary = Color(0xFF0B7F73), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6F2E8), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF3F7D76),
    background = Color(0xFFF4FFFC), onSurface = Color(0xFF0F2A2A),
    surfaceLow = Color(0xFFEAFBF7), surfaceMid = Color(0xFFE0F6F1),
    surfaceHigh = Color(0xFFD5EEE8), surfaceHighest = Color(0xFFCBE6E0),
    surfaceVariant = Color(0xFFD9EDE8), onSurfaceVariant = Color(0xFF3F5F5B),
    outline = Color(0xFF6F8F8B),
)

private val GoldScheme = buildScheme(
    dark = false,
    primary = Color(0xFF7A5F2B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0DDB0), onPrimaryContainer = Color(0xFF2A1D00),
    secondary = Color(0xFF8C7A55),
    background = Color(0xFFF7F2E8), onSurface = Color(0xFF2B2418),
    surfaceLow = Color(0xFFF2ECDD), surfaceMid = Color(0xFFECE4D1),
    surfaceHigh = Color(0xFFE4DAC3), surfaceHighest = Color(0xFFDBD0B6),
    surfaceVariant = Color(0xFFE8DFCB), onSurfaceVariant = Color(0xFF5E5340),
    outline = Color(0xFF8C8066),
)

private fun status(dark: Boolean, connected: Color, connecting: Color, onVariant: Color) =
    FoxyStatusColors(connected = connected, connecting = connecting, disconnected = onVariant)

/** مشخصات کامل یک پالت (ColorScheme + رنگ‌های برند + رنگ وضعیت اتصال). */
fun ThemePalette.spec(dark: Boolean): FoxThemeSpec = when (this) {
    ThemePalette.ORANGE_WHITE ->
        if (dark) FoxThemeSpec(DarkColors, OrangeDarkExtras, DarkStatusColors)
        else FoxThemeSpec(LightColors, OrangeLightExtras, LightStatusColors)

    ThemePalette.BLUE_VIOLET -> FoxThemeSpec(
        BlueScheme, BlueExtras,
        status(true, Color(0xFF5EE6A8), Color(0xFFFFC46B), Color(0xFFA4A9D6))
    )

    ThemePalette.BLACK_RED -> FoxThemeSpec(
        RedScheme, RedExtras,
        status(true, Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFB0B0B0))
    )

    ThemePalette.TURQUOISE_WHITE -> FoxThemeSpec(
        TealScheme, TealExtras,
        status(false, Color(0xFF2E7D32), Color(0xFFB26A00), Color(0xFF3F5F5B))
    )

    ThemePalette.GRANITE_GOLD -> FoxThemeSpec(
        GoldScheme, GoldExtras,
        status(false, Color(0xFF3B7A3F), Color(0xFF9A6A00), Color(0xFF5E5340))
    )
}

val LocalFoxExtras = staticCompositionLocalOf { OrangeLightExtras }
