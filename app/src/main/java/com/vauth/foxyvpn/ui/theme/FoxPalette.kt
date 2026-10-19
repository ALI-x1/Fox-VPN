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
    // فقط برای تم‌هایی که هم روشن و هم تیره دارن (fixedDark == null): پیش‌نمایش حالت تیره
    val darkPreviewBg: Color? = null,
    val darkPreviewText: Color? = null,
    val darkPreviewSub: Color? = null,
) {
    ORANGE_WHITE(
        "نارنجی و سفید", "روشن و مینیمال", null,
        Color(0xFFF59E0B), Color(0xFFFFF8EE), Color(0xFF2B1D10), Color(0xFF8A7560),
        Color(0xFF241B12), Color(0xFFF5EEE6), Color(0xFFB8AB9C)
    ),
    OCEAN_BLUE(
        "آبی اقیانوسی", "عمیق و آرام", null,
        Color(0xFF0EA5E9), Color(0xFFF0F9FF), Color(0xFF0B2536), Color(0xFF4B6B80),
        Color(0xFF0A2236), Color(0xFFE6F4FC), Color(0xFF8FB0C6)
    ),
    LIME_FRESH(
        "لیمویی تازه", "شاداب و انرژی‌بخش", null,
        Color(0xFF84CC16), Color(0xFFF9FDF0), Color(0xFF1A2A0A), Color(0xFF5A6B45),
        Color(0xFF18260A), Color(0xFFF0F9E0), Color(0xFFA6B88A)
    ),
    FUCHSIA_POP(
        "سرخابی", "پرانرژی و مدرن", null,
        Color(0xFFD946EF), Color(0xFFFEF5FF), Color(0xFF2D0F33), Color(0xFF7A5A80),
        Color(0xFF2A0E33), Color(0xFFFDEBFF), Color(0xFFC9A3D0)
    ),
    CORAL_SUNSET(
        "مرجانی غروب", "گرم و پرانرژی", null,
        Color(0xFFFF6B4A), Color(0xFFFFF7F4), Color(0xFF2E150E), Color(0xFF80574B),
        Color(0xFF30150E), Color(0xFFFFEEE9), Color(0xFFD0A79B)
    ),
    GRAPHITE_MONO(
        "گرافیتی", "ساده و جدی", null,
        Color(0xFF64748B), Color(0xFFF8F9FA), Color(0xFF111418), Color(0xFF5F6B7A),
        Color(0xFF191D22), Color(0xFFEEF1F4), Color(0xFF9AA5B1)
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
    ),
    ROSE_WHITE(
        "صورتی و سفید", "ملایم و شیک", false,
        Color(0xFFE8548F), Color(0xFFFFF0F6), Color(0xFF2A1420), Color(0xFF8A6475)
    ),
    FOREST_GREEN(
        "سبز جنگلی", "طبیعی و آرام", true,
        Color(0xFF34D399), Color(0xFF0F2418), Color(0xFFFFFFFF), Color(0xFF8CC7A8)
    ),
    ORCHID_PURPLE(
        "بنفش ارکیده", "شبانه و رویایی", true,
        Color(0xFFA855F7), Color(0xFF261340), Color(0xFFFFFFFF), Color(0xFFC4A9EE)
    ),
    SKY_BLUE(
        "آبی آسمانی", "روشن و شفاف", false,
        Color(0xFF3B82F6), Color(0xFFE8F2FE), Color(0xFF0F2038), Color(0xFF5F7A99)
    ),
    CYBER_CYAN(
        "نئون فیروزه‌ای", "سایبر و درخشان", true,
        Color(0xFF22D3EE), Color(0xFF08151F), Color(0xFFFFFFFF), Color(0xFF7FC4D6)
    ),
    LIQUID_GLASS(
        "شیشه‌ی مایع", "سفید و شفاف", false,
        Color(0xFF1F2937), Color(0xFFF4F7FB), Color(0xFF111827), Color(0xFF6C7A8C)
    ),
    MOCHA_LATTE(
        "موکا و لاته", "گرم و دنج", true,
        Color(0xFFC8925A), Color(0xFF2A1D16), Color(0xFFFFFFFF), Color(0xFFC9B39A)
    );

    /** آیا این تم هم روشن و هم تیره داره (و کلید لایت/دارک روش اثر می‌ذاره)؟ */
    val supportsLightDark: Boolean get() = fixedDark == null

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

private val RoseExtras = FoxExtras(
    bgTop = Color(0xFFFFFAFC), bgBottom = Color(0xFFFDE7F0),
    card = Color(0xB3FFFFFF), cardBorder = Color(0x33E8548F), innerCard = Color(0xFFFFF0F6),
    textPrimary = Color(0xFF2A1420), textSecondary = Color(0xFF7A5A68),
    accent = Color(0xFFE8548F), accentLight = Color(0xFFFF8FB8), accentText = Color(0xFFB8275F),
    upload = Color(0xFFE8548F), download = Color(0xFF7C9CF5),
    buttonFillCenter = Color(0xFFFFFFFF), buttonFillEdge = Color(0xFFFBD3E3),
)

private val ForestExtras = FoxExtras(
    bgTop = Color(0xFF07140E), bgBottom = Color(0xFF0F2418),
    card = Color(0xCC12281C), cardBorder = Color(0x4434D399), innerCard = Color(0xFF173325),
    textPrimary = Color(0xFFE8F6EE), textSecondary = Color(0xFF9DBBAA),
    accent = Color(0xFF34D399), accentLight = Color(0xFF86EFAC), accentText = Color(0xFF6EE7B7),
    upload = Color(0xFF34D399), download = Color(0xFF60A5FA),
    buttonFillCenter = Color(0xFF1F4A35), buttonFillEdge = Color(0xFF0F2A1D),
)

private val OrchidExtras = FoxExtras(
    bgTop = Color(0xFF140B22), bgBottom = Color(0xFF261340),
    card = Color(0xCC2A1745), cardBorder = Color(0x44C084FC), innerCard = Color(0xFF33205A),
    textPrimary = Color(0xFFF5ECFF), textSecondary = Color(0xFFB9A6D6),
    accent = Color(0xFFA855F7), accentLight = Color(0xFFD8B4FE), accentText = Color(0xFFD8B4FE),
    upload = Color(0xFFC084FC), download = Color(0xFF67E8F9),
    buttonFillCenter = Color(0xFF4A2A7A), buttonFillEdge = Color(0xFF2A1647),
)

private val SkyExtras = FoxExtras(
    bgTop = Color(0xFFF5FAFF), bgBottom = Color(0xFFDCEBFB),
    card = Color(0xB3FFFFFF), cardBorder = Color(0x333B82F6), innerCard = Color(0xFFE8F2FE),
    textPrimary = Color(0xFF0F2038), textSecondary = Color(0xFF55708F),
    accent = Color(0xFF3B82F6), accentLight = Color(0xFF7DB3FF), accentText = Color(0xFF1D4ED8),
    upload = Color(0xFF3B82F6), download = Color(0xFFF59E0B),
    buttonFillCenter = Color(0xFFFFFFFF), buttonFillEdge = Color(0xFFCFE3FB),
)

private val CyberExtras = FoxExtras(
    bgTop = Color(0xFF03070D), bgBottom = Color(0xFF08151F),
    card = Color(0xCC0E1C28), cardBorder = Color(0x4422D3EE), innerCard = Color(0xFF122736),
    textPrimary = Color(0xFFE6FAFF), textSecondary = Color(0xFF8FB3C2),
    accent = Color(0xFF22D3EE), accentLight = Color(0xFF7DEBFF), accentText = Color(0xFF67E8F9),
    upload = Color(0xFF22D3EE), download = Color(0xFFA3E635),
    buttonFillCenter = Color(0xFF0F3A4D), buttonFillEdge = Color(0xFF07202C),
)

// شیشه‌ی مایع: پس‌زمینه‌ی سفیدِ مایل به آبی‌خاکستری، کارت‌های نیمه‌شفاف مثل شیشه‌ی مات،
// حاشیه‌ی بلورین و اکسنت گرافیتی؛ بدون رنگ تند تا همه‌چیز سبک و تمیز بمونه.
private val GlassExtras = FoxExtras(
    bgTop = Color(0xFFF9FBFE), bgBottom = Color(0xFFE2E9F4),
    card = Color(0x99FFFFFF), cardBorder = Color(0x40A0AEC0), innerCard = Color(0xCCFFFFFF),
    textPrimary = Color(0xFF111827), textSecondary = Color(0xFF66758A),
    accent = Color(0xFF1F2937), accentLight = Color(0xFF6B7A90), accentText = Color(0xFF111827),
    upload = Color(0xFF4B5563), download = Color(0xFF0EA5E9),
    buttonFillCenter = Color(0xFFFFFFFF), buttonFillEdge = Color(0xFFDDE5F1),
)

private val MochaExtras = FoxExtras(
    bgTop = Color(0xFF17110D), bgBottom = Color(0xFF2A1D16),
    card = Color(0xCC33251C), cardBorder = Color(0x44D4A373), innerCard = Color(0xFF3E2D22),
    textPrimary = Color(0xFFF3E9DC), textSecondary = Color(0xFFB9A590),
    accent = Color(0xFFC8925A), accentLight = Color(0xFFF0D2A8), accentText = Color(0xFFE8C79A),
    upload = Color(0xFFC8925A), download = Color(0xFF8FB8A8),
    buttonFillCenter = Color(0xFF5A4130), buttonFillEdge = Color(0xFF2E2018),
)

internal fun buildScheme(
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

private val RoseScheme = buildScheme(
    dark = false,
    primary = Color(0xFFB8275F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E6), onPrimaryContainer = Color(0xFF3E0020),
    secondary = Color(0xFF9C4A68),
    background = Color(0xFFFFFAFC), onSurface = Color(0xFF2A1420),
    surfaceLow = Color(0xFFFFF3F8), surfaceMid = Color(0xFFFCEAF2),
    surfaceHigh = Color(0xFFF7E0EB), surfaceHighest = Color(0xFFF1D6E3),
    surfaceVariant = Color(0xFFF6DFEA), onSurfaceVariant = Color(0xFF6E4B5B),
    outline = Color(0xFF9C7A89),
)

private val ForestScheme = buildScheme(
    dark = true,
    primary = Color(0xFF5EE0AB), onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F5C3A), onPrimaryContainer = Color(0xFFC6F7DE),
    secondary = Color(0xFF8FD6B3),
    background = Color(0xFF07140E), onSurface = Color(0xFFE8F6EE),
    surfaceLow = Color(0xFF0C1B13), surfaceMid = Color(0xFF12281C),
    surfaceHigh = Color(0xFF173325), surfaceHighest = Color(0xFF1D3E2D),
    surfaceVariant = Color(0xFF1A3526), onSurfaceVariant = Color(0xFF9DBBAA),
    outline = Color(0xFF5E8A73),
)

private val OrchidScheme = buildScheme(
    dark = true,
    primary = Color(0xFFC79BFF), onPrimary = Color(0xFF2B0057),
    primaryContainer = Color(0xFF5B21A6), onPrimaryContainer = Color(0xFFEBD9FF),
    secondary = Color(0xFFE0A6FF),
    background = Color(0xFF140B22), onSurface = Color(0xFFF5ECFF),
    surfaceLow = Color(0xFF1B1030), surfaceMid = Color(0xFF241540),
    surfaceHigh = Color(0xFF2E1A52), surfaceHighest = Color(0xFF382163),
    surfaceVariant = Color(0xFF33205A), onSurfaceVariant = Color(0xFFB9A6D6),
    outline = Color(0xFF8A74B0),
)

private val SkyScheme = buildScheme(
    dark = false,
    primary = Color(0xFF1D4ED8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E6FF), onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF4F6A8F),
    background = Color(0xFFF5FAFF), onSurface = Color(0xFF0F2038),
    surfaceLow = Color(0xFFEEF5FD), surfaceMid = Color(0xFFE5EFFB),
    surfaceHigh = Color(0xFFDBE8F8), surfaceHighest = Color(0xFFD0E0F4),
    surfaceVariant = Color(0xFFDDE8F6), onSurfaceVariant = Color(0xFF4A6280),
    outline = Color(0xFF7A91AD),
)

