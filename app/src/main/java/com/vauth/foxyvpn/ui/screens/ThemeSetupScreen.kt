package com.vauth.foxyvpn.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView

/**
 * انیمیشن تغییر تم به روش «دایره‌ی باز‌شونده» (circular reveal).
 *
 * روش قبلی هر فریم تمام رنگ‌های برنامه را lerp می‌کرد و کل UI را ~۳۰ بار recompose می‌کرد (لگ).
 * اینجا قبل از تغییر، یک عکس نیم‌اندازه از صفحه‌ی فعلی گرفته می‌شود، تم جدید فوراً اعمال می‌شود
 * و عکس قدیمی فقط با alpha/scale (کار GPU، بدون recompose) محو می‌شود؛ نتیجه: نرم، ارزان و یکدست
 * برای همه‌ی صفحه‌ها (خانه، تنظیمات، ...) و هر دو حالت روشن/تیره.
 */
object ThemeTransition {
    internal var snapshot by mutableStateOf<Bitmap?>(null)
    private var host: View? = null
    /** آخرین نقطه‌ی لمس (px، مختصات همین view)؛ دایره‌ی تغییر تم از همین‌جا باز می‌شه. */
    internal var origin: Offset? = null

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

private const val REVEAL_MS = 560
private val RevealEasing = CubicBezierEasing(0.33f, 0f, 0.15f, 1f)

@Composable
internal fun ThemeTransitionHost(content: @Composable () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        ThemeTransition.attach(view)
        onDispose { ThemeTransition.detach(view) }
    }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    e.changes.firstOrNull()?.let { ThemeTransition.origin = it.position }
                }
            }
        },
    ) {
        content()
        val snap = ThemeTransition.snapshot
        if (snap != null) {
            val progress = remember(snap) { Animatable(0f) }
            LaunchedEffect(snap) {
                withFrameNanos { } // اجازه بده فریمِ اول با تم جدید زیر عکس ساخته بشه (بدون پرش)
                progress.animateTo(1f, tween(REVEAL_MS, easing = RevealEasing))
                if (ThemeTransition.snapshot === snap) ThemeTransition.snapshot = null
            }
            val img = remember(snap) { snap.asImageBitmap() }
            Canvas(Modifier.matchParentSize()) {
                val o = ThemeTransition.origin ?: Offset(size.width / 2f, size.height / 2f)
                val maxR = maxOf(
                    hypot(o.x, o.y), hypot(size.width - o.x, o.y),
                    hypot(o.x, size.height - o.y), hypot(size.width - o.x, size.height - o.y),
                )
                val r = maxR * progress.value
                val hole = Path().apply { addOval(androidx.compose.ui.geometry.Rect(o, r)) }
                clipPath(hole, ClipOp.Difference) {
                    drawImage(img, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
            }
        }
    }
}
