package com.vauth.foxyvpn.vpn

import android.content.Context
import com.vauth.foxyvpn.data.AppLogger
import com.vauth.foxyvpn.data.PlanConfigStore
import com.vauth.foxyvpn.data.ProxyStateStore
import com.vauth.foxyvpn.data.ServerListClient
import com.vauth.foxyvpn.data.model.ProxyCandidate
import com.vauth.foxyvpn.ui.components.Perks
import kotlinx.coroutines.withTimeout

/**
 * نگهبان قفل کشورها: وقتی آفر/سرور موقت تموم می‌شه یا ادمین کشوری رو از لیست باز حذف می‌کنه،
 * سرور انتخاب‌شده‌ی کاربر (اگه دیگه برایش مجاز نیست) خودکار با یک سرور رایگان عوض می‌شه.
 * هم سرویس (وقتی وصله) و هم صفحه‌ی اصلی (وقتی قطعه) ازش استفاده می‌کنن.
 */
object LockGuard {
    private const val TAG = "LockGuard"

    /** @return سرور تازه اگه عوض شد؛ وگرنه null (یعنی همه‌چیز مجازه یا سرور رایگانی پیدا نشد). */
    suspend fun fixSelection(context: Context, effectiveCountry: String? = null): ProxyCandidate? {
        val app = context.applicationContext
        // همان نمونه‌ی اپلیکیشن (تا Flow صفحه‌ی اصلی هم خودکار تازه بشه)
        val store = (app as? com.vauth.foxyvpn.FoxyVpnApp)?.proxyStateStore ?: ProxyStateStore(app)
        val cur = store.load() ?: return null
        val code = (effectiveCountry ?: cur.countryCode).trim()
        // «Recommended» کشور واقعی‌اش رو خودش انتخاب می‌کنه؛ اونجا قفلی نیست
        if (code.isBlank() || code.equals("REC", ignoreCase = true)) return null
        if (Perks.canUseCountry(app, code)) return null
        val pick = runCatching {
            val countries = withTimeout(12_000L) { ServerListClient().fetchCountries() }
            countries.filter { it.code.isNotBlank() && !it.code.equals("REC", true) && Perks.isFreeCountry(it.code) }
                .flatMap { ServerListClient.candidatesForCountry(countries, it.code) }
                .randomOrNull()
        }.getOrNull() ?: return null
        store.save(pick)
        AppLogger.i(TAG, "country $code is locked now; switched the selection to free server ${pick.authority} (${pick.countryCode})")
        return pick
    }

    /** پیکربندی تازه از سرور (قفل موقت/آفر) و بعد بررسی/اصلاح انتخاب. */
    suspend fun refreshAndFix(context: Context, effectiveCountry: String? = null): ProxyCandidate? {
        runCatching { PlanConfigStore.refresh(context.applicationContext) }
        return fixSelection(context, effectiveCountry)
    }
}
