package com.vauth.foxyvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

object PingUtil {
    private const val TIMEOUT_MS = 1_500
    private const val SAMPLE_COUNT = 5
    private const val TOTAL_TIMEOUT_MS = 9_000L

    /**
     * زمان برقراری اتصال TCP به سرور را ۵ بار اندازه می‌گیرد
     * و میانه‌ی نمونه‌های موفق را برمی‌گرداند.
     */
    suspend fun measureTcpLatencyMs(host: String, port: Int): Int? =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
                val address = runCatching {
                    InetSocketAddress(host, port)
                }.getOrNull()

                if (address == null || address.isUnresolved) {
                    return@withTimeoutOrNull null
                }

                val samples = mutableListOf<Int>()

                repeat(SAMPLE_COUNT) {
                    measureOne(address)?.let(samples::add)
                }

                median(samples)
            }
        }

    private fun measureOne(address: InetSocketAddress): Int? =
        runCatching {
            Socket().use { socket ->
                val start = System.nanoTime()
                socket.connect(address, TIMEOUT_MS)
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
