/**
 * ============================================================================
 * AssistService3.kt - 辅助进程服务 3
 * ============================================================================
 *
 * 功能简介：
 *   运行在独立进程 :assist3 中的辅助服务。
 *   负责锁定自己的文件并监控其他辅助进程，实现多进程互相守护。
 *
 * 进程名：:assist3
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.service.framework.R
import com.service.framework.util.FwLog

class AssistService3 : Service() {

    companion object {
        private const val TAG = "AssistService3" // 日志标签
        private const val NOTIFICATION_ID = 20003 // 通知 ID
        private const val CHANNEL_ID = "fw_assist3_channel" // 通知渠道 ID
    }

    override fun onCreate() {
        super.onCreate()
        FwLog.d("$TAG: onCreate, PID=${android.os.Process.myPid()}")

        // 提升为前台服务
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("$TAG: onStartCommand")
        return START_STICKY // 被杀后自动重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        FwLog.w("$TAG: onDestroy, 尝试自救")

        // 被杀时尝试拉起其他服务
        try {
            val intent = Intent(this, AssistService1::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            FwLog.e("$TAG: 自救失败", e)
        }
    }

    /**
     * 启动前台服务并显示通知
     */
    private fun startForegroundWithNotification() {
        createNotificationChannel()
        val notification = buildNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            FwLog.d("$TAG: 前台服务启动成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: 前台服务启动失败", e)
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "辅助进程3",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "辅助进程服务"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * 构建通知
     */
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("辅助进程3")
            .setContentText("守护中...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
