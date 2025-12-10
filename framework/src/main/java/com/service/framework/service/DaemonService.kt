package com.service.framework.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.service.framework.Fw
import com.service.framework.R
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 守护进程服务，运行于独立的 `:daemon` 进程中。
 *
 * **核心机制**：
 * 1.  **独立进程**: 在 `AndroidManifest.xml` 中通过 `android:process=":daemon"` 实现，与主进程隔离。
 * 2.  **双向守护**: 此服务监控主进程，主进程的 `FwForegroundService` 也通过 `startService` 保证此服务的运行。
 * 3.  **协程定时检查**: 使用 `lifecycleScope` 启动一个轻量级的协程，周期性地检查主进程是否存活。
 * 4.  **进程存活判断**: 通过遍历 `ActivityManager.getRunningAppProcesses()` 来判断主进程（包名同名进程）是否在运行，此方法比废弃的 `getRunningServices` 更可靠。
 * 5.  **自动拉起**: 如果检测到主进程死亡，会立即调用 [ServiceStarter] 尝试拉起主进程的 [FwForegroundService]。
 *
 * **生命周期**：
 * - `onCreate()`: 提升为前台服务，并启动监控协程。
 * - `onDestroy()`: 协程自动取消，并尝试最后一次拉起主服务，形成闭环。
 *
 * @author https://github.com/Pangu-Immortal/KeepAlivePerfect
 * @since 1.0.0
 */
class DaemonService : LifecycleService() {

    private var monitoringJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 10002
        private const val CHANNEL_ID = "fw_daemon_channel"
    }

    override fun onCreate() {
        super.onCreate()
        FwLog.d("DaemonService initializing in PID: ${android.os.Process.myPid()}")

        startForegroundWithNotification()
        startMonitoringJob()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        FwLog.d("DaemonService received start command.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // lifecycleScope 会自动取消 monitoringJob，无需手动停止
        FwLog.w("DaemonService is being destroyed. It will attempt to restart the main service.")
        // 作为最后的保障，在自身被销毁时尝试拉起主服务
        ServiceStarter.startForegroundService(this, "守护进程被杀后拉起")
    }

    private fun startForegroundWithNotification() {
        createNotificationChannel()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            FwLog.e("DaemonService failed to start foreground", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "守护精灵",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "守护精灵后台服务"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("守护精灵")
            .setContentText("正在默默守护中~")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    /**
     * 启动一个周期性监控任务，使用协程实现。
     */
    private fun startMonitoringJob() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = lifecycleScope.launch {
            FwLog.d("Starting main process monitoring job.")
            while (isActive) {
                delay(Fw.config.dualProcessCheckInterval)
                checkAndRestartMainProcess()
            }
        }
    }

    /**
     * 检查主进程是否存活，如果不在则尝试拉起。
     */
    private fun checkAndRestartMainProcess() {
        if (!isMainProcessAlive()) {
            FwLog.w("Main process is not running. Attempting to restart it...")
            ServiceStarter.startForegroundService(this, "守护进程拉起")
        } else {
            FwLog.d("Main process is alive.")
        }
    }

    /**
     * 通过检查正在运行的进程列表来判断主进程是否存活。
     * @return 如果主进程正在运行，返回 `true`。
     */
    private fun isMainProcessAlive(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mainProcessName = applicationInfo.packageName

        // getRunningAppProcesses 在较新系统上可能只返回当前应用自己的进程，但这正是我们需要的
        return am.runningAppProcesses.orEmpty().any { it.processName == mainProcessName }
    }

    // onBind 不是必须的，但因为是 LifecycleService，所以保留
    override fun onBind(intent: Intent) = super.onBind(intent)
}
