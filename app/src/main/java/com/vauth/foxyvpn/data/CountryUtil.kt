package com.vauth.foxyvpn.data

import java.util.Locale

/** ابزار کوچک برای ایموجی پرچم و اسم کامل کشور (برای نوتیفیکیشن). */
object CountryUtil {

    /** کد دو حرفی (مثل "DE") → ایموجی پرچم؛ برای ورودی نامعتبر رشته‌ی خالی. */
    fun flagEmoji(code: String?): String {
        val cc = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (cc.length != 2 || cc.any { it !in 'A'..'Z' }) return ""
        val base = 0x1F1E6 - 'A'.code
        return String(Character.toChars(base + cc[0].code)) + String(Character.toChars(base + cc[1].code))
    }

    /** کد دو حرفی → اسم کامل انگلیسی؛ اگه ناشناخته بود null. */
    fun countryName(code: String?): String? {
        val cc = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (cc.length != 2 || cc.any { it !in 'A'..'Z' }) return null
        val name = Locale("", cc).getDisplayCountry(Locale.ENGLISH)
        return name.takeIf { it.isNotBlank() && !it.equals(cc, ignoreCase = true) }
    }
}
