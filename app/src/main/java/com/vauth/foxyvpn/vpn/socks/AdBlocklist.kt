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
    private var domains: Set<String> = builtIn

    /** لیست بزرگ‌تر از res/raw/adblock_domains.txt؛ اگه خوانده نشد، همون لیست داخلی می‌مونه. */
    fun load(context: Context) {
        if (domains !== builtIn) return
        runCatching {
            val all = HashSet<String>(builtIn)
            context.resources.openRawResource(R.raw.adblock_domains).bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { all.add(it) }
            }
            domains = all
            AppLogger.i("AdBlocklist", "loaded ${all.size} domains")
        }.onFailure { AppLogger.w("AdBlocklist", "could not load the big list; using the built-in one", it) }
    }

    fun isBlocked(host: String): Boolean {
        val domains = domains
        var h = host.trim().trimEnd('.').lowercase()
        if (h.isEmpty()) return false
        while (true) {
            if (h in domains) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
        }
    }
}
