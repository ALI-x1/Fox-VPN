package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.layout.layout
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
import com.vauth.foxyvpn.data.gbLabel
import com.vauth.foxyvpn.data.rememberPlanConfig
import com.vauth.foxyvpn.i18n.trf
import androidx.compose.material.icons.filled.ShoppingCart
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

/**
 * جمع‌شدن نرم (۱ = کامل، ۰ = کاملاً جمع). فقط تو فاز layout/draw اجرا می‌شه؛
 * پس با هر فریم انیمیشن، صفحه recompose نمی‌شه.
 */
private fun Modifier.collapsible(progress: () -> Float): Modifier = this
    .clipToBounds()
    .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val p = progress().coerceIn(0f, 1f)
        val h = (placeable.height * p).roundToInt()
        layout(placeable.width, h) { placeable.place(0, 0) }
    }

// قبلاً با Regex ساخته می‌شد که با «هر حرفِ تایپ‌شده» دوباره کامپایل می‌شد و تایپ رو سنگین می‌کرد
private fun isZeroWidth(ch: Char) = ch in '\u200B'..'\u200D' || ch == '\uFEFF'

private fun String.stripBlankAndZeroWidth(): String =
    filterNot { it.isWhitespace() || Character.isSpaceChar(it) || it == '\u00A0' || isZeroWidth(it) }

private fun String.stripZeroWidth(): String = filterNot { isZeroWidth(it) }

// ───────────────────────── صفحه ورود ─────────────────────────

