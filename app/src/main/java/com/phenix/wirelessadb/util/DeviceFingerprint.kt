package com.phenix.wirelessadb.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.phenix.wirelessadb.model.DeviceIdentifier

/**
 * Utility for reading device identification data (v1.2.0).
 *
 * Gathers various device identifiers for trust verification:
 * - ANDROID_ID (unique per app/device)
 * - Build fingerprint (ROM/build info)
 * - WiFi MAC address (if accessible)
 * - Device model and manufacturer
 */
object DeviceFingerprint {

  /**
   * Get the current device's identifier data.
   */
  @SuppressLint("HardwareIds")
  fun getDeviceIdentifier(context: Context): DeviceIdentifier {
    val androidId = Settings.Secure.getString(
      context.contentResolver,
      Settings.Secure.ANDROID_ID
    ) ?: "unknown"

    val buildFingerprint = Build.FINGERPRINT
    val macAddress = getMacAddress(context)
    val deviceModel = Build.MODEL
    val manufacturer = Build.MANUFACTURER

    return DeviceIdentifier(
      androidId = androidId,
      buildFingerprint = buildFingerprint,
      macAddress = macAddress,
      deviceModel = deviceModel,
      manufacturer = manufacturer
    )
  }

  /**
   * Get WiFi MAC address if available.
   * Returns null if not accessible (common on Android 10+).
   */
  @SuppressLint("HardwareIds", "MissingPermission")
  private fun getMacAddress(context: Context): String? {
    return try {
      // On Android 10+, this returns a randomized MAC
      // We try anyway as some manufacturers expose the real MAC
      val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as? WifiManager
      @Suppress("DEPRECATION")
      wifiManager?.connectionInfo?.macAddress?.let { mac ->
        // Filter out the placeholder MAC that Android returns
        if (mac != "02:00:00:00:00:00") mac else null
      }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Get a short device description for display.
   */
  fun getDeviceDescription(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
  }

  /**
   * Get Android version info.
   */
  fun getAndroidVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
  }

  /**
   * Compute hash for the current device.
   */
  fun computeDeviceHash(context: Context): String {
    return getDeviceIdentifier(context).computeHash()
  }
}
