/**
 * ============================================================================
 * FwNotificationListenerService.kt - 通知监听服务保活策略
 * ============================================================================
 *
 * 功能简介：
 *   实现基于通知监听服务的保活策略。NotificationListenerService 是系统级
 *   服务，具有较高的进程优先级。提供通知监听权限检测、设置页面跳转、
 *   服务重绑定请求、通知事件处理等功能。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 通知监听服务保活策略
 *
 * 核心机制：
 * 1. NotificationListenerService 是系统级服务
 * 2. 需要用户授权后才能运行
 * 3. 系统会尽量保持服务存活
 * 4. 可以监听所有应用的通知
 *
 * 安全研究要点：
 * - 类似无障碍服务，优先级很高
 * - 需要用户手动在设置中开启
 * - 可以作为保活的辅助手段
 * - 有通知到达时会被唤醒
 *
 * 使用场景：
 * - 通知管理应用
 * - 通知同步应用
 * - 智能手表配套应用
 */
class FwNotificationListenerService : NotificationListenerService() {

    companion object {
        private var instance: FwNotificationListenerService? = null

        /**
         * 检查通知监听权限是否开启
         */
        fun isEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            return flat.contains(packageName)
        }

        /**
         * 跳转到通知监听设置页面
         */
        fun openNotificationListenerSettings(context: Context) {
            try {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                } else {
                    android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                }
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                FwLog.e("打开通知监听设置失败: ${e.message}", e)
            }
        }

        /**
         * 请求重新绑定服务
         */
        fun requestRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val componentName = ComponentName(
                        context,
                        FwNotificationListenerService::class.java
                    )
                    requestRebind(componentName)
                    FwLog.d("请求重新绑定通知监听服务")
                } catch (e: Exception) {
                    FwLog.e("请求重新绑定失败: ${e.message}", e)
                }
            }
        }

        /**
         * 获取实例
         */
        fun getInstance(): FwNotificationListenerService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FwLog.d("FwNotificationListenerService: onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        FwLog.d("FwNotificationListenerService: 监听器已连接")

        // 拉起保活服务
        ServiceStarter.startForegroundService(this, "通知监听服务连接")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // 有新通知时触发
        // 可以利用此机会检查保活状态
        sbn?.let {
            FwLog.d("FwNotificationListenerService: 收到通知 - ${it.packageName}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // 通知被移除时触发
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        FwLog.d("FwNotificationListenerService: 监听器断开连接")

        // 尝试请求重新绑定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, FwNotificationListenerService::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        FwLog.d("FwNotificationListenerService: onDestroy")
    }
}
