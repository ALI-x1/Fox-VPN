package com.vauth.foxyvpn.notice

import android.content.Intent
import android.net.Uri
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
        queue = r.notices
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
