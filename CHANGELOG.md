# Changelog

All notable changes to RootADB Pro will be documented in this file.

## [1.0.0] - 2025-01-03

### Initial Public Release

**Local ADB**
- Enable/disable wireless ADB with one tap
- Custom port configuration (default: 5555)
- Auto-enable on device boot
- Copy `adb connect` command to clipboard
- Persistent notification with quick toggle
- Real-time status indicator

**Remote Relay (Tailscale)**
- Built-in TCP relay server for remote ADB access
- Auto-detect Tailscale IP (100.x.x.x range)
- Device authentication with persistent trust
- Approve/deny new device connections on-device
- Relay type selector (SSH Tunnel & Custom Relay coming soon)

**UI/UX**
- Material 3 design language
- Dark mode support (follows system theme)
- Edge-to-edge modern UI
- Tabbed interface: Local ADB / Remote Relay / Help
- Split tunnel setup guide in Help tab

**Technical**
- Fixed: Tailscale IP detection with VPN active (uses ConnectivityManager API)
- Fixed: WiFi IP detection no longer affected by VPN
- Uses Ktor for TCP relay server
- Gson for trusted device persistence

### Requirements
- Android 8.0+ (API 26)
- Root access (Magisk, KernelSU, etc.)
- Tailscale (optional, for remote relay)
