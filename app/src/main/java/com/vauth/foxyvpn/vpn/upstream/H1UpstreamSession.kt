package com.vauth.foxyvpn.vpn.upstream

import com.vauth.foxyvpn.data.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

private const val TAG = "H1UpstreamSession"
private const val TCP_CONNECT_TIMEOUT_MS = 15_000
private const val HANDSHAKE_TIMEOUT_MS = 15_000
private const val OPEN_TUNNEL_TIMEOUT_MS = 20_000
private const val SPARE_MAX_AGE_MS = 20_000L
private const val MAX_RESPONSE_HEAD_BYTES = 16 * 1024
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

/**
 * HTTP/1.1 (TLS): هر تونل = یک اتصال TLS جدا به سرور Mozilla که یک درخواست CONNECT می‌بره (مثل مرورگر پشت پروکسی HTTPS).
 * چیزی بین تونل‌ها مشترک نیست؛ پس شبکه‌ای که اتصال‌های طولانی رو کند یا قطع می‌کنه، فقط یک تونل رو می‌بره نه همه رو.
 * هزینه: هر تونل یک دست‌دهی TLS؛ برای پنهان کردنش همیشه یک اتصال آماده (spare) نگه می‌داریم.
 */
class H1UpstreamSession(
    private val upstreamHost: String,
    private val upstreamPort: Int,
    bearerToken: String,
    private val edgeAddress: String? = null,
    private val ipVersion: String = "auto",
) : UpstreamSession {

    @Volatile private var currentToken: String = bearerToken
    private val closed = AtomicBoolean(false)
    @Volatile private var accepting = true

    private val lock = Any()
    private var spare: SSLSocket? = null
    private var spareAt = 0L
    private var filling = false
    private val tunnels: MutableSet<Socket> = Collections.newSetFromMap(ConcurrentHashMap())

    override val isConnected: Boolean
        get() = !closed.get() && accepting

    override fun updateBearerToken(token: String) {
        if (token.isNotBlank()) currentToken = token
    }

    override suspend fun connect() = withContext(Dispatchers.IO) {
        // اولین اتصال نشون می‌ده سرور HTTP/1.1 قبول می‌کنه؛ بعدش به‌عنوان spare برای اولین تونل می‌مونه
        val s = dial()
        synchronized(lock) { spare = s; spareAt = System.currentTimeMillis() }
        AppLogger.i(TAG, "upstream HTTP/1.1 connection established to $upstreamHost:$upstreamPort (alpn=${runCatching { s.applicationProtocol }.getOrNull()})")
    }

    override suspend fun openStream(targetHost: String, targetPort: Int): TunneledStream = withContext(Dispatchers.IO) {
        if (closed.get()) throw IOException("H1UpstreamSession is closed")
        if (!accepting) throw IOException("H1UpstreamSession is shutting down")
        val authority = if (targetHost.contains(":")) "[$targetHost]:$targetPort" else "$targetHost:$targetPort"

        val ready = takeSpare()
        if (ready != null) {
            try {
                return@withContext handshake(ready, authority)
            } catch (rejected: UpstreamConnectRejectedException) {
                throw rejected
            } catch (stale: IOException) {
                // spare که سرور وسط انتظار بسته؛ با یک اتصال تازه دوباره امتحان می‌کنیم
                runCatching { ready.close() }
            }
        }
        handshake(dial(), authority)
    }

    override fun disableNewStreamsAndCloseWhenIdle() {
        accepting = false
        synchronized(lock) { spare?.let { runCatching { it.close() } }; spare = null }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        accepting = false
        synchronized(lock) { spare?.let { runCatching { it.close() } }; spare = null }
        tunnels.toList().forEach { runCatching { it.close() } }
        tunnels.clear()
    }

    private fun takeSpare(): SSLSocket? {
        var out: SSLSocket? = null
        var refill = false
        synchronized(lock) {
            out = spare
            spare = null
            if (out != null && System.currentTimeMillis() - spareAt > SPARE_MAX_AGE_MS) {
                runCatching { out!!.close() }
                out = null
            }
            if (!closed.get() && accepting && !filling) { filling = true; refill = true }
        }
        if (refill) Thread({ fillSpare() }, "h1-spare").apply { isDaemon = true }.start()
        return out
    }

    private fun fillSpare() {
        val fresh = runCatching { dial() }.getOrNull()
        synchronized(lock) {
            filling = false
            if (fresh == null) return
            if (closed.get() || !accepting || spare != null) { runCatching { fresh.close() }; return }
            spare = fresh
            spareAt = System.currentTimeMillis()
        }
    }

    // ───────── اتصال TLS ─────────

    private fun pickAddress(): InetAddress {
        edgeAddress?.takeIf { it.isNotBlank() }?.let { return InetAddress.getByName(it) }
        val all = InetAddress.getAllByName(upstreamHost).toList()
        val wanted = when (ipVersion) {
            "v4" -> all.filterIsInstance<Inet4Address>()
            "v6" -> all.filterIsInstance<Inet6Address>()
            else -> all.sortedBy { it is Inet6Address } // خودکار: اول IPv4، بعد IPv6
        }
        return wanted.firstOrNull() ?: throw IOException("no $ipVersion address for $upstreamHost")
    }

    private fun dial(): SSLSocket {
        val raw = Socket()
        try {
            raw.tcpNoDelay = true
            raw.keepAlive = true
            raw.connect(InetSocketAddress(pickAddress(), upstreamPort), TCP_CONNECT_TIMEOUT_MS)
            val context = SSLContext.getInstance("TLS").apply {
                init(null, UpstreamTrust.trustManagerFactory()?.trustManagers, null)
            }
            val ssl = context.socketFactory.createSocket(raw, upstreamHost, upstreamPort, true) as SSLSocket
            val params = ssl.sslParameters
            params.serverNames = listOf(SNIHostName(upstreamHost))
            params.endpointIdentificationAlgorithm = "HTTPS"
            val supported = ssl.supportedProtocols.toSet()
            params.protocols = listOf("TLSv1.3", "TLSv1.2").filter { it in supported }.toTypedArray()
            runCatching { params.applicationProtocols = arrayOf("http/1.1") }
            ssl.sslParameters = params
            ssl.soTimeout = HANDSHAKE_TIMEOUT_MS
            ssl.startHandshake()
            ssl.soTimeout = 0
            return ssl
        } catch (t: Throwable) {
            runCatching { raw.close() }
            throw t
        }
    }

    // ───────── CONNECT ─────────

    private fun handshake(ssl: SSLSocket, authority: String): TunneledStream {
        try {
            ssl.soTimeout = OPEN_TUNNEL_TIMEOUT_MS
            val request = "CONNECT $authority HTTP/1.1\r\n" +
                "Host: $authority\r\n" +
                "Proxy-Authorization: Bearer $currentToken\r\n" +
                "User-Agent: $USER_AGENT\r\n\r\n"
            ssl.outputStream.write(request.toByteArray(Charsets.US_ASCII))
            ssl.outputStream.flush()

            val input = BufferedInputStream(ssl.inputStream, 16 * 1024)
            val head = StringBuilder()
            var tail = 0
            while (true) {
                val b = input.read()
                if (b < 0) throw IOException("upstream closed the connection before answering CONNECT to $authority")
                head.append(b.toChar())
                tail = if (b == '\r'.code || b == '\n'.code) tail + 1 else 0
                if (tail >= 4 && head.endsWith("\r\n\r\n")) break
                if (head.length > MAX_RESPONSE_HEAD_BYTES) throw IOException("upstream CONNECT answer too long")
            }
            val statusLine = head.lineSequence().firstOrNull().orEmpty()
            val code = statusLine.split(' ').getOrNull(1)?.toIntOrNull()
            if (code == null || code !in 200..299) {
                runCatching { ssl.close() }
                throw UpstreamConnectRejectedException(
                    statusCode = code,
                    authority = authority,
                    message = "Upstream rejected CONNECT to $authority: status=${code ?: statusLine.ifBlank { "<missing>" }}",
                )
            }
            ssl.soTimeout = 0
            tunnels.add(ssl)
            if (closed.get()) { runCatching { ssl.close() }; tunnels.remove(ssl); throw IOException("H1UpstreamSession is closed") }
            return TunneledStream(
                input = input,
                output = ssl.outputStream,
                close = { tunnels.remove(ssl); runCatching { ssl.close() } },
            )
        } catch (timeout: SocketTimeoutException) {
            runCatching { ssl.close() }
            throw UpstreamConnectTimeoutException(authority, "Timed out waiting for CONNECT response from $upstreamHost:$upstreamPort ($authority)", timeout)
        } catch (other: UpstreamConnectRejectedException) {
            throw other
        } catch (t: Throwable) {
            runCatching { ssl.close() }
            throw t
        }
    }
}
