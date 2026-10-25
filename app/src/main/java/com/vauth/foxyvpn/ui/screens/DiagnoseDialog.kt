package com.vauth.foxyvpn.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.DiagResult
import com.vauth.foxyvpn.data.Diagnostics
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.bidi
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

/** پنجره‌ی «عیب‌یابی اتصال»: چک می‌کنه و علت رو ساده می‌گه؛ دکمه‌ی ارسال گزارش به پشتیبانی داره. */
@Composable
fun DiagnoseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<DiagResult?>(null) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { result = runCatching { Diagnostics.run(context) }.getOrNull() }
    val ok = Color(0xFF3DDC84); val warn = Color(0xFFFFB020); val bad = Color(0xFFFF5252)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.innerCard,
        shape = RoundedCornerShape(24.dp),
        title = { Text(tr("Connection check"), fontWeight = FontWeight.ExtraBold, color = c.textPrimary) },
        text = {
            val r = result
            if (r == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = c.accent, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Checking…"), color = c.textSecondary)
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.items.forEach {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(if (it.level == 0) "✓" else if (it.level == 1) "!" else "✕", color = if (it.level == 0) ok else if (it.level == 1) warn else bad, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.width(22.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(bidi(it.detail), color = c.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(bidi(r.advice), color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            val r = result
            if (r != null) TextButton(enabled = !sending && !sent, onClick = {
                sending = true
                scope.launch {
                    val okSend = RedeemClient(context).sendReport(r.report, supportInfo(context))
                    sending = false; sent = okSend
                    Toast.makeText(context, if (okSend) tr("Report sent. Thank you!") else tr("Could not send the report. Try again later."), Toast.LENGTH_LONG).show()
                }
            }) { Text(if (sent) tr("Sent ✓") else tr("Send report to support"), fontWeight = FontWeight.Bold, color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close"), color = c.textSecondary) } },
    )
}
