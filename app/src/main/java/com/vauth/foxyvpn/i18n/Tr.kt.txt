package com.vauth.foxyvpn.i18n

import java.util.Locale

/**
 * ترجمه‌ی یک متن انگلیسی. اگه زبان فارسی باشه و ترجمه‌اش تو [FA_STRINGS] باشه همون برمی‌گرده؛
 * وگرنه خود متن انگلیسی (پس یه ترجمه‌ی جامونده هیچ‌وقت کرش یا متن خالی نمی‌سازه).
 */
fun tr(en: String): String = if (Lang.isFa) FA_STRINGS[en] ?: en else en

/** مثل [tr] ولی با جای‌گذاری مقدارها (%s و %d)؛ عددها همیشه لاتین چاپ می‌شن. */
fun trf(en: String, vararg args: Any?): String = String.format(Locale.US, tr(en), *args)
