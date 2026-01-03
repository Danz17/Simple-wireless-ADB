package com.phenix.wirelessadb.relay

import android.content.Context
import android.util.Log
import com.phenix.wirelessadb.model.TrustedDevice
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import java.net.InetSocketAddress

/**
 * ADB Relay Server that bridges remote Tailscale connections to local ADB.
 * Only accepts connections from Tailscale network (100.x.x.x).
 * First-time connections require approval, then remembered.
 */
class AdbRelayServer(
  private val context: Context,
  private val relayPort: Int = DEFAULT_RELAY_PORT,
  private val adbPort: Int = DEFAULT_ADB_PORT,
  private val onPendingAuth: ((String) -> Unit)? = null,
  private val onConnectionEstablished: ((String) -> Unit)? = null,
  private val onConnectionClosed: ((String) -> Unit)? = null
) {

  private val authManager = DeviceAuthManager(context)
  private var serverJob: Job? = null
  private var serverSocket: ServerSocket? = null
  private val pendingConnections = mutableMapOf<String, Socket>()
  private val activeConnections = mutableSetOf<String>()

  val isRunning: Boolean
    get() = serverJob?.isActive == true

  val trustedDeviceCount: Int
    get() = authManager.getTrustedDeviceCount()

  val pendingApprovalIp: String?
    get() = pendingConnections.keys.firstOrNull()

  /**
   * Start the relay server.
   */
  suspend fun start() = withContext(Dispatchers.IO) {
    if (isRunning) {
      Log.w(TAG, "Relay server already running")
      return@withContext
    }

    try {
      val selectorManager = SelectorManager(Dispatchers.IO)
      serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", relayPort)
      Log.i(TAG, "Relay server started on port $relayPort")

      serverJob = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
          try {
            val clientSocket = serverSocket?.accept() ?: break
            launch { handleConnection(clientSocket) }
          } catch (e: CancellationException) {
            break
          } catch (e: Exception) {
            Log.e(TAG, "Error accepting connection", e)
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start relay server", e)
      throw e
    }
  }

  /**
   * Handle an incoming connection.
   */
  private suspend fun handleConnection(client: Socket) {
    val remoteAddress = client.remoteAddress
    val clientIp = remoteAddress.toString()
      .substringAfter("/")
      .substringBefore(":")
      .ifEmpty { "unknown" }

    Log.i(TAG, "Connection from: $clientIp")

    // Only accept Tailscale connections
    if (!TailscaleHelper.isFromTailscaleNetwork(clientIp)) {
      Log.w(TAG, "Rejected non-Tailscale connection from: $clientIp")
      client.close()
      return
    }

    // Check if trusted
    if (authManager.isDeviceTrusted(clientIp)) {
      // Trusted device - auto-connect
      Log.i(TAG, "Trusted device connected: $clientIp")
      authManager.updateLastSeen(clientIp)
      activeConnections.add(clientIp)
      onConnectionEstablished?.invoke(clientIp)
      try {
        bridgeToAdb(client, clientIp)
      } finally {
        activeConnections.remove(clientIp)
        onConnectionClosed?.invoke(clientIp)
      }
    } else {
      // New device - require approval
      Log.i(TAG, "New device requesting approval: $clientIp")
      pendingConnections[clientIp] = client
      onPendingAuth?.invoke(clientIp)

      // Wait for approval (60 second timeout)
      val approved = waitForApproval(clientIp)

      pendingConnections.remove(clientIp)

      if (approved) {
        Log.i(TAG, "Device approved: $clientIp")
        activeConnections.add(clientIp)
        onConnectionEstablished?.invoke(clientIp)
        try {
          bridgeToAdb(client, clientIp)
        } finally {
          activeConnections.remove(clientIp)
          onConnectionClosed?.invoke(clientIp)
        }
      } else {
        Log.w(TAG, "Device not approved, closing: $clientIp")
        client.close()
      }
    }
  }

  /**
   * Wait for device approval with timeout.
   */
  private suspend fun waitForApproval(clientIp: String): Boolean {
    return withTimeoutOrNull(APPROVAL_TIMEOUT_MS) {
      while (!authManager.isDeviceTrusted(clientIp)) {
        delay(500)
      }
      true
    } ?: false
  }

  /**
   * Bridge the client socket to local ADB.
   */
  private suspend fun bridgeToAdb(client: Socket, clientIp: String) {
    try {
      val selectorManager = SelectorManager(Dispatchers.IO)
      val adbSocket = aSocket(selectorManager).tcp().connect("127.0.0.1", adbPort)
      Log.i(TAG, "Bridging $clientIp to ADB on port $adbPort")
      ConnectionProxy(client, adbSocket).start()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to bridge to ADB for $clientIp", e)
      client.close()
    }
  }

  /**
   * Approve a pending device.
   */
  fun approveDevice(clientIp: String, name: String? = null) {
    authManager.addTrustedDevice(clientIp, name)
  }

  /**
   * Deny a pending device.
   */
  fun denyDevice(clientIp: String) {
    pendingConnections[clientIp]?.close()
    pendingConnections.remove(clientIp)
  }

  /**
   * Remove a trusted device.
   */
  fun removeTrustedDevice(clientIp: String) {
    authManager.removeTrustedDevice(clientIp)
  }

  /**
   * Get all trusted devices.
   */
  fun getTrustedDevices(): List<TrustedDevice> {
    return authManager.getTrustedDevices()
  }

  /**
   * Stop the relay server.
   */
  fun stop() {
    serverJob?.cancel()
    serverJob = null
    serverSocket?.close()
    serverSocket = null
    pendingConnections.values.forEach { it.close() }
    pendingConnections.clear()
    activeConnections.clear()
    Log.i(TAG, "Relay server stopped")
  }

  companion object {
    private const val TAG = "AdbRelayServer"
    const val DEFAULT_RELAY_PORT = 5556
    const val DEFAULT_ADB_PORT = 5555
    private const val APPROVAL_TIMEOUT_MS = 60_000L
  }
}
