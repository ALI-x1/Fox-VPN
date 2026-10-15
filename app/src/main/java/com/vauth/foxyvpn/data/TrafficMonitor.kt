package com.vauth.foxyvpn.data

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrafficState(
    val uploadSpeed: Double = 0.0,
    val downloadSpeed: Double = 0.0
)

object TrafficMonitor {
    private val _trafficState = MutableStateFlow(TrafficState())
    val trafficState: StateFlow<TrafficState> = _trafficState.asStateFlow()

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var lastRx = TrafficStats.getTotalRxBytes()
            var lastTx = TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()

            while (true) {
                delay(1000L)
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val timeElapsed = (currentTime - lastTime) / 1000.0
                if (timeElapsed > 0) {
                    val rxBytes = currentRx - lastRx
                    val txBytes = currentTx - lastTx

                    val downloadSpeed = rxBytes / timeElapsed
                    val uploadSpeed = txBytes / timeElapsed

                    _trafficState.value = TrafficState(
                        uploadSpeed = uploadSpeed,
                        downloadSpeed = downloadSpeed
                    )
                }

                lastRx = currentRx
                lastTx = currentTx
                lastTime = currentTime
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _trafficState.value = TrafficState(0.0, 0.0)
    }
}
