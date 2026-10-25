package com.vauth.foxyvpn.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.vauth.foxyvpn.BuildConfig
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** نتیجه‌ی یک بررسی: [level] 0 = سالم، 1 = هشدار، 2 = مشکل. */
data class DiagItem(val level: Int, val title: String, val detail: String)

data class DiagResult(val items: List<DiagItem>, val advice: String, val report: String)

/**
 * عیب‌یابی تک‌لمسی: اینترنت گوشی، ساعت گوشی، دسترسی به سرور برنامه، وضعیت اشتراک، لیست سرورها و سرور انتخاب‌شده رو چک می‌کنه
 * و به زبان ساده می‌گه مشکل از کجاست. گزارش متنی (بدون کد و اطلاعات خصوصی) برای ارسال به پشتیبانی هم ساخته می‌شه.
 */
object Diagnostics {
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS).build()

    suspend fun run(context: Context): DiagResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val items = ArrayList<DiagItem>()

        // ۱) اینترنت گوشی
        val cm = app.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        val nets = runCatching { cm?.allNetworks?.toList() }.getOrNull().orEmpty().mapNotNull { cm?.getNetworkCapabilities(it) }
            .filter { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
        val validated = nets.any { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
        items += when {
            nets.isEmpty() -> DiagItem(2, tr("Phone internet"), tr("No network connected"))
            !validated -> DiagItem(2, tr("Phone internet"), tr("Connected, but no internet access"))
            else -> DiagItem(0, tr("Phone internet"), if (nets.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }) "Wi-Fi" else tr("Mobile data"))
        }

        // ۲) سرور برنامه + ساعت گوشی (از هدر Date جواب سرور)
        val base = RedeemClient(app).diagBase()
        var skewSec = 0L
        if (base == null) {
            items += DiagItem(1, tr("App server"), tr("Not configured"))
        } else {
            val t0 = SystemClock.elapsedRealtime()
            val r = runCatching {
                http.newCall(Request.Builder().url("$base/health").build()).execute().use { resp -> resp.code to resp.header("Date") }
            }.getOrNull()
            val ms = SystemClock.elapsedRealtime() - t0
            if (r == null) items += DiagItem(2, tr("App server"), tr("Not reachable"))
            else {
                items += DiagItem(if (ms > 2500) 1 else 0, tr("App server"), if (ms > 2500) trf("Slow (%d ms)", ms.toInt()) else trf("OK (%d ms)", ms.toInt()))
                val d = runCatching { r.second?.let { java.time.ZonedDateTime.parse(it, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() } }.getOrNull()
                if (d != null) skewSec = d - System.currentTimeMillis() / 1000
            }
        }
        if (abs(skewSec) > 300) items += DiagItem(2, tr("Phone clock"), tr("The date or time on your phone is wrong"))
        else items += DiagItem(0, tr("Phone clock"), tr("Correct"))

        // ۳) اشتراک
        LicenseState.load(app)
        val st = LicenseState.status.value
        val block = LicenseState.blockNow(app)
        items += when {
            block != null -> DiagItem(2, tr("Your plan"), block.message)
            st == null -> DiagItem(1, tr("Your plan"), tr("Unknown yet"))
            else -> {
                val left = st.expiresAt?.let { (it - System.currentTimeMillis() / 1000) / 86400 }
                if (left != null && left in 0..2) DiagItem(1, tr("Your plan"), trf("Ends in %d day(s)", left.toInt() + 1)) else DiagItem(0, tr("Your plan"), tr("Active"))
            }
        }

        // ۴) لیست سرورها
        val servers = withTimeoutOrNull(8000) { runCatching { ServerListClient().fetchCountries() }.getOrNull() }
        items += if (servers == null) DiagItem(2, tr("Server list"), tr("Could not load"))
        else DiagItem(0, tr("Server list"), trf("%d countries", servers.size))

        // ۵) سرور انتخاب‌شده
        val sel = runCatching { ProxyStateStore(app).load() }.getOrNull()
        if (sel != null && validated) {
            val t1 = SystemClock.elapsedRealtime()
            val ok = runCatching { Socket().use { it.connect(InetSocketAddress(sel.host, sel.port), 4000) }; true }.getOrDefault(false)
            val ms = (SystemClock.elapsedRealtime() - t1).toInt()
            items += if (!ok) DiagItem(2, tr("Selected server"), tr("Not responding"))
            else DiagItem(if (ms > 1200) 1 else 0, tr("Selected server"), if (ms > 1200) trf("Slow (%d ms)", ms) else trf("OK (%d ms)", ms))
        }

        val advice = when {
            items.any { it.title == tr("Phone internet") && it.level == 2 } -> tr("Your phone has no working internet. Check mobile data or Wi-Fi, then try again.")
            items.any { it.title == tr("Phone clock") && it.level == 2 } -> tr("Fix the date and time on your phone (set it to automatic), then reconnect.")
            items.any { it.title == tr("Your plan") && it.level == 2 } -> tr("Your plan needs attention. Open Plans to renew or upgrade.")
            items.any { it.title == tr("App server") && it.level == 2 } -> tr("The app server is unreachable from your network. Try another network or wait a few minutes.")
            items.any { it.title == tr("Selected server") && it.level == 2 } -> tr("The selected server is not responding. Pick another location.")
            items.any { it.level == 1 } -> tr("Everything works, but the connection is slow. Try another location or a better signal.")
            else -> tr("Everything looks fine. If it still fails, send the report to support.")
        }
        val report = buildString {
            appendLine("Fox-VPN ${BuildConfig.VERSION_NAME} · android ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MODEL}")
            items.forEach { appendLine((if (it.level == 0) "OK " else if (it.level == 1) "WARN " else "FAIL ") + it.title + ": " + it.detail) }
            appendLine("skew: ${skewSec}s")
        }
        DiagResult(items, advice, report)
    }
}
