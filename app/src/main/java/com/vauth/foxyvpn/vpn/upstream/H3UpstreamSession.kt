package com.vauth.foxyvpn.vpn.upstream

import android.content.Context
import android.net.Network
import com.vauth.foxyvpn.data.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "H3UpstreamSession"
private const val OPEN_TIMEOUT_MS = 20_000
private const val PROBE_TIMEOUT_MS = 10_000L
private const val PROBE_HOST = "1.1.1.1"
private const val PROBE_PORT = 443

/**
 * HTTP/3 (QUIC): هر تونل یک اتصال SOCKS5 به هسته‌ی محلی H3Core (libbox) است و خود هسته
 * روی یک اتصال QUIC به سرور Mozilla مالتی‌پلکس می‌کنه (CONNECT روی HTTP/3).
 */
class H3UpstreamSession(
    private val context: Context,
    private val protect: (Int) -> Boolean,
    private val network: () -> Network?,
    private val upstreamHost: String,
    private val upstreamPort: Int,
    bearerToken: String,
    private val edgeAddress: String?,
    private val ipVersion: String,
) : UpstreamSession {

    @Volatile private var token = bearerToken
    @Volatile private var accepting = true
    @Volatile private var socksPort = 0
    private val closed = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val tunnels: MutableSet<Socket> = Collections.newSetFromMap(ConcurrentHashMap())

    override val isConnected: Boolean get() = !closed.get() && accepting && socksPort != 0

    override fun updateBearerToken(token: String) {
        if (token.isBlank()) return
        this.token = token
        H3Core.setToken(token)
    }

    override suspend fun connect() = withContext(Dispatchers.IO) {
        socksPort = H3Core.acquire(context, protect, network, upstreamHost, upstreamPort, edgeAddress, ipVersion, token)
        // تست واقعی: یک تونل کوتاه از مسیر QUIC باز می‌کنیم؛ اگه UDP بسته باشه همین‌جا خطا می‌گیریم نه «وصل ولی بی‌اینترنت»
        val probeStart = System.currentTimeMillis()
        try {
            kotlinx.coroutines.withTimeout(PROBE_TIMEOUT_MS) { openStream(PROBE_HOST, PROBE_PORT) }.close()
            AppLogger.i(TAG, "HTTP/3 probe ok in ${System.currentTimeMillis() - probeStart} ms")
        } catch (t: Throwable) {
            val why = if (t is kotlinx.coroutines.TimeoutCancellationException) "timeout after ${PROBE_TIMEOUT_MS / 1000}s (UDP/QUIC probably blocked)" else (t.message ?: t.javaClass.simpleName)
            H3Core.tailLog().forEach { AppLogger.w(TAG, "core log: $it") }
            close()
            throw IOException("HTTP/3 probe failed: $why", t)
        }
    }

    override suspend fun openStream(targetHost: String, targetPort: Int): TunneledStream = withContext(Dispatchers.IO) {
        if (closed.get() || !accepting || socksPort == 0) throw IOException("H3UpstreamSession is not accepting streams")
        val authority = if (targetHost.contains(":")) "[$targetHost]:$targetPort" else "$targetHost:$targetPort"
        val s = Socket()
        try {
            s.tcpNoDelay = true
            s.connect(InetSocketAddress("127.0.0.1", socksPort), 3_000)
            s.soTimeout = OPEN_TIMEOUT_MS
            val out = s.getOutputStream()
            val input = DataInputStream(s.getInputStream())

            out.write(byteArrayOf(5, 1, 0)); out.flush()
            val greet = ByteArray(2); input.readFully(greet)
            if (greet[0].toInt() != 5 || greet[1].toInt() != 0) throw IOException("local H3 core refused SOCKS5 handshake")

            val req = java.io.ByteArrayOutputStream()
            req.write(byteArrayOf(5, 1, 0))
            val literal = literalAddress(targetHost)
            when {
                literal is java.net.Inet4Address -> { req.write(1); req.write(literal.address) }
                literal is java.net.Inet6Address -> { req.write(4); req.write(literal.address) }
                else -> {
                    val name = targetHost.toByteArray(Charsets.US_ASCII)
                    if (name.size > 255) throw IOException("target host name too long")
                    req.write(3); req.write(name.size); req.write(name)
                }
            }
            req.write(targetPort shr 8); req.write(targetPort and 0xFF)
            out.write(req.toByteArray()); out.flush()

            val head = ByteArray(4); input.readFully(head)
            val rep = head[1].toInt() and 0xFF
            if (rep != 0) {
                runCatching { s.close() }
                throw UpstreamConnectRejectedException(rep, authority, "Upstream (HTTP/3) rejected CONNECT to $authority: socks reply=$rep")
            }
            when (head[3].toInt()) {
                1 -> input.skipBytes(4 + 2)
                4 -> input.skipBytes(16 + 2)
                3 -> { val n = input.readUnsignedByte(); input.skipBytes(n + 2) }
                else -> throw IOException("bad SOCKS5 reply")
            }
            s.soTimeout = 0
            tunnels.add(s)
            TunneledStream(
                input = input,
                output = out,
                close = { tunnels.remove(s); runCatching { s.close() }; maybeRelease() },
            )
        } catch (t: SocketTimeoutException) {
            runCatching { s.close() }
            throw UpstreamConnectTimeoutException(authority, "Timed out waiting for HTTP/3 CONNECT response ($authority)", t)
        } catch (t: Throwable) {
            runCatching { s.close() }
            throw t
        }
    }

    private fun literalAddress(host: String): InetAddress? {
        val looksV4 = host.split('.').let { p -> p.size == 4 && p.all { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) } }
        if (!looksV4 && !host.contains(':')) return null
        return runCatching { InetAddress.getByName(host) }.getOrNull()
    }

    override fun disableNewStreamsAndCloseWhenIdle() {
        accepting = false
        maybeRelease()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        accepting = false
        tunnels.toList().forEach { runCatching { it.close() } }
        tunnels.clear()
        releaseOnce()
    }

    private fun maybeRelease() {
        if (!accepting && tunnels.isEmpty()) releaseOnce()
    }

    private fun releaseOnce() {
        if (socksPort != 0 && released.compareAndSet(false, true)) {
            AppLogger.d(TAG, "releasing HTTP/3 core reference")
            H3Core.release()
        }
    }
}
