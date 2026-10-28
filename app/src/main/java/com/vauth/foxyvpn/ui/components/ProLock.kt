package com.vauth.foxyvpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.LicenseState
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.ui.screens.PlanButton
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras

/**
 * امتیازهای پلن‌ها: چند مورد از هر بخش برای همه آزاده و بقیه فقط با Pro و Max باز می‌شه.
 * قفل فقط ظاهری/تجربه‌ایه؛ اتصال و حجم کاری به اینجا ندارن.
 */
object Perks {
    const val FREE_THEMES = 5
    const val FREE_ICONS = 5
    const val FREE_SERVERS = 5

    /** کشورهای رایگان: آلمان، فنلاند، بریتانیا، ایالات متحده، کره‌ی جنوبی (همه‌ی شهرهاشون برای همه آزاده). */
    val FREE_COUNTRIES = setOf("DE", "FI", "GB", "UK", "US", "KR")
    fun isFreeCountry(code: String?): Boolean = code != null && code.uppercase() in FREE_COUNTRIES

    /** برای کدهای غیر-Compose (مثلاً سرویس VPN): آیا این کاربر مجازه به این کشور وصل بشه؟ (Pro/Max/ادمین، کشور رایگان، یا باز موقت) */
    fun canUseCountry(context: android.content.Context, code: String?): Boolean {
        val app = context.applicationContext
        if (runCatching { RedeemClient(app).adminMode }.getOrDefault(false)) return true
        LicenseState.load(app)
        val plan = LicenseState.status.value?.plan
        if (plan == null || plan == "pro" || plan == "max" || plan == "vip") return true
        return isFreeCountry(code) || com.vauth.foxyvpn.data.PlanConfigStore.cached(app).temp?.covers(code) == true
    }

    /**
     * آیا موارد قفل‌دار باز هستن؟ true برای Pro/Max و ادمین؛ تا وقتی وضعیت لایسنس لود نشده (null)
     * هم true می‌مونه که کاربر پولی موقع بالا آمدن اپ لحظه‌ای قفل نبینه.
     */
    @Composable
    fun rememberUnlocked(): Boolean {
        val context = LocalContext.current
        LaunchedEffect(Unit) { LicenseState.load(context) }
        val lic by LicenseState.status.collectAsState()
        val admin = remember { runCatching { RedeemClient(context.applicationContext).adminMode }.getOrDefault(false) }
        return admin || lic == null || lic?.plan == "pro" || lic?.plan == "max" || lic?.plan == "vip"
    }
}

/** پنجره‌ی کرکره‌ای «این مورد مخصوص Pro و Max است» با دکمه‌ی دیدن پلن‌ها. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProLockSheet(text: String, onSeePlans: () -> Unit, onDismiss: () -> Unit) {
    val c = LocalFoxExtras.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgBottom,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Lock, null, tint = c.accent, modifier = Modifier.size(32.dp)) }
            Text(tr("Pro & Max feature"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(Modifier.height(6.dp))
            PlanButton(tr("See plans"), c, modifier = Modifier.fillMaxWidth(), onClick = onSeePlans)
            TextButton(onClick = onDismiss) { Text(tr("Not now"), color = c.textSecondary, fontWeight = FontWeight.Bold) }
        }
    }
}

/** نشان کوچک قفل + «PRO» برای گوشه‌ی کارت‌های قفل‌دار. */
@Composable
fun ProBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = LocalFoxExtras.current
    if (compact) {
        // فقط یه قفل کوچکِ گرد؛ روی لوگو نمی‌افته
        androidx.compose.foundation.layout.Box(
            modifier = modifier.size(18.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.accent.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Lock, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(11.dp)) }
        return
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.92f)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Lock, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(11.dp))
        Spacer(Modifier.size(3.dp))
        Text("PRO", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}
