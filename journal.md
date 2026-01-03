# RootADB Pro Development Journal

## Session: 2026-01-03

### v1.0.0 Release Preparation

#### Rebranding
- Renamed from "Super ADB" / "Simple Wireless ADB" to **RootADB Pro**
- Updated all string resources and documentation
- Version set to 1.0.0

#### New Features Added
- **Relay Type Dropdown**: Added dropdown in Remote Relay tab with options:
  - Tailscale (active)
  - SSH Tunnel (Coming Soon)
  - Custom Relay (Coming Soon)
- **Split Tunnel Guide**: Added Help section explaining Tailscale split tunneling for performance

#### v2.1.3 Fix - ConnectivityManager API Solution

**Problem**: Shell commands (`ip addr show wlan0/tun0`) and `NetworkInterface.getNetworkInterfaces()` fail silently on Android when VPN is active.

**Root Cause**: Android blocks apps from running network shell commands. The NetworkInterface API also doesn't reliably enumerate TUN interfaces.

**Solution**: Used `ConnectivityManager.allNetworks` API to enumerate all networks:

```kotlin
// TailscaleHelper.kt - getTailscaleIpFromApi()
val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
val allNetworks = cm.allNetworks
for (network in allNetworks) {
  val caps = cm.getNetworkCapabilities(network)
  if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
    val linkProps = cm.getLinkProperties(network)
    val ip = linkProps.linkAddresses
      .filterIsInstance<Inet4Address>()
      .firstOrNull { isTailscaleIp(it) }
    // Found Tailscale IP (100.x.x.x)
  }
}
```

Similarly for WiFi IP in `AdbManager.kt`:
```kotlin
if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
  // Get WiFi IP, filter out Tailscale range
}
```

**Result**: Both tabs now work simultaneously:
- Local ADB: Shows 10.1.1.107:5555 (WiFi)
- Remote Relay: Shows 100.99.87.44:5556 (Tailscale)

---

## Session: 2026-01-02

### Completed Fixes

#### v2.1.1 - Tailscale Detection Fix
- **Problem**: App showed "Tailscale not connected" even when Tailscale was active
- **Root Cause**: `NetworkInterface.getNetworkInterfaces()` doesn't reliably enumerate TUN interfaces on Android
- **Solution**: Added shell fallback in `TailscaleHelper.kt` using `ip addr show tun0`
- **Status**: Superseded by v2.1.3 ConnectivityManager fix

#### v2.1.2 - WiFi IP Detection Fix
- **Problem**: Local ADB tab showed Tailscale IP (100.x.x.x) instead of WiFi IP (10.x.x.x)
- **Root Cause**: `ConnectivityManager.activeNetwork` returns VPN addresses when Tailscale is active
- **Solution**: Added shell fallback in `AdbManager.kt` using `ip addr show wlan0` + `isTailscaleIp()` filter
- **Status**: Superseded by v2.1.3 ConnectivityManager fix

---

## Network Interface Reference

```
wlan0: 10.1.1.107/24 (WiFi - always present when connected)
tun0: 100.99.87.44/32 (Tailscale - only when VPN active)
```

## Tab Structure Reference

| Tab | Fragment | IP Source | Port |
|-----|----------|-----------|------|
| 0 - Local ADB | LocalAdbFragment | WiFi (TRANSPORT_WIFI) | 5555 |
| 1 - Remote Relay | RemoteRelayFragment | VPN (TRANSPORT_VPN) | 5556 |
| 2 - Help | HelpFragment | N/A | N/A |

## Key Technical Insights

1. **ConnectivityManager.allNetworks** is the reliable way to enumerate all network interfaces on Android
2. **NetworkCapabilities.TRANSPORT_VPN** identifies VPN networks (including Tailscale)
3. **NetworkCapabilities.TRANSPORT_WIFI** identifies WiFi networks
4. **LinkProperties.linkAddresses** provides all IP addresses for a network
5. Tailscale uses CGNAT range: 100.64.0.0/10 (100.64.x.x - 100.127.x.x)
