/**
 * ============================================================================
 * ForceStopResistance.kt - 无法强制停止 策略入口
 * ============================================================================
 *
 * 功能简介：
 *   通过多进程文件锁监控 + Native 层 Binder 直接调用，
 *   实现在用户点击"强制停止"时快速拉起服务，抵抗强制停止操作。
 *
 * 核心原理（C++ Native 层实现）：
 *   1. 启动多个辅助进程（:assist1, :assist2, :assist3）
 *   2. 使用 flock() 文件锁互相监控进程存活
 *   3. 检测到进程被杀时，直接通过 Binder 驱动调用 AMS.startService
 *   4. 跳过 Java 层所有开销，响应时间极短（毫秒级）
 *
 * 教学说明（历史背景）：
 *   这是 Android 5.0 - 9.0 时代的保活技术复现，展示了早期 Android 系统的漏洞：
 *   1. 应用可以直接打开 /dev/binder 设备
 *   2. 可以通过 ioctl 直接与 Binder 驱动通信
 *   3. 强制停止进程是逐个杀死，存在时间窗口
 *
 *   Android 10+ 已封堵：
 *   1. 强制停止改为 cgroup 进程组整体杀死
 *   2. SELinux 限制了 Binder 设备的直接访问
 *   3. 后台进程的 Binder 调用受限
 *
 * 适用版本：
 *   Android 5.0 - Android 12.0（Android 13.0 + 已失效）
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.native.FwNative
import com.service.framework.util.FwLog

object ForceStopResistance {

    private const val TAG = "ForceStopResistance" // 日志标签

    @Volatile
    private var isInitialized = false // 是否已初始化

    @Volatile
    private var isAttached = false // 是否已附加

    /**
     * 初始化策略
     * 必须在 Application.attachBaseContext() 中调用
     *
     * @param context 应用上下文
     */
    fun init(context: Context) {
        if (isInitialized) { // 防止重复初始化
            FwLog.d("$TAG: 已初始化，跳过")
            return
        }

        // Android 10+ 此策略已失效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FwLog.w("$TAG: Android 10+ 此策略已失效（cgroup 进程组杀死机制）")
            return
        }

        // 绕过隐藏 API 限制（Android 9+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.exemptAll()
        }

        // 初始化 Native 模块
        FwNative.init(context)

        isInitialized = true
        FwLog.d("$TAG: 初始化完成")
    }

    /**
     * 附加到应用
     * 必须在 Application.onCreate() 中调用
     *
     * @param app 应用实例
     */
    fun attach(app: Application) {
        // Android 10+ 此策略已失效，直接返回
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FwLog.w("$TAG: Android 10+ 此策略已失效，跳过附加")
            return
        }

        if (!isInitialized) { // 必须先初始化
            // 尝试自动初始化
            init(app)
            if (!isInitialized) {
                FwLog.e("$TAG: 初始化失败")
                return
            }
        }

        if (isAttached) { // 防止重复附加
            FwLog.d("$TAG: 已附加，跳过")
            return
        }

        val processName = ProcessUtils.getProcessName() // 获取当前进程名
        val packageName = app.packageName // 获取包名

        FwLog.d("$TAG: 当前进程: $processName, 包名: $packageName")

        when {
            // 主进程：启动守护服务和辅助进程
            processName == packageName -> {
                FwLog.d("$TAG: 主进程，启动 Native 守护机制")
                startAssistProcesses(app) // 启动辅助进程（用于多进程监控）
                startNativeDaemon(app)    // 启动 Native 层守护进程
            }
            // 辅助进程：启动文件锁监控
            processName?.contains(":assist") == true -> {
                val assistName = processName.substringAfter(":") // 提取进程后缀名
                FwLog.d("$TAG: 辅助进程 $assistName，启动 Native 文件锁监控")
                startNativeFileLockMonitor(app, assistName) // 启动 Native 层文件锁监控
            }
        }

        isAttached = true
        FwLog.d("$TAG: 附加完成")
    }

    /**
     * 检查策略是否启用
     */
    fun isEnabled(): Boolean = isInitialized && isAttached

    /**
     * 停止策略
     */
    fun stop() {
        isInitialized = false
        isAttached = false
        FwLog.d("$TAG: 策略已停止")
    }

    /**
     * 启动辅助进程
     * 通过 startService 启动多个辅助进程服务
     *
     * @param context 上下文
     */
    private fun startAssistProcesses(context: Context) {
        val assistServices = listOf( // 辅助服务列表
            AssistService1::class.java,
            AssistService2::class.java,
            AssistService3::class.java
        )

        assistServices.forEach { serviceClass -> // 遍历启动每个辅助服务
            try {
                val intent = Intent(context, serviceClass)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent) // Android 8.0+ 使用前台服务启动
                } else {
                    context.startService(intent) // 低版本直接启动服务
                }
                FwLog.d("$TAG: 启动辅助服务 ${serviceClass.simpleName}")
            } catch (e: Exception) {
                FwLog.e("$TAG: 启动辅助服务失败 ${serviceClass.simpleName}", e)
            }
        }
    }

    /**
     * 启动 Native 层守护进程
     * 通过 C++ 层 fork() 创建守护进程，与主进程互相监控
     *
     * @param context 上下文
     */
    private fun startNativeDaemon(context: Context) {
        if (!FwNative.isAvailable()) {
            FwLog.e("$TAG: Native 模块不可用，无法启动守护进程")
            return
        }

        val filesDir = context.filesDir.absolutePath
        val packageName = context.packageName
        val serviceName = "com.service.framework.service.FwForegroundService"
        val sdkVersion = Build.VERSION.SDK_INT

        // 构建文件路径
        val indicatorSelfPath = "$filesDir/indicator_main"
        val indicatorDaemonPath = "$filesDir/indicator_daemon"
        val observerSelfPath = "$filesDir/observer_main"
        val observerDaemonPath = "$filesDir/observer_daemon"

        FwLog.d("$TAG: 启动 Native 守护进程, SDK=$sdkVersion")

        try {
            FwNative.startForceStopDaemon(
                indicatorSelfPath,
                indicatorDaemonPath,
                observerSelfPath,
                observerDaemonPath,
                packageName,
                serviceName,
                sdkVersion
            )
            FwLog.d("$TAG: Native 守护进程启动成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: Native 守护进程启动失败", e)
        }
    }

    /**
     * 启动 Native 层文件锁监控
     * 在辅助进程中使用 Native 层 flock() 监控其他进程
     *
     * @param context 上下文
     * @param assistName 辅助进程名称（assist1/assist2/assist3）
     */
    private fun startNativeFileLockMonitor(context: Context, assistName: String) {
        if (!FwNative.isAvailable()) {
            FwLog.e("$TAG: Native 模块不可用，无法启动文件锁监控")
            return
        }

        val filesDir = context.filesDir.absolutePath
        val packageName = context.packageName
        val serviceName = "com.service.framework.service.FwForegroundService"
        val sdkVersion = Build.VERSION.SDK_INT

        // 根据辅助进程名构建文件路径
        val indicatorSelfPath = "$filesDir/indicator_$assistName"
        val indicatorDaemonPath = "$filesDir/indicator_main"
        val observerSelfPath = "$filesDir/observer_$assistName"
        val observerDaemonPath = "$filesDir/observer_main"

        FwLog.d("$TAG: $assistName 启动 Native 文件锁监控")

        try {
            // 先锁定自己的文件
            FwNative.lockFile(indicatorSelfPath)

            // 启动守护进程监控主进程
            FwNative.startForceStopDaemon(
                indicatorSelfPath,
                indicatorDaemonPath,
                observerSelfPath,
                observerDaemonPath,
                packageName,
                serviceName,
                sdkVersion
            )
            FwLog.d("$TAG: $assistName Native 文件锁监控启动成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: $assistName Native 文件锁监控启动失败", e)
        }
    }
}
