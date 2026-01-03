# RootADB Pro

**Persistent Wireless ADB with Remote Access**

Lightweight Android utility for rooted devices that enables wireless ADB debugging with optional remote access via Tailscale. Clean Material 3 UI with dark mode support.

![RootADB Pro](screenshot.png)

## Features

### Local ADB (WiFi)
- Enable/disable wireless ADB with one tap
- Custom port configuration (default: 5555)
- Auto-enable on device boot
- Copy `adb connect` command to clipboard
- Persistent notification when active
- Real-time status indicator

### Remote Relay (Tailscale)
- Built-in TCP relay for remote ADB access
- Auto-detect Tailscale IP (100.x.x.x range)
- Device authentication with persistent trust
- Approve/deny new device connections
- Relay type selector (SSH Tunnel & Custom Relay coming soon)

### UI/UX
- Material 3 design language
- Dark mode support (follows system)
- Edge-to-edge modern UI
- Tabbed interface (Local ADB / Remote Relay / Help)
- **No ads, no analytics, no bloat** (~2MB)

## Requirements

- Android 8.0+ (API 26)
- Root access (Magisk, KernelSU, etc.)
- Tailscale (optional, for remote relay)

## Installation

1. Download the latest APK from [Releases](../../releases)
2. Install on your rooted device
3. Grant root permissions when prompted
4. Enable wireless ADB from the Local ADB tab

## Usage

### Local Connection (WiFi)

```bash
# On your PC (same WiFi network)
adb connect <DEVICE_IP>:5555

# Example
adb connect 192.168.1.100:5555
```

### Remote Connection (Tailscale)

```bash
# On your PC (connected to same Tailscale network)
adb connect <TAILSCALE_IP>:5556

# Example
adb connect 100.99.87.44:5556
```

First connection from a new device requires approval on your phone. Approved devices auto-connect on subsequent connections.

## Split Tunnel Setup (Recommended)

For best performance with Tailscale remote access:

1. Open Tailscale app on your phone
2. Go to **Settings > Split Tunneling**
3. Enable "Use Tailscale for specific apps"
4. Add only: **RootADB Pro**

This routes only ADB traffic through Tailscale, keeping other apps at full speed while maintaining remote ADB access.

## How It Works

### Architecture

```
LOCAL ADB:
[PC on WiFi] ────> [Android:5555] ────> [ADB Daemon]
                        │
                   RootADB Pro
                   (enables ADB)

REMOTE RELAY:
[Remote PC] ──Tailscale VPN──> [Android:5556] ──relay──> [ADB:5555]
    │                               │                        │
100.x.x.x                      RootADB Pro              Local Daemon
                              (authenticates)
```

### Root Commands

```bash
su -c setprop service.adb.tcp.port <PORT>
su -c stop adbd
su -c start adbd
```

## Security

| Layer | Protection |
|-------|------------|
| Network | Tailscale relay only accepts 100.x.x.x range |
| Device Auth | First connection requires phone approval |
| Persistence | Approved devices remembered, auto-connect |
| ADB Auth | Standard RSA key authentication still applies |
| Encryption | Tailscale provides WireGuard encryption |

## Build

```bash
# Clone the repo
git clone https://github.com/AlaQwe/RootADB-Pro.git
cd RootADB-Pro

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Project Structure

```
app/src/main/java/com/phenix/wirelessadb/
├── AdbManager.kt           # Core ADB logic (root shell, IP detection)
├── AdbService.kt           # Foreground service + notification + relay
├── BootReceiver.kt         # Auto-enable on device boot
├── MainActivity.kt         # ViewPager + tab navigation
├── PrefsManager.kt         # SharedPreferences wrapper
├── ui/
│   ├── LocalAdbFragment.kt     # Tab 1: WiFi ADB
│   ├── RemoteRelayFragment.kt  # Tab 2: Tailscale relay
│   └── HelpFragment.kt         # Tab 3: Help & about
├── viewmodel/
│   └── AdbViewModel.kt         # Shared state management
└── relay/
    ├── AdbRelayServer.kt       # TCP relay server with auth
    ├── ConnectionProxy.kt      # Bidirectional socket bridge
    ├── DeviceAuthManager.kt    # Trusted device whitelist
    └── TailscaleHelper.kt      # Tailscale IP detection
```

## Roadmap

### v1.0.0 (Current)
- [x] Enable/disable wireless ADB
- [x] Custom port configuration
- [x] Auto-enable on boot
- [x] Tailscale relay server
- [x] Device authentication
- [x] Material 3 UI with dark mode
- [x] Tabbed interface
- [x] Split tunnel documentation
- [x] Relay type selector (future-ready)

### v1.1.0 (Planned)
- [ ] SSH Tunnel relay option
- [ ] Custom relay server support
- [ ] QR code for quick connection
- [ ] Connection history

### v2.0.0 (Future)
- [ ] Trusted device management screen
- [ ] PC companion scripts (auto-connect)
- [ ] Widget for quick toggle

## License

MIT License - See [LICENSE](LICENSE) for details.

## Author

**Phenix** (Alaa Qweider)
Email: alaa@nulled.ai
Package: `com.phenix.wirelessadb`
