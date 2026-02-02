/**
 * ============================================================================
 * FwConfig.kt - 保活框架配置类
 * ============================================================================
 *
 * 功能简介：
 *   Fw 保活框架的配置数据类，支持精细化控制每项保活策略。
 *   使用 Builder DSL 模式进行配置。
 *
 * 配置分类：
 *   - 基础策略：前台服务、MediaSession、1像素Activity
 *   - 定时唤醒：JobScheduler、WorkManager、AlarmManager
 *   - 账户同步：账户类型、同步间隔
 *   - 广播策略：系统广播、蓝牙、媒体按键等
 *   - 内容观察者：媒体、联系人、短信、设置变化
 *   - 进程守护：双进程、Native守护、Socket通信
 *   - 通知配置：渠道、标题、图标等
 *   - 高级策略：锁屏Activity、悬浮窗
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.core

import android.app.Activity

/**
 * Fw保活框架的配置类。
 *
 * 通过此类可以精細化控制每项保活策略的开关和参数。
 * 所有 `enable` 前缀的属性默认为 `true`，除了侵入性较强的 [enableLockScreenActivity] 和 [enableFloatWindow]。
 *
 * 建议使用 [com.service.framework.Fw.init] 的 DSL 写法进行配置。
 *
 * @see com.service.framework.Fw
 * @see Builder
 */
