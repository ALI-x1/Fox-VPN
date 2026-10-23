package com.vauth.foxyvpn.ui.screens

import com.vauth.foxyvpn.i18n.tr
import com.vauth.foxyvpn.i18n.trf
import android.content.Context
import android.widget.Toast
import java.text.Collator
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.vauth.foxyvpn.data.CityNames
import com.vauth.foxyvpn.data.CountryUtil
import com.vauth.foxyvpn.data.ProxyStateStore
import com.vauth.foxyvpn.i18n.Lang
import com.vauth.foxyvpn.data.RECOMMENDED_COUNTRY_CODE
import com.vauth.foxyvpn.data.ServerListClient
import com.vauth.foxyvpn.data.model.ConnectionState
import com.vauth.foxyvpn.data.model.VpnCity
import com.vauth.foxyvpn.data.model.VpnCountry
import com.vauth.foxyvpn.ui.theme.FoxExtras
import com.vauth.foxyvpn.ui.theme.LocalFoxExtras
import com.vauth.foxyvpn.vpn.FoxyVpnService
import com.vauth.foxyvpn.vpn.PingUtil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

// ───────────────────────── مدل ردیف‌ها ─────────────────────────

private data class CityEntry(val country: VpnCountry, val city: VpnCity) {
    val key: String get() = "${country.code}:${city.code}"
}

private data class PingTarget(val key: String, val host: String, val port: Int)

private enum class PingState { PENDING, DONE, FAILED }

/** ترتیب نمایش لیست؛ انتخاب کاربر ذخیره می‌شه. */
private enum class SortMode(val label: String) {
    PING("Fastest ping"),
    COUNTRY("Country (A\u2013Z)"),
    CITY("City (A\u2013Z)"),
}

/** نشان وضعیت روی کارتِ لوکیشن فعلی. */
private enum class CardStatus { CONNECTED, CONNECTING, SELECTED }

/**
 * ذخیره‌ی تنظیمات این صفحه تو SharedPreferences: علاقه‌مندی‌ها، اخیراً استفاده‌شده‌ها و ترتیب لیست.
 * کلید هر لوکیشن: "کشور:شهر".
 */
