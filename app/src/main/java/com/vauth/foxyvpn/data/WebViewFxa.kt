package com.vauth.foxyvpn.data

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * راه دور زدنِ محافظ ضدرباتِ موزیلا: درخواست را از داخل یک WebView واقعی (موتور کروم) می‌فرستد.
 * مرورگر واقعی چالش Fastly را خودش رد می‌کند و اثر انگشت TLS اش مشکوک نیست؛ درخواست OkHttp اغلب رد می‌شود.
 * فقط وقتی استفاده می‌شود که مسیر معمولی (OkHttp + حل‌کننده‌ی چالش) شکست بخورد.
 */
object WebViewFxa {
    private const val PAGE = "https://accounts.firefox.com/"
    private const val MAX_INJECTIONS = 5

    /** WebViewِ قابل‌مشاهده‌ی ورود با مرورگر؛ همون که کاربر توش وارد شده (کوکی/چالش/UA درست). null وقتی بسته‌ست. */
    @Volatile var liveView: WebView? = null

    private suspend fun evalString(view: WebView, js: String): String = suspendCancellableCoroutine { c ->
        view.evaluateJavascript(js) { raw ->
            val v = if (raw == null || raw == "null") "" else runCatching { org.json.JSONArray("[$raw]").getString(0) }.getOrDefault("")
            if (c.isActive) c.resume(v)
        }
    }

    /**
     * درخواست رو توی همون WebViewِ واردشده اجرا می‌کنه. اگه Fastly جواب ۴۰۶ (بدنه‌ی خالی) داد، خود صفحه رو reload می‌کنیم
     * تا اسکریپت چالشِ Fastly توی مرورگر واقعی حل بشه و کوکی‌ش ست بشه، بعد دوباره امتحان می‌کنیم (۳ دور).
     */
    private suspend fun runLive(view: WebView, path: String, body: JSONObject, authorization: String?): Pair<Int, String> = withContext(Dispatchers.Main) {
        val bodyLiteral = JSONObject.quote(body.toString())
        val authJs = if (authorization.isNullOrEmpty()) "" else ", 'Authorization': " + JSONObject.quote(authorization)
        var last = "no response"
        val origUa = view.settings.userAgentString
        val defaultUa = runCatching { android.webkit.WebSettings.getDefaultUserAgent(view.context) }.getOrDefault(origUa)
        try {
        for (cycle in 1..3) {
            // دور ۱: UA پیش‌فرض WebView (هم‌خوان با TLS کروم)، دور ۲: همراه کوکی، دور ۳: UA فایرفاکس
            val useDefaultUa = cycle != 3
            val withCred = cycle == 2
            view.settings.userAgentString = if (useDefaultUa) defaultUa else origUa
            LoginTrace.log("WebView(live) try $cycle/3 ua=" + (if (useDefaultUa) "default" else "firefox") + " cred=" + withCred)
            val credJs = if (withCred) ", credentials: 'include'" else ""
            val key = "__fox" + System.nanoTime()
            val script = """
                (async function(){
                  try {
                    if (location.hostname.indexOf('firefox.com') < 0) { window.$key = JSON.stringify({s:-1,t:'origin ' + location.hostname}); return; }
                    var r, t;
                    for (var i = 0; i < 2; i++) {
                      r = await fetch('https://api.accounts.firefox.com/v1$path', { method: 'POST'$credJs, headers: { 'Content-Type': 'application/json', 'Accept': 'application/json'$authJs }, body: $bodyLiteral });
                      t = await r.text();
                      if (r.status !== 406) break;
                      await new Promise(function(res){ setTimeout(res, 1200); });
                    }
                    if (r.status === 406 && !t) { try { t = 'hdrs=' + Array.from(r.headers.keys()).join(',') + ' ct=' + r.headers.get('content-type'); } catch (e2) {} }
                    window.$key = JSON.stringify({s:r.status,t:t});
                  } catch (e) { window.$key = JSON.stringify({s:-2,t:String(e)}); }
                })();
            """.trimIndent()
            view.evaluateJavascript(script, null)
            var raw = ""
            repeat(60) { if (raw.isEmpty()) { delay(300); raw = evalString(view, "window.$key||''") } }
            if (raw.isEmpty()) { last = "no response"; continue }
            val o = JSONObject(raw)
            val s = o.getInt("s")
            if (s >= 0 && s != 406) return@withContext s to o.optString("t")
            last = "HTTP $s, body " + o.optString("t").length + " chars " + o.optString("t").take(80)
            if (s == -1) break
            LoginTrace.log("WebView(live) 406 -> reload page (cycle $cycle/3)")
            view.reload()
            delay(4_000) // صفحه + اسکریپت چالش فرصت حل‌شدن داشته باشن
            LoginTrace.log("WebView(live) cycle $cycle result: $last")
        }
        } finally { runCatching { view.settings.userAgentString = origUa } }
        throw java.io.IOException(last)
    }

