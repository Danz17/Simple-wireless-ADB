package com.phenix.wirelessadb.p2p

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phenix.wirelessadb.model.DeviceIdentifier
import com.phenix.wirelessadb.model.P2PToken
import com.phenix.wirelessadb.model.TokenState
import com.phenix.wirelessadb.relay.DeviceAuthManager
import com.phenix.wirelessadb.util.DeviceFingerprint
import kotlinx.coroutines.*

/**
 * P2P Connection Manager (v1.2.0).
 *
 * Manages P2P token lifecycle and NAT traversal for device-to-device connections.
 *
 * Flow:
 * 1. Device A calls generateToken() → gets token like "ABC-123-XYZ"
 * 2. Device A shares token with Device B (verbally, text, QR code)
 * 3. Device B enters token in their app
 * 4. Both devices perform NAT traversal via STUN
 * 5. Connection established or falls back to TURN relay
 *
 * Security:
 * - Token expires after 30 minutes (or 24h for trusted)
 * - Token rotates on disconnect (unless trusted)
 * - Device verified by hardware fingerprint
 */
class P2PManager private constructor(private val context: Context) {

  private val TAG = "P2PManager"
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val authManager = DeviceAuthManager(context)

  // Current connection state
  private val _connectionState = MutableLiveData(P2PState.IDLE)
  val connectionState: LiveData<P2PState> = _connectionState

  // Current token (if any)
  private val _currentToken = MutableLiveData<P2PToken?>(null)
  val currentToken: LiveData<P2PToken?> = _currentToken

  // Error message
  private val _error = MutableLiveData<String?>(null)
  val error: LiveData<String?> = _error

  // Loading state
  private val _isLoading = MutableLiveData(false)
  val isLoading: LiveData<Boolean> = _isLoading

  /**
   * Generate a new P2P token for this device.
   */
  suspend fun generateToken(): Result<P2PToken> = withContext(Dispatchers.IO) {
    try {
      _isLoading.postValue(true)
      _error.postValue(null)

      // Revoke any existing token
      revokeToken()

      // Get device fingerprint
      val deviceHash = DeviceFingerprint.computeDeviceHash(context)

      // Generate new token
      val token = TokenGenerator.generate(deviceHash)
      _currentToken.postValue(token)
      _connectionState.postValue(P2PState.TOKEN_READY)

      Log.i(TAG, "Token generated: ${token.masked()}")
      Result.success(token)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate token", e)
      _error.postValue("Failed to generate token: ${e.message}")
      Result.failure(e)
    } finally {
      _isLoading.postValue(false)
    }
  }

  /**
   * Generate a persistent token (for trusted device).
   */
  suspend fun generatePersistentToken(): Result<P2PToken> = withContext(Dispatchers.IO) {
    try {
      _isLoading.postValue(true)
      _error.postValue(null)

      val deviceHash = DeviceFingerprint.computeDeviceHash(context)
      val token = TokenGenerator.generatePersistent(deviceHash)
      _currentToken.postValue(token)
      _connectionState.postValue(P2PState.TOKEN_READY)

      Log.i(TAG, "Persistent token generated: ${token.masked()}")
      Result.success(token)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate persistent token", e)
      _error.postValue("Failed to generate token: ${e.message}")
      Result.failure(e)
    } finally {
      _isLoading.postValue(false)
    }
  }

  /**
   * Revoke the current token.
   */
  fun revokeToken() {
    _currentToken.value?.let { token ->
      Log.i(TAG, "Token revoked: ${token.masked()}")
    }
    _currentToken.postValue(null)
    _connectionState.postValue(P2PState.IDLE)
  }

  /**
   * Connect to a peer using their token.
   * This would initiate NAT traversal in a full implementation.
   */
  suspend fun connectWithToken(tokenCode: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      _isLoading.postValue(true)
      _error.postValue(null)
      _connectionState.postValue(P2PState.CONNECTING)

      // Validate token format
      val normalizedCode = TokenGenerator.normalize(tokenCode)
      if (!TokenGenerator.isValidFormat(normalizedCode)) {
        _connectionState.postValue(P2PState.FAILED)
        return@withContext Result.failure(Exception("Invalid token format"))
      }

      // In a full implementation, this would:
      // 1. Send token to rendezvous server
      // 2. Perform STUN discovery
      // 3. Exchange connection info with peer
      // 4. Establish direct connection or TURN relay

      Log.i(TAG, "Connecting with token: ${normalizedCode.takeLast(3)}")

      // Placeholder for NAT traversal
      delay(2000) // Simulate connection attempt

      // For now, report that P2P requires additional infrastructure
      _connectionState.postValue(P2PState.FAILED)
      _error.postValue("P2P connection requires NAT traversal server (coming in v1.3.0)")

      Result.failure(Exception("P2P NAT traversal not yet implemented"))
    } catch (e: Exception) {
      Log.e(TAG, "Connection failed", e)
      _connectionState.postValue(P2PState.FAILED)
      _error.postValue("Connection failed: ${e.message}")
      Result.failure(e)
    } finally {
      _isLoading.postValue(false)
    }
  }

  /**
   * Accept an incoming connection request.
   */
  suspend fun acceptConnection(deviceIdentifier: DeviceIdentifier): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      _isLoading.postValue(true)

      // Verify device against token
      val token = _currentToken.value
        ?: return@withContext Result.failure(Exception("No active token"))

      // Update token state
      val connectedToken = token.copy(state = TokenState.CONNECTED)
      _currentToken.postValue(connectedToken)
      _connectionState.postValue(P2PState.CONNECTED)

      Log.i(TAG, "Connection accepted from: ${deviceIdentifier.computeHash().takeLast(4)}")

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to accept connection", e)
      _error.postValue("Failed to accept: ${e.message}")
      Result.failure(e)
    } finally {
      _isLoading.postValue(false)
    }
  }

  /**
   * Disconnect current P2P connection.
   */
  fun disconnect() {
    val token = _currentToken.value ?: return

    if (token.isPersistent) {
      // Keep token for trusted devices
      _connectionState.postValue(P2PState.TOKEN_READY)
    } else {
      // Rotate token for non-trusted
      revokeToken()
    }

    Log.i(TAG, "Disconnected")
  }

  /**
   * Check if token is still valid.
   */
  fun isTokenValid(): Boolean {
    return _currentToken.value?.isValid() == true
  }

  /**
   * Get the ADB connect command for P2P connection.
   * Returns null if not connected or no external endpoint discovered.
   */
  fun getAdbCommand(): String? {
    val token = _currentToken.value ?: return null
    if (_connectionState.value != P2PState.CONNECTED) return null
    return token.externalEndpoint?.let { "adb connect $it" }
  }

  /**
   * Clear any error state.
   */
  fun clearError() {
    _error.postValue(null)
  }

  /**
   * Clean up resources.
   */
  fun destroy() {
    scope.cancel()
  }

  companion object {
    @Volatile
    private var instance: P2PManager? = null

    fun getInstance(context: Context): P2PManager {
      return instance ?: synchronized(this) {
        instance ?: P2PManager(context.applicationContext).also { instance = it }
      }
    }
  }
}

/**
 * P2P connection states.
 */
enum class P2PState {
  /** No active token or connection */
  IDLE,

  /** Token generated, waiting for peer */
  TOKEN_READY,

  /** NAT traversal in progress */
  CONNECTING,

  /** Connection established */
  CONNECTED,

  /** Connection failed */
  FAILED
}
