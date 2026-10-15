package com.vauth.foxyvpn.ui.screens

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
import kotlinx.coroutines.launch

// ───────────────────────── پالت رنگی (هماهنگ با HomeScreen) ─────────────────────────

private val AccOrange = Color(0xFFF7941D)
private val AccOrangeDeep = Color(0xFFC2570C)
private val AccOrangeLight = Color(0xFFFFC857)
private val AccGreen = Color(0xFF22B573)

private data class AccColors(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val innerCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentText: Color,
)

private fun accColors(dark: Boolean) = if (dark) {
    AccColors(
        bgTop = Color(0xFF16120E),
        bgBottom = Color(0xFF241B12),
        card = Color(0xFF2A221A),
        cardBorder = Color(0x33FFB066),
        innerCard = Color(0xFF33291F),
        textPrimary = Color(0xFFF5EEE6),
        textSecondary = Color(0xFFB8AB9C),
        accentText = AccOrangeLight,
    )
} else {
    AccColors(
        bgTop = Color(0xFFFFFAF4),
        bgBottom = Color(0xFFFDEBD6),
        card = Color(0xB3FFFFFF),
        cardBorder = Color(0x33F7941D),
        innerCard = Color(0xFFFFF4E8),
        textPrimary = Color(0xFF14172B),
        textSecondary = Color(0xFF5B6275),
        accentText = AccOrangeDeep,
    )
}

private fun Modifier.accCard(c: AccColors, radius: Dp, color: Color = c.card): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(
            6.dp, shape,
            ambientColor = AccOrange.copy(alpha = 0.15f),
            spotColor = AccOrange.copy(alpha = 0.15f),
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val c = accColors(isDark)

    fun refresh() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val accessToken = authRepository.currentAccessToken()
            if (accessToken == null) {
                isLoading = false
                errorMessage = "Not signed in."
                return@launch
            }
            runCatching { GuardianClient().fetchUserInfo(GUARDIAN_ENDPOINT_DEFAULT, accessToken) }
                .onSuccess { entitlement = it }
                .onFailure { errorMessage = it.message ?: "Failed to load account info" }
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
                                contentDescription = "Back",
                                tint = c.textPrimary.copy(alpha = 0.75f),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Account",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                    }
                    AccRoundButton(c, { if (!isLoading) refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
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
                            .background(Brush.linearGradient(listOf(AccOrangeLight, AccOrange))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF3A1F00),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Firefox Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        Text(
                            "Signed in with Mozilla VPN",
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
                        CircularProgressIndicator(color = AccOrange, strokeWidth = 3.dp)
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
                        AccPrimaryButton("Retry", onClick = { refresh() })
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
                                label = "Subscription",
                                value = if (info.subscribed) "Active" else "Inactive",
                                valueColor = if (info.subscribed) AccGreen else c.textSecondary,
                            )
                            AccountInfoRow(
                                c = c,
                                icon = Icons.Filled.Person,
                                label = "Account ID",
                                value = info.uid.ifBlank { "\u2014" },
                            )
                            AccountInfoRow(
                                c = c,
                                icon = Icons.Filled.Public,
                                label = "Data remaining",
                                value = when {
                                    !info.limitedBandwidth -> "Unlimited"
                                    info.quotaRemaining != null -> formatBytes(info.quotaRemaining)
                                    info.maxBytes != null -> "Limited to ${formatBytes(info.maxBytes)}"
                                    else -> "Limited"
                                },
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        AccPrimaryButton(
                            text = "Manage subscription",
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
private fun AccRoundButton(c: AccColors, onClick: () -> Unit, content: @Composable () -> Unit) {
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
    c: AccColors,
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
                .background(AccOrange.copy(alpha = 0.10f))
                .border(1.dp, c.cardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AccOrange, modifier = Modifier.size(22.dp))
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
private fun AccPrimaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                10.dp, shape,
                ambientColor = AccOrange.copy(alpha = 0.35f),
                spotColor = AccOrange.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(AccOrangeLight, AccOrange)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFF3A1F00),
        )
    }
}
