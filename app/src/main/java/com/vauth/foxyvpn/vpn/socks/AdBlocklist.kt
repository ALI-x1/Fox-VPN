package com.vauth.foxyvpn.vpn.socks

import android.content.Context
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.AppLogger

/**
 * لیست کوچک دامنه‌های تبلیغاتی و ردیاب. تطبیق روی خود دامنه و همه‌ی زیردامنه‌هاشه.
 * فقط تبلیغاتی که دامنه‌ی جدا دارن بلاک می‌شن (تبلیغ هم‌دامنه با محتوا، مثل یوتیوب، نه).
 */
object AdBlocklist {
    private val builtIn: Set<String> = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "pagead2.googlesyndication.com", "googletagservices.com", "google-analytics.com",
        "googletagmanager.com", "admob.com", "adsense.com", "2mdn.net", "adnxs.com",
        "ads.yahoo.com", "advertising.com", "adsrvr.org", "criteo.com", "criteo.net",
        "taboola.com", "outbrain.com", "pubmatic.com", "rubiconproject.com", "openx.net",
        "casalemedia.com", "amazon-adsystem.com", "moatads.com", "scorecardresearch.com",
        "quantserve.com", "adcolony.com", "applovin.com", "unityads.unity3d.com",
        "ironsrc.com", "supersonicads.com", "vungle.com", "chartboost.com", "inmobi.com",
        "mopub.com", "smaato.net", "startapp.com", "tapjoy.com", "fyber.com", "mintegral.com",
        "pangleglobal.com", "an.facebook.com", "ads.twitter.com",
        "analytics.twitter.com", "ads.linkedin.com", "ads.tiktok.com", "business-api.tiktok.com",
        "analytics.tiktok.com", "adjust.com", "appsflyer.com", "branch.io", "kochava.com",
        "bugsnag.com", "hotjar.com", "mixpanel.com", "segment.io", "yandexadexchange.net",
        "mc.yandex.ru", "an.yandex.ru", "adfox.yandex.ru", "tapin.ir", "yektanet.com",
        "tanzimads.com", "adro.co", "mediaad.org", "sabavision.com", "adivery.com",
    )

    @Volatile
    private var standard: Set<String>? = null

    @Volatile
    private var strong: Set<String>? = null

    private fun readList(context: Context, res: Int, into: HashSet<String>) {
        context.resources.openRawResource(res).bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { into.add(it) }
        }
    }

    /** لیست‌ها را (فقط بار اول) از res/raw می‌خونه؛ سطح ۲ لیست «قوی» رو هم اضافه می‌کنه. */
    @Synchronized
    fun ensureLoaded(context: Context, level: Int) {
        if (level >= 1 && standard == null) {
            runCatching {
                val all = HashSet<String>(builtIn)
                readList(context, R.raw.adblock_domains, all)
                standard = all
                AppLogger.i("AdBlocklist", "standard list: ${all.size} domains")
            }.onFailure {
                standard = builtIn
                AppLogger.w("AdBlocklist", "could not load the standard list; using the built-in one", it)
            }
        }
        if (level >= 2 && strong == null) {
            runCatching {
                val extra = HashSet<String>(80_000)
                readList(context, R.raw.adblock_strong, extra)
                strong = extra
                AppLogger.i("AdBlocklist", "strong list: ${extra.size} extra domains")
            }.onFailure {
                strong = emptySet()
                AppLogger.w("AdBlocklist", "could not load the strong list", it)
            }
        }
    }

    /** level: 0 خاموش، 1 معمولی، 2 قوی. */
    fun isBlocked(host: String, level: Int): Boolean {
        if (level <= 0) return false
        val std = standard ?: builtIn
        val str = if (level >= 2) strong else null
        var h = host.trim().trimEnd('.').lowercase()
        if (h.isEmpty()) return false
        while (true) {
            if (h in std || (str != null && h in str)) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
        }
    }
}
