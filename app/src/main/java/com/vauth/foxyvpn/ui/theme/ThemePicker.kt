package com.vauth.foxyvpn.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * بخش «پالت تم‌ها» برای صفحه‌ی تنظیمات.
 * استفاده: ThemePickerSection(themeController)
 */
@Composable
fun ThemePickerSection(
    themeController: ThemeController,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val selected = themeController.palette
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showTitle) {
            Text(
                text = "پالت تم‌های FOX-VPN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        ThemePalette.values().toList().chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = palette == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { themeController.setPalette(palette) }
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ThemePalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.previewBg)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) palette.swatch else palette.previewSub.copy(alpha = 0.25f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.swatch),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = palette.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.previewText,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = palette.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.previewSub,
            textAlign = TextAlign.End
        )
    }
}
