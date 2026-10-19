package com.vauth.foxyvpn.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.widget.QuickConnectWidgetProvider

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private const val PALETTE_PREFS = "fox_theme_prefs"
private const val PALETTE_KEY = "theme_palette"
private const val HOME_TOGGLE_KEY = "home_toggle"

class ThemeController(private val settingsStore: SettingsStore, context: Context) {

    // پالت در SharedPreferences خودش ذخیره می‌شه؛ نیازی به دست‌زدن به SettingsStore نیست
    private val appContext = context.applicationContext

    private val palettePrefs =
        appContext.getSharedPreferences(PALETTE_PREFS, Context.MODE_PRIVATE)

    // ویجت‌ها هم‌رنگ پالت و تم برنامه‌ان؛ با تغییرشون فوراً تازه می‌شن
    private fun refreshWidget() {
        runCatching { QuickConnectWidgetProvider.updateAll(appContext, FoxyVpnService.state.value) }
    }

    var mode: ThemeMode by mutableStateOf(settingsStore.themeMode)
        private set

    // state داخلی؛ اسمش فرق داره تا ستر خودکارش با تابع setPalette(next) تداخل JVM نداشته باشه
    private var paletteState: ThemePalette by mutableStateOf(
        ThemePalette.fromName(palettePrefs.getString(PALETTE_KEY, null))
    )

    val palette: ThemePalette get() = paletteState

    /** کلید خورشید/ماه کنار تنظیمات در صفحه‌ی اصلی؛ از صفحه‌ی «تم» می‌شه خاموشش کرد. */
    var showHomeToggle: Boolean by mutableStateOf(palettePrefs.getBoolean(HOME_TOGGLE_KEY, true))
        private set

    fun setShowHomeToggle(value: Boolean) {
        if (value == showHomeToggle) return
        showHomeToggle = value
        palettePrefs.edit().putBoolean(HOME_TOGGLE_KEY, value).apply()
    }

    fun resolveDark(systemInDarkTheme: Boolean): Boolean = palette.fixedDark ?: when (mode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun setPalette(next: ThemePalette) {
        if (next == paletteState) return
        paletteState = next
        palettePrefs.edit().putString(PALETTE_KEY, next.name).apply()
        refreshWidget()
    }

    fun set(next: ThemeMode) {
        if (next == mode) return
        mode = next
        settingsStore.themeMode = next
        refreshWidget()
    }

    fun toggle(systemInDarkTheme: Boolean) {
        set(if (resolveDark(systemInDarkTheme)) ThemeMode.LIGHT else ThemeMode.DARK)
    }
}

@Composable
fun rememberThemeController(settingsStore: SettingsStore): ThemeController {
    val context = LocalContext.current
    return remember(settingsStore) { ThemeController(settingsStore, context) }
}
