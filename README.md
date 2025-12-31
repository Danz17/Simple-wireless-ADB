# Wireless ADB

Lightweight Android app to enable wireless ADB debugging on rooted devices.

## Features

- Enable/disable wireless ADB with one tap
- Custom port configuration (default: 5555)
- Auto-enable on boot
- Copy `adb connect` command to clipboard
- Persistent notification when active
- **No ads, no analytics, no bloat** (~1MB vs 75MB original)

## Requirements

- Android 8.0+ (API 26)
- Root access (Magisk, KernelSU, etc.)

## Build

1. Open this folder in Android Studio
2. Sync Gradle
3. Build → Build APK

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

## Reference

The `reference/` folder contains the decompiled original app (com.origiq.wirelessadb) for comparison.

## Author

Phenix (com.phenix.wirelessadb)
