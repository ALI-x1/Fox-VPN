package com.vauth.foxyvpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas as GCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * افکت تمام‌صفحه‌ی جدید (بدون ایموجی؛ همه‌چیز با شکل‌های برداری کشیده می‌شه و بدون گرفتن لمس):
 *  confetti  = دو توپِ کاغذرنگی از گوشه‌های پایین (برگه‌های سه‌بعدیِ چرخان با رنگ تم) + بارانِ آرام
 *  snow      = دانه‌برف‌های شش‌پر در سه عمق + گلوله‌های برفیِ نرم
 *  fireworks = موشک‌هایی که بالا می‌رن و با جرقه‌های دنباله‌دار می‌ترکن
 *  hearts    = قلب‌هایی که آروم بالا می‌رن
 * [durationMs]: -1 = مدت پیش‌فرضِ هر افکت، 0 = تکرار تا وقتی صفحه بازه، >0 = همون مدت. بعدش [onDone].
 * فقط مرحله‌ی draw دوباره اجرا می‌شه (بدون recompose در هر فریم).
 */
@Composable
fun FxOverlay(fx: String, modifier: Modifier = Modifier, durationMs: Int = -1, onDone: () -> Unit = {}) {
    val c = LocalFoxExtras.current
    val kind = when (fx) { "fireworks", "hearts", "snow" -> fx; else -> "confetti" }
    val total = if (durationMs >= 0) durationMs else when (kind) { "snow" -> 9000; "hearts" -> 7000; "fireworks" -> 7500; else -> 5200 }
    // تم روشن: رنگ «سفید» پالتِ کاغذرنگی تیره می‌شه؛ برف همیشه سفیده (فقط یه هاله‌ی آبیِ خیلی ملایم دورش می‌گیره تا روی زمینه‌ی روشن گم نشه)
    val light = remember(c) { c.bgTop.luminance() > 0.55f }
    val density = LocalDensity.current.density
    val palette = remember(c, light) {
        listOf(c.accent, c.accentLight, Color(0xFFFFD166), Color(0xFF06D6A0), Color(0xFFEF476F), Color(0xFF4CC9F0), if (light) Color(0xFF455A64) else Color.White)
    }
    var tSec by remember(kind) { mutableStateOf(0f) }
    val done by rememberUpdatedState(onDone)
    // موتور (اسپرایت‌ها و فهرست ذره‌ها) یک‌بار و قبل از اولین فریز ساخته می‌شه
    val engine = remember(kind, palette, light, density) {
        when (kind) {
            "snow" -> SnowFx(light, density)
            "hearts" -> HeartsFx(palette)
            "fireworks" -> FireworksFx(palette)
            else -> ConfettiFx(palette)
        }
    }
    LaunchedEffect(kind, total) {
        // کمی صبر: انیمیشن ورودِ صفحه تموم بشه و بعد افکت شروع بشه (لگِ اول همین‌جا از بین می‌ره)
        delay(160)
        var last = withFrameNanos { it }
        var e = 0f
        while (true) {
            val now = withFrameNanos { it }
            // اگه یه فریم طولانی شد (لگ)، زمان یک‌دفعه نمی‌پره؛ حداکثر ۱/۳۰ ثانیه جلو می‌ره تا حرکت پیوسته بمونه
            e += ((now - last) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            last = now
            tSec = e
            if (total > 0 && e * 1000f >= total) { done(); break }
        }
    }
    Canvas(
        modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.ModulateAlpha // بدون بافر آف‌اسکرین؛ فقط شفافیت ضرب می‌شه
            val e = tSec
            val x = (e / 0.7f).coerceIn(0f, 1f)
            val fin = x * x * (3f - 2f * x) // ورود نرم (smoothstep)
            val y = if (total > 0) ((total / 1000f - e) / 1.1f).coerceIn(0f, 1f) else 1f
            alpha = fin * (y * y * (3f - 2f * y))
        },
    ) { engine.draw(this, tSec) }
}

private interface FxEngine { fun draw(s: DrawScope, t: Float) }

