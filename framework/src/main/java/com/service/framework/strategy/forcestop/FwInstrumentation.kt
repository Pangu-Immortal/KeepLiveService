/**
 * ============================================================================
 * FwInstrumentation.kt - 保活用 Instrumentation
 * ============================================================================
 *
 * 功能简介：
 *   通过 Instrumentation 机制实现保活。
 *   当 Instrumentation 被启动时，会触发应用进程启动。
 *
 * 使用方式：
 *   需要在 AndroidManifest.xml 中声明：
 *   <instrumentation
 *       android:name="com.service.framework.strategy.forcestop.FwInstrumentation"
 *       android:targetPackage="${applicationId}"
 *       android:targetProcesses="..." />
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.service.framework.service.FwForegroundService
import com.service.framework.util.FwLog

class FwInstrumentation : Instrumentation() {

    companion object {
        private const val TAG = "FwInstrumentation" // 日志标签
    }

    /**
     * Application.onCreate 回调
     */
    override fun callApplicationOnCreate(application: Application) {
        super.callApplicationOnCreate(application)
        FwLog.d("$TAG: callApplicationOnCreate")
    }

    /**
     * Instrumentation 创建回调
     * 这是一个很好的拉活时机
     */
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        FwLog.d("$TAG: onCreate")

        // 尝试启动前台服务
        try {
            val intent = Intent(targetContext, FwForegroundService::class.java)
            intent.putExtra("source", TAG)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                targetContext.startForegroundService(intent)
            } else {
                targetContext.startService(intent)
            }

            FwLog.d("$TAG: 启动前台服务成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: 启动前台服务失败", e)
        }
    }
}
