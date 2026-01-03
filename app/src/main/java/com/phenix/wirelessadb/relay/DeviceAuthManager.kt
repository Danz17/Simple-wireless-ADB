package com.phenix.wirelessadb.relay

import android.content.Context
import com.google.gson.Gson
import com.phenix.wirelessadb.model.AuthMethod
import com.phenix.wirelessadb.model.DeviceIdentifier
import com.phenix.wirelessadb.model.TrustedDevice

/**
 * Manages trusted devices for the ADB relay server (v1.2.0).
 *
 * Enhanced to support:
 * - IP-based authentication (legacy)
 * - Hardware ID authentication (ANDROID_ID, fingerprint)
 * - Token-based authentication (P2P)
 *
 * Backwards compatible with v1.0 stored devices.
 */
class DeviceAuthManager(context: Context) {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val gson = Gson()

  // Legacy model for backwards compatibility with stored data
  private data class LegacyTrustedDevice(
    val ip: String,
    val name: String?,
    val addedAt: Long,
    val lastSeen: Long
  )

  /**
   * Check if a device IP is in the trusted list (legacy method).
   */
  fun isDeviceTrusted(clientIp: String): Boolean {
    return prefs.contains(clientIp) || findDeviceByIp(clientIp) != null
  }

  /**
   * Check if a device identifier is trusted (enhanced method).
   */
  fun isDeviceTrusted(identifier: DeviceIdentifier): Boolean {
    return findDeviceByIdentifier(identifier) != null
  }

  /**
   * Find a trusted device by IP address.
   */
  fun findDeviceByIp(ip: String): TrustedDevice? {
    return getTrustedDevices().find { it.ip == ip }
  }

  /**
   * Find a trusted device by hardware identifier.
   */
  fun findDeviceByIdentifier(identifier: DeviceIdentifier): TrustedDevice? {
    return getTrustedDevices().find { identifier.matchesTrustedDevice(it) }
  }

  /**
   * Find a trusted device by persistent token.
   */
  fun findDeviceByToken(token: String): TrustedDevice? {
    return getTrustedDevices().find { it.persistentToken == token }
  }

  /**
   * Add a device to the trusted list (legacy IP-only method).
   */
  fun addTrustedDevice(clientIp: String, name: String? = null) {
    val device = TrustedDevice.fromIpOnly(clientIp, name)
    saveDevice(device)
  }

  /**
   * Add an enhanced trusted device.
   */
  fun addTrustedDevice(device: TrustedDevice) {
    saveDevice(device)
  }

  /**
   * Add a device from identifier with auth method.
   */
  fun addTrustedDevice(
    identifier: DeviceIdentifier,
    ip: String?,
    name: String? = null,
    authMethod: AuthMethod = AuthMethod.IP_ADDRESS,
    persistentToken: String? = null
  ): TrustedDevice {
    val device = identifier.toTrustedDevice(
      ip = ip,
      name = name,
      authMethod = authMethod,
      persistentToken = persistentToken
    )
    saveDevice(device)
    return device
  }

  /**
   * Save a device to storage.
   */
  private fun saveDevice(device: TrustedDevice) {
    // Use device ID as the key for enhanced devices
    val key = device.id
    prefs.edit().putString(key, gson.toJson(device)).apply()
  }

  /**
   * Remove a device from the trusted list by IP.
   */
  fun removeTrustedDevice(clientIp: String) {
    // Try to find and remove by IP
    val device = findDeviceByIp(clientIp)
    if (device != null) {
      prefs.edit().remove(device.id).apply()
    }
    // Also remove legacy key if present
    prefs.edit().remove(clientIp).apply()
  }

  /**
   * Remove a device by its ID.
   */
  fun removeTrustedDeviceById(deviceId: String) {
    prefs.edit().remove(deviceId).apply()
  }

  /**
   * Get all trusted devices.
   */
  fun getTrustedDevices(): List<TrustedDevice> {
    return prefs.all.mapNotNull { (key, value) ->
      try {
        val json = value as String
        // Try parsing as enhanced device first
        try {
          gson.fromJson(json, TrustedDevice::class.java)
        } catch (e: Exception) {
          // Fallback: try parsing as legacy device
          val legacy = gson.fromJson(json, LegacyTrustedDevice::class.java)
          TrustedDevice(
            id = key, // Use the IP as ID for legacy devices
            ip = legacy.ip,
            name = legacy.name,
            addedAt = legacy.addedAt,
            lastSeen = legacy.lastSeen,
            authMethod = AuthMethod.IP_ADDRESS
          )
        }
      } catch (e: Exception) {
        null
      }
    }.sortedByDescending { it.lastSeen }
  }

  /**
   * Update the last seen timestamp for a device by IP.
   */
  fun updateLastSeen(clientIp: String) {
    val device = findDeviceByIp(clientIp) ?: return
    updateDevice(device.copy(lastSeen = System.currentTimeMillis()))
  }

  /**
   * Update the last seen timestamp for a device by identifier.
   */
  fun updateLastSeen(identifier: DeviceIdentifier) {
    val device = findDeviceByIdentifier(identifier) ?: return
    updateDevice(device.copy(lastSeen = System.currentTimeMillis()))
  }

  /**
   * Update a device in storage.
   */
  fun updateDevice(device: TrustedDevice) {
    prefs.edit().putString(device.id, gson.toJson(device)).apply()
  }

  /**
   * Update device name by IP.
   */
  fun updateDeviceName(clientIp: String, name: String) {
    val device = findDeviceByIp(clientIp) ?: return
    updateDevice(device.copy(name = name, lastSeen = System.currentTimeMillis()))
  }

  /**
   * Upgrade a device to persistent token auth.
   */
  fun upgradeToTokenAuth(
    device: TrustedDevice,
    persistentToken: String
  ): TrustedDevice {
    val upgraded = device.copy(
      persistentToken = persistentToken,
      authMethod = AuthMethod.TOKEN_PERSISTENT,
      lastSeen = System.currentTimeMillis()
    )
    updateDevice(upgraded)
    return upgraded
  }

  /**
   * Get trusted device count.
   */
  fun getTrustedDeviceCount(): Int {
    return prefs.all.size
  }

  /**
   * Remove all trusted devices.
   */
  fun clearAllDevices() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val PREFS_NAME = "trusted_devices"
  }
}
