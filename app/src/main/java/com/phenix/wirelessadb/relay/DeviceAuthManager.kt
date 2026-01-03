package com.phenix.wirelessadb.relay

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages trusted devices for the ADB relay server.
 * Stores device IPs with metadata in SharedPreferences.
 */
class DeviceAuthManager(context: Context) {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val gson = Gson()

  data class TrustedDevice(
    val ip: String,
    val name: String?,
    val addedAt: Long,
    val lastSeen: Long
  )

  /**
   * Check if a device IP is in the trusted list.
   */
  fun isDeviceTrusted(clientIp: String): Boolean {
    return prefs.contains(clientIp)
  }

  /**
   * Add a device to the trusted list.
   */
  fun addTrustedDevice(clientIp: String, name: String? = null) {
    val now = System.currentTimeMillis()
    val device = TrustedDevice(
      ip = clientIp,
      name = name,
      addedAt = now,
      lastSeen = now
    )
    prefs.edit().putString(clientIp, gson.toJson(device)).apply()
  }

  /**
   * Remove a device from the trusted list.
   */
  fun removeTrustedDevice(clientIp: String) {
    prefs.edit().remove(clientIp).apply()
  }

  /**
   * Get all trusted devices.
   */
  fun getTrustedDevices(): List<TrustedDevice> {
    return prefs.all.mapNotNull { (_, value) ->
      try {
        gson.fromJson(value as String, TrustedDevice::class.java)
      } catch (e: Exception) {
        null
      }
    }.sortedByDescending { it.lastSeen }
  }

  /**
   * Update the last seen timestamp for a device.
   */
  fun updateLastSeen(clientIp: String) {
    val json = prefs.getString(clientIp, null) ?: return
    try {
      val device = gson.fromJson(json, TrustedDevice::class.java)
      val updated = device.copy(lastSeen = System.currentTimeMillis())
      prefs.edit().putString(clientIp, gson.toJson(updated)).apply()
    } catch (e: Exception) {
      // Ignore parsing errors
    }
  }

  /**
   * Update device name.
   */
  fun updateDeviceName(clientIp: String, name: String) {
    val json = prefs.getString(clientIp, null) ?: return
    try {
      val device = gson.fromJson(json, TrustedDevice::class.java)
      val updated = device.copy(name = name, lastSeen = System.currentTimeMillis())
      prefs.edit().putString(clientIp, gson.toJson(updated)).apply()
    } catch (e: Exception) {
      // Ignore parsing errors
    }
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
