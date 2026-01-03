package com.phenix.wirelessadb.model

/**
 * P2P Token for device-to-device ADB connections (v1.2.0).
 *
 * Token format: "ABC-123-XYZ" (readable, easy to share verbally)
 * Token is masked by default when displayed: "***-***-XYZ"
 *
 * Lifecycle:
 * 1. Device A generates token → shares with Device B
 * 2. Device B connects using token
 * 3. If "Mark as trusted": token becomes persistent, device added to trust list
 * 4. If not trusted: token invalidated on disconnect
 */
data class P2PToken(
  /** Token code in format "ABC-123-XYZ" */
  val code: String,

  /** Hash of the generating device (for verification) */
  val deviceHash: String,

  /** Unique session identifier */
  val sessionId: String,

  /** Expiration timestamp (milliseconds since epoch) */
  val expiresAt: Long,

  /** Primary STUN server for NAT traversal */
  val stunServer: String = DEFAULT_STUN_SERVER,

  /** Optional TURN server for relay fallback */
  val turnServer: String? = null,

  /** Whether this token persists across connections */
  val isPersistent: Boolean = false,

  /** Creation timestamp */
  val createdAt: Long = System.currentTimeMillis(),

  /** External IP:port discovered via STUN (set after NAT traversal) */
  val externalEndpoint: String? = null,

  /** Connection state */
  val state: TokenState = TokenState.PENDING
) {
  /**
   * Get masked token for safe display: "***-***-XYZ"
   */
  fun masked(): String {
    val parts = code.split("-")
    return if (parts.size == 3) {
      "***-***-${parts[2]}"
    } else {
      "***-***-${code.takeLast(3)}"
    }
  }

  /**
   * Check if token has expired.
   */
  fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

  /**
   * Check if token is still valid (not expired, not revoked).
   */
  fun isValid(): Boolean = !isExpired() && state != TokenState.REVOKED

  /**
   * Get remaining time in human-readable format.
   */
  fun getRemainingTime(): String {
    val remaining = expiresAt - System.currentTimeMillis()
    if (remaining <= 0) return "Expired"
    val minutes = remaining / 60000
    val seconds = (remaining % 60000) / 1000
    return if (minutes > 0) {
      "${minutes}m ${seconds}s"
    } else {
      "${seconds}s"
    }
  }

  /**
   * Get shareable connection string for the connecting device.
   */
  fun getShareableString(): String {
    return "p2p:$code"
  }

  companion object {
    /** Default expiration time: 30 minutes */
    const val DEFAULT_EXPIRY_MS = 30 * 60 * 1000L

    /** Extended expiration for trusted devices: 24 hours */
    const val TRUSTED_EXPIRY_MS = 24 * 60 * 60 * 1000L

    /** Google's public STUN server */
    const val DEFAULT_STUN_SERVER = "stun.l.google.com:19302"

    /** Cloudflare's public STUN server (fallback) */
    const val FALLBACK_STUN_SERVER = "stun.cloudflare.com:3478"
  }
}

/**
 * Token lifecycle states.
 */
enum class TokenState {
  /** Token created, waiting for connection */
  PENDING,

  /** NAT traversal in progress */
  CONNECTING,

  /** Connection established */
  CONNECTED,

  /** Token manually revoked */
  REVOKED,

  /** Token expired */
  EXPIRED,

  /** Connection failed */
  FAILED
}
