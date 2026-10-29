package com.vauth.foxyvpn.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

private const val MIN_LEN = 10
private const val MAX_LEN = 1000

/**
 * «گزارش باگ» (صفحه‌ی کرکره‌ای): کاربر نوع مشکل رو انتخاب می‌کنه، توضیح می‌نویسه و می‌فرسته؛
 * به‌همراه نسخه/پلن/مدل گوشی (قابل خاموش کردن) و بدون خودِ کد. سمت ربات اصلی با برچسب «🐞 گزارش باگ» می‌رسه.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BugReportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val scope = rememberCoroutineScope()
    val kinds = listOf("Connection" to "connection", "App crash" to "crash", "Payment / plan" to "plan", "Other" to "other")
    var kind by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    val ready = text.trim().length >= MIN_LEN && !sending

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgBottom,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 22.dp).padding(bottom = 18.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.BugReport, null, tint = c.accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr("Report a bug"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                    Text(tr("Tell us what went wrong — we read every report."), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEachIndexed { i, (label, _) ->
                    val on = i == kind
                    val sh = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.clip(sh).background(if (on) c.accent.copy(alpha = 0.18f) else c.innerCard)
                            .border(1.dp, if (on) c.accent.copy(alpha = 0.6f) else c.cardBorder, sh)
                            .clickable { kind = i }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(tr(label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) c.accentText else c.textPrimary) }
                }
            }
            val fShape = RoundedCornerShape(20.dp)
            Box(Modifier.fillMaxWidth().heightIn(min = 130.dp).clip(fShape).background(c.innerCard).border(1.dp, c.cardBorder, fShape).padding(14.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_LEN) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = true),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(tr("What happened? What did you expect?"), fontSize = 15.sp, color = c.textSecondary)
                        inner()
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Attach device info"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
                    Text(tr("App version, plan and phone model (never your code)"), fontSize = 13.sp, color = c.textSecondary)
                }
                Switch(checked = attach, onCheckedChange = { attach = it })
            }
            PlanButton(
                if (sending) tr("Sending…") else tr("Send report"), c, Modifier.fillMaxWidth(), enabled = ready,
                onClick = {
                    sending = true
                    val body = "#BUG [${kinds[kind].second}]\n" + text.trim()
                    val info = if (attach) supportInfo(context) + " · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}" else "-"
                    scope.launch {
                        val ok = RedeemClient(context).sendReport(body, info)
                        sending = false
                        Toast.makeText(context, if (ok) tr("Report sent. Thank you!") else tr("Could not send the report. Try again later."), Toast.LENGTH_LONG).show()
                        if (ok) onDismiss()
                    }
                },
            )
            if (text.isNotEmpty() && text.trim().length < MIN_LEN) {
                Text(tr("Please write a little more so we can help."), fontSize = 13.sp, color = c.textSecondary)
            }
        }
    }
}
