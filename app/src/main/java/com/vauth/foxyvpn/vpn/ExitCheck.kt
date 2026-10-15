package com.vauth.foxyvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

// ترتیب: اول IPv4 (آدرس عددی کلودفلر؛ چون hostname نیست حتماً از راه IPv4 می‌ره)،
// و فقط اگه IPv4 در دسترس نبود، IPv6. پس ip= توی پاسخ تا جای ممکن IPv4 می‌شه.
private val EXIT_CHECK_URLS = listOf(
    "https://1.1.1.1/cdn-cgi/trace",
    "https://1.0.0.1/cdn-cgi/trace",
    "https://[2606:4700:4700::1111]/cdn-cgi/trace",
)

private val IPV4_REGEX = Regex("""^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$""")
private val IPV6_REGEX = Regex("""^[0-9a-fA-F:.]+$""")

/** فقط آی‌پیِ عددی (IPv4 یا IPv6) معتبره؛ هر متن دیگه‌ای رد می‌شه. */
private fun isNumericIp(value: String): Boolean =
    IPV4_REGEX.matches(value) || (value.contains(':') && IPV6_REGEX.matches(value))

/** کشور خروجی با کشور مورد انتظار نمی‌خونه؛ اتصال کار کرده، پس نباید سراغ آدرس بعدی رفت. */
private class ExitCountryMismatch(message: String) : IllegalStateException(message)

private const val EXIT_CHECK_TIMEOUT_SECONDS = 15L

// مدلی برای نگهداری آی‌پی و کد کشور خروجی
data class IpInfo(
    val ip: String = "---",
    val countryCode: String = "---"
)

class ExitCheck {

    // اگر socksPort برابر با null یا صفر باشد، یعنی VPN قطع است و بدون پروکسی تست می‌کند
    suspend fun verifyExitCountry(socksPort: Int? = null, expectedCountryCode: String? = null): Result<IpInfo> =
        withContext(Dispatchers.IO) {

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(EXIT_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(EXIT_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(EXIT_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // اگر پورت پروکسی داده شده بود، از طریق تونل متصل شو
            if (socksPort != null && socksPort > 0) {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
                clientBuilder.proxy(proxy)
            }

            val client = clientBuilder.build()
            try {
                var lastFailure: Throwable? = null
                for (url in EXIT_CHECK_URLS) {
                    val attempt = runCatching {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("Exit check request failed: HTTP ${response.code}")
                        val body = response.body?.string().orEmpty()
                        
                        val lines = body.lineSequence()
                        
                        // استخراج آی‌پی از پاسخ کلودفلر
                        val actualIp = lines.firstOrNull { it.startsWith("ip=") }
                            ?.removePrefix("ip=")
                            ?.trim()
                            .orEmpty()

                        // استخراج کد کشور از پاسخ کلودفلر
                        val actualCountry = lines.firstOrNull { it.startsWith("loc=") }
                            ?.removePrefix("loc=")
                            ?.trim()
                            .orEmpty()

                        if (actualCountry.isEmpty() && actualIp.isEmpty()) {
                            error("Exit check response did not report a location or IP")
                        }
                        // آی‌پی باید عدد باشه (IPv4، یا IPv6 وقتی IPv4 نبود)
                        if (actualIp.isNotEmpty() && !isNumericIp(actualIp)) {
                            error("Exit check returned a non-numeric IP: $actualIp")
                        }

                        if (expectedCountryCode != null &&
                            !expectedCountryCode.equals("REC", ignoreCase = true) &&
                            !actualCountry.equals(expectedCountryCode, ignoreCase = true)
                        ) {
                            throw ExitCountryMismatch("Exit country mismatch: expected $expectedCountryCode, got $actualCountry")
                        }
                        
                        IpInfo(ip = actualIp, countryCode = actualCountry)
                    }
                    }
                    if (attempt.isSuccess) return@withContext attempt
                    lastFailure = attempt.exceptionOrNull()
                    // عدم تطابق کشور یعنی اتصال سالمه؛ با IPv6 هم همین می‌شه
                    if (lastFailure is ExitCountryMismatch) return@withContext attempt
                }
                Result.failure(lastFailure ?: IllegalStateException("Exit check failed"))
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
}
