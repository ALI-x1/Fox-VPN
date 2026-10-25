package com.vauth.foxyvpn.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.data.RefInfo
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

/** دعوت دوستان: کد من (اشتراک‌گذاری)، تعداد دعوت‌ها، و وارد کردن کد دوست. */
@Composable
fun ReferralDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val scope = rememberCoroutineScope()
    val client = remember { RedeemClient(context) }
    var info by remember { mutableStateOf<RefInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var friend by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { info = client.referral(); loaded = true }

    fun errText(e: String): String = when (e) {
        "self" -> tr("That's your own code.")
        "already" -> tr("You've already used an invite.")
        "full" -> tr("This friend has reached the invite limit.")
        "same_network" -> tr("This invite can't be used from the same network.")
        "too_old" -> tr("Invites only work in your first days with the app.")
        "disabled" -> tr("Invites are turned off right now.")
        else -> tr("This code isn't valid.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.innerCard,
        shape = RoundedCornerShape(24.dp),
        title = { Text(tr("Invite friends"), fontWeight = FontWeight.ExtraBold, color = c.textPrimary) },
        text = {
            val i = info
            when {
                !loaded -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = c.accent, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp)); Text(tr("Loading…"), color = c.textSecondary)
                }
                i == null -> Text(tr("Could not load. Check your connection and try again."), color = c.textSecondary)
                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trf("Share your code. When a friend enters it, you get +%d days and they get +%d days.", i.inviterDays, i.inviteeDays), color = c.textSecondary, fontSize = 14.sp)
                    Text(i.refCode, color = c.accentText, fontWeight = FontWeight.Black, fontSize = 26.sp, modifier = Modifier.fillMaxWidth())
                    Text(trf("Friends joined: %d of %d", i.count, i.max), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(tr("Got a friend's code?"), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = friend, onValueChange = { friend = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(18) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("F…") },
                    )
                    msg?.let { Text(it, color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = {
            val i = info
            if (i != null) Row {
                TextButton(enabled = friend.length > 2 && !busy, onClick = {
                    busy = true; msg = null
                    scope.launch {
                        val r = client.referral(friend)
                        busy = false
                        msg = when {
                            r == null -> tr("Could not reach the server. Try again.")
                            r.ok -> { friend = ""; trf("Done! +%d days added to your plan.", r.gained) }
                            else -> errText(r.error)
                        }
                        if (r?.ok == true) info = r
                    }
                }) { Text(tr("Apply"), fontWeight = FontWeight.Bold, color = c.accent) }
                TextButton(onClick = {
                    val text = trf("Try Fox-VPN! Enter my invite code %s in the app to get free extra days.", i.refCode)
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                    runCatching { context.startActivity(Intent.createChooser(send, tr("Invite friends")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
                }) { Text(tr("Share"), fontWeight = FontWeight.Bold, color = c.accent) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Close"), color = c.textSecondary) } },
    )
}
