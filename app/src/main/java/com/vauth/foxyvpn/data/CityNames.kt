package com.vauth.foxyvpn.data

import com.vauth.foxyvpn.i18n.Lang
import java.text.Normalizer
import java.util.Locale

/**
 * اسم شهر برای نمایش زیر اسم کشور.
 * - اگه سرور اسم شهر واقعی داده باشه همون استفاده می‌شه؛
 * - اگه اسم شهر خالی بود یا همان اسم کشور بود، شهرِ پیش‌فرضِ آن کشور (از روی تست) نشان داده می‌شه؛
 * - در حالت فارسی، اسم شهر فارسی می‌شه (اگه ترجمه‌اش را داشته باشیم).
 */
object CityNames {

    /** کد کشور → شهری که سرور آن کشور واقعاً در آن است. */
    private val DEFAULT_BY_COUNTRY = mapOf(
        "AR" to "Buenos Aires", "AU" to "Sydney", "AT" to "Vienna", "BE" to "Brussels",
        "BR" to "São Paulo", "BG" to "Sofia", "CA" to "Vancouver", "CL" to "Santiago",
        "CO" to "Bogotá", "DK" to "Copenhagen", "FI" to "Helsinki", "FR" to "Paris",
        "DE" to "Munich", "GH" to "Accra", "IE" to "Dublin", "IT" to "Milan",
        "JP" to "Tokyo", "KR" to "Seoul", "MY" to "Kuala Lumpur", "MX" to "Querétaro",
        "NL" to "Amsterdam", "NZ" to "Auckland", "NO" to "Oslo", "PE" to "Lima",
        "PH" to "Manila", "PT" to "Lisbon", "SG" to "Singapore", "ZA" to "Johannesburg",
        "ES" to "Madrid", "TH" to "Bangkok", "GB" to "London", "US" to "New York",
        "SE" to "Stockholm",
    )

    /** اسم انگلیسی (بدون حساسیت به حروف/آکسان) → فارسی. */
    private val FA = mapOf(
        "buenos aires" to "بوئنوس آیرس", "sydney" to "سیدنی", "vienna" to "وین",
        "brussels" to "بروکسل", "sao paulo" to "سائوپائولو", "sofia" to "صوفیه",
        "vancouver" to "ونکوور", "santiago" to "سانتیاگو", "bogota" to "بوگوتا",
        "copenhagen" to "کپنهاگ", "helsinki" to "هلسینکی", "paris" to "پاریس",
        "munich" to "مونیخ", "accra" to "آکرا", "dublin" to "دوبلین", "milan" to "میلان",
        "tokyo" to "توکیو", "seoul" to "سئول", "kuala lumpur" to "کوالالامپور",
        "queretaro" to "کرتارو", "amsterdam" to "آمستردام", "auckland" to "اکلند",
        "oslo" to "اسلو", "lima" to "لیما", "manila" to "مانیلا", "lisbon" to "لیسبون",
        "singapore" to "سنگاپور", "johannesburg" to "ژوهانسبورگ", "madrid" to "مادرید",
        "bangkok" to "بانکوک", "london" to "لندن", "new york" to "نیویورک",
        // شهرهای دیگری که ممکنه سرور بده
        "frankfurt" to "فرانکفورت", "los angeles" to "لس‌آنجلس", "toronto" to "تورنتو",
        "montreal" to "مونترال", "zurich" to "زوریخ", "stockholm" to "استکهلم",
        "warsaw" to "ورشو", "prague" to "پراگ", "chicago" to "شیکاگو", "dallas" to "دالاس",
        "seattle" to "سیاتل", "miami" to "میامی", "atlanta" to "آتلانتا", "denver" to "دنور",
        "salt lake city" to "سالت‌لیک‌سیتی", "san jose" to "سن‌خوزه", "melbourne" to "ملبورن",
        "hong kong" to "هنگ‌کنگ", "istanbul" to "استانبول", "dubai" to "دبی",
        "berlin" to "برلین", "rome" to "رم", "barcelona" to "بارسلون", "manchester" to "منچستر",
        "osaka" to "اوساکا", "mexico city" to "مکزیکوسیتی", "washington" to "واشنگتن",
        "ashburn" to "اشبرن", "san francisco" to "سان‌فرانسیسکو", "bucharest" to "بخارست",
        "budapest" to "بوداپست", "athens" to "آتن", "belgrade" to "بلگراد", "kyiv" to "کیف",
        "tallinn" to "تالین", "riga" to "ریگا", "vilnius" to "ویلنیوس", "reykjavik" to "ریکیاویک",
        "luxembourg" to "لوکزامبورگ", "geneva" to "ژنو", "brisbane" to "بریزبن", "perth" to "پرت",
    )

    private fun key(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .trim()

    /** اسمی که زیر اسم کشور نشان داده می‌شه؛ خالی یعنی چیزی نشان نده. */
    fun display(countryCode: String, countryName: String, cityName: String): String {
        val raw = cityName.trim()
        val english = if (raw.isBlank() || raw.equals(countryName.trim(), ignoreCase = true)) {
            DEFAULT_BY_COUNTRY[countryCode.trim().uppercase(Locale.ROOT)].orEmpty()
        } else raw
        if (english.isBlank()) return ""
        return if (Lang.isFa) FA[key(english)] ?: english else english
    }
}
