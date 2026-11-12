package com.vauth.foxyvpn.data

import android.app.Activity
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings

/**
 * مراقب سیستم برای پیدا کردن علت «VPN permission was revoked by the system».
 * همه‌چیز در لاگ برنامه (AppLogger) با برچسب [Watch] نوشته می‌شه و با دکمه‌ی کپی لاگ اتصال همراهش می‌آد.
 */
object SystemWatch {
    private const val T = "Watch"
    private var installed = false

    fun install(app: Application) {
        if (installed) return
        installed = true
        runCatching { logPreviousExits(app) }
        runCatching { revertH3IfItCrashed(app) }
        runCatching { registerActivities(app) }
        runCatching { registerBroadcasts(app) }
        runCatching { registerNetworks(app) }
        AppLogger.i(T, "start: pid=${android.os.Process.myPid()} ${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT} pkg=${app.packageName}")
    }

    /** چرا پردازه‌ی قبلی بسته شد؟ (کشته شدن توسط سیستم/MIUI، کراش، …) */
    private fun logPreviousExits(app: Application) {
        if (Build.VERSION.SDK_INT < 30) return
        val am = app.getSystemService(ActivityManager::class.java)
        am.getHistoricalProcessExitReasons(app.packageName, 0, 3).forEach {
            AppLogger.i(T, "previous exit: reason=${it.reason} status=${it.status} importance=${it.importance} desc=${it.description} at=${it.timestamp}")
        }
        // کرش native (reason=5): از tombstone فقط رشته‌های مهم (پیام abort، libbox، go) رو لاگ کن
        am.getHistoricalProcessExitReasons(app.packageName, 0, 1).firstOrNull()
            ?.takeIf { it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE }
            ?.let { info ->
                val bytes = runCatching { info.traceInputStream?.use { s -> s.readBytes().take(400_000).toByteArray() } }.getOrNull()
                    ?: return
                val keys = listOf("Abort message", "libbox", "libgojni", "fatal error", "panic", "signal", "SIG", "runtime.", "go.", "Cause", "backtrace")
                val seen = LinkedHashSet<String>()
                Regex("[\\x20-\\x7e]{6,160}").findAll(String(bytes, Charsets.ISO_8859_1)).forEach { m ->
                    val t = m.value.trim()
                    val isMessage = t.length >= 25 && t.contains(' ') && !t.startsWith("/") && !t.contains("/apex/") &&
                        listOf("JNI", "DETECTED", "xception", "failed", "invalid", "pending", "abort", "java_vm", "jni", "panic", "fatal", "go:").any { k -> t.contains(k, ignoreCase = true) }
                    if ((isMessage || keys.any { k -> t.contains(k, ignoreCase = true) }) && seen.size < 60) seen.add(t)
                }
                AppLogger.i(T, "native crash trace (${bytes.size} bytes): ${seen.joinToString(" | ").ifEmpty { "<no keywords>" }}")
            }
    }

    /** اگه راه‌اندازی هسته‌ی H3 وسط کار کرش کرد، پروتکل رو برگردون روی HTTP/2 تا اپ دوباره کرش نکنه. */
    private fun revertH3IfItCrashed(app: Application) {
        val p = app.getSharedPreferences("h3_guard", Context.MODE_PRIVATE)
        if (!p.getBoolean("inflight", false)) return
        p.edit().putBoolean("inflight", false).commit()
        val store = com.vauth.foxyvpn.data.SettingsStore(app)
        if (store.upstreamProtocol == "h3") {
            store.upstreamProtocol = "h2"
            AppLogger.w(T, "HTTP/3 core crashed the app last time; protocol reverted to HTTP/2")
        }
    }

    private fun registerActivities(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private fun l(a: Activity, e: String) = AppLogger.i(T, "activity ${a.javaClass.simpleName} $e")
            override fun onActivityCreated(a: Activity, b: Bundle?) = l(a, "created")
            override fun onActivityStarted(a: Activity) = l(a, "started")
            override fun onActivityResumed(a: Activity) = l(a, "resumed")
            override fun onActivityPaused(a: Activity) = l(a, "paused")
            override fun onActivityStopped(a: Activity) = l(a, "stopped")
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) = l(a, "destroyed")
        })
    }

    private fun registerBroadcasts(app: Application) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val extra = i.data?.schemeSpecificPart?.let { " pkg=$it" } ?: ""
                AppLogger.i(T, "broadcast ${i.action?.substringAfterLast('.')}$extra")
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        val pf = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED); addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
            app.registerReceiver(r, pf, Context.RECEIVER_EXPORTED)
        } else {
            app.registerReceiver(r, f); app.registerReceiver(r, pf)
        }
    }

    private fun tr(c: NetworkCapabilities?): String {
        if (c == null) return "?"
        val t = listOf("CELL" to NetworkCapabilities.TRANSPORT_CELLULAR, "WIFI" to NetworkCapabilities.TRANSPORT_WIFI, "VPN" to NetworkCapabilities.TRANSPORT_VPN)
            .filter { c.hasTransport(it.second) }.joinToString("+") { it.first }
        val uid = if (Build.VERSION.SDK_INT >= 31 && c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) " owner=${c.ownerUid}" else ""
        return "$t validated=${c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}$uid"
    }

    private fun registerNetworks(app: Application) {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) = AppLogger.i(T, "net available $n ${tr(cm.getNetworkCapabilities(n))}")
            override fun onLost(n: Network) = AppLogger.i(T, "net LOST $n")
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) = AppLogger.i(T, "net caps $n ${tr(c)}")
        })
    }

    /** وضعیت لحظه‌ی قطع: آیا هنوز اجازه با ماست؟ اهمیت پردازه؟ Always-on؟ */
    fun snapshotAtRevoke(c: Context): String = runCatching {
        val prep = runCatching { android.net.VpnService.prepare(c) != null }.getOrDefault(false)
        val am = c.getSystemService(ActivityManager::class.java)
        val st = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }
        val aoApp = runCatching { Settings.Secure.getString(c.contentResolver, "always_on_vpn_app") }.getOrNull()
        val aoLock = runCatching { Settings.Secure.getInt(c.contentResolver, "always_on_vpn_lockdown", 0) }.getOrNull()
        val pm = c.getSystemService(android.os.PowerManager::class.java)
        "permissionStillOurs=${!prep} importance=${st.importance} alwaysOnApp=$aoApp lockdown=$aoLock idle=${pm.isDeviceIdleMode} powerSave=${pm.isPowerSaveMode} ignoreBattOpt=${pm.isIgnoringBatteryOptimizations(c.packageName)} bgRestricted=${if (Build.VERSION.SDK_INT >= 28) am.isBackgroundRestricted else "n/a"}"
    }.getOrElse { "snapshot failed: ${it.javaClass.simpleName}" }
}
