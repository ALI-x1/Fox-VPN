package com.vauth.foxyvpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
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
import kotlinx.coroutines.launch

// ───────────────────────── پالت رنگی (هماهنگ با HomeScreen) ─────────────────────────

private val LoginOrange = Color(0xFFF7941D)
private val LoginOrangeDeep = Color(0xFFC2570C)
private val LoginOrangeLight = Color(0xFFFFC857)

private data class LoginColors(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentText: Color,
)

private fun loginColors(dark: Boolean) = if (dark) {
    LoginColors(
        bgTop = Color(0xFF16120E),
        bgBottom = Color(0xFF241B12),
        card = Color(0xFF2A221A),
        cardBorder = Color(0x33FFB066),
        field = Color(0xFF33291F),
        textPrimary = Color(0xFFF5EEE6),
        textSecondary = Color(0xFFB8AB9C),
        accentText = LoginOrangeLight,
    )
} else {
    LoginColors(
        bgTop = Color(0xFFFFFAF4),
        bgBottom = Color(0xFFFDEBD6),
        card = Color(0xB3FFFFFF),
        cardBorder = Color(0x33F7941D),
        field = Color(0xFFFFF4E8),
        textPrimary = Color(0xFF14172B),
        textSecondary = Color(0xFF5B6275),
        accentText = LoginOrangeDeep,
    )
}

private fun Modifier.loginCard(c: LoginColors, radius: Dp): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    return this
        .shadow(
            6.dp, shape,
            ambientColor = LoginOrange.copy(alpha = 0.15f),
            spotColor = LoginOrange.copy(alpha = 0.15f),
        )
        .clip(shape)
        .background(c.card)
        .border(1.dp, c.cardBorder, shape)
}

// ───────────────────────── صفحه ورود ─────────────────────────

@Composable
fun LoginScreen(
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val c = loginColors(isDark)

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
                // ───── لوگو با هاله نارنجی ─────
                LoginLogo()

                Spacer(Modifier.height(14.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = c.textPrimary)) { append("FOX-") }
                        withStyle(SpanStyle(color = LoginOrange)) { append("VPN") }
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

                // ───── کارت فرم ─────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .loginCard(c, 28.dp)
                        .padding(20.dp),
                ) {
                    AnimatedContent(
                        targetState = awaitingTwoFactor,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "login-step",
                    ) { twoFactor ->
                        if (!twoFactor) {
                            Column {
                                Text(
                                    "Sign in with Firefox",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "FoxyVPN runs on the free 50 GB of monthly VPN traffic Mozilla includes with a Firefox account. No subscription is required.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                )
                                Spacer(Modifier.height(20.dp))

                                LoginField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                            .replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "")
                                            .lowercase()
                                    },
                                    label = "Firefox account email",
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
                                        password = it.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
                                    },
                                    label = "Password",
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
                                        code = it.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "")
                                    },
                                    label = "Confirmation code",
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
                    AnimatedVisibility(visible = errorMessage != null) {
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
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ───── فوتر ─────
                Row(verticalAlignment = Alignment.CenterVertically) {
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
    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(LoginOrange.copy(alpha = 0.18f), LoginOrange.copy(alpha = 0.03f))
                    )
                )
                .border(1.dp, LoginOrange.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(LoginOrange.copy(alpha = 0.08f))
                .border(1.dp, LoginOrange.copy(alpha = 0.2f), CircleShape)
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
    colors: LoginColors,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
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
        shape = RoundedCornerShape(18.dp),
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.field,
            unfocusedContainerColor = colors.field.copy(alpha = 0.6f),
            focusedBorderColor = LoginOrange,
            unfocusedBorderColor = colors.cardBorder,
            focusedLabelColor = colors.accentText,
            unfocusedLabelColor = colors.textSecondary,
            focusedLeadingIconColor = LoginOrange,
            unfocusedLeadingIconColor = colors.textSecondary,
            cursorColor = LoginOrange,
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier.fillMaxWidth(),
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
                ambientColor = LoginOrange.copy(alpha = 0.35f),
                spotColor = LoginOrange.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(LoginOrangeLight, LoginOrange)))
    } else {
        Modifier
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.cardBorder, shape)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = Color(0xFF3A1F00),
            )
        } else {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (enabled) Color(0xFF3A1F00) else colors.textSecondary,
            )
        }
    }
}
