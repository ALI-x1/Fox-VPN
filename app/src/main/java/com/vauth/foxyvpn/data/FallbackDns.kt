package com.vauth.foxyvpn.data

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

/**
 * DNS با پشتیبان: اول DNS سیستم؛ اگه «Unable to resolve host» داد (DNS خراب/فیلتر/Private DNS/VPN دیگه)،
 * از DoH با آدرس IP مستقیم (بدون نیاز به DNS) جواب می‌گیره. WebView DNS خودش رو داره، برای همین
 * گاهی WebView باز می‌شد ولی OkHttp می‌گفت No address associated with hostname.
 */
internal class FallbackDns(private val socketFactory: SocketFactory) : Dns {
    private val cache = ConcurrentHashMap<String, Pair<Long, List<InetAddress>>>()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .socketFactory(socketFactory)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val endpoints = listOf(
        "https://1.1.1.1/dns-query?type=A&name=",
        "https://8.8.8.8/resolve?type=A&name=",
        "https://1.0.0.1/dns-query?type=A&name=",
        "https://8.8.4.4/resolve?type=A&name=",
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val sys = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            null
        }
        if (!sys.isNullOrEmpty()) return sys
        cache[hostname]?.let { (t, l) -> if (System.currentTimeMillis() - t < 5 * 60_000L) return l }
        for (base in endpoints) {
            val list = runCatching { query(base + hostname) }.getOrNull()
            if (!list.isNullOrEmpty()) {
                cache[hostname] = System.currentTimeMillis() to list
                return list
            }
        }
        throw UnknownHostException("Unable to resolve host \"$hostname\" (system DNS and DoH both failed)")
    }

    private fun query(url: String): List<InetAddress> {
        val req = Request.Builder().url(url).header("Accept", "application/dns-json").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return emptyList()
            val arr = JSONObject(r.body?.string().orEmpty()).optJSONArray("Answer") ?: return emptyList()
            val out = ArrayList<InetAddress>()
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                if (a.optInt("type") == 1) out.add(InetAddress.getByName(a.getString("data")))
            }
            return out
        }
    }
}
