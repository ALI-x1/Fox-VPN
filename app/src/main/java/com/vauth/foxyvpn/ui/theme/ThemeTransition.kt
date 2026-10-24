package com.vauth.foxyvpn.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView

/**
 * انیمیشن تغییر تم به روش «محو شدن عکس قبلی».
 *
 * روش قبلی هر فریم تمام رنگ‌های برنامه را lerp می‌کرد و کل UI را ~۳۰ بار recompose می‌کرد (لگ).
 * اینجا قبل از تغییر، یک عکس نیم‌اندازه از صفحه‌ی فعلی گرفته می‌شود، تم جدید فوراً اعمال می‌شود
 * و عکس قدیمی فقط با alpha/scale (کار GPU، بدون recompose) محو می‌شود؛ نتیجه: نرم، ارزان و یکدست
 * برای همه‌ی صفحه‌ها (خانه، تنظیمات، ...) و هر دو حالت روشن/تیره.
 */
object ThemeTransition {
    internal var snapshot by mutableStateOf<Bitmap?>(null)
    private var host: View? = null

    internal fun attach(v: View) { host = v }
    internal fun detach(v: View) { if (host === v) host = null }

    /** قبل از تغییر تم صدا زده می‌شود. هر خطایی بی‌صدا نادیده گرفته می‌شود (فقط انیمیشن حذف می‌شود). */
    fun capture() {
        val v = host ?: return
        val w = v.width
        val h = v.height
        if (w <= 0 || h <= 0 || !v.isAttachedToWindow) return
        runCatching {
            val scale = 0.5f
            val bmp = Bitmap.createBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.scale(scale, scale)
            v.draw(canvas)
            snapshot = bmp
        }
    }
}

private const val FADE_MS = 420

@Composable
internal fun ThemeTransitionHost(content: @Composable () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        ThemeTransition.attach(view)
        onDispose { ThemeTransition.detach(view) }
    }
    Box(Modifier.fillMaxSize()) {
        content()
        val snap = ThemeTransition.snapshot
        if (snap != null) {
            val progress = remember(snap) { Animatable(0f) }
            LaunchedEffect(snap) {
                progress.animateTo(1f, tween(FADE_MS, easing = FastOutSlowInEasing))
                if (ThemeTransition.snapshot === snap) ThemeTransition.snapshot = null
            }
            val img = remember(snap) { snap.asImageBitmap() }
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val p = progress.value
                        alpha = 1f - p
                        val s = 1f + 0.035f * p
                        scaleX = s; scaleY = s
                    },
            )
        }
    }
}
