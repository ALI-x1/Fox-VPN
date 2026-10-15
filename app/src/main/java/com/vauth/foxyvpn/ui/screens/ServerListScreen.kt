package com.vauth.foxyvpn.ui.screens

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vauth.foxyvpn.data.ProxyStateStore
import com.vauth.foxyvpn.data.RECOMMENDED_COUNTRY_CODE
import com.vauth.foxyvpn.data.ServerListClient
import com.vauth.foxyvpn.data.model.VpnCity
import com.vauth.foxyvpn.data.model.VpnCountry
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.roundToInt

// ───────────────────────── مدل ردیف‌ها ─────────────────────────

private data class CityEntry(val country: VpnCountry, val city: VpnCity) {
    val key: String get() = "${country.code}:${city.code}"
}

private data class PingTarget(val key: String, val host: String, val port: Int)

private enum class PingState { PENDING, DONE, FAILED }

/** هر چند ms بعد از پایان یک دور، دور بعدیِ پینگ شروع بشه. */
private const val LIVE_INTERVAL_MS = 4_000L

/** حداکثر پینگ هم‌زمان؛ که هم سریع باشه هم شبکه خفه نشه. */
private const val PING_PARALLELISM = 8

// ───────────────────────── رنگ‌ها ─────────────────────────
// رنگ‌های کارت/متن/اکسنت از تم برنامه (LocalFoxExtras) می‌آن؛ فقط رنگ‌های «کیفیت پینگ» ثابتن.

private val PingGood = Color(0xFF22B573)
private val PingMedium = Color(0xFFF2A81D)
private val PingBad = Color(0xFFEF5350)

// ───────────────────────── کمکی‌ها ─────────────────────────

/** کد دو حرفی کشور (مثل "us") رو به ایموجی پرچم تبدیل می‌کنه؛ اگه معتبر نبود null. */
private fun flagEmoji(code: String): String? {
    val cc = code.trim().uppercase().let { if (it == "UK") "GB" else it }
    if (cc.length != 2 || !cc.all { it in 'A'..'Z' }) return null
    val base = 0x1F1E6
    return String(Character.toChars(base + (cc[0] - 'A'))) +
        String(Character.toChars(base + (cc[1] - 'A')))
}

/** آی‌دی drawable پرچم (flag_us, flag_de, ...) یا 0 اگه تصویرش تو پروژه نباشه. */
private fun flagResId(context: Context, code: String): Int {
    val cc = code.trim().lowercase().let { if (it == "uk") "gb" else it }
    if (cc.length != 2) return 0
    return context.resources.getIdentifier("flag_$cc", "drawable", context.packageName)
}

private fun pingColor(ms: Int): Color = when {
    ms < 300 -> PingGood
    ms < 500 -> PingMedium
    else -> PingBad
}

private fun pingLevel(ms: Int): Int = when {
    ms < 300 -> 3
    ms < 500 -> 2
    else -> 1
}

// ───────────────────────── صفحه ─────────────────────────

