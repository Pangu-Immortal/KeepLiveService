/**
 * ============================================================================
 * FwAccessibilityService.kt - 无障碍服务保活策略
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android 无障碍服务的保活策略。无障碍服务是系统级服务，
 *   具有极高的进程优先级，系统会尽量保持其存活。提供服务状态检测、
 *   设置页面跳转、服务连接管理等功能。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 无障碍服务保活策略
 *
 * 核心机制：
 * 1. 无障碍服务是系统级服务，优先级极高
 * 2. 系统会尽量保持无障碍服务存活
 * 3. 即使被杀也会被系统自动重启
 *
 * 安全研究要点：
 * - 无障碍服务需要用户在设置中手动开启
 * - 系统对无障碍服务有特殊保护
 * - 是目前最有效的保活方式之一
 * - 但滥用会被 Google Play 下架
 *
 * 注意：
 * - 需要在 AndroidManifest 中声明
 * - 需要提供 accessibility_service_config.xml
 * - 用户必须手动在设置中开启
 * - 强制停止后服务会被禁用
 */
class FwAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: FwAccessibilityService? = null

        /**
         * 检查无障碍服务是否开启
         */
        fun isEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${FwAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.contains(serviceName)
        }

        /**
         * 跳转到无障碍设置页面
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                FwLog.e("打开无障碍设置失败: ${e.message}", e)
            }
        }

        /**
         * 获取实例
         */
        fun getInstance(): FwAccessibilityService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FwLog.d("FwAccessibilityService: onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FwLog.d("FwAccessibilityService: 服务已连接")

        // 配置服务
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        }
        serviceInfo = info

        // 拉起保活服务
        ServiceStarter.startForegroundService(this, "无障碍服务连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可以监听系统事件，但这里只用于保活
        // 不做任何实际处理
    }

    override fun onInterrupt() {
        FwLog.d("FwAccessibilityService: onInterrupt")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        FwLog.d("FwAccessibilityService: onDestroy，尝试重启")

        // 尝试重启保活服务
        ServiceStarter.startForegroundService(this, "无障碍服务销毁")
    }
}
