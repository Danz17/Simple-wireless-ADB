package com.phenix.wirelessadb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.phenix.wirelessadb.AdbManager
import com.phenix.wirelessadb.PrefsManager
import com.phenix.wirelessadb.relay.DeviceAuthManager
import com.phenix.wirelessadb.relay.TailscaleHelper
import kotlinx.coroutines.launch

class AdbViewModel(application: Application) : AndroidViewModel(application) {

  private val context = application.applicationContext
  private val authManager = DeviceAuthManager(context)

  // ADB Status
  private val _adbStatus = MutableLiveData<AdbManager.AdbStatus>()
  val adbStatus: LiveData<AdbManager.AdbStatus> = _adbStatus

  // Root availability
  private val _hasRoot = MutableLiveData<Boolean>()
  val hasRoot: LiveData<Boolean> = _hasRoot

  // Loading state
  private val _isLoading = MutableLiveData(false)
  val isLoading: LiveData<Boolean> = _isLoading

  // Error messages
  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  // Pending approval IP
  private val _pendingApprovalIp = MutableLiveData<String?>()
  val pendingApprovalIp: LiveData<String?> = _pendingApprovalIp

  // Trusted device count
  private val _trustedDeviceCount = MutableLiveData(0)
  val trustedDeviceCount: LiveData<Int> = _trustedDeviceCount

  init {
    checkRootAndRefresh()
  }

  fun checkRootAndRefresh() {
    viewModelScope.launch {
      val rootAvailable = AdbManager.isRootAvailable()
      _hasRoot.value = rootAvailable
      if (rootAvailable) {
        refreshStatus()
      }
    }
  }

  fun refreshStatus() {
    viewModelScope.launch {
      val status = AdbManager.getStatus(context)
      _adbStatus.value = status
      _trustedDeviceCount.value = authManager.getTrustedDeviceCount()
    }
  }

  fun toggleAdb(port: Int) {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      val currentStatus = _adbStatus.value
      val isEnabled = currentStatus?.enabled == true

      val result = if (isEnabled) {
        AdbManager.disable()
      } else {
        PrefsManager.setPort(context, port)
        AdbManager.enable(port)
      }

      result.onSuccess {
        refreshStatus()
      }.onFailure { e ->
        _error.value = e.message
      }

      _isLoading.value = false
    }
  }

  fun setPendingApproval(ip: String?) {
    _pendingApprovalIp.value = ip
  }

  fun refreshTrustedCount() {
    _trustedDeviceCount.value = authManager.getTrustedDeviceCount()
  }

  fun clearError() {
    _error.value = null
  }

  // Preferences
  fun isEnableOnBoot(): Boolean = PrefsManager.isEnableOnBoot(context)
  fun setEnableOnBoot(enabled: Boolean) = PrefsManager.setEnableOnBoot(context, enabled)

  fun isRelayEnabled(): Boolean = PrefsManager.isRelayEnabled(context)
  fun setRelayEnabled(enabled: Boolean) = PrefsManager.setRelayEnabled(context, enabled)

  fun getPort(): Int = PrefsManager.getPort(context)
  fun getRelayPort(): Int = PrefsManager.getRelayPort(context)
}
