package com.phenix.wirelessadb.p2p

import com.phenix.wirelessadb.model.P2PToken
import java.security.SecureRandom
import java.util.UUID

/**
 * Generates and validates P2P connection tokens (v1.2.0).
 *
 * Token format: "ABC-123-XYZ" (9 characters with dashes)
 * - Characters: A-Z, 0-9 (excluding confusing chars like O/0, I/1, L)
 * - Designed to be easy to read aloud and type
 */
object TokenGenerator {

  // Charset excluding confusing characters (O, 0, I, 1, L)
  private const val TOKEN_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
  private const val SEGMENT_LENGTH = 3
  private const val SEGMENT_COUNT = 3
  private val random = SecureRandom()

  /**
   * Generate a new P2P token.
   *
   * @param deviceHash Hash of the generating device
   * @param isPersistent Whether token should persist across connections
   * @return New P2P token
   */
  fun generate(
    deviceHash: String,
    isPersistent: Boolean = false
  ): P2PToken {
    val code = generateCode()
    val sessionId = UUID.randomUUID().toString()
    val expiresAt = System.currentTimeMillis() +
      if (isPersistent) P2PToken.TRUSTED_EXPIRY_MS else P2PToken.DEFAULT_EXPIRY_MS

    return P2PToken(
      code = code,
      deviceHash = deviceHash,
      sessionId = sessionId,
      expiresAt = expiresAt,
      isPersistent = isPersistent
    )
  }

  /**
   * Generate a token code in format "ABC-123-XYZ".
   */
  private fun generateCode(): String {
    return (1..SEGMENT_COUNT)
      .map { generateSegment() }
      .joinToString("-")
  }

  /**
   * Generate a single segment (e.g., "ABC").
   */
  private fun generateSegment(): String {
    return (1..SEGMENT_LENGTH)
      .map { TOKEN_CHARS[random.nextInt(TOKEN_CHARS.length)] }
      .joinToString("")
  }

  /**
   * Validate token format.
   */
  fun isValidFormat(code: String): Boolean {
    val pattern = Regex("^[A-Z2-9]{3}-[A-Z2-9]{3}-[A-Z2-9]{3}$")
    return pattern.matches(code.uppercase())
  }

  /**
   * Normalize token input (uppercase, add dashes if needed).
   */
  fun normalize(input: String): String {
    val cleaned = input.uppercase().filter { it.isLetterOrDigit() }
    return if (cleaned.length == 9) {
      "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6, 9)}"
    } else {
      input.uppercase()
    }
  }

  /**
   * Generate a persistent token for a trusted device.
   */
  fun generatePersistent(deviceHash: String): P2PToken {
    return generate(deviceHash, isPersistent = true)
  }

  /**
   * Regenerate a token (new code, same settings).
   */
  fun regenerate(oldToken: P2PToken): P2PToken {
    return generate(
      deviceHash = oldToken.deviceHash,
      isPersistent = oldToken.isPersistent
    )
  }
}
