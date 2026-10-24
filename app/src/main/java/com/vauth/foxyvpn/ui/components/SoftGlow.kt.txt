package com.vauth.foxyvpn.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * جایگزین سبک و بدون‌ایراد برای `.shadow()` روی شکل‌های نیمه‌شفاف.
 * سایه‌ی سیستمی از زیر پرکننده‌ی شفاف دیده می‌شد (همان «پنج‌ضلعی» پشت لوگو) و هر فریم هزینه داشت.
 * اینجا فقط یک گرادیان شعاعی بیرون از شکل کشیده می‌شود؛ پشت شکل چیزی نیست.
 */
fun Modifier.softGlow(color: Color, spread: Float = 18f, strength: Float = 0.35f): Modifier =
    drawBehind {
        val r = size.minDimension / 2f + spread * density
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.Transparent,
                (size.minDimension / 2f * 0.9f / r).coerceIn(0f, 0.95f) to color.copy(alpha = strength),
                1.0f to Color.Transparent,
                center = center, radius = r,
            ),
            radius = r, center = center,
        )
    }

/** هاله‌ی نرم زیر کارت/دکمه‌ی مستطیلی؛ چند لایه‌ی خیلی کم‌رنگ، بدون سایه‌ی سیستمی. */
fun Modifier.softGlowRect(color: Color, corner: Float = 20f, spread: Float = 10f, strength: Float = 0.10f): Modifier =
    drawBehind {
        val d = density
        for (i in 1..3) {
            val s = spread * d * i / 3f
            drawRoundRect(
                color = color.copy(alpha = strength / i),
                topLeft = Offset(-s, -s + 3f * d),
                size = androidx.compose.ui.geometry.Size(size.width + 2 * s, size.height + 2 * s),
                cornerRadius = CornerRadius((corner * d) + s),
            )
        }
    }
