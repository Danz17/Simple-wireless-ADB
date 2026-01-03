package com.phenix.wirelessadb.shell

import org.json.JSONObject

/**
 * Result of executing a shell command via Shizuku UserService.
 * Uses JSON serialization for AIDL transport.
 */
data class CommandResult(
  val exitCode: Int,
  val stdout: String,
  val stderr: String,
  val success: Boolean = exitCode == 0
) {

  /**
   * Serialize to JSON string for AIDL transport.
   */
  fun toJson(): String {
    return JSONObject().apply {
      put("exitCode", exitCode)
      put("stdout", stdout)
      put("stderr", stderr)
      put("success", success)
    }.toString()
  }

  companion object {
    /**
     * Deserialize from JSON string received via AIDL.
     */
    fun fromJson(json: String): CommandResult {
      val obj = JSONObject(json)
      return CommandResult(
        exitCode = obj.getInt("exitCode"),
        stdout = obj.optString("stdout", ""),
        stderr = obj.optString("stderr", ""),
        success = obj.optBoolean("success", false)
      )
    }

    /**
     * Create a failure result for exceptions.
     */
    fun failure(message: String): CommandResult {
      return CommandResult(
        exitCode = -1,
        stdout = "",
        stderr = message,
        success = false
      )
    }
  }
}
