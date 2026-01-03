package com.phenix.wirelessadb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.phenix.wirelessadb.AdbService
import com.phenix.wirelessadb.R
import com.phenix.wirelessadb.databinding.FragmentRemoteRelayBinding
import com.phenix.wirelessadb.model.ConnectionMode
import com.phenix.wirelessadb.p2p.P2PState
import com.phenix.wirelessadb.viewmodel.AdbViewModel
import com.phenix.wirelessadb.warpgate.WarpgateConfig

/**
 * TAB 2: REMOTE RELAY (v1.1.0)
 * ============================
 * This fragment handles remote ADB connections with multiple modes:
 * - Tailscale Direct: adb connect <tailscale_ip>:5555 (direct VPN routing)
 * - Tailscale Relay: adb connect <tailscale_ip>:5556 (relay server)
 * - Warpgate: adb connect localhost:5557 (SSH tunnel via bastion)
 */
class RemoteRelayFragment : Fragment() {

  private var _binding: FragmentRemoteRelayBinding? = null
  private val binding get() = _binding!!
  private val viewModel: AdbViewModel by activityViewModels()

  // Connection mode options
  private val connectionModes = listOf(
    ConnectionMode.TAILSCALE_DIRECT,
    ConnectionMode.TAILSCALE_RELAY,
    ConnectionMode.WARPGATE,
    ConnectionMode.P2P_TOKEN
  )

  // P2P token visibility state
  private var isTokenVisible = false

  // Handler for token expiry updates
  private val handler = Handler(Looper.getMainLooper())
  private val expiryUpdateRunnable = object : Runnable {
    override fun run() {
      updateTokenExpiry()
      handler.postDelayed(this, 1000)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentRemoteRelayBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeViewModel()
  }

  private fun setupViews() {
    binding.apply {
      // Setup connection mode dropdown
      val modeLabels = arrayOf(
        getString(R.string.relay_type_tailscale_direct),
        getString(R.string.relay_type_tailscale_relay),
        getString(R.string.relay_type_warpgate),
        getString(R.string.relay_type_p2p_token)
      )
      val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modeLabels)
      connectionModeDropdown.setAdapter(adapter)

      // Set current mode from preferences
      val currentMode = viewModel.getConnectionMode()
      val modeIndex = when (currentMode) {
        ConnectionMode.TAILSCALE_DIRECT -> 0
        ConnectionMode.TAILSCALE_RELAY -> 1
        ConnectionMode.WARPGATE -> 2
        ConnectionMode.P2P_TOKEN -> 3
        else -> 0 // Default to Tailscale Direct for LOCAL_WIFI
      }
      connectionModeDropdown.setText(modeLabels[modeIndex], false)
      updateUIForMode(currentMode)

      connectionModeDropdown.setOnItemClickListener { _, _, position, _ ->
        val selectedMode = connectionModes[position]
        viewModel.setConnectionMode(selectedMode)
        updateUIForMode(selectedMode)
      }

      // Relay switch (only for Tailscale Relay mode)
      relaySwitch.isChecked = viewModel.isRelayEnabled()
      relaySwitch.setOnCheckedChangeListener { _, checked ->
        viewModel.setRelayEnabled(checked)
        restartServiceIfNeeded()
      }

      // Notification hiding switch
      hideNotificationSwitch.isChecked = viewModel.devNotificationHidden.value ?: false
      hideNotificationSwitch.isEnabled = viewModel.isNotificationHidingSupported()
      hideNotificationSwitch.setOnCheckedChangeListener { _, checked ->
        if (checked) {
          viewModel.hideDevNotification()
        } else {
          viewModel.showDevNotification()
        }
      }

      // Copy command button
      remoteCopyButton.setOnClickListener {
        copyRemoteCommand()
      }

      // Warpgate configuration
      setupWarpgateConfig()

      // Pending approval buttons
      approveButton.setOnClickListener {
        viewModel.pendingApprovalIp.value?.let { ip ->
          AdbService.approveDevice(requireContext(), ip)
          viewModel.setPendingApproval(null)
          viewModel.refreshTrustedCount()
        }
      }

      denyButton.setOnClickListener {
        viewModel.pendingApprovalIp.value?.let { ip ->
          AdbService.denyDevice(requireContext(), ip)
          viewModel.setPendingApproval(null)
        }
      }

      // P2P Token handlers
      setupP2PHandlers()
    }
  }

