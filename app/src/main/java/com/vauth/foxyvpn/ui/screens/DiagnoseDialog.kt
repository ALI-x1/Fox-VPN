package com.vauth.foxyvpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.DiagItem
import com.vauth.foxyvpn.data.DiagResult
import com.vauth.foxyvpn.data.Diagnostics
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.bidi
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val OkColor = Color(0xFF3DDC84)
private val WarnColor = Color(0xFFFFB020)
private val BadColor = Color(0xFFFF5252)
private fun levelColor(l: Int) = when (l) { 0 -> OkColor; 1 -> WarnColor; else -> BadColor }

/**
 * «بررسی اتصال من» (صفحه‌ی کرکره‌ای): بررسی‌ها یکی‌یکی و زنده ظاهر می‌شن، بالا یک کارت جمع‌بندی با رنگ وضعیت
 * (سالم / کند / مشکل) و توصیه‌ی ساده؛ پایین «دوباره بررسی»، «کپی گزارش» و «ارسال به پشتیبانی».
 */
@Composable
fun DiagnoseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<DiagItem>() }
    var result by remember { mutableStateOf<DiagResult?>(null) }
    var round by remember { mutableStateOf(0) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }

    LaunchedEffect(round) {
        items.clear(); result = null; sent = false
        result = runCatching {
            Diagnostics.run(context) { item -> scope.launch(Dispatchers.Main) { items.add(item) } }
        }.getOrNull()
    }
    val r = result
    val running = r == null
    val worst = items.maxOfOrNull { it.level } ?: 0
    val tone = if (running) c.accent else levelColor(worst)
    val progress by animateFloatAsState(if (running) (items.size.toFloat() / Diagnostics.TOTAL).coerceIn(0.05f, 0.95f) else 1f, tween(350), label = "diag-progress")

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgBottom,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 22.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // سربرگ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Info, null, tint = c.accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr("Connection check"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                    Text(tr("We look at your network, our server and your plan."), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                }
            }
            // کارت جمع‌بندی
            val sShape = RoundedCornerShape(22.dp)
            Column(
                Modifier.fillMaxWidth().animateContentSize().clip(sShape).background(tone.copy(alpha = 0.12f)).border(1.dp, tone.copy(alpha = 0.45f), sShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    when { running -> tr("Checking…"); worst == 0 -> tr("Everything is fine"); worst == 1 -> tr("Works, but slow"); else -> tr("Problem found") },
                    color = tone, fontSize = 20.sp, fontWeight = FontWeight.Black,
                )
                if (running) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = tone, trackColor = tone.copy(alpha = 0.18f))
                else Text(bidi(r!!.advice), color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            // بررسی‌ها (زنده)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateContentSize()) {
                items.forEach { DiagRow(it, c) }
            }
            if (!running) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanButton(tr("Run again"), c, Modifier.weight(1f), outlined = true, onClick = { round++ })
                    PlanButton(if (sent) tr("Sent ✓") else if (sending) tr("Checking…") else tr("Send report to support"), c, Modifier.weight(1f), enabled = !sending && !sent, onClick = {
                        sending = true
                        scope.launch {
                            val okSend = RedeemClient(context).sendReport(r!!.report, supportInfo(context))
                            sending = false; sent = okSend
                            Toast.makeText(context, if (okSend) tr("Report sent. Thank you!") else tr("Could not send the report. Try again later."), Toast.LENGTH_LONG).show()
                        }
                    })
                }
                TextButton(onClick = {
                    runCatching { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("report", r!!.report)) }
                    Toast.makeText(context, tr("Report copied"), Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(tr("Copy report"), color = c.textSecondary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DiagRow(item: DiagItem, c: com.vauth.foxyvpn.ui.theme.FoxExtras) {
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    val col = levelColor(item.level)
    AnimatedVisibility(visibleState = appear, enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 }) {
        val shape = RoundedCornerShape(18.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(c.innerCard).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(col.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(if (item.level == 0) "✓" else if (item.level == 1) "!" else "✕", color = col, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(bidi(item.detail), color = c.textSecondary, fontSize = 13.sp)
            }
        }
    }
}
