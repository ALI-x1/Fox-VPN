package com.vauth.foxyvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

object PingUtil {
    private const val TIMEOUT_MS = 1_200 // تایم‌اوت سریع‌تر برای هر تلاش
    private const val PING_SAMPLES = 2   // تعداد دفعات تست برای گرفتن دقیق‌ترین عدد (حذف نویز شبکه)

    suspend fun measureTcpLatencyMs(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
        withTimeoutOrNull((TIMEOUT_MS * PING_SAMPLES).toLong()) {
            var bestLatency: Int? = null

            // اجرای تست‌های چندگانه برای رسیدن به واقعی‌ترین و کمترین پینگ
            repeat(PING_SAMPLES) {
                val latency = singleTcpPing(host, port)
                if (latency != null) {
                    bestLatency = if (bestLatency == null) latency else min(bestLatency!!, latency)
                }
            }
            bestLatency
        }
    }

    private fun singleTcpPing(host: String, port: Int): Int? {
        return runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                // غیرفعال کردن الگوریتم Nagle برای جلوگیری از تاخیر مصنوعی در ارسال پاکت‌های تست
                socket.tcpNoDelay = true
                socket.bind(InetSocketAddress(0))
                
                // عدم استفاده از socketProtector جهت سنجش پینگِ کاملاً مستقیم و خام سرور
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            }
            ((System.nanoTime() - start) / 1_000_000L).toInt()
        }.getOrNull()
    }
}
