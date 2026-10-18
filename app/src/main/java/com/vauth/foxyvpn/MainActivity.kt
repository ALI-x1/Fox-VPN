package com.vauth.foxyvpn

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.navigation.FoxyNavGraph
import com.vauth.foxyvpn.ui.theme.FoxyVpnTheme
import com.vauth.foxyvpn.ui.theme.rememberThemeController
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.PingUtil

class MainActivity : ComponentActivity() {

    // زبان انتخابی (و جهت راست‌به‌چپ) روی Context فعالیت اعمال می‌شه
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            FoxyVpnService.start(this)
        } else {

            Toast.makeText(
                this,
                tr("Fox-VPN needs Android's VPN permission to tunnel this device's traffic. Proxy-only mode in Settings works without it."),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {  }

    private fun requestConnect() {
        if ((application as FoxyVpnApp).settingsStore.proxyOnlyMode) {
            FoxyVpnService.start(this)
            return
        }

        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            FoxyVpnService.start(this)
        }
    }

    /** فقط یک بار (بعد از ورود به حساب)، نه هنگام زدن دکمه‌ی اتصال. */
    private fun requestNotificationPermissionOnce() {
        val prefs = getSharedPreferences("foxyvpn_ui", MODE_PRIVATE)
        if (prefs.getBoolean("notif_permission_asked", false)) return
        prefs.edit().putBoolean("notif_permission_asked", true).apply()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // تمام‌صفحه: نوار وضعیت و نوار ناوبری شفاف می‌شن و پس‌زمینه‌ی برنامه زیرشون دیده می‌شه
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        PingUtil.init(applicationContext)
        val app = application as FoxyVpnApp
        setContent {

            val themeController = rememberThemeController(app.settingsStore)
            FoxyVpnTheme(
                themeMode = themeController.mode,
                palette = themeController.palette,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    FoxyNavGraph(
                        navController = navController,
                        app = app,
                        themeController = themeController,
                        onRequestConnect = { requestConnect() },
                        onDisconnect = { FoxyVpnService.stop(this) },
                        onHomeShown = { requestNotificationPermissionOnce() },
                    )
                }
            }
        }
    }
}
