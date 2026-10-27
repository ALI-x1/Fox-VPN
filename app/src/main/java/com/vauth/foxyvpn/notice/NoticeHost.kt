package com.vauth.foxyvpn.notice

import android.content.Intent
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults as BtnDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vauth.foxyvpn.ui.components.FxOverlay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras

/**
 * پنجره‌ی شناور اعلان‌ها و آپدیت. بالای همه‌ی صفحه‌ها گذاشته می‌شه.
 * ترتیب: آپدیت (اجباری: بدون دکمه‌ی بستن) ← اعلان‌های همگانی یکی‌یکی.
 */
@Composable
fun NoticeHost() {
    val ctx = LocalContext.current
    val c = LocalFoxExtras.current
    // اگه دفعه‌ی قبل آپدیت اجباری دیده بودیم، از همون اول (قبل از جواب شبکه) صفحه‌ی آپدیت میاد؛ هوم دیده نمی‌شه و کانکت هم قفله
    var update by remember { mutableStateOf<UpdateInfo?>(NoticeClient.cachedForce(ctx)) }
    var queue by remember { mutableStateOf<List<Notice>>(emptyList()) }

    LaunchedEffect(Unit) {
        NoticeClient.schedule(ctx)
        NoticeClient.ensureChannel(ctx)
        val r = NoticeClient.fetchForDialog(ctx)
        if (r.ok || r.update != null) update = r.update
        // آپدیت اجباری تازه رسید ولی کاربر زودتر وصل شده بود: قطعش کن
        if (r.update?.force == true && FoxyVpnService.state.value != ConnectionState.DISCONNECTED) FoxyVpnService.stop(ctx)
        // بعد از آپدیت اپ: صفحه‌ی «تبریک نسخه‌ی جدید» (یک‌بار)
        val congrats = if (NoticeClient.justUpdated(ctx)) listOf(
            Notice(-1, -1L, tr("Updated to the new version!"), r.whatsNew?.takeIf { it.isNotBlank() } ?: tr("Enjoy the new features and improvements."), "", "screen", false, "open", "fireworks", ""),
        ) else emptyList()
        queue = congrats + r.notices
        // نوتیفِ اعلان‌های تازه همین حالا هم برسه (قبلاً فقط با کار پس‌زمینه‌ی چند ساعته می‌رسید)
        runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { NoticeClient.runBackground(ctx) } }
    }

    // اعلان‌های «موقع وصل‌شدن به VPN»
    LaunchedEffect(Unit) {
        var prev = FoxyVpnService.state.value
        FoxyVpnService.state.collect { s ->
            if (s == ConnectionState.CONNECTED && prev != ConnectionState.CONNECTED) {
                val l = runCatching { NoticeClient.fetchForConnect(ctx) }.getOrDefault(emptyList())
                if (l.isNotEmpty()) queue = queue + l
            }
            prev = s
        }
    }

    fun open(url: String) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    val btn = ButtonDefaults.textButtonColors(contentColor = c.accent)

    val u = update
    // مدیر از ربات روش نمایش آپدیت اختیاری رو انتخاب می‌کنه: screen = اسکرین تمام‌صفحه (با افکت) · window/both = پنجره (بدون افکت) · push = فقط نوتیف
    val uMode = if (u == null || u.force) "screen" else u.mode
    if (u != null && uMode == "screen") {
        NoticeScreen(
            title = if (u.force) tr("Update required") else tr("New version available"),
            badge = u.versionName.takeIf { it.isNotBlank() }?.let { trf("Version %s", it) },
            body = (u.notes.ifBlank { tr("Enjoy the new features and improvements.") }) +
                if (u.force) "\n\n" + tr("You need to install this update to keep using the app.") else "",
            fx = u.fx,
            primary = tr("Install"),
            onPrimary = { if (u.url.isNotBlank()) open(u.url) },
            secondary = if (u.force) null else tr("Later"),
            onSecondary = if (u.force) null else ({ NoticeClient.dismissUpdate(ctx, u); update = null }),
            canDismiss = !u.force,
            onDismiss = { NoticeClient.dismissUpdate(ctx, u); update = null },
        )
        return
    }
    if (u != null && uMode != "push") {
        AlertDialog(
            onDismissRequest = { NoticeClient.dismissUpdate(ctx, u); update = null },
            containerColor = c.innerCard,
            shape = RoundedCornerShape(28.dp),
            titleContentColor = c.textPrimary,
            textContentColor = c.textPrimary,
            title = { Text(tr("New version available") + (u.versionName.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), fontWeight = FontWeight.ExtraBold) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(com.vauth.foxyvpn.i18n.bidi(u.notes.ifBlank { tr("Enjoy the new features and improvements.") })) } },
            confirmButton = { TextButton(onClick = { if (u.url.isNotBlank()) open(u.url); NoticeClient.dismissUpdate(ctx, u); update = null }, colors = btn) { Text(tr("Install"), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { NoticeClient.dismissUpdate(ctx, u); update = null }, colors = btn) { Text(tr("Later")) } },
        )
        return
    }

    val n = queue.firstOrNull() ?: return
    fun next() { NoticeClient.markShown(ctx, n); queue = queue.drop(1) }
    if (n.mode == "screen") {
        NoticeScreen(
            title = n.title, body = n.body, code = n.ccode, fx = n.fx.ifBlank { "confetti" },
            primary = if (n.url.isNotBlank()) tr("Open") else tr("Great!"),
            onPrimary = { if (n.url.isNotBlank()) { open(n.url); next() } else next() },
            secondary = if (n.url.isNotBlank()) tr("Close") else null,
            onSecondary = if (n.url.isNotBlank()) ({ next() }) else null,
            onDismiss = { next() },
        )
        return
    }
    AlertDialog(
        onDismissRequest = { next() },
        containerColor = c.innerCard,
        shape = RoundedCornerShape(28.dp),
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(com.vauth.foxyvpn.i18n.bidi(n.title), fontWeight = FontWeight.ExtraBold) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { if (n.body.isNotBlank()) Text(com.vauth.foxyvpn.i18n.bidi(n.body)) } },
        confirmButton = {
            if (n.url.isNotBlank()) TextButton(onClick = { open(n.url); next() }, colors = btn) { Text(tr("Open"), fontWeight = FontWeight.Bold) }
            else TextButton(onClick = { next() }, colors = btn) { Text(tr("OK"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = if (n.url.isNotBlank()) ({
            TextButton(onClick = { next() }, colors = btn) { Text(tr("Close")) }
        }) else null,
    )
}
