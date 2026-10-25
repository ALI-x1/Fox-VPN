package com.vauth.foxyvpn.i18n

import java.util.Locale

/**
 * ترجمه‌ی یک متن انگلیسی. اگه زبان فارسی باشه و ترجمه‌اش تو [FA_STRINGS] باشه همون برمی‌گرده؛
 * وگرنه خود متن انگلیسی (پس یه ترجمه‌ی جامونده هیچ‌وقت کرش یا متن خالی نمی‌سازه).
 */
fun tr(en: String): String = if (Lang.isFa) bidi(FA_STRINGS[en] ?: en) else en

/** مثل [tr] ولی با جای‌گذاری مقدارها (%s و %d)؛ عددها همیشه لاتین چاپ می‌شن. */
fun trf(en: String, vararg args: Any?): String {
    val out = String.format(Locale.US, tr(en), *args)
    return if (Lang.isFa) bidi(out) else out
}

/**
 * ترتیب درست چپ‌به‌راست/راست‌به‌چپ برای متن فارسیِ آمیخته با عدد یا حروف لاتین (مثل «6 روز و 16 ساعت مانده» یا «397 MB مانده»).
 * UI برنامه LTR می‌مونه؛ پس هر خطِ فارسیِ آمیخته داخل یک embedding راست‌به‌چپ (RLE…PDF) گذاشته می‌شه تا کلمه‌ها جابه‌جا نشن.
 * متن‌های فارسیِ خالص یا فقط-لاتین دست نمی‌خورن. اگه قبلاً پیچیده شده باشه دوباره پیچیده نمی‌شه.
 */
fun bidi(s: String): String {
    if (s.isEmpty() || '\u202B' in s) return s
    var hasFa = false
    var hasMixed = false
    for (ch in s) {
        if (ch in '\u0600'..'\u06FF') hasFa = true
        else if (ch.isDigit() || ch in 'A'..'Z' || ch in 'a'..'z') hasMixed = true
    }
    // ارقام فارسی (۰-۹) هم «ضعیف» حساب می‌شن و وسط متن فارسی جابه‌جا می‌شن
    if (!hasFa || !(hasMixed || s.any { it in '\u06F0'..'\u06F9' })) return s
    return s.split("\n").joinToString("\n") { line ->
        if (line.isBlank() || line.none { it in '\u0600'..'\u06FF' }) line else "\u202B" + line + "\u202C"
    }
}
