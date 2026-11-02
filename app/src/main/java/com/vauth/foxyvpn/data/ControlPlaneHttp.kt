package com.vauth.foxyvpn.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

private const val TAG = "ControlPlaneHttp"

const val MOZILLA_VPN_USER_AGENT = "MozillaVPN/2.35.0 (sys:linux; iap:true)"

fun Request.Builder.applyMozillaVpnHeaders(): Request.Builder {
    header("User-Agent", MOZILLA_VPN_USER_AGENT)
    header("Accept", "application/json")
    return this
}

class SimpleCookieJar : CookieJar {
    private val byDomain = ConcurrentHashMap<String, MutableList<Cookie>>()

    // کوکی‌های Fastly (نتیجه‌ی حل چالش ۴۰۶) ماندگار می‌شن تا اتصال اول بعد از هر بار باز کردن برنامه
    // مجبور به حل دوباره‌ی چالش (چند درخواست + PoW) نشه؛ همین مهم‌ترین دلیل کندی اتصال اول بود.
    @Volatile private var prefs: android.content.SharedPreferences? = null

    fun attachStorage(context: android.content.Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences("fox_cp_cookies", android.content.Context.MODE_PRIVATE)
        prefs = sp
        runCatching {
            val arr = org.json.JSONArray(sp.getString("jar", "[]"))
            val now = System.currentTimeMillis()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val exp = o.optLong("e", 0L)
                if (exp <= now) continue
                val b = Cookie.Builder().name(o.getString("n")).value(o.getString("v"))
                    .domain(o.getString("d")).path(o.optString("p", "/")).expiresAt(exp)
                if (o.optBoolean("s", false)) b.secure()
                val c = b.build()
                val bucket = byDomain.getOrPut(c.domain) { mutableListOf() }
                synchronized(bucket) { bucket.removeAll { it.name == c.name }; bucket.add(c) }
            }
        }
    }

    private fun persist() {
        val sp = prefs ?: return
        runCatching {
            val now = System.currentTimeMillis()
            val arr = org.json.JSONArray()
            for (c in snapshotAll()) {
                if (c.expiresAt <= now) continue
                arr.put(org.json.JSONObject().put("n", c.name).put("v", c.value).put("d", c.domain)
                    .put("p", c.path).put("e", c.expiresAt).put("s", c.secure))
            }
            sp.edit().putString("jar", arr.toString()).apply()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            val bucket = byDomain.getOrPut(cookie.domain) { mutableListOf() }
            synchronized(bucket) {
                bucket.removeAll { it.name == cookie.name }
                bucket.add(cookie)
            }
        }
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<Cookie>()
        for ((domain, cookies) in byDomain) {
            if (url.host == domain || url.host.endsWith(".$domain")) {
                synchronized(cookies) {
                    result.addAll(cookies.filter { it.expiresAt > now })
                }
            }
        }
        return result
    }

    fun set(name: String, value: String, domain: String) {
        val bucket = byDomain.getOrPut(domain) { mutableListOf() }
        synchronized(bucket) {
            bucket.removeAll { it.name == name }
            bucket.add(
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(domain)
                    .path("/")
                    .expiresAt(Long.MAX_VALUE)
                    .build(),
            )
        }
        persist()
    }

    fun snapshotAll(): List<Cookie> = byDomain.values.flatten()
}

private class ProtectingSocketFactory : SocketFactory() {

    private fun protect(socket: Socket, localAddress: InetSocketAddress?): Socket {

        runCatching {
            if (!socket.isBound) socket.bind(localAddress ?: InetSocketAddress(0))
        }.onFailure {
            AppLogger.w(TAG, "could not bind a control-plane socket before protecting it", it)
        }
        val protector = ControlPlaneHttp.socketProtector
        if (protector != null) {
            val protectedOk = runCatching { protector.invoke(socket) }.getOrDefault(false)
            if (!protectedOk) {
                AppLogger.w(
                    TAG,
                    "VpnService.protect() refused a control-plane socket; this request would be routed " +
                        "back into the tunnel instead of out to the network",
                )
            }
        }
        return socket
    }

    override fun createSocket(): Socket = protect(Socket(), null)

    override fun createSocket(host: String, port: Int): Socket =
        protect(Socket(), null).apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: String, port: Int, localAddr: InetAddress, localPort: Int): Socket =
        protect(Socket(), InetSocketAddress(localAddr, localPort))
            .apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: InetAddress, port: Int): Socket =
        protect(Socket(), null).apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: InetAddress, port: Int, localAddr: InetAddress, localPort: Int): Socket =
        protect(Socket(), InetSocketAddress(localAddr, localPort))
            .apply { connect(InetSocketAddress(host, port)) }
}

object ControlPlaneHttp {
    val cookieJar = SimpleCookieJar()

    @Volatile
    var socketProtector: ((Socket) -> Boolean)? = null

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .socketFactory(ProtectingSocketFactory())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
