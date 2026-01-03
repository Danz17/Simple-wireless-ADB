package com.phenix.wirelessadb.model

/**
 * Represents the state of toolbar status indicators (v1.2.0).
 * Each indicator shows the connection status of a specific service.
 */
data class StatusIndicators(
  val localAdb: IndicatorState,
  val tailscale: IndicatorState,
  val warpgate: IndicatorState,
  /** P2P Token connection status (v1.2.0) */
  val p2p: IndicatorState = IndicatorState.INACTIVE
) {
  companion object {
    /**
     * Default state - all indicators inactive.
     */
    val DEFAULT = StatusIndicators(
      localAdb = IndicatorState.INACTIVE,
      tailscale = IndicatorState.INACTIVE,
      warpgate = IndicatorState.INACTIVE,
      p2p = IndicatorState.INACTIVE
    )
  }
}

/**
 * State of a single status indicator.
 */
enum class IndicatorState {
  /**
   * Service is active and connected (green).
   */
  ACTIVE,

  /**
   * Service is inactive or disconnected (gray/red).
   */
  INACTIVE,

  /**
   * Service has a warning condition (yellow).
   * Example: ADB enabled but no IP available.
   */
  WARNING
}
