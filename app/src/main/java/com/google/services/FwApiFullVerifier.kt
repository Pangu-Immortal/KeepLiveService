package com.google.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.service.framework.Fw
import com.service.framework.core.AggressiveLevel
import com.service.framework.core.FwConfig
import com.service.framework.health.FwStrategyKey
import com.service.framework.health.FwStrategyStateManager
import com.service.framework.start.FwStartResult
import com.service.framework.start.FwStartStrategy
import com.service.framework.strategy.AutoStartPermissionManager
import com.service.framework.strategy.BatteryOptimizationManager
import com.service.framework.strategy.CompanionDeviceManagerHelper
import com.service.framework.strategy.FloatWindowManager
import com.service.framework.strategy.FwCallStyleManager
import com.service.framework.strategy.FwVpnService
import com.service.framework.strategy.LockScreenActivity
import com.service.framework.strategy.MediaSessionNotificationManager
import com.service.framework.strategy.VendorIntegrationAnalyzer

/**
 * FW 全量 API 验证器。
 *
 * 功能：集中调用 app 可访问的 Fw/FwStart/FwConfig/策略工具公开函数，
 * 通过日志证明变量、枚举、结果模型和函数入口均真实可调用。
 */
object FwApiFullVerifier {

    private const val TAG = "FwApiVerifier"

    /** 执行全量 API 验证，返回可展示在 UI 上的摘要。 */
    fun verify(activity: Activity): String {
        val context = activity.applicationContext
        val lines = mutableListOf<String>()
        fun record(message: String) {
            lines += message
            Log.d(TAG, message)
        }

        record("================ FW 全量 API 验证开始 ================")
        record("设备：API=${Build.VERSION.SDK_INT}, manufacturer=${Build.MANUFACTURER}, model=${Build.MODEL}")

        runCatching {
            val fullConfig = buildFullConfig(activity)
            record("FwConfig 全量构建成功：${fullConfig.javaClass.declaredFields.size} 个字段")
            fullConfig.javaClass.declaredFields.forEach { field ->
                field.isAccessible = true
                record("FwConfig.${field.name}=${field.get(fullConfig)}")
            }
        }.onFailure { throwable ->
            record("FwConfig 全量构建失败：${throwable.message}")
        }

        runCatching {
            record("Fw.isInitialized()=${Fw.isInitialized()}")
            val report = Fw.check()
            record("Fw.check() 成功：healthy=${report.healthyCount}, issue=${report.issueCount}, killRisk=${report.killRisk}")
            record(report.toLogString())
        }.onFailure { throwable ->
            record("Fw.check()/状态读取失败：${throwable.message}")
        }

        verifyStartModels(::record)
        verifyStartFunctions(activity, ::record)
        verifyStrategyUtilities(activity, context, ::record)
        record("================ FW 全量 API 验证结束 ================")
        return lines.takeLast(18).joinToString(separator = "\n")
    }

    /** 构建全量参数配置，覆盖 Builder 中每一个公开变量。 */
    fun buildFullConfig(activity: Activity): FwConfig {
        return FwConfig.Builder().apply {
            // 基础策略全量赋值。
            enableForegroundService = true
            enableMediaSession = true
            enableOnePixelActivity = true

            // 定时唤醒策略全量赋值。
            enableJobScheduler = true
            jobSchedulerInterval = 15 * 60 * 1000L
            enableWorkManager = true
            workManagerIntervalMinutes = 15L
            enableAlarmManager = true
            alarmManagerInterval = 5 * 60 * 1000L

            // 账户同步策略全量赋值。
            enableAccountSync = true
            accountType = "com.service.framework.account"
            syncIntervalSeconds = 60L

            // 广播策略全量赋值。
            enableSystemBroadcast = true
            enableBluetoothBroadcast = true
            enableMediaButtonReceiver = true
            enableUsbBroadcast = true
            enableNfcBroadcast = true
            enableMediaMountBroadcast = true

            // 内容观察者策略全量赋值；敏感联系人/短信默认保持关闭，避免无授权直接触发。
            enableMediaContentObserver = true
            enableContactsContentObserver = false
            enableSmsContentObserver = false
            enableSettingsContentObserver = true
            enableFileObserver = true

            // 进程守护策略全量赋值。
            enableDualProcess = true
            dualProcessCheckInterval = 3000L
            enableNativeDaemon = true
            nativeDaemonCheckInterval = 3000
            enableNativeSocket = true
            nativeSocketName = "fw_native_socket"
            nativeSocketHeartbeatInterval = 5000

            // 通知配置全量赋值。
            notificationChannelId = "fw_channel"
            notificationChannelName = "守护精灵"
            notificationTitle = "守护精灵运行中"
            notificationContent = "全量参数测试正在运行"
            notificationIconResId = R.drawable.ic_notification
            notificationActivityClass = MainActivity::class.java

            // 日志配置全量赋值。
            enableDebugLog = true
            logTag = "FwApp"

            // 高级侵入性策略全量赋值；需要授权的能力由系统权限决定真实执行结果。
            enableLockScreenActivity = true
            enableFloatWindow = true
            floatWindowHidden = false
            enableForceStopResistance = true

            // MediaRoute / 音频 / 新增策略全量赋值。
            enableMediaRouteProvider = true
            enableMediaRoute2Provider = true
            enableMediaIntentActivity = true
            enableMediaBrowserService = true
            enableSilentAudio = true
            aggressiveLevel = AggressiveLevel.HIGH
            enableVpnService = FwVpnService.isPrepared(activity)
            enableCompanionDevice = CompanionDeviceManagerHelper.isSupported()
            enableCallStyleNotification = true
            enableMediaSessionNotification = true
            enableDeviceAdmin = true
            enableTileService = true
            enableWidget = true
            enableDreamService = true
        }.build()
    }