private fun fmod(a: Float, b: Float): Float = a - b * floor(a / b)
private fun easeOut(x: Float): Float { val v = (1f - x.coerceIn(0f, 1f)); return 1f - v * v * v }

// ───────────────────────── کاغذرنگی ─────────────────────────

private class ConfettiFx(private val palette: List<Color>) : FxEngine {
    private class Cannon(
        val side: Int, val wave: Float, val ang: Float, val spd: Float, val size: Float, val color: Int, val shape: Int,
        val flip: Float, val rot0: Float, val rotSp: Float, val sway: Float, val phase: Float, val vt: Float,
    )
    private class Drizzle(val x: Float, val delay: Float, val period: Float, val size: Float, val color: Int, val shape: Int, val flip: Float, val phase: Float, val rot0: Float)

    private val cycle = 4.4f
    private val life = 3.6f
    private val cannons: List<Cannon>
    private val drizzle: List<Drizzle>

    init {
        val r = Random(21)
        val list = ArrayList<Cannon>()
        for (side in 0..1) for (w in 0..1) repeat(20) {
            list += Cannon(
                side, if (w == 0) 0f else 0.5f,
                (48f + r.nextFloat() * 38f) * (PI.toFloat() / 180f), 1.0f + r.nextFloat() * 1.35f,
                5f + r.nextFloat() * 6f, r.nextInt(palette.size), r.nextInt(3),
                4f + r.nextFloat() * 8f, r.nextFloat() * 360f, (r.nextFloat() - 0.5f) * 520f,
                0.5f + r.nextFloat() * 1.2f, r.nextFloat() * 6.28f, 0.13f + r.nextFloat() * 0.16f,
            )
        }
        cannons = list
        drizzle = List(22) {
            Drizzle(r.nextFloat(), r.nextFloat() * 5f, 3.6f + r.nextFloat() * 2f, 4.5f + r.nextFloat() * 5f, r.nextInt(palette.size), r.nextInt(3), 3f + r.nextFloat() * 5f, r.nextFloat() * 6.28f, r.nextFloat() * 360f)
        }
    }

    private fun DrawScope.piece(x: Float, y: Float, sizePx: Float, shape: Int, color: Color, flipArg: Float, rotDeg: Float) {
        val fl = cos(flipArg)
        val sy = if (abs(fl) < 0.08f) 0.08f else fl
        val shade = 0.62f + 0.38f * abs(fl)
        val col = color.copy(alpha = color.alpha * shade)
        val pivot = Offset(x, y)
        when (shape) {
            1 -> drawCircle(col, sizePx * 0.42f, pivot)
            else -> withTransform({ rotate(rotDeg, pivot); scale(1f, sy, pivot) }) {
                if (shape == 2) drawRect(col, Offset(x - sizePx * 0.16f, y - sizePx * 1.05f), Size(sizePx * 0.32f, sizePx * 2.1f))
                else drawRect(col, Offset(x - sizePx * 0.7f, y - sizePx * 0.35f), Size(sizePx * 1.4f, sizePx * 0.7f))
            }
        }
    }

    override fun draw(s: DrawScope, t: Float) = with(s) {
        val w = size.width; val h = size.height
        val dp = density
        val tc = fmod(t, cycle)
        val k = 1.9f
        for (p in cannons) {
            val tau = tc - p.wave
            if (tau < 0f || tau > life) continue
            val u = (1f - exp(-k * tau)) / k
            val dirx = if (p.side == 0) cos(p.ang) else -cos(p.ang)
            val diry = -sin(p.ang)
            val x0 = if (p.side == 0) -0.02f * w else 1.02f * w
            val y0 = 1.03f * h
            val x = x0 + dirx * p.spd * h * u + sin(p.phase + tau * 2.6f) * 0.018f * w * (tau / (tau + 0.6f))
            val y = y0 + diry * p.spd * h * u + p.vt * h * (tau - u)
            if (y > h + 40f || x < -40f || x > w + 40f) continue
            val a = (1f - (tau - (life - 0.8f)) / 0.8f).coerceIn(0f, 1f)
            piece(x, y, p.size * dp, p.shape, palette[p.color].copy(alpha = a), p.flip * tau + p.phase, p.rot0 + p.rotSp * tau)
        }
        for (d in drizzle) {
            val tau = fmod(t + d.delay, d.period)
            val pr = tau / d.period
            val y = -0.05f * h + pr * 1.15f * h
            val x = d.x * w + sin(d.phase + tau * 2.2f) * 0.04f * w
            val a = if (pr < 0.08f) pr / 0.08f else if (pr > 0.9f) (1f - pr) / 0.1f else 1f
            piece(x, y, d.size * dp, d.shape, palette[d.color].copy(alpha = 0.95f * a), d.flip * tau + d.phase, d.rot0 + tau * 90f)
        }
    }
}

