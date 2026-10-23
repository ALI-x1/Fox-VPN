package com.vauth.foxyvpn.data

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.notice.NoticeClient
import com.vauth.foxyvpn.i18n.trf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** اطلاعاتی که سرور برای یک کد معتبر برمی‌گردونه. */
data class RedeemGrant(
    val refreshToken: String,
)

/** وضعیت لایسنس (کد) طبق سرور: مصرف، محدودیت‌ها و اعتبار. */
data class LicenseStatus(
    /** ok | daily_limit | total_limit | expired | revoked | device_removed */
    val state: String,
    val plan: String?,
    val expiresAt: Long?,
    val totalBytes: Long?,
    val usedBytes: Long,
    val dailyBytes: Long?,
    val dailyUsed: Long,
    /** زمان (epoch ثانیه) ریست شدن سقف روزانه (نیمه‌شب تهران). */
    val resetAt: Long,
    /** فاصله‌ی گزارش مصرف (ثانیه) که سرور تعیین می‌کنه؛ ۰ = پیش‌فرض اپ. */
    val poll: Int = 0,
    /** آخرین اعلانِ نوتیف‌دارِ سرور؛ اگه از آخرین دیده‌شده بزرگ‌تر باشه اپ همون لحظه نوتیف می‌ده. */
    val nn: Long = 0,
) {
    fun toJson(): String = JSONObject()
        .put("state", state).put("plan", plan ?: JSONObject.NULL)
        .put("expiresAt", expiresAt ?: JSONObject.NULL).put("totalBytes", totalBytes ?: JSONObject.NULL)
        .put("usedBytes", usedBytes).put("dailyBytes", dailyBytes ?: JSONObject.NULL)
        .put("dailyUsed", dailyUsed).put("resetAt", resetAt).toString()

    companion object {
        private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else optLong(key)

        fun fromJson(j: JSONObject) = LicenseStatus(
            state = j.optString("state", "ok"),
            plan = if (j.isNull("plan")) null else j.optString("plan"),
            expiresAt = j.longOrNull("expiresAt"),
            totalBytes = j.longOrNull("totalBytes"),
            usedBytes = j.optLong("usedBytes", 0L),
            dailyBytes = j.longOrNull("dailyBytes"),
            dailyUsed = j.optLong("dailyUsed", 0L),
            resetAt = j.optLong("resetAt", 0L),
            poll = j.optInt("poll", 0),
            nn = j.optLong("nn", 0L),
        )

        fun parse(text: String?): LicenseStatus? =
            runCatching { fromJson(JSONObject(text ?: return null)) }.getOrNull()
    }
}

/** دلیل قفل بودن دکمه‌ی اتصال (همراه با پیام قابل‌نمایش). */
enum class LicenseBlock(private val en: String, val needsNewCode: Boolean) {
    DAILY("You've reached today's data limit. It resets at midnight (Tehran time).", false),
    TOTAL("Your data allowance is used up.", true),
    EXPIRED("Your plan has expired.", true),
    REVOKED("This code has been disabled.", true),
    DEVICE("This device was removed from the code.", true),
    FROZEN("Your account is temporarily frozen. Contact support.", false),
    MAINT("The server is being updated. Please try again in a few minutes.", false);

    val message: String get() = tr(en)
}

/** آخرین وضعیت لایسنس؛ بین صفحه‌ی اصلی و سرویس VPN مشترکه و روی دستگاه ذخیره می‌شه. */
object LicenseState {
    private val _status = MutableStateFlow<LicenseStatus?>(null)
    val status: StateFlow<LicenseStatus?> = _status.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("foxvpn_redeem", Context.MODE_PRIVATE)

    fun load(context: Context) {
        if (_status.value == null) _status.value = LicenseStatus.parse(prefs(context).getString("license", null))
    }

    fun set(context: Context, value: LicenseStatus) {
        _status.value = value
        prefs(context).edit().putString("license", value.toJson()).apply()
    }

    fun clear(context: Context) {
        _status.value = null
        prefs(context).edit().remove("license").remove("pending_bytes").apply()
    }

