package com.vauth.foxyvpn.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.data.RefInfo
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

/**
 * دعوت دوستان (صفحه‌ی کرکره‌ای): جایزه‌ی هر دو طرف، کد من با «کپی» و «اشتراک»، پیشرفت دعوت‌ها،
 * راهنمای ۳ مرحله‌ای (دوست کد رو کجا وارد می‌کنه) و کارت «کد دوستم رو دارم» با چسباندنِ هوشمند.
 */
@Composable
fun ReferralDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val scope = rememberCoroutineScope()
    val client = remember { RedeemClient(context) }
    val prefs = remember { context.getSharedPreferences("foxvpn_ui", Context.MODE_PRIVATE) }
    var info by remember { mutableStateOf<RefInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var friend by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var msgOk by remember { mutableStateOf(false) }
    var used by remember { mutableStateOf(prefs.getBoolean("ref_used", false)) }
    // کلیپ‌بوردی که شبیه کد دعوت باشه (حروف/عدد ۵ تا ۱۸ کاراکتر، مثلاً F...)؛ یک‌لمسی می‌چسبه
    val clipCode = remember {
        runCatching {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        }.getOrNull()?.trim()?.uppercase()?.takeIf { it.length in 5..18 && it.all { ch -> ch.isLetterOrDigit() } }
    }
    LaunchedEffect(reload) { loaded = false; info = client.referral(); loaded = true }

    fun errText(e: String): String = when (e) {
        "self" -> tr("That's your own code.")
        "already" -> tr("You've already used an invite.")
        "full" -> tr("This friend has reached the invite limit.")
        "same_network" -> tr("This invite can't be used from the same network.")
        "too_old" -> tr("Invites only work in your first days with the app.")
        "disabled" -> tr("Invites are turned off right now.")
        else -> tr("This code isn't valid.")
    }

    fun apply() {
        if (friend.length < 3 || busy) return
        busy = true; msg = null
        scope.launch {
            val r = client.referral(friend)
            busy = false
            when {
                r == null -> { msgOk = false; msg = tr("Could not reach the server. Try again.") }
                r.ok -> { msgOk = true; msg = trf("Done! +%d days added to your plan.", r.gained); friend = ""; info = r; used = true; prefs.edit().putBoolean("ref_used", true).apply() }
                else -> {
                    msgOk = false; msg = errText(r.error)
                    if (r.error == "already") { used = true; prefs.edit().putBoolean("ref_used", true).apply() }
                }
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgBottom,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // سربرگ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CardGiftcard, null, tint = c.accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr("Invite friends"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                    Text(tr("Free days for you and your friend"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                }
            }
            val i = info
            when {
                !loaded -> Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = c.accent, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp)); Text(tr("Loading…"), color = c.textSecondary)
                }
                i == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Could not load. Check your connection and try again."), color = c.textSecondary)
                    PlanButton(tr("Try again"), c, Modifier.fillMaxWidth(), onClick = { reload++ })
                }
                else -> {
                    // جایزه‌ی دو طرف
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RewardChip(c, trf("You get +%d days", i.inviterDays), Modifier.weight(1f))
                        RewardChip(c, trf("Your friend gets +%d days", i.inviteeDays), Modifier.weight(1f))
                    }
                    // کد من
                    val shape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(shape).background(c.card).border(2.dp, c.accent.copy(alpha = 0.45f), shape).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(tr("Your invite code"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.textSecondary)
                        Text(i.refCode, color = c.accentText, fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlanButton(tr("Copy"), c, Modifier.weight(1f), outlined = true, onClick = {
                                runCatching { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", i.refCode)) }
                                Toast.makeText(context, tr("Copied"), Toast.LENGTH_SHORT).show()
                            })
                            PlanButton(tr("Share"), c, Modifier.weight(1f), onClick = {
                                val text = trf("Try Fox-VPN! Install the app, open Settings → Invite friends and enter my code %s. We both get free extra days.", i.refCode)
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                runCatching { context.startActivity(Intent.createChooser(send, tr("Invite friends")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                    .onFailure { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
                            })
                        }
                    }
                    // پیشرفت
                    val frac = if (i.max > 0) (i.count.toFloat() / i.max).coerceIn(0f, 1f) else 0f
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.innerCard).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(trf("Friends joined: %d of %d", i.count, i.max), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(trf("+%d days earned", i.count * i.inviterDays), color = c.accentText, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = c.accent, trackColor = c.accent.copy(alpha = 0.16f))
                        if (i.count == 0) Text(tr("Your first friend is one share away!"), color = c.textSecondary, fontSize = 13.sp)
                        else if (i.max > i.count) Text(trf("%d more invites available", i.max - i.count), color = c.textSecondary, fontSize = 13.sp)
                        else Text(tr("You've reached the maximum. Thank you!"), color = c.textSecondary, fontSize = 13.sp)
                    }
                    // چطور کار می‌کنه
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.innerCard).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(tr("How it works"), color = c.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        StepLine(c, 1, tr("Share your code with a friend."))
                        StepLine(c, 2, tr("They install Fox-VPN, open Settings → Invite friends and enter your code."))
                        StepLine(c, 3, tr("You both get free days right away."))
                    }
                    // کد دوست
                    val fShape = RoundedCornerShape(20.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(fShape).background(c.innerCard).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(tr("Got a friend's code?"), color = c.textPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        if (used && msg == null) {
                            Text(tr("You've already used an invite. Each account can use one."), color = c.textSecondary, fontSize = 13.sp)
                        } else {
                            OutlinedTextField(
                                value = friend, onValueChange = { friend = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(18); msg = null },
                                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                                placeholder = { Text(tr("Friend's code"), color = c.textSecondary.copy(alpha = 0.6f)) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                                    focusedContainerColor = c.bgBottom, unfocusedContainerColor = c.bgBottom, cursorColor = c.accent,
                                ),
                            )
                            if (clipCode != null && friend.isEmpty() && clipCode != i.refCode) {
                                TextButton(onClick = { friend = clipCode }) { Text(trf("Paste %s", clipCode), color = c.accentText, fontWeight = FontWeight.Bold) }
                            }
                            PlanButton(if (busy) tr("Checking…") else tr("Apply"), c, Modifier.fillMaxWidth(), enabled = friend.length > 2 && !busy, onClick = ::apply)
                        }
                        msg?.let { Text(it, color = if (msgOk) c.accentText else androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardChip(c: FoxExtras, text: String, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.clip(shape).background(c.accent.copy(alpha = 0.14f)).border(1.dp, c.accent.copy(alpha = 0.4f), shape).padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = c.accentText, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StepLine(c: FoxExtras, n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Text("$n", color = c.accentText, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(top = 2.dp))
    }
}
