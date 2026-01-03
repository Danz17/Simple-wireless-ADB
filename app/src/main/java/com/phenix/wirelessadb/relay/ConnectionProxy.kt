package com.phenix.wirelessadb.relay

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bidirectional proxy between remote client socket and local ADB socket.
 * Copies data in both directions until either side closes.
 */
class ConnectionProxy(
  private val clientSocket: Socket,
  private val adbSocket: Socket
) {

  /**
   * Start proxying data between client and ADB.
   * Blocks until one side closes the connection.
   */
  suspend fun start() = coroutineScope {
    try {
      val clientRead = clientSocket.openReadChannel()
      val clientWrite = clientSocket.openWriteChannel(autoFlush = true)
      val adbRead = adbSocket.openReadChannel()
      val adbWrite = adbSocket.openWriteChannel(autoFlush = true)

      // Copy data bidirectionally
      val clientToAdb = launch { copyChannel(clientRead, adbWrite, "client->adb") }
      val adbToClient = launch { copyChannel(adbRead, clientWrite, "adb->client") }

      // Wait for either direction to close, then cancel the other
      select {
        clientToAdb.onJoin { adbToClient.cancel() }
        adbToClient.onJoin { clientToAdb.cancel() }
      }
    } finally {
      closeQuietly()
    }
  }

  /**
   * Copy data from one channel to another.
   */
  private suspend fun copyChannel(
    from: ByteReadChannel,
    to: ByteWriteChannel,
    tag: String
  ) {
    val buffer = ByteArray(BUFFER_SIZE)
    try {
      while (!from.isClosedForRead) {
        val bytesRead = from.readAvailable(buffer)
        if (bytesRead == -1) break
        if (bytesRead > 0) {
          to.writeFully(buffer, 0, bytesRead)
        }
      }
    } catch (e: CancellationException) {
      // Normal cancellation
      throw e
    } catch (e: Exception) {
      // Connection closed or error
    }
  }

  /**
   * Close both sockets quietly, ignoring errors.
   */
  private fun closeQuietly() {
    try { clientSocket.close() } catch (_: Exception) {}
    try { adbSocket.close() } catch (_: Exception) {}
  }

  companion object {
    private const val BUFFER_SIZE = 8192
  }
}
