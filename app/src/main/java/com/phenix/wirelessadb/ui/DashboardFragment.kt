package com.phenix.wirelessadb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.phenix.wirelessadb.AdbService
import com.phenix.wirelessadb.R
import com.phenix.wirelessadb.databinding.FragmentDashboardBinding
import com.phenix.wirelessadb.model.ConnectionMode
import com.phenix.wirelessadb.model.IndicatorState
import com.phenix.wirelessadb.pairing.AdbPairingManager
import com.phenix.wirelessadb.viewmodel.AdbViewModel

/**
 * TAB 0: DASHBOARD (v1.2.0)
 * =========================
 * Unified control center merging Local ADB + settings:
 * - Quick Status: 4 indicators (Local/Tailscale/Warpgate/P2P) + current command
 * - Quick Toggles: Local ADB, Tailscale Relay, Warpgate
 * - Settings: Boot persistence, notification hiding
 * - Trusted Devices: Count and manage button
 */
class DashboardFragment : Fragment() {

  private var _binding: FragmentDashboardBinding? = null
  private val binding get() = _binding!!
  private val viewModel: AdbViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDashboardBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeViewModel()
  }

  private fun setupViews() {
    binding.apply {
      // Port input - load saved value
      portInput.setText(viewModel.getPort().toString())

      // Local ADB Switch
      localAdbSwitch.setOnCheckedChangeListener { _, checked ->
        if (checked != (viewModel.adbStatus.value?.enabled == true)) {
          val port = portInput.text.toString().toIntOrNull() ?: 5555
          viewModel.toggleAdb(port)
        }
      }

      // Tailscale Relay Switch
      tailscaleSwitch.isChecked = viewModel.isRelayEnabled()
      tailscaleSwitch.setOnCheckedChangeListener { _, checked ->
        viewModel.setRelayEnabled(checked)
        if (checked) {
          viewModel.setConnectionMode(ConnectionMode.TAILSCALE_RELAY)
        }
        restartServiceIfNeeded()
      }

      // Warpgate Switch
      warpgateSwitch.isChecked = viewModel.warpgateConnected.value ?: false
      warpgateSwitch.setOnCheckedChangeListener { _, checked ->
        if (checked) {
          viewModel.setConnectionMode(ConnectionMode.WARPGATE)
          viewModel.connectWarpgate()
        } else {
          viewModel.disconnectWarpgate()
        }
      }

      // Boot Switch
      bootSwitch.isChecked = viewModel.isEnableOnBoot()
      bootSwitch.setOnCheckedChangeListener { _, checked ->
        viewModel.setEnableOnBoot(checked)
      }

      // Hide Notification Switch
      hideNotificationSwitch.isChecked = viewModel.devNotificationHidden.value ?: false
      hideNotificationSwitch.isEnabled = viewModel.isNotificationHidingSupported()
      hideNotificationSwitch.setOnCheckedChangeListener { _, checked ->
        if (checked) {
          viewModel.hideDevNotification()
        } else {
          viewModel.showDevNotification()
        }
      }

      // Copy Button
      copyButton.setOnClickListener {
        copyCommand()
      }

      // Manage Devices Button
      manageDevicesButton.setOnClickListener {
        // Navigate to trusted devices management (future enhancement)
        Toast.makeText(requireContext(), "Device management coming in v1.3.0", Toast.LENGTH_SHORT).show()
      }

      // ADB Pairing Button (Android 11+)
      setupPairingCard()
    }
  }

  private fun setupPairingCard() {
    binding.apply {
      // Show pairing card only on Android 11+
      if (AdbPairingManager.isSupported()) {
        pairingCard.visibility = View.VISIBLE
        pairingButton.setOnClickListener {
          AdbPairingDialog.showOrAlert(this@DashboardFragment) {
            // On success, refresh ADB status
            viewModel.refreshStatus()
            Toast.makeText(requireContext(), R.string.pairing_success, Toast.LENGTH_SHORT).show()
          }
        }
      } else {
        // Hide on older Android versions
        pairingCard.visibility = View.GONE
      }
    }
  }

  private fun observeViewModel() {
    // Root availability
    viewModel.hasRoot.observe(viewLifecycleOwner) { hasRoot ->
      binding.apply {
        localAdbSwitch.isEnabled = hasRoot
        rootWarning.visibility = if (hasRoot) View.GONE else View.VISIBLE
        if (!hasRoot) {
          mainStatusText.text = getString(R.string.error_no_root)
          mainStatusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
        }
      }
    }

    // ADB Status
    viewModel.adbStatus.observe(viewLifecycleOwner) { status ->
      binding.apply {
        val isEnabled = status.enabled && status.ip != null

        // Update Local ADB switch without triggering listener
        localAdbSwitch.setOnCheckedChangeListener(null)
        localAdbSwitch.isChecked = status.enabled
        localAdbSwitch.setOnCheckedChangeListener { _, checked ->
          if (checked != (viewModel.adbStatus.value?.enabled == true)) {
            val port = portInput.text.toString().toIntOrNull() ?: 5555
            viewModel.toggleAdb(port)
          }
        }

        // Main status
        if (isEnabled) {
          mainStatusText.text = getString(R.string.status_connected)
          mainStatusIndicator.setBackgroundResource(R.drawable.ic_status_active)
          ipPortText.text = "${status.ip}:${status.port}"
          copyButton.isEnabled = true
        } else {
          mainStatusText.text = getString(R.string.status_disabled)
          mainStatusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
          ipPortText.text = "---.---.---.---:${status.port}"
          copyButton.isEnabled = false
        }

        // Update command text
        updateCommandDisplay()
      }
    }

    // Status Indicators
    viewModel.statusIndicators.observe(viewLifecycleOwner) { indicators ->
      binding.apply {
        indicatorLocalAdb.setBackgroundResource(
          when (indicators.localAdb) {
            IndicatorState.ACTIVE -> R.drawable.ic_status_active
            IndicatorState.WARNING -> R.drawable.ic_status_warning
            IndicatorState.INACTIVE -> R.drawable.ic_status_inactive
          }
        )
        indicatorTailscale.setBackgroundResource(
          when (indicators.tailscale) {
            IndicatorState.ACTIVE -> R.drawable.ic_status_active
            IndicatorState.WARNING -> R.drawable.ic_status_warning
            IndicatorState.INACTIVE -> R.drawable.ic_status_inactive
          }
        )
        indicatorWarpgate.setBackgroundResource(
          when (indicators.warpgate) {
            IndicatorState.ACTIVE -> R.drawable.ic_status_active
            IndicatorState.WARNING -> R.drawable.ic_status_warning
            IndicatorState.INACTIVE -> R.drawable.ic_status_inactive
          }
        )
        // P2P indicator - inactive for now (Phase 4)
        indicatorP2p.setBackgroundResource(R.drawable.ic_status_inactive)
      }
    }

    // Warpgate connection status
    viewModel.warpgateConnected.observe(viewLifecycleOwner) { connected ->
      binding.apply {
        warpgateSwitch.setOnCheckedChangeListener(null)
        warpgateSwitch.isChecked = connected
        warpgateSwitch.setOnCheckedChangeListener { _, checked ->
          if (checked) {
            viewModel.setConnectionMode(ConnectionMode.WARPGATE)
            viewModel.connectWarpgate()
          } else {
            viewModel.disconnectWarpgate()
          }
        }
        updateCommandDisplay()
      }
    }

    // Developer notification state
    viewModel.devNotificationHidden.observe(viewLifecycleOwner) { hidden ->
      binding.hideNotificationSwitch.isChecked = hidden
    }

    // Trusted device count
    viewModel.trustedDeviceCount.observe(viewLifecycleOwner) { count ->
      binding.trustedCountText.text = resources.getQuantityString(
        R.plurals.device_count, count, count
      )
    }

    // Loading state
    viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
      binding.apply {
        localAdbSwitch.isEnabled = !loading && viewModel.hasRoot.value == true
        warpgateSwitch.isEnabled = !loading
      }
    }

    // Error messages
    viewModel.error.observe(viewLifecycleOwner) { error ->
      error?.let {
        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        viewModel.clearError()
      }
    }
  }

  private fun updateCommandDisplay() {
    binding.apply {
      val command = viewModel.getConnectionCommand()
      commandText.text = command.ifEmpty { getString(R.string.adb_not_active) }
      copyButton.isEnabled = command.isNotEmpty()
    }
  }

  private fun restartServiceIfNeeded() {
    val status = viewModel.adbStatus.value ?: return
    if (!status.enabled) return

    status.ip?.let { ip ->
      AdbService.stop(requireContext())
      val relayEnabled = viewModel.isRelayEnabled()
      AdbService.start(requireContext(), ip, status.port, relayEnabled)
    }
  }

  private fun copyCommand() {
    val command = binding.commandText.text.toString()
    if (command.isEmpty() || command == getString(R.string.adb_not_active)) {
      return
    }
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