private object ServerListPrefs {
    private const val PREFS = "fox_favorite_locations"
    private const val KEY_FAVORITES = "locations"
    private const val KEY_RECENTS = "recent_locations"
    private const val KEY_SORT = "sort_mode"
    private const val MAX_RECENTS = 3

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadFavorites(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()

    fun saveFavorites(context: Context, keys: Set<String>) {
        prefs(context).edit().putStringSet(KEY_FAVORITES, HashSet(keys)).apply()
    }

    fun loadRecents(context: Context): List<String> =
        prefs(context).getString(KEY_RECENTS, "")
            ?.split("|")?.filter { it.isNotBlank() }
            ?: emptyList()

    /** لوکیشن رو میاره اول لیست اخیراً‌ها (بدون تکرار) و لیست جدید رو برمی‌گردونه. */
    fun pushRecent(context: Context, key: String): List<String> {
        val updated = (listOf(key) + loadRecents(context).filter { it != key }).take(MAX_RECENTS)
        prefs(context).edit().putString(KEY_RECENTS, updated.joinToString("|")).apply()
        return updated
    }

    fun loadSort(context: Context): SortMode =
        SortMode.entries.firstOrNull { it.name == prefs(context).getString(KEY_SORT, null) } ?: SortMode.PING

    fun saveSort(context: Context, mode: SortMode) {
        prefs(context).edit().putString(KEY_SORT, mode.name).apply()
    }
}

/** هر چند ms بعد از پایان یک دور، دور بعدیِ پینگ شروع بشه. */
private const val LIVE_INTERVAL_MS = 4_000L

/** حداکثر پینگ هم‌زمان؛ که هم سریع باشه هم شبکه خفه نشه. */
private const val PING_PARALLELISM = 6

/** نمونه‌ی پینگ برای هر سرور تو لیست؛ کم‌تر از هوم‌اسکرین تا دور زنده سریع بمونه. */
private const val LIST_PING_SAMPLES = 2

/** برای هر لوکیشن در هر دور حداکثر چند سرور سنجیده بشن (بقیه به‌صورت چرخشی تو دورهای بعد). */
private const val MAX_SERVERS_PER_CITY = 3

/** عدد نمایش‌داده‌شده = بهترین پینگ این تعداد دورِ اخیر (~۱۵ تا ۲۰ ثانیه)؛ جهش‌های لحظه‌ای عدد رو خراب نمی‌کنن. */
private const val PING_HISTORY_ROUNDS = 4

/** سرورهای دور فعلی برای یک لوکیشن؛ اگه سرورها بیشتر از سقف باشن، هر دور یه پنجره‌ی بعدی میاد. */
private fun pickRound(list: List<PingTarget>, round: Int): List<PingTarget> {
    if (list.size <= MAX_SERVERS_PER_CITY) return list
    val start = (round * MAX_SERVERS_PER_CITY) % list.size
    return List(MAX_SERVERS_PER_CITY) { list[(start + it) % list.size] }
}

/** انتخاب سریع‌ترین سرور: حداکثر زمان انتظار، تعداد کاندیدای هم‌زمان و حداکثر کاندیدای سنجش‌شده. */
private const val PICK_TIMEOUT_MS = 2_500L
private const val PICK_PARALLELISM = 12
private const val MAX_PICK_CANDIDATES = 24

/**
 * از بین سرورهای یک لوکیشن، سریع‌ترین (کم‌ترین پینگ TCP مستقیم) رو برمی‌گردونه.
 * اگه هیچ‌کدوم تا مهلت جواب ندادن، یکی تصادفی (مثل رفتار قبلی). [target] میزبان و پورت هر کاندیدا رو می‌ده.
 */
private suspend fun <T> pickFastest(candidates: List<T>, target: (T) -> Pair<String, Int>): T? {
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()
    val pool = if (candidates.size > MAX_PICK_CANDIDATES) {
        candidates.shuffled().take(MAX_PICK_CANDIDATES)
    } else {
        candidates
    }
    val results = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    withTimeoutOrNull(PICK_TIMEOUT_MS) {
        val semaphore = Semaphore(PICK_PARALLELISM)
        coroutineScope {
            pool.forEachIndexed { index, candidate ->
                launch {
                    semaphore.withPermit {
                        val (host, port) = target(candidate)
                        PingUtil.measureTcpLatencyMs(host, port, samples = 1)?.let { results[index] = it }
                    }
                }
            }
        }
    }
    val best = results.entries.minByOrNull { it.value }?.key
    return if (best != null) pool[best] else pool.random()
}

/** اسم فارسی کشورهایی که تو CountryUtil نیستن (مثلاً سوئد). */
private val EXTRA_COUNTRY_FA = mapOf(
    "SE" to "\u0633\u0648\u0626\u062F",
)

/** اسم کشور به زبان برنامه (فارسی → «فرانسه»)؛ اگه نبود همون اسم انگلیسی. */
private fun localCountryName(code: String, fallback: String): String =
    if (Lang.isFa) {
        CountryUtil.countryName(code)
            ?: EXTRA_COUNTRY_FA[code.trim().uppercase(Locale.ROOT)]
            ?: fallback
    } else {
        fallback
    }

/** اسمِ شهرِ نمایشی یک لوکیشن (همونی که زیر اسم کشور دیده می‌شه). */
private fun cityDisplayName(entry: CityEntry): String =
    CityNames.display(entry.country.code, entry.country.name, entry.city.name)
        .ifBlank { entry.city.name.ifBlank { entry.city.code } }

/** مقایسه‌ی الفبایی مطابق زبان برنامه؛ فارسی با Collator درست مرتب می‌شه (پ، چ، ژ، گ سر جاشون). */
private fun appCollator(): Collator =
    Collator.getInstance(if (Lang.isFa) Locale("fa") else Locale.ENGLISH)

/** جستجو روی اسم کشور، اسم/کد شهر و کد کشور. */
private fun CityEntry.matches(query: String): Boolean =
    country.name.contains(query, ignoreCase = true) ||
        localCountryName(country.code, country.name).contains(query, ignoreCase = true) ||
        city.name.contains(query, ignoreCase = true) ||
        CityNames.display(country.code, country.name, city.name).contains(query, ignoreCase = true) ||
        city.code.contains(query, ignoreCase = true) ||
        country.code.equals(query, ignoreCase = true)

/** یک بخش لیست (هدر + ردیف‌ها)؛ اگه ردیفی نباشه چیزی اضافه نمی‌کنه. */
private fun LazyListScope.citySection(
    id: String,
    title: String,
    icon: ImageVector?,
    iconTint: Color,
    c: FoxExtras,
    entries: List<CityEntry>,
    renderCity: @Composable (CityEntry, Modifier) -> Unit,
    showHeader: Boolean = true,
) {
    if (entries.isEmpty()) return
    if (showHeader) {
        item(key = "header:$id") {
            SectionHeader(
                modifier = Modifier.animateItem(),
                c = c,
                title = title,
                count = entries.size,
                icon = icon,
                iconTint = iconTint,
            )
        }
    }
    items(entries, key = { "city:${it.key}" }) { entry ->
        renderCity(entry, Modifier.animateItem())
    }
}

// ───────────────────────── رنگ‌ها ─────────────────────────
// رنگ‌های کارت/متن/اکسنت از تم برنامه (LocalFoxExtras) می‌آن؛ فقط رنگ‌های «کیفیت پینگ» ثابتن.

private val PingGood = Color(0xFF22B573)
private val PingMedium = Color(0xFFF2A81D)
private val PingBad = Color(0xFFEF5350)

/** رنگ ستاره‌ی علاقه‌مندی؛ طلایی که با نارنجی پروژه هماهنگه. */
private val HeartPink = Color(0xFFFF5C8D)

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
    ms < PingUtil.GOOD_MS -> PingGood
    ms < PingUtil.FAIR_MS -> PingMedium
    else -> PingBad
}

