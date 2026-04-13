/**
 * ============================================================================
 * FwCompanionService.kt - 伴随设备服务保活策略
 * ============================================================================
 *
 * 功能简介：
 *   基于 CompanionDeviceService（API 31+）实现保活策略。
 *   通过 CompanionDeviceManager 关联虚拟设备，利用系统对伴随设备
 *   服务的保护机制提升进程优先级。设备出现/消失时触发保活检查。
 *
 * 核心机制：
 *   - CompanionDeviceService 享有系统级绑定保护
 *   - 通过 CompanionDeviceManager 建立设备关联
 *   - API 33+ 支持设备存在状态观察，持续保持服务运行
 *   - 设备出现/消失回调中触发 Fw.check() 保活检查
 *
 * 主要功能：
 *   - CompanionDeviceManagerHelper.associate(): 发起设备关联
 *   - CompanionDeviceManagerHelper.startObserving(): 开始观察设备存在状态
 *   - onDeviceAppeared(): 设备出现回调
 *   - onDeviceDisappeared(): 设备消失回调
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.3.0
 */
package com.service.framework.strategy

import android.annotation.SuppressLint
import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.annotation.RequiresApi
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 伴随设备服务保活策略
 *
 * 核心机制：
 * 1. CompanionDeviceService 是 API 31+ 的系统级服务
 * 2. 系统会在关联设备出现/消失时唤醒此服务
 * 3. 享有比普通服务更高的进程优先级
 * 4. API 33+ 支持持续观察设备存在状态
 *
 * 安全研究要点：
 * - 需要先通过 CompanionDeviceManager 关联设备
 * - 关联过程需要用户确认（系统弹窗）
 * - 关联后系统会自动管理服务生命周期
 * - 适合智能手表、IoT 设备等配套应用场景
 * - 最低支持 API 31（Android 12）
 */
@RequiresApi(Build.VERSION_CODES.S)
class FwCompanionService : CompanionDeviceService() {

    companion object {
        private const val TAG = "FwCompanionService" // 日志子标签
    }

    override fun onCreate() {
        super.onCreate()
        FwLog.d("$TAG: onCreate - 伴随设备服务初始化")
        // 服务连接时拉起前台服务
        ServiceStarter.startForegroundService(this, "伴随设备服务连接")
    }

    /**
     * 伴随设备出现回调
     * 当关联的设备被系统检测到时触发
     * @param associationInfo 关联信息
     */
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        FwLog.d("$TAG: onDeviceAppeared - 伴随设备出现，关联ID: ${associationInfo.id}")
        // 触发保活检查
        try {
            Fw.check() // 手动触发保活检查
            FwLog.d("$TAG: 已触发 Fw.check() 保活检查")
        } catch (e: Exception) {
            FwLog.e("$TAG: 触发保活检查失败: ${e.message}", e)
        }
    }

    /**
     * 伴随设备消失回调
     * 当关联的设备不再被系统检测到时触发
     * @param associationInfo 关联信息
     */
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        super.onDeviceDisappeared(associationInfo)
        FwLog.d("$TAG: onDeviceDisappeared - 伴随设备消失，关联ID: ${associationInfo.id}")
        // 触发保活检查
        try {
            Fw.check() // 手动触发保活检查
            FwLog.d("$TAG: 已触发 Fw.check() 保活检查")
        } catch (e: Exception) {
            FwLog.e("$TAG: 触发保活检查失败: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.w("$TAG: onDestroy - 伴随设备服务被销毁")
    }
}

/**
 * CompanionDeviceManager 辅助工具类
 *
 * 封装 CompanionDeviceManager 的关联和观察功能，
 * 提供简洁的 API 用于发起设备关联和设备存在状态观察。
 *
 * 主要功能：
 *   - associate(): 发起设备关联请求（需要 Activity 上下文）
 *   - startObserving(): 开始观察已关联设备的存在状态（API 33+）
 *   - getAssociations(): 获取已关联设备列表
 *   - isSupported(): 检查当前系统是否支持
 */
object CompanionDeviceManagerHelper {

    private const val TAG = "CompanionHelper" // 日志子标签
    private const val REQUEST_CODE_ASSOCIATE = 20001 // 关联请求码

    /**
     * 检查当前系统是否支持 CompanionDeviceService
     * @return API 31+ 返回 true
     */
    fun isSupported(): Boolean {
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // API 31+
        FwLog.d("$TAG: 伴随设备服务支持状态: $supported (API=${Build.VERSION.SDK_INT})")
        return supported
    }

