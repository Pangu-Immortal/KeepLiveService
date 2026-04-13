/**
 * ============================================================================
 * FwDeviceAdminReceiver.kt - 设备管理员保活策略
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android 设备管理员（DeviceAdmin）的保活策略。设备管理员拥有
 *   系统级别的特殊权限，一旦激活后应用无法被普通方式卸载或强制停止，
 *   从而大幅提升进程存活率。
 *
 * 主要功能：
 *   - onEnabled(): 设备管理员激活时拉起保活服务
 *   - onDisabled(): 设备管理员停用时记录日志
 *   - onDisableRequested(): 用户尝试停用时弹出警告提示
 *   - isAdminActive(): 检查设备管理员是否已激活
 *   - requestEnable(): 请求用户启用设备管理员
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 设备管理员广播接收器
 *
 * 核心机制：
 * 1. 设备管理员是系统级权限，激活后应用受到系统保护
 * 2. 用户无法直接卸载已激活设备管理员的应用
 * 3. 系统会尽量保持设备管理员进程存活
 *
 * 安全研究要点：
 * - 需要用户手动确认授权
 * - 停用前必须先取消设备管理员
 * - 适合企业设备管理场景
 * - 滥用会被 Google Play 下架
 *
 * 注意：
 * - 需要在 AndroidManifest 中声明 receiver 并关联 device_admin.xml
 * - 需要 BIND_DEVICE_ADMIN 权限
 */
class FwDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        /** 请求启用设备管理员的请求码 */
        private const val REQUEST_CODE_ENABLE_ADMIN = 20001

        /**
         * 检查设备管理员是否已激活
         *
         * @param context 上下文
         * @return 设备管理员是否处于激活状态
         */
        fun isAdminActive(context: Context): Boolean {
            return try {
                // 获取设备策略管理器
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                        as? DevicePolicyManager ?: return false
                // 构建当前组件名
                val componentName = ComponentName(context, FwDeviceAdminReceiver::class.java)
                // 检查是否已激活
                val active = dpm.isAdminActive(componentName)
                FwLog.d("设备管理员激活状态: $active")
                active
            } catch (e: Exception) {
                FwLog.e("检查设备管理员状态失败: ${e.message}", e)
                false
            }
        }

        /**
         * 请求用户启用设备管理员
         *
         * @param context Activity 上下文，用于 startActivityForResult
         */
        fun requestEnable(context: Activity) {
            try {
                // 构建当前组件名
                val componentName = ComponentName(context, FwDeviceAdminReceiver::class.java)
                // 创建启用设备管理员的 Intent
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    // 设置要激活的设备管理员组件
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    // 设置提示信息
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "启用设备管理员可增强保活能力，防止应用被系统强制停止"
                    )
                }
                // 启动系统授权界面
                context.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
                FwLog.d("已发起设备管理员启用请求")
            } catch (e: Exception) {
                FwLog.e("请求启用设备管理员失败: ${e.message}", e)
            }
        }
    }

    /**
     * 设备管理员被激活时回调
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        FwLog.d("FwDeviceAdminReceiver: 设备管理员已启用")
        // 拉起前台保活服务
        ServiceStarter.startForegroundService(context, "设备管理员已启用")
    }

    /**
     * 设备管理员被停用时回调
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        FwLog.d("FwDeviceAdminReceiver: 设备管理员已停用")
    }

    /**
     * 用户请求停用设备管理员时回调
     *
     * @return 提示用户停用后果的警告字符串
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        FwLog.d("FwDeviceAdminReceiver: 用户请求停用设备管理员")
        // 返回警告信息，系统会在确认对话框中显示
        return "停用设备管理员将减弱保活能力"
    }
}
