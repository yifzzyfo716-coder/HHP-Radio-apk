package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.HhpRadioApp
import com.example.MainActivity
import com.example.R
import com.example.model.PttState

class HhpRadioService : Service() {

    private lateinit var sessionManager: RadioSessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = RadioSessionManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        HhpRadioApp.NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        } else {
                            0
                        }
                    )
                } else {
                    startForeground(HhpRadioApp.NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE -> {
                val notification = buildNotification()
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(HhpRadioApp.NOTIFICATION_ID, notification)
            }
            ACTION_DISCONNECT -> {
                sessionManager.disconnectServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, HhpRadioService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentChId = sessionManager.radioClient.currentChannelId.value
        val channel = sessionManager.radioClient.channels.value.find { it.id == currentChId }
        val channelName = channel?.name ?: currentChId

        val pttState = sessionManager.pttState.value
        val activeSpeaker = sessionManager.radioClient.activeSpeaker.value

        val statusText = when {
            pttState == PttState.TRANSMITTING -> getString(R.string.notif_transmitting)
            activeSpeaker != null -> getString(R.string.notif_receiving, activeSpeaker)
            else -> getString(R.string.notif_connected_format, channelName)
        }

        return NotificationCompat.Builder(this, HhpRadioApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notif_action_disconnect),
                disconnectPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_UPDATE = "com.example.ACTION_UPDATE"
        const val ACTION_DISCONNECT = "com.example.ACTION_DISCONNECT"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }
}
