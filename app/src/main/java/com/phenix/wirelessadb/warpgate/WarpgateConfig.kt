package com.phenix.wirelessadb.warpgate

/**
 * Configuration for Warpgate bastion host connection.
 *
 * Warpgate is a transparent bastion host that supports SSH tunneling.
 * We use it to create a secure tunnel for remote ADB access.
 */
data class WarpgateConfig(
  /**
   * Whether Warpgate connection is enabled.
   */
  val enabled: Boolean = false,

  /**
   * Warpgate server hostname or IP.
   */
  val host: String = "",

  /**
   * Warpgate SSH port (default 8443 for Warpgate).
   */
  val port: Int = DEFAULT_PORT,

  /**
   * Username for Warpgate authentication.
   * Format: "username#target" where target is the Warpgate target name.
   */
  val username: String = "",

  /**
   * Password for authentication (if using password auth).
   */
  val password: String = "",

  /**
   * Warpgate target name that points to this device's ADB.
   */
  val targetName: String = "adb",

  /**
   * Local port for the SSH tunnel.
   * Users will connect via: adb connect localhost:localPort
   */
  val localPort: Int = DEFAULT_LOCAL_PORT
) {
  companion object {
    const val DEFAULT_PORT = 8443
    const val DEFAULT_LOCAL_PORT = 5557
  }

  /**
   * Get the full Warpgate username with target.
   * Format: "username#target"
   */
  fun getFullUsername(): String {
    return if (targetName.isNotEmpty()) {
      "$username#$targetName"
    } else {
      username
    }
  }

  /**
   * Get the ADB connect command for Warpgate tunnel.
   */
  fun getAdbCommand(): String {
    return "adb connect localhost:$localPort"
  }

  /**
   * Check if configuration is valid for connection.
   */
  fun isValid(): Boolean {
    return host.isNotBlank() &&
      port in 1..65535 &&
      username.isNotBlank() &&
      localPort in 1024..65535
  }
}