// ───────────────────────── برف ─────────────────────────

private class SnowFx(private val light: Boolean, density: Float) : FxEngine {
    private class Flake(val x: Float, val y: Float, val z: Int, val f: Float, val ph: Float, val rs: Float, val amp: Float)
    private class Ball(val x: Float, val y: Float, val r: Float, val sp: Float, val ph: Float)
    private val flakes: List<Flake>
    private val balls: List<Ball>
    private val halo = Color(0xFFCFDDEB) // هاله‌ی آبیِ خیلی روشن (فقط تم روشن)
    // دانه‌برف‌ها فقط یک‌بار روی بیت‌مپ کشیده می‌شن؛ هر فریم فقط یک drawImage (به‌جای ده‌ها drawLine)
    private val dot: ImageBitmap = sprite(2.2f * density, false)
    private val mid: ImageBitmap = sprite(4.5f * density, true)
    private val big: ImageBitmap = sprite(8f * density, true)

    private fun sprite(r: Float, star: Boolean): ImageBitmap {
        val pad = r * 0.5f + 5f
        val side = ((r + pad) * 2f).toInt().coerceAtLeast(10)
        val bmp = ImageBitmap(side, side)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GCanvas(bmp), Size(side.toFloat(), side.toFloat())) {
            val c = Offset(side / 2f, side / 2f)
            val sw = (r * 0.22f).coerceAtLeast(1.4f)
            fun paint(col: Color, extra: Float) {
                if (!star) { drawCircle(col, r + extra / 2f, c); return }
                for (i in 0..2) {
                    val a = i * (PI.toFloat() / 3f)
                    val dx = cos(a) * r; val dy = sin(a) * r
                    drawLine(col, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), sw + extra, StrokeCap.Round)
                    for (sgn in intArrayOf(-1, 1)) {
                        val bx = c.x + dx * 0.6f * sgn; val by = c.y + dy * 0.6f * sgn
                        val ba = a + PI.toFloat() / 2f
                        val ex = cos(ba) * r * 0.22f; val ey = sin(ba) * r * 0.22f
                        drawLine(col, Offset(bx - ex, by - ey), Offset(bx + ex, by + ey), sw * 0.8f + extra, StrokeCap.Round)
                    }
                }
                drawCircle(col, sw * 0.9f + extra / 2f, c)
            }
            if (light) paint(halo, if (star) sw * 1.6f else 3f)
            paint(Color.White, 0f)
        }
        return bmp
    }

    init {
        val r = Random(5)
        flakes = List(70) { i -> val z = if (i < 34) 0 else if (i < 54) 1 else 2; Flake(r.nextFloat(), r.nextFloat(), z, 0.5f + r.nextFloat(), r.nextFloat() * 6.28f, (r.nextFloat() - 0.5f) * 40f, 10f + r.nextFloat() * 22f) }
        balls = List(6) { Ball(r.nextFloat(), r.nextFloat(), 16f + r.nextFloat() * 14f, 0.16f + r.nextFloat() * 0.1f, r.nextFloat() * 6.28f) }
    }

    override fun draw(s: DrawScope, t: Float) = with(s) {
        val w = size.width; val h = size.height
        val dp = density
        val cyc = h * 1.12f
        for (f in flakes) {
            val sp = when (f.z) { 0 -> 0.055f; 1 -> 0.095f; else -> 0.15f }
            val al = when (f.z) { 0 -> 0.6f; 1 -> 0.82f; else -> 0.96f }
            val y = fmod(f.y * cyc + t * sp * h, cyc) - 0.06f * h
            val x = f.x * w + sin(t * 0.8f * f.f + f.ph) * f.amp * dp
            val img = when (f.z) { 0 -> dot; 1 -> mid; else -> big }
            val tl = Offset(x - img.width / 2f, y - img.height / 2f)
            if (f.z == 0) drawImage(img, tl, alpha = al)
            else withTransform({ rotate(f.ph * 57f + t * f.rs, Offset(x, y)) }) { drawImage(img, tl, alpha = al) }
        }
        // گلوله‌های برفیِ نرم و درشت (نزدیک به دوربین): سفید با لبه‌ی محو
        for (b in balls) {
            val y = fmod(b.y * cyc + t * b.sp * h, cyc) - 0.06f * h
            val x = b.x * w + sin(t * 0.5f + b.ph) * 26f * dp
            val rr = b.r * dp
            val edge = if (light) halo else Color.White
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.7f), edge.copy(alpha = if (light) 0.55f else 0.25f), Color.Transparent), Offset(x, y), rr), rr, Offset(x, y))
        }
    }
}

