/**
 * ============================================================================
 * MyApp.kt - Application 入口类
 * ============================================================================
 *
 * 功能简介：
 *   Application 类，在这里一行代码初始化 Framework 模块
 *
 * 安全研究说明：
 *   - 使用独立的 framework 模块实现所有策略
 *   - 在 Application.onCreate() 中初始化，确保进程唤醒后立即启动
 *   - 所有策略可通过配置开关控制
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services

import android.app.Application
import android.os.Build
import android.util.Log
import com.service.framework.Fw
import com.service.framework.core.AggressiveLevel
import java.io.File

/**
 * Application 类
 * 核心：在这里一行代码初始化 Framework 模块
 */
class MyApp : Application() {

    companion object {
        private const val TAG = "FwApp"
    }

    override fun onCreate() {
        super.onCreate()
        val processName = currentProcessName()
        Log.d(TAG, "Application onCreate - 进程已唤醒：$processName")

        // 只在主进程初始化完整 Fw 策略，避免 :daemon/:assist 进程递归启动 WorkManager 和前台服务。
        if (processName != packageName) {
            Log.d(TAG, "当前为子进程，跳过 Fw.init：process=$processName, package=$packageName")
            return
        }

        // 全量参数初始化 Framework 模块，保证 FwConfig 每个变量都在 app 层真实赋值。
        Fw.init(this) {
            // ==================== 基础策略 ====================
            enableForegroundService = true      // 前台服务
            enableMediaSession = true           // MediaSession（核心：让系统认为是媒体应用）
            enableOnePixelActivity = true       // 1像素Activity

            // ==================== 定时唤醒策略 ====================
            enableJobScheduler = true           // JobScheduler
            jobSchedulerInterval = 15 * 60 * 1000L  // 15分钟
            enableWorkManager = true            // WorkManager
            workManagerIntervalMinutes = 15L    // 15分钟
            enableAlarmManager = true           // AlarmManager
            alarmManagerInterval = 5 * 60 * 1000L   // 5分钟

            // ==================== 账户同步策略 ====================
            enableAccountSync = true            // 账户同步
            accountType = "com.service.framework.account" // 账户类型
            syncIntervalSeconds = 60L           // 60秒

            // ==================== 广播策略 ====================
            enableSystemBroadcast = true        // 系统广播
            enableBluetoothBroadcast = true     // 蓝牙广播
            enableMediaButtonReceiver = true    // 媒体按键
            enableUsbBroadcast = true           // USB 广播
            enableNfcBroadcast = true           // NFC 广播
            enableMediaMountBroadcast = true    // 媒体挂载广播

            // ==================== 内容观察者策略 ====================
            enableMediaContentObserver = true   // 相册变化
            enableContactsContentObserver = false // 联系人变化（需要权限）
            enableSmsContentObserver = false    // 短信变化（需要权限）
            enableSettingsContentObserver = true // 设置变化
            enableFileObserver = true           // 文件观察

            // ==================== 双进程 / Native 策略 ====================
            enableDualProcess = true            // 双进程守护
            dualProcessCheckInterval = 3000L    // 双进程检查间隔
            enableNativeDaemon = true           // Native 守护进程
            nativeDaemonCheckInterval = 3000    // Native 检查间隔
            enableNativeSocket = true           // Native Socket
            nativeSocketName = "fw_native_socket" // Socket 名称
            nativeSocketHeartbeatInterval = 5000 // Socket 心跳间隔

            // ==================== 通知配置 ====================
            notificationChannelId = "fw_channel"
            notificationChannelName = "守护精灵"
            notificationTitle = "守护精灵运行中"
            notificationContent = "全量参数测试正在运行"
            notificationIconResId = R.drawable.ic_notification
            notificationActivityClass = MainActivity::class.java

            // ==================== 日志配置 ====================
            enableDebugLog = true
            logTag = "FwApp"

            // ==================== 高级侵入性策略 ====================
            enableLockScreenActivity = true     // 锁屏 Activity（测试模式全量开启）
            enableFloatWindow = true            // 悬浮窗；无权限时框架会记录跳过
            floatWindowHidden = true            // Application 默认隐藏 1 像素，避免启动即显示可见悬浮窗
            enableForceStopResistance = true    // 防强停策略（测试模式全量开启）

            // ==================== MediaRoute / 静默音频策略 ====================
            enableMediaRouteProvider = true
            enableMediaRoute2Provider = true
            enableMediaIntentActivity = true
            enableMediaBrowserService = true
            enableSilentAudio = true
            aggressiveLevel = AggressiveLevel.HIGH

            // ==================== v2.0+ 新策略 ====================
            enableVpnService = false            // VPN 需用户授权后由测试台验证
            enableCompanionDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            enableCallStyleNotification = true
            enableMediaSessionNotification = true
            enableDeviceAdmin = true
            enableTileService = true
            enableWidget = true
            enableDreamService = true
        }

        Log.d(TAG, "Framework 模块初始化完成")
    }

    /** 获取当前进程名，用于确保全量保活策略只在主进程初始化。 */
    private fun currentProcessName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        return runCatching {
            File("/proc/self/cmdline")
                .readText()
                .trim { char -> char <= ' ' || char == '\u0000' }
        }.getOrDefault(packageName)
    }
}
