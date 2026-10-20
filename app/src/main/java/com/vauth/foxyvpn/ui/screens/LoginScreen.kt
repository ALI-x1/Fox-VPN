package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.RedeemClient
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

// ───────────────────────── رنگ‌ها (از تم برنامه) ─────────────────────────

private data class LoginColors(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentLight: Color,
    val accentText: Color,
    val onAccent: Color,   // رنگ متن/آیکون روی دکمه‌ی اکسنت (تیره روی اکسنت روشن، سفید روی تیره)
)

private fun FoxExtras.toLoginColors() = LoginColors(
    bgTop = bgTop,
    bgBottom = bgBottom,
    card = card,
    cardBorder = cardBorder,
    field = innerCard,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    accent = accent,
    accentLight = accentLight,
    accentText = accentText,
    onAccent = if (accent.luminance() > 0.3f) Color(0xFF3A1F00) else Color.White,
)

private fun Modifier.loginCard(c: LoginColors, radius: Dp): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(
            6.dp, shape,
            ambientColor = c.accent.copy(alpha = 0.15f),
            spotColor = c.accent.copy(alpha = 0.15f),
        )
        .clip(shape)
        .background(c.card)
        .border(1.dp, c.cardBorder, shape)
}

/** ورود نرم: محو + کمی بالا اومدن؛ فقط تو فاز draw اجرا می‌شه (بدون recompose). */
private fun Modifier.reveal(progress: () -> Float, offsetPx: Float = 40f): Modifier = graphicsLayer {
    val v = progress()
    alpha = v
    translationY = (1f - v) * offsetPx
}

// ───────────────────────── صفحه ورود ─────────────────────────

