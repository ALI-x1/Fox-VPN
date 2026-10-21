package com.vauth.foxyvpn.data

import com.vauth.foxyvpn.i18n.tr
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
    DEVICE("This device was removed from the code.", true);

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
 *   POST {BASE_URL}/v1/redeem   body: {"code": "...", "deviceId": "..."}
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

    /** آخرین کدی که با موفقیت وارد شده (برای مراحل بعدی: گزارش مصرف و تمدید). */
    var activeCode: String?
        get() = prefs.getString(KEY_ACTIVE_CODE, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_ACTIVE_CODE) else putString(KEY_ACTIVE_CODE, value)
            }.apply()
        }

    /** حالت ادمین (ورود با ایمیل و رمز از صفحه‌ی مخفی)؛ فقط برای صاحب برنامه. */
    var adminMode: Boolean
        get() = prefs.getBoolean(KEY_ADMIN, false)
        set(value) { prefs.edit().putBoolean(KEY_ADMIN, value).apply() }

    suspend fun redeem(code: String): Result<RedeemGrant> = withContext(Dispatchers.IO) {
        runCatching {
            if (BASE_URL.isBlank()) throw RedeemException(tr("Service is not configured yet. Contact support."))

            val body = JSONObject()
                .put("code", code)
                .put("deviceId", deviceId)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/v1/redeem").post(body).build()

            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    when (response.code) {
                        200 -> {
                            val json = JSONObject(text)
                            val refreshToken = json.optString("refreshToken")
                            if (refreshToken.isBlank()) {
                                throw RedeemException(tr("Unexpected response. Contact support."))
                            }
                            activeCode = code
                            LicenseState.set(appContext, LicenseStatus.fromJson(json))
                            RedeemGrant(refreshToken = refreshToken)
                        }
                        404 -> throw RedeemException(tr("This code is not valid. Check it and try again."))
                        403 -> throw RedeemException(tr("This code has expired or was disabled. Contact support."))
                        409 -> throw RedeemException(tr("This code is already used on another device."))
                        429 -> throw RedeemException(tr("Too many attempts. Please try again later."))
                        503 -> throw RedeemException(tr("All servers are busy right now. Please try again later or contact support."))
                        else -> throw RedeemException(trf("Server error (%s). Try again later.", response.code))
                    }
                }
            } catch (e: IOException) {
                throw RedeemException(tr("No connection. Check your internet and try again."))
            }
        }
    }

    /** (ادمین) کلید دسترسی رو مستقیم به استخر حساب‌های سرور اضافه می‌کنه؛ خروجی: شناسه‌ی حساب. */
    suspend fun addAccountToServer(adminKey: String, label: String, refreshToken: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (BASE_URL.isBlank()) throw RedeemException(tr("Service is not configured yet."))
                val body = JSONObject().put("label", label).put("refreshToken", refreshToken)
                    .toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("$BASE_URL/admin/accounts")
                    .header("Authorization", "Bearer $adminKey").post(body).build()
                try {
                    http.newCall(request).execute().use { response ->
                        val text = response.body?.string().orEmpty()
                        when (response.code) {
                            200 -> JSONObject(text).optString("id").ifBlank { throw RedeemException(tr("Unexpected response.")) }
                            401 -> throw RedeemException(tr("The admin key is wrong."))
                            422 -> throw RedeemException(tr("Mozilla rejected this key. Sign in again and retry."))
                            else -> throw RedeemException(trf("Server error (%s).", response.code))
                        }
                    }
                } catch (e: IOException) {
                    throw RedeemException(tr("No connection. Check your internet and try again."))
                }
            }
        }

    /** از سرور یه کد پلن Free می‌گیره (هر دستگاه یه بار؛ دفعه‌ی بعد همون کد برمی‌گرده). */
    suspend fun requestFreeCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (BASE_URL.isBlank()) throw RedeemException(tr("Service is not configured yet. Contact support."))
            val body = JSONObject().put("deviceId", deviceId).toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/v1/free").post(body).build()
            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    when (response.code) {
                        200 -> JSONObject(text).optString("code").takeIf { it.isNotBlank() }
                            ?: throw RedeemException(tr("Unexpected response. Contact support."))
                        403 -> throw RedeemException(tr("Free accounts are not available right now. Contact support."))
                        429 -> throw RedeemException(tr("Free accounts are full for today. Try again tomorrow or contact support."))
                        503 -> throw RedeemException(tr("All servers are busy right now. Please try again later or contact support."))
                        else -> throw RedeemException(trf("Server error (%s). Try again later.", response.code))
                    }
                }
            } catch (e: IOException) {
                throw RedeemException(tr("No connection. Check your internet and try again."))
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
            if (BASE_URL.isBlank()) throw RedeemException(tr("Service is not configured yet."))
            val body = JSONObject().put("code", code).put("deviceId", deviceId).put("bytes", bytes.coerceAtLeast(0))
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/v1/usage").post(body).build()
            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    when (response.code) {
                        200 -> LicenseStatus.fromJson(JSONObject(text)).also { LicenseState.set(appContext, it) }
                        404 -> {
                            // کد دیگه روی سرور وجود نداره (حذف/لغو شده)
                            val old = LicenseState.status.value ?: LicenseStatus("revoked", null, null, null, 0, null, 0, 0)
                            old.copy(state = "revoked").also { LicenseState.set(appContext, it) }
                        }
                        else -> throw RedeemException(trf("Server error (%s)", response.code))
                    }
                }
            } catch (e: IOException) {
                throw RedeemException(tr("No connection"))
            }
        }
    }

    companion object {
        /**
         * آدرس Worker (بعد از نصب مرحله‌ی ۲ اینجا گذاشته می‌شه)، بدون «/» آخر.
         * مثال: "https://fox-redeem.your-name.workers.dev"
         */
        const val BASE_URL = "https://fox-redeem.javidnamaan.workers.dev"

        /** پیام به پشتیبانی. */
        const val SUPPORT_URL = "https://t.me/Ali_jahangirr"

        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACTIVE_CODE = "active_code"
        private const val KEY_ADMIN = "admin_mode"

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