    /** POST به https://api.accounts.firefox.com/v1[path]؛ جواب = (کد HTTP، متن). */
    suspend fun postJson(path: String, body: JSONObject, authorization: String? = null, hiddenAllowed: Boolean = true): Pair<Int, String> {
        liveView?.let { lv ->
            LoginTrace.log("WebView(live) $path")
            try { return withTimeout(45_000L) { runLive(lv, path, body, authorization) } }
            catch (e: kotlinx.coroutines.TimeoutCancellationException) { LoginTrace.log("WebView(live) timeout 45s"); if (!hiddenAllowed) throw java.io.IOException("live timeout") }
            catch (e: java.io.IOException) { LoginTrace.log("WebView(live) failed: " + e.message); if (!hiddenAllowed) throw e }
        }
        val ctx = ControlPlaneHttp.appContext ?: currentApplication() ?: throw IllegalStateException("no app context for WebView fallback")
        LoginTrace.log("WebView(hidden) $path")
        return withTimeout(30_000L) { withContext(Dispatchers.Main) { runInWebView(ctx, path, body, authorization) } }
    }

    /** اگر attachStorage صدا زده نشده بود (مثلاً برنچ ادمین)، Application فعلی را از خود اندروید می‌گیریم. */
    private fun currentApplication(): android.content.Context? = runCatching {
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? android.app.Application
    }.getOrNull()

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun runInWebView(ctx: android.content.Context, path: String, body: JSONObject, authorization: String?): Pair<Int, String> {
        val view = WebView(ctx)
        try {
            return suspendCancellableCoroutine { cont ->
                var injections = 0
                var finished = false
                val bodyLiteral = JSONObject.quote(body.toString())
                val authJs = if (authorization.isNullOrEmpty()) "" else ", 'Authorization': " + JSONObject.quote(authorization)
                val script = """
                    (async function(){
                      var url = 'https://api.accounts.firefox.com/v1$path';
                      for (var i = 0; i < 4; i++) {
                        try {
                          var r = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json'$authJs }, body: $bodyLiteral });
                          var t = await r.text();
                          if (r.status !== 406) { FoxBridge.done(r.status, t); return; }
                        } catch (e) {}
                        await new Promise(function(res){ setTimeout(res, 1500); });
                      }
                      FoxBridge.done(406, '');
                    })();
                """.trimIndent()

                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun done(status: Int, text: String) {
                        if (finished) return
                        if (status == 406 && injections < MAX_INJECTIONS) {
                            view.post { view.reload() } // صفحه‌ی چالش بود؛ بعد از حل شدنش دوباره امتحان می‌کنیم
                            return
                        }
                        finished = true
                        if (cont.isActive) cont.resume(status to text)
                    }
                }, "FoxBridge")
                view.webViewClient = object : WebViewClient() {
                    // خطای شبکه‌ی صفحه‌ی اصلی ← به‌جای ۵۰ ثانیه منتظر ماندن، همان لحظه خطای واقعی رو برگردون
                    override fun onReceivedError(v: WebView, request: android.webkit.WebResourceRequest, error: android.webkit.WebResourceError) {
                        if (request.isForMainFrame && !finished) {
                            finished = true
                            if (cont.isActive) cont.resumeWith(Result.failure(java.io.IOException("WebView: " + error.description)))
                        }
                    }
                    override fun onPageFinished(v: WebView, url: String?) {
                        if (finished || injections >= MAX_INJECTIONS) return
                        if (url == null || !url.startsWith("https://accounts.firefox.com")) return
                        injections++
                        v.evaluateJavascript(script, null)
                    }
                }
                cont.invokeOnCancellation { view.post { runCatching { view.stopLoading() } } }
                view.loadUrl(PAGE)
            }
        } finally {
            runCatching { view.stopLoading(); view.removeJavascriptInterface("FoxBridge"); view.destroy() }
        }
    }
}
