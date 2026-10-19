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
import com.vauth.foxyvpn.vpn.FoxyVpnService

/**
 * ویجت «کارت باریک» (۴×۱): دایره‌ی لوگو، وضعیت، لوکیشن (با پرچم) و دکمه‌ی قطع/وصل.
 * مثل ویجت دایره‌ای فقط با تغییر وضعیت/تم/لوکیشن آپدیت می‌شه. لمس کارت برنامه رو باز می‌کنه.
 */
class QuickConnectSlimWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds, FoxyVpnService.state.value, force = true)
    }

    private data class Look(
        val state: ConnectionState,
        val colors: WidgetColors,
        val location: WidgetLocation?,
        val isFa: Boolean,
    )

    companion object {
        @Volatile
        private var lastLook: Look? = null

        fun updateAll(context: Context, state: ConnectionState) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = runCatching {
                manager.getAppWidgetIds(ComponentName(context, QuickConnectSlimWidgetProvider::class.java))
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
            val look = Look(state, c, location, Lang.isFa)
            if (!force && look == lastLook) return
            lastLook = look

            val active = state != ConnectionState.DISCONNECTED
            val title = FoxWidgetSupport.statusLabel(state)
            val subtitle = location?.name ?: tr("Recommended")

            val views = RemoteViews(context.packageName, R.layout.widget_quick_connect_slim)
            views.setOnClickPendingIntent(R.id.widget_root, FoxWidgetSupport.openAppPendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_action, FoxWidgetSupport.togglePendingIntent(context))

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

            // متن‌ها
            views.setTextViewText(R.id.widget_title, title)
            views.setTextColor(R.id.widget_title, c.textPrimary)
            views.setTextViewText(R.id.widget_subtitle, subtitle)
            views.setTextColor(R.id.widget_subtitle, c.textSecondary)

            // پرچم
            val flagRes = location?.let { FoxWidgetSupport.flagResId(context, it.countryCode) } ?: 0
            if (flagRes != 0) {
                views.setImageViewResource(R.id.widget_flag, flagRes)
                views.setViewVisibility(R.id.widget_flag, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_flag, View.GONE)
            }

            // دکمه‌ی قطع/وصل: وصل بودن → حاشیه‌دار، قطع بودن → توپر
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

            views.setContentDescription(R.id.widget_root, "Fox-VPN, $title, $subtitle")
            manager.updateAppWidget(ids, views)
        }
    }
}
