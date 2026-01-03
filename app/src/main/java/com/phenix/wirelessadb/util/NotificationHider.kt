package com.phenix.wirelessadb.util

import android.util.Log
import com.phenix.wirelessadb.shell.ExecutorBackend
import com.phenix.wirelessadb.shell.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Utility for hiding/showing the Android developer options USB debugging notification.
 * Requires ROOT access - Shizuku cannot do this (needs WRITE_SECURE_SETTINGS).
 */
object NotificationHider {

  private const val TAG = "NotificationHider"

  /**
   * Hide the USB debugging notification.
   * Uses multiple methods for compatibility across different Android versions/ROMs.
   *
   * @return Result indicating success or failure
   */
  suspend fun hideUsbDebuggingNotification(): Result<Unit> = withContext(Dispatchers.IO) {
    if (ShellExecutor.backend != ExecutorBackend.ROOT) {
      return@withContext Result.failure(
        Exception("Notification hiding requires ROOT access. Shizuku cannot perform this operation.")
      )
    }

    try {
      // Method 1: Try the adb_notify setting (works on some ROMs)
      val method1 = ShellExecutor.execute("settings put global adb_notify 0")

      // Method 2: Try blocking the notification channel (Android 8+)
      val method2 = ShellExecutor.execute(
        "cmd notification block_notifications com.android.systemui development"
      )

      // Method 3: Try disabling development settings notification
      val method3 = ShellExecutor.execute(
        "settings put secure development_settings_enabled 0"
      )

      // Check if any method succeeded
      val success = method1.isSuccess || method2.isSuccess || method3.isSuccess
      if (success) {
        Log.i(TAG, "USB debugging notification hidden")
        Result.success(Unit)
      } else {
        Result.failure(Exception("All methods to hide notification failed"))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to hide notification: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Show the USB debugging notification (restore default behavior).
   *
   * @return Result indicating success or failure
   */
  suspend fun showUsbDebuggingNotification(): Result<Unit> = withContext(Dispatchers.IO) {
    if (ShellExecutor.backend != ExecutorBackend.ROOT) {
      return@withContext Result.failure(
        Exception("Notification control requires ROOT access.")
      )
    }

    try {
      ShellExecutor.execute(
        "settings put global adb_notify 1",
        "cmd notification unblock_notifications com.android.systemui development",
        "settings put secure development_settings_enabled 1"
      )
      Log.i(TAG, "USB debugging notification restored")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to show notification: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Check if the USB debugging notification is currently hidden.
   *
   * @return true if hidden, false if visible, null if cannot determine
   */
  suspend fun isNotificationHidden(): Boolean? = withContext(Dispatchers.IO) {
    try {
      val process = Runtime.getRuntime().exec(arrayOf("settings", "get", "global", "adb_notify"))
      val completed = process.waitFor(3, TimeUnit.SECONDS)
      if (!completed) {
        process.destroyForcibly()
        return@withContext null
      }
      val result = process.inputStream.bufferedReader().readLine()?.trim()
      result == "0"
    } catch (e: Exception) {
      Log.w(TAG, "Failed to check notification status: ${e.message}")
      null
    }
  }

  /**
   * Check if hiding notifications is supported (requires root).
   */
  fun isSupported(): Boolean = ShellExecutor.backend == ExecutorBackend.ROOT
}
