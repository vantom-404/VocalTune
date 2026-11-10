package com.intervigil.micdroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.intervigil.micdroid.MainActivity
import com.intervigil.micdroid.R

/**
 * Foreground Service that keeps the vocal recording process active
 * and prevents Android from killing the audio thread when the user
 * leaves the app, switches tabs, or turns off the screen.
 */
class VocalRecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "vocal_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.intervigil.micdroid.action.START_RECORDING"
        const val ACTION_PAUSE = "com.intervigil.micdroid.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.intervigil.micdroid.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.intervigil.micdroid.action.STOP_RECORDING"

        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, VocalRecordingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VocalRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                isServiceRunning = true
                isPaused = false
                val notification = buildNotification(isPaused = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_PAUSE -> {
                isPaused = true
                val notification = buildNotification(isPaused = true)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_RESUME -> {
                isPaused = false
                val notification = buildNotification(isPaused = false)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                isServiceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VocalTune Studio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and controls for active vocal recording"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Recording Paused" else "Recording Auto-Tuned Vocals..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VocalTune Studio")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }
}
