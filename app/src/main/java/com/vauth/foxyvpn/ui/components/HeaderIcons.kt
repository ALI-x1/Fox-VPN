package com.vauth.foxyvpn.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome as FAutoAwesome
import androidx.compose.material.icons.filled.Bolt as FBolt
import androidx.compose.material.icons.filled.CardGiftcard as FGift
import androidx.compose.material.icons.filled.ChatBubble as FChat
import androidx.compose.material.icons.filled.Diamond as FDiamond
import androidx.compose.material.icons.filled.EmojiEvents as FPrize
import androidx.compose.material.icons.filled.Favorite as FHeart
import androidx.compose.material.icons.filled.LocalFireDepartment as FFire
import androidx.compose.material.icons.filled.Notifications as FBell
import androidx.compose.material.icons.filled.Star as FStar
import androidx.compose.material.icons.filled.WorkspacePremium as FCrown
import androidx.compose.material.icons.outlined.AutoAwesome as OAutoAwesome
import androidx.compose.material.icons.outlined.Bolt as OBolt
import androidx.compose.material.icons.outlined.CardGiftcard as OGift
import androidx.compose.material.icons.outlined.ChatBubbleOutline as OChat
import androidx.compose.material.icons.outlined.Diamond as ODiamond
import androidx.compose.material.icons.outlined.EmojiEvents as OPrize
import androidx.compose.material.icons.outlined.FavoriteBorder as OHeart
import androidx.compose.material.icons.outlined.LocalFireDepartment as OFire
import androidx.compose.material.icons.outlined.Notifications as OBell
import androidx.compose.material.icons.outlined.StarBorder as OStar
import androidx.compose.material.icons.outlined.WorkspacePremium as OCrown
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * آیکن‌های دکمه‌ی بالای صفحه‌ی اصلی (انتخاب از ربات با نام انگلیسی؛ بدون ایموجی).
 * حالت عادی = خطی و بی‌رنگ، بعد از لمس = پر و هم‌رنگ تم. «logo» لوگوی خود برنامه است (جدا رسم می‌شه).
 */
object HeaderIcons {
    private val pairs: Map<String, Pair<ImageVector, ImageVector>> = mapOf(
        "heart" to (OHeart to FHeart),
        "gift" to (OGift to FGift),
        "fire" to (OFire to FFire),
        "star" to (OStar to FStar),
        "bolt" to (OBolt to FBolt),
        "crown" to (OCrown to FCrown),
        "prize" to (OPrize to FPrize),
        "diamond" to (ODiamond to FDiamond),
        "bell" to (OBell to FBell),
        "chat" to (OChat to FChat),
        "spark" to (OAutoAwesome to FAutoAwesome),
    )

    fun isKnown(key: String): Boolean = key == "logo" || key.lowercase() in pairs
    fun vector(key: String, filled: Boolean): ImageVector? = pairs[key.lowercase()]?.let { if (filled) it.second else it.first }
}
