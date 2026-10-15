package com.vauth.foxyvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

private const val EXIT_CHECK_URL = "https://www.cloudflare.com/cdn-cgi/trace"
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
                runCatching {
                    val request = Request.Builder().url(EXIT_CHECK_URL).build()
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

                        if (expectedCountryCode != null &&
                            !expectedCountryCode.equals("REC", ignoreCase = true) &&
                            !actualCountry.equals(expectedCountryCode, ignoreCase = true)
                        ) {
                            error("Exit country mismatch: expected $expectedCountryCode, got $actualCountry")
                        }
                        
                        IpInfo(ip = actualIp, countryCode = actualCountry)
                    }
                }
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
}