private val CyberScheme = buildScheme(
    dark = true,
    primary = Color(0xFF4EE1F5), onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF00576A), onPrimaryContainer = Color(0xFFB8F1FF),
    secondary = Color(0xFF7FD8E8),
    background = Color(0xFF03070D), onSurface = Color(0xFFE6FAFF),
    surfaceLow = Color(0xFF08121A), surfaceMid = Color(0xFF0C1B26),
    surfaceHigh = Color(0xFF112634), surfaceHighest = Color(0xFF173243),
    surfaceVariant = Color(0xFF12293A), onSurfaceVariant = Color(0xFF8FB3C2),
    outline = Color(0xFF5A8394),
)

private val GlassScheme = buildScheme(
    dark = false,
    primary = Color(0xFF1F2937), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E9F3), onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF5B6B80),
    background = Color(0xFFF7F9FC), onSurface = Color(0xFF111827),
    surfaceLow = Color(0xFFF2F5FA), surfaceMid = Color(0xFFECF0F7),
    surfaceHigh = Color(0xFFE5EBF4), surfaceHighest = Color(0xFFDEE5F0),
    surfaceVariant = Color(0xFFE8EDF5), onSurfaceVariant = Color(0xFF4B5B70),
    outline = Color(0xFF8593A6),
)

