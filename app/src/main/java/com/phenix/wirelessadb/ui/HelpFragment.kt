package com.phenix.wirelessadb.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.phenix.wirelessadb.BuildConfig
import com.phenix.wirelessadb.R
import com.phenix.wirelessadb.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

  private var _binding: FragmentHelpBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHelpBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
  }

  private fun setupViews() {
    binding.apply {
      versionText.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)

      githubButton.setOnClickListener {
        openUrl(GITHUB_URL)
      }

      emailButton.setOnClickListener {
        sendEmail(DEV_EMAIL, getString(R.string.email_subject_contact))
      }

      bugReportButton.setOnClickListener {
        sendBugReport()
      }

      downloadScriptsButton.setOnClickListener {
        openUrl(SCRIPTS_URL)
      }
    }
  }

  private fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
  }

  private fun sendEmail(email: String, subject: String, body: String = "") {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:")
      putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
      putExtra(Intent.EXTRA_SUBJECT, subject)
      if (body.isNotEmpty()) {
        putExtra(Intent.EXTRA_TEXT, body)
      }
    }
    if (intent.resolveActivity(requireContext().packageManager) != null) {
      startActivity(intent)
    }
  }

  private fun sendBugReport() {
    val deviceInfo = buildString {
      appendLine("--- Device Info ---")
      appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
      appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
      appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
      appendLine("-------------------")
      appendLine()
      appendLine("Describe your issue:")
      appendLine()
    }

    sendEmail(
      DEV_EMAIL,
      getString(R.string.email_subject_bug),
      deviceInfo
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    private const val GITHUB_URL = "https://github.com/Danz17/Simple-wireless-ADB"
    private const val SCRIPTS_URL = "https://github.com/Danz17/Simple-wireless-ADB/releases"
    private const val DEV_EMAIL = "alaa@nulled.ai"
  }
}
