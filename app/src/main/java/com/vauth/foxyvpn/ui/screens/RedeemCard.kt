package com.vauth.foxyvpn.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

/** از جاهای دیگه (مثل تنظیمات) می‌شه خواست که پنجره‌ی ردیم همون لحظه باز بشه. */
object RedeemRequest {
    var pending by mutableStateOf(false)
}

private fun planRank(key: String?): Int = when (key) { "free" -> 1; "pro" -> 2; "max" -> 3; else -> 0 }

/**
 * کارت وارد کردن کد (ردیم): هم تو «پلن‌ها» و هم تو «اکانت» و «تنظیمات» استفاده می‌شه.
 * اگه پلنِ کد جدید بالاتر باشه [onUpgraded] صدا زده می‌شه (صفحه‌ی تبریک).
 */
@Composable
fun RedeemCard(
    authRepository: FxaAuthRepository,
    onDisconnect: () -> Unit,
    onUpgraded: () -> Unit,
    modifier: Modifier = Modifier,
    inline: Boolean = false, // true = کارت بازوبسته‌شونده مثل «کد تخفیف» (صفحه‌ی پلن‌ها)
    showCard: Boolean = true, // false = فقط پنجره (برای بازکردن از تنظیمات بدون رفتن به صفحه‌ی دیگر)
) {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    val redeem = remember { RedeemClient(context) }
    val license by LicenseState.status.collectAsState()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(RedeemRequest.pending) { if (RedeemRequest.pending) { RedeemRequest.pending = false; open = true } }

    fun apply() {
        if (!RedeemClient.isComplete(code) || busy) return
        val before = planRank(license?.plan)
        val oldCode = redeem.activeCode
        val releaseOld = redeem.releaseSnapshot()
        busy = true; msg = null
        scope.launch {
            val grant = redeem.redeem(code).getOrElse {
                busy = false; ok = false; msg = it.message ?: tr("Could not verify the code"); return@launch
            }
            onDisconnect()
            val r = authRepository.loginWithRefreshToken(grant.refreshToken)
            busy = false
            if (r.isSuccess) {
                code = ""
                if (redeem.activeCode != oldCode) releaseOld() // جای دستگاه روی کد قبلی آزاد بشه
                if (planRank(LicenseState.status.value?.plan) > before) { open = false; onUpgraded() }
                else { ok = true; msg = tr("Your code was applied.") }
            } else {
                ok = false; msg = tr("This code can't sign in right now. Please contact support.")
            }
        }
    }

    // کارت جمع‌وجور؛ با لمس، پنجره‌ی کرکره‌ای (bottom sheet) برای وارد کردن کد باز می‌شه
    val shape = RoundedCornerShape(28.dp)
    val rot by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, androidx.compose.animation.core.tween(260), label = "redeem-chev")
    if (inline) {
        Column(
            modifier = modifier.fillMaxWidth().planCard(c, 28, c.card, c.cardBorder).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { open = !open; msg = null }, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.VpnKey, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Have a code?"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                    Text(tr("Enter it and your plan upgrades instantly."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
                }
                Icon(Icons.Filled.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(26.dp).rotate(rot))
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = open,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(280)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(280)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)),
            ) {
                RedeemSheetContent(c, code, busy, msg, ok, { code = RedeemClient.formatCode(it); msg = null }, ::apply, compact = true)
            }
        }
    } else if (showCard) {
    Row(
        modifier = modifier.fillMaxWidth().clip(shape).background(c.card).border(2.dp, c.accent.copy(alpha = 0.45f), shape)
            .clickable { msg = null; open = true }.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.VpnKey, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Have a code?"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            Text(tr("Enter it and your plan upgrades instantly."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
        }
        Icon(Icons.Filled.ExpandMore, null, tint = c.textSecondary, modifier = Modifier.size(26.dp).rotate(rot))
    }
    }
    if (open && !inline) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { if (!busy) open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgBottom,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            RedeemSheetContent(c, code, busy, msg, ok, { code = RedeemClient.formatCode(it); msg = null }, ::apply)
        }
    }
}

@Composable
private fun RedeemSheetContent(
    c: FoxExtras,
    code: String,
    busy: Boolean,
    message: String?,
    ok: Boolean,
    onCode: (String) -> Unit,
    onApply: () -> Unit,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val err = MaterialTheme.colorScheme.error
    val focus = remember { FocusRequester() }
    // بعد از تموم شدن انیمیشن باز شدن پنجره، فیلد فوکوس می‌گیره و کیبورد میاد (بدون پرش)
    if (!compact) LaunchedEffect(Unit) { delay(280); runCatching { focus.requestFocus() } }
    Column(
        modifier = if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.VpnKey, null, tint = c.accent, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tr("Redeem a code"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(tr("Enter it and your plan upgrades instantly."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            singleLine = true,
            enabled = !busy,
            placeholder = { Text("FOX-XXXX-XXXX-XXXX", color = c.textSecondary.copy(alpha = 0.6f)) },
            trailingIcon = {
                TextButton(onClick = {
                    val t = runCatching {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    }.getOrNull()
                    if (!t.isNullOrBlank()) onCode(t.trim())
                }, enabled = !busy) { Text(tr("Paste"), color = c.accentText, fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(18.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onApply() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                focusedContainerColor = c.innerCard, unfocusedContainerColor = c.innerCard,
                cursorColor = c.accent,
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        if (message != null) {
            Text(message, color = if (ok) c.accentText else err, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        PlanButton(
            if (busy) tr("Checking…") else tr("Redeem"), c,
            enabled = RedeemClient.isComplete(code) && !busy, modifier = Modifier.fillMaxWidth(), onClick = onApply,
        )
    }
}