data class FwConfig(
    // region 基础策略
    val enableForegroundService: Boolean,
    val enableMediaSession: Boolean,
    val enableOnePixelActivity: Boolean,
    // endregion

    // region 定时唤醒策略
    val enableJobScheduler: Boolean,
    val jobSchedulerInterval: Long,
    val enableWorkManager: Boolean,
    val workManagerIntervalMinutes: Long,
    val enableAlarmManager: Boolean,
    val alarmManagerInterval: Long,
    // endregion

    // region 账户同步策略
    val enableAccountSync: Boolean,
    val accountType: String,
    val syncIntervalSeconds: Long,
    // endregion

    // region 广播策略
    val enableSystemBroadcast: Boolean,
    val enableBluetoothBroadcast: Boolean,
    val enableMediaButtonReceiver: Boolean,
    val enableUsbBroadcast: Boolean,
    val enableNfcBroadcast: Boolean,
    val enableMediaMountBroadcast: Boolean,
    // endregion

    // region 内容观察者策略
    val enableMediaContentObserver: Boolean,
    val enableContactsContentObserver: Boolean,
    val enableSmsContentObserver: Boolean,
    val enableSettingsContentObserver: Boolean,
    val enableFileObserver: Boolean,
    // endregion

    // region 进程守护策略
    val enableDualProcess: Boolean,
    val dualProcessCheckInterval: Long,
    val enableNativeDaemon: Boolean,
    val nativeDaemonCheckInterval: Int,
    val enableNativeSocket: Boolean,
    val nativeSocketName: String,
    val nativeSocketHeartbeatInterval: Int,
    // endregion

    // region 通知配置
    val notificationChannelId: String,
    val notificationChannelName: String,
    val notificationTitle: String,
    val notificationContent: String,
    val notificationIconResId: Int,
    val notificationActivityClass: Class<out Activity>?,
    // endregion

    // region 日志配置
    val enableDebugLog: Boolean,
    val logTag: String,
    // endregion

    // region 高级侵入性策略
    val enableLockScreenActivity: Boolean,
    val enableFloatWindow: Boolean,
    val floatWindowHidden: Boolean,
    // endregion

    // region 无法强制停止策略
    val enableForceStopResistance: Boolean,
    // endregion

    // region MediaRoute 保活策略
    val enableMediaRouteProvider: Boolean,
    val enableMediaRoute2Provider: Boolean,
    val enableMediaIntentActivity: Boolean
    // endregion
) {

    /**
     * 用于通过 DSL 方式构建 [FwConfig] 实例的 Builder。
     *
     * 使用示例:
     * ```kotlin
     * Fw.init(this) {
     *     notificationTitle = "新的标题"
     *     enableNativeDaemon = false
     * }
     * ```
     */
    class Builder {
        // region 基础策略
        var enableForegroundService: Boolean = true
        var enableMediaSession: Boolean = true
        var enableOnePixelActivity: Boolean = true
        // endregion

        // region 定时唤醒策略
        var enableJobScheduler: Boolean = true
        var jobSchedulerInterval: Long = 15 * 60 * 1000L
        var enableWorkManager: Boolean = true
        var workManagerIntervalMinutes: Long = 15L
        var enableAlarmManager: Boolean = true
        var alarmManagerInterval: Long = 5 * 60 * 1000L
        // endregion

        // region 账户同步策略
        var enableAccountSync: Boolean = true
        var accountType: String = "com.service.framework.account"
        var syncIntervalSeconds: Long = 60L
        // endregion

        // region 广播策略
        var enableSystemBroadcast: Boolean = true
        var enableBluetoothBroadcast: Boolean = true
        var enableMediaButtonReceiver: Boolean = true
        var enableUsbBroadcast: Boolean = true
        var enableNfcBroadcast: Boolean = true
        var enableMediaMountBroadcast: Boolean = true
        // endregion

        // region 内容观察者策略
        var enableMediaContentObserver: Boolean = true
        var enableContactsContentObserver: Boolean = true
        var enableSmsContentObserver: Boolean = true
        var enableSettingsContentObserver: Boolean = true
        var enableFileObserver: Boolean = true
        // endregion

        // region 进程守护策略
        var enableDualProcess: Boolean = true
        var dualProcessCheckInterval: Long = 3000L
        var enableNativeDaemon: Boolean = true
        var nativeDaemonCheckInterval: Int = 3000
        var enableNativeSocket: Boolean = true
        var nativeSocketName: String = "fw_native_socket"
        var nativeSocketHeartbeatInterval: Int = 5000
        // endregion

        // region 通知配置
        var notificationChannelId: String = "fw_channel"
        var notificationChannelName: String = "保活服务"
        var notificationTitle: String = "服务运行中"
        var notificationContent: String = "点击打开应用"
        var notificationIconResId: Int = android.R.drawable.ic_media_play
        var notificationActivityClass: Class<out Activity>? = null
        // endregion

        // region 日志配置
        var enableDebugLog: Boolean = true
        var logTag: String = "Fw"
        // endregion

        // region 高级侵入性策略
        var enableLockScreenActivity: Boolean = false // 默认关闭
        var enableFloatWindow: Boolean = false        // 默认关闭
        var floatWindowHidden: Boolean = true
        // endregion

        // region 无法强制停止策略
        var enableForceStopResistance: Boolean = false // 默认关闭（侵入性强，仅 Android 5.0-9.0 有效）
        // endregion

        // region MediaRoute 保活策略
        var enableMediaRouteProvider: Boolean = true   // 启用 MediaRouteProviderService（默认开启）
        var enableMediaRoute2Provider: Boolean = true  // 启用 MediaRoute2ProviderService（Android 11+，默认开启）
        var enableMediaIntentActivity: Boolean = true  // 启用媒体意图处理 Activity（默认开启）
        // endregion

        /**
         * 构建一个不可变的 [FwConfig] 实例。
         */
        fun build(): FwConfig = FwConfig(
            // 基础策略
            enableForegroundService, enableMediaSession, enableOnePixelActivity,
            // 定时唤醒策略
            enableJobScheduler, jobSchedulerInterval, enableWorkManager, workManagerIntervalMinutes,
            enableAlarmManager, alarmManagerInterval,
            // 账户同步策略
            enableAccountSync, accountType, syncIntervalSeconds,
            // 广播策略
            enableSystemBroadcast, enableBluetoothBroadcast, enableMediaButtonReceiver, enableUsbBroadcast,
            enableNfcBroadcast, enableMediaMountBroadcast,
            // 内容观察者策略
            enableMediaContentObserver, enableContactsContentObserver, enableSmsContentObserver,
            enableSettingsContentObserver, enableFileObserver,
            // 进程守护策略
            enableDualProcess, dualProcessCheckInterval, enableNativeDaemon, nativeDaemonCheckInterval,
            enableNativeSocket, nativeSocketName, nativeSocketHeartbeatInterval,
            // 通知配置
            notificationChannelId, notificationChannelName, notificationTitle, notificationContent,
            notificationIconResId, notificationActivityClass,
            // 日志配置
            enableDebugLog, logTag,
            // 高级侵入性策略
            enableLockScreenActivity, enableFloatWindow, floatWindowHidden,
            // 无法强制停止策略
            enableForceStopResistance,
            // MediaRoute 保活策略
            enableMediaRouteProvider, enableMediaRoute2Provider, enableMediaIntentActivity
        )
    }
}
