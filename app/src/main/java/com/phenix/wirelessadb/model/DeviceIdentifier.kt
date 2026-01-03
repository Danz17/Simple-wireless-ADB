package com.phenix.wirelessadb.model

import java.security.MessageDigest

/**
 * Device identification data for connecting clients (v1.2.0).
 *
 * Used to identify a device during P2P token exchange.
 * This data is sent by the connecting device to prove identity.
 */
data class DeviceIdentifier(
  /** ANDROID_ID (Settings.Secure) */
  val androidId: String,

  /** Build.FINGERPRINT */
  val buildFingerprint: String,

  /** WiFi MAC address (null if unavailable) */
  val macAddress: String?,

  /** Build.MODEL */
  val deviceModel: String,

  /** Build.MANUFACTURER */
  val manufacturer: String
) {
  /**
   * Compute a 16-character hash of this device's identifiers.
   * Used for quick comparison and display (masked tokens).
   */
  fun computeHash(): String {
    val input = "$androidId|$buildFingerprint|${macAddress ?: ""}|$deviceModel|$manufacturer"
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return hashBytes.take(8).joinToString("") { "%02x".format(it) }
  }

  /**
   * Get display name for this device.
   */
  fun getDisplayName(): String {
    return "$manufacturer $deviceModel"
  }

  /**
   * Create a TrustedDevice from this identifier.
   */
  fun toTrustedDevice(
    ip: String?,
    name: String? = null,
    authMethod: AuthMethod = AuthMethod.TOKEN_ONCE,
    persistentToken: String? = null
  ): TrustedDevice {
    val now = System.currentTimeMillis()
    return TrustedDevice(
      id = java.util.UUID.randomUUID().toString(),
      ip = ip,
      name = name ?: getDisplayName(),
      addedAt = now,
      lastSeen = now,
      hardwareId = androidId,
      buildFingerprint = buildFingerprint,
      macAddress = macAddress,
      deviceModel = deviceModel,
      manufacturer = manufacturer,
      persistentToken = persistentToken,
      authMethod = authMethod,
      deviceHash = computeHash()
    )
  }

  /**
   * Check if this identifier matches a trusted device.
   */
  fun matchesTrustedDevice(device: TrustedDevice): Boolean {
    // Match by device hash
    val myHash = computeHash()
    if (!device.deviceHash.isNullOrEmpty() && device.deviceHash == myHash) {
      return true
    }
    // Fallback to hardware ID
    if (!device.hardwareId.isNullOrEmpty() && device.hardwareId == androidId) {
      return true
    }
    return false
  }

  companion object {
    /**
     * Create an identifier for comparison from partial data.
     */
    fun fromPartial(
      androidId: String,
      buildFingerprint: String = "",
      macAddress: String? = null,
      deviceModel: String = "Unknown",
      manufacturer: String = "Unknown"
    ): DeviceIdentifier {
      return DeviceIdentifier(
        androidId = androidId,
        buildFingerprint = buildFingerprint,
        macAddress = macAddress,
        deviceModel = deviceModel,
        manufacturer = manufacturer
      )
    }
  }
}
