package com.phenix.wirelessadb.shell

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Shell executor using Shizuku's UserService for proper privileged access.
 *
 * This implementation correctly delegates commands to a UserService running
 * in Shizuku's privileged process, rather than incorrectly calling Runtime.exec()
 * in the app's own process.
 *
 * The UserService runs with:
 * - UID 0 (root) when Shizuku is started with root
 * - UID 2000 (shell) when Shizuku is started via ADB
 */
object ShizukuExecutor {

  private const val TAG = "ShizukuExecutor"
  private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

  /**
   * Check if Shizuku binder is available (Shizuku app is running).
   */
  fun isBinderAlive(): Boolean {
    return try {
      Shizuku.pingBinder()
    } catch (e: Exception) {
      Log.w(TAG, "Shizuku binder check failed: ${e.message}")
      false
    }
  }

  /**
   * Check if Shizuku permission is granted.
   */
  fun isPermissionGranted(): Boolean {
    return try {
      if (!isBinderAlive()) return false
      Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
      Log.w(TAG, "Permission check failed: ${e.message}")
      false
    }
  }

  /**
   * Check if Shizuku is fully available (binder alive + permission granted).
   */
  fun isAvailable(): Boolean {
    return isBinderAlive() && isPermissionGranted()
  }

  /**
   * Request Shizuku permission.
   * @param requestCode Request code for callback
   */
  fun requestPermission(requestCode: Int = SHIZUKU_PERMISSION_REQUEST_CODE) {
    try {
      if (!isPermissionGranted()) {
        Shizuku.requestPermission(requestCode)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
    }
  }

  /**
   * Detect whether Shizuku is running as root or shell.
   * @return SHIZUKU_ROOT if running as UID 0, SHIZUKU_SHELL if UID 2000, NONE otherwise
   */
  suspend fun detectBackend(): ExecutorBackend = withContext(Dispatchers.IO) {
    if (!isAvailable()) {
      return@withContext ExecutorBackend.NONE
    }

    val uid = ShizukuServiceManager.getServiceUid()
    when (uid) {
      0 -> ExecutorBackend.SHIZUKU_ROOT
      2000 -> ExecutorBackend.SHIZUKU_SHELL
      else -> {
        Log.w(TAG, "Unexpected Shizuku UID: $uid")
        // If we got a UID, Shizuku is working
        if (uid != null) ExecutorBackend.SHIZUKU_SHELL else ExecutorBackend.NONE
      }
    }
  }

  /**
   * Execute shell command via Shizuku UserService.
   * The command runs in Shizuku's privileged process with UID 0 or 2000.
   *
   * @param command Shell command to execute
   * @return Result with stdout on success
   */
  suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
    if (!isAvailable()) {
      return@withContext Result.failure(
        Exception("Shizuku not available or permission denied")
      )
    }

    ShizukuServiceManager.execute(command)
  }

  /**
   * Bind to the UserService.
   * Call this early in app lifecycle to reduce latency on first command.
   */
  suspend fun bind(): Boolean {
    if (!isAvailable()) return false
    return ShizukuServiceManager.bind()
  }

  /**
   * Unbind from the UserService.
   * Call this when the app is going to background or being destroyed.
   */
  suspend fun unbind() {
    ShizukuServiceManager.unbind()
  }

  /**
   * Check if the UserService is running with root privileges.
   */
  suspend fun isRunningAsRoot(): Boolean {
    return ShizukuServiceManager.isRunningAsRoot()
  }

  /**
   * Add Shizuku permission result listener.
   */
  fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
    try {
      Shizuku.addRequestPermissionResultListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add permission listener: ${e.message}")
    }
  }

  /**
   * Remove Shizuku permission result listener.
   */
  fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
    try {
      Shizuku.removeRequestPermissionResultListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to remove permission listener: ${e.message}")
    }
  }

  /**
   * Add Shizuku binder received listener (for when Shizuku starts).
   */
  fun addBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
    try {
      Shizuku.addBinderReceivedListenerSticky(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add binder listener: ${e.message}")
    }
  }

  /**
   * Remove Shizuku binder received listener.
   */
  fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
    try {
      Shizuku.removeBinderReceivedListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to remove binder listener: ${e.message}")
    }
  }

  /**
   * Add Shizuku binder dead listener (for when Shizuku stops).
   */
  fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
    try {
      Shizuku.addBinderDeadListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add binder dead listener: ${e.message}")
    }
  }

  /**
   * Remove Shizuku binder dead listener.
   */
  fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
    try {
      Shizuku.removeBinderDeadListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to remove binder dead listener: ${e.message}")
    }
  }
}
