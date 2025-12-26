/**
 * ============================================================================
 * BatteryOptimizationManager.kt - 电池优化与自启动权限管理
 * ============================================================================
 *
 * 功能简介：
 *   管理电池优化豁免设置和各厂商自启动权限。包含电池优化白名单申请、
 *   厂商自启动设置跳转、设备厂商识别等功能。支持小米、华为、OPPO、
 *   vivo、三星、魅族等主流厂商的自启动管理入口。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.service.framework.util.FwLog

/**
 * 电池优化豁免管理
 *
 * 安全研究要点：
 * 1. 加入电池优化白名单后，系统不会限制后台活动
 * 2. Doze 模式和 App Standby 不会影响白名单应用
 * 3. 可以请求用户授权，或者通过某些方式自动获取
 *
 * 墨迹天气等应用可能的做法：
 * 1. 用户首次启动时引导开启
 * 2. 与厂商合作，预置白名单
 * 3. 利用某些漏洞或特殊 API 自动添加（需要系统权限）
 */
object BatteryOptimizationManager {

    private const val TAG = "BatteryOptimization"

    /**
     * 检查是否已忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    /**
     * 请求忽略电池优化
     *
     * 会弹出系统对话框让用户确认
     */
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        if (isIgnoringBatteryOptimizations(context)) {
            FwLog.d("$TAG: 已在电池优化白名单中")
            return true
        }

        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            FwLog.d("$TAG: 已请求忽略电池优化")
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 请求失败 - ${e.message}", e)
            false
        }
    }

    /**
     * 打开电池优化设置页面
     *
     * 让用户手动设置
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            FwLog.d("$TAG: 打开电池优化设置")
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 打开设置失败 - ${e.message}", e)
            false
        }
    }

    /**
     * 获取状态摘要
     */
    fun getStatusSummary(context: Context): String {
        return buildString {
            appendLine("=== 电池优化状态 ===")
            appendLine("忽略电池优化: ${isIgnoringBatteryOptimizations(context)}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                appendLine("Doze 模式: ${pm?.isDeviceIdleMode}")
                appendLine("交互模式: ${pm?.isInteractive}")
            }
        }
    }
}

/**
 * 厂商自启动权限管理
 *
 * 安全研究要点：
 * 不同厂商的自启动管理入口不同
 * 这里收集了常见厂商的 Intent
 *
 * 墨迹天气可能的做法：
 * 1. 与厂商合作，被加入预置白名单
 * 2. 使用厂商特殊 API（需要合作协议）
 * 3. 利用系统漏洞自动获取（不太可能长期有效）
 */
object AutoStartPermissionManager {

    private const val TAG = "AutoStartPermission"

    /**
     * 厂商自启动设置 Intent 列表
     */
    private val AUTOSTART_INTENTS = listOf(
        // 小米
        Intent().setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ),
        // 小米（备用）
        Intent().setClassName(
            "com.miui.securitycenter",
            "com.miui.appmanager.ApplicationsDetailsActivity"
        ),
        // 华为
        Intent().setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        // 华为（备用）
        Intent().setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        ),
        // OPPO
        Intent().setClassName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ),
        // OPPO（备用）
        Intent().setClassName(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ),
        // vivo
        Intent().setClassName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ),
        // vivo（备用）
        Intent().setClassName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        ),
        // 三星
        Intent().setClassName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        ),
        // 魅族
        Intent().setClassName(
            "com.meizu.safe",
            "com.meizu.safe.security.SHOW_APPSEC"
        ),
        // 联想
        Intent().setClassName(
            "com.lenovo.security",
            "com.lenovo.security.purebackground.PureBackgroundActivity"
        ),
        // 一加
        Intent().setClassName(
            "com.oneplus.security",
            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
        ),
        // 锤子
        Intent().setClassName(
            "com.smartisanos.security",
            "com.smartisanos.security.SwitchActivity"
        ),
        // 360
        Intent().setClassName(
            "com.qihoo360.mobilesafe",
            "com.qihoo360.mobilesafe.ui.index.AppEnterActivity"
        ),
        // 乐视
        Intent().setClassName(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        ),
        // 金立
        Intent().setClassName(
            "com.gionee.softmanager",
            "com.gionee.softmanager.MainActivity"
        ),
    )

    /**
     * 打开自启动设置
     *
     * 尝试所有已知的厂商 Intent
     */
    fun openAutoStartSettings(context: Context): Boolean {
        for (intent in AUTOSTART_INTENTS) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    FwLog.d("$TAG: 打开自启动设置成功 - ${intent.component}")
                    return true
                }
            } catch (e: Exception) {
                // 继续尝试下一个
            }
        }

        // 如果都失败了，打开应用详情页
        return openAppDetailsSettings(context)
    }

    /**
     * 打开应用详情设置
     */
    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            FwLog.d("$TAG: 打开应用详情设置")
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 打开设置失败 - ${e.message}", e)
            false
        }
    }
}
