/**
 * ============================================================================
 * Fw.kt - 保活框架入口类
 * ============================================================================
 *
 * 功能简介：
 *   Fw 框架的核心入口，提供一行代码初始化保活功能。
 *   负责统一管理所有保活策略的启动、停止和配置。
 *
 * 主要功能：
 *   - init(): 初始化框架，启动所有已配置的保活策略
 *   - check(): 手动触发保活检查
 *   - stop(): 停止所有保活策略
 *
 * 保活策略包括：
 *   - 前台服务、MediaSession
 *   - 双进程守护、Native 守护
 *   - JobScheduler、WorkManager、AlarmManager
 *   - 账户同步、系统广播监听
 *   - 内容观察者、文件观察者
 *   - 1像素Activity、锁屏Activity
 *   - 悬浮窗、无法强制停止策略
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.service.framework.account.FwAuthenticator
import com.service.framework.account.FwSyncAdapter
import com.service.framework.core.FwConfig
import com.service.framework.mediaroute.FwMediaRouteManager
import com.service.framework.native.FwNative
import com.service.framework.observer.ContentObserverManager
import com.service.framework.observer.FileObserverManager
import com.service.framework.receiver.DynamicEventReceiver
import com.service.framework.receiver.MediaButtonReceiver
import com.service.framework.receiver.WifiReceiver
import com.service.framework.service.DaemonService
import com.service.framework.strategy.*
import com.service.framework.strategy.forcestop.ForceStopResistance
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

@SuppressLint("StaticFieldLeak")
object Fw {

    @Volatile
    private var isInitialized = false

    lateinit var config: FwConfig
        private set

    lateinit var application: Application
        private set

    private val receivers = mutableListOf<BroadcastReceiver>()

    fun init(app: Application) {
        val builder = FwConfig.Builder().apply {
            enableLockScreenActivity = true
            enableFloatWindow = true
        }
        init(app, builder.build())
    }

    fun init(app: Application, block: FwConfig.Builder.() -> Unit) {
        val builder = FwConfig.Builder().apply(block)
        init(app, builder.build())
    }

    private fun init(app: Application, newConfig: FwConfig) {
        if (isInitialized) {
            FwLog.w("Fw 已初始化，请勿重复调用")
            return
        }
        synchronized(this) {
            if (isInitialized) return

            application = app
            config = newConfig
            isInitialized = true

            logConfig()
            startAllStrategies()
        }
    }

    fun check() {
        ensureInitialized()
        FwLog.d("手动触发保活检查...")
        ServiceStarter.startForegroundService(application, "手动检查")
    }

    fun stop() {
        ensureInitialized()
        FwLog.d("正在停止所有保活策略...")

        ServiceStarter.stopForegroundService(application)
        application.stopService(Intent(application, DaemonService::class.java))
        FwJobService.cancel(application)
        FwWorker.cancel(application)
        AlarmStrategy.cancel(application)
        FwSyncAdapter.disableSync(application)
        ContentObserverManager.unregisterAll(application)
        FileObserverManager.unregisterAll()

        receivers.forEach { try { application.unregisterReceiver(it) } catch (e: Exception) { /* ignore */ } }
        receivers.clear()

        OnePixelActivity.finish()
        LockScreenActivity.finish()
        FloatWindowManager.hide()

        if (FwNative.isAvailable()) {
            FwNative.stopDaemon()
            FwNative.stopSocketServer()
        }

        // 停止 MediaRoute 模块
        if (config.enableMediaRouteProvider || config.enableMediaRoute2Provider) {
            FwMediaRouteManager.stop(application)
        }

        isInitialized = false
        FwLog.d("所有保活策略已停止。")
    }

    fun isInitialized(): Boolean = isInitialized

    private fun startAllStrategies() {
        FwLog.d("================= 开始启动所有策略 ================")

        config.run {
            if (enableForegroundService) startForegroundService()
            if (enableDualProcess) startDaemonService()
            if (enableJobScheduler) FwJobService.schedule(application)
            if (enableWorkManager) FwWorker.schedule(application)
            if (enableAlarmManager) AlarmStrategy.schedule(application)
            if (enableAccountSync) startAccountSync()
            if (enableSystemBroadcast) registerDynamicReceivers()
            if (enableMediaContentObserver || enableContactsContentObserver || enableSmsContentObserver || enableSettingsContentObserver) {
                ContentObserverManager.registerAll(application)
            }
            if (enableFileObserver) FileObserverManager.registerAll(application)
            if (enableOnePixelActivity) registerScreenReceiver()
            if (enableLockScreenActivity) registerLockScreenReceiver()
            if (enableFloatWindow) startFloatWindow()
            if (enableNativeDaemon || enableNativeSocket) initNativeModule()
            if (enableForceStopResistance) startForceStopResistance() // 无法强制停止策略
            if (enableMediaRouteProvider || enableMediaRoute2Provider) initMediaRouteModule() // MediaRoute 保活策略
        }

        FwLog.d("================= 所有策略已启动 ================")
    }

    private fun startForegroundService() {
        FwLog.d("策略: 启动核心前台服务...")
        ServiceStarter.startForegroundService(application, "初始化启动")
    }

    private fun startDaemonService() {
        FwLog.d("策略: 启动 Java 守护进程...")
        try {
            val intent = Intent(application, DaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        } catch (e: Exception) {
            FwLog.e("启动守护进程失败", e)
        }
    }

    private fun startAccountSync() {
        FwLog.d("策略: 注册账户同步...")
        FwAuthenticator.addAccount(application)
        FwSyncAdapter.enableSync(application)
    }

    private fun registerDynamicReceivers() {
        FwLog.d("策略: 注册动态系统广播...")
        registerReceiver(DynamicEventReceiver(), DynamicEventReceiver.getIntentFilter())
        if (config.enableMediaButtonReceiver) {
            registerReceiver(MediaButtonReceiver(), MediaButtonReceiver.getIntentFilter(), isExported = true)
        }
        registerReceiver(WifiReceiver(), WifiReceiver.getIntentFilter())
    }

    private fun registerScreenReceiver() {
        FwLog.d("策略: 注册屏幕亮灭广播 (用于 1 像素 Activity)...")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> OnePixelActivity.start(context)
                    Intent.ACTION_USER_PRESENT -> OnePixelActivity.finish()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)
    }

    private fun registerLockScreenReceiver() {
        FwLog.d("策略: 注册锁屏广播 (用于锁屏 Activity)...")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF && LockScreenActivity.isKeyguardLocked(context)) {
                    LockScreenActivity.start(context)
                }
            }
        }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun startFloatWindow() {
        FwLog.d("策略: 启动悬浮窗...")
        if (!FloatWindowManager.canDrawOverlays(application)) {
            FwLog.w("没有悬浮窗权限，跳过此策略")
            return
        }
        if (config.floatWindowHidden) {
            FloatWindowManager.showOnePixelFloat(application)
        } else {
            FloatWindowManager.showVisibleFloat(application)
        }
    }

    private fun initNativeModule() {
        FwLog.d("策略: 初始化 Native 模块...")
        if (!FwNative.init(application)) {
            FwLog.w("Native 模块初始化失败，相关策略将无法工作")
            return
        }
        config.run {
            if (enableNativeDaemon) FwNative.startDaemon(application.packageName, "com.service.framework.service.FwForegroundService", nativeDaemonCheckInterval)
            if (enableNativeSocket) FwNative.startSocketServer(nativeSocketName)
        }
    }

    /**
     * 启动无法强制停止策略，用户疯狂点击 强制停止 时无法停止。
     * 通过多进程文件锁监控和 app_process 拉活实现
     * 注意：此策略仅在 Android 5.0-11.0 上有效
     */
    private fun startForceStopResistance() {
        FwLog.d("策略: 启动无法强制停止策略...")
        ForceStopResistance.attach(application)
    }

    /**
     * 初始化 MediaRoute 保活模块
     * 通过 MediaRouteProviderService 和 MediaRoute2ProviderService 实现保活
     * 这是酷狗音乐等 App 的核心保活技术之一
     */
    private fun initMediaRouteModule() {
        FwLog.d("策略: 初始化 MediaRoute 保活模块...")
        if (!FwMediaRouteManager.init(application, config)) {
            FwLog.w("MediaRoute 模块初始化失败")
            return
        }
        FwMediaRouteManager.start(application)
    }

    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter, isExported: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flag = if (isExported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
                application.registerReceiver(receiver, filter, flag)
            } else {
                application.registerReceiver(receiver, filter)
            }
            receivers.add(receiver)
            FwLog.d("广播 [${receiver.javaClass.simpleName}] 注册成功")
        } catch (e: Exception) {
            FwLog.e("广播 [${receiver.javaClass.simpleName}] 注册失败", e)
        }
    }

    private fun logConfig() {
        FwLog.d("================= Fw 框架配置 ================")
        config.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            FwLog.d("  ${field.name} = ${field.get(config)}")
        }
        FwLog.d("===============================================")
    }

    private fun ensureInitialized() {
        check(isInitialized) { "Fw 框架尚未初始化，请先在 Application.onCreate 中调用 Fw.init()" }
    }
}
