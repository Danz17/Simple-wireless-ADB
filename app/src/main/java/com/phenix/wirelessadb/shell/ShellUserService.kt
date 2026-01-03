package com.phenix.wirelessadb.shell

import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * UserService implementation that runs in Shizuku's privileged process.
 *
 * This class runs with UID 2000 (shell) when Shizuku is started via ADB,
 * or UID 0 (root) when Shizuku is started via root.
 *
 * IMPORTANT: This class is instantiated by Shizuku in a separate process.
 * It has access to elevated privileges that the main app process doesn't have.
 */
class ShellUserService : IShellService.Stub() {

  companion object {
    private const val DEFAULT_TIMEOUT_MS = 30_000L
  }

  /**
   * Execute a shell command with elevated privileges.
   * This runs in Shizuku's process with UID 0 or 2000.
   */
  override fun executeCommand(command: String): String {
    return executeCommandWithTimeout(command, DEFAULT_TIMEOUT_MS)
  }

  /**
   * Execute a shell command with a custom timeout.
   */
  override fun executeCommandWithTimeout(command: String, timeoutMs: Long): String {
    return try {
      val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))

      val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
      if (!completed) {
        process.destroyForcibly()
        return CommandResult.failure("Command timed out after ${timeoutMs}ms").toJson()
      }

      val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
      val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
      val exitCode = process.exitValue()

      CommandResult(
        exitCode = exitCode,
        stdout = stdout,
        stderr = stderr
      ).toJson()
    } catch (e: Exception) {
      CommandResult.failure("Execution failed: ${e.message}").toJson()
    }
  }

  /**
   * Get the UID this service is running as.
   * Returns 0 for root mode, 2000 for shell (ADB) mode.
   */
  override fun getUid(): Int {
    return Process.myUid()
  }

  /**
   * Check if this service is running with root privileges.
   */
  override fun isRoot(): Boolean {
    return Process.myUid() == 0
  }

  /**
   * Destroy this service instance.
   * Called when the service should clean up resources.
   */
  override fun destroy() {
    // Nothing to clean up
  }
}