@Composable
fun LoginScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
    onOpenAdmin: () -> Unit = {},
) {
    // کد فعال‌سازی (قالب FOX-XXXX-XXXX-XXXX)
    var code by remember { mutableStateOf("") }
    // ۵ بار ضربه‌ی پشت‌سرهم روی لوگو: ورود ادمین (فقط برای صاحب برنامه)
    var logoTaps by remember { mutableStateOf(0) }
    var lastLogoTap by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val redeemClient = remember { RedeemClient(context.applicationContext) }

    val c = LocalFoxExtras.current.toLoginColors()

    var codeFocused by remember { mutableStateOf(false) }

    // کیبورد که باز شد، هدر جمع می‌شه تا فیلد و دکمه پشت کیبورد نمونن.
    // فقط وقتی «واقعاً» باز باشه: فیلد فوکوس داشته باشه و ارتفاع کیبورد معنی‌دار باشه.
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeVisible = codeFocused && imeBottomPx > with(density) { 120.dp.toPx() }

    // انیمیشن ورود پله‌ای (هدر ← کارت ← فوتر)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val heroP by animateFloatAsState(if (entered) 1f else 0f, tween(520, easing = FastOutSlowInEasing), label = "hero-in")
    val cardP by animateFloatAsState(if (entered) 1f else 0f, tween(520, delayMillis = 110, easing = FastOutSlowInEasing), label = "card-in")
    val footerP by animateFloatAsState(if (entered) 1f else 0f, tween(520, delayMillis = 220, easing = FastOutSlowInEasing), label = "footer-in")

    // لرزش کوتاه کارت وقتی خطا میاد
    val shake = remember { Animatable(0f) }
    var lastError by remember { mutableStateOf("") }
    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        lastError = msg
        repeat(2) {
            shake.animateTo(10f, tween(45))
            shake.animateTo(-10f, tween(45))
        }
        shake.animateTo(0f, tween(45))
    }

    fun openSupport() {
        runCatching { uriHandler.openUri(RedeemClient.SUPPORT_URL) }
    }

    fun doRedeem(value: String = code) {
        if (!RedeemClient.isComplete(value) || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val grant = redeemClient.redeem(value).getOrElse {
                isLoading = false
                errorMessage = it.message ?: "Could not verify the code"
                return@launch
            }
            val result = authRepository.loginWithRefreshToken(grant.refreshToken)
            isLoading = false
            result.onSuccess { onSignedIn() }
                .onFailure {
                    redeemClient.activeCode = null
                    errorMessage = "This code can't sign in right now. Please contact support."
                }
        }
    }

    // «ادامه با حساب رایگان»: سرور یه کد Free می‌ده و خودکار وارد می‌شیم
    fun doFree() {
        if (isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val free = redeemClient.requestFreeCode().getOrElse {
                isLoading = false
                errorMessage = it.message ?: "Could not create a free account"
                return@launch
            }
            isLoading = false
            doRedeem(free)
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))
                // لمس بیرون از فیلدها کیبورد رو می‌بنده
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ───── لوگو + نام (با باز شدن کیبورد نرم جمع می‌شه) ─────
                AnimatedVisibility(
                    visible = !imeVisible,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
                ) {
                    Column(
                        modifier = Modifier.reveal({ heroP }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    val now = System.currentTimeMillis()
                                    logoTaps = if (now - lastLogoTap < 1_500L) logoTaps + 1 else 1
                                    lastLogoTap = now
                                    if (logoTaps >= 5) {
                                        logoTaps = 0
                                        onOpenAdmin()
                                    }
                                })
                            },
                        ) { LoginLogo() }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = c.textPrimary)) { append("FOX-") }
                                withStyle(SpanStyle(color = c.accent)) { append("VPN") }
                            },
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Secure & Private",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                            letterSpacing = 1.sp,
                        )

                        Spacer(Modifier.height(28.dp))
                    }
                }

                // ───── کارت فرم ─────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .reveal({ cardP })
                        .graphicsLayer { translationX = shake.value }
                        .loginCard(c, 28.dp)
                        .padding(20.dp),
                ) {
                    Column {
                        Text(
                            "Enter your code",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        AnimatedVisibility(
                            visible = !imeVisible,
                            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(100)) + shrinkVertically(tween(180)),
                        ) {
                            Column {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Type the access code you received to start using Fox-VPN.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))

                        LoginField(
                            value = code,
                            onValueChange = {
                                errorMessage = null
                                code = RedeemClient.formatCode(it)
                            },
                            label = "Redeem code",
                            onFocusChange = { codeFocused = it },
                            enabled = !isLoading,
                            leadingIcon = Icons.Filled.VpnKey,
                            colors = c,
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = c.textPrimary,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = false,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    doRedeem()
                                }
                            ),
                        )
                        Spacer(Modifier.height(20.dp))
                        LoginPrimaryButton(
                            text = "Redeem",
                            enabled = !isLoading && RedeemClient.isComplete(code),
                            isLoading = isLoading,
                            colors = c,
                            onClick = {
                                focusManager.clearFocus()
                                doRedeem()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        LoginSecondaryButton(
                            text = "Continue with free account",
                            enabled = !isLoading,
                            colors = c,
                            onClick = {
                                focusManager.clearFocus()
                                doFree()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        SupportLink(
                            text = "Need more data? Message support",
                            colors = c,
                            onClick = ::openSupport,
                        )
                    }

                    // ───── پیام خطا ─────
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: lastError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ───── فوتر ─────
                Row(
                    modifier = Modifier.reveal({ footerP }, 24f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Your code is checked securely",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

@Composable
private fun LoginLogo() {
    val accent = LocalFoxExtras.current.accent
    val pulse = rememberInfiniteTransition(label = "login-pulse")
    val halo by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "login-halo",
    )
    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    scaleX = halo
                    scaleY = halo
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.03f))
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.08f))
                .border(1.dp, accent.copy(alpha = 0.2f), CircleShape)
        )
        Image(
            painter = painterResource(id = R.drawable.ic_fox_logo),
            contentDescription = null,
            modifier = Modifier.size(76.dp),
        )
    }
}

@Composable
private fun SupportLink(text: String, colors: LoginColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.SupportAgent,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = colors.accentText,
        )
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: LoginColors,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default.copy(color = colors.textPrimary, fontSize = 16.sp),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(22.dp))
        },
        trailingIcon = trailing,
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.field,
            unfocusedContainerColor = colors.field.copy(alpha = 0.6f),
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.cardBorder,
            focusedLabelColor = colors.accentText,
            unfocusedLabelColor = colors.textSecondary,
            focusedLeadingIconColor = colors.accent,
            unfocusedLeadingIconColor = colors.textSecondary,
            cursorColor = colors.accent,
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
    )
}

@Composable
private fun LoginPrimaryButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    colors: LoginColors,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val bg = if (enabled || isLoading) {
        Modifier
            .shadow(
                10.dp, shape,
                ambientColor = colors.accent.copy(alpha = 0.35f),
                spotColor = colors.accent.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(colors.accentLight, colors.accent)))
    } else {
        Modifier
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.cardBorder, shape)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(90),
        label = "login-btn-press",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = colors.onAccent,
            )
        } else {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (enabled) colors.onAccent else colors.textSecondary,
            )
        }
    }
}

/** دکمه‌ی دوم (حاشیه‌دار): «ادامه با حساب رایگان». */
@Composable
private fun LoginSecondaryButton(
    text: String,
    enabled: Boolean,
    colors: LoginColors,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.accent.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.accentText else colors.textSecondary,
        )
    }
}
