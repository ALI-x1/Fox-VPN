package com.vauth.foxyvpn.data

import android.content.Context
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.tun.HevSocks5Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * گزارش مصرف به سرور، حین اتصال: هر سه دقیقه یک بار (و یک بار موقع قطع شدن).
 *
 * مصرف از شمارنده‌ی خود تونل می‌آد. اگه اینترنت قطع باشه مصرف تو گوشی جمع می‌شه (حتی با بسته شدن برنامه)
 * و بار بعد فرستاده می‌شه. اگه سرور بگه سقف روزانه/کل پر شده یا کد منقضی/لغو شده، اتصال قطع می‌شه.
 * ادمین (ورود با ایمیل) گزارش نمی‌ده.
 */
object UsageReporter {
    // هر ۳ دقیقه (به‌جای ۱ دقیقه): مصرف درخواست‌های روزانه‌ی سرور یک‌سوم می‌شه. ONLINE_WINDOW سرور باید بیشتر از این باشه.
    private const val INTERVAL_MS = 180_000L
    private const val KEY_PENDING = "pending_bytes"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var job: Job? = null
    private var lastTotal = 0L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("foxvpn_redeem", Context.MODE_PRIVATE)

    /** مجموع بایت‌های رد و بدل‌شده‌ی تونل از شروع اتصال. */
    private fun sampleTotal(): Long = runCatching {
        val s = HevSocks5Tunnel.stats()
        s[1].coerceAtLeast(0) + s[3].coerceAtLeast(0)
    }.getOrDefault(0L)

    private fun addPending(context: Context, delta: Long): Long = synchronized(lock) {
        val p = prefs(context)
        val value = (p.getLong(KEY_PENDING, 0L) + delta).coerceAtLeast(0L)
        p.edit().putLong(KEY_PENDING, value).apply()
        value
    }

    /** مصرف جدید از آخرین نمونه رو به مقدار منتظر اضافه می‌کنه. */
    private fun sample(context: Context) {
        val total = sampleTotal()
        val delta = if (total >= lastTotal) total - lastTotal else total // شمارنده با اتصال جدید از صفر شروع می‌شه
        lastTotal = total
        if (delta > 0) addPending(context, delta)
    }

    private suspend fun send(context: Context, client: RedeemClient, notifyBlock: Boolean) {
        val pending = synchronized(lock) { prefs(context).getLong(KEY_PENDING, 0L) }
        val status = client.reportUsage(pending).getOrNull() ?: return // بدون اینترنت: بعداً دوباره
        if (pending > 0) addPending(context, -pending)
        val block = LicenseState.blockOf(status)
        if (block != null && notifyBlock) FoxyVpnService.disconnectWithMessage(context, block.message)
    }

    /** بعد از وصل شدن VPN صدا زده می‌شه. */
    fun start(context: Context) {
        val app = context.applicationContext
        val client = RedeemClient(app)
        if (client.adminMode || client.activeCode == null) return
        synchronized(lock) {
            if (job?.isActive == true) return
            lastTotal = sampleTotal()
            job = scope.launch {
                while (isActive) {
                    // فاصله رو سرور تعیین می‌کنه (ربات: ⚙️ زمان‌بندی)؛ کمتر = فریز/نگهداری زودتر به کاربر متصل می‌رسه
                    val sec = LicenseState.status.value?.poll?.takeIf { it > 0 }?.coerceIn(30, 600)
                    delay(if (sec != null) sec * 1000L else INTERVAL_MS)
                    sample(app)
                    send(app, client, notifyBlock = true)
                }
            }
        }
    }

    /** موقع قطع شدن: آخرین مصرف ثبت و فرستاده می‌شه. */
    fun finish(context: Context) {
        val app = context.applicationContext
        val running = synchronized(lock) {
            val j = job ?: return
            j.cancel(); job = null; true
        }
        if (!running) return
        sample(app)
        scope.launch { send(app, RedeemClient(app), notifyBlock = false) }
    }
}
