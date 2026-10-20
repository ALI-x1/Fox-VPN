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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.R
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch

// ───────────────────────── رنگ‌ها (از تم برنامه) ─────────────────────────

private data class AdminLoginColors(
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

private fun FoxExtras.toAdminLoginColors() = AdminLoginColors(
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

private fun Modifier.loginCard(c: AdminLoginColors, radius: Dp): Modifier {
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
fun AdminLoginScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var awaitingTwoFactor by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val c = LocalFoxExtras.current.toAdminLoginColors()

    // کدوم فیلد فوکوس داره؟ (برای تشخیص «کیبورد واقعاً بازه»)
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var codeFocused by remember { mutableStateOf(false) }
    // با رفتن بین مرحله‌ی ورود و کد تأیید، فوکوس قبلی پاک می‌شه
    LaunchedEffect(awaitingTwoFactor) {
        emailFocused = false
        passwordFocused = false
        codeFocused = false
    }

    // کیبورد که باز شد، هدر جمع می‌شه تا فیلد پسورد و دکمه پشت کیبورد نمونن.
    // فقط وقتی «واقعاً» باز باشه: یه فیلد فوکوس داشته باشه و ارتفاع کیبورد معنی‌دار باشه.
    // (قبلاً فقط به WindowInsets.isImeVisible تکیه می‌شد؛ تو بعضی گوشی‌ها اون مقدار بدون کیبورد هم
    // true می‌مونه و لوگو و توضیحات برای همیشه مخفی می‌شدن.)
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeVisible = (emailFocused || passwordFocused || codeFocused) &&
        imeBottomPx > with(density) { 120.dp.toPx() }

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

    fun doSubmitLogin() {
        val cleanEmail = FxaAuthRepository.normalizeEmail(email)
        if (cleanEmail.isBlank() || password.isBlank() || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val result = authRepository.startLogin(cleanEmail, password)
            isLoading = false
            result.onSuccess { needsVerification ->
                if (needsVerification) awaitingTwoFactor = true else onSignedIn()
            }.onFailure { errorMessage = it.message ?: "Sign-in failed" }
        }
    }

    fun doSubmitTwoFactor() {
        val cleanCode = FxaAuthRepository.sanitizeCode(code)
        if (cleanCode.isBlank() || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val result = authRepository.submitTwoFactorCode(cleanCode)
            isLoading = false
            result.onSuccess { onSignedIn() }
                .onFailure { errorMessage = it.message ?: "Verification failed" }
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
                        LoginLogo()

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
                    AnimatedContent(
                        targetState = awaitingTwoFactor,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }) togetherWith
                                fadeOut(tween(120))
                        },
                        label = "login-step",
                    ) { twoFactor ->
                        if (!twoFactor) {
                            Column {
                                Text(
                                    "Admin sign-in",
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
                                            "Owner mode: sign in once with the Firefox account that will back your redeem codes.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = c.textSecondary,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(20.dp))

                                LoginField(
                                    value = email,
                                    onValueChange = {
                                        errorMessage = null
                                        email = it
                                            .replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "")
                                            .lowercase()
                                    },
                                    label = "Firefox account email",
                                    onFocusChange = { emailFocused = it },
                                    enabled = !isLoading,
                                    leadingIcon = Icons.Filled.Email,
                                    colors = c,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next,
                                        autoCorrectEnabled = false,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                )
                                Spacer(Modifier.height(12.dp))
                                LoginField(
                                    value = password,
                                    onValueChange = {
                                        errorMessage = null
                                        password = it.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
                                    },
                                    label = "Password",
                                    onFocusChange = { passwordFocused = it },
                                    enabled = !isLoading,
                                    leadingIcon = Icons.Filled.Lock,
                                    colors = c,
                                    visualTransformation = if (passwordVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    trailing = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = c.textSecondary,
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            doSubmitLogin()
                                        }
                                    ),
                                )
                                Spacer(Modifier.height(20.dp))
                                LoginPrimaryButton(
                                    text = "Continue",
                                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                                    isLoading = isLoading,
                                    colors = c,
                                    onClick = {
                                        focusManager.clearFocus()
                                        doSubmitLogin()
                                    },
                                )
                            }
                        } else {
                            Column {
                                Text(
                                    "Verify it's you",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Enter the confirmation code sent to your email",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                )
                                Spacer(Modifier.height(20.dp))

                                LoginField(
                                    value = code,
                                    onValueChange = {
                                        errorMessage = null
                                        code = it.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "")
                                    },
                                    label = "Confirmation code",
                                    onFocusChange = { codeFocused = it },
                                    enabled = !isLoading,
                                    leadingIcon = Icons.Filled.Lock,
                                    colors = c,
                                    textStyle = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 6.sp,
                                        textAlign = TextAlign.Center,
                                        color = c.textPrimary,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            doSubmitTwoFactor()
                                        }
                                    ),
                                )
                                Spacer(Modifier.height(20.dp))
                                LoginPrimaryButton(
                                    text = "Verify",
                                    enabled = !isLoading && code.isNotBlank(),
                                    isLoading = isLoading,
                                    colors = c,
                                    onClick = {
                                        focusManager.clearFocus()
                                        doSubmitTwoFactor()
                                    },
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Use a different account",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = c.accentText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isLoading) {
                                            awaitingTwoFactor = false
                                            code = ""
                                            errorMessage = null
                                        }
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
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
                        "Your credentials are sent only to Mozilla",
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
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: AdminLoginColors,
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
    colors: AdminLoginColors,
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
