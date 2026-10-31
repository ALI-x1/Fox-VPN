package com.vauth.foxyvpn.data

import android.annotation.SuppressLint
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
 * مرورگر واقعی چالش Fastly را خودش رد می‌کند و اثر انگشت TLS اش مشکوک نیست؛ درخواست OkHttp اغلب رد می‌شود.
 * فقط وقتی استفاده می‌شود که مسیر معمولی (OkHttp + حل‌کننده‌ی چالش) شکست بخورد.
 */
object WebViewFxa {
    private const val PAGE = "https://accounts.firefox.com/"
    private const val MAX_INJECTIONS = 5

    /** POST به https://api.accounts.firefox.com/v1[path]؛ جواب = (کد HTTP، متن). */
    suspend fun postJson(path: String, body: JSONObject, authorization: String? = null): Pair<Int, String> {
        val ctx = ControlPlaneHttp.appContext ?: currentApplication() ?: throw IllegalStateException("no app context for WebView fallback")
        return withTimeout(50_000L) { withContext(Dispatchers.Main) { runInWebView(ctx, path, body, authorization) } }
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
