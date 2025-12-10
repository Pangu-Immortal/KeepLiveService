package com.service.framework.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import android.support.v4.media.session.MediaSessionCompat
import com.service.framework.Fw
import com.service.framework.R
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

@SuppressLint("WakelockTimeout")
class FwForegroundService : LifecycleService() {

    companion object {
        const val EXTRA_START_REASON = "start_reason"
        private const val NOTIFICATION_ID = 10001
    }

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        FwLog.d("FwForegroundService initializing...")

        startForegroundWithNotification()

        if (Fw.config.enableMediaSession) {
            mediaSession = createMediaSession()
            mediaSession?.isActive = true
        }
        wakeLock = createWakeLock()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val reason = intent?.getStringExtra(EXTRA_START_REASON) ?: "未知原因"
        FwLog.d("Service started or restarted. Reason: $reason")
        startForegroundWithNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.w("FwForegroundService is being destroyed.")

        releaseMediaSession()
        releaseWakeLock()

        ServiceStarter.startForegroundService(this, "服务被杀后自救")
    }

    private fun startForegroundWithNotification() {
        createNotificationChannel()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            FwLog.d("Service promoted to foreground successfully.")
        } catch (e: Exception) {
            FwLog.e("Failed to start foreground service", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Fw.config.notificationChannelId,
                Fw.config.notificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持后台服务运行"
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val config = Fw.config

        val pendingIntent = config.notificationActivityClass?.let {
            val intent = Intent(this, it).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(this, 0, intent, flags)
        }

        return NotificationCompat.Builder(this, config.notificationChannelId)
            .setContentTitle(config.notificationTitle)
            .setContentText(config.notificationContent)
            .setSmallIcon(config.notificationIconResId)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createMediaSession(): MediaSessionCompat {
        FwLog.d("Creating MediaSession...")
        return MediaSessionCompat(this, "FwMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
    }

    private fun releaseMediaSession() {
        mediaSession?.let {
            it.release()
            FwLog.d("MediaSession released.")
        }
        mediaSession = null
    }

    private fun createWakeLock(): PowerManager.WakeLock {
        FwLog.d("Creating WakeLock...")
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Fw::WakeLock").apply {
            setReferenceCounted(false)
        }
    }

    private fun acquireWakeLock() {
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(10 * 60 * 1000L) // 持有 10 分钟超时
                    FwLog.d("WakeLock acquired.")
                }
            }
        } catch (e: Exception) {
            FwLog.e("Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    FwLog.d("WakeLock released.")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            FwLog.e("Failed to release WakeLock", e)
        }
    }

    override fun onBind(intent: Intent) = super.onBind(intent)
}
