package com.vauth.foxyvpn.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** مشخصات یک پلن از سرور (gb/daily: 0 = نامحدود). قیمت و تخفیف رو از ربات (💳 پلن و پرداخت) عوض می‌کنی. */
data class PlanInfo(
    val days: Int,
    val gb: Double,
    val daily: Double,
    val devices: Int,
    val price: Long = 0,
    val disc: Int = 0,
    val final: Long = 0,
)

/** راه‌های پرداخت/ارتباط؛ هر چی خالی باشه تو اپ «به‌زودی» نشون داده می‌شه. */
data class ContactInfo(
    val card: String = "",
    val crypto: String = "",
    val donate: String = "",
    val tg: String = "",
    val bot: String = "",
    val note: String = "",
    val cur: String = "",
)

data class PlanConfig(val plans: Map<String, PlanInfo>, val contacts: ContactInfo) {
    fun plan(key: String): PlanInfo = plans[key] ?: DEFAULT.plans.getValue(key)

    companion object {
        /** مقدارهای پیش‌فرضِ داخل اپ (قبل از رسیدن جواب سرور) — با PLANS تو worker یکی. */
        val DEFAULT = PlanConfig(
            mapOf(
                "free" to PlanInfo(7, 5.0, 0.5, 1),
                "pro" to PlanInfo(30, 30.0, 5.0, 3),
                "max" to PlanInfo(30, 60.0, 15.0, 5),
            ),
            ContactInfo(),
        )
    }
}

/** گرفتن و کش کردنِ پیکربندی پلن‌ها (/v1/config) و بررسی کد تخفیف (/v1/discount). */
object PlanConfigStore {
    private const val PREFS = "foxvpn_planconfig"
    private const val KEY = "json"
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()

    private fun parse(text: String): PlanConfig? = runCatching {
        val j = JSONObject(text)
        val pj = j.getJSONObject("plans")
        val map = DEFAULT_KEYS.associateWith { k ->
            val o = pj.optJSONObject(k) ?: return@associateWith PlanConfig.DEFAULT.plans.getValue(k)
            PlanInfo(
                o.optInt("days"), o.optDouble("gb"), o.optDouble("daily"), o.optInt("devices", 1),
                o.optLong("price"), o.optInt("disc"), o.optLong("final"),
            )
        }
        val cj = j.optJSONObject("contacts") ?: JSONObject()
        PlanConfig(
            map,
            ContactInfo(
                cj.optString("card"), cj.optString("crypto"), cj.optString("donate"), cj.optString("tg"),
                cj.optString("bot"), cj.optString("note"), cj.optString("cur"),
            ),
        )
    }.getOrNull()

    private val DEFAULT_KEYS = listOf("free", "pro", "max")

    fun cached(context: Context): PlanConfig {
        val t = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return t?.let(::parse) ?: PlanConfig.DEFAULT
    }

    /** از اولین Workerِ جوابگو می‌گیره و کش می‌کنه؛ اگه نشد همون کش قبلی برمی‌گرده. */
    suspend fun refresh(context: Context): PlanConfig = withContext(Dispatchers.IO) {
        for (base in RedeemClient.WORKERS.map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }) {
            val text = runCatching {
                http.newCall(Request.Builder().url("$base/v1/config").build()).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
            }.getOrNull() ?: continue
            val cfg = parse(text) ?: continue
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, text).apply()
            return@withContext cfg
        }
        cached(context)
    }

    /** کد تخفیف → (درصد، قیمت نهایی) یا null اگه معتبر نبود. */
    suspend fun checkDiscount(code: String, plan: String): Pair<Int, Long>? = withContext(Dispatchers.IO) {
        for (base in RedeemClient.WORKERS.map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }) {
            val body = JSONObject().put("code", code).put("plan", plan).toString().toRequestBody("application/json".toMediaType())
            val text = runCatching {
                http.newCall(Request.Builder().url("$base/v1/discount").post(body).build()).execute().use { r -> r.body?.string() }
            }.getOrNull() ?: continue
            val j = runCatching { JSONObject(text) }.getOrNull() ?: continue
            if (j.has("error") && j.optString("error") == "rate_limited") return@withContext null
            return@withContext if (j.optBoolean("ok")) j.optInt("pct") to j.optLong("final") else null
        }
        null
    }
}

/** کامپوز: اول کشِ ذخیره‌شده، بعد از سرور تازه می‌شه. */
@Composable
fun rememberPlanConfig(): State<PlanConfig> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(PlanConfigStore.cached(context)) }
    LaunchedEffect(Unit) { state.value = PlanConfigStore.refresh(context) }
    return state
}

/** قالب عدد پول با جداکننده‌ی هزارگان. */
fun formatMoney(v: Long): String = String.format(java.util.Locale.US, "%,d", v)

/** «500 مگابایت» / «5 گیگابایت» / «نامحدود» (ترجمه‌شده)؛ 0 = نامحدود. */
fun gbLabel(gb: Double): String = when {
    gb <= 0 -> tr("Unlimited")
    gb < 1 -> trf("%d MB", (gb * 1024).toInt())
    gb % 1.0 == 0.0 -> trf("%d GB", gb.toInt())
    else -> trf("%.1f GB", gb)
}
