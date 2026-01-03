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
import com.phenix.wirelessadb.R
import com.phenix.wirelessadb.databinding.FragmentLocalAdbBinding
import com.phenix.wirelessadb.viewmodel.AdbViewModel

/**
 * TAB 1: LOCAL ADB
 * ================
 * This fragment handles LOCAL NETWORK ADB connections:
 * - Shows WiFi IP address (e.g., 10.1.1.107)
 * - Uses local port (default 5555)
 * - Command format: adb connect <wifi_ip>:5555
 *
 * For REMOTE/TAILSCALE connections, see [RemoteRelayFragment]
 */
class LocalAdbFragment : Fragment() {

  private var _binding: FragmentLocalAdbBinding? = null
  private val binding get() = _binding!!
  private val viewModel: AdbViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentLocalAdbBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeViewModel()
  }

  private fun setupViews() {
    binding.apply {
      portInput.setText(viewModel.getPort().toString())

      toggleButton.setOnClickListener {
        val port = portInput.text.toString().toIntOrNull() ?: 5555
        viewModel.toggleAdb(port)
      }

      bootSwitch.isChecked = viewModel.isEnableOnBoot()
      bootSwitch.setOnCheckedChangeListener { _, checked ->
        viewModel.setEnableOnBoot(checked)
      }

      copyButton.setOnClickListener {
        copyCommand()
      }
    }
  }

  private fun observeViewModel() {
    viewModel.hasRoot.observe(viewLifecycleOwner) { hasRoot ->
      binding.apply {
        toggleButton.isEnabled = hasRoot
        rootWarning.visibility = if (hasRoot) View.GONE else View.VISIBLE
        if (!hasRoot) {
          statusText.text = getString(R.string.error_no_root)
          statusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
        }
      }
    }

    viewModel.adbStatus.observe(viewLifecycleOwner) { status ->
      binding.apply {
        if (status.enabled && status.ip != null) {
          statusText.text = getString(R.string.status_connected)
          statusIndicator.setBackgroundResource(R.drawable.ic_status_active)
          // LOCAL: Show WiFi IP (NOT Tailscale IP)
          ipText.text = status.ip
          portText.text = ":${status.port}"
          // LOCAL: Command uses WiFi IP and local port (5555)
          commandText.text = "adb connect ${status.ip}:${status.port}"
          toggleButton.text = getString(R.string.btn_disable)
          copyButton.isEnabled = true
        } else {
          statusText.text = getString(R.string.status_disabled)
          statusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
          ipText.text = "---.---.---.---"
          portText.text = ":${status.port}"
          commandText.text = getString(R.string.adb_not_active)
          toggleButton.text = getString(R.string.btn_enable)
          copyButton.isEnabled = false
        }
      }
    }

    viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
      binding.toggleButton.isEnabled = !loading && viewModel.hasRoot.value == true
    }

    viewModel.error.observe(viewLifecycleOwner) { error ->
      error?.let {
        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        viewModel.clearError()
      }
    }
  }

  private fun copyCommand() {
    val command = binding.commandText.text.toString()
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