  private fun setupP2PHandlers() {
    binding.apply {
      // Generate token button
      p2pGenerateButton.setOnClickListener {
        viewModel.generateP2PToken()
      }

      // Revoke token button
      p2pRevokeButton.setOnClickListener {
        viewModel.revokeP2PToken()
        isTokenVisible = false
      }

      // Show/Hide token toggle
      p2pShowHideButton.setOnClickListener {
        isTokenVisible = !isTokenVisible
        updateTokenDisplay()
      }

      // Copy token button
      p2pCopyButton.setOnClickListener {
        viewModel.p2pToken.value?.let { token ->
          val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("P2P Token", token.code))
          Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
        }
      }

      // Connect with token button
      p2pConnectButton.setOnClickListener {
        val tokenCode = p2pTokenInput.text.toString().trim()
        if (tokenCode.isNotEmpty()) {
          viewModel.connectWithP2PToken(tokenCode)
        } else {
          Toast.makeText(requireContext(), "Enter a token code", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  private fun setupWarpgateConfig() {
    binding.apply {
      // Load saved Warpgate config
      val config = viewModel.getWarpgateConfig()
      warpgateHostInput.setText(config.host)
      warpgatePortInput.setText(config.port.toString())
      warpgateUsernameInput.setText(config.username)
      warpgatePasswordInput.setText(config.password)
      warpgateTargetInput.setText(config.targetName)

      // Connect/Disconnect button
      warpgateConnectButton.setOnClickListener {
        val isConnected = viewModel.warpgateConnected.value ?: false
        if (isConnected) {
          viewModel.disconnectWarpgate()
        } else {
          // Save config before connecting
          saveWarpgateConfig()
          viewModel.connectWarpgate()
        }
      }
    }
  }

  private fun saveWarpgateConfig() {
    binding.apply {
      val config = WarpgateConfig(
        enabled = true,
        host = warpgateHostInput.text.toString().trim(),
        port = warpgatePortInput.text.toString().toIntOrNull() ?: 8443,
        username = warpgateUsernameInput.text.toString().trim(),
        password = warpgatePasswordInput.text.toString(),
        targetName = warpgateTargetInput.text.toString().trim().ifEmpty { "adb" }
      )
      viewModel.setWarpgateConfig(config)
    }
  }

  private fun updateUIForMode(mode: ConnectionMode) {
    binding.apply {
      // Show/hide Warpgate card
      warpgateCard.visibility = if (mode == ConnectionMode.WARPGATE) View.VISIBLE else View.GONE

      // Show/hide P2P card
      p2pCard.visibility = if (mode == ConnectionMode.P2P_TOKEN) View.VISIBLE else View.GONE

      // Show/hide relay switch (only for Tailscale Relay mode)
      relaySwitch.visibility = if (mode == ConnectionMode.TAILSCALE_RELAY) View.VISIBLE else View.GONE

      // Update port info text
      portInfoText.text = when (mode) {
        ConnectionMode.TAILSCALE_DIRECT -> "Port: 5555 (direct)"
        ConnectionMode.TAILSCALE_RELAY -> "Port: ${viewModel.getRelayPort()} (relay)"
        ConnectionMode.WARPGATE -> "Port: 5557 (local tunnel)"
        ConnectionMode.P2P_TOKEN -> "Port: via NAT traversal"
        else -> "Port: 5555"
      }

      // Show/hide Tailscale card based on mode
      val showTailscale = mode != ConnectionMode.WARPGATE && mode != ConnectionMode.P2P_TOKEN
      tailscaleCard.visibility = if (showTailscale) View.VISIBLE else View.GONE

      // Start/stop expiry timer for P2P mode
      if (mode == ConnectionMode.P2P_TOKEN) {
        handler.post(expiryUpdateRunnable)
      } else {
        handler.removeCallbacks(expiryUpdateRunnable)
      }

      // Update command display
      updateCommandDisplay()
    }
  }

  private fun observeViewModel() {
    viewModel.adbStatus.observe(viewLifecycleOwner) { status ->
      binding.apply {
        if (status.tailscaleIp != null) {
          tailscaleIndicator.setBackgroundResource(R.drawable.ic_status_active)
          tailscaleStatusText.text = getString(R.string.tailscale_connected)
          tailscaleIpText.text = status.tailscaleIp
          remoteCopyButton.isEnabled = status.enabled
        } else {
          tailscaleIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
          tailscaleStatusText.text = getString(R.string.tailscale_not_connected)
          tailscaleIpText.text = "---"
          remoteCopyButton.isEnabled = false
        }
        updateCommandDisplay()
      }
    }

    viewModel.connectionMode.observe(viewLifecycleOwner) { mode ->
      updateUIForMode(mode)
    }

    viewModel.warpgateConnected.observe(viewLifecycleOwner) { connected ->
      binding.apply {
        if (connected) {
          warpgateConnectButton.text = getString(R.string.btn_disconnect)
          warpgateStatusText.text = getString(R.string.warpgate_connected)
          warpgateStatusText.setTextColor(requireContext().getColor(R.color.status_green))
          // Disable inputs while connected
          setWarpgateInputsEnabled(false)
        } else {
          warpgateConnectButton.text = getString(R.string.btn_connect)
          warpgateStatusText.text = getString(R.string.warpgate_disconnected)
          warpgateStatusText.setTextColor(requireContext().getColor(R.color.text_secondary))
          // Enable inputs when disconnected
          setWarpgateInputsEnabled(true)
        }
        updateCommandDisplay()
      }
    }

    viewModel.devNotificationHidden.observe(viewLifecycleOwner) { hidden ->
      binding.hideNotificationSwitch.isChecked = hidden
    }

    viewModel.trustedDeviceCount.observe(viewLifecycleOwner) { count ->
      binding.trustedCountText.text = count.toString()
    }

    viewModel.pendingApprovalIp.observe(viewLifecycleOwner) { ip ->
      binding.apply {
        if (ip != null) {
          pendingCard.visibility = View.VISIBLE
          pendingIpText.text = getString(R.string.pending_device_format, ip)
        } else {
          pendingCard.visibility = View.GONE
        }
      }
    }

    viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
      binding.apply {
        warpgateConnectButton.isEnabled = !loading
        if (loading) {
          warpgateStatusText.text = getString(R.string.warpgate_connecting)
        }
        // Also handle P2P loading
        p2pGenerateButton.isEnabled = !loading
        p2pConnectButton.isEnabled = !loading
      }
    }

    // P2P Token observers
    viewModel.p2pToken.observe(viewLifecycleOwner) { token ->
      binding.apply {
        if (token != null) {
          p2pTokenSection.visibility = View.VISIBLE
          p2pGenerateButton.visibility = View.GONE
          updateTokenDisplay()
        } else {
          p2pTokenSection.visibility = View.GONE
          p2pGenerateButton.visibility = View.VISIBLE
          isTokenVisible = false
        }
      }
    }

    viewModel.p2pState.observe(viewLifecycleOwner) { state ->
      binding.apply {
        p2pStatusText.text = when (state) {
          P2PState.IDLE -> getString(R.string.p2p_status_idle)
          P2PState.TOKEN_READY -> getString(R.string.p2p_status_ready)
          P2PState.CONNECTING -> getString(R.string.p2p_status_connecting)
          P2PState.CONNECTED -> getString(R.string.p2p_status_connected)
          P2PState.FAILED -> getString(R.string.p2p_status_failed)
        }

        // Update status text color
        val color = when (state) {
          P2PState.CONNECTED -> R.color.status_green
          P2PState.FAILED -> R.color.warning_orange
          else -> R.color.text_secondary
        }
        p2pStatusText.setTextColor(requireContext().getColor(color))

        // Update connect button state
        when (state) {
          P2PState.CONNECTING -> {
            p2pConnectButton.isEnabled = false
            p2pConnectButton.text = getString(R.string.p2p_status_connecting)
          }
          P2PState.CONNECTED -> {
            p2pConnectButton.text = getString(R.string.btn_disconnect)
            p2pConnectButton.setOnClickListener { viewModel.disconnectP2P() }
          }
          else -> {
            p2pConnectButton.isEnabled = true
            p2pConnectButton.text = getString(R.string.btn_connect)
            p2pConnectButton.setOnClickListener {
              val tokenCode = p2pTokenInput.text.toString().trim()
              if (tokenCode.isNotEmpty()) {
                viewModel.connectWithP2PToken(tokenCode)
              }
            }
          }
        }
      }
    }

    viewModel.p2pError.observe(viewLifecycleOwner) { error ->
      error?.let {
        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        viewModel.clearP2PError()
      }
    }
  }

  private fun setWarpgateInputsEnabled(enabled: Boolean) {
    binding.apply {
      warpgateHostInput.isEnabled = enabled
      warpgatePortInput.isEnabled = enabled
      warpgateUsernameInput.isEnabled = enabled
      warpgatePasswordInput.isEnabled = enabled
      warpgateTargetInput.isEnabled = enabled
    }
  }

  private fun updateTokenDisplay() {
    val token = viewModel.p2pToken.value ?: return
    binding.apply {
      p2pTokenText.text = if (isTokenVisible) token.code else token.masked()
      p2pShowHideButton.setIconResource(
        if (isTokenVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
      )
      p2pShowHideButton.contentDescription = getString(
        if (isTokenVisible) R.string.p2p_hide_token else R.string.p2p_show_token
      )
      updateTokenExpiry()
    }
  }

  private fun updateTokenExpiry() {
    val token = viewModel.p2pToken.value ?: return
    binding.apply {
      if (token.isExpired()) {
        p2pExpiresText.text = "Token expired"
        p2pExpiresText.setTextColor(requireContext().getColor(R.color.warning_orange))
      } else {
        p2pExpiresText.text = getString(R.string.p2p_expires_in, token.getRemainingTime())
        p2pExpiresText.setTextColor(requireContext().getColor(R.color.text_secondary))
      }
    }
  }

  private fun updateCommandDisplay() {
    binding.apply {
      val command = viewModel.getConnectionCommand()
      remoteCommandText.text = command.ifEmpty { getString(R.string.tailscale_not_active) }
      remoteCopyButton.isEnabled = command.isNotEmpty()
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

  private fun copyRemoteCommand() {
    val command = binding.remoteCommandText.text.toString()
    if (command.isEmpty() || command == getString(R.string.tailscale_not_active)) {
      Toast.makeText(requireContext(), "No command to copy", Toast.LENGTH_SHORT).show()
      return
    }
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Remote ADB Command", command))
    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    handler.removeCallbacks(expiryUpdateRunnable)
    _binding = null
  }
}
