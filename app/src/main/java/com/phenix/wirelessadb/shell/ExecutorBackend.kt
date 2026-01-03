package com.phenix.wirelessadb.shell

/**
 * Available backends for executing privileged shell commands.
 */
enum class ExecutorBackend {
  /**
   * Execute commands via root (su).
   * Most reliable, full access to all operations.
   * UID: 0
   */
  ROOT,

  /**
   * Execute commands via Shizuku running as root.
   * Shizuku daemon started with root, full access.
   * UID: 0
   */
  SHIZUKU_ROOT,

  /**
   * Execute commands via Shizuku running as shell.
   * Shizuku daemon started via ADB.
   * Limited: Cannot hide developer notifications (needs WRITE_SECURE_SETTINGS).
   * UID: 2000
   */
  SHIZUKU_SHELL,

  /**
   * No privileged backend available.
   */
  NONE;

  /**
   * Check if this backend has root-level access.
   */
  fun isRoot(): Boolean = this == ROOT || this == SHIZUKU_ROOT

  /**
   * Check if this backend uses Shizuku.
   */
  fun isShizuku(): Boolean = this == SHIZUKU_ROOT || this == SHIZUKU_SHELL

  /**
   * Check if this backend can hide developer notifications.
   * Only ROOT and SHIZUKU_ROOT can do this.
   */
  fun canHideDevNotification(): Boolean = isRoot()
}
