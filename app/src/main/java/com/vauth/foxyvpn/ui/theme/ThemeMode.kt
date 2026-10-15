package com.vauth.foxyvpn.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vauth.foxyvpn.data.SettingsStore

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private const val PALETTE_PREFS = "fox_theme_prefs"
private const val PALETTE_KEY = "theme_palette"

class ThemeController(private val settingsStore: SettingsStore, context: Context) {

    // پالت در SharedPreferences خودش ذخیره می‌شه؛ نیازی به دست‌زدن به SettingsStore نیست
    private val palettePrefs =
        context.applicationContext.getSharedPreferences(PALETTE_PREFS, Context.MODE_PRIVATE)

    var mode: ThemeMode by mutableStateOf(settingsStore.themeMode)
        private set

    var palette: ThemePalette by mutableStateOf(ThemePalette.fromName(palettePrefs.getString(PALETTE_KEY, null)))
        private set

    fun resolveDark(systemInDarkTheme: Boolean): Boolean = palette.fixedDark ?: when (mode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun setPalette(next: ThemePalette) {
        if (next == palette) return
        palette = next
        palettePrefs.edit().putString(PALETTE_KEY, next.name).apply()
    }

    fun set(next: ThemeMode) {
        if (next == mode) return
        mode = next
        settingsStore.themeMode = next
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
