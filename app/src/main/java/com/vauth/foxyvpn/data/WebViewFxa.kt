package com.vauth.foxyvpn.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * راه دور زدنِ محافظ ضدرباتِ موزیلا: درخواست را از داخل یک WebView واقعی (موتور کروم) می‌فرستد.
 * مرورگر واقعی چالش Fastly را خودش رد می‌کند و اثر انگشت TLS اش مشکوک نیست.
 *
 * سرعت: [prewarm] از وقتی صفحه‌ی ورود باز می‌شود یک WebView را پشت‌صحنه بالا می‌آورد و چالش را رد می‌کند؛
 * تا کاربر کد را بزند صفحه آماده است و درخواست فوری می‌رود (به‌جای چند ثانیه بارگذاری صفحه).
 */
object WebViewFxa {
    private const val PAGE = "https://accounts.firefox.com/"
    private const val MAX_INJECTIONS = 5
    private const val WARM_TTL_MS = 4 * 60_000L

    /** آخرین باری که برای تازه‌سازی توکن ۴۰۶ دیدیم؛ تا مدتی درخواست OkHttp محکوم به شکست را رد می‌کنیم. */
    @Volatile var sawChallengeAt = 0L
    fun challengeRecent(): Boolean = System.currentTimeMillis() - sawChallengeAt < 30 * 60_000L

    class FoxBridge {
        @Volatile var cb: ((Int, String) -> Unit)? = null

        @JavascriptInterface
        fun done(status: Int, text: String) { cb?.invoke(status, text) }
    }

    // فقط روی نخ اصلی لمس می‌شود
    private var warmView: WebView? = null
    private var warmBridge: FoxBridge? = null
    private var warmReady = false
    private var warmAt = 0L

    @SuppressLint("SetJavaScriptEnabled")
    private fun createView(ctx: Context, bridge: FoxBridge): WebView = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        addJavascriptInterface(bridge, "FoxBridge")
    }

    /** صفحه‌ی موزیلا را پشت‌صحنه بالا می‌آورد (بی‌خطر؛ هر خطایی نادیده گرفته می‌شود). */
    fun prewarm(ctx: Context) {
        val app = ctx.applicationContext
        Handler(Looper.getMainLooper()).post {
            runCatching {
                val now = System.currentTimeMillis()
                if (warmView != null && now - warmAt < WARM_TTL_MS) return@post
                runCatching { warmView?.destroy() }
                warmView = null; warmReady = false
                val bridge = FoxBridge()
                val v = createView(app, bridge)
                v.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(w: WebView, url: String?) {
                        if (url != null && url.startsWith("https://accounts.firefox.com")) warmReady = true
                    }
                }
                warmView = v; warmBridge = bridge; warmAt = now
                v.loadUrl(PAGE)
            }
        }
    }

    private fun takeWarm(): Pair<WebView, FoxBridge>? {
        val v = warmView ?: return null
        val b = warmBridge
        val fresh = warmReady && b != null && System.currentTimeMillis() - warmAt < WARM_TTL_MS
        if (!fresh) return null
        warmView = null; warmBridge = null; warmReady = false
        return v to b!!
    }

    /** POST به https://api.accounts.firefox.com/v1[path]؛ جواب = (کد HTTP، متن). */
    suspend fun postJson(path: String, body: JSONObject): Pair<Int, String> {
        val ctx = ControlPlaneHttp.appContext ?: throw IllegalStateException("no app context for WebView fallback")
        return withTimeout(50_000L) { withContext(Dispatchers.Main) { runInWebView(ctx, path, body) } }
    }

    private suspend fun runInWebView(ctx: Context, path: String, body: JSONObject): Pair<Int, String> {
        val warm = takeWarm()
        val bridge = warm?.second ?: FoxBridge()
        val view = warm?.first ?: createView(ctx, bridge)
        try {
            return suspendCancellableCoroutine { cont ->
                var injections = 0
                var finished = false
                val bodyLiteral = JSONObject.quote(body.toString())
                val script = """
                    (async function(){
                      var url = 'https://api.accounts.firefox.com/v1$path';
                      for (var i = 0; i < 4; i++) {
                        try {
                          var r = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, body: $bodyLiteral });
                          var t = await r.text();
                          if (r.status !== 406) { FoxBridge.done(r.status, t); return; }
                        } catch (e) {}
                        await new Promise(function(res){ setTimeout(res, 1500); });
                      }
                      FoxBridge.done(406, '');
                    })();
                """.trimIndent()

                bridge.cb = { status, text ->
                    if (!finished) {
                        if (status == 406 && injections < MAX_INJECTIONS) {
                            view.post { view.reload() } // صفحه‌ی چالش بود؛ بعد از حل شدنش دوباره امتحان می‌کنیم
                        } else {
                            finished = true
                            if (cont.isActive) cont.resume(status to text)
                        }
                    }
                }
                view.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView, url: String?) {
                        if (finished || injections >= MAX_INJECTIONS) return
                        if (url == null || !url.startsWith("https://accounts.firefox.com")) return
                        injections++
                        v.evaluateJavascript(script, null)
                    }
                }
                cont.invokeOnCancellation { view.post { runCatching { view.stopLoading() } } }
                if (warm != null) { injections++; view.evaluateJavascript(script, null) } else view.loadUrl(PAGE)
            }
        } finally {
            runCatching { view.stopLoading(); view.removeJavascriptInterface("FoxBridge"); view.destroy() }
        }
    }
}
