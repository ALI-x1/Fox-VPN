package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.SessionStatus
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val SESSION_RESTORE_TIMEOUT_MS = 8_000L

@Composable
fun SplashScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
    onNeedsLogin: () -> Unit,
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        delay(500)
        val status = withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
            authRepository.restoreSession()
        } ?: SessionStatus.UNREACHABLE

        when (status) {
            SessionStatus.ACTIVE -> onSignedIn()
            SessionStatus.UNREACHABLE -> onSignedIn()
            SessionStatus.NEEDS_LOGIN -> {
                // کلید موزیلا رد شده؛ اگه با کد وارد شده بود، بی‌صدا با همون کد کلید تازه می‌گیریم (نه بیرون‌انداختن از اکانت)
                val recovered = withTimeoutOrNull(12_000L) { com.vauth.foxyvpn.data.SessionRecovery.recover(appContext, authRepository, rotate = true) } ?: false
                if (recovered) onSignedIn() else onNeedsLogin()
            }
        }
    }

    // همون پالت و گرادیان هوم‌اسکرین؛ با عوض شدن تم، اسپلش هم عوض می‌شه
    val c = LocalFoxExtras.current

    // تپش آرام هاله‌ی پشت لوگو
    val pulse = rememberInfiniteTransition(label = "splash-pulse")
    val haloScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splash-halo-scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // لوگوی روباه با دو حلقه‌ی هاله، مثل دکمه‌ی اتصال
            Box(modifier = Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(c.accent.copy(alpha = 0.16f), c.accent.copy(alpha = 0.02f)),
                            ),
                        )
                        .border(1.dp, c.accent.copy(alpha = 0.14f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.08f))
                        .border(1.dp, c.accent.copy(alpha = 0.2f), CircleShape),
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_fox_logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // «FOX-VPN» مثل هدر هوم‌اسکرین
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.textPrimary)) { append("FOX-") }
                    withStyle(SpanStyle(color = c.accent)) { append("VPN") }
                },
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = tr("Secure & Private"),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                letterSpacing = 1.sp,
            )

            Spacer(Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.5.dp,
                color = c.accent,
            )
        }
    }
}
