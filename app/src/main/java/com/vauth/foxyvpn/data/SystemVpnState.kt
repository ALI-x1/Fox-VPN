package com.vauth.foxyvpn.data

import android.content.Context
import android.provider.Settings
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService

/**
 * وضعیت واقعیِ «Always-on VPN» و «Block connections without VPN» (کیل‌سوییچ اندروید).
 * اول از تنظیمات سیستم می‌خونیم؛ اگه گوشی اجازه‌ی خوندن نداد، مقداری که خود سرویس
 * (در حال اجرا) گزارش کرده استفاده می‌شه.
 */
object SystemVpnState {
    data class Snapshot(val alwaysOn: Boolean, val lockdown: Boolean)

    /** سرویس موقع اتصال این‌ها رو از خود VpnService می‌گیره (API 29+). */
    @Volatile
    var reported: Snapshot? = null

    fun read(context: Context): Snapshot {
        val cr = context.contentResolver
        val app = runCatching { Settings.Secure.getString(cr, "always_on_vpn_app") }.getOrNull()
        val lock = runCatching { Settings.Secure.getInt(cr, "always_on_vpn_lockdown", 0) == 1 }.getOrDefault(false)
        val alwaysOn = app == context.packageName
        if (alwaysOn) return Snapshot(alwaysOn = true, lockdown = lock)
        // وقتی سرویس زنده است، پاسخ خود سیستم (به‌روز و قابل‌اعتماد) را ترجیح می‌دیم
        FoxyVpnService.liveSystemFlags()?.let { return it }
        val live = reported
        if (live != null && FoxyVpnService.state.value != ConnectionState.DISCONNECTED) return live
        return Snapshot(alwaysOn = false, lockdown = false)
    }
}

/** اثرانگشتِ تنظیماتِ مؤثر بر اتصال، در لحظه‌ی شروع آخرین اتصال. */
object AppliedConfig {
    @Volatile
    var fingerprint: Int? = null
}
