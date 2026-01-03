package com.phenix.wirelessadb.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for the main tab navigation.
 *
 * TAB STRUCTURE (v1.2.0):
 * =======================
 * Tab 0: DashboardFragment   - Unified control center (Local ADB + Settings)
 * Tab 1: RemoteRelayFragment - REMOTE connections (Tailscale/Warpgate/P2P)
 * Tab 2: HelpFragment        - Help docs, downloads, theme settings
 *
 * v1.2.0 Changes:
 * - Replaced LocalAdbFragment with DashboardFragment
 * - Dashboard merges local ADB + boot/notification settings + trusted devices
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

  override fun getItemCount(): Int = TAB_COUNT

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      TAB_DASHBOARD -> DashboardFragment()       // Tab 0: Dashboard control center
      TAB_REMOTE_RELAY -> RemoteRelayFragment()  // Tab 1: Remote connections
      TAB_HELP -> HelpFragment()                 // Tab 2: Help & downloads
      else -> throw IllegalArgumentException("Invalid tab position: $position")
    }
  }

  companion object {
    const val TAB_COUNT = 3

    // Tab indices (v1.2.0)
    const val TAB_DASHBOARD = 0     // DashboardFragment: Unified control center
    const val TAB_REMOTE_RELAY = 1  // RemoteRelayFragment: Remote connections
    const val TAB_HELP = 2          // HelpFragment: Help & downloads
  }
}