@Composable
fun ServerListScreen(
    serverListClient: ServerListClient,
    proxyStateStore: ProxyStateStore,
    onServerSelected: () -> Unit,
    onBack: () -> Unit,
) {
    var countries by remember { mutableStateOf<List<VpnCountry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedState by proxyStateStore.selectedProxyFlow.collectAsState()

    // پینگِ هموارشده‌ی هر لوکیشن (کلید: "کشور:شهر") و وضعیتش
    val pingResults = remember { mutableStateMapOf<String, Int>() }
    val pingStates = remember { mutableStateMapOf<String, PingState>() }
    // تعداد شکست‌های پشت‌سرهم؛ یه timeout تصادفی نباید سرور خوب رو بندازه ته لیست
    val failCounts = remember { HashMap<String, Int>() }

    // حالت زنده: وقتی روشنه، پینگ‌ها مدام دوباره گرفته می‌شن و لیست جابه‌جا می‌شه
    var isLive by remember { mutableStateOf(true) }

    // با هر بار زدن دکمه‌ی رفرش یکی زیاد می‌شه و یه دور پینگ فوری شروع می‌شه
    var refreshTick by remember { mutableIntStateOf(0) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshTick * 360f,
        animationSpec = tween(durationMillis = 600),
        label = "refresh-rotation",
    )

    LaunchedEffect(Unit) {
        runCatching { serverListClient.fetchCountries() }
            .onSuccess { countries = it }
            .onFailure { errorMessage = it.message ?: "Failed to load server list" }
        isLoading = false
    }

    val recommended = remember(countries) { countries.firstOrNull { it.code == RECOMMENDED_COUNTRY_CODE } }

    val cityEntries = remember(countries) {
        countries
            .filter { it.code != RECOMMENDED_COUNTRY_CODE }
            .sortedBy { it.name }
            .flatMap { country -> country.cities.map { CityEntry(country, it) } }
    }

    val targets = remember(cityEntries) {
        cityEntries.mapNotNull { entry ->
            val target = entry.city.servers.firstOrNull { !it.quarantined }
                ?.let { ServerListClient.defaultConnectTarget(it) }
            target?.let { PingTarget(entry.key, it.first, it.second) }
        }
    }

    // ───── حلقه‌ی زنده‌ی پینگ برای «همه‌ی» لوکیشن‌ها (نه فقط ردیف‌های دیده‌شده) ─────
    LaunchedEffect(targets, refreshTick) {
        val targetKeys = targets.mapTo(HashSet()) { it.key }
        cityEntries.forEach { if (it.key !in targetKeys) pingStates[it.key] = PingState.FAILED }

        val semaphore = Semaphore(PING_PARALLELISM)
        while (true) {
            coroutineScope {
                for (t in targets) {
                    launch {
                        semaphore.withPermit {
                            val ms = PingUtil.measureTcpLatencyMs(t.host, t.port)
                            if (ms != null) {
                                failCounts[t.key] = 0
                                val prev = pingResults[t.key]
                                // میانگین نمایی: عدد نرم عوض می‌شه و ردیف‌ها بی‌دلیل لرزش نمی‌کنن
                                pingResults[t.key] =
                                    if (prev == null) ms else (prev * 0.4f + ms * 0.6f).roundToInt()
                                pingStates[t.key] = PingState.DONE
                            } else {
                                val fails = (failCounts[t.key] ?: 0) + 1
                                failCounts[t.key] = fails
                                // تا دو شکستِ پشت‌سرهم عدد قبلی می‌مونه
                                if (pingResults[t.key] == null || fails >= 2) {
                                    pingResults.remove(t.key)
                                    pingStates[t.key] = PingState.FAILED
                                }
                            }
                        }
                    }
                }
            }
            delay(LIVE_INTERVAL_MS)
            // اگه پاز شده باشه همین‌جا منتظر می‌مونه تا دوباره LIVE بشه
            snapshotFlow { isLive }.first { it }
        }
    }

    // لیست مرتب‌شده: اول کم‌ترین پینگ، بعد در حال سنجش، آخر timeout ها.
    // با هر تغییر پینگ خودکار دوباره محاسبه می‌شه (و animateItem جابه‌جایی رو نرم نشون می‌ده)
    val sortedCities by remember(cityEntries) {
        derivedStateOf {
            cityEntries.sortedWith(
                compareBy<CityEntry>(
                    {
                        when {
                            pingResults.containsKey(it.key) -> 0
                            pingStates[it.key] == PingState.FAILED -> 2
                            else -> 1
                        }
                    },
                    { pingResults[it.key] ?: Int.MAX_VALUE },
                    { it.country.name },
                    { it.city.code },
                ),
            )
        }
    }

    val locationCount = cityEntries.size

    // سریع‌ترین = اولین ردیفِ لیست (وقتی حداقل دو نتیجه داریم که مقایسه معنی داشته باشه)
    val fastestKey by remember {
        derivedStateOf {
            if (pingResults.size >= 2) {
                sortedCities.firstOrNull()?.takeIf { pingResults.containsKey(it.key) }?.key
            } else null
        }
    }

    val c = LocalFoxExtras.current

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom))),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // ───── هدر: برگشت + عنوان + رفرش پینگ ─────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(c = c, onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = c.textPrimary.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose location",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                        if (locationCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$locationCount locations",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary,
                                )
                                Spacer(Modifier.width(10.dp))
                                LiveChip(c = c, live = isLive, onClick = { isLive = !isLive })
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    CircleIconButton(
                        c = c,
                        onClick = { refreshTick++ },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh ping",
                            tint = c.accent,
                            modifier = Modifier.rotate(refreshRotation),
                        )
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        isLoading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = c.accent,
                        )
                        errorMessage != null && countries.isEmpty() -> Text(
                            errorMessage.orEmpty(),
                            color = c.textSecondary,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            recommended?.let { rec ->
                                item(key = "recommended:${rec.code}") {
                                    RecommendedCard(
                                        modifier = Modifier.animateItem(),
                                        c = c,
                                        isSelected = rec.code == selectedState?.countryCode,
                                        onClick = {
                                            val candidates =
                                                ServerListClient.candidatesForCountry(countries, rec.code)
                                            val chosen = candidates.randomOrNull()
                                            if (chosen != null) {
                                                proxyStateStore.save(chosen)
                                                onServerSelected()
                                            } else {
                                                errorMessage = "No available servers for ${rec.name}"
                                            }
                                        },
                                    )
                                }
                            }

                            items(sortedCities, key = { "city:${it.key}" }) { entry ->
                                val country = entry.country
                                val city = entry.city
                                val selected = selectedState

                                CityCard(
                                    modifier = Modifier.animateItem(),
                                    c = c,
                                    countryCode = country.code,
                                    countryName = country.name,
                                    cityLabel = city.name.ifBlank { city.code },
                                    pingMs = pingResults[entry.key],
                                    pingState = pingStates[entry.key] ?: PingState.PENDING,
                                    isFastest = entry.key == fastestKey,
                                    isSelected = country.code == selected?.countryCode &&
                                        city.code == selected?.cityCode,
                                    onClick = {
                                        val candidates = ServerListClient.candidatesForCity(
                                            countries, country.code, city.code,
                                        )
                                        val chosen = candidates.randomOrNull()
                                        if (chosen != null) {
                                            proxyStateStore.save(chosen)
                                            onServerSelected()
                                        } else {
                                            errorMessage = "No available servers in ${city.code}"
                                        }
                                    },
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── کامپوننت‌ها ─────────────────────────

@Composable
private fun CircleIconButton(
    c: FoxExtras,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(c.card)
            .border(1.dp, c.cardBorder, CircleShape),
    ) { content() }
}

/** قالب مشترک کارت‌ها؛ همون استایل کارت‌های هوم‌اسکرین (گوشه گرد + سایه نارنجی ملایم). */
@Composable
private fun LocationCardShell(
    modifier: Modifier = Modifier,
    c: FoxExtras,
    isSelected: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                6.dp,
                shape,
                ambientColor = c.accent.copy(alpha = 0.15f),
                spotColor = c.accent.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(c.card)
            .then(if (highlight) Modifier.background(c.accent.copy(alpha = 0.08f)) else Modifier)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) c.accent else c.cardBorder,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RecommendedCard(
    modifier: Modifier = Modifier,
    c: FoxExtras,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    LocationCardShell(modifier = modifier, c = c, isSelected = isSelected, highlight = true, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(c.accentLight, c.accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recommended",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                text = "Automatically chosen for the best performance",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = c.accent,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun CityCard(
    modifier: Modifier = Modifier,
    c: FoxExtras,
    countryCode: String,
    countryName: String,
    cityLabel: String,
    pingMs: Int?,
    pingState: PingState,
    isFastest: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    LocationCardShell(modifier = modifier, c = c, isSelected = isSelected, onClick = onClick) {
        // پرچم کشور
        val context = LocalContext.current
        val flagRes = remember(countryCode) { flagResId(context, countryCode) }
        val flag = remember(countryCode) { flagEmoji(countryCode) }
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.innerCard)
                .border(1.dp, c.cardBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (flagRes != 0) {
                // تصویر دقیق پرچم (res/drawable-nodpi/flag_xx.png) — مستقل از فونت گوشی
                Image(
                    painter = painterResource(id = flagRes),
                    contentDescription = countryName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 40.dp, height = 30.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
            } else if (flag != null) {
                // اگه تصویر نبود، ایموجی
                Text(text = flag, fontSize = 28.sp)
            } else {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // اسم کشور + شهر
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = countryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isFastest) {
                    Spacer(Modifier.width(8.dp))
                    FastestChip()
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        PingBadge(pingMs = pingMs, pingState = pingState, c = c)

        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = c.accent,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** نشان «LIVE» با نقطه‌ی چشمک‌زن؛ با لمس، به‌روزرسانی زنده رو متوقف/شروع می‌کنه. */
@Composable
private fun LiveChip(c: FoxExtras, live: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live-pulse-alpha",
    )
    val color = if (live) PingGood else c.textSecondary
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(if (live) pulse else 1f)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (live) "LIVE" else "PAUSED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = color,
        )
    }
}

/** نشان کوچک «FASTEST» برای سریع‌ترین لوکیشن. */
@Composable
private fun FastestChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PingGood.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = PingGood,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "FASTEST",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = PingGood,
        )
    }
}

/** پینگ بزرگ با رنگ بر اساس کیفیت (سبز / زرد / قرمز) و میله‌های سیگنال. */
@Composable
private fun PingBadge(pingMs: Int?, pingState: PingState, c: FoxExtras) {
    val color = when {
        pingMs != null -> pingColor(pingMs)
        pingState == PingState.FAILED -> PingBad
        else -> c.textSecondary
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .widthIn(min = 104.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            pingMs != null -> {
                SignalBars(color = color, level = pingLevel(pingMs))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pingMs.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color.copy(alpha = 0.85f),
                )
            }

            pingState == PingState.FAILED -> Text(
                text = "Timeout",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )

            else -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = c.accent,
            )
        }
    }
}

/** سه میله‌ی سیگنال؛ هر چقدر پینگ بهتر، میله‌های روشن بیشتر. */
@Composable
private fun SignalBars(color: Color, level: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(8.dp, 13.dp, 18.dp).forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < level) color else color.copy(alpha = 0.25f)),
            )
        }
    }
}
