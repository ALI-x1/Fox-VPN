package com.vauth.foxyvpn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService

/**
 * ویجت دایره‌ای اتصال سریع. فقط وقتی وضعیت VPN، تم یا پالت عوض بشه آپدیت می‌شه (بدون تایمر)،
 * رنگ‌ها از پالت فعال برنامه می‌آن و فقط چند تغییر رنگ روی همون چهار تصویر ثابت اعمال می‌شه.
 * [updateAll] همزمان ویجت کارت باریک و کارت بزرگ رو هم تازه می‌کنه؛ پس سرویس VPN و ThemeController
 * فقط همین یک تابع رو صدا می‌زنن.
 */
class QuickConnectWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds, FoxyVpnService.state.value, force = true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_VPN) {
            FoxWidgetSupport.toggle(context)
        }
    }

    /** ظاهر ویجت برای یک وضعیت؛ data class تا مقایسه‌ی «چیزی عوض شده؟» ارزون باشه. */
    private data class Look(
        val fill: Int,
        val ring: Int,
        val icon: Int,
        val gloss: Boolean,
        val label: String,
    )

    companion object {
        const val ACTION_TOGGLE_VPN = "com.vauth.foxyvpn.action.TOGGLE_VPN"

        /** آخرین ظاهری که روی ویجت‌ها اعمال شد؛ اگه تغییری نکرده باشه، آپدیت اضافه نمی‌فرستیم. */
        @Volatile
        private var lastLook: Look? = null

        fun updateAll(context: Context, state: ConnectionState) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, QuickConnectWidgetProvider::class.java)
            val appWidgetIds = runCatching { appWidgetManager.getAppWidgetIds(componentName) }.getOrNull()
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                render(context, appWidgetManager, appWidgetIds, state, force = false)
            }
            QuickConnectSlimWidgetProvider.updateAll(context, state)
            QuickConnectLargeWidgetProvider.updateAll(context, state)
        }

        private fun render(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            state: ConnectionState,
            force: Boolean,
        ) {
            if (appWidgetIds.isEmpty()) return
            val look = resolveLook(context, state)
            if (!force && look == lastLook) return
            lastLook = look

            val views = RemoteViews(context.packageName, R.layout.widget_quick_connect)
            views.setOnClickPendingIntent(R.id.widget_root, FoxWidgetSupport.togglePendingIntent(context))
            views.setInt(R.id.widget_background, "setColorFilter", look.fill)
            views.setInt(R.id.widget_ring, "setColorFilter", look.ring)
            views.setInt(R.id.widget_button, "setColorFilter", look.icon)
            views.setViewVisibility(R.id.widget_gloss, if (look.gloss) View.VISIBLE else View.GONE)
            views.setContentDescription(R.id.widget_root, "Fox-VPN, ${look.label}")

            // یک تماس برای همه‌ی ویجت‌های دایره‌ای روی صفحه
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        private fun resolveLook(context: Context, state: ConnectionState): Look {
            val c = FoxWidgetSupport.colors(context)
            val label = FoxWidgetSupport.statusLabel(state)
            return when (state) {
                ConnectionState.CONNECTED -> Look(
                    fill = c.accent,
                    ring = ColorUtils.setAlphaComponent(c.accent, 115),
                    icon = Color.WHITE,
                    gloss = true,
                    label = label,
                )
                ConnectionState.CONNECTING -> Look(
                    fill = c.innerCard,
                    ring = c.connecting,
                    icon = c.connecting,
                    gloss = false,
                    label = label,
                )
                ConnectionState.DISCONNECTED -> Look(
                    fill = c.innerCard,
                    ring = c.cardBorder,
                    icon = c.textSecondary,
                    gloss = false,
                    label = label,
                )
            }
        }

    }
}