// ───────────────────────── قلب ─────────────────────────

private class HeartsFx(private val palette: List<Color>) : FxEngine {
    private class H(val x: Float, val sp: Float, val ph: Float, val size: Float, val color: Int, val f: Float, val o: Float)
    private val hearts: List<H>
    private val cols = listOf(Color(0xFFFF5A8A), Color(0xFFFF8FB1), Color(0xFFE63946), Color(0xFFFFB3C7), palette.first())

    init {
        val r = Random(9)
        hearts = List(34) { H(r.nextFloat(), 0.10f + r.nextFloat() * 0.14f, r.nextFloat() * 6.28f, 14f + r.nextFloat() * 28f, r.nextInt(5), 0.6f + r.nextFloat(), r.nextFloat()) }
    }

    private fun heart(cx: Float, cy: Float, s: Float): Path = Path().apply {
        moveTo(cx, cy + s * 0.45f)
        cubicTo(cx - s * 0.78f, cy - s * 0.02f, cx - s * 0.62f, cy - s * 0.72f, cx, cy - s * 0.25f)
        cubicTo(cx + s * 0.62f, cy - s * 0.72f, cx + s * 0.78f, cy - s * 0.02f, cx, cy + s * 0.45f)
        close()
    }

    override fun draw(s: DrawScope, t: Float) = with(s) {
        val w = size.width; val h = size.height
        for (e in hearts) {
            val pr = fmod(t * e.sp + e.o, 1f)
            val y = h * (1.08f - pr * 1.25f)
            val x = e.x * w + sin(t * e.f + e.ph) * 0.045f * w
            val a = (if (pr < 0.1f) pr / 0.1f else if (pr > 0.78f) (1f - pr) / 0.22f else 1f) * 0.92f
            val sz = e.size * density
            val tilt = sin(t * e.f + e.ph) * 14f
            val col = cols[e.color]
            withTransform({ rotate(tilt, Offset(x, y)) }) {
                drawCircle(col.copy(alpha = a * 0.18f), sz * 0.9f, Offset(x, y))
                drawPath(heart(x, y, sz), col.copy(alpha = a))
                drawCircle(Color.White.copy(alpha = a * 0.55f), sz * 0.07f, Offset(x - sz * 0.3f, y - sz * 0.28f))
            }
        }
    }
}

// ───────────────────────── آتش‌بازی ─────────────────────────

private class FireworksFx(private val palette: List<Color>) : FxEngine {
    private class Rocket(val launch: Float, val x0: Float, val tx: Float, val ty: Float, val c1: Color, val c2: Color, val ring: Boolean, val ang0: Float)
    private class Spark(val a: Float, val sp: Float, val col: Int, val sz: Float, val ph: Float)

