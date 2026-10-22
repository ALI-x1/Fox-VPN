package com.vauth.foxyvpn.notice

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vauth.foxyvpn.FoxyVpnApp
import com.vauth.foxyvpn.MainActivity
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** یک اعلان همگانی از ربات. [worker] = ایندکس Worker که ازش اومده (شماره‌ی اعلان‌ها برای هر Worker جداست). */
data class Notice(val worker: Int, val id: Long, val title: String, val body: String, val url: String)

/** اعلان «نسخه‌ی جدید». [force] = اجباری: پنجره بسته نمی‌شه تا کاربر آپدیت کنه. */
data class UpdateInfo(val versionCode: Int, val versionName: String, val notes: String, val url: String, val force: Boolean)

/** بنر وضعیت سرور: kind = "maint" (در حال به‌روزرسانی) یا "info"؛ [text] خالی یعنی متن پیش‌فرض اپ. */
data class StatusBanner(val kind: String, val text: String)

data class NoticeResult(val notices: List<Notice>, val update: UpdateInfo?, val banner: StatusBanner? = null)

/**
 * گرفتن اعلان‌ها از Workerها (POST /v1/notice). دو مسیر داره:
 *  - داخل برنامه: [fetchForDialog] موقع باز شدن اپ → پنجره‌ی شناور.
 *  - پس‌زمینه: [NoticeJobService] هر چند ساعت (حتی وقتی VPN وصل نیست) → نوتیفیکیشن سیستم.
 */
object NoticeClient {
    const val CHANNEL_ID = "foxyvpn_notices"
    private const val PREFS = "foxvpn_notice"

