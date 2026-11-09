package com.vauth.foxyvpn.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.vauth.foxyvpn.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * لاگ دقیقِ یک بار زدن دکمه‌ی اتصال: از لحظه‌ی شروع، همه‌ی خط‌های AppLogger با زمانِ نسبی (+ثانیه)
 * و یک خلاصه‌ی محیط (شبکه، VPN دیگر، ساعت، وضعیت توکن). توکن/رمز هیچ‌وقت نوشته نمی‌شود.
 */
object ConnectTrace {
    private const val TAG = "Trace"
    @Volatile private var mark = 0L
    @Volatile private var startMs = 0L
    @Volatile private var header = ""

    fun begin(ctx: Context) {
        mark = AppLogger.lastId()
        startMs = System.currentTimeMillis()
        header = runCatching { snapshot(ctx) }.getOrDefault("env: unavailable")
        AppLogger.i(TAG, "connect pressed")
    }

    fun step(msg: String) = AppLogger.i(TAG, msg)

    fun chain(t: Throwable): String =
        generateSequence(t) { it.cause }.take(6).joinToString(" <- ") { it.javaClass.simpleName + ": " + (it.message ?: "").take(120) }

    private fun snapshot(ctx: Context): String {
        val sb = StringBuilder()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
        sb.append("Fox-VPN ").append(BuildConfig.VERSION_NAME).append(" | Android ").append(Build.VERSION.RELEASE)
            .append(" (SDK ").append(Build.VERSION.SDK_INT).append(") | ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
        sb.append("phone time: ").append(fmt.format(Date())).append(" | tz ").append(java.util.TimeZone.getDefault().id).append('\n')
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nets = cm.allNetworks.mapNotNull { n -> cm.getNetworkCapabilities(n)?.let { n to it } }
        for ((_, c) in nets) {
            val kind = when {
                c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL"
                c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETH"
                else -> "OTHER"
            }
            sb.append("net ").append(kind)
                .append(" internet=").append(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .append(" validated=").append(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                .append(" down=").append(c.linkDownstreamBandwidthKbps / 1000).append("Mbps\n")
        }
        val vpnActive = nets.any { it.second.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
        sb.append("another VPN already active: ").append(vpnActive).append('\n')
        val auth = TokenStore(ctx).loadAuth()
        if (auth == null) sb.append("token: none\n") else {
            val left = auth.expiresAtEpochSeconds - System.currentTimeMillis() / 1000
            sb.append("token: present, expires in ").append(left).append("s, has refresh=").append(!auth.refreshToken.isNullOrBlank()).append('\n')
        }
        return sb.toString().trimEnd()
    }

    fun dump(): String {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append(header.ifBlank { "(no connect attempt yet)" }).append("\n---\n")
        for (e in AppLogger.since(mark)) {
            val dt = (e.timestampMillis - startMs) / 1000.0
            sb.append(fmt.format(Date(e.timestampMillis))).append(" +").append(String.format(Locale.US, "%.2f", dt)).append("s ")
                .append(e.level.name.take(1)).append(" [").append(e.tag).append("] ").append(e.message).append('\n')
        }
        return sb.toString()
    }

    fun copy(ctx: Context) {
        runCatching {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("connect-log", dump()))
            android.widget.Toast.makeText(ctx, "Connect log copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
