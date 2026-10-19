package com.vauth.foxyvpn.widget

import android.content.Context
import com.vauth.foxyvpn.data.model.Entitlement
import com.vauth.foxyvpn.vpn.FoxyVpnService
import kotlin.math.abs

/**
 * آخرین حجم باقی‌مانده‌ی حساب برای ویجت بزرگ. هوم‌اسکرین بعد از هر دریافت اطلاعات حساب اینجا ذخیره می‌کنه؛
 * ویجت خودش هیچ درخواست شبکه‌ای نمی‌زنه (باتری).
 */
object WidgetQuotaCache {
    private const val PREFS = "fox_widget_quota"
    private const val KEY_UNLIMITED = "unlimited"
    private const val KEY_TOTAL = "total_gb"
    private const val KEY_REMAINING = "remaining_gb"
    private const val BYTES_PER_GB = 1_073_741_824.0

    internal data class Quota(val remainingGb: Double?, val totalGb: Double, val unlimited: Boolean)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    internal fun load(context: Context): Quota? {
        val p = prefs(context)
        if (!p.contains(KEY_TOTAL)) return null
        val remaining = p.getFloat(KEY_REMAINING, -1f).toDouble().takeIf { it >= 0.0 }
        return Quota(
            remainingGb = remaining,
            totalGb = p.getFloat(KEY_TOTAL, 50f).toDouble(),
            unlimited = p.getBoolean(KEY_UNLIMITED, false),
        )
    }

    fun save(context: Context, entitlement: Entitlement?) {
        if (entitlement == null) return
        val unlimited = !entitlement.limitedBandwidth
        val total = entitlement.maxBytes?.let { it.toDouble() / BYTES_PER_GB }?.takeIf { it > 0.0 } ?: 50.0
        val remaining = entitlement.quotaRemaining?.let { it.toDouble() / BYTES_PER_GB }

        val old = load(context)
        prefs(context).edit()
            .putBoolean(KEY_UNLIMITED, unlimited)
            .putFloat(KEY_TOTAL, total.toFloat())
            .putFloat(KEY_REMAINING, (remaining ?: -1.0).toFloat())
            .apply()

        // ویجت‌ها فقط وقتی تازه می‌شن که عدد قابل‌دیدن (۰٫۱ گیگ) عوض شده باشه
        val changed = old == null ||
            old.unlimited != unlimited ||
            abs(old.totalGb - total) >= 0.1 ||
            (old.remainingGb == null) != (remaining == null) ||
            (old.remainingGb != null && remaining != null && abs(old.remainingGb - remaining) >= 0.1)
        if (changed) {
            runCatching { QuickConnectWidgetProvider.updateAll(context.applicationContext, FoxyVpnService.state.value) }
        }
    }
}
