package com.vauth.foxyvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * سنجش پینگ TCP (زمان برقراری اتصال) به یک سرور.
 *
 * نسبت به نسخه‌ی قبل:
 *  - سرور مرده زود رد می‌شه: دو شکستِ پشت‌سرهم قبل از اولین موفقیت یعنی null (حداکثر ~۲.۴ ثانیه
 *    به‌جای ۵×۱.۵ = ۷.۵ ثانیه). تو حلقه‌ی زنده‌ی لیست سرورها این فرقِ بزرگیه.
 *  - تعداد نمونه قابل تنظیمه: لیست سرورها ۲ نمونه، هوم‌اسکرین ۳ (پیش‌فرض).
 *  - بین نمونه‌ها لغو شدن کوروتین چک می‌شه؛ با بستن صفحه، سنجش‌ها سریع قطع می‌شن.
 *    (قبلاً withTimeout روی کد بلاک‌کننده اثری نداشت.)
 *  - DNS فقط یک بار حل می‌شه و داخل زمان پینگ حساب نمی‌شه.
 *  - آستانه‌های کیفیت (سبز/زرد/قرمز و تعداد میله‌ها) اینجا یک‌جاست تا همه‌ی صفحه‌ها یکی باشن.
 */
object PingUtil {
    private const val CONNECT_TIMEOUT_MS = 1_200

    /** تعداد نمونه‌ی پیش‌فرض؛ میانه‌ی نمونه‌های موفق برگردونده می‌شه. */
    const val DEFAULT_SAMPLES = 3

    /** کم‌تر از این = خوب (سبز، ۳ میله). */
    const val GOOD_MS = 300

    /** کم‌تر از این = متوسط (زرد، ۲ میله)؛ بیشتر = ضعیف (قرمز، ۱ میله). */
    const val FAIR_MS = 500

    /** تعداد میله‌ی روشن برای یک پینگ: ۳ خوب، ۲ متوسط، ۱ ضعیف. */
    fun level(ms: Int): Int = when {
        ms < GOOD_MS -> 3
        ms < FAIR_MS -> 2
        else -> 1
    }

    /**
     * میانه‌ی زمان اتصال TCP به [host]:[port] بر حسب میلی‌ثانیه، یا null اگه سرور جواب نداد.
     */
    suspend fun measureTcpLatencyMs(
        host: String,
        port: Int,
        samples: Int = DEFAULT_SAMPLES,
    ): Int? = withContext(Dispatchers.IO) {
        val wanted = samples.coerceAtLeast(1)

        val address = runCatching { InetSocketAddress(host, port) }.getOrNull()
        if (address == null || address.isUnresolved) return@withContext null

        val results = ArrayList<Int>(wanted)
        var failures = 0
        var attempts = 0
        // یک تلاشِ اضافه برای جبران یه timeout تصادفی وسط نمونه‌ها
        while (results.size < wanted && attempts < wanted + 1) {
            ensureActive()
            attempts++
            val ms = connectOnce(address)
            if (ms != null) {
                results.add(ms)
            } else {
                failures++
                // هنوز هیچ جوابی نیومده و دو بار پشت‌سرهم شکست: سرور مرده، منتظر نمون
                if (results.isEmpty() && failures >= 2) return@withContext null
            }
        }
        median(results)
    }

    private fun connectOnce(address: InetSocketAddress): Int? =
        runCatching {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                val start = System.nanoTime()
                socket.connect(address, CONNECT_TIMEOUT_MS)
                ((System.nanoTime() - start) / 1_000_000L).toInt()
            }
        }.getOrNull()

    private fun median(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            ((sorted[middle - 1].toLong() + sorted[middle].toLong()) / 2).toInt()
        }
    }
}
