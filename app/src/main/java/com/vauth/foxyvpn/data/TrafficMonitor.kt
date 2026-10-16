package com.vauth.foxyvpn.data

import com.vauth.foxyvpn.vpn.tun.HevSocks5Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * سرعت لحظه‌ای (بایت بر ثانیه) و مصرف کل از شروع اتصال (بایت).
 * مصرف کل از شمارنده‌ی خود تونل می‌آد، پس با قطع و وصل بعدی از صفر شروع می‌شه.
 */
data class TrafficState(
    val uploadSpeed: Double = 0.0,
    val downloadSpeed: Double = 0.0,
    val totalUpload: Long = 0L,
    val totalDownload: Long = 0L,
)

object TrafficMonitor {
    private const val SAMPLE_INTERVAL_MS = 500L

    // نرم‌کردن سرعت تا عدد نپره (۰ = فقط مقدار قبلی، ۱ = فقط نمونه‌ی جدید)
    private const val SMOOTHING = 0.6

    private val _trafficState = MutableStateFlow(TrafficState())
    val trafficState: StateFlow<TrafficState> = _trafficState.asStateFlow()

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // آمار تونل: [1] = بایت ارسالی (آپلود)، [3] = بایت دریافتی (دانلود)
            var first = HevSocks5Tunnel.stats()
            var lastTx = first[1]
            var lastRx = first[3]
            var lastTime = System.nanoTime()
            var upSpeed = 0.0
            var downSpeed = 0.0

            while (true) {
                delay(SAMPLE_INTERVAL_MS)
                val s = HevSocks5Tunnel.stats()
                val now = System.nanoTime()
                val elapsed = (now - lastTime) / 1_000_000_000.0
                if (elapsed > 0) {
                    val dTx = (s[1] - lastTx).coerceAtLeast(0)
                    val dRx = (s[3] - lastRx).coerceAtLeast(0)
                    upSpeed = upSpeed * (1 - SMOOTHING) + (dTx / elapsed) * SMOOTHING
                    downSpeed = downSpeed * (1 - SMOOTHING) + (dRx / elapsed) * SMOOTHING
                    _trafficState.value = TrafficState(
                        uploadSpeed = upSpeed,
                        downloadSpeed = downSpeed,
                        totalUpload = s[1].coerceAtLeast(0),
                        totalDownload = s[3].coerceAtLeast(0),
                    )
                }
                lastTx = s[1]
                lastRx = s[3]
                lastTime = now
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _trafficState.value = TrafficState()
    }
}
