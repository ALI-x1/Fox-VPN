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
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var queue by remember { mutableStateOf<List<Notice>>(emptyList()) }

    LaunchedEffect(Unit) {
        NoticeClient.schedule(ctx)
        NoticeClient.ensureChannel(ctx)
        val r = NoticeClient.fetchForDialog(ctx)
        update = r.update
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
    if (u != null) {
        AlertDialog(
            onDismissRequest = { if (!u.force) { NoticeClient.dismissUpdate(ctx, u); update = null } },
            properties = DialogProperties(dismissOnBackPress = !u.force, dismissOnClickOutside = !u.force),
            containerColor = c.innerCard,
            shape = RoundedCornerShape(28.dp),
            titleContentColor = c.textPrimary,
            textContentColor = c.textPrimary,
            title = { Text(if (u.force) tr("Update required") else tr("New version available"), fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (u.versionName.isNotBlank()) Text(trf("Version %s", u.versionName), color = c.textSecondary)
                    if (u.versionName.isNotBlank() && u.notes.isNotBlank()) Spacer(Modifier.height(8.dp))
                    if (u.notes.isNotBlank()) Text(u.notes)
                    if (u.force) {
                        Spacer(Modifier.height(10.dp))
                        Text(tr("You need to install this update to keep using the app."), color = c.textSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (u.url.isNotBlank()) open(u.url) }, colors = btn) { Text(tr("Install"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = if (u.force) null else ({
                TextButton(onClick = { NoticeClient.dismissUpdate(ctx, u); update = null }, colors = btn) { Text(tr("Later")) }
            }),
        )
        return
    }

    val n = queue.firstOrNull() ?: return
    fun next() { NoticeClient.markShown(ctx, n); queue = queue.drop(1) }
    if (n.mode == "screen") {
        FullScreenNotice(n, onOpen = { open(n.url); next() }, onClose = { next() })
        return
    }
    AlertDialog(
        onDismissRequest = { next() },
        containerColor = c.innerCard,
        shape = RoundedCornerShape(28.dp),
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        title = { Text(n.title, fontWeight = FontWeight.ExtraBold) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { if (n.body.isNotBlank()) Text(n.body) } },
        confirmButton = {
            if (n.url.isNotBlank()) TextButton(onClick = { open(n.url); next() }, colors = btn) { Text(tr("Open"), fontWeight = FontWeight.Bold) }
            else TextButton(onClick = { next() }, colors = btn) { Text(tr("OK"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = if (n.url.isNotBlank()) ({
            TextButton(onClick = { next() }, colors = btn) { Text(tr("Close")) }
        }) else null,
    )
}


/** اسکرین تمام‌صفحه برای تبریک/تخفیف/مناسبت: افکت ایموجی + عنوان + متن + کد قابل‌کپی + دکمه. */
@Composable
private fun FullScreenNotice(n: Notice, onOpen: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalFoxExtras.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    when (n.fx) { "fireworks" -> "🎆"; "hearts" -> "💖"; "snow" -> "❄️"; else -> "🎉" },
                    fontSize = 64.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(n.title, color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                if (n.body.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        Text(n.body, color = c.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 24.sp)
                    }
                }
                if (n.ccode.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    val chip = RoundedCornerShape(18.dp)
                    Row(
                        modifier = Modifier.clip(chip).background(c.accent.copy(alpha = 0.16f)).border(1.dp, c.accent.copy(alpha = 0.55f), chip)
                            .clickable {
                                runCatching {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("code", n.ccode))
                                }
                                Toast.makeText(ctx, tr("Code copied"), Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(n.ccode, color = c.accentText, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(tr("Tap to copy"), color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { if (n.url.isNotBlank()) onOpen() else onClose() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = BtnDefaults.buttonColors(containerColor = c.accent, contentColor = androidx.compose.ui.graphics.Color.White),
                ) { Text(if (n.url.isNotBlank()) tr("Open") else tr("Great!"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) }
                if (n.url.isNotBlank()) TextButton(onClick = onClose) { Text(tr("Close"), color = c.textSecondary) }
            }
            if (n.fx.isNotBlank() && n.fx != "none") FxOverlay(n.fx, durationMs = 5200)
        }
    }
}
