package com.vauth.foxyvpn.vpn.upstream

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.system.OsConstants
import com.vauth.foxyvpn.data.AppLogger
import io.nekohasekai.libbox.AutoRedirectHandler
import io.nekohasekai.libbox.AutoRedirectSession
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.ExchangeContext
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterface as LibboxNetworkInterface
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.SetupOptions
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "H3Core"

/**
 * هسته‌ی HTTP/3 (QUIC): همون libbox پروژه‌ی Vauth/FoxyVPN (فورک sing-box با خروجی `http` نسخه ۳).
 * اینجا فقط به‌عنوان «اتصال به سرور» استفاده می‌شه: یک ورودی SOCKS محلی روی 127.0.0.1 و خروجی HTTP/3؛
 * TUN رو هنوز خود اپ (hev-socks5-tunnel) می‌سازه، پس مسیر H2/H1 و بقیه‌ی اپ دست‌نخورده می‌مونه.
 */
internal object H3Core {

    private val lock = Any()
    private var setupDone = false
    private var server: CommandServer? = null
    private var port = 0
    private var key: String = ""
    private var refs = 0

    /** پورت SOCKS محلی هسته؛ اگه هسته با همین مشخصات بالاست همونو برمی‌گردونه. */
    fun acquire(
        context: Context,
        protect: (Int) -> Boolean,
        network: () -> Network?,
        host: String,
        serverPort: Int,
        edgeAddress: String?,
        ipVersion: String,
        token: String,
    ): Int = synchronized(lock) {
        val wanted = "$host|$serverPort|${edgeAddress.orEmpty()}|$ipVersion"
        if (server != null && key == wanted) {
            refs++
            Libbox.setBearerToken(token)
            return port
        }
        stopLocked()
        val app = context.applicationContext
        val guard = app.getSharedPreferences("h3_guard", Context.MODE_PRIVATE)
        guard.edit().putBoolean("inflight", true).commit()
        if (!setupDone) {
            val options = SetupOptions().apply {
                basePath = app.filesDir.absolutePath
                workingPath = app.filesDir.absolutePath
                tempPath = app.cacheDir.absolutePath
                logMaxLines = 2000
                debug = false
            }
            Libbox.setup(options)
            setupDone = true
        }
        val socksPort = ServerSocket(0).use { it.localPort }
        val config = buildConfig(host, serverPort, edgeAddress, ipVersion, socksPort)
        Libbox.setBearerToken(token)
        Libbox.checkConfig(config)
        runCatching { File(app.filesDir, "command.sock").delete() }
        runCatching { File(app.filesDir, "cache.db").delete() }
        val platform = Platform(app, protect, network)
        val cs = CommandServer(platform, platform)
        cs.start()
        cs.startOrReloadService(config, OverrideOptions())
        server = cs
        port = socksPort
        key = wanted
        refs = 1
        AppLogger.i(TAG, "HTTP/3 core started: socks=127.0.0.1:$socksPort -> $host:$serverPort (edge=${edgeAddress ?: "dns"})")
        waitReady(socksPort)
        guard.edit().putBoolean("inflight", false).commit()
        socksPort
    }

    fun setToken(token: String) {
        if (token.isNotBlank()) runCatching { Libbox.setBearerToken(token) }
    }

    fun release() = synchronized(lock) {
        if (refs > 0) refs--
        if (refs == 0) stopLocked()
    }

    private fun stopLocked() {
        val s = server ?: return
        server = null
        runCatching { Libbox.setBearerToken("") }
        runCatching { s.closeService() }
        runCatching { s.close() }
        AppLogger.i(TAG, "HTTP/3 core stopped")
        port = 0
        key = ""
        refs = 0
    }

