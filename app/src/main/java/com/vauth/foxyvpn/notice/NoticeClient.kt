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
data class Notice(val worker: Int, val id: Long, val title: String, val body: String, val url: String, val mode: String = "both", val rep: Boolean = false, val ev: String = "open", val fx: String = "", val ccode: String = "")

/** اعلان «نسخه‌ی جدید». [force] = اجباری: پنجره بسته نمی‌شه تا کاربر آپدیت کنه. */
data class UpdateInfo(val versionCode: Int, val versionName: String, val notes: String, val url: String, val force: Boolean, val fx: String = "confetti", val rep: Boolean = false, val mode: String = "screen")

/** بنر وضعیت سرور: kind = "maint" (در حال به‌روزرسانی) یا "info"؛ [text] خالی یعنی متن پیش‌فرض اپ. */
data class StatusBanner(val kind: String, val text: String, val id: String = "", val fx: String = "")

/** [banners] = همه‌ی نوارهای فعال (به‌نوبت نشون داده می‌شن)؛ [whatsNew] = متن «تبریک نسخه‌ی جدید» وقتی همین نسخه آخرینه (null = ندارد). */
data class NoticeResult(
    val notices: List<Notice>, val update: UpdateInfo?, val banner: StatusBanner? = null, val updSrc: String = "both", val pm: Int = 60,
    val banners: List<StatusBanner> = emptyList(), val whatsNew: String? = null,
    /** false = هیچ سروری جواب نداد (آفلاین)؛ نتیجه‌ی خالی رو «آپدیتی نیست» حساب نکن. */
    val ok: Boolean = true,
)

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
    private const val JOB_ID = 7302 // 7301 = نسخه‌ی قدیمی با دوره‌ی ۶ ساعته
    private const val OLD_JOB_ID = 7301

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
                    Notice(worker, o.optLong("id"), o.optString("title"), o.optString("body"), o.optString("url"), o.optString("mode", "both"), o.optInt("rep", 0) == 1, o.optString("ev", "open"), o.optString("fx"), o.optString("ccode"))
                }
                val u = j.optJSONObject("update")?.let {
                    UpdateInfo(it.optInt("versionCode"), it.optString("versionName"), it.optString("notes"), it.optString("url"), it.optBoolean("force"), it.optString("fx", "confetti"), it.optBoolean("rep"), it.optString("mode", "screen"))
                }
                val b = j.optJSONObject("banner")?.let { StatusBanner(it.optString("kind"), it.optString("text")) }
                val bArr = j.optJSONArray("banners")
                val bars = (0 until (bArr?.length() ?: 0)).map { i ->
                    val o = bArr!!.getJSONObject(i)
                    StatusBanner(o.optString("kind"), o.optString("text"), o.optString("id"), o.optString("fx"))
                }.filter { it.text.isNotBlank() || it.kind == "maint" }
                val wn = j.optJSONObject("wn")?.optString("notes")
                NoticeResult(list, u, b, j.optString("updSrc", "both"), j.optInt("pm", 60).coerceIn(15, 720), bars, wn)
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
        var banners: List<StatusBanner> = emptyList()
        var whatsNew: String? = null
        var updSrc = "both"
        var pm = 0
        for ((i, b) in bases) {
            val r = query(c, i, b, seenKey, seenOverride) ?: continue
            anyOk = true
            if (banner == null) { banner = r.banner; updSrc = r.updSrc }
            if (banners.isEmpty()) banners = r.banners.ifEmpty { listOfNotNull(r.banner) }
            if (whatsNew == null) whatsNew = r.whatsNew
            if (pm == 0) pm = r.pm
            notices += r.notices
            val u = r.update
            if (u != null && (update == null || u.versionCode > update.versionCode || (u.versionCode == update.versionCode && u.force))) update = u
        }
        // آپدیت از گیت‌هاب: وقتی ووکرها جواب ندادن، یا ادمین «گیت‌هاب» رو انتخاب کرده، یا هیچ‌کدوم مشخص نشده (بالاترین نسخه برنده)
        val gh = if (!anyOk || (updSrc != "bot" && updSrc != "off")) queryFallback(c) else null
        if (gh != null) update = when {
            update == null || gh.versionCode > update.versionCode -> gh
            gh.versionCode == update.versionCode -> update.copy(force = update.force || gh.force)
            else -> update
        }
        if (pm > 0) {
            val p = prefs(c)
            if (p.getInt("job_pm", 60) != pm) { p.edit().putInt("job_pm", pm).apply(); schedule(c) }
        }
        return NoticeResult(notices, update, banner, updSrc, if (pm > 0) pm else 60, banners, whatsNew, anyOk || gh != null)
    }

    /** فقط بنر وضعیت و آپدیت (برای نوار بالای صفحه‌ی اصلی)؛ اعلان‌ها رو مصرف نمی‌کنه. */
    suspend fun fetchStatus(c: Context): NoticeResult = withContext(Dispatchers.IO) { queryAll(c, "shown", 1_000_000_000_000L) }

    /** برای پنجره‌ی داخل برنامه؛ آپدیتِ اختیاریِ قبلاً «بعداً» زده‌شده برنمی‌گرده. */
    suspend fun fetchForDialog(c: Context): NoticeResult = withContext(Dispatchers.IO) {
        val r = queryAll(c, "shown")
        val dismissed = prefs(c).getInt("upd_dismissed", 0)
        // rep = «هر بار ورود»: با «بعداً» فقط تا بستن همین اجرا کنار می‌ره؛ دفعه‌ی بعد که اپ باز بشه دوباره می‌آد
        val u = r.update?.takeIf { it.force || it.rep || it.versionCode > dismissed }
        if (r.ok) cacheForce(c, r.update)
        NoticeResult(launchFilter(r.notices.filter { it.mode != "push" && it.ev != "connect" }), u, whatsNew = r.whatsNew, ok = r.ok)
    }

    /**
     * اعلان‌های «موقع وصل‌شدن به VPN» (ev=connect). فقط‌نوتیف‌ها همین‌جا نوتیف می‌شن، بقیه برای پنجره برمی‌گردن.
     * «یک‌بار» ها یک بار نشون داده می‌شن؛ «هر ورود» ها در هر وصل‌شدن.
     */
    suspend fun fetchForConnect(c: Context): List<Notice> = withContext(Dispatchers.IO) {
        val r = queryAll(c, "shown", 1_000_000_000_000L)
        val seen = prefs(c).getStringSet("cseen", emptySet()) ?: emptySet()
        val list = r.notices.filter { it.ev == "connect" && (it.rep || "${it.worker}:${it.id}" !in seen) }
        val canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ensureChannel(c)
        val (pushOnly, dialogs) = list.partition { it.mode == "push" }
        for (n in pushOnly) {
            if (canNotify) post(c, (n.worker * 1_000_000 + n.id.toInt()).coerceAtLeast(1), n.title, n.body.ifBlank { tr("Tap to open") }, n.url)
            markShown(c, n)
        }
        dialogs
    }

    /** نوتیفِ پوش واقعی (از [com.vauth.foxyvpn.push.FoxPushService]). */
    fun showPush(c: Context, nid: Long, title: String, body: String, url: String) {
        val canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!canNotify) return
        ensureChannel(c)
        post(c, (nid.toInt()).coerceAtLeast(1), title, body.ifBlank { tr("Tap to open") }, url)
    }

    /** بعد از آپدیت اپ، فقط یک‌بار true می‌ده (برای صفحه‌ی «تبریک نسخه‌ی جدید»). نصب تازه false است. */
    fun justUpdated(c: Context): Boolean {
        val p = prefs(c)
        val cur = versionCode(c)
        val last = p.getInt("last_vc", 0)
        if (last != cur) p.edit().putInt("last_vc", cur).apply()
        return last in 1 until cur
    }

    /** نوارهایی که افکتشون قبلاً پخش شده (برای هر نوار فقط یک بار). */
    fun fxSeen(c: Context, id: String): Boolean = prefs(c).getStringSet("fx_seen", emptySet())?.contains(id) == true
    fun markFxSeen(c: Context, id: String) {
        val p = prefs(c)
        val set = HashSet(p.getStringSet("fx_seen", emptySet()) ?: emptySet()); set.add(id)
        p.edit().putStringSet("fx_seen", set.toList().takeLast(40).toSet()).apply()
    }

    /** سرور تو جواب گزارش مصرف خبر می‌ده اعلان نوتیف‌دار جدیدی هست → فوری می‌گیریمش و نوتیف می‌دیم. */
    fun onHint(c: Context, nn: Long) {
        if (nn <= 0) return
        val p = prefs(c)
        if (nn <= p.getLong("hint_nn", 0L)) return
        p.edit().putLong("hint_nn", nn).apply()
        runCatching { runBackground(c) }
    }

    /** اعلان‌های «هر ورود» که تو همین اجرای برنامه نشون داده شدن (با بستن برنامه خالی می‌شه). */
    private val shownThisLaunch = HashSet<String>()

    fun launchFilter(list: List<Notice>): List<Notice> = list.filter { !it.rep || "${it.worker}:${it.id}" !in shownThisLaunch }

    fun markShown(c: Context, n: Notice) {
        if (n.ev == "connect") {
            if (!n.rep) {
                val p = prefs(c)
                val set = HashSet(p.getStringSet("cseen", emptySet()) ?: emptySet()); set.add("${n.worker}:${n.id}")
                p.edit().putStringSet("cseen", set).apply()
            }
            return
        }
        if (n.rep) { shownThisLaunch.add("${n.worker}:${n.id}"); return }
        val p = prefs(c)
        if (n.id > p.getLong("shown_${n.worker}", 0L)) p.edit().putLong("shown_${n.worker}", n.id).apply()
    }

    // ───────── آپدیت اجباری: کش، تا از همون لحظه‌ی باز شدن اپ (قبل از جواب شبکه) صفحه‌ی آپدیت بیاد و اتصال قفل باشه ─────────

    private fun cacheForce(c: Context, u: UpdateInfo?) {
        val e = prefs(c).edit()
        if (u != null && u.force && u.versionCode > versionCode(c)) {
            e.putInt("force_vc", u.versionCode).putString("force_name", u.versionName).putString("force_notes", u.notes)
                .putString("force_url", u.url).putString("force_fx", u.fx)
        } else {
            e.remove("force_vc").remove("force_name").remove("force_notes").remove("force_url").remove("force_fx")
        }
        e.apply()
    }

    /** آپدیت اجباریِ ذخیره‌شده‌ی دفعه‌ی قبل (اگه هنوز نصب نشده)؛ وگرنه null. */
    fun cachedForce(c: Context): UpdateInfo? {
        val p = prefs(c)
        val vc = p.getInt("force_vc", 0)
        if (vc <= versionCode(c)) return null
        return UpdateInfo(vc, p.getString("force_name", "").orEmpty(), p.getString("force_notes", "").orEmpty(), p.getString("force_url", "").orEmpty(), true, p.getString("force_fx", "confetti").orEmpty())
    }

    fun forcePending(c: Context): Boolean = cachedForce(c) != null

    fun dismissUpdate(c: Context, u: UpdateInfo) {
        if (!u.force && !u.rep) prefs(c).edit().putInt("upd_dismissed", u.versionCode).apply()
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
            js.cancel(OLD_JOB_ID)
            val minutes = prefs(c).getInt("job_pm", 60).coerceIn(15, 720)
            val periodMs = minutes * 60_000L
            val pending = js.getPendingJob(JOB_ID)
            if (pending != null && pending.intervalMillis == periodMs) return
            val info = JobInfo.Builder(JOB_ID, ComponentName(c, NoticeJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(periodMs)
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
            if (n.ev == "connect") continue // این‌ها موقع وصل‌شدن به VPN نشون داده می‌شن
            // اعلان‌های «هر ورود» هر بار از سرور برمی‌گردن؛ نوتیف فقط یک بار برای هر اعلان
            val isNew = n.id > p.getLong("notif_${n.worker}", 0L)
            if (isNew && canNotify && n.mode != "window") post(c, (n.worker * 1_000_000 + n.id.toInt()).coerceAtLeast(1), n.title, n.body.ifBlank { tr("Tap to open") }, n.url)
            if (isNew) p.edit().putLong("notif_${n.worker}", n.id).apply()
        }
        val u = r.update
        if (u != null && u.versionCode > p.getInt("upd_notified", 0)) {
            if (canNotify && (u.force || u.mode != "window")) post(c, 99, tr("New version available"), if (u.versionName.isNotBlank()) tr("Fox-VPN") + " " + u.versionName else tr("Tap to update"))
            p.edit().putInt("upd_notified", u.versionCode).apply()
        }
    }

    private fun post(c: Context, id: Int, title: String, text: String, url: String = "") {
        val tap = if (url.startsWith("http")) Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        else Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(
            c, id, tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fox_mono)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
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
