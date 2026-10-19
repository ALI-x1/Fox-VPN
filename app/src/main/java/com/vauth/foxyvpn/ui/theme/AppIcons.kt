package com.vauth.foxyvpn.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.i18n.tr

/**
 * آیکون‌های قابل انتخاب برنامه. هر کدوم یک activity-alias تو مانیفسته (مثل تلگرام)؛
 * فقط یکی‌شون فعاله. [title] کلید ترجمه‌ست.
 */
enum class AppIcon(
    val alias: String,
    val title: String,
    val bgTop: Color,
    val bgBottom: Color,
    val fg: Int,
    val zoom: Float,
) {
    DEFAULT("IconDefault", "Default", Color(0xFFFFB34D), Color(0xFFE86A10), R.drawable.ic_launcher_logo, 1.15f),
    BLUE("IconBlue", "Blue", Color(0xFF00041B), Color(0xFF00041B), R.drawable.ic_launcher_fg_blue, 1.4f),
    GOLD("IconGold", "Gold", Color(0xFF110A01), Color(0xFF110A01), R.drawable.ic_launcher_fg_gold, 1.4f),
    SILVER("IconSilver", "Silver", Color(0xFF0A0A0B), Color(0xFF0A0A0B), R.drawable.ic_launcher_fg_silver, 1.4f),
    PURPLE("IconPurple", "Purple", Color(0xFF07001D), Color(0xFF07001D), R.drawable.ic_launcher_fg_purple, 1.4f),
    CYAN("IconCyan", "Cyan", Color(0xFF001114), Color(0xFF001114), R.drawable.ic_launcher_fg_cyan, 1.4f),
    RED("IconRed", "Red", Color(0xFF110101), Color(0xFF110101), R.drawable.ic_launcher_fg_red, 1.4f),
    AURORA("IconAurora", "Aurora", Color(0xFF000323), Color(0xFF000323), R.drawable.ic_launcher_fg_aurora, 1.4f),
    ONYX("IconOnyx", "Onyx", Color(0xFF060503), Color(0xFF060503), R.drawable.ic_launcher_fg_onyx, 1.4f),
    FROST("IconFrost", "Frost", Color(0xFF000324), Color(0xFF000324), R.drawable.ic_launcher_fg_frost, 1.4f),
    INDIGO("IconIndigo", "Indigo", Color(0xFF000000), Color(0xFF000000), R.drawable.ic_launcher_fg_indigo, 1.4f),
    CRYSTAL("IconCrystal", "Crystal", Color(0xFFFFFFFF), Color(0xFFFFFFFF), R.drawable.ic_launcher_fg_crystal, 1.4f),
    EMBER("IconEmber", "Ember", Color(0xFF000000), Color(0xFF000000), R.drawable.ic_launcher_fg_ember, 1.4f),
}

/**
 * عوض کردن آیکون برنامه با فعال/غیرفعال کردن aliasها (بدون بستن برنامه: DONT_KILL_APP).
 * انتخاب کاربر اول «در انتظار» ذخیره می‌شه و وقتی از برنامه بیرون رفت ([applyPending] از onStop) اعمال می‌شه.
 */
object AppIconManager {
    private const val PREFS = "fox_icon_prefs"
    private const val KEY_PENDING = "pending_icon"
    private const val ALIAS_PREFIX = "com.vauth.foxyvpn.alias."

    private fun component(context: Context, icon: AppIcon) = ComponentName(context, ALIAS_PREFIX + icon.alias)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** آیکونی که الان فعاله (پیش‌فرض اگه هیچ aliasی صراحتاً فعال نشده باشه). */
    fun current(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.values().firstOrNull {
            runCatching { pm.getComponentEnabledSetting(component(context, it)) }.getOrNull() ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: AppIcon.DEFAULT
    }

    fun pending(context: Context): AppIcon? =
        prefs(context).getString(KEY_PENDING, null)?.let { name -> AppIcon.values().firstOrNull { it.name == name } }

    /** انتخاب کاربر؛ تا خروج از برنامه فقط ذخیره می‌شه. */
    fun request(context: Context, icon: AppIcon) {
        val edit = prefs(context).edit()
        if (icon == current(context)) edit.remove(KEY_PENDING) else edit.putString(KEY_PENDING, icon.name)
        edit.apply()
    }

    fun applyPending(context: Context) {
        val target = pending(context) ?: return
        prefs(context).edit().remove(KEY_PENDING).apply()
        if (target == current(context)) return
        val pm = context.packageManager
        runCatching {
            // اول جدید فعال می‌شه، بعد بقیه غیرفعال؛ تا هیچ لحظه‌ای آیکون لانچر نداشته باشیم
            pm.setComponentEnabledSetting(
                component(context, target),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            AppIcon.values().filter { it != target }.forEach {
                pm.setComponentEnabledSetting(
                    component(context, it),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}

/** شبکه‌ی انتخاب آیکون برنامه (۳ ستونه). */
@Composable
fun AppIconPickerSection() {
    val context = LocalContext.current
    val c = LocalFoxExtras.current
    var selected by remember { mutableStateOf(AppIconManager.pending(context) ?: AppIconManager.current(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppIcon.values().toList().chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val isSelected = icon == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selected = icon
                                AppIconManager.request(context, icon)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val shape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(shape)
                                .background(Brush.verticalGradient(listOf(icon.bgTop, icon.bgBottom)))
                                .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) c.accent else c.cardBorder, shape),
                        ) {
                            Image(
                                painter = painterResource(icon.fg),
                                contentDescription = tr(icon.title),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(scaleX = icon.zoom, scaleY = icon.zoom),
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(5.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(c.accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = tr(icon.title),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) c.textPrimary else c.textSecondary,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            text = tr("The icon changes when you leave the app, so Fox-VPN stays open while you choose."),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
