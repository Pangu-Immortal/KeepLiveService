/**
 * ============================================================================
 * ServiceStarter.kt - 服务启动工具类
 * ============================================================================
 *
 * 功能简介：
 *   封装服务启动逻辑，兼容不同 Android 版本。
 *
 * 主要功能：
 *   - startForegroundService(): 启动前台服务
 *   - startServiceCompat(): 兼容不同版本启动服务
 *   - stopForegroundService(): 停止前台服务
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.service.FwForegroundService

/**
 * 服务启动工具类
 */
object ServiceStarter {

    /**
     * 启动前台服务
     */
    fun startForegroundService(context: Context, reason: String) {
        val intent = Intent(context, FwForegroundService::class.java).apply {
            putExtra(FwForegroundService.EXTRA_START_REASON, reason)
        }
        startServiceCompat(context, intent)
    }

    /**
     * 兼容不同版本启动服务
     */
    fun startServiceCompat(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
                FwLog.d("通过 startForegroundService 启动服务")
            } else {
                context.startService(intent)
                FwLog.d("通过 startService 启动服务")
            }
        } catch (e: Exception) {
            FwLog.e("启动服务失败: ${e.message}", e)
        }
    }

    /**
     * 停止服务
     */
    fun stopForegroundService(context: Context) {
        try {
            val intent = Intent(context, FwForegroundService::class.java)
            context.stopService(intent)
            FwLog.d("停止前台服务")
        } catch (e: Exception) {
            FwLog.e("停止服务失败: ${e.message}", e)
        }
    }
}
