package com.vauth.foxyvpn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.vpn.FoxyVpnService
import java.util.Locale

/**
 * ویجت «کارت بزرگ» (۴×۲): لوگو، لوکیشن، برچسب وضعیت، نوار حجم باقی‌مانده، و دو دکمه
 * (قطع/وصل و تغییر سرور). نوار حجم از آخرین مقداری می‌خونه که هوم‌اسکرین ذخیره کرده؛ ویجت شبکه نمی‌زنه.
 */
class QuickConnectLargeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds, FoxyVpnService.state.value, force = true)
    }

    private data class Look(
        val state: ConnectionState,
        val colors: WidgetColors,
        val location: WidgetLocation?,
        val quota: WidgetQuotaCache.Quota?,
        val isFa: Boolean,
    )

    companion object {
        @Volatile
        private var lastLook: Look? = null

        fun updateAll(context: Context, state: ConnectionState) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = runCatching {
                manager.getAppWidgetIds(ComponentName(context, QuickConnectLargeWidgetProvider::class.java))
            }.getOrNull() ?: return
            if (ids.isEmpty()) return
            render(context, manager, ids, state, force = false)
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            state: ConnectionState,
            force: Boolean,
        ) {
            if (ids.isEmpty()) return
            val c = FoxWidgetSupport.colors(context)
            val location = FoxWidgetSupport.selectedLocation(context)
            val quota = WidgetQuotaCache.load(context)
            val look = Look(state, c, location, quota, Lang.isFa)
            if (!force && look == lastLook) return
            lastLook = look

            val active = state != ConnectionState.DISCONNECTED
            val statusText = FoxWidgetSupport.statusLabel(state)
            val countryName = location?.name ?: tr("Recommended")

            val views = RemoteViews(context.packageName, R.layout.widget_quick_connect_large)
            views.setOnClickPendingIntent(R.id.widget_root, FoxWidgetSupport.openAppPendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_action, FoxWidgetSupport.togglePendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_secondary, FoxWidgetSupport.openServersPendingIntent(context))

            // کارت
            views.setInt(R.id.widget_card_bg, "setColorFilter", c.innerCard)
            views.setInt(R.id.widget_card_border, "setColorFilter", c.cardBorder)

            // دایره‌ی لوگو
            val (fill, icon) = when (state) {
                ConnectionState.CONNECTED -> c.accent to Color.WHITE
                ConnectionState.CONNECTING -> ColorUtils.setAlphaComponent(c.connecting, 38) to c.connecting
                ConnectionState.DISCONNECTED -> ColorUtils.setAlphaComponent(c.textSecondary, 30) to c.textSecondary
            }
            views.setInt(R.id.widget_background, "setColorFilter", fill)
            views.setInt(R.id.widget_button, "setColorFilter", icon)

            // لوکیشن + پرچم
            views.setTextViewText(R.id.widget_title, countryName)
            views.setTextColor(R.id.widget_title, c.textPrimary)
            views.setTextViewText(R.id.widget_subtitle, tr("Server location"))
            views.setTextColor(R.id.widget_subtitle, c.textSecondary)
            val flagRes = location?.let { FoxWidgetSupport.flagResId(context, it.countryCode) } ?: 0
            if (flagRes != 0) {
                views.setImageViewResource(R.id.widget_flag, flagRes)
                views.setViewVisibility(R.id.widget_flag, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_flag, View.GONE)
            }

            // برچسب وضعیت
            val chipColor = when (state) {
                ConnectionState.CONNECTED -> c.connected
                ConnectionState.CONNECTING -> c.connecting
                ConnectionState.DISCONNECTED -> c.textSecondary
            }
            views.setInt(R.id.widget_chip_bg, "setColorFilter", ColorUtils.setAlphaComponent(chipColor, 36))
            views.setTextViewText(R.id.widget_chip_text, statusText)
            views.setTextColor(R.id.widget_chip_text, chipColor)

            // نوار حجم باقی‌مانده (اگه هنوز مقداری ذخیره نشده باشه، مخفی می‌مونه)
            if (quota != null) {
                views.setViewVisibility(R.id.widget_data_box, View.VISIBLE)
                views.setTextViewText(R.id.widget_data_label, tr("Data remaining"))
                views.setTextColor(R.id.widget_data_label, c.textSecondary)
                val dataText: String
                val fraction: Double
                if (quota.unlimited) {
                    dataText = tr("Unlimited")
                    fraction = 1.0
                } else if (quota.remainingGb == null) {
                    dataText = ""
                    fraction = 0.0
                } else {
                    val total = if (quota.totalGb % 1.0 == 0.0) {
                        String.format(Locale.US, "%.0f", quota.totalGb)
                    } else {
                        String.format(Locale.US, "%.1f", quota.totalGb)
                    }
                    dataText = trf("%.1f of %s GB", quota.remainingGb, total)
                    fraction = (quota.remainingGb / quota.totalGb).coerceIn(0.0, 1.0)
                }
                views.setTextViewText(R.id.widget_data_text, dataText)
                views.setTextColor(R.id.widget_data_text, c.textPrimary)
                views.setInt(R.id.widget_bar_track, "setColorFilter", ColorUtils.setAlphaComponent(c.accent, 46))
                views.setInt(R.id.widget_bar_fill, "setColorFilter", c.accent)
                views.setInt(R.id.widget_bar_fill, "setImageLevel", (fraction * 10_000).toInt())
            } else {
                views.setViewVisibility(R.id.widget_data_box, View.GONE)
            }

            // دکمه‌ها
            if (active) {
                views.setImageViewResource(R.id.widget_action_bg, R.drawable.widget_pill_outline)
                views.setInt(R.id.widget_action_bg, "setColorFilter", ColorUtils.setAlphaComponent(c.accent, 140))
                views.setTextViewText(R.id.widget_action_text, tr("Disconnect"))
                views.setTextColor(R.id.widget_action_text, c.accentText)
            } else {
                views.setImageViewResource(R.id.widget_action_bg, R.drawable.widget_pill)
                views.setInt(R.id.widget_action_bg, "setColorFilter", c.accent)
                views.setTextViewText(R.id.widget_action_text, tr("Connect"))
                views.setTextColor(R.id.widget_action_text, Color.WHITE)
            }
            views.setInt(R.id.widget_secondary_bg, "setColorFilter", c.cardBorder)
            views.setTextViewText(R.id.widget_secondary_text, tr("Change server"))
            views.setTextColor(R.id.widget_secondary_text, c.textPrimary)

            views.setContentDescription(R.id.widget_root, "Fox-VPN, $statusText, $countryName")
            manager.updateAppWidget(ids, views)
        }
    }
}