private val MochaScheme = buildScheme(
    dark = true,
    primary = Color(0xFFE0AE7C), onPrimary = Color(0xFF3A2410),
    primaryContainer = Color(0xFF6B4524), onPrimaryContainer = Color(0xFFF6DFC4),
    secondary = Color(0xFFC9B79F),
    background = Color(0xFF17110D), onSurface = Color(0xFFF3E9DC),
    surfaceLow = Color(0xFF1D1611), surfaceMid = Color(0xFF261C15),
    surfaceHigh = Color(0xFF30241B), surfaceHighest = Color(0xFF3A2C21),
    surfaceVariant = Color(0xFF33261D), onSurfaceVariant = Color(0xFFB9A590),
    outline = Color(0xFF8A7561),
)

private fun status(dark: Boolean, connected: Color, connecting: Color, onVariant: Color) =
    FoxyStatusColors(connected = connected, connecting = connecting, disconnected = onVariant)

/** مشخصات کامل یک پالت (ColorScheme + رنگ‌های برند + رنگ وضعیت اتصال). */
fun ThemePalette.spec(dark: Boolean): FoxThemeSpec = when (this) {
    ThemePalette.ORANGE_WHITE ->
        if (dark) FoxThemeSpec(DarkColors, OrangeDarkExtras, DarkStatusColors)
        else FoxThemeSpec(LightColors, OrangeLightExtras, LightStatusColors)

    ThemePalette.OCEAN_BLUE,
    ThemePalette.LIME_FRESH,
    ThemePalette.FUCHSIA_POP,
    ThemePalette.CORAL_SUNSET,
    ThemePalette.GRAPHITE_MONO -> dualSpec(this, dark)

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

    ThemePalette.ROSE_WHITE -> FoxThemeSpec(
        RoseScheme, RoseExtras,
        status(false, Color(0xFF2E7D32), Color(0xFFB26A00), Color(0xFF6E4B5B))
    )

    ThemePalette.FOREST_GREEN -> FoxThemeSpec(
        ForestScheme, ForestExtras,
        status(true, Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFF9DBBAA))
    )

    ThemePalette.ORCHID_PURPLE -> FoxThemeSpec(
        OrchidScheme, OrchidExtras,
        status(true, Color(0xFF5EE6A8), Color(0xFFFFC46B), Color(0xFFB9A6D6))
    )

    ThemePalette.SKY_BLUE -> FoxThemeSpec(
        SkyScheme, SkyExtras,
        status(false, Color(0xFF2E7D32), Color(0xFFB26A00), Color(0xFF4A6280))
    )

    ThemePalette.CYBER_CYAN -> FoxThemeSpec(
        CyberScheme, CyberExtras,
        status(true, Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFF8FB3C2))
    )

    ThemePalette.LIQUID_GLASS -> FoxThemeSpec(
        GlassScheme, GlassExtras,
        status(false, Color(0xFF2E7D32), Color(0xFFB26A00), Color(0xFF4B5B70))
    )

    ThemePalette.MOCHA_LATTE -> FoxThemeSpec(
        MochaScheme, MochaExtras,
        status(true, Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFB9A590))
    )
}

val LocalFoxExtras = staticCompositionLocalOf { OrangeLightExtras }