    /** null یعنی اتصال آزاده. حالت «سقف روزانه» بعد از نیمه‌شب خودش آزاد می‌شه. */
    fun blockOf(s: LicenseStatus?, nowSec: Long = System.currentTimeMillis() / 1000): LicenseBlock? = when (s?.state) {
        null, "ok" -> null
        "daily_limit" -> if (s.resetAt > 0 && nowSec >= s.resetAt) null else LicenseBlock.DAILY
        "total_limit" -> LicenseBlock.TOTAL
        "expired" -> LicenseBlock.EXPIRED
        "revoked" -> LicenseBlock.REVOKED
        "device_removed" -> LicenseBlock.DEVICE
        "frozen" -> LicenseBlock.FROZEN
        "maintenance" -> LicenseBlock.MAINT
        else -> null
    }

    /** برای سرویس VPN: ادمین هیچ‌وقت قفل نمی‌شه. */
    fun blockNow(context: Context): LicenseBlock? {
        val client = RedeemClient(context)
        if (client.adminMode || client.activeCode == null) return null
        load(context)
        return blockOf(_status.value)
    }
}

/** خطای قابل‌نمایش به کاربر (متن آماده). */
class RedeemException(message: String) : Exception(message)

/**
 * ورود با «Redeem code».
 *
 * کد به سرور (Cloudflare Worker) فرستاده می‌شه و سرور «کلید دسترسی» (refresh token) حساب مربوط به اون کد
 * رو برمی‌گردونه. برنامه با همین کلید وارد می‌شه، بدون هیچ لاگین جدیدی به Mozilla (رمز هیچ‌جا ذخیره نمی‌شه).
 *
 * قرارداد API (مرحله‌ی ۲):
 *   POST {Worker}/v1/redeem   body: {"code": "...", "deviceId": "..."}
 *   200 -> {"refreshToken": "..."}
 *   404 کد نامعتبر | 403 لغو/منقضی | 409 روی دستگاه دیگه استفاده شده | 429 تلاش زیاد
 */
