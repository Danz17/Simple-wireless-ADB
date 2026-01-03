// IShellService.aidl
package com.phenix.wirelessadb.shell;

/**
 * AIDL interface for Shizuku UserService.
 * This service runs in Shizuku's privileged process (UID 2000 for shell, UID 0 for root).
 */
interface IShellService {

  /**
   * Execute a shell command with elevated privileges.
   * @param command The shell command to execute
   * @return JSON-encoded result with exitCode, stdout, and stderr
   */
  String executeCommand(String command);

  /**
   * Execute a shell command with a custom timeout.
   * @param command The shell command to execute
   * @param timeoutMs Timeout in milliseconds
   * @return JSON-encoded result with exitCode, stdout, and stderr
   */
  String executeCommandWithTimeout(String command, long timeoutMs);

  /**
   * Get the UID this service is running as.
   * Returns 0 for root, 2000 for shell.
   */
  int getUid();

  /**
   * Check if this service is running with root privileges.
   */
  boolean isRoot();

  /**
   * Destroy this service instance.
   */
  void destroy();
}
