<div align="center">

<img src="https://raw.githubusercontent.com/ALI-x1/Fox-VPN/main/app/src/main/res/drawable/ic_fox_logo.png" alt="Fox-VPN logo" width="112" />

# Fox-VPN

**An unofficial Android client for Mozilla VPN connectivity**

[![Android CI](https://github.com/ALI-x1/Fox-VPN/actions/workflows/android-ci.yml/badge.svg)](https://github.com/ALI-x1/Fox-VPN/actions/workflows/android-ci.yml)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Secure your Android device through a Firefox Account-backed VPN connection.**

[English](README.md) · [فارسی](README.fa.md)

[Features](#features) · [How it works](#how-it-works) · [Screenshots](#screenshots) · [Build](#build-from-source) · [Privacy](#privacy--security)

</div>

> **Unofficial project:** Fox-VPN is an independent community client. It is not made, endorsed, or supported by Mozilla. Availability and access depend on Mozilla services, your account, and regional eligibility.

## Overview

Fox-VPN is an open-source Android application written in Kotlin and Jetpack Compose. It signs in with Firefox Accounts, retrieves VPN access information from Mozilla Guardian, and routes device traffic through an Android `VpnService` tunnel.

The app brings server selection, connection status, traffic usage, and VPN settings together in one place. A local proxy-only mode is also available for apps that can be configured to use a SOCKS5 or HTTP proxy.

## Features

- **System-wide VPN** through Android's VPN interface.
- **Firefox Account sign-in**, session restoration, and email verification where required.
- **Dynamic server locations** loaded from Mozilla Remote Settings; quarantined servers are excluded.
- **TCP latency checks** to VPN server endpoints. These are TCP connection-time estimates, not ICMP echo pings.
- **Split tunneling** to exclude selected apps from the VPN tunnel.
- **Local SOCKS5 and optional HTTP proxies** for proxy-aware apps.
- **DNS choices:** automatic or selected DNS-over-HTTPS providers, plus an optional custom DNS server.
- **Upstream proxy chaining** through another SOCKS5 or HTTP proxy.
- **Connection diagnostics:** public IP/exit-country verification and in-app logs.
- **Traffic information:** live upload/download rates and account usage when available.
- **Quick access** through an Android Quick Settings tile and home-screen widget.
- **Personalization** with dark, light, and system themes and multiple palettes.
- **Advanced options** including proxy-only mode and a custom edge address.

## Screenshots

<div align="center">
<table>
<tr>
<td align="center"><img src="docs/screenshots/home.jpg" alt="Fox-VPN home screen preview" width="230" /><br/><sub>Home</sub></td>
<td align="center"><img src="docs/screenshots/settings.jpg" alt="Fox-VPN settings screen preview" width="230" /><br/><sub>Settings</sub></td>
<td align="center"><img src="docs/screenshots/account.jpg" alt="Fox-VPN account screen preview" width="230" /><br/><sub>Account</sub></td>
</tr>
</table>
</div>

> These are visual previews supplied by the project owner; some UI elements may be concepts and may not match the current build. The IP address and account ID have been masked for privacy.

## How it works

1. Sign in with a Firefox Account.
2. Fox-VPN requests VPN access information from Mozilla Guardian.
3. Choose an available country or city.
4. Approve Android's VPN permission, then connect.
5. The app creates a system VPN interface and carries traffic through its tunnel and the selected upstream endpoint.

> Server-list TCP latency and end-to-end latency through the VPN are different measurements. The first estimates the path to a VPN endpoint; the second also includes the tunnel and the destination service.

## Requirements

- Android **8.0 (API 26)** or newer
- JDK **17**
- Android SDK **35**
- Android NDK **26.1.10909125**
- Git
- Gradle **8.10.2** for the build commands below

The native HEV SOCKS5 tunnel source is fetched from its pinned upstream release during the Android build, so the first build needs network access.

## Build from source

```bash
git clone https://github.com/ALI-x1/Fox-VPN.git
cd Fox-VPN
```

This repository currently does not include the Gradle Wrapper. With Gradle 8.10.2 installed, generate it once and build the debug APK:

```bash
gradle wrapper --gradle-version 8.10.2
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

You can also start the GitHub Actions workflow from **Actions → Android CI → Run workflow**. After a successful run, download the `foxyvpn-debug-apk` artifact from that run.

## Release signing

Release signing is optional for local development. To create a signed GitHub Actions release, add these repository secrets:

- `SIGNING_KEYSTORE_BASE64` — Base64-encoded Android keystore
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`

Never commit a keystore, signing password, Firefox Account credential, or access token. A release built without signing secrets is unsigned and cannot update an already-installed signed build.

## Privacy & security

- Firefox Account tokens are stored using AndroidX encrypted preferences.
- Account, server-list, and VPN-access requests communicate with Mozilla services.
- When exit checking is enabled, the app contacts Cloudflare's trace endpoint to determine the public IP and exit country.
- Crash traces are saved in app-private storage and replayed into the local in-app log on the next launch. Normal diagnostic logs are held locally in memory; review exports and remove personal details before sharing them.
- The local proxy binds to loopback (`127.0.0.1`) by default. Binding it to `0.0.0.0` may expose an unauthenticated proxy to other devices on the local network. Use that option only on a trusted network and understand the risk.
- Fox-VPN does not promise anonymity or protection beyond the behavior of the underlying VPN service and the Android device.

## Troubleshooting

### Android does not connect

- Confirm that Android's VPN permission was approved.
- Check that the Firefox Account can access the underlying Mozilla VPN service in your region.
- Refresh the server list and try another available location.
- Check the in-app logs. Redact IP addresses, account identifiers, and credentials before sharing them.

### Proxy-only mode does not affect every app

Proxy-only mode provides local proxy endpoints. Apps must support and be configured to use SOCKS5 or HTTP; this mode does not create a system-wide Android VPN interface.

### Ping differs from another app

Fox-VPN measures TCP connection timing to a server endpoint. Other clients may use ICMP or an HTTP test URL through the full VPN tunnel, so the values are not directly comparable.

### The native tunnel fails to build

Check that the required Android NDK is installed and that Git/network access is available for fetching the pinned native tunnel source.

## Contributing

Bug reports and focused pull requests are welcome. For connection issues, include the Android version, device/network type, selected server region, and a redacted diagnostic log. Never include passwords, access tokens, keystores, or unredacted personal IP information.

## License and attribution

Fox-VPN is distributed under the [MIT License](LICENSE). The project uses third-party libraries and the HEV SOCKS5 tunnel; see their upstream repositories and licenses for details.

**Mozilla, Firefox, and related marks belong to their respective owners.** Fox-VPN is an independent project and is not affiliated with Mozilla Corporation.
