package com.vauth.foxyvpn.data

import android.content.Context
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.notice.NoticeClient

/**
 * هشدار هوشمند حجم و انقضا (نوتیف): ۸۰٪ و ۹۵٪ حجم، و ۳ روز / ۱ روز مانده به پایان اعتبار.
 * هر هشدار در هر دوره فقط یک‌بار می‌آد (با تمدید/ارتقا خودکار از نو شروع می‌شه). با لمس نوتیف اپ باز می‌شه
 * و نوارِ «تمدید» صفحه‌ی اصلی (به پلن‌ها) همون‌جاست. از [LicenseState.set] صدا زده می‌شه.
 */
object UsageAlerts {
    private const val PREFS = "foxvpn_usage_alerts"
    private const val BASE_ID = 7100

    fun check(context: Context, s: LicenseStatus) {
        val app = context.applicationContext
        if (s.plan == null) return
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis() / 1000

        // دوره‌ی جدید (تمدید/ارتقا/کد تازه) = پرچم‌ها پاک
        val cycle = "${s.plan}:${s.expiresAt ?: 0}"
        if (p.getString("cycle", null) != cycle) {
            p.edit().clear().putString("cycle", cycle).apply()
        }
        val sent = p.getStringSet("sent", emptySet()) ?: emptySet()
        val out = HashSet(sent)

        // حجم
        val total = s.totalBytes
        if (total != null && total > 0) {
            val pct = (s.usedBytes * 100 / total).toInt()
            if (pct < 70) { out.remove("d80"); out.remove("d95") } // حجم اضافه شد/ریست شد
            if (pct >= 95 && "d95" !in out) { out += "d95"; out += "d80"; notify(app, 1, tr("Data almost gone"), trf("You've used %d%% of your data. Renew or upgrade to stay connected.", pct)) }
            else if (pct >= 80 && "d80" !in out) { out += "d80"; notify(app, 2, tr("Most of your data is used"), trf("You've used %d%% of your data. Upgrade to get more.", pct)) }
        }

        // انقضا
        val exp = s.expiresAt
        if (exp != null && exp > now) {
            val days = (exp - now) / 86400.0
            if (days <= 1.0 && "e1" !in out) { out += "e1"; out += "e3"; notify(app, 3, tr("Your plan ends tomorrow"), tr("Renew now so your connection doesn't stop.")) }
            else if (days <= 3.0 && "e3" !in out) { out += "e3"; notify(app, 4, tr("Your plan ends soon"), trf("%d days left. Renew to stay connected.", Math.ceil(days).toInt())) }
        }

        if (out != sent) p.edit().putStringSet("sent", out).apply()
    }

    private fun notify(app: Context, n: Int, title: String, body: String) {
        runCatching { NoticeClient.showPush(app, (BASE_ID + n).toLong(), title, body, "") }
    }
}
