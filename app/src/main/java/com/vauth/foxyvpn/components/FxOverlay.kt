package com.vauth.foxyvpn.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * افکت تمام‌صفحه‌ی ایموجی (بدون گرفتن لمس): fx = confetti | fireworks | hearts | snow.
 * confetti/hearts/snow می‌ریزن پایین، fireworks چند بار می‌ترکه. بعد از [durationMs] خودش تموم می‌شه و [onDone] صدا زده می‌شه.
 */
@Composable
fun FxOverlay(fx: String, modifier: Modifier = Modifier, durationMs: Int = 4200, onDone: () -> Unit = {}) {
    val emojis = remember(fx) {
        when (fx) {
            "fireworks" -> listOf("🎆", "🎇", "✨", "🎉")
            "hearts" -> listOf("💖", "💗", "💕", "❤️")
            "snow" -> listOf("❄️", "❄️", "✨", "☃️")
            else -> listOf("🎉", "🎊", "✨", "🎈")
        }
    }
    val burst = fx == "fireworks"
    val prog = remember(fx) { Animatable(0f) }
    LaunchedEffect(fx) {
        prog.animateTo(1f, tween(durationMs, easing = LinearEasing))
        onDone()
    }
    class P(val a: Float, val b: Float, val c: Float, val d: Float, val e: Int, val g: Float)
    val parts = remember(fx) {
        val r = Random(fx.hashCode() + 11)
        if (burst) List(84) { i -> P(i / 21, r.nextFloat() * 2f * PI.toFloat(), 0.45f + r.nextFloat() * 0.55f, 0.8f + r.nextFloat() * 0.6f, r.nextInt(emojis.size), r.nextFloat()) }
        else List(48) { P(r.nextFloat(), r.nextFloat() * 0.55f, 0.6f + r.nextFloat() * 0.8f, 0.6f + r.nextFloat() * 0.9f, r.nextInt(emojis.size), r.nextFloat() * 6f) }
    }
    val centers = remember(fx) { val r = Random(fx.hashCode() + 5); List(4) { Pair(0.15f + r.nextFloat() * 0.7f, 0.12f + r.nextFloat() * 0.38f) } }
    val paint = remember { Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER } }

    Canvas(modifier.fillMaxSize()) {
        val p = prog.value
        val w = size.width
        val h = size.height
        val base = w / 13f
        drawIntoCanvas { cv ->
            val nc = cv.nativeCanvas
            if (burst) {
                parts.forEach { pt ->
                    val bi = pt.a.toInt().coerceIn(0, 3)
                    val start = bi * 0.17f
                    val t = ((p - start) / 0.5f).coerceIn(0f, 1f)
                    if (t <= 0f || t >= 1f) return@forEach
                    val ease = 1f - (1f - t) * (1f - t)
                    val (cx, cy) = centers[bi]
                    val dist = w * 0.42f * pt.c * ease
                    val x = cx * w + cos(pt.b) * dist
                    val y = cy * h + sin(pt.b) * dist + h * 0.18f * t * t
                    paint.textSize = base * pt.d * (1f - 0.35f * t)
                    paint.alpha = ((1f - t * t) * 255).toInt().coerceIn(0, 255)
                    nc.drawText(emojis[pt.e], x, y, paint)
                }
            } else {
                parts.forEach { pt ->
                    val delay = pt.b
                    val t = ((p - delay * 0.5f) / (1f - delay * 0.5f)).coerceIn(0f, 1f)
                    if (t <= 0f || t >= 1f) return@forEach
                    val rise = fx == "hearts"
                    val y = if (rise) h * (1.05f - t * (0.5f + pt.c * 0.6f)) else -base + t * (h + 2 * base)
                    val x = (pt.a * w + sin(pt.g + t * 6f) * w * 0.05f * pt.d).coerceIn(0f, w)
                    paint.textSize = base * pt.c
                    paint.alpha = (if (t > 0.85f) (1f - t) / 0.15f * 255 else 255f).toInt().coerceIn(0, 255)
                    nc.drawText(emojis[pt.e], x, y, paint)
                }
            }
        }
    }
}
