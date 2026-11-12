-dontwarn io.netty.**
-keep class io.netty.** { *; }

-keep class com.vauth.foxyvpn.vpn.tun.HevSocks5Tunnel { *; }
-keepnames class com.vauth.foxyvpn.vpn.tun.HevSocks5Tunnel
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**

-dontwarn io.netty.internal.tcnative.**
-dontwarn io.netty.handler.ssl.OpenSsl**

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn com.google.errorprone.annotations.**

-dontwarn reactor.blockhound.**

-keep class io.nekohasekai.libbox.** { *; }
-dontwarn io.nekohasekai.libbox.**
-keep class go.** { *; }
-dontwarn go.**
-keep class * implements io.nekohasekai.libbox.PlatformInterface { *; }
-keep class * implements io.nekohasekai.libbox.CommandServerHandler { *; }
-keep class com.vauth.foxyvpn.vpn.upstream.H3Core { *; }
-keep class com.vauth.foxyvpn.vpn.upstream.H3Core$** { *; }
-keep class * implements io.nekohasekai.libbox.StringIterator { *; }
-keep class * implements io.nekohasekai.libbox.NetworkInterfaceIterator { *; }
-keep class * implements io.nekohasekai.libbox.LocalDNSTransport { *; }
-keep class * implements io.nekohasekai.libbox.InterfaceUpdateListener { *; }
-keepclassmembers class * implements io.nekohasekai.libbox.** { *; }
