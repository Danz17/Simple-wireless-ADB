package com.phenix.wirelessadb.model

/**
 * Defines the available connection modes for ADB access.
 */
enum class ConnectionMode {
  /**
   * Local WiFi connection - adb connect <wifi_ip>:5555
   * Standard wireless ADB over local network.
   */
  LOCAL_WIFI,

  /**
   * Tailscale Direct - adb connect <tailscale_ip>:5555
   * Direct VPN routing via Tailscale, no relay needed.
   * Uses same port as local ADB since Tailscale routes directly to adbd.
   */
  TAILSCALE_DIRECT,

  /**
   * Tailscale Relay - adb connect <tailscale_ip>:5556
   * Uses the relay server to bridge connections.
   * Useful when direct routing doesn't work.
   */
  TAILSCALE_RELAY,

  /**
   * Warpgate Bastion - adb connect <warpgate_host>:<port>
   * SSH tunnel through Warpgate bastion host.
   * Requires Warpgate server configuration.
   */
  WARPGATE,

  /**
   * P2P Token - Device-to-device connection using STUN/TURN (v1.2.0)
   * Uses NAT traversal for direct connections without relay server.
   * Requires token exchange between devices.
   */
  P2P_TOKEN
}
