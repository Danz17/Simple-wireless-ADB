package com.phenix.wirelessadb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
import com.phenix.wirelessadb.viewmodel.AdbViewModel

/**
 * TAB 2: REMOTE RELAY
 * ===================
 * This fragment handles TAILSCALE/REMOTE ADB connections:
 * - Shows Tailscale IP address (e.g., 100.99.87.44)
 * - Uses relay port (default 5556)
 * - Command format: adb connect <tailscale_ip>:5556
 *
 * For LOCAL/WIFI connections, see [LocalAdbFragment]
 */
class RemoteRelayFragment : Fragment() {

  private var _binding: FragmentRemoteRelayBinding? = null
  private val binding get() = _binding!!
  private val viewModel: AdbViewModel by activityViewModels()

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
      // Setup relay type dropdown
      val relayTypes = arrayOf(
        getString(R.string.relay_type_tailscale),
        getString(R.string.relay_type_ssh),
        getString(R.string.relay_type_custom)
      )
      val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, relayTypes)
      relayTypeDropdown.setAdapter(adapter)
      relayTypeDropdown.setText(relayTypes[0], false) // Default to Tailscale
      relayTypeDropdown.setOnItemClickListener { _, _, position, _ ->
        when (position) {
          0 -> { /* Tailscale - currently active */ }
          1, 2 -> {
            // SSH Tunnel and Custom Relay coming soon
            Toast.makeText(requireContext(), "Coming soon!", Toast.LENGTH_SHORT).show()
            relayTypeDropdown.setText(relayTypes[0], false) // Reset to Tailscale
          }
        }
      }

      relaySwitch.isChecked = viewModel.isRelayEnabled()
      relaySwitch.setOnCheckedChangeListener { _, checked ->
        viewModel.setRelayEnabled(checked)
        restartServiceIfNeeded()
      }

      remoteCopyButton.setOnClickListener {
        copyRemoteCommand()
      }

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
    }
  }

  private fun observeViewModel() {
    viewModel.adbStatus.observe(viewLifecycleOwner) { status ->
      binding.apply {
        // REMOTE: Use relay port (default 5556)
        val relayPort = viewModel.getRelayPort()
        relayPortText.text = getString(R.string.relay_port_format, relayPort)

        if (status.tailscaleIp != null) {
          tailscaleIndicator.setBackgroundResource(R.drawable.ic_status_active)
          tailscaleStatusText.text = getString(R.string.tailscale_connected)
          // REMOTE: Show Tailscale IP (NOT WiFi IP)
          tailscaleIpText.text = status.tailscaleIp
          // REMOTE: Command uses Tailscale IP and relay port (5556)
          remoteCommandText.text = "adb connect ${status.tailscaleIp}:$relayPort"
          remoteCopyButton.isEnabled = status.enabled
        } else {
          tailscaleIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
          tailscaleStatusText.text = getString(R.string.tailscale_not_connected)
          tailscaleIpText.text = "---"
          remoteCommandText.text = getString(R.string.tailscale_not_active)
          remoteCopyButton.isEnabled = false
        }
      }
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
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Remote ADB Command", command))
    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
