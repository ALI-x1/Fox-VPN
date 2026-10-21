package com.vauth.foxyvpn.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** گزینه‌های زبان برنامه. SYSTEM یعنی «همون زبان گوشی». */
enum class AppLanguage { SYSTEM, ENGLISH, PERSIAN }

/**
 * زبان برنامه. انتخاب کاربر تو SharedPreferences ذخیره می‌شه (قبل از ساخته شدن UI هم خونده می‌شه).
 *
 * زبان فعلی تو [isFa] نگه داشته می‌شه تا [tr] از هرجا (صفحه‌ها، سرویس VPN، نوتیفیکیشن) بدون Context کار کنه.
 * با عوض شدن زبان، Activity دوباره ساخته می‌شه تا جهت صفحه (راست‌به‌چپ / چپ‌به‌راست) هم عوض بشه.
 */
object Lang {
    private const val PREFS = "foxyvpn_lang"
    private const val KEY_CHOICE = "choice"
    private const val KEY_CHOSEN = "chosen"

    /** الان فارسیه؟ (بعد از [init] مقدار می‌گیره) */
    @Volatile
    var isFa: Boolean = false
        private set

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun choice(context: Context): AppLanguage =
        when (prefs(context).getString(KEY_CHOICE, null)) {
            "fa" -> AppLanguage.PERSIAN
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }

    fun setChoice(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(
            KEY_CHOICE,
            when (language) {
                AppLanguage.PERSIAN -> "fa"
                AppLanguage.ENGLISH -> "en"
                AppLanguage.SYSTEM -> "system"
            },
        ).apply()
        init(context)
    }

    /** کاربر صفحه‌ی «انتخاب زبان» اولین اجرا رو رد کرده؟ */
    fun hasChosen(context: Context): Boolean = prefs(context).getBoolean(KEY_CHOSEN, false)

    fun markChosen(context: Context) {
        prefs(context).edit().putBoolean(KEY_CHOSEN, true).apply()
    }

    /** زبان گوشی (نه زبان انتخابی برنامه) فارسیه؟ */
    fun systemIsPersian(): Boolean =
        Resources.getSystem().configuration.locales.get(0)?.language == "fa"

    fun resolveIsFa(language: AppLanguage): Boolean = when (language) {
        AppLanguage.PERSIAN -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> systemIsPersian()
    }

    /** باید تو Application.onCreate صدا زده بشه. */
    fun init(context: Context) {
        isFa = resolveIsFa(choice(context))
    }

    /** زبان فعلی به‌صورت Locale (برای اسم کشورها و ...). */
    val locale: Locale
        get() = if (isFa) Locale.forLanguageTag("fa") else Locale.ENGLISH

    /**
     * Context با زبان و جهت انتخابی؛ تو attachBaseContext فعالیت استفاده می‌شه.
     * وقتی «سیستم» انتخاب شده، همون Context اصلی برمی‌گرده (خود اندروید زبان و جهت رو درست می‌کنه).
     */
    fun wrap(base: Context): Context {
        val language = choice(base)
        if (language == AppLanguage.SYSTEM) {
            Resources.getSystem().configuration.locales.get(0)?.let { Locale.setDefault(it) }
            return base
        }
        val locale = if (language == AppLanguage.PERSIAN) Locale.forLanguageTag("fa") else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