private fun pingLevel(ms: Int): Int = PingUtil.level(ms)

/** بخش‌های لیست (کلاس معمولی، نه data class؛ تا مقایسه‌ی عمیق لیست‌ها انجام نشه). */
private class ListSections(
    val searching: Boolean,
    val query: String,
    val visible: List<CityEntry>,
    val current: List<CityEntry>,
    val favorites: List<CityEntry>,
    val recent: List<CityEntry>,
    val others: List<CityEntry>,
) {
    val hasSections: Boolean get() = current.isNotEmpty() || favorites.isNotEmpty() || recent.isNotEmpty()
}

/** هر لوکیشن فقط یک جا میاد: Current > Favorites > Recent > All. */
private fun buildSections(
    sorted: List<CityEntry>,
    rawQuery: String,
    favorites: Set<String>,
    recents: List<String>,
    currentEntry: CityEntry?,
): ListSections {
    val query = rawQuery.trim()
    val searching = query.isNotEmpty()
    val visible = if (searching) sorted.filter { it.matches(query) } else sorted
    val current = if (!searching && currentEntry != null) listOf(currentEntry) else emptyList()
    val currentKey = current.firstOrNull()?.key
    val favs = if (searching) emptyList() else visible.filter { it.key in favorites && it.key != currentKey }
    val byKey = if (searching) emptyMap() else visible.associateBy { it.key }
    val recent = if (searching) {
        emptyList()
    } else {
        recents.mapNotNull { byKey[it] }.filter { it.key !in favorites && it.key != currentKey }
    }
    val pinned = HashSet<String>().apply {
        currentKey?.let { add(it) }
        favs.forEach { add(it.key) }
        recent.forEach { add(it.key) }
    }
    val others = if (searching) visible else visible.filter { it.key !in pinned }
    return ListSections(searching, query, visible, current, favs, recent, others)
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
    val vpnState by FoxyVpnService.state.collectAsState()

    // پینگ از شبکه‌ی مستقیم گوشی (نه تونل VPN) گرفته بشه
    val context = LocalContext.current
    // یک بار (نه با هر recompose)
    remember { PingUtil.init(context) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // وقتی برنامه تو پس‌زمینه‌ست (یا صفحه خاموشه) پینگ‌های زنده متوقف می‌شن تا باتری و دیتا هدر نره
    val lifecycleOwner = LocalContext.current as? LifecycleOwner
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        if (lifecycle == null) {
            onDispose { }
        } else {
            isForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> isForeground = true
                    Lifecycle.Event.ON_STOP -> isForeground = false
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }
    val listState = rememberLazyListState()

    // علاقه‌مندی‌ها (ستاره)، اخیراً استفاده‌شده‌ها، ترتیب لیست و متن جستجو
    var favorites by remember { mutableStateOf<Set<String>>(ServerListPrefs.loadFavorites(context)) }
    var recents by remember { mutableStateOf(ServerListPrefs.loadRecents(context)) }
    var sortMode by remember { mutableStateOf(ServerListPrefs.loadSort(context)) }
    var searchQuery by remember { mutableStateOf("") }

    // true وقتی داریم سریع‌ترین سرورِ لوکیشن انتخاب‌شده رو پیدا می‌کنیم (لمس‌ها قفل می‌شن)
    var isPicking by remember { mutableStateOf(false) }

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

    // با هر بار «تلاش مجدد» یکی زیاد می‌شه و لیست دوباره گرفته می‌شه
    var loadTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(loadTick) {
        isLoading = true
        errorMessage = null
        runCatching { serverListClient.fetchCountries() }
            .onSuccess { countries = it }
            .onFailure { errorMessage = it.message ?: tr("Failed to load server list") }
        isLoading = false
    }

    // خطای انتخاب سرور (مثلاً «سروری در دسترس نیست») تا حالا وقتی لیست پر بود هیچ‌جا نشون داده نمی‌شد
    LaunchedEffect(errorMessage, countries) {
        val msg = errorMessage
        if (msg != null && countries.isNotEmpty()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    val recommended = remember(countries) { countries.firstOrNull { it.code == RECOMMENDED_COUNTRY_CODE } }

    val cityEntries = remember(countries) {
        countries
            .filter { it.code != RECOMMENDED_COUNTRY_CODE }
            .sortedBy { it.name }
            .flatMap { country -> country.cities.map { CityEntry(country, it) } }
    }

    // برای هر لوکیشن «چند» سرور سنجیده می‌شه (نه فقط اولی)؛ عدد لوکیشن = بهترین سرورِ اون لوکیشن،
    // دقیقاً همونی که موقع انتخاب هم (pickFastest) برای اتصال برداشته می‌شه.
    val cityTargets = remember(cityEntries) {
        cityEntries
            .associate { entry ->
                entry.key to entry.city.servers
                    .filter { !it.quarantined }
                    .mapNotNull { ServerListClient.defaultConnectTarget(it) }
                    .distinct()
                    .map { PingTarget(entry.key, it.first, it.second) }
            }
            .filterValues { it.isNotEmpty() }
    }

    // ───── حلقه‌ی زنده‌ی پینگ برای «همه‌ی» لوکیشن‌ها (نه فقط ردیف‌های دیده‌شده) ─────
    // نتیجه‌ها اول تو نقشه‌های محلی جمع می‌شن و دسته‌ای (حداکثر هر ~۰٫۵ ثانیه، در یک snapshot) تو state نوشته می‌شن؛
    // قبلاً هر نتیجه جدا لیست رو دوباره مرتب می‌کرد. وسط اسکرول هم نوشته نمی‌شه تا لیست زیر انگشت جابه‌جا نشه.
    LaunchedEffect(cityTargets, refreshTick) {
        val localResults = HashMap<String, Int>(pingResults)
        val localStates = HashMap<String, PingState>(pingStates)
        val dirty = HashSet<String>()
        var lastFlush = 0L

        fun flush() {
            if (dirty.isEmpty()) return
            Snapshot.withMutableSnapshot {
                for (k in dirty) {
                    val r = localResults[k]
                    if (r != null) pingResults[k] = r else pingResults.remove(k)
                    localStates[k]?.let { pingStates[k] = it }
                }
            }
            dirty.clear()
            lastFlush = System.currentTimeMillis()
        }

        cityEntries.forEach {
            if (it.key !in cityTargets) {
                localStates[it.key] = PingState.FAILED
                dirty += it.key
            }
        }
        flush()

        val semaphore = Semaphore(PING_PARALLELISM)
        // بهترین پینگِ هر لوکیشن تو چند دور اخیر
        val history = HashMap<String, ArrayDeque<Int>>()
        var round = 0
        while (true) {
            // پاز: برنامه تو پس‌زمینه‌ست یا «Live» خاموشه
            snapshotFlow { isLive && isForeground }.first { it }

            // بهترین پینگ هر لوکیشن تو همین دور (همه‌ی کدها روی ترد اصلی اجرا می‌شن؛ نیازی به قفل نیست)
            val roundBest = HashMap<String, Int>()
            coroutineScope {
                for ((key, list) in cityTargets) {
                    for (t in pickRound(list, round)) {
                        launch {
                            semaphore.withPermit {
                                val ms = PingUtil.measureTcpLatencyMs(t.host, t.port, samples = LIST_PING_SAMPLES)
                                if (ms != null) {
                                    val best = minOf(roundBest[key] ?: ms, ms)
                                    roundBest[key] = best
                                    val past = history[key]?.minOrNull()
                                    localResults[key] = if (past == null) best else minOf(past, best)
                                    localStates[key] = PingState.DONE
                                    dirty += key
                                    if (System.currentTimeMillis() - lastFlush >= 500L && !listState.isScrollInProgress) flush()
                                }
                            }
                        }
                    }
                }
            }

            // پایان دور: ثبت تو تاریخچه، یا حساب شکست‌ها
            for (key in cityTargets.keys) {
                val best = roundBest[key]
                if (best != null) {
                    failCounts[key] = 0
                    val window = history.getOrPut(key) { ArrayDeque() }
                    window.addLast(best)
                    while (window.size > PING_HISTORY_ROUNDS) window.removeFirst()
                } else {
                    // یه دور بدون جواب نباید لوکیشن خوب رو بندازه ته لیست؛ دو دور پشت‌سرهم یعنی در دسترس نیست
                    val fails = (failCounts[key] ?: 0) + 1
                    failCounts[key] = fails
                    if (localResults[key] == null || fails >= 2) {
                        localResults.remove(key)
                        localStates[key] = PingState.FAILED
                        dirty += key
                        history.remove(key)
                    }
                }
            }
            // اگه کاربر داره اسکرول می‌کنه صبر می‌کنیم تا ول کنه، بعد همه‌ی تغییرها یک‌جا اعمال می‌شن
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            flush()

            round++
            delay(LIVE_INTERVAL_MS)
        }
    }

    // ترتیب لیست بر اساس انتخاب کاربر. حالت پینگ: اول کم‌ترین پینگ، بعد در حال سنجش، آخر timeout ها.
    // با هر تغییر پینگ خودکار دوباره محاسبه می‌شه (و animateItem جابه‌جایی رو نرم نشون می‌ده)
    val sortedCities by remember(cityEntries, sortMode, Lang.isFa) {
        derivedStateOf {
            when (sortMode) {
                SortMode.PING -> cityEntries.sortedWith(
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
                SortMode.COUNTRY -> {
                    val collator = appCollator()
                    cityEntries.sortedWith(
                        Comparator { a, b ->
                            val byCountry = collator.compare(
                                localCountryName(a.country.code, a.country.name),
                                localCountryName(b.country.code, b.country.name),
                            )
                            if (byCountry != 0) byCountry else a.city.code.compareTo(b.city.code)
                        },
                    )
                }
                SortMode.CITY -> {
                    // بر اساس اسم شهرِ سرور (زیر اسم کشور)؛ اگه دو شهر هم‌نام بودن، بر اساس کشور
                    val collator = appCollator()
                    cityEntries.sortedWith(
                        Comparator { a, b ->
                            val byCity = collator.compare(cityDisplayName(a), cityDisplayName(b))
                            if (byCity != 0) {
                                byCity
                            } else {
                                collator.compare(
                                    localCountryName(a.country.code, a.country.name),
                                    localCountryName(b.country.code, b.country.name),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    val locationCount = cityEntries.size

    // سریع‌ترین = کم‌ترین پینگ بین همه‌ی لوکیشن‌ها (وقتی حداقل دو نتیجه داریم که مقایسه معنی داشته باشه)
    val fastestKey by remember {
        derivedStateOf {
            if (pingResults.size >= 2) pingResults.minByOrNull { it.value }?.key else null
        }
    }

    val c = LocalFoxExtras.current

    // ───── لوکیشنی که الان انتخاب/وصله ─────
    val currentEntry = remember(cityEntries, selectedState) {
        val sel = selectedState
        if (sel == null) {
            null
        } else {
            cityEntries.firstOrNull { it.country.code == sel.countryCode && it.city.code == sel.cityCode }
        }
    }
    val currentEntryState = rememberUpdatedState(currentEntry)
    // sortedCities با عوض شدن cityEntries / sortMode «State جدید» می‌سازه؛ اگه derivedStateOf پایین مستقیم
    // اون رو بگیره، همیشه نسخه‌ی اول (لیست خالی قبل از لود) رو می‌خونه و هیچ لوکیشنی نشون داده نمی‌شه.
    val sortedCitiesState = rememberUpdatedState(sortedCities)

    // ───── بخش‌های لیست ─────
    // فقط وقتی ترتیب، جستجو، علاقه‌مندی‌ها، اخیراً‌ها یا لوکیشن فعلی عوض بشن دوباره ساخته می‌شه
    // (نه با هر recompose، و نه با هر نتیجه‌ی پینگ که ترتیب رو عوض نمی‌کنه)
    val sections by remember {
        derivedStateOf { buildSections(sortedCitiesState.value, searchQuery, favorites, recents, currentEntryState.value) }
    }
    val query = sections.query
    val searching = sections.searching
    val visibleCities = sections.visible
    val currentCities = sections.current
    val favoriteCities = sections.favorites
    val recentCities = sections.recent
    val otherCities = sections.others
    val hasSections = sections.hasSections

    // ───── اکشن‌ها ─────
    val toggleFavorite: (String) -> Unit = { key ->
        haptics.performHapticFeedback(if (key in favorites) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        favorites = if (key in favorites) favorites - key else favorites + key
        ServerListPrefs.saveFavorites(context, favorites)
    }

    // انتخاب لوکیشن: از بین سرورهاش سریع‌ترین انتخاب می‌شه (نه تصادفی)
    val selectCity: (CityEntry) -> Unit = { entry ->
        if (!isPicking) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            isPicking = true
            scope.launch {
                val candidates = ServerListClient.candidatesForCity(countries, entry.country.code, entry.city.code)
                // میزبان/پورت هر کاندیدا؛ اگه اسم فیلدها تو مدل شما فرق داره فقط همین یک خط رو عوض کن
                val chosen = pickFastest(candidates) { it.host to it.port }
                isPicking = false
                if (chosen != null) {
                    recents = ServerListPrefs.pushRecent(context, entry.key)
                    proxyStateStore.save(chosen)
                    onServerSelected()
                } else {
                    errorMessage = trf("No available servers in %s", entry.city.code)
                }
            }
        }
    }

    val selectRecommended: (VpnCountry) -> Unit = { rec ->
        if (!isPicking) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            isPicking = true
            scope.launch {
                val candidates = ServerListClient.candidatesForCountry(countries, rec.code)
                val chosen = pickFastest(candidates) { it.host to it.port }
                isPicking = false
                if (chosen != null) {
                    proxyStateStore.save(chosen)
                    onServerSelected()
                } else {
                    errorMessage = trf("No available servers for %s", rec.name)
                }
            }
        }
    }

    val renderCity: @Composable (CityEntry, Modifier) -> Unit = { entry, itemModifier ->
        val selected = selectedState
        val isSelected = entry.country.code == selected?.countryCode &&
            entry.city.code == selected?.cityCode
        CityCard(
            modifier = itemModifier,
            c = c,
            countryCode = entry.country.code,
            countryName = localCountryName(entry.country.code, entry.country.name),
            cityLabel = CityNames.display(entry.country.code, entry.country.name, entry.city.name),
            pingMs = pingResults[entry.key],
            pingState = pingStates[entry.key] ?: PingState.PENDING,
            isFastest = entry.key == fastestKey,
            isSelected = isSelected,
            status = if (isSelected) {
                when (vpnState) {
                    ConnectionState.CONNECTED -> CardStatus.CONNECTED
                    ConnectionState.CONNECTING -> CardStatus.CONNECTING
                    else -> CardStatus.SELECTED
                }
            } else {
                null
            },
            isFavorite = entry.key in favorites,
            onClick = { selectCity(entry) },
            onToggleFavorite = { toggleFavorite(entry.key) },
        )
    }

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
                            contentDescription = tr("Back"),
                            tint = c.textPrimary.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Choose location"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = c.textPrimary,
                        )
                        if (locationCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trf("%d locations", locationCount),
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
                            contentDescription = tr("Refresh ping"),
                            tint = c.accent,
                            modifier = Modifier.rotate(refreshRotation),
                        )
                    }
                }

                // ───── جستجو + ترتیب لیست ─────
                if (locationCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchField(
                            c = c,
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        SortButton(
                            c = c,
                            mode = sortMode,
                            onSelected = { mode ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sortMode = mode
                                ServerListPrefs.saveSort(context, mode)
                            },
                        )
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        isLoading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = c.accent,
                        )
                        errorMessage != null && countries.isEmpty() -> Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                errorMessage.orEmpty(),
                                color = c.textSecondary,
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { loadTick++ }) {
                                Text(tr("Retry"), color = c.accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        else -> LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (!searching) {
                                recommended?.let { rec ->
                                    item(key = "recommended:${rec.code}") {
                                        RecommendedCard(
                                            modifier = Modifier.animateItem(),
                                            c = c,
                                            isSelected = rec.code == selectedState?.countryCode,
                                            onClick = { selectRecommended(rec) },
                                        )
                                    }
                                }
                            }

                            if (searching) {
                                if (visibleCities.isEmpty()) {
                                    item(key = "empty-search") { NoResults(c = c, query = query) }
                                }
                                items(visibleCities, key = { "city:${it.key}" }) { entry ->
                                    renderCity(entry, Modifier.animateItem())
                                }
                            } else {
                                citySection("current", tr("Current location"), Icons.Filled.CheckCircle, c.accent, c, currentCities, renderCity)
                                citySection("favorites", tr("Favorites"), Icons.Filled.Favorite, HeartPink, c, favoriteCities, renderCity)
                                citySection("recent", tr("Recent"), Icons.Filled.History, c.accentText, c, recentCities, renderCity)
                                citySection(
                                    id = "all",
                                    title = tr("All locations"),
                                    icon = null,
                                    iconTint = c.accentText,
                                    c = c,
                                    entries = otherCities,
                                    renderCity = renderCity,
                                    showHeader = hasSections,
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            if (isPicking) PickingOverlay(c = c)
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
                text = tr("Recommended"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                text = tr("Automatically chosen for the best performance"),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = tr("Selected"),
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
    status: CardStatus?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    LocationCardShell(modifier = modifier, c = c, isSelected = isSelected, onClick = onClick) {
        // پرچم کشور (نشان انتخاب‌شده روی گوشه‌ی پرچمه تا جای اسم کشور تنگ نشه)
        val context = LocalContext.current
        val flagRes = remember(countryCode) { flagResId(context, countryCode) }
        val flag = remember(countryCode) { flagEmoji(countryCode) }
        Box(modifier = Modifier.size(width = 48.dp, height = 44.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            .size(width = 38.dp, height = 28.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    )
                } else if (flag != null) {
                    // اگه تصویر نبود، ایموجی
                    Text(text = flag, fontSize = 26.sp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = tr("Selected"),
                    tint = c.accent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 5.dp, y = 5.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // اسم کشور + شهر
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = countryName,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (cityLabel.isNotBlank()) {
                Text(
                    text = cityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // نشان‌ها تو خط جدا می‌آن تا اسم شهر کوتاه نشه (وضعیت اتصال اولویت داره)
            if (status != null || isFastest) {
                Spacer(Modifier.height(4.dp))
                if (status != null) StatusChip(status = status, c = c) else FastestChip()
            }
        }

        Spacer(Modifier.width(8.dp))

        PingBadge(pingMs = pingMs, pingState = pingState, c = c)

        Spacer(Modifier.width(8.dp))

        FavoriteButton(c = c, isFavorite = isFavorite, onClick = onToggleFavorite)
    }
}

/** قلبِ علاقه‌مندی؛ هم‌قد قرص پینگ و با همان گوشه‌گردی. با لمس، لوکیشن می‌ره بالای لیست. */
@Composable
private fun FavoriteButton(c: FoxExtras, isFavorite: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isFavorite) HeartPink else c.textSecondary.copy(alpha = 0.75f),
        label = "favorite-tint",
    )
    val starScale by animateFloatAsState(
        targetValue = if (isFavorite) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "favorite-scale",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .background(if (isFavorite) HeartPink.copy(alpha = 0.16f) else c.innerCard.copy(alpha = 0.6f))
            .border(1.dp, if (isFavorite) HeartPink.copy(alpha = 0.45f) else c.cardBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) tr("Remove from favorites") else tr("Add to favorites"),
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .scale(starScale),
        )
    }
}

/** عنوان بخش‌های لیست (Current location، Favorites، Recent، All locations). */
@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    c: FoxExtras,
    title: String,
    count: Int,
    icon: ImageVector?,
    iconTint: Color,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = c.accentText,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
        )
    }
}

/** کادر جستجو؛ هم‌استایل کارت‌ها و هم‌قد دکمه‌ی ترتیب. */
@Composable
private fun SearchField(
    c: FoxExtras,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.cardBorder, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = tr("Search country or city"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = c.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = tr("Clear search"),
                    tint = c.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** دکمه‌ی انتخاب ترتیب لیست؛ وقتی ترتیب پیش‌فرض (پینگ) نیست، نارنجی می‌شه. */
@Composable
private fun SortButton(c: FoxExtras, mode: SortMode, onSelected: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val custom = mode != SortMode.PING
    Box {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(shape)
                .background(if (custom) c.accent.copy(alpha = 0.12f) else c.card)
                .border(1.dp, if (custom) c.accent.copy(alpha = 0.45f) else c.cardBorder, shape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = tr("Sort locations"),
                tint = c.accent,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = c.innerCard,
            shape = RoundedCornerShape(18.dp),
        ) {
            SortMode.entries.forEach { option ->
                val selected = option == mode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(option.label),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) c.accentText else c.textPrimary,
                        )
                    },
                    leadingIcon = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = c.accent,
                            )
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

/** وقتی جستجو نتیجه‌ای نداره. */
@Composable
private fun NoResults(c: FoxExtras, query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = c.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = trf("No locations match \u201C%s\u201D", query),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
    }
}

/** پرده‌ی «در حال پیدا کردن سریع‌ترین سرور»؛ لمس‌های دیگه رو تا پایان کار می‌بلعه. */
@Composable
private fun PickingOverlay(c: FoxExtras) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(c.innerCard)
                .border(1.dp, c.cardBorder, shape)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = c.accent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = tr("Finding the fastest server\u2026"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
        }
    }
}

/** نشان وضعیت روی لوکیشن فعلی: CONNECTED (سبز) / CONNECTING (زرد) / SELECTED (نارنجی). */
@Composable
private fun StatusChip(status: CardStatus, c: FoxExtras) {
    val (text, color) = when (status) {
        CardStatus.CONNECTED -> tr("CONNECTED") to PingGood
        CardStatus.CONNECTING -> tr("CONNECTING") to PingMedium
        CardStatus.SELECTED -> tr("SELECTED") to c.accent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = color,
        )
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
            text = if (live) tr("LIVE") else tr("PAUSED"),
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
            text = tr("FASTEST"),
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
                text = tr("Timeout"),
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
