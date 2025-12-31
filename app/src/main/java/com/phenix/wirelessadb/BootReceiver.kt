package com.phenix.wirelessadb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
        intent.action != "android.intent.action.QUICKBOOT_POWERON") {
      return
    }

    if (!PrefsManager.isEnableOnBoot(context)) return

    val port = PrefsManager.getPort(context)

    CoroutineScope(Dispatchers.IO).launch {
      AdbManager.enable(port)
    }
  }
}
