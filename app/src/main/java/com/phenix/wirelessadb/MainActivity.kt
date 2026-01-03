package com.phenix.wirelessadb

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayoutMediator
import com.phenix.wirelessadb.databinding.ActivityMainBinding
import com.phenix.wirelessadb.model.IndicatorState
import com.phenix.wirelessadb.model.StatusIndicators
import com.phenix.wirelessadb.theme.ThemeManager
import com.phenix.wirelessadb.ui.MainPagerAdapter
import com.phenix.wirelessadb.viewmodel.AdbViewModel

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private val viewModel: AdbViewModel by viewModels()

  private val pendingAuthReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val clientIp = intent.getStringExtra(AdbService.EXTRA_CLIENT_IP)
      if (clientIp != null) {
        viewModel.setPendingApproval(clientIp)
      }
    }
  }

  // Network change listener - refreshes status when VPN connects/disconnects
  private val networkChangeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      viewModel.refreshStatus()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Apply saved theme before super.onCreate()
    ThemeManager.applyTheme(this)

    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    requestNotificationPermission()
    setupViewPager()
    observeStatusIndicators()

    LocalBroadcastManager.getInstance(this).registerReceiver(
      pendingAuthReceiver,
      IntentFilter(AdbService.ACTION_PENDING_AUTH)
    )

    // Register network change listener for VPN connect/disconnect
    @Suppress("DEPRECATION")
    registerReceiver(
      networkChangeReceiver,
      IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    )
  }

  override fun onDestroy() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(pendingAuthReceiver)
    unregisterReceiver(networkChangeReceiver)
    super.onDestroy()
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshStatus()
    viewModel.refreshTrustedCount()
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.POST_NOTIFICATIONS),
          REQUEST_NOTIFICATION_PERMISSION
        )
      }
    }
  }

  private fun setupViewPager() {
    val adapter = MainPagerAdapter(this)
    binding.viewPager.adapter = adapter

    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
      tab.text = when (position) {
        MainPagerAdapter.TAB_DASHBOARD -> getString(R.string.tab_dashboard)
        MainPagerAdapter.TAB_REMOTE_RELAY -> getString(R.string.tab_remote_relay)
        MainPagerAdapter.TAB_HELP -> getString(R.string.tab_help)
        else -> null
      }
    }.attach()
  }

  private fun observeStatusIndicators() {
    viewModel.statusIndicators.observe(this) { indicators ->
      updateToolbarIndicators(indicators)
    }
  }

  private fun updateToolbarIndicators(indicators: StatusIndicators) {
    // Local ADB indicator
    binding.indicatorLocalAdb.setBackgroundResource(
      when (indicators.localAdb) {
        IndicatorState.ACTIVE -> R.drawable.indicator_dot_active
        IndicatorState.WARNING -> R.drawable.indicator_dot_warning
        IndicatorState.INACTIVE -> R.drawable.indicator_dot_inactive
      }
    )

    // Tailscale indicator
    binding.indicatorTailscale.setBackgroundResource(
      when (indicators.tailscale) {
        IndicatorState.ACTIVE -> R.drawable.indicator_dot_active
        IndicatorState.WARNING -> R.drawable.indicator_dot_warning
        IndicatorState.INACTIVE -> R.drawable.indicator_dot_inactive
      }
    )

    // Warpgate indicator
    binding.indicatorWarpgate.setBackgroundResource(
      when (indicators.warpgate) {
        IndicatorState.ACTIVE -> R.drawable.indicator_dot_active
        IndicatorState.WARNING -> R.drawable.indicator_dot_warning
        IndicatorState.INACTIVE -> R.drawable.indicator_dot_inactive
      }
    )
  }

  companion object {
    private const val REQUEST_NOTIFICATION_PERMISSION = 100
  }
}
