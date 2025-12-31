package com.phenix.wirelessadb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdbService : Service() {

  companion object {
    private const val CHANNEL_ID = "adb_service_channel"
    private const val NOTIFICATION_ID = 1001

    fun start(context: Context, ip: String, port: Int) {
      val intent = Intent(context, AdbService::class.java).apply {
        putExtra("ip", ip)
        putExtra("port", port)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, AdbService::class.java))
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val ip = intent?.getStringExtra("ip") ?: "Unknown"
    val port = intent?.getIntExtra("port", 5555) ?: 5555

    startForeground(NOTIFICATION_ID, createNotification(ip, port))
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "ADB Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows when Wireless ADB is active"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun createNotification(ip: String, port: Int): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Wireless ADB Active")
      .setContentText("adb connect $ip:$port")
      .setSmallIcon(R.drawable.ic_notification)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .build()
  }
}
