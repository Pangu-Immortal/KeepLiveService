/**
 * ============================================================================
 * FwHealthMonitor.kt - 保活框架统一健康巡检器
 * ============================================================================
 *
 * 功能简介：
 *   对所有已配置保活策略执行统一巡检，输出进程优先级、OOM、权限状态、
 *   策略运行状态和补偿结果。该巡检器是 Fw.check() 的核心实现。
 *
 * 主要函数：
 *   - check(): 执行全量巡检
 *   - lastReport(): 读取最近一次巡检报告
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.health

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.service.framework.account.FwSyncAdapter
import com.service.framework.core.FwConfig
import com.service.framework.mediaroute.FwMediaRouteManager
import com.service.framework.native.FwNative
import com.service.framework.observer.ContentObserverManager
import com.service.framework.observer.FileObserverManager
import com.service.framework.rust.FwRustNative
import com.service.framework.service.FwForegroundService
import com.service.framework.strategy.AlarmStrategy
import com.service.framework.strategy.BatteryOptimizationManager
import com.service.framework.strategy.CompanionDeviceManagerHelper
import com.service.framework.strategy.FwAccessibilityService
import com.service.framework.strategy.FwCallStyleManager
import com.service.framework.strategy.FwDeviceAdminReceiver
import com.service.framework.strategy.FwDreamService
import com.service.framework.strategy.FwJobService
import com.service.framework.strategy.FwMediaBrowserService
import com.service.framework.strategy.FwNotificationListenerService
import com.service.framework.strategy.FwVpnService
import com.service.framework.strategy.FwWidgetProvider
import com.service.framework.strategy.FwWorker
import com.service.framework.strategy.MediaSessionNotificationManager
import com.service.framework.strategy.ProcessPriorityManager
import com.service.framework.strategy.SilentAudioStrategy
import com.service.framework.strategy.VendorIntegrationAnalyzer
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 统一健康巡检器。
 */
object FwHealthMonitor {

    @Volatile
    private var latestReport: FwHealthReport? = null

    /**
     * 执行全量健康巡检。
     */
    fun check(context: Context, config: FwConfig, autoRecover: Boolean = true): FwHealthReport {
        val appContext = context.applicationContext
        val processImportance = ProcessPriorityManager.getCurrentImportance(appContext)
        val oomAdj = ProcessPriorityManager.getOomAdj()
        val killRisk = ProcessPriorityManager.checkKillRisk(appContext).name
        val results = mutableListOf<FwStrategyHealth>()

        results += checkForegroundService(appContext, config, autoRecover)
        results += checkSilentAudio(config)
        results += checkJobScheduler(appContext, config, autoRecover)
        results += checkWorkManager(appContext, config, autoRecover)
        results += checkAlarmManager(appContext, config, autoRecover)
        results += checkAccountSync(appContext, config, autoRecover)
        results += checkObservers(appContext, config, autoRecover)
        results += checkNative(appContext, config, autoRecover)
        results += checkMediaRoute(appContext, config, autoRecover)
        results += checkMediaBrowser(appContext, config, autoRecover)
        results += checkVpn(appContext, config, autoRecover)
        results += checkCompanion(appContext, config, autoRecover)
        results += checkNotificationStrategies(appContext, config)
        results += checkSystemBoundStrategies(appContext, config)
        results += checkEnvironment(appContext)

        val report = FwHealthReport(
            generatedAtMillis = System.currentTimeMillis(),
            processImportance = processImportance,
            oomAdj = oomAdj,
            killRisk = killRisk,
            strategies = results
        )
        latestReport = report
        FwLog.d(report.toLogString())
        return report
    }

    /**
     * 读取最近一次巡检报告。
     */
    fun lastReport(): FwHealthReport? = latestReport

    private fun checkForegroundService(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableForegroundService) {
            return disabled(FwStrategyKey.FOREGROUND_SERVICE, "配置关闭")
        }
        val running = isServiceRunning(context, FwForegroundService::class.java.name)
        val recovered = autoRecover && !running && recover(FwStrategyKey.FOREGROUND_SERVICE) {
            ServiceStarter.startForegroundService(context, "健康巡检补偿")
        }
        return health(
            key = FwStrategyKey.FOREGROUND_SERVICE,
            state = if (running || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
            enabled = true,
            supported = true,
            message = if (running) "服务运行中" else if (recovered) "已触发补偿启动" else "服务未运行",
            recovered = recovered
        )
    }

