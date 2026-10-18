package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.GUARDIAN_ENDPOINT_DEFAULT
import com.vauth.foxyvpn.data.GuardianClient
import com.vauth.foxyvpn.data.formatBytes
import com.vauth.foxyvpn.data.model.Entitlement
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxyStatusColors
import kotlinx.coroutines.launch

// ───────────────────────── رنگ‌ها از تم فعال (LocalFoxExtras) ─────────────────────────

/** رنگ متن/آیکون روی دکمه‌ها و آواتار: تیره روی accent روشن، سفید روی accent تیره. */
private fun onAccentColor(c: FoxExtras): Color =
    if (c.accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White

private fun Modifier.accCard(c: FoxExtras, radius: Dp, color: Color = c.card): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(
            6.dp, shape,
            ambientColor = c.accent.copy(alpha = 0.15f),
            spotColor = c.accent.copy(alpha = 0.15f),
        )
        .clip(shape)
        .background(color)
        .border(1.dp, c.cardBorder, shape)
}

// ───────────────────────── صفحه حساب ─────────────────────────

@Composable
fun AccountScreen(
    authRepository: FxaAuthRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entitlement by remember { mutableStateOf<Entitlement?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val c = LocalFoxExtras.current
    val statusColors = LocalFoxyStatusColors.current
    val onAccent = onAccentColor(c)

    fun refresh() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val accessToken = authRepository.currentAccessToken()
            if (accessToken == null) {
                isLoading = false
                errorMessage = tr("Not signed in.")
                return@launch
            }
            runCatching { GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, accessToken) }
                .onSuccess { entitlement = it }
                .onFailure { errorMessage = it.message ?: tr("Failed to load account info") }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // ───── هدر: بازگشت + عنوان + رفرش ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccRoundButton(c, onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("Back"),
                                tint = c.textPrimary.copy(alpha = 0.75f),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            tr("Account"),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                    }
                    AccRoundButton(c, { if (!isLoading) refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = tr("Refresh"),
                            tint = c.textPrimary.copy(alpha = 0.75f),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ───── کارت پروفایل ─────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .accCard(c, 28.dp)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tr("FOX-VPN Account"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        Text(
                            tr("Signed in to FOX-VPN"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ───── محتوا: لودینگ / خطا / اطلاعات ─────
                when {
                    isLoading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = c.accent, strokeWidth = 3.dp)
                    }

                    errorMessage != null -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .accCard(c, 24.dp)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        AccPrimaryButton(tr("Retry"), c, onClick = { refresh() })
                    }

                    entitlement != null -> {
                        val info = entitlement!!

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .accCard(c, 28.dp)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AccountInfoRow(
                                c = c,
                                icon = Icons.Filled.Star,
                                label = tr("Subscription"),
                                value = if (info.subscribed) tr("Active") else tr("Inactive"),
                                valueColor = if (info.subscribed) statusColors.connected else c.textSecondary,
                            )
                            AccountInfoRow(
                                c = c,
                                icon = Icons.Filled.Person,
                                label = tr("Account ID"),
                                value = info.uid.ifBlank { "\u2014" },
                            )
                            AccountInfoRow(
                                c = c,
                                icon = Icons.Filled.Public,
                                label = tr("Data remaining"),
                                value = when {
                                    !info.limitedBandwidth -> tr("Unlimited")
                                    info.quotaRemaining != null -> formatBytes(info.quotaRemaining)
                                    info.maxBytes != null -> trf("Limited to %s", formatBytes(info.maxBytes))
                                    else -> tr("Limited")
                                },
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        AccPrimaryButton(
                            text = tr("Manage subscription"),
                            c = c,
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(GUARDIAN_ENDPOINT_DEFAULT))
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

@Composable
private fun AccRoundButton(c: FoxExtras, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(c.card)
            .border(1.dp, c.cardBorder, CircleShape),
    ) { content() }
}

@Composable
private fun AccountInfoRow(
    c: FoxExtras,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = c.textPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accCard(c, 18.dp, color = c.innerCard.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.accent.copy(alpha = 0.10f))
                .border(1.dp, c.cardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun AccPrimaryButton(text: String, c: FoxExtras, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                10.dp, shape,
                ambientColor = c.accent.copy(alpha = 0.35f),
                spotColor = c.accent.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(c.accentLight, c.accent)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = onAccentColor(c),
        )
    }
}
