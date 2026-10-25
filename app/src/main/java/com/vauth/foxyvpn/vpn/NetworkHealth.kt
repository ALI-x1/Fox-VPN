package com.vauth.foxyvpn.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vauth.foxyvpn.data.model.ConnectionState
import kotlinx.coroutines.delay

/** مشکلی که کاربر باید بدونه (از «اینترنت گوشی» می‌آد نه از خودِ VPN). */
enum class NetIssue { NONE, AIRPLANE, OFFLINE, NO_INTERNET, WEAK_WIFI, UNSTABLE, SLOW_CONNECT }

/**
 * پایش سلامت اینترنت گوشی؛ تا کاربر وقتی اتصال طول می‌کشه یا قطع‌ووصل می‌شه بفهمه مشکل از آنتن/اینترنتشه، نه از VPN.
 *  AIRPLANE    = حالت هواپیما روشنه
 *  OFFLINE     = هیچ شبکه‌ای (Wi-Fi/موبایل) وصل نیست
 *  NO_INTERNET = به شبکه وصله ولی اینترنت نداره (بسته تموم، مودم بی‌اینترنت، پورتال…)
 *  WEAK_WIFI   = سیگنال Wi-Fi خیلی ضعیف (≤ -82dBm)
 *  UNSTABLE    = تو دو دقیقه‌ی اخیر چند بار قطع‌ووصل شده (آنتن ضعیف/در حال حرکت)
 *  SLOW_CONNECT= اتصال بیش از ۱۰ ثانیه طول کشیده ولی شبکه سالمه
 * هر مورد باید ~۲ ثانیه ثابت بمونه تا چشمک نزنه. فقط وقتی [state] اتصال/متصله بررسی می‌شه.
 */
@Composable
fun rememberNetIssue(state: ConnectionState): NetIssue {
    val ctx = LocalContext.current.applicationContext
    var issue by remember { mutableStateOf(NetIssue.NONE) }
    val tracker = remember { NetTracker(ctx) }
    DisposableEffect(tracker) {
        tracker.start()
        onDispose { tracker.stop() }
    }
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        slow = false
        if (state == ConnectionState.CONNECTING) {
            delay(10_000)
            slow = true
        }
    }
    LaunchedEffect(state, slow) {
        if (state == ConnectionState.DISCONNECTED) { issue = NetIssue.NONE; return@LaunchedEffect }
        var last = NetIssue.NONE
        var stable = 0
        while (true) {
            val cand = tracker.evaluate(slow && state == ConnectionState.CONNECTING)
            if (cand == last) stable++ else { last = cand; stable = 0 }
            if (cand == NetIssue.NONE || stable >= 2) issue = cand
            delay(1000)
        }
    }
    return issue
}

private class NetTracker(private val ctx: Context) {
    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private val nets = HashMap<Network, NetworkCapabilities>()
    private val flaps = ArrayList<Long>()
    private var cb: ConnectivityManager.NetworkCallback? = null

    @Synchronized private fun noteFlap() {
        val t = System.currentTimeMillis()
        flaps.add(t)
        flaps.removeAll { t - it > 120_000 }
    }

    fun start() {
        val m = cm ?: return
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { synchronized(this@NetTracker) { nets[network] = m.getNetworkCapabilities(network) ?: return }; noteFlap() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { synchronized(this@NetTracker) { nets[network] = caps } }
            override fun onLost(network: Network) { synchronized(this@NetTracker) { nets.remove(network) }; noteFlap() }
        }
        cb = callback
        runCatching { m.registerNetworkCallback(req, callback, Handler(Looper.getMainLooper())) }
    }

    fun stop() {
        val m = cm ?: return
        cb?.let { runCatching { m.unregisterNetworkCallback(it) } }
        cb = null
        synchronized(this) { nets.clear() }
    }

    @Synchronized fun evaluate(slowConnect: Boolean): NetIssue {
        val airplane = runCatching { Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 }.getOrDefault(false)
        if (airplane && nets.isEmpty()) return NetIssue.AIRPLANE
        if (nets.isEmpty()) return NetIssue.OFFLINE
        if (nets.values.none { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }) return NetIssue.NO_INTERNET
        val wifiOnly = android.os.Build.VERSION.SDK_INT >= 29 && nets.values.all { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
        if (wifiOnly) {
            val rssi = nets.values.map { if (android.os.Build.VERSION.SDK_INT >= 29) it.signalStrength else Int.MIN_VALUE }.filter { it != Int.MIN_VALUE }.maxOrNull()
            if (rssi != null && rssi <= -82) return NetIssue.WEAK_WIFI
        }
        val t = System.currentTimeMillis()
        if (flaps.count { t - it <= 120_000 } >= 4) return NetIssue.UNSTABLE
        if (slowConnect) return NetIssue.SLOW_CONNECT
        return NetIssue.NONE
    }
}