@Composable
fun LoginScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
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
    val planCfg by rememberPlanConfig()
    val freePlan = planCfg.plan("free")
    val storeBot = planCfg.contacts.bot

    val extras = LocalFoxExtras.current
    val c = remember(extras) { extras.toLoginColors() }

    var codeFocused by remember { mutableStateOf(false) }

    // وقتی یکی از فیلدها فوکوس داره، هدر نرم جمع می‌شه تا فیلدها و دکمه پشت کیبورد نمونن.
    // قبلاً ارتفاع کیبورد (WindowInsets.ime) داخل composition خونده می‌شد؛ یعنی کل صفحه با هر فریمِ
    // انیمیشن کیبورد recompose می‌شد و همین لگ می‌انداخت. حالا فقط فوکوس تعیین‌کننده‌ست و
    // پیشرفت جمع‌شدن فقط تو فاز layout خونده می‌شه (بدون recompose).
    val anyFieldFocused = codeFocused
    val heroExpand by animateFloatAsState(
        targetValue = if (anyFieldFocused) 0f else 1f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "hero-collapse",
    )
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
        onOpenSupport()
    }

    fun doRedeem(value: String = code) {
        if (!RedeemClient.isComplete(value) || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val grant = redeemClient.redeem(value).getOrElse {
                isLoading = false
                errorMessage = it.message ?: tr("Could not verify the code")
                return@launch
            }
            val result = redeemClient.loginWithFailover(grant) { authRepository.loginWithRefreshToken(it) }
            isLoading = false
            result.onSuccess { onSignedIn() }
                .onFailure {
                    redeemClient.activeCode = null
                    // دلیل فنیِ کوتاه (برای پشتیبانی)؛ بدون این، همه‌ی خطاها یک پیام یکسان می‌شدند
                    val why = (it.message ?: it::class.java.simpleName).replace(Regex("\\s+"), " ").replace(Regex("https?://[^/ ]+/_fs-ch-[A-Za-z0-9]+/"), "…/").takeLast(90)
                    errorMessage = tr("This code can't sign in right now. Please contact support.") + "\n[" + why + "]"
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
                errorMessage = it.message ?: tr("Could not create a free account")
                return@launch
            }
            isLoading = false
            doRedeem(free)
        }
    }

    // Scaffold اینست‌ها رو نمی‌گیره؛ خود ستون با safeDrawingPadding (نوار وضعیت + ناوبری + کیبورد) فاصله می‌گیره
    // تا ارتفاع نوار ناوبری با کیبورد دوبار حساب نشه.
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
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
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ───── لوگو + نام (با باز شدن کیبورد نرم جمع می‌شه) ─────
                Column(
                    modifier = Modifier
                        .collapsible { heroExpand },
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
                        text = tr("Secure & Private"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        letterSpacing = 1.sp,
                    )

                    Spacer(Modifier.height(28.dp))
                }

                // ───── کارت فرم ─────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shake.value }
                        .loginCard(c, 28.dp)
                        .padding(20.dp),
                ) {
                    Column {
                        Text(
                            tr("Enter your code"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        Column(modifier = Modifier.collapsible { heroExpand }) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    tr("Type the access code you received to start using Fox-VPN."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                )
                        }
                        Spacer(Modifier.height(20.dp))

                        LoginField(
                            // فیلد فقط ۱۲ حرف خام نگه می‌داره؛ «FOX-» و خط‌تیره‌ها فقط نمایشی‌ان (جلوی حرف‌های اضافه‌ی کیبورد)
                            value = code.removePrefix("FOX-").replace("-", "").let { if (it == "FOX") "" else it },
                            onValueChange = { raw0 ->
                                errorMessage = null
                                val raw = raw0.uppercase().filter { ch -> ch in 'A'..'Z' || ch in '0'..'9' }
                                val b = (if (raw.length > 12 && raw.startsWith("FOX")) raw.drop(3) else raw).take(12)
                                code = if (b.isEmpty()) "" else "FOX-" + b.chunked(4).joinToString("-")
                            },
                            visualTransformation = CodeVisualTransformation,
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
                        val codeReady = RedeemClient.isComplete(code)
                        // وقتی کد کامل نیست، «حساب رایگان» دکمه‌ی اصلیه (اکثر کاربرهای جدید کد ندارن)؛ با کامل شدن کد، «Redeem» اصلی می‌شه
                        LoginPrimaryButton(
                            text = tr("Redeem"),
                            enabled = !isLoading && codeReady,
                            isLoading = isLoading,
                            colors = c,
                            onClick = {
                                focusManager.clearFocus()
                                doRedeem()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        val freeSub = com.vauth.foxyvpn.i18n.bidi(
                            trf("%s · %d days free", gbLabel(freePlan.gb), freePlan.days).replace("\u202B", "").replace("\u202C", ""),
                        )
                        if (codeReady) {
                            LoginSecondaryButton(
                                text = tr("Continue with free account"),
                                subtitle = freeSub,
                                enabled = !isLoading,
                                colors = c,
                                onClick = { focusManager.clearFocus(); doFree() },
                            )
                        } else {
                            LoginPrimaryButton(
                                text = tr("Continue with free account"),
                                subtitle = freeSub,
                                enabled = !isLoading,
                                isLoading = false,
                                colors = c,
                                onClick = { focusManager.clearFocus(); doFree() },
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        SupportLink(
                            text = if (storeBot.isNotBlank()) tr("Need more data? Buy a plan") else tr("Need more data? Message support"),
                            icon = if (storeBot.isNotBlank()) Icons.Filled.ShoppingCart else Icons.Filled.SupportAgent,
                            colors = c,
                            onClick = { if (storeBot.isNotBlank()) runCatching { uriHandler.openUri(tgUrl(storeBot)) } else openSupport() },
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
                        tr("Your code is checked securely"),
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
    // لوگوی ثابت؛ بدون انیمیشن بی‌نهایت (باتری و فریم‌ها آزاد می‌مونن)
    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
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
private fun SupportLink(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: LoginColors, onClick: () -> Unit) {
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
            imageVector = icon,
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
    subtitle: String? = null,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (enabled) colors.onAccent else colors.textSecondary,
                )
                if (subtitle != null) Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = (if (enabled) colors.onAccent else colors.textSecondary).copy(alpha = 0.8f),
                )
            }
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
    subtitle: String? = null,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) colors.accentText else colors.textSecondary,
            )
            if (subtitle != null) Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
            )
        }
    }
}