    private fun waitReady(p: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", p), 300) }
                return
            } catch (_: Exception) {
                Thread.sleep(80)
            }
        }
        throw java.io.IOException("HTTP/3 core did not open its local port")
    }

    private fun buildConfig(host: String, serverPort: Int, edge: String?, ipVersion: String, socksPort: Int): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("level", "warn").put("timestamp", true))
        root.put("certificate", JSONObject().put("store", "mozilla"))
        root.put(
            "dns",
            JSONObject()
                .put("servers", JSONArray().put(JSONObject().put("type", "local").put("tag", "local-dns")))
                .put("final", "local-dns")
                .put("strategy", when (ipVersion) { "v4" -> "ipv4_only"; "v6" -> "ipv6_only"; else -> "prefer_ipv4" })
                .put("disable_cache", false),
        )
        root.put(
            "http_clients",
            JSONArray().put(JSONObject().put("tag", "default-http-client").put("engine", "go").put("version", 2)),
        )
        root.put(
            "inbounds",
            JSONArray().put(
                JSONObject().put("type", "socks").put("tag", "socks-in")
                    .put("listen", "127.0.0.1").put("listen_port", socksPort),
            ),
        )
        val proxy = JSONObject()
            .put("type", "http").put("tag", "proxy-out")
            .put("server", edge ?: host).put("server_port", serverPort)
            .put("domain_resolver", "local-dns")
            .put("dynamic_bearer_token", true)
            .put("version", 3)
            .put("disable_version_fallback", true)
            .put("tls", JSONObject().put("enabled", true).put("server_name", host))
        root.put(
            "outbounds",
            JSONArray().put(proxy).put(JSONObject().put("type", "direct").put("tag", "direct-out")),
        )
        root.put(
            "route",
            JSONObject()
                .put("final", "proxy-out")
                .put("auto_detect_interface", true)
                .put("find_process", false)
                .put("default_domain_resolver", "local-dns")
                .put("default_http_client", "default-http-client"),
        )
        root.put(
            "experimental",
            JSONObject().put("cache_file", JSONObject().put("enabled", false)),
        )
        return root.toString()
    }

    // ───────── PlatformInterface (نسخه‌ی بدون TUN از FoxyPlatformInterface پروژه‌ی Vauth) ─────────

    private class Platform(
        private val context: Context,
        private val protect: (Int) -> Boolean,
        private val networkProvider: () -> Network?,
    ) : PlatformInterface, CommandServerHandler {

        override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

        override fun autoDetectInterfaceControl(fd: Int) {
            if (!protect(fd)) throw java.io.IOException("VPN refused native socket protection")
        }

        override fun openTun(options: TunOptions): Int = error("TUN is owned by the app, not the HTTP/3 core")

        override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        override fun findConnectionOwner(
            ipProtocol: Int, sourceAddress: String, sourcePort: Int, destinationAddress: String, destinationPort: Int,
        ): ConnectionOwner = throw java.io.FileNotFoundException("connection owner not found")

        @Volatile private var defaultListener: InterfaceUpdateListener? = null

        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
            defaultListener = listener
            notifyDefault()
        }

        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
            if (defaultListener == listener) defaultListener = null
        }

        private fun notifyDefault() {
            val listener = defaultListener ?: return
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            val net = networkProvider()
            if (net == null) {
                listener.updateDefaultInterface("", -1, false, false)
                return
            }
            val caps = cm.getNetworkCapabilities(net) ?: return
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
            val name = cm.getLinkProperties(net)?.interfaceName ?: return
            val index = runCatching { NetworkInterface.getByName(name)?.index }.getOrNull() ?: return
            val expensive = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val constrained = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)
            runCatching { listener.updateDefaultInterface(name, index, expensive, constrained) }
        }

        @Suppress("DEPRECATION")
        override fun getInterfaces(): NetworkInterfaceIterator {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val networks = runCatching { cm?.allNetworks }.getOrNull() ?: emptyArray()
            val all = runCatching { NetworkInterface.getNetworkInterfaces()?.toList() }.getOrNull().orEmpty()
            val out = mutableListOf<LibboxNetworkInterface>()
            for (n in networks) {
                val lp = cm?.getLinkProperties(n) ?: continue
                val caps = cm.getNetworkCapabilities(n) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                val ifaceName = lp.interfaceName ?: continue
                val nif = all.find { it.name == ifaceName } ?: continue
                out.add(
                    LibboxNetworkInterface().apply {
                        name = ifaceName
                        index = nif.index
                        dnsServer = Strings(lp.dnsServers.mapNotNull { it.hostAddress })
                        gateway = Strings(
                            lp.routes.filter { it.destination?.prefixLength == 0 }
                                .mapNotNull { it.gateway }.filterNot { it.isAnyLocalAddress }
                                .mapNotNull { it.hostAddress },
                        )
                        type = when {
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                            else -> Libbox.InterfaceTypeOther
                        }
                        mtu = runCatching { nif.mtu }.getOrNull() ?: 1500
                        addresses = Strings(
                            nif.interfaceAddresses.mapNotNull { a ->
                                runCatching { "${a.address.hostAddress}/${a.networkPrefixLength}" }.getOrNull()
                            },
                        )
                        var f = 0
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            f = f or OsConstants.IFF_UP or OsConstants.IFF_RUNNING
                        }
                        if (nif.isLoopback) f = f or OsConstants.IFF_LOOPBACK
                        if (nif.isPointToPoint) f = f or OsConstants.IFF_POINTOPOINT
                        if (runCatching { nif.supportsMulticast() }.getOrDefault(false)) f = f or OsConstants.IFF_MULTICAST
                        flags = f
                        metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    },
                )
            }
            return object : NetworkInterfaceIterator {
                private val iter = out.iterator()
                override fun hasNext(): Boolean = iter.hasNext()
                override fun next(): LibboxNetworkInterface = iter.next()
            }
        }

        private class Strings(private val list: List<String>) : StringIterator {
            private val iter = list.iterator()
            override fun len(): Int = list.size
            override fun hasNext(): Boolean = iter.hasNext()
            override fun next(): String = if (iter.hasNext()) iter.next() else ""
        }

        override fun underNetworkExtension(): Boolean = false
        override fun includeAllNetworks(): Boolean = false

        override fun readWIFIState(): WIFIState = WIFIState("", "")
        override fun clearDNSCache() {}
        override fun sendNotification(notification: Notification) {}
        override fun cancelNotification(identifier: String, typeID: Int) {}
        override fun startNeighborMonitor(listener: NeighborUpdateListener) {}
        override fun closeNeighborMonitor(listener: NeighborUpdateListener) {}
        override fun registerMyInterface(name: String) {}
        override fun usePlatformShell(): Boolean = false
        override fun checkPlatformShell() {}
        override fun openShellSession(
            user: PlatformUser, command: String, environ: StringIterator, term: String, rows: Int, cols: Int,
        ): ShellSession = error("shell session not supported")
        override fun lookupUser(username: String): PlatformUser = error("user lookup not supported")
        override fun lookupSFTPServer(): String = error("sftp not supported")
        override fun readSystemSSHHostKey(): String = error("ssh not supported")
        override fun tailscaleHostname(): String = ""
        override fun usePlatformBridge(): Boolean = false
        override fun createBridge(options: BridgeOptions): BridgeSession = error("bridge not supported")
        override fun usePlatformAutoRedirect(): Boolean = false
        override fun createAutoRedirect(options: ByteArray, handler: AutoRedirectHandler): AutoRedirectSession =
            error("auto-redirect not supported")

        override fun localDNSTransport(): LocalDNSTransport = object : LocalDNSTransport {
            override fun raw(): Boolean = false
            override fun exchange(ctx: ExchangeContext, message: ByteArray) { ctx.errorCode(4) }
            override fun lookup(ctx: ExchangeContext, network: String, domain: String) {
                try {
                    val net = networkProvider()
                    if (net == null) { ctx.errorCode(2); return }
                    val all = net.getAllByName(domain.trimEnd('.'))
                    val filtered = when (network) {
                        "ip4" -> all.filterIsInstance<Inet4Address>()
                        "ip6" -> all.filterIsInstance<Inet6Address>()
                        else -> all.toList()
                    }
                    ctx.success(filtered.joinToString("\n") { it.hostAddress.orEmpty() })
                } catch (e: Exception) {
                    ctx.errorCode(2)
                }
            }
        }

        override fun serviceStop() {}
        override fun serviceReload() {}
        override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()
        override fun setSystemProxyEnabled(enabled: Boolean) {}
        override fun triggerNativeCrash() {}
        override fun writeDebugMessage(message: String) {
            when {
                message.contains("ERROR", true) || message.contains("FATAL", true) -> AppLogger.e(TAG, "core: $message")
                message.contains("WARN", true) -> AppLogger.w(TAG, "core: $message")
                else -> AppLogger.i(TAG, "core: $message")
            }
        }
        override fun connectSSHAgent(): Int = -1
    }
}
