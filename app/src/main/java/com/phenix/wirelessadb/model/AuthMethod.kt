package com.phenix.wirelessadb.model

/**
 * Authentication methods for trusted devices (v1.2.0).
 */
enum class AuthMethod {
  /**
   * Traditional IP-based authentication.
   * Device is trusted by its IP address only.
   * Less secure - IP can change or be spoofed.
   */
  IP_ADDRESS,

  /**
   * One-time token authentication.
   * Token is valid for single connection only.
   * Token rotates after disconnect.
   */
  TOKEN_ONCE,

  /**
   * Persistent token authentication.
   * Token remains valid across connections.
   * Device is marked as trusted with hardware ID.
   */
  TOKEN_PERSISTENT
}
