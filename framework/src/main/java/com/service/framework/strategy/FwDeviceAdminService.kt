/**
 * ============================================================================
 * FwDeviceAdminService.kt - 设备管理员服务（API 26+）
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android DeviceAdminService 的保活策略。当应用被设置为设备所有者
 *   （Device Owner）时，系统会自动绑定此服务，使其获得极高的进程优先级。
 *   此服务由系统自动管理，无需手动启动。
 *
 * 主要功能：
 *   - onCreate(): 服务创建时拉起保活服务
 *   - onConnected(): 系统绑定连接时记录日志
 *   - onDisconnected(): 系统解绑时记录日志
 *   - onDestroy(): 服务销毁时记录日志
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.admin.DeviceAdminService
import android.os.Build
import androidx.annotation.RequiresApi
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 设备管理员服务（API 26+）
 *
 * 核心机制：
 * 1. 当应用为 Device Owner 时，系统自动绑定此服务
 * 2. 绑定后进程优先级极高，几乎不会被杀死
 * 3. 无需手动启动，完全由系统管理生命周期
 *
 * 安全研究要点：
 * - 仅当应用通过 adb 或 NFC provisioning 设为 Device Owner 时生效
 * - 企业设备管理（MDM）场景下常用
 * - 普通应用很难成为 Device Owner
 * - 但一旦激活，保活效果极佳
 *
 * 注意：
 * - 需要 API 26+（Android 8.0）
 * - 需要在 AndroidManifest 中声明
 * - 需要 BIND_DEVICE_ADMIN 权限
 * - 应用必须是 Device Owner 才会被系统自动绑定
 */
@RequiresApi(Build.VERSION_CODES.O)
class FwDeviceAdminService : DeviceAdminService() {

    override fun onCreate() {
        super.onCreate()
        FwLog.d("FwDeviceAdminService: onCreate，系统已绑定设备管理员服务")
        // 拉起前台保活服务
        ServiceStarter.startForegroundService(this, "设备管理员服务创建")
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        FwLog.d("FwDeviceAdminService: onStartCommand，设备管理员服务收到启动命令")
        ServiceStarter.startForegroundService(this, "设备管理员服务启动")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("FwDeviceAdminService: onDestroy，设备管理员服务已销毁")
    }
}