    private fun checkSilentAudio(config: FwConfig): FwStrategyHealth {
        if (!config.enableSilentAudio) {
            return disabled(FwStrategyKey.SILENT_AUDIO, "配置关闭")
        }
        val active = SilentAudioStrategy.isActive()
        return health(
            key = FwStrategyKey.SILENT_AUDIO,
            state = if (active) FwStrategyHealthState.RUNNING else FwStrategyHealthState.DEGRADED,
            enabled = true,
            supported = true,
            message = if (active) "静默音频活跃" else "静默音频未播放"
        )
    }

    private fun checkJobScheduler(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableJobScheduler) {
            return disabled(FwStrategyKey.JOB_SCHEDULER, "配置关闭")
        }
        val scheduled = FwJobService.isScheduled(context)
        val recovered = autoRecover && !scheduled && recover(FwStrategyKey.JOB_SCHEDULER) {
            FwJobService.schedule(context)
        }
        return health(
            key = FwStrategyKey.JOB_SCHEDULER,
            state = if (scheduled || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
            enabled = true,
            supported = true,
            message = if (scheduled) "Job 已在调度队列" else if (recovered) "已重新调度 Job" else "Job 不在调度队列",
            recovered = recovered
        )
    }

    private fun checkWorkManager(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableWorkManager) {
            return disabled(FwStrategyKey.WORK_MANAGER, "配置关闭")
        }
        val scheduled = FwWorker.isScheduled(context)
        val recovered = autoRecover && !scheduled && recover(FwStrategyKey.WORK_MANAGER) {
            FwWorker.schedule(context)
        }
        return health(
            key = FwStrategyKey.WORK_MANAGER,
            state = if (scheduled || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
            enabled = true,
            supported = true,
            message = if (scheduled) "Work 已在队列" else if (recovered) "已重新入队 Work" else "Work 不在队列",
            recovered = recovered
        )
    }

