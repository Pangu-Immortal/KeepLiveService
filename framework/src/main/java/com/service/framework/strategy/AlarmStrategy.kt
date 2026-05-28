/**
 * ============================================================================
 * AlarmStrategy.kt - AlarmManager 保活策略
 * ============================================================================
 *
 * 功能简介：
 *   使用精确闹钟定时触发，在闹钟触发时拉起服务。
 *
 * 核心机制：
 *   - 使用精确闹钟定时触发
 *   - Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限
 *   - 闹钟触发后自动重新调度下一次
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.service.framework.Fw
import com.service.framework.health.FwStrategyKey
import com.service.framework.health.FwStrategyStateManager
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * AlarmManager 保活策略
 *
 * 核心机制：
 * 1. 使用精确闹钟定时触发
 * 2. 闹钟触发时拉起服务
 * 3. 支持精确闹钟和非精确闹钟
 *
 * 安全研究要点：
 * - Android 12+ 限制了精确闹钟权限
 * - 需要 SCHEDULE_EXACT_ALARM 或 USE_EXACT_ALARM 权限
 * - 强制停止后闹钟会被取消
 * - 但可以通过 setPersisted 在设备重启后保留
 */
object AlarmStrategy {

    private const val REQUEST_CODE = 10001
    private const val ACTION_ALARM_WAKEUP = "com.service.framework.ALARM_WAKEUP"

    /**
     * 调度闹钟
     */
    fun schedule(context: Context) {
        val config = Fw.config ?: return
        if (!config.enableAlarmManager) return

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_WAKEUP
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val triggerTime = SystemClock.elapsedRealtime() + config.alarmManagerInterval

            when {
                // Android 12+ 需要检查精确闹钟权限
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        FwStrategyStateManager.markStarted(FwStrategyKey.ALARM_MANAGER, "精确闹钟")
                        FwLog.d("AlarmManager 设置精确闹钟成功 (Android 12+)")
                    } else {
                        // 没有精确闹钟权限，使用非精确闹钟
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        FwStrategyStateManager.markStarted(FwStrategyKey.ALARM_MANAGER, "非精确闹钟")
                        FwLog.d("AlarmManager 设置非精确闹钟 (无权限)")
                    }
                }
                // Android 6.0+ 使用 setExactAndAllowWhileIdle
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    FwStrategyStateManager.markStarted(FwStrategyKey.ALARM_MANAGER, "setExactAndAllowWhileIdle")
                    FwLog.d("AlarmManager 设置精确闹钟成功 (Android 6.0+)")
                }
                // 更低版本使用 setExact
                else -> {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    FwStrategyStateManager.markStarted(FwStrategyKey.ALARM_MANAGER, "setExact")
                    FwLog.d("AlarmManager 设置精确闹钟成功")
                }
            }
        } catch (e: Exception) {
            FwStrategyStateManager.markError(FwStrategyKey.ALARM_MANAGER, e.message ?: "调度失败", e)
            FwLog.e("AlarmManager 调度失败: ${e.message}", e)
        }
    }

    /**
     * 取消闹钟
     */
    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_WAKEUP
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            FwStrategyStateManager.markStopped(FwStrategyKey.ALARM_MANAGER, "取消闹钟")
            FwLog.d("AlarmManager 已取消")
        } catch (e: Exception) {
            FwStrategyStateManager.markError(FwStrategyKey.ALARM_MANAGER, e.message ?: "取消失败", e)
            FwLog.e("取消 AlarmManager 失败: ${e.message}", e)
        }
    }

    /**
     * 检查闹钟 PendingIntent 是否仍存在。
     */
    fun isScheduled(context: Context): Boolean {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_WAKEUP
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
            )
            pendingIntent != null
        } catch (e: Exception) {
            FwLog.e("检查 AlarmManager 状态失败: ${e.message}", e)
            false
        }
    }

    /**
     * 检查当前是否具备精确闹钟能力。
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                true
            } else {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() == true
            }
        } catch (e: Exception) {
            FwLog.e("检查精确闹钟权限失败: ${e.message}", e)
            false
        }
    }

    /**
     * 打开精确闹钟授权页面。
     */
    fun openExactAlarmSettings(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return true
            }
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            FwLog.d("AlarmManager 已打开精确闹钟授权页面")
            true
        } catch (e: Exception) {
            FwStrategyStateManager.markError(FwStrategyKey.ALARM_MANAGER, e.message ?: "打开授权页失败", e)
            FwLog.e("打开精确闹钟授权页面失败: ${e.message}", e)
            false
        }
    }
}

/**
 * 闹钟广播接收器
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        FwLog.d("AlarmReceiver 收到广播: ${intent?.action}")
        FwStrategyStateManager.markTriggered(FwStrategyKey.ALARM_MANAGER, intent?.action ?: "未知闹钟")

        // 拉起服务
        ServiceStarter.startForegroundService(context, "AlarmManager唤醒")

        // 重新调度下一次闹钟
        AlarmStrategy.schedule(context)
    }
}
