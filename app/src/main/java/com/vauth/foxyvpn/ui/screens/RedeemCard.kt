package com.vauth.foxyvpn.ui.screens

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
                if (planRank(LicenseState.status.value?.plan) > before) onUpgraded()
                else { ok = true; msg = tr("Your code was applied.") }
            } else {
                ok = false; msg = tr("This code can't sign in right now. Please contact support.")
            }
        }
    }

    Box(modifier) {
        RedeemBoxUi(c, code, busy, msg, ok, { code = RedeemClient.formatCode(it); msg = null }, ::apply)
    }
}

@Composable
private fun RedeemBoxUi(
    c: FoxExtras,
    code: String,
    busy: Boolean,
    message: String?,
    ok: Boolean,
    onCode: (String) -> Unit,
    onApply: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val err = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier.fillMaxWidth().planCard(c, 28, c.card, c.accent.copy(alpha = 0.45f), 2).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.VpnKey, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tr("Have a code?"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
                Text(tr("Enter it and your plan upgrades instantly."), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.textSecondary)
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            singleLine = true,
            enabled = !busy,
            placeholder = { Text("FOX-XXXX-XXXX-XXXX", color = c.textSecondary.copy(alpha = 0.6f)) },
            shape = RoundedCornerShape(18.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onApply() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent, unfocusedBorderColor = c.cardBorder,
                focusedContainerColor = c.innerCard, unfocusedContainerColor = c.innerCard,
                cursorColor = c.accent,
            ),
            modifier = Modifier.fillMaxWidth(),
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

