package com.vauth.foxyvpn.data

import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TrafficInfo(
    val txBytes: Long = 0L,
    val rxBytes: Long = 0L,
    val uploadSpeed: Double = 0L,
    val downloadSpeed: Double = 0L
)

object TrafficMonitor {
    private var job: Job? = null
    private val _trafficState = MutableStateFlow(TrafficInfo())
    val trafficState: StateFlow<TrafficInfo> = _trafficState

    private var baseTx = 0L
    private var baseRx = 0L
    private var prevTx = 0L
    private var prevRx = 0L

    fun start() {
        if (job?.isActive == true) return
        val uid = Process.myUid()
        val rawTx = TrafficStats.getUidTxBytes(uid)
        val rawRx = TrafficStats.getUidRxBytes(uid)

        baseTx = if (rawTx < 0) 0L else rawTx
        baseRx = if (rawRx < 0) 0L else rawRx
        prevTx = 0L
        prevRx = 0L

        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                delay(1000L)
                val currentTx = TrafficStats.getUidTxBytes(uid)
                val currentRx = TrafficStats.getUidRxBytes(uid)

                if (currentTx >= 0 && currentRx >= 0) {
                    val totalTx = (currentTx - baseTx).coerceAtLeast(0L)
                    val totalRx = (currentRx - baseRx).coerceAtLeast(0L)

                    val upSpeed = (totalTx - prevTx).coerceAtLeast(0L).toDouble()
                    val downSpeed = (totalRx - prevRx).coerceAtLeast(0L).toDouble()

                    prevTx = totalTx
                    prevRx = totalRx

                    _trafficState.value = TrafficInfo(totalTx, totalRx, upSpeed, downSpeed)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _trafficState.value = TrafficInfo()
    }
}