    /** 验证 FwStartStrategy 与 FwStartResult 的变量和转换函数。 */
    private fun verifyStartModels(record: (String) -> Unit) {
        record("FwStartStrategy.defaultExecutableMask=${FwStartStrategy.defaultExecutableMask}")
        record("FwStartStrategy.allMask=${FwStartStrategy.allMask}")
        FwStartStrategy.entries.forEach { strategy ->
            record(
                "Strategy.${strategy.name}: mask=${strategy.mask}, display=${strategy.displayName}, " +
                    "min=${strategy.minSdk}, max=${strategy.maxSdk}, executable=${strategy.executable}"
            )
            record("Strategy.fromMask(${strategy.mask})=${FwStartStrategy.fromMask(strategy.mask)?.name}")
        }
        val resultCodes = listOf(
            FwStartResult.INVALID_ARGUMENT,
            FwStartResult.JNI_EXCEPTION,
            FwStartResult.UNSUPPORTED_SDK,
            FwStartResult.SKIPPED_BY_POLICY,
            FwStartResult.REQUIRES_PRIVILEGE,
            FwStartResult.NOT_ACTIVITY_CONTEXT,
            FwStartResult.SYSTEM_API_BLOCKED,
            FwStartResult.NO_STRATEGY_SUCCEEDED
        )
        resultCodes.forEach { code ->
            record("FwStartResult.fromNativeCode($code)=${FwStartResult.fromNativeCode(code).message}")
        }
    }

    /** 验证 FwStart 每个公开函数入口。 */
    private fun verifyStartFunctions(activity: Activity, record: (String) -> Unit) {
        runCatching {
            val defaultResult = ExternalActivityLaunchTester.startDefaultOnce(activity)
            record("FwStart.start() 验证结果：${defaultResult.message}")
        }.onFailure { throwable ->
            record("FwStart.start() 验证异常：${throwable.message}")
        }
        runCatching {
            val startResults = ExternalActivityLaunchTester.verifyAllStartEntrypoints(activity, "api-verifier")
            record("FwStart.startAuditAll/startWithStrategies/startWithMask 验证数量：${startResults.size}")
            startResults.forEachIndexed { index, result ->
                record("FwStart 入口[$index]：${result.message}")
            }
        }.onFailure { throwable ->
            record("FwStart 全量入口验证异常：${throwable.message}")
        }
    }

    /** 验证策略工具类公开函数，所有高权限能力只记录系统真实结果。 */
    private fun verifyStrategyUtilities(activity: Activity, context: Context, record: (String) -> Unit) {
        record("BatteryOptimization.isIgnoring=${BatteryOptimizationManager.isIgnoringBatteryOptimizations(context)}")
        record("BatteryOptimization.status=${BatteryOptimizationManager.getStatusSummary(context).replace('\n', '|')}")
        record("FloatWindow.canDraw=${FloatWindowManager.canDrawOverlays(context)}")
        record("FloatWindow.isShowing(before)=${FloatWindowManager.isShowing()}")
        if (FloatWindowManager.canDrawOverlays(context)) {
            val floatResult = FloatWindowManager.showVisibleFloat(context, "这个是体外 Activity")
            record("FloatWindow.showVisible=$floatResult")
            record("FloatWindow.isShowing(after)=${FloatWindowManager.isShowing()}")
            FloatWindowManager.hide()
            record("FloatWindow.hide() 已调用，isShowing=${FloatWindowManager.isShowing()}")
        }
        record("Vpn.isPrepared=${FwVpnService.isPrepared(context)}")
        record("Vpn.prepareIntent=${FwVpnService.prepareIntent(context)}")
        record("Vpn.isRunning=${FwVpnService.isRunning()}")
        record("MediaSessionNotification.permission=${MediaSessionNotificationManager.isNotificationPermissionGranted(context)}")
        record("CallStyle.isShowing(before)=${FwCallStyleManager.isShowing()}")
        runCatching { FwCallStyleManager.show(context) }
            .onSuccess { record("CallStyle.show() 已调用") }
            .onFailure { record("CallStyle.show() 异常：${it.message}") }
        record("CallStyle.isShowing(after)=${FwCallStyleManager.isShowing()}")
        FwCallStyleManager.dismiss(context)
        record("CallStyle.dismiss() 已调用，isShowing=${FwCallStyleManager.isShowing()}")
        record("Companion.isSupported=${CompanionDeviceManagerHelper.isSupported()}")
        record("Companion.associationCount=${CompanionDeviceManagerHelper.getAssociationCount(context)}")
        record("LockScreen.isKeyguardLocked=${LockScreenActivity.isKeyguardLocked(context)}")
        FwStrategyKey.entries.forEach { key ->
            val state = FwStrategyStateManager.snapshot(key)
            record("FwState.${key.name}: status=${state.status}, trigger=${state.triggerCount}, error=${state.errorCount}")
        }
        runCatching { VendorIntegrationAnalyzer.getFullAnalysisReport(context, context.packageName) }
            .onSuccess { report -> record("VendorIntegration.report.length=${report.length}") }
            .onFailure { throwable -> record("VendorIntegration.report 异常：${throwable.message}") }
        record("BatteryOptimization.openSettings 由独立权限按钮触发，验证器不强制跳系统设置页")
        record("AutoStart.openAutoStartSettings 可由 UI 按钮触发，避免验证器强制跳设置页")
        record("Companion.associate 可由 UI 按钮触发，避免验证器强制弹系统选择器")
        record("Settings.canDrawOverlays=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true}")
        record("Activity for high-permission APIs=${activity::class.java.name}")
    }
}