class RedeemClient(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("foxvpn_redeem", Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /** شناسه‌ی ثابت این نصب برنامه؛ برای محدود کردن کد به تعداد مشخصی دستگاه. */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    /** نسخه‌ی اپ (versionCode)؛ با گزارش مصرف به سرور می‌ره تا تو ربات معلوم باشه کی نسخه‌ی قدیمی داره. */
    private val appVersion: Int = runCatching {
        val p = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) p.longVersionCode.toInt() else @Suppress("DEPRECATION") p.versionCode
    }.getOrDefault(0)

    /**
     * عکس از کد و سرور فعلی؛ فراخوانی خروجی، جای این دستگاه رو روی همون کد آزاد می‌کنه (خروج از حساب).
     * اول بگیر، بعد کد رو عوض/پاک کن.
     */
    fun releaseSnapshot(): () -> Unit {
        val code = activeCode
        val all = workers
        val base = homeWorker?.takeIf { it in all } ?: all.firstOrNull()
        val dev = deviceId
        if (code == null || base == null) return {}
        return {
            Thread { runCatching { post(base, "/v1/release", JSONObject().put("code", code).put("deviceId", dev)) } }.start()
        }
    }

    /** آخرین کدی که با موفقیت وارد شده (برای مراحل بعدی: گزارش مصرف و تمدید). */
    var activeCode: String?
        get() = prefs.getString(KEY_ACTIVE_CODE, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_ACTIVE_CODE).remove(KEY_HOME) else putString(KEY_ACTIVE_CODE, value)
            }.apply()
        }

    /** Workerهای فعال؛ آدرس‌های خالی نادیده گرفته می‌شن. */
    private val workers: List<String> get() = WORKERS.map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }

    /** Workerی که کد فعلی روش ساخته شده؛ گزارش مصرف فقط به همین می‌ره (کد روی بقیه وجود نداره). */
    private var homeWorker: String?
        get() = prefs.getString(KEY_HOME, null)
        set(value) { prefs.edit().apply { if (value == null) remove(KEY_HOME) else putString(KEY_HOME, value) }.apply() }

    /**
     * جواب یک Worker. [infra] یعنی مشکل از زیرساخته نه از درخواست: قطع شبکه، خطای ۵۰۰/۵۰۲/۵۰۴ یا جوابِ غیر-JSON
     * (مثلاً صفحه‌ی خطای سقف روزانه‌ی کلودفلر). تو این حالت سراغ Worker بعدی می‌ریم.
     */
    private class Reply(val code: Int, val text: String, val infra: Boolean, val network: Boolean)

    private fun post(base: String, path: String, json: JSONObject, adminKey: String? = null): Reply {
        val request = Request.Builder().url(base + path)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .apply { if (adminKey != null) header("Authorization", "Bearer $adminKey") }
            .build()
        return try {
            http.newCall(request).execute().use { r ->
                val text = r.body?.string().orEmpty()
                val isJson = text.trimStart().startsWith("{")
                Reply(r.code, text, infra = !isJson || r.code == 500 || r.code == 502 || r.code == 504, network = false)
            }
        } catch (e: IOException) {
            Reply(0, "", infra = true, network = true)
        }
    }

    private fun noConnection() = RedeemException(tr("No connection. Check your internet and try again."))
    private fun serversBusy() = RedeemException(tr("All servers are busy right now. Please try again later or contact support."))

    /** حالت ادمین (ورود با ایمیل و رمز از صفحه‌ی مخفی)؛ فقط برای صاحب برنامه. */
    var adminMode: Boolean
        get() = prefs.getBoolean(KEY_ADMIN, false)
        set(value) { prefs.edit().putBoolean(KEY_ADMIN, value).apply() }

    /**
     * ورود با کد. اگه چند Worker تنظیم شده باشه، اول Workerی که کد قبلی روش بود و بعد بقیه امتحان می‌شن:
     * ۴۰۴ یعنی «کد اینجا نیست» (بعدی)، خطای زیرساخت یعنی «این Worker الان جواب نمی‌ده» (بعدی)،
     * هر جواب دیگه قطعیه و همون برمی‌گرده.
     */
    suspend fun redeem(code: String): Result<RedeemGrant> = withContext(Dispatchers.IO) {
        runCatching {
            val all = workers
            if (all.isEmpty()) throw RedeemException(tr("Service is not configured yet. Contact support."))
            val home = homeWorker
            val order = if (home != null && home in all) listOf(home) + all.filter { it != home } else all
            val json = JSONObject().put("code", code).put("deviceId", deviceId).put("multi", order.size > 1)
            var infraCount = 0
            var networkCount = 0
            for (base in order) {
                val r = post(base, "/v1/redeem", json)
                if (r.infra) { infraCount++; if (r.network) networkCount++; continue }
                if (r.code == 404) continue
                return@runCatching grantFrom(r, code, base)
            }
            throw when {
                networkCount == order.size -> noConnection()
                infraCount > 0 -> serversBusy() // یه Worker جواب نداد؛ شاید کد اونجاست، پس «نامعتبر» نمی‌گیم
                else -> RedeemException(tr("This code is not valid. Check it and try again."))
            }
        }
    }

    /**
     * موزیلا به حساب این دستگاه 401 داده: از سرور می‌خوایم دستگاه رو به حساب سالم دیگه‌ای منتقل کنه و کلید تازه بده.
     * سرور این درخواست رو برای هر دستگاه هر ۱۰ دقیقه یک بار قبول می‌کنه.
     */
    suspend fun rotate(): Result<RedeemGrant> = withContext(Dispatchers.IO) {
        runCatching {
            val code = activeCode ?: throw RedeemException(tr("No active code"))
            val all = workers
            if (all.isEmpty()) throw RedeemException(tr("Service is not configured yet."))
            val base = homeWorker?.takeIf { it in all } ?: all.first()
            val r = post(base, "/v1/redeem", JSONObject().put("code", code).put("deviceId", deviceId).put("rotate", true))
            if (r.network) throw noConnection()
            if (r.infra) throw serversBusy()
            grantFrom(r, code, base)
        }
    }

    private fun grantFrom(r: Reply, code: String, base: String): RedeemGrant = when (r.code) {
        200 -> {
            val json = JSONObject(r.text)
            val refreshToken = json.optString("refreshToken")
            if (refreshToken.isBlank()) throw RedeemException(tr("Unexpected response. Contact support."))
            activeCode = code
            homeWorker = base
            LicenseState.set(appContext, LicenseStatus.fromJson(json))
            RedeemGrant(refreshToken = refreshToken)
        }
        403 -> throw RedeemException(
            if (runCatching { JSONObject(r.text).optString("error") }.getOrDefault("") == "frozen") tr("Your account is temporarily frozen. Contact support.")
            else tr("This code has expired or was disabled. Contact support."),
        )
        409 -> throw RedeemException(tr("This code is already used on another device."))
        429 -> throw RedeemException(tr("Too many attempts. Please try again later."))
        503 -> throw RedeemException(tr("All servers are busy right now. Please try again later or contact support."))
        else -> throw RedeemException(trf("Server error (%s). Try again later.", r.code))
    }

    /**
     * (ادمین) کلید دسترسی رو مستقیم به استخر حساب‌های یک Worker اضافه می‌کنه ([server] = شماره‌ی Worker از ۰)؛ خروجی: شناسه‌ی حساب.
     * هر Worker استخر حساب خودش رو داره؛ یه حساب موزیلا رو تو چند Worker نذار (سقف دستگاه‌هاش بین‌شون شریک می‌شه).
     */
    suspend fun addAccountToServer(adminKey: String, label: String, refreshToken: String, server: Int = 0): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val all = workers
                if (all.isEmpty()) throw RedeemException(tr("Service is not configured yet."))
                val base = all[server.coerceIn(0, all.lastIndex)]
                val r = post(base, "/admin/accounts", JSONObject().put("label", label).put("refreshToken", refreshToken), adminKey)
                when {
                    r.network -> throw noConnection()
                    r.infra -> throw serversBusy()
                    r.code == 200 -> JSONObject(r.text).optString("id").ifBlank { throw RedeemException(tr("Unexpected response.")) }
                    r.code == 401 -> throw RedeemException(tr("The admin key is wrong."))
                    r.code == 422 -> throw RedeemException(tr("Mozilla rejected this key. Sign in again and retry."))
                    else -> throw RedeemException(trf("Server error (%s).", r.code))
                }
            }
        }

    /**
     * از سرور یه کد پلن Free می‌گیره (هر دستگاه یه بار؛ دفعه‌ی بعد همون کد برمی‌گرده).
     * شروع از Workerی که با هش شناسه‌ی دستگاه انتخاب می‌شه (پخش بار)؛ اگه جواب نداد یا پر بود، بعدی.
     */
    suspend fun requestFreeCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val all = workers
            if (all.isEmpty()) throw RedeemException(tr("Service is not configured yet. Contact support."))
            val start = (deviceId.hashCode() and 0x7fffffff) % all.size
            val order = all.drop(start) + all.take(start)
            val json = JSONObject().put("deviceId", deviceId)
            var semantic: Reply? = null // اولین خطای واقعی (غیرِ زیرساخت)
            var infraCount = 0
            var networkCount = 0
            for (base in order) {
                val r = post(base, "/v1/free", json)
                if (r.infra) { infraCount++; if (r.network) networkCount++; continue }
                if (r.code == 200) {
                    val code = JSONObject(r.text).optString("code")
                    if (code.isNotBlank()) {
                        homeWorker = base
                        return@runCatching code
                    }
                }
                if (semantic == null) semantic = r
            }
            val s = semantic
            throw when {
                s != null -> when (s.code) {
                    403 -> RedeemException(tr("Free accounts are not available right now. Contact support."))
                    429 -> RedeemException(tr("Free accounts are full for today. Try again tomorrow or contact support."))
                    503 -> serversBusy()
                    200 -> RedeemException(tr("Unexpected response. Contact support."))
                    else -> RedeemException(trf("Server error (%s). Try again later.", s.code))
                }
                networkCount == order.size -> noConnection()
                else -> serversBusy()
            }
        }
    }

    /**
     * گزارش مصرف به سرور ([bytes] = مصرف جدید از آخرین گزارش؛ ۰ = فقط گرفتن وضعیت) و گرفتن وضعیت تازه.
     * وضعیت تازه تو [LicenseState] ذخیره می‌شه.
     */
    suspend fun reportUsage(bytes: Long): Result<LicenseStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val code = activeCode ?: throw RedeemException(tr("No active code"))
            val all = workers
            if (all.isEmpty()) throw RedeemException(tr("Service is not configured yet."))
            // فقط Workerی که کد روش ساخته شده؛ بقیه کد رو نمی‌شناسن و ۴۰۴ می‌دن (که به‌اشتباه «لغو شده» حساب می‌شد)
            val base = homeWorker?.takeIf { it in all } ?: all.first()
            val r = post(base, "/v1/usage", JSONObject().put("code", code).put("deviceId", deviceId).put("bytes", bytes.coerceAtLeast(0)).put("v", appVersion))
            when {
                r.network -> throw RedeemException(tr("No connection"))
                // سقف روزانه‌ی Worker یا خطای زیرساخت: مصرف تو گوشی می‌مونه و بعداً فرستاده می‌شه
                r.infra -> throw RedeemException(trf("Server error (%s)", r.code))
                r.code == 200 -> LicenseStatus.fromJson(JSONObject(r.text)).also {
                    LicenseState.set(appContext, it)
                    // اعلان تازه‌ی نوتیف‌دار: همین حالا نوتیف می‌شه (روی همین ترد پس‌زمینه)
                    NoticeClient.onHint(appContext, it.nn)
                }
                r.code == 404 -> {
                    // کد دیگه روی سرور وجود نداره (حذف/لغو شده)
                    val old = LicenseState.status.value ?: LicenseStatus("revoked", null, null, null, 0, null, 0, 0)
                    old.copy(state = "revoked").also { LicenseState.set(appContext, it) }
                }
                else -> throw RedeemException(trf("Server error (%s)", r.code))
            }
        }
    }

    companion object {
        /**
         * آدرس Worker (بعد از نصب مرحله‌ی ۲ اینجا گذاشته می‌شه)، بدون «/» آخر.
         * مثال: "https://fox-redeem.your-name.workers.dev"
         */
        const val BASE_URL = "https://fox-redeem.javidnamaan.workers.dev"

        /**
         * همه‌ی Workerها (اولی همون BASE_URL فعلیه تا کاربرهای قبلی خراب نشن). آدرس خالی نادیده گرفته می‌شه؛
         * آدرس Worker دوم و سوم رو اینجا بنویس. هر Worker دیتابیس و استخر حساب خودش رو داره.
         */
        val WORKERS: List<String> = listOf(
            BASE_URL,
            "", // Worker دوم، مثلاً "https://fox-redeem.partner1.workers.dev"
            "", // Worker سوم
        )

        /** تعداد Workerهای تنظیم‌شده. */
        val serverCount: Int get() = WORKERS.count { it.isNotBlank() }

        /** پیام به پشتیبانی. */
        const val SUPPORT_URL = "https://t.me/Ali_jahangirr"

        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACTIVE_CODE = "active_code"
        private const val KEY_ADMIN = "admin_mode"
        private const val KEY_HOME = "home_worker"

        /** ورودی کاربر رو به قالب FOX-XXXX-XXXX-XXXX می‌رسونه (حروف بزرگ، بدون فاصله و نویسه‌ی اضافه). */
        fun formatCode(input: String): String {
            val raw = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
            if (raw.isEmpty()) return ""
            // وسط تایپ «FOX» هنوز کامل نشده
            if (raw.length < 3 && "FOX".startsWith(raw)) return raw
            val body = (if (raw.startsWith("FOX")) raw.removePrefix("FOX") else raw).take(12)
            if (body.isEmpty()) return "FOX"
            return "FOX-" + body.chunked(4).joinToString("-")
        }

        /** کد کامل (FOX- و سه گروه ۴ کاراکتری). */
        fun isComplete(code: String): Boolean = code.length == 18
    }
}
