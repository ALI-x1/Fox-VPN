package com.vauth.foxyvpn.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * تم‌هایی که هم حالت روشن دارن و هم تیره (از کلید لایت/دارک و گزینه‌ی «سیستم» پیروی می‌کنن).
 * برای هر تم فقط رنگ‌های برند (FoxExtras) رو دستی می‌نویسیم؛ ColorScheme متریال و رنگ‌های
 * وضعیت اتصال از روی همان‌ها ساخته می‌شن تا همه‌چیز هماهنگ بمونه.
 */
internal fun dualSpec(palette: ThemePalette, dark: Boolean): FoxThemeSpec {
    val e = dualExtras(palette, dark)
    return FoxThemeSpec(deriveScheme(dark, e), e, dualStatus(dark, e))
}

private fun dualStatus(dark: Boolean, e: FoxExtras) = if (dark) {
    FoxyStatusColors(
        connected = Color(0xFF4ADE80), connecting = Color(0xFFFBBF24), disconnected = e.textSecondary
    )
} else {
    FoxyStatusColors(
        connected = Color(0xFF2E7D32), connecting = Color(0xFFB26A00), disconnected = e.textSecondary
    )
}

private fun deriveScheme(dark: Boolean, e: FoxExtras): ColorScheme {
    val primary = if (dark) e.accentLight else e.accentText
    val onPrimary = if (dark) e.bgTop else Color.White
    val container = lerp(e.innerCard, e.accent, if (dark) 0.35f else 0.22f)
    return buildScheme(
        dark = dark,
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = e.textPrimary,
        secondary = if (dark) e.accentLight else e.accent,
        background = e.bgTop, onSurface = e.textPrimary,
        surfaceLow = lerp(e.bgTop, e.bgBottom, 0.5f),
        surfaceMid = e.innerCard,
        surfaceHigh = lerp(e.innerCard, e.accent, 0.08f),
        surfaceHighest = lerp(e.innerCard, e.accent, 0.14f),
        surfaceVariant = lerp(e.innerCard, e.accent, 0.10f),
        onSurfaceVariant = e.textSecondary,
        outline = lerp(e.textSecondary, e.bgTop, 0.35f),
    )
}

private fun x(
    bgTop: Long, bgBottom: Long, card: Long, border: Long, inner: Long,
    text: Long, textSec: Long, accent: Long, accentLight: Long, accentText: Long,
    upload: Long, download: Long, btnCenter: Long, btnEdge: Long,
) = FoxExtras(
    bgTop = Color(bgTop), bgBottom = Color(bgBottom), card = Color(card), cardBorder = Color(border),
    innerCard = Color(inner), textPrimary = Color(text), textSecondary = Color(textSec),
    accent = Color(accent), accentLight = Color(accentLight), accentText = Color(accentText),
    upload = Color(upload), download = Color(download),
    buttonFillCenter = Color(btnCenter), buttonFillEdge = Color(btnEdge),
)

private fun dualExtras(p: ThemePalette, dark: Boolean): FoxExtras = when (p) {
    ThemePalette.OCEAN_BLUE -> if (dark) {
        x(0xFF050F1A, 0xFF0A2236, 0xCC0F2A40, 0x4438BDF8, 0xFF133550,
            0xFFE6F4FC, 0xFF8FB0C6, 0xFF0EA5E9, 0xFF7DD3FC, 0xFF7DD3FC,
            0xFF38BDF8, 0xFF2DD4BF, 0xFF14506E, 0xFF0A2A3E)
    } else {
        x(0xFFF0F9FF, 0xFFD3EAF7, 0xB3FFFFFF, 0x330284C7, 0xFFE3F3FC,
            0xFF0B2536, 0xFF4B6B80, 0xFF0284C7, 0xFF38BDF8, 0xFF075985,
            0xFF0284C7, 0xFF14B8A6, 0xFFFFFFFF, 0xFFC5E4F5)
    }

    ThemePalette.LIME_FRESH -> if (dark) {
        x(0xFF0C1405, 0xFF18260A, 0xCC1E3010, 0x4484CC16, 0xFF263B15,
            0xFFF0F9E0, 0xFFA6B88A, 0xFF84CC16, 0xFFBEF264, 0xFFBEF264,
            0xFFA3E635, 0xFF38BDF8, 0xFF3A5A12, 0xFF1B2D0A)
    } else {
        x(0xFFF9FDF0, 0xFFE8F4CC, 0xB3FFFFFF, 0x4084CC16, 0xFFF1F9DD,
            0xFF1A2A0A, 0xFF5A6B45, 0xFF84CC16, 0xFFBEF264, 0xFF4D7C0F,
            0xFF65A30D, 0xFF0EA5E9, 0xFFFFFFFF, 0xFFDDEFB5)
    }

    ThemePalette.FUCHSIA_POP -> if (dark) {
        x(0xFF14061A, 0xFF2A0E33, 0xCC32123D, 0x44E879F9, 0xFF3E184B,
            0xFFFDEBFF, 0xFFC9A3D0, 0xFFD946EF, 0xFFF0ABFC, 0xFFF0ABFC,
            0xFFE879F9, 0xFF818CF8, 0xFF5A1F6B, 0xFF2E0F38)
    } else {
        x(0xFFFEF5FF, 0xFFF8DDFA, 0xB3FFFFFF, 0x33D946EF, 0xFFFCEBFE,
            0xFF2D0F33, 0xFF7A5A80, 0xFFD946EF, 0xFFF0ABFC, 0xFFA21CAF,
            0xFFD946EF, 0xFF6366F1, 0xFFFFFFFF, 0xFFF5CDF9)
    }

    ThemePalette.CORAL_SUNSET -> if (dark) {
        x(0xFF1A0B07, 0xFF30150E, 0xCC3A1B13, 0x44FF8F73, 0xFF46231A,
            0xFFFFEEE9, 0xFFD0A79B, 0xFFFF6B4A, 0xFFFFA48F, 0xFFFFA48F,
            0xFFFF8F73, 0xFF38BDF8, 0xFF6A2F20, 0xFF361810)
    } else {
        x(0xFFFFF7F4, 0xFFFFE0D6, 0xB3FFFFFF, 0x33FF6B4A, 0xFFFFEDE7,
            0xFF2E150E, 0xFF80574B, 0xFFFF6B4A, 0xFFFFA48F, 0xFFC2371A,
            0xFFFF6B4A, 0xFF0EA5E9, 0xFFFFFFFF, 0xFFFFD2C6)
    }

    // GRAPHITE_MONO (و هر مقدار دیگر که اینجا نیاید)
    else -> if (dark) {
        x(0xFF0C0E11, 0xFF191D22, 0xCC1F242A, 0x4494A3B8, 0xFF272D34,
            0xFFEEF1F4, 0xFF9AA5B1, 0xFF94A3B8, 0xFFCBD5E1, 0xFFCBD5E1,
            0xFF94A3B8, 0xFF38BDF8, 0xFF38404A, 0xFF1C2127)
    } else {
        x(0xFFF8F9FA, 0xFFE4E7EB, 0xB3FFFFFF, 0x33475569, 0xFFF1F3F5,
            0xFF111418, 0xFF5F6B7A, 0xFF475569, 0xFF94A3B8, 0xFF1E293B,
            0xFF475569, 0xFF0EA5E9, 0xFFFFFFFF, 0xFFD8DDE3)
    }
}