    private fun checkAlarmManager(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableAlarmManager) {
            return disabled(FwStrategyKey.ALARM_MANAGER, "配置关闭")
        }
        val scheduled = AlarmStrategy.isScheduled(context)
        val exactAllowed = AlarmStrategy.canScheduleExactAlarms(context)
        val recovered = autoRecover && !scheduled && recover(FwStrategyKey.ALARM_MANAGER) {
            AlarmStrategy.schedule(context)
        }
        val state = when {
            scheduled || recovered -> FwStrategyHealthState.RUNNING
            !exactAllowed -> FwStrategyHealthState.PERMISSION_REQUIRED
            else -> FwStrategyHealthState.STOPPED
        }
        return health(
            key = FwStrategyKey.ALARM_MANAGER,
            state = state,
            enabled = true,
            supported = true,
            message = "闹钟存在=$scheduled, 精确闹钟权限=$exactAllowed",
            recovered = recovered
        )
    }

    private fun checkAccountSync(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableAccountSync) {
            return disabled(FwStrategyKey.ACCOUNT_SYNC, "配置关闭")
        }
        val enabled = FwSyncAdapter.isSyncEnabled(context)
        val recovered = autoRecover && !enabled && recover(FwStrategyKey.ACCOUNT_SYNC) {
            FwSyncAdapter.enableSync(context)
        }
        return health(
            key = FwStrategyKey.ACCOUNT_SYNC,
            state = if (enabled || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
            enabled = true,
            supported = true,
            message = if (enabled) "账户同步已开启" else if (recovered) "已重新启用同步" else "账户同步未开启",
            recovered = recovered
        )
    }

    private fun checkObservers(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): List<FwStrategyHealth> {
        val contentEnabled = config.enableMediaContentObserver ||
            config.enableContactsContentObserver ||
            config.enableSmsContentObserver ||
            config.enableSettingsContentObserver
        val contentHealth = if (!contentEnabled) {
            disabled(FwStrategyKey.CONTENT_OBSERVER, "配置关闭")
        } else {
            val registered = ContentObserverManager.isRegistered()
            val recovered = autoRecover && !registered && recover(FwStrategyKey.CONTENT_OBSERVER) {
                ContentObserverManager.registerAll(context)
            }
            health(
                key = FwStrategyKey.CONTENT_OBSERVER,
                state = if (registered || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
                enabled = true,
                supported = true,
                message = if (registered) "内容观察者已注册" else "内容观察者未注册",
                recovered = recovered
            )
        }
        val fileHealth = if (!config.enableFileObserver) {
            disabled(FwStrategyKey.FILE_OBSERVER, "配置关闭")
        } else {
            val registered = FileObserverManager.isRegistered()
            val recovered = autoRecover && !registered && recover(FwStrategyKey.FILE_OBSERVER) {
                FileObserverManager.registerAll(context)
            }
            health(
                key = FwStrategyKey.FILE_OBSERVER,
                state = if (registered || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
                enabled = true,
                supported = true,
                message = if (registered) "文件观察者已注册" else "文件观察者未注册",
                recovered = recovered
            )
        }
        return listOf(contentHealth, fileHealth)
    }

    private fun checkNative(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): List<FwStrategyHealth> {
        val nativeAvailable = FwNative.isAvailable() || FwNative.init(context)
        val rustSnapshot = FwRustNative.processSnapshotOrNull()
        val rustMessage = rustSnapshot?.toHealthMessage()
        val daemonHealth = if (!config.enableNativeDaemon) {
            disabled(FwStrategyKey.NATIVE_DAEMON, "配置关闭")
        } else if (!nativeAvailable) {
            health(FwStrategyKey.NATIVE_DAEMON, FwStrategyHealthState.ERROR, true, true, "Native 库不可用")
        } else {
            val running = runCatching { FwNative.isDaemonRunning() }.getOrDefault(false)
            val recovered = autoRecover && !running && recover(FwStrategyKey.NATIVE_DAEMON) {
                FwNative.startDaemon(
                    context.packageName,
                    "com.service.framework.service.FwForegroundService",
                    config.nativeDaemonCheckInterval
                )
            }
            health(
                key = FwStrategyKey.NATIVE_DAEMON,
                state = if (running || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
                enabled = true,
                supported = true,
                message = listOfNotNull(
                    if (running) "Native 守护运行中" else "Native 守护未运行",
                    rustMessage
                ).joinToString("；"),
                recovered = recovered
            )
        }
        val socketHealth = if (!config.enableNativeSocket) {
            disabled(FwStrategyKey.NATIVE_SOCKET, "配置关闭")
        } else if (!nativeAvailable) {
            health(FwStrategyKey.NATIVE_SOCKET, FwStrategyHealthState.ERROR, true, true, "Native 库不可用")
        } else {
            health(FwStrategyKey.NATIVE_SOCKET, FwStrategyStateManager.snapshot(FwStrategyKey.NATIVE_SOCKET).status, true, true, "Native Socket 状态见运行时统计")
        }
        return listOf(daemonHealth, socketHealth)
    }

    private fun checkMediaRoute(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableMediaRouteProvider && !config.enableMediaRoute2Provider) {
            return disabled(FwStrategyKey.MEDIA_ROUTE, "配置关闭")
        }
        val ok = runCatching {
            FwMediaRouteManager.heartbeat()
            FwMediaRouteManager.check()
        }.getOrDefault(false)
        val recovered = autoRecover && !ok && recover(FwStrategyKey.MEDIA_ROUTE) {
            FwMediaRouteManager.init(context, config)
            FwMediaRouteManager.start(context)
        }
        return health(
            key = FwStrategyKey.MEDIA_ROUTE,
            state = if (ok || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.DEGRADED,
            enabled = true,
            supported = true,
            message = if (ok) "MediaRoute 心跳正常" else "MediaRoute 状态异常",
            recovered = recovered
        )
    }

    private fun checkMediaBrowser(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableMediaBrowserService) {
            return disabled(FwStrategyKey.MEDIA_BROWSER, "配置关闭")
        }
        val running = FwMediaBrowserService.isRunning()
        val recovered = autoRecover && !running && recover(FwStrategyKey.MEDIA_BROWSER) {
            FwMediaBrowserService.start(context)
        }
        return health(
            key = FwStrategyKey.MEDIA_BROWSER,
            state = if (running || recovered) FwStrategyHealthState.RUNNING else FwStrategyHealthState.STOPPED,
            enabled = true,
            supported = true,
            message = if (running) "MediaBrowserService 运行中" else "MediaBrowserService 未运行",
            recovered = recovered
        )
    }

    private fun checkVpn(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableVpnService) {
            return disabled(FwStrategyKey.VPN_SERVICE, "配置关闭")
        }
        val supported = Build.VERSION.SDK_INT >= 24
        if (!supported) {
            return health(FwStrategyKey.VPN_SERVICE, FwStrategyHealthState.UNSUPPORTED, true, false, "API 24 以下不支持")
        }
        val prepared = FwVpnService.isPrepared(context)
        val running = FwVpnService.isRunning()
        val recovered = autoRecover && prepared && !running && recover(FwStrategyKey.VPN_SERVICE) {
            FwVpnService.start(context)
        }
        val state = when {
            running || recovered -> FwStrategyHealthState.RUNNING
            !prepared -> FwStrategyHealthState.PERMISSION_REQUIRED
            else -> FwStrategyHealthState.STOPPED
        }
        return health(
            key = FwStrategyKey.VPN_SERVICE,
            state = state,
            enabled = true,
            supported = true,
            message = "授权=$prepared, 运行=$running",
            recovered = recovered
        )
    }

    private fun checkCompanion(
        context: Context,
        config: FwConfig,
        autoRecover: Boolean
    ): FwStrategyHealth {
        if (!config.enableCompanionDevice) {
            return disabled(FwStrategyKey.COMPANION_DEVICE, "配置关闭")
        }
        val supported = CompanionDeviceManagerHelper.isSupported()
        if (!supported) {
            return health(FwStrategyKey.COMPANION_DEVICE, FwStrategyHealthState.UNSUPPORTED, true, false, "API 31 以下不支持")
        }
        val count = CompanionDeviceManagerHelper.getAssociationCount(context)
        val recovered = autoRecover && count > 0 && recover(FwStrategyKey.COMPANION_DEVICE) {
            CompanionDeviceManagerHelper.startObserving(context)
        }
        val state = if (count > 0) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED
        return health(
            key = FwStrategyKey.COMPANION_DEVICE,
            state = state,
            enabled = true,
            supported = true,
            message = "已关联设备数量=$count",
            recovered = recovered
        )
    }

    private fun checkNotificationStrategies(
        context: Context,
        config: FwConfig
    ): List<FwStrategyHealth> {
        val mediaSession = if (!config.enableMediaSessionNotification) {
            disabled(FwStrategyKey.MEDIA_SESSION_NOTIFICATION, "配置关闭")
        } else {
            val state = FwStrategyStateManager.snapshot(FwStrategyKey.MEDIA_SESSION_NOTIFICATION).status
            health(FwStrategyKey.MEDIA_SESSION_NOTIFICATION, normalizeRuntimeState(state), true, true, "MediaSession 通知状态见运行时统计")
        }
        val callStyle = if (!config.enableCallStyleNotification) {
            disabled(FwStrategyKey.CALL_STYLE_NOTIFICATION, "配置关闭")
        } else {
            val state = FwStrategyStateManager.snapshot(FwStrategyKey.CALL_STYLE_NOTIFICATION).status
            health(FwStrategyKey.CALL_STYLE_NOTIFICATION, normalizeRuntimeState(state), true, Build.VERSION.SDK_INT >= 31, "CallStyle 通知状态见运行时统计")
        }
        return listOf(mediaSession, callStyle)
    }

    private fun checkSystemBoundStrategies(
        context: Context,
        config: FwConfig
    ): List<FwStrategyHealth> {
        val deviceAdmin = if (!config.enableDeviceAdmin) {
            disabled(FwStrategyKey.DEVICE_ADMIN, "配置关闭")
        } else {
            val active = FwDeviceAdminReceiver.isAdminActive(context)
            health(
                FwStrategyKey.DEVICE_ADMIN,
                if (active) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED,
                true,
                true,
                "设备管理员激活=$active"
            )
        }
        val accessibility = health(
            FwStrategyKey.ACCESSIBILITY_SERVICE,
            if (FwAccessibilityService.isEnabled(context)) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED,
            true,
            true,
            "无障碍授权=${FwAccessibilityService.isEnabled(context)}"
        )
        val notificationListener = health(
            FwStrategyKey.NOTIFICATION_LISTENER,
            if (FwNotificationListenerService.isEnabled(context)) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED,
            true,
            true,
            "通知监听授权=${FwNotificationListenerService.isEnabled(context)}"
        )
        val tile = if (!config.enableTileService) {
            disabled(FwStrategyKey.TILE_SERVICE, "配置关闭")
        } else {
            health(FwStrategyKey.TILE_SERVICE, FwStrategyHealthState.DEGRADED, true, Build.VERSION.SDK_INT >= 24, "磁贴需用户手动添加")
        }
        val widget = if (!config.enableWidget) {
            disabled(FwStrategyKey.WIDGET_PROVIDER, "配置关闭")
        } else {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, FwWidgetProvider::class.java))
            health(
                FwStrategyKey.WIDGET_PROVIDER,
                if (ids.isNotEmpty()) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED,
                true,
                true,
                "已添加小组件数量=${ids.size}"
            )
        }
        val dream = if (!config.enableDreamService) {
            disabled(FwStrategyKey.DREAM_SERVICE, "配置关闭")
        } else {
            health(FwStrategyKey.DREAM_SERVICE, FwStrategyHealthState.DEGRADED, true, true, "屏保服务需用户在系统设置选择")
        }
        return listOf(deviceAdmin, accessibility, notificationListener, tile, widget, dream)
    }

    private fun checkEnvironment(context: Context): List<FwStrategyHealth> {
        val batteryIgnored = BatteryOptimizationManager.isIgnoringBatteryOptimizations(context)
        val battery = health(
            FwStrategyKey.BATTERY_OPTIMIZATION,
            if (batteryIgnored) FwStrategyHealthState.RUNNING else FwStrategyHealthState.PERMISSION_REQUIRED,
            true,
            true,
            "忽略电池优化=$batteryIgnored"
        )
        val pushSdks = VendorIntegrationAnalyzer.analyzePushSdks(context, context.packageName)
        val vendor = health(
            FwStrategyKey.VENDOR_INTEGRATION,
            if (pushSdks.isNotEmpty()) FwStrategyHealthState.RUNNING else FwStrategyHealthState.DEGRADED,
            true,
            true,
            if (pushSdks.isEmpty()) "未检测到厂商推送 SDK" else "厂商推送 SDK=${pushSdks.joinToString()}"
        )
        return listOf(battery, vendor)
    }

    private fun disabled(key: FwStrategyKey, message: String): FwStrategyHealth {
        FwStrategyStateManager.markDisabled(key, message)
        return health(key, FwStrategyHealthState.DISABLED, enabled = false, supported = true, message = message)
    }

    private fun health(
        key: FwStrategyKey,
        state: FwStrategyHealthState,
        enabled: Boolean,
        supported: Boolean,
        message: String,
        recovered: Boolean = false
    ): FwStrategyHealth {
        return FwStrategyHealth(
            key = key,
            state = state,
            enabled = enabled,
            supported = supported,
            message = message,
            runtimeState = FwStrategyStateManager.snapshot(key),
            recovered = recovered
        )
    }

    private fun recover(key: FwStrategyKey, action: () -> Unit): Boolean {
        return try {
            FwLog.w("FwHealth: ${key.displayName} 状态异常，执行补偿")
            action()
            FwStrategyStateManager.markTriggered(key, "健康巡检补偿")
            true
        } catch (e: Exception) {
            FwStrategyStateManager.markError(key, e.message ?: "补偿失败", e)
            false
        }
    }

    private fun normalizeRuntimeState(state: FwStrategyHealthState): FwStrategyHealthState {
        return if (state == FwStrategyHealthState.STOPPED) {
            FwStrategyHealthState.DEGRADED
        } else {
            state
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClassName: String): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
            activityManager.getRunningServices(Int.MAX_VALUE).any { serviceInfo ->
                serviceInfo.service.className == serviceClassName
            }
        } catch (e: Exception) {
            FwLog.e("FwHealth: 检查服务运行状态失败: ${e.message}", e)
            false
        }
    }
}
