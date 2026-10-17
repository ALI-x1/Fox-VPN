package com.vauth.foxyvpn.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * سنجش پینگ TCP (زمان برقراری اتصال) به یک سرور.
 *
 * چی عوض شد تا عدد «واقعی‌تر» و دقیق‌تر بشه (بدون دست‌بردن تو خود عدد):
 *  - مسیر مستقیم: سوکت پینگ به شبکه‌ی واقعی گوشی (وای‌فای/دیتا) بسته می‌شه، نه تونل VPN.
 *    قبلاً وقتی به VPN وصل بودی (حتی خود فاکس)، پینگ از دل تونل رد می‌شد و دو برابر می‌اومد.
 *    اتصال واقعیِ فاکس هم از شبکه‌ی مستقیم می‌ره، پس این عدد همون چیزیه که کاربر تجربه می‌کنه.
 *  - DNS هم از همون شبکه‌ی مستقیم حل می‌شه و داخل زمان پینگ حساب نمی‌شه.
 *  - اگه اسم سرور هم IPv4 داشته باشه هم IPv6، هر دو (مثل Happy Eyeballs مرورگرها) آزموده می‌شن و
 *    سریع‌ترین مسیر عدد پینگه؛ بدون اینکه تعداد اتصال‌ها بیشتر بشه. آدرسی که خراب باشه کنار گذاشته می‌شه.
 *  - عدد نمایش‌داده‌شده «کم‌ترین زمان» بین نمونه‌هاست (استاندارد RTT)؛ نمونه‌ی اولِ دیتای موبایل
 *    که رادیو تازه بیدار شده و کنده، عدد رو خراب نمی‌کنه.
 *  - سرور مرده زود رد می‌شه: دو شکستِ پشت‌سرهم قبل از اولین موفقیت یعنی null.
 *  - بین نمونه‌ها لغو شدن کوروتین چک می‌شه.
 *  - آستانه‌های کیفیت (سبز/زرد/قرمز و تعداد میله‌ها) اینجا یک‌جاست.
 *
 * یک بار PingUtil.init(context) صدا زده بشه (تو MainActivity)؛ اگه صدا زده نشه، مثل قبل
 * (بدون اتصال به شبکه‌ی مستقیم) کار می‌کنه و چیزی خراب نمی‌شه.
 */
object PingUtil {
    private const val CONNECT_TIMEOUT_MS = 1_200

    /** حداکثر آدرس آزموده‌شده برای یک اسم: یکی از هر خانواده (IPv4 / IPv6). */
    private const val MAX_ADDRESSES = 2

    /** تعداد نمونه‌ی پیش‌فرض؛ کم‌ترین زمانِ نمونه‌های موفق برگردونده می‌شه. */
    const val DEFAULT_SAMPLES = 3

    /** تا ۳۵۰ms = سبز (۳ میله). */
    const val GOOD_MS = 350

    /** از ۳۵۰ تا ۷۰۰ms = نارنجی (۲ میله)؛ از ۷۰۰ به بالا = قرمز (۱ میله). */
    const val FAIR_MS = 700

    @Volatile
    private var connectivityManager: ConnectivityManager? = null

    /** یک بار تو شروع برنامه صدا بزن (applicationContext). چند بار صدا زدنش مشکلی نداره. */
    fun init(context: Context) {
        if (connectivityManager == null) {
            connectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        }
    }

    /** تعداد میله‌ی روشن برای یک پینگ: ۳ سبز، ۲ نارنجی، ۱ قرمز. */
    fun level(ms: Int): Int = when {
        ms < GOOD_MS -> 3
        ms < FAIR_MS -> 2
        else -> 1
    }

    /**
     * کم‌ترین زمان اتصال TCP به [host]:[port] بر حسب میلی‌ثانیه، یا null اگه سرور جواب نداد.
     */
    suspend fun measureTcpLatencyMs(
        host: String,
        port: Int,
        samples: Int = DEFAULT_SAMPLES,
    ): Int? = withContext(Dispatchers.IO) {
        val wanted = samples.coerceAtLeast(1)
        val network = underlyingNetwork()

        // آدرس‌ها (حداکثر یکی IPv4 و یکی IPv6) یک بار حل می‌شن و تو زمان پینگ حساب نمی‌شن
        val candidates = resolveAll(host, port, network).toMutableList()
        if (candidates.isEmpty()) return@withContext null

        val results = ArrayList<Int>(wanted)
        var failures = 0
        var attempts = 0
        var index = 0
        // چند تلاشِ اضافه برای جبران timeout تصادفی یا آدرس خراب
        while (results.size < wanted && attempts < wanted + MAX_ADDRESSES) {
            ensureActive()
            attempts++
            val address = candidates[index % candidates.size]
            val ms = connectOnce(address, network)
            if (ms != null) {
                results.add(ms)
                index++ // نمونه‌ی بعدی روی آدرس بعدی؛ هر دو مسیر آزموده می‌شن
            } else {
                failures++
                if (candidates.size > 1) {
                    candidates.remove(address) // آدرس خراب (مثلاً IPv6 بی‌مسیر) کنار می‌ره
                } else if (results.isEmpty() && failures >= 2) {
                    // هنوز هیچ جوابی نیومده و دو بار پشت‌سرهم شکست: سرور مرده، منتظر نمون
                    return@withContext null
                }
            }
        }
        results.minOrNull()
    }

    /** شبکه‌ی واقعی گوشی (غیر VPN)؛ اگه پیدا نشد null و پینگ مثل قبل از مسیر پیش‌فرض می‌ره. */
    private fun underlyingNetwork(): Network? = runCatching {
        val cm = connectivityManager ?: return null
        @Suppress("DEPRECATION")
        val candidates = cm.allNetworks.filter { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@filter false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
        candidates.firstOrNull { network ->
            cm.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        } ?: candidates.firstOrNull()
    }.getOrNull()

    /** یک آدرس از هر خانواده‌ی IP (به ترتیب ترجیح سیستم)، حل‌شده از شبکه‌ی مستقیم. */
    private fun resolveAll(host: String, port: Int, network: Network?): List<InetSocketAddress> =
        runCatching {
            val all: Array<InetAddress> =
                network?.let { runCatching { it.getAllByName(host) }.getOrNull() }
                    ?: InetAddress.getAllByName(host)
            all.distinctBy { it is Inet6Address }
                .take(MAX_ADDRESSES)
                .map { InetSocketAddress(it, port) }
        }.getOrElse { emptyList() }

    private fun connectOnce(address: InetSocketAddress, network: Network?): Int? =
        runCatching {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                // اگه بستن به شبکه‌ی مستقیم جواب نداد (مثلاً lockdown)، با مسیر پیش‌فرض ادامه می‌دیم
                network?.let { runCatching { it.bindSocket(socket) } }
                val start = System.nanoTime()
                socket.connect(address, CONNECT_TIMEOUT_MS)
                ((System.nanoTime() - start) / 1_000_000L).toInt()
            }
        }.getOrNull()
}
