package com.phenix.wirelessadb.model

/**
 * Enhanced trusted device model (v1.2.0).
 *
 * Supports multiple identification methods:
 * - IP address (legacy, less secure)
 * - Hardware ID (ANDROID_ID)
 * - Build fingerprint
 * - MAC address (when available)
 * - Persistent token (for P2P connections)
 */
data class TrustedDevice(
  /** Unique device identifier (UUID) */
  val id: String,

  /** Device IP address (may change) */
  val ip: String?,

  /** User-assigned device name */
  val name: String?,

  /** Timestamp when device was first trusted */
  val addedAt: Long,

  /** Timestamp of last connection */
  val lastSeen: Long,

  // v1.2.0 Enhanced identification fields

  /** Hardware ID (ANDROID_ID) - stable per device/app combo */
  val hardwareId: String? = null,

  /** Build fingerprint - identifies device/ROM combination */
  val buildFingerprint: String? = null,

  /** WiFi MAC address (if available) */
  val macAddress: String? = null,

  /** Device model string (e.g., "Pixel 6 Pro") */
  val deviceModel: String? = null,

  /** Device manufacturer (e.g., "Google") */
  val manufacturer: String? = null,

  /** Persistent token for trusted P2P connections */
  val persistentToken: String? = null,

  /** How this device was authenticated */
  val authMethod: AuthMethod = AuthMethod.IP_ADDRESS,

  /** Computed hash of device identifiers (16-char) */
  val deviceHash: String? = null
) {
  /**
   * Check if device can be identified by hardware (not just IP).
   */
  fun hasHardwareId(): Boolean {
    return !hardwareId.isNullOrEmpty() || !buildFingerprint.isNullOrEmpty()
  }

  /**
   * Get display name (user name or device model or "Unknown Device").
   */
  fun getDisplayName(): String {
    return name
      ?: deviceModel?.let { model ->
        manufacturer?.let { "$it $model" } ?: model
      }
      ?: "Unknown Device"
  }

  /**
   * Get short device hash for display (last 4 chars).
   */
  fun getShortHash(): String {
    return deviceHash?.takeLast(4) ?: "????"
  }

  /**
   * Check if this device matches another by hardware ID.
   */
  fun matchesByHardware(other: TrustedDevice): Boolean {
    // Match by device hash if available
    if (!deviceHash.isNullOrEmpty() && !other.deviceHash.isNullOrEmpty()) {
      return deviceHash == other.deviceHash
    }
    // Fallback to hardware ID
    if (!hardwareId.isNullOrEmpty() && !other.hardwareId.isNullOrEmpty()) {
      return hardwareId == other.hardwareId
    }
    return false
  }

  companion object {
    /**
     * Create a legacy device (IP-only, for backwards compatibility).
     */
    fun fromIpOnly(ip: String, name: String? = null): TrustedDevice {
      val now = System.currentTimeMillis()
      return TrustedDevice(
        id = java.util.UUID.randomUUID().toString(),
        ip = ip,
        name = name,
        addedAt = now,
        lastSeen = now,
        authMethod = AuthMethod.IP_ADDRESS
      )
    }
  }
}
