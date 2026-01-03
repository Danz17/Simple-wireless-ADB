package com.phenix.wirelessadb

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {

  private const val PREFS_NAME = "wireless_adb_prefs"
  private const val KEY_ENABLE_ON_BOOT = "enable_on_boot"
  private const val KEY_PORT = "port"
  private const val KEY_RELAY_ENABLED = "relay_enabled"
  private const val KEY_RELAY_PORT = "relay_port"
  private const val DEFAULT_PORT = 5555
  private const val DEFAULT_RELAY_PORT = 5556

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isEnableOnBoot(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_ENABLE_ON_BOOT, false)
  }

  fun setEnableOnBoot(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_ENABLE_ON_BOOT, enabled).apply()
  }

  fun getPort(context: Context): Int {
    return getPrefs(context).getInt(KEY_PORT, DEFAULT_PORT)
  }

  fun setPort(context: Context, port: Int) {
    getPrefs(context).edit().putInt(KEY_PORT, port).apply()
  }

  fun isRelayEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_RELAY_ENABLED, false)
  }

  fun setRelayEnabled(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_RELAY_ENABLED, enabled).apply()
  }

  fun getRelayPort(context: Context): Int {
    return getPrefs(context).getInt(KEY_RELAY_PORT, DEFAULT_RELAY_PORT)
  }

  fun setRelayPort(context: Context, port: Int) {
    getPrefs(context).edit().putInt(KEY_RELAY_PORT, port).apply()
  }
}