    private val cycle = 6.6f
    private val rise = 0.78f
    private val life = 2.0f
    private val rockets: List<Rocket>
    private val sparks: List<Spark>

    init {
        val r = Random(33)
        val times = floatArrayOf(0f, 0.55f, 1.2f, 1.85f, 2.5f, 3.2f, 3.9f)
        val pairs = listOf(
            palette[0] to palette[1], palette[2] to Color.White, palette[3] to palette[5], palette[4] to palette[2],
            palette[5] to Color.White, palette[1] to palette[2], palette[3] to palette[4],
        )
        rockets = times.mapIndexed { i, tm ->
            val tx = 0.14f + r.nextFloat() * 0.72f
            Rocket(tm, (tx + (r.nextFloat() - 0.5f) * 0.25f).coerceIn(0.05f, 0.95f), tx, 0.16f + r.nextFloat() * 0.3f, pairs[i].first, pairs[i].second, i % 3 == 1, r.nextFloat() * 6.28f)
        }
        sparks = List(56) { j -> Spark((j / 56f) * 2f * PI.toFloat() + r.nextFloat() * 0.25f, 0.55f + r.nextFloat() * 0.45f, r.nextInt(3), 1.6f + r.nextFloat() * 1.6f, r.nextFloat() * 6.28f) }
    }

    override fun draw(s: DrawScope, t: Float) = with(s) {
        val w = size.width; val h = size.height
        val dp = density
        val tc = fmod(t, cycle)
        for (rk in rockets) {
            val tl = tc - rk.launch
            if (tl < 0f || tl > rise + life) continue
            if (tl < rise) {
                // بالا رفتن با دنباله‌ی طلایی
                for (j in 0..6) {
                    val pj = easeOut((tl - j * 0.028f) / rise)
                    if (tl - j * 0.028f < 0f) continue
                    val x = (rk.x0 + (rk.tx - rk.x0) * pj) * w + sin(pj * 18f + rk.ang0) * 3f * dp
                    val y = h * 1.02f + (rk.ty * h - h * 1.02f) * pj
                    val a = (1f - j / 7f)
                    drawCircle(Color(0xFFFFD166).copy(alpha = 0.85f * a), (3.2f - j * 0.35f).coerceAtLeast(0.8f) * dp, Offset(x, y))
                }
            } else {
                val tau = tl - rise
                val cx = rk.tx * w; val cy = rk.ty * h
                if (tau < 0.25f) {
                    val f = tau / 0.25f
                    val rr = 0.2f * w * easeOut(f)
                    drawCircle(Brush.radialGradient(listOf(rk.c1.copy(alpha = 0.5f * (1f - f)), Color.Transparent), Offset(cx, cy), rr.coerceAtLeast(1f)), rr.coerceAtLeast(1f), Offset(cx, cy))
                }
                val k = 2.4f
                val maxR = 0.3f * w
                for (sp in sparks) {
                    val col = when (sp.col) { 0 -> rk.c1; 1 -> rk.c2; else -> if (rk.ring) rk.c1 else Color.White }
                    val rad = maxR * (if (rk.ring) (0.9f + 0.1f * sp.sp) else sp.sp)
                    for (j in 0..3) {
                        val tj = tau - j * 0.05f
                        if (tj < 0f) continue
                        val dist = rad * (1f - exp(-k * tj))
                        val x = cx + cos(sp.a) * dist
                        val y = cy + sin(sp.a) * dist + 0.07f * h * tj * tj
                        val fade = (1f - tj / life).coerceIn(0f, 1f).pow(1.3f)
                        val tw = 0.78f + 0.22f * sin(tj * 38f + sp.ph)
                        val a = fade * tw * (1f - j * 0.24f)
                        if (a <= 0.01f) continue
                        val rr = sp.sz * dp * (1f - j * 0.18f)
                        if (j == 0) drawCircle(col.copy(alpha = a * 0.22f), rr * 2.6f, Offset(x, y))
                        drawCircle(col.copy(alpha = a), rr, Offset(x, y))
                    }
                }
            }
        }
    }
}
