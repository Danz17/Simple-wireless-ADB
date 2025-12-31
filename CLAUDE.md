# wirelessadb

## Project Identity

- **Name**: wirelessadb
- **Package**: com.phenix.wirelessadb
- **Type**: Native Android Utility (Kotlin)
- **Purpose**: Persistent Wireless ADB with Remote Tunnel Support
- **Author**: Phenix (Alaa Qweider)
- **Email**: Alaa@nulled.ai

## Goal

Build a lightweight utility that:
1. Enables wireless ADB automatically on boot
2. Keeps ADB connection persistent across reboots
3. Optionally creates a secure tunnel for remote ADB access over the internet
4. Works similar to Shizuku but focused on ADB connectivity

---

## Core Features

### v1.0 - Local Persistence
- [ ] Enable wireless ADB on device boot
- [ ] Auto-start service on boot (BOOT_COMPLETED receiver)
- [ ] Show persistent notification with connection status
- [ ] Display current IP:port for easy connection
- [ ] QR code for quick `adb connect` command
- [ ] Copy connection string to clipboard
- [ ] Toggle ADB on/off from notification

### v2.0 - Remote Tunnel (Future)
- [ ] Secure tunnel via reverse SSH / ngrok-like service
- [ ] Custom relay server support
- [ ] End-to-end encrypted tunnel
- [ ] Authentication (password/key-based)
- [ ] Connect from anywhere over internet
- [ ] Web dashboard for connection management

---

## Technical Approach

### Method 1: Shell Commands (Requires Root or ADB initially)
```bash
# Enable wireless ADB on port 5555
setprop service.adb.tcp.port 5555
stop adbd
start adbd

# Or using settings
settings put global adb_wifi_enabled 1
```

### Method 2: Shizuku-style (No Root after initial setup)
- Use Shizuku API to execute privileged commands
- One-time ADB setup, then app handles persistence
- Best for non-rooted devices

### Method 3: Root (If available)
- Direct shell access via `su`
- Most reliable for rooted devices

---

## Architecture

```
com.phenix.wirelessadb/
├── app/
│   └── src/main/
│       ├── java/com/phenix/wirelessadb/
│       │   ├── WirelessAdbApp.kt
│       │   ├── MainActivity.kt
│       │   │
│       │   ├── service/
│       │   │   ├── AdbService.kt           # Foreground service
│       │   │   └── BootReceiver.kt         # BOOT_COMPLETED
│       │   │
│       │   ├── adb/
│       │   │   ├── AdbManager.kt           # ADB control logic
│       │   │   ├── ShellExecutor.kt        # Shell command runner
│       │   │   └── ShizukuHelper.kt        # Shizuku integration
│       │   │
│       │   ├── tunnel/                     # v2.0
│       │   │   ├── TunnelManager.kt
│       │   │   ├── SshTunnel.kt
│       │   │   └── RelayClient.kt
│       │   │
│       │   ├── ui/
│       │   │   ├── MainScreen.kt
│       │   │   ├── SettingsScreen.kt
│       │   │   └── QrCodeView.kt
│       │   │
│       │   └── util/
│       │       ├── NetworkUtils.kt         # Get IP address
│       │       └── NotificationHelper.kt
│       │
│       ├── res/
│       └── AndroidManifest.xml
└── build.gradle.kts
```

---

## Dependencies

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")

// UI (minimal - utility app)
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// QR Code generation
implementation("com.google.zxing:core:3.5.2")

// Shizuku (for non-root privileged access)
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")

// Root (optional)
implementation("com.github.topjohnwu.libsu:core:5.2.2")
implementation("com.github.topjohnwu.libsu:service:5.2.2")

// Tunnel (v2.0)
// implementation("com.jcraft:jsch:0.1.55")  # SSH
```

---

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"
    tools:ignore="ProtectedPermissions" />
```

---

## Implementation Phases

### Phase 1: Basic Setup
1. Create Android project
2. Set up boot receiver
3. Implement foreground service
4. Add notification with IP display

### Phase 2: ADB Control
1. Implement shell command executor
2. Add ADB enable/disable logic
3. Test on rooted device
4. Add Shizuku support for non-root

### Phase 3: UI Polish
1. Main screen with status
2. QR code generation
3. Settings (port, auto-start)
4. Copy to clipboard

### Phase 4: Remote Tunnel (v2.0)
1. Research tunnel options (SSH, WebSocket, custom)
2. Implement secure tunnel client
3. Add authentication
4. Create relay server (separate project)

---

## Similar Apps for Reference

| App | Approach | Notes |
|-----|----------|-------|
| **Shizuku** | ADB/Root shell access | Industry standard for privileged ops |
| **LADB** | Local ADB via pairing | Works without root on Android 11+ |
| **WiFi ADB** | Simple toggle | Basic, requires initial USB setup |
| **Remote ADB Shell** | SSH tunnel | Paid, proprietary |

---

## Notes

- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Android 11+ has native wireless debugging (different from TCP ADB)
- Shizuku integration allows non-root operation after initial ADB grant
- Tunnel feature requires careful security consideration
- Consider using Android's native wireless debugging API on supported devices

---

## Security Considerations

- ADB access = full device control
- Tunnel must be encrypted (TLS/SSH)
- Authentication required for remote access
- Consider IP whitelisting
- Auto-disable after timeout option
- Show warning about security implications