    /**
     * 发起设备关联请求
     * 系统会弹出选择器让用户确认关联设备
     *
     * @param activity Activity 上下文（需要用于 startIntentSenderForResult）
     */
    @SuppressLint("NewApi")
    fun associate(activity: Activity) {
        // 检查 API 版本
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            FwLog.w("$TAG: 当前系统版本低于 API 31，不支持 CompanionDeviceManager 关联")
            return
        }

        FwLog.d("$TAG: 开始发起设备关联请求...")
        try {
            val deviceManager = activity.getSystemService(Context.COMPANION_DEVICE_SERVICE)
                as? CompanionDeviceManager // 获取 CompanionDeviceManager 系统服务
            if (deviceManager == null) {
                FwLog.e("$TAG: 无法获取 CompanionDeviceManager 系统服务")
                return
            }

            // 构建关联请求
            val associationRequest = AssociationRequest.Builder()
                .setSingleDevice(false) // 不限定单个设备，显示所有可关联设备
                .build()
            FwLog.d("$TAG: 关联请求已构建，setSingleDevice=false")

            // 发起关联
            deviceManager.associate(
                associationRequest,
                object : CompanionDeviceManager.Callback() {
                    /**
                     * 设备关联选择器就绪回调
                     * @param chooserLauncher 用于启动选择器的 IntentSender
                     */
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        FwLog.d("$TAG: 设备关联选择器就绪，启动选择界面")
                        try {
                            activity.startIntentSenderForResult(
                                chooserLauncher,    // 选择器意图发送者
                                REQUEST_CODE_ASSOCIATE, // 请求码
                                null,               // 填充意图
                                0, 0, 0             // 标志位
                            )
                        } catch (e: Exception) {
                            FwLog.e("$TAG: 启动设备关联选择器失败: ${e.message}", e)
                        }
                    }

                    /**
                     * 关联失败回调
                     * @param error 错误信息
                     */
                    override fun onFailure(error: CharSequence?) {
                        FwLog.e("$TAG: 设备关联失败: $error")
                    }
                },
                null // 在主线程回调
            )
        } catch (e: Exception) {
            FwLog.e("$TAG: 发起设备关联异常: ${e.message}", e)
        }
    }

    /**
     * 开始观察已关联设备的存在状态（API 33+）
     * 当设备出现或消失时，系统会回调 FwCompanionService
     *
     * @param context 上下文
     */
    @SuppressLint("NewApi")
    fun startObserving(context: Context) {
        // API 33+ 才支持 startObservingDevicePresence
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            FwLog.w("$TAG: 当前系统版本低于 API 33，不支持设备存在状态观察")
            return
        }

        FwLog.d("$TAG: 开始观察已关联设备的存在状态...")
        try {
            val deviceManager = context.getSystemService(Context.COMPANION_DEVICE_SERVICE)
                as? CompanionDeviceManager // 获取 CompanionDeviceManager 系统服务
            if (deviceManager == null) {
                FwLog.e("$TAG: 无法获取 CompanionDeviceManager 系统服务")
                return
            }

            // 获取已关联设备列表
            val associations = deviceManager.myAssociations // 获取当前应用的所有关联
            if (associations.isEmpty()) {
                FwLog.w("$TAG: 没有已关联的设备，跳过存在状态观察")
                return
            }

            FwLog.d("$TAG: 找到 ${associations.size} 个已关联设备")

            // 对每个已关联设备开始观察存在状态
            for (association in associations) {
                try {
                    val associationId = association.id // 获取关联 ID
                    deviceManager.startObservingDevicePresence(association.deviceMacAddress.toString())
                    FwLog.d("$TAG: 已开始观察设备存在状态，关联ID: $associationId")
                } catch (e: Exception) {
                    FwLog.e("$TAG: 观察设备存在状态失败: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            FwLog.e("$TAG: 启动设备存在状态观察异常: ${e.message}", e)
        }
    }

    /**
     * 获取已关联设备列表
     * @param context 上下文
     * @return 已关联设备数量
     */
    @SuppressLint("NewApi")
    fun getAssociationCount(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return 0
        }

        return try {
            val deviceManager = context.getSystemService(Context.COMPANION_DEVICE_SERVICE)
                as? CompanionDeviceManager
            val count = deviceManager?.myAssociations?.size ?: 0 // 获取关联数量
            FwLog.d("$TAG: 已关联设备数量: $count")
            count
        } catch (e: Exception) {
            FwLog.e("$TAG: 获取已关联设备列表失败: ${e.message}", e)
            0
        }
    }
}