    /** پشتیبان آپدیت: وقتی هیچ Workerی جواب نده، اپ این فایل رو از گیت‌هاب می‌خونه. */
    const val FALLBACK_UPDATE_URL = "https://raw.githubusercontent.com/ALI-x1/Fox-VPN/main/update.json"
    private const val JOB_ID = 7301
    private const val PERIOD_MS = 6L * 60 * 60 * 1000

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun versionCode(c: Context): Int = runCatching {
        val p = c.packageManager.getPackageInfo(c.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode.toInt() else @Suppress("DEPRECATION") p.versionCode
    }.getOrDefault(0)

    /** با [seenKey] مشخص می‌شه کدوم «آخرین اعلان دیده‌شده» به سرور فرستاده بشه (پنجره یا نوتیف جدا حساب می‌شن). */
    private fun query(c: Context, worker: Int, base: String, seenKey: String, seenOverride: Long? = null): NoticeResult? {
        val rc = RedeemClient(c)
        val body = JSONObject()
            .put("deviceId", rc.deviceId)
            .put("v", versionCode(c))
            .put("seen", seenOverride ?: prefs(c).getLong("${seenKey}_$worker", 0L))
        rc.activeCode?.let { body.put("code", it) }
        val req = Request.Builder().url(base.trimEnd('/') + "/v1/notice")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return runCatching {
            http.newCall(req).execute().use { r ->
                if (r.code != 200) return null
                val j = JSONObject(r.body?.string().orEmpty())
                val arr = j.optJSONArray("notices")
                val list = (0 until (arr?.length() ?: 0)).map { i ->
                    val o = arr!!.getJSONObject(i)
                    Notice(worker, o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("url"))
                }
                val u = j.optJSONObject("update")?.let {
                    UpdateInfo(it.optInt("versionCode"), it.optString("versionName"), it.optString("notes"), it.optString("url"), it.optBoolean("force"))
                }
                val b = j.optJSONObject("banner")?.let { StatusBanner(it.optString("kind"), it.optString("text")) }
                NoticeResult(list, u, b)
            }
        }.getOrNull()
    }

    /** فایل update.json: {"version","version_code","force","changelog","release_url"}؛ null اگه نشد بخونیم. */
    private fun queryFallback(c: Context): UpdateInfo? {
        if (FALLBACK_UPDATE_URL.isBlank()) return null
        return runCatching {
            http.newCall(Request.Builder().url(FALLBACK_UPDATE_URL).build()).execute().use { r ->
                if (r.code != 200) return null
                val j = JSONObject(r.body?.string().orEmpty())
                val code = j.optInt("version_code")
                if (code <= versionCode(c)) return null
                UpdateInfo(code, j.optString("version"), j.optString("changelog"), j.optString("release_url"), j.optBoolean("force") && j.optString("release_url").isNotBlank())
            }
        }.getOrNull()
    }

    private fun queryAll(c: Context, seenKey: String, seenOverride: Long? = null): NoticeResult {
        val bases = RedeemClient.WORKERS.mapIndexed { i, b -> i to b }.filter { it.second.isNotBlank() }
        val notices = ArrayList<Notice>()
        var update: UpdateInfo? = null
        var anyOk = false
        var banner: StatusBanner? = null
        for ((i, b) in bases) {
            val r = query(c, i, b, seenKey, seenOverride) ?: continue
            anyOk = true
            if (banner == null) banner = r.banner
            notices += r.notices
            val u = r.update
            if (u != null && (update == null || u.versionCode > update.versionCode || (u.versionCode == update.versionCode && u.force))) update = u
        }
        if (!anyOk) update = queryFallback(c)
        return NoticeResult(notices, update, banner)
    }

    /** فقط بنر وضعیت و آپدیت (برای نوار بالای صفحه‌ی اصلی)؛ اعلان‌ها رو مصرف نمی‌کنه. */
    suspend fun fetchStatus(c: Context): NoticeResult = withContext(Dispatchers.IO) { queryAll(c, "shown", 1_000_000_000_000L) }

    /** برای پنجره‌ی داخل برنامه؛ آپدیتِ اختیاریِ قبلاً «بعداً» زده‌شده برنمی‌گرده. */
    suspend fun fetchForDialog(c: Context): NoticeResult = withContext(Dispatchers.IO) {
        val r = queryAll(c, "shown")
        val dismissed = prefs(c).getInt("upd_dismissed", 0)
        val u = r.update?.takeIf { it.force || it.versionCode > dismissed }
        NoticeResult(r.notices, u)
    }

    fun markShown(c: Context, n: Notice) {
        val p = prefs(c)
        if (n.id > p.getLong("shown_${n.worker}", 0L)) p.edit().putLong("shown_${n.worker}", n.id).apply()
    }

    fun dismissUpdate(c: Context, u: UpdateInfo) {
        if (!u.force) prefs(c).edit().putInt("upd_dismissed", u.versionCode).apply()
    }

    // ───────── پس‌زمینه ─────────

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = android.app.NotificationChannel(CHANNEL_ID, tr("Announcements"), NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** هر بار باز شدن برنامه صدا زده می‌شه؛ اگه کار قبلاً زمان‌بندی شده باشه دست نمی‌زنه. */
    fun schedule(c: Context) {
        runCatching {
            val js = c.getSystemService(JobScheduler::class.java)
            if (js.getPendingJob(JOB_ID) != null) return
            val info = JobInfo.Builder(JOB_ID, ComponentName(c, NoticeJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD_MS)
                .setPersisted(true)
                .build()
            js.schedule(info)
        }
    }

    /** اجرای کار پس‌زمینه: اعلان‌های تازه رو نوتیف می‌کنه (فقط یک بار برای هر اعلان). */
    fun runBackground(c: Context) {
        val r = queryAll(c, "notif")
        val p = prefs(c)
        val canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ensureChannel(c)
        for (n in r.notices) {
            if (canNotify) post(c, (n.worker * 1_000_000 + n.id.toInt()).coerceAtLeast(1), n.title, n.body.ifBlank { tr("Tap to open") })
            if (n.id > p.getLong("notif_${n.worker}", 0L)) p.edit().putLong("notif_${n.worker}", n.id).apply()
        }
        val u = r.update
        if (u != null && u.versionCode > p.getInt("upd_notified", 0)) {
            if (canNotify) post(c, 99, tr("New version available"), if (u.versionName.isNotBlank()) tr("Fox-VPN") + " " + u.versionName else tr("Tap to update"))
            p.edit().putInt("upd_notified", u.versionCode).apply()
        }
    }

    private fun post(c: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            c, id, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fox_mono)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { c.getSystemService(NotificationManager::class.java).notify(id, n) }
    }
}

class NoticeJobService : JobService() {
    @Volatile private var thread: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        thread = Thread {
            runCatching { NoticeClient.runBackground(applicationContext) }
            jobFinished(params, false)
        }.also { it.start() }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
