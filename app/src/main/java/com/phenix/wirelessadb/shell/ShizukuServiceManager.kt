package com.phenix.wirelessadb.shell

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

/**
 * Manages the Shizuku UserService connection lifecycle.
 *
 * Provides thread-safe access to the IShellService running in Shizuku's
 * privileged process.
 */
object ShizukuServiceManager {

  private const val TAG = "ShizukuServiceManager"
  private const val BIND_TIMEOUT_MS = 10_000L

  private var shellService: IShellService? = null
  private var isBound = false
  private var isBinding = false
  private var cachedUid: Int? = null

  private val mutex = Mutex()
  private var bindDeferred: CompletableDeferred<Boolean>? = null

  private val userServiceArgs = Shizuku.UserServiceArgs(
    ComponentName(
      "com.phenix.wirelessadb",
      ShellUserService::class.java.name
    )
  )
    .daemon(false)       // Don't keep alive after unbind
    .processNameSuffix("shell_service")
    .debuggable(true)
    .version(1)

  private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
      Log.d(TAG, "UserService connected: $name")
      if (binder != null) {
        shellService = IShellService.Stub.asInterface(binder)
        isBound = true
        isBinding = false

        // Cache the UID
        try {
          cachedUid = shellService?.uid
          Log.i(TAG, "Connected to ShellUserService (UID: $cachedUid)")
        } catch (e: Exception) {
          Log.w(TAG, "Failed to get UID: ${e.message}")
        }

        bindDeferred?.complete(true)
      } else {
        Log.e(TAG, "UserService connected with null binder")
        isBound = false
        isBinding = false
        bindDeferred?.complete(false)
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      Log.w(TAG, "UserService disconnected: $name")
      shellService = null
      isBound = false
      cachedUid = null
    }
  }

  /**
   * Bind to the Shizuku UserService.
   * Returns true if binding succeeds, false otherwise.
   */
  suspend fun bind(): Boolean = mutex.withLock {
    if (isBound && shellService != null) {
      Log.d(TAG, "Already bound to UserService")
      return true
    }

    if (isBinding) {
      Log.d(TAG, "Already binding, waiting for result...")
      return bindDeferred?.await() ?: false
    }

    if (!Shizuku.pingBinder()) {
      Log.e(TAG, "Shizuku binder not available")
      return false
    }

    return try {
      isBinding = true
      bindDeferred = CompletableDeferred()

      Log.d(TAG, "Binding to UserService...")
      Shizuku.bindUserService(userServiceArgs, serviceConnection)

      withTimeout(BIND_TIMEOUT_MS) {
        bindDeferred?.await() ?: false
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "UserService bind timed out")
      isBinding = false
      false
    } catch (e: Exception) {
      Log.e(TAG, "Failed to bind UserService: ${e.message}")
      isBinding = false
      false
    }
  }

  /**
   * Unbind from the Shizuku UserService.
   */
  suspend fun unbind() = mutex.withLock {
    if (!isBound) return@withLock

    try {
      Log.d(TAG, "Unbinding from UserService...")
      Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
      shellService = null
      isBound = false
      cachedUid = null
    } catch (e: Exception) {
      Log.e(TAG, "Failed to unbind UserService: ${e.message}")
    }
  }

  /**
   * Get the bound service, binding if necessary.
   */
  suspend fun getService(): IShellService? {
    if (!isBound || shellService == null) {
      if (!bind()) {
        return null
      }
    }
    return shellService
  }

  /**
   * Execute a command using the UserService.
   */
  suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val service = getService()
        ?: return@withContext Result.failure(Exception("Failed to connect to ShellUserService"))

      val resultJson = service.executeCommand(command)
      val result = CommandResult.fromJson(resultJson)

      if (result.success) {
        Result.success(result.stdout)
      } else {
        Result.failure(Exception("Exit code ${result.exitCode}: ${result.stderr}"))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Command execution failed: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Check if the UserService is running with root privileges.
   */
  suspend fun isRunningAsRoot(): Boolean {
    val service = getService() ?: return false
    return try {
      service.isRoot
    } catch (e: Exception) {
      Log.e(TAG, "Failed to check root status: ${e.message}")
      false
    }
  }

  /**
   * Get the UID the UserService is running as.
   */
  suspend fun getServiceUid(): Int? {
    if (cachedUid != null) return cachedUid

    val service = getService() ?: return null
    return try {
      cachedUid = service.uid
      cachedUid
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get UID: ${e.message}")
      null
    }
  }

  /**
   * Check if currently bound to the UserService.
   */
  fun isBound(): Boolean = isBound && shellService != null
}
