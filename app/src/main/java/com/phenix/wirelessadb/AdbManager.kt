package com.phenix.wirelessadb

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object AdbManager {

  private const val DEFAULT_PORT = 5555
  private const val MIN_PORT = 1024
  private const val MAX_PORT = 65535

  data class AdbStatus(
    val enabled: Boolean,
    val port: Int,
    val ip: String?
  )

  fun validatePort(port: Int): Boolean = port in MIN_PORT..MAX_PORT

  suspend fun getStatus(context: Context): AdbStatus = withContext(Dispatchers.IO) {
    val port = getCurrentPort()
    val enabled = port > 0
    val ip = if (enabled) getWifiIp(context) else null
    AdbStatus(enabled, if (enabled) port else DEFAULT_PORT, ip)
  }

  suspend fun enable(port: Int = DEFAULT_PORT): Result<Unit> = withContext(Dispatchers.IO) {
    if (!validatePort(port)) {
      return@withContext Result.failure(IllegalArgumentException("Port must be $MIN_PORT-$MAX_PORT"))
    }
    runRootCommand(
      "setprop service.adb.tcp.port $port",
      "stop adbd",
      "start adbd"
    )
  }

  suspend fun disable(): Result<Unit> = withContext(Dispatchers.IO) {
    runRootCommand(
      "setprop service.adb.tcp.port -1",
      "stop adbd",
      "start adbd"
    )
  }

  suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
    try {
      val process = Runtime.getRuntime().exec("su -c id")
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      val output = reader.readText()
      process.waitFor()
      output.contains("uid=0")
    } catch (e: Exception) {
      false
    }
  }

  private fun getCurrentPort(): Int {
    return try {
      val process = Runtime.getRuntime().exec("getprop service.adb.tcp.port")
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      val output = reader.readLine()?.trim() ?: ""
      process.waitFor()
      output.toIntOrNull() ?: -1
    } catch (e: Exception) {
      -1
    }
  }

  private fun getWifiIp(context: Context): String? {
    val wifiManager = context.applicationContext
      .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val ip = wifiManager.connectionInfo.ipAddress
    if (ip == 0) return null
    return String.format(
      "%d.%d.%d.%d",
      ip and 0xff,
      ip shr 8 and 0xff,
      ip shr 16 and 0xff,
      ip shr 24 and 0xff
    )
  }

  private fun runRootCommand(vararg commands: String): Result<Unit> {
    return try {
      val process = Runtime.getRuntime().exec("su")
      DataOutputStream(process.outputStream).use { os ->
        commands.forEach { cmd ->
          os.writeBytes("$cmd\n")
        }
        os.writeBytes("exit\n")
        os.flush()
      }
      val exitCode = process.waitFor()
      if (exitCode == 0) {
        Result.success(Unit)
      } else {
        Result.failure(RuntimeException("Root command failed with exit code $exitCode"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
