package com.vauth.foxyvpn.quicksettings

import com.vauth.foxyvpn.i18n.tr
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.MainActivity
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.data.TokenStore
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QuickConnectTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listenJob: Job? = null

    // آیکن یک بار ساخته می‌شه (همون لوگوی ویجت)؛ سیستم خودش رنگش رو با حالت کاشی هماهنگ می‌کنه
    private val tileIcon: Icon by lazy { Icon.createWithResource(this, R.drawable.ic_widget_fox) }

    override fun onStartListening() {
        super.onStartListening()
        listenJob?.cancel()
        listenJob = serviceScope.launch {
            FoxyVpnService.state.collect { state ->
                updateTileState(state)
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        when (FoxyVpnService.state.value) {
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                FoxyVpnService.stop(this)
            }
            ConnectionState.DISCONNECTED -> {
                val app = applicationContext as? FoxyVpnApp
                val tokenStore = app?.tokenStore ?: TokenStore(this)
                if (!tokenStore.hasStoredSession()) {
                    launchMainActivityAndCollapse()
                    return
                }

                val settingsStore = app?.settingsStore ?: SettingsStore(this)
                if (settingsStore.proxyOnlyMode) {
                    FoxyVpnService.start(this)
                    return
                }

                val prepareIntent = VpnService.prepare(this)
                if (prepareIntent != null) {
                    launchMainActivityAndCollapse()
                } else {
                    FoxyVpnService.start(this)
                }
            }
        }
    }

    private fun updateTileState(state: ConnectionState) {
        val tile = qsTile ?: return
        tile.icon = tileIcon
        tile.label = tr("Quick Connect")
        when (state) {
            ConnectionState.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = tr("Connected")
                }
            }
            ConnectionState.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = tr("Connecting\u2026")
                }
            }
            ConnectionState.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = tr("Disconnected")
                }
            }
        }
        tile.updateTile()
    }

    private fun launchMainActivityAndCollapse() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
