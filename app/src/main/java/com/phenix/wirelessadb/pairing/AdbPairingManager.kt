package com.phenix.wirelessadb.pairing

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level manager for ADB pairing operations.
 *
 * Provides a simple API for pairing with the device's wireless debugging service.
 * Only available on Android 11+ (API 30+).
 *
 * Usage:
 * 1. User enables Wireless Debugging in Developer Options
 * 2. User taps "Pair device with pairing code"
 * 3. User enters the port and 6-digit code shown
 * 4. Call pair(port, code) to establish pairing
 */
object AdbPairingManager {

  private const val TAG = "AdbPairingManager"
  private const val DEFAULT_HOST = "127.0.0.1"

  /**
   * Pairing state.
   */
  enum class PairingState {
    IDLE,
    PAIRING,
    PAIRED,
    FAILED
  }

  /**
   * Pairing result data.
   */
  data class PairingResult(
    val success: Boolean,
    val message: String,
    val pairingPort: Int? = null
  )

  private val _state = MutableLiveData(PairingState.IDLE)
  val state: LiveData<PairingState> = _state

  private val _lastResult = MutableLiveData<PairingResult?>(null)
  val lastResult: LiveData<PairingResult?> = _lastResult

  /**
   * Check if ADB pairing is supported on this device.
   * Requires Android 11 (API 30) or higher.
   */
  fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  /**
   * Get minimum API level required for pairing.
   */
  fun getMinApiLevel(): Int = Build.VERSION_CODES.R

  /**
   * Get human-readable reason why pairing is not supported.
   */
  fun getUnsupportedReason(): String? {
    return if (!isSupported()) {
      "ADB pairing requires Android 11 or higher (API ${Build.VERSION_CODES.R}). " +
        "Your device is running API ${Build.VERSION.SDK_INT}."
    } else {
      null
    }
  }

  /**
   * Pair with the device's wireless debugging service.
   *
   * @param port Pairing port shown in Developer Options
   * @param pairingCode 6-digit pairing code
   * @param host Optional host (default: localhost)
   * @return Result indicating success or failure
   */
  @RequiresApi(Build.VERSION_CODES.R)
  suspend fun pair(
    port: Int,
    pairingCode: String,
    host: String = DEFAULT_HOST
  ): PairingResult = withContext(Dispatchers.IO) {
    if (!isSupported()) {
      val result = PairingResult(
        success = false,
        message = getUnsupportedReason() ?: "Pairing not supported"
      )
      _lastResult.postValue(result)
      return@withContext result
    }

    // Validate inputs
    if (port < 1 || port > 65535) {
      val result = PairingResult(
        success = false,
        message = "Invalid port: $port"
      )
      _lastResult.postValue(result)
      return@withContext result
    }

    if (pairingCode.length != 6 || !pairingCode.all { it.isDigit() }) {
      val result = PairingResult(
        success = false,
        message = "Pairing code must be 6 digits"
      )
      _lastResult.postValue(result)
      return@withContext result
    }

    _state.postValue(PairingState.PAIRING)
    Log.i(TAG, "Starting ADB pairing to $host:$port")

    try {
      val client = AdbPairingClient(host, port, pairingCode)
      val pairingResult = client.pair()

      if (pairingResult.isSuccess) {
        _state.postValue(PairingState.PAIRED)
        val result = PairingResult(
          success = true,
          message = "Pairing successful! Wireless debugging is now enabled.",
          pairingPort = port
        )
        _lastResult.postValue(result)
        Log.i(TAG, "Pairing completed successfully")
        result
      } else {
        _state.postValue(PairingState.FAILED)
        val error = pairingResult.exceptionOrNull()?.message ?: "Unknown error"
        val result = PairingResult(
          success = false,
          message = "Pairing failed: $error"
        )
        _lastResult.postValue(result)
        Log.e(TAG, "Pairing failed: $error")
        result
      }
    } catch (e: Exception) {
      _state.postValue(PairingState.FAILED)
      val result = PairingResult(
        success = false,
        message = "Pairing error: ${e.message}"
      )
      _lastResult.postValue(result)
      Log.e(TAG, "Pairing exception", e)
      result
    }
  }

  /**
   * Reset the pairing state to idle.
   */
  fun reset() {
    _state.value = PairingState.IDLE
    _lastResult.value = null
  }

  /**
   * Check if currently pairing.
   */
  fun isPairing(): Boolean = _state.value == PairingState.PAIRING

  /**
   * Check if last pairing was successful.
   */
  fun isPaired(): Boolean = _state.value == PairingState.PAIRED

  /**
   * Get instructions for the user.
   */
  fun getInstructions(): List<String> = listOf(
    "1. Go to Settings → Developer Options",
    "2. Enable 'Wireless debugging'",
    "3. Tap 'Pair device with pairing code'",
    "4. Enter the port and 6-digit code shown",
    "5. Tap 'Pair' to complete the process"
  )
}
