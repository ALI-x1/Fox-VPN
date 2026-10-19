package com.vauth.foxyvpn.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import androidx.compose.ui.graphics.toArgb
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.MainActivity
import com.vauth.foxyvpn.data.CountryUtil
import com.vauth.foxyvpn.data.RECOMMENDED_COUNTRY_CODE
import com.vauth.foxyvpn.data.SettingsStore
import com.vauth.foxyvpn.data.TokenStore
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.ThemeMode
import com.vauth.foxyvpn.ui.theme.ThemePalette
import com.vauth.foxyvpn.ui.theme.spec
import com.vauth.foxyvpn.vpn.FoxyVpnService

/** رنگ‌های پالت فعال برنامه (به‌صورت Int)، مشترک بین هر سه ویجت. */
internal data class WidgetColors(
    val accent: Int,
    val accentText: Int,
    val innerCard: Int,
    val cardBorder: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val connected: Int,
    val connecting: Int,
)

/** لوکیشن انتخاب‌شده: کد کشور (برای پرچم) و اسم نمایشی. */
internal data class WidgetLocation(val countryCode: String, val name: String)

/**
 * کدهای مشترک ویجت‌ها: رنگ‌ها، کلیک‌ها (قطع/وصل، باز کردن برنامه، لیست سرورها) و لوکیشن انتخاب‌شده.
 * همه‌چیز فقط موقع آپدیت ویجت محاسبه می‌شه؛ هیچ کار پس‌زمینه‌ای نداره.
 */
internal object FoxWidgetSupport {
    // همون SharedPreferences ای که ThemeController پالت رو توش ذخیره می‌کنه
    private const val PALETTE_PREFS = "fox_theme_prefs"
    private const val PALETTE_KEY = "theme_palette"

    private val EXTRA_COUNTRY_FA = mapOf("SE" to "\u0633\u0648\u0626\u062F")

    fun colors(context: Context): WidgetColors {
        val app = context.applicationContext
        val settingsStore = (app as? FoxyVpnApp)?.settingsStore ?: SettingsStore(app)
        val palette = ThemePalette.fromName(
            app.getSharedPreferences(PALETTE_PREFS, Context.MODE_PRIVATE).getString(PALETTE_KEY, null),
        )
        val systemDark = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val isDark = palette.fixedDark ?: when (settingsStore.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val spec = palette.spec(isDark)
        val e = spec.extras
        return WidgetColors(
            accent = e.accent.toArgb(),
            accentText = e.accentText.toArgb(),
            innerCard = e.innerCard.toArgb(),
            cardBorder = e.cardBorder.toArgb(),
            textPrimary = e.textPrimary.toArgb(),
            textSecondary = e.textSecondary.toArgb(),
            connected = spec.status.connected.toArgb(),
            connecting = spec.status.connecting.toArgb(),
        )
    }

    fun statusLabel(state: ConnectionState): String = when (state) {
        ConnectionState.CONNECTED -> tr("Connected")
        ConnectionState.CONNECTING -> tr("Connecting\u2026")
        ConnectionState.DISCONNECTED -> tr("Disconnected")
    }

    /** لوکیشن انتخاب‌شده از ProxyStateStore؛ اگه هنوز چیزی انتخاب نشده null. */
    fun selectedLocation(context: Context): WidgetLocation? {
        val app = context.applicationContext as? FoxyVpnApp ?: return null
        val proxy = app.proxyStateStore.selectedProxyFlow.value ?: return null
        val code = proxy.countryCode
        val name = if (code.equals(RECOMMENDED_COUNTRY_CODE, ignoreCase = true)) {
            tr("Recommended")
        } else if (Lang.isFa) {
            CountryUtil.countryName(code)
                ?: EXTRA_COUNTRY_FA[code.trim().uppercase()]
                ?: proxy.countryName
        } else {
            proxy.countryName
        }
        return WidgetLocation(code, name)
    }

    /** آی‌دی drawable پرچم (flag_us, flag_de, ...) یا 0 اگه تصویرش تو پروژه نباشه. */
    fun flagResId(context: Context, code: String): Int {
        val cc = code.trim().lowercase().let { if (it == "uk") "gb" else it }
        if (cc.length != 2) return 0
        return context.resources.getIdentifier("flag_$cc", "drawable", context.packageName)
    }

    // ───── کلیک‌ها ─────

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** قطع/وصل؛ همیشه به گیرنده‌ی ویجت دایره‌ای می‌ره (اکشنش تو مانیفست ثبت شده). */
    fun togglePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickConnectWidgetProvider::class.java).apply {
            action = QuickConnectWidgetProvider.ACTION_TOGGLE_VPN
        }
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(context, 1, intent, FLAGS)
    }

    fun openServersPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_SERVERS, true)
        }
        return PendingIntent.getActivity(context, 2, intent, FLAGS)
    }

    /** منطق ضربه‌ی دکمه‌ی قطع/وصل (برای هر سه ویجت یکسانه). */
    fun toggle(context: Context) {
        when (FoxyVpnService.state.value) {
            ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                FoxyVpnService.stop(context)
            }
            ConnectionState.DISCONNECTED -> {
                val app = context.applicationContext as? FoxyVpnApp
                val tokenStore = app?.tokenStore ?: TokenStore(context)
                if (!tokenStore.hasStoredSession()) {
                    launchMainActivity(context)
                    return
                }
                val settingsStore = app?.settingsStore ?: SettingsStore(context)
                if (settingsStore.proxyOnlyMode) {
                    FoxyVpnService.start(context)
                    return
                }
                if (VpnService.prepare(context) != null) {
                    launchMainActivity(context)
                } else {
                    FoxyVpnService.start(context)
                }
            }
        }
    }

    private fun launchMainActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }
}
