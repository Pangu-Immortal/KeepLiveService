/**
 * ============================================================================
 * ForceStopReceiver.kt - 强制停止唤醒广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   接收强制停止后的唤醒广播，用于拉起服务。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.service.FwForegroundService
import com.service.framework.util.FwLog

class ForceStopReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ForceStopReceiver" // 日志标签
    }

    override fun onReceive(context: Context, intent: Intent?) {
        FwLog.d("$TAG: onReceive, action=${intent?.action}")

        // 尝试启动前台服务
        try {
            val serviceIntent = Intent(context, FwForegroundService::class.java)
            serviceIntent.putExtra("source", TAG)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            FwLog.d("$TAG: 启动前台服务成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: 启动前台服务失败", e)
        }
    }
}
