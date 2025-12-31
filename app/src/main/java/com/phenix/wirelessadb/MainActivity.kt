package com.phenix.wirelessadb

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.phenix.wirelessadb.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private var isEnabled = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupEdgeToEdge()
    requestNotificationPermission()
    setupViews()
    checkRootAndRefresh()
  }

  override fun onResume() {
    super.onResume()
    refreshStatus()
  }

  private fun setupEdgeToEdge() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, windowInsets ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(
        left = insets.left + 24,
        top = insets.top + 24,
        right = insets.right + 24,
        bottom = insets.bottom + 24
      )
      windowInsets
    }
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.POST_NOTIFICATIONS),
          100
        )
      }
    }
  }

  private fun setupViews() {
    binding.apply {
      portInput.setText(PrefsManager.getPort(this@MainActivity).toString())

      toggleButton.setOnClickListener {
        toggleAdb()
      }

      bootSwitch.isChecked = PrefsManager.isEnableOnBoot(this@MainActivity)
      bootSwitch.setOnCheckedChangeListener { _, checked ->
        PrefsManager.setEnableOnBoot(this@MainActivity, checked)
      }

      copyButton.setOnClickListener {
        copyCommand()
      }
    }
  }

  private fun checkRootAndRefresh() {
    lifecycleScope.launch {
      val hasRoot = AdbManager.isRootAvailable()
      if (!hasRoot) {
        binding.statusText.text = "Root access required"
        binding.statusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
        binding.toggleButton.isEnabled = false
        Toast.makeText(this@MainActivity, "No root access", Toast.LENGTH_LONG).show()
      } else {
        refreshStatus()
      }
    }
  }

  private fun refreshStatus() {
    lifecycleScope.launch {
      val status = AdbManager.getStatus(this@MainActivity)
      isEnabled = status.enabled

      binding.apply {
        if (status.enabled && status.ip != null) {
          statusText.text = "Connected"
          statusIndicator.setBackgroundResource(R.drawable.ic_status_active)
          ipText.text = status.ip
          portText.text = ":${status.port}"
          commandText.text = "adb connect ${status.ip}:${status.port}"
          toggleButton.text = "Disable"
          copyButton.isEnabled = true
        } else {
          statusText.text = "Disabled"
          statusIndicator.setBackgroundResource(R.drawable.ic_status_inactive)
          ipText.text = "---.---.---.---"
          portText.text = ":${status.port}"
          commandText.text = "ADB not active"
          toggleButton.text = "Enable"
          copyButton.isEnabled = false
        }
      }
    }
  }

  private fun toggleAdb() {
    val port = binding.portInput.text.toString().toIntOrNull() ?: 5555

    if (!AdbManager.validatePort(port)) {
      Toast.makeText(this, "Port must be 1024-65535", Toast.LENGTH_SHORT).show()
      return
    }

    PrefsManager.setPort(this, port)
    binding.toggleButton.isEnabled = false

    lifecycleScope.launch {
      val result = if (isEnabled) {
        AdbService.stop(this@MainActivity)
        AdbManager.disable()
      } else {
        AdbManager.enable(port)
      }

      result.onSuccess {
        if (!isEnabled) {
          val status = AdbManager.getStatus(this@MainActivity)
          status.ip?.let { ip ->
            AdbService.start(this@MainActivity, ip, port)
          }
        }
        refreshStatus()
      }.onFailure { e ->
        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
      }

      binding.toggleButton.isEnabled = true
    }
  }

  private fun copyCommand() {
    val command = binding.commandText.text.toString()
    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
    Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
  }
}
