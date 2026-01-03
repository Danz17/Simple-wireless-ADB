package com.phenix.wirelessadb.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for the main tab navigation.
 *
 * TAB STRUCTURE:
 * ==============
 * Tab 0: LocalAdbFragment    - LOCAL WiFi ADB (IP: 10.x.x.x, Port: 5555)
 * Tab 1: RemoteRelayFragment - REMOTE Tailscale relay (IP: 100.x.x.x, Port: 5556)
 * Tab 2: HelpFragment        - Help docs and credits
 *
 * IMPORTANT: Do NOT mix up the tabs!
 * - Local tab shows WiFi IP for same-network connections
 * - Remote tab shows Tailscale IP for internet connections
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

  override fun getItemCount(): Int = TAB_COUNT

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      TAB_LOCAL_ADB -> LocalAdbFragment()      // Tab 0: WiFi IP + port 5555
      TAB_REMOTE_RELAY -> RemoteRelayFragment() // Tab 1: Tailscale IP + port 5556
      TAB_HELP -> HelpFragment()                // Tab 2: Help & credits
      else -> throw IllegalArgumentException("Invalid tab position: $position")
    }
  }

  companion object {
    const val TAB_COUNT = 3

    // Tab indices - DO NOT CHANGE without updating fragments!
    const val TAB_LOCAL_ADB = 0     // LocalAdbFragment: WiFi connections
    const val TAB_REMOTE_RELAY = 1  // RemoteRelayFragment: Tailscale connections
    const val TAB_HELP = 2          // HelpFragment: Documentation
  }
}
