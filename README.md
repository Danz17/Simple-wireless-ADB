# Simple Wireless ADB

**ROOT REQUIRED**

Lightweight Android app to enable wireless ADB debugging on rooted devices. Clean, modern UI with dark mode support.

## Features

- Enable/disable wireless ADB with one tap
- Custom port configuration (default: 5555)
- Auto-enable on boot
- Copy `adb connect` command to clipboard
- Persistent notification when active
- Status indicator (green = connected)
- Dark mode support (follows system)
- Edge-to-edge modern UI
- **No ads, no analytics, no bloat** (~1MB)

## Screenshots

| Light Mode | Dark Mode |
|------------|-----------|
| Coming soon | Coming soon |

## Requirements

- Android 8.0+ (API 26)
- Root access (Magisk, KernelSU, etc.)

## Installation

1. Download the latest APK from [Releases](../../releases)
2. Install on your rooted device
3. Grant root permissions when prompted
4. Tap "Enable" to start wireless ADB

## Usage

```bash
# On your PC, connect to your device
adb connect <DEVICE_IP>:5555

# Example
adb connect 192.168.1.100:5555
```

## Build

```bash
# Clone the repo
git clone https://github.com/AlaQwe/Simple-wireless-ADB.git
cd Simple-wireless-ADB

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Project Structure

```
app/src/main/java/com/phenix/wirelessadb/
├── AdbManager.kt      # Core ADB logic (root shell commands)
├── AdbService.kt      # Foreground service + notification
├── BootReceiver.kt    # Auto-enable on device boot
├── MainActivity.kt    # UI controller
└── PrefsManager.kt    # SharedPreferences wrapper
```

## How It Works

Executes root commands to configure ADB daemon:

```bash
su -c setprop service.adb.tcp.port <PORT>
su -c stop adbd
su -c start adbd
```

## Roadmap

### v1.0 (Current)
- [x] Enable/disable wireless ADB
- [x] Custom port configuration
- [x] Auto-enable on boot
- [x] Copy command to clipboard
- [x] Persistent notification
- [x] Modern Material 3 UI
- [x] Dark mode support

### v2.0 (Planned)
- [ ] QR code for quick connection
- [ ] Shizuku support (non-root option)
- [ ] Connection history

### v3.0 (Research)
- [ ] **Remote ADB over Internet** - Secure tunnel for remote device access
  - SSH tunnel integration
  - Custom relay server support
  - End-to-end encryption
  - Authentication (password/key-based)
  - Web dashboard for connection management

## License

MIT License - See [LICENSE](LICENSE) for details.

## Author

**Phenix** (Alaa Qweider)
- Package: `com.phenix.wirelessadb`
