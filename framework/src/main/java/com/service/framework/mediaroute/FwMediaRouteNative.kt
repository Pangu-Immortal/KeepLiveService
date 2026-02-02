/**
 * ============================================================================
 * FwMediaRouteNative.kt - MediaRoute 模块 Native 层接口
 * ============================================================================
 *
 * 功能简介：
 *   封装 MediaRoute 保活模块的 Native 层功能，包括：
 *   - WakeLock 管理（保持 CPU 唤醒）
 *   - 服务状态监控
 *   - 心跳检测
 *
 * 核心机制：
 *   - 通过 JNI 调用 Native 层代码
 *   - 在 Native 层获取 PowerManager WakeLock
 *   - 实现更底层的保活逻辑
 *
 * 安全研究要点：
 *   - WakeLock 可以防止 CPU 休眠
 *   - 但长期持有 WakeLock 会严重消耗电量
 *   - 某些厂商 ROM 会检测并限制 WakeLock 滥用
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.content.Context
import android.os.PowerManager
import com.service.framework.util.FwLog

/**
 * MediaRoute 模块 Native 层接口
 *
 * 提供以下功能：
 * 1. WakeLock 管理 - 保持 CPU 唤醒状态
 * 2. 服务状态监控 - 监听服务启停事件
 * 3. 心跳检测 - 周期性检查服务存活
 */
object FwMediaRouteNative {

    private const val TAG = "FwMediaRouteNative"

    // Native 库加载状态
    private var isLoaded = false

    // WakeLock 实例
    private var wakeLock: PowerManager.WakeLock? = null

    // 应用上下文
    private var appContext: Context? = null

    /**
     * 初始化 Native 模块
     *
     * @param context 应用上下文
     * @return 是否初始化成功
     */
    fun init(context: Context): Boolean {
        appContext = context.applicationContext

        if (isLoaded) {
            FwLog.d("$TAG: 已初始化，跳过")
            return true
        }

        return try {
            System.loadLibrary("fw_mediaroute")
            isLoaded = true
            FwLog.d("$TAG: Native 库加载成功")

            // 初始化 Native 层
            nativeInit()
            true
        } catch (e: UnsatisfiedLinkError) {
            FwLog.e("$TAG: Native 库加载失败 - ${e.message}", e)
            isLoaded = false
            false
        }
    }

    /**
     * 检查 Native 模块是否可用
     */
    fun isAvailable(): Boolean = isLoaded

    // ==================== WakeLock 管理 ====================

    /**
     * 获取 WakeLock
     *
     * 获取部分唤醒锁，保持 CPU 运行。
     * 使用 PARTIAL_WAKE_LOCK 级别，不影响屏幕状态。
     *
     * @param tag WakeLock 标签
     * @param timeout 超时时间（毫秒），0 表示无超时
     * @return 是否获取成功
     */
    @Suppress("DEPRECATION")
    fun acquireWakeLock(tag: String = "FwMediaRoute", timeout: Long = 0): Boolean {
        val context = appContext ?: return false

        return try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    ?: return false

                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "FwFramework:$tag"
                ).apply {
                    setReferenceCounted(false)  // 不使用引用计数
                }
            }

            wakeLock?.let {
                if (!it.isHeld) {
                    if (timeout > 0) {
                        it.acquire(timeout)
                        FwLog.d("$TAG: WakeLock 已获取 (timeout=${timeout}ms)")
                    } else {
                        it.acquire()
                        FwLog.d("$TAG: WakeLock 已获取 (无超时)")
                    }
                }
            }
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 获取 WakeLock 失败 - ${e.message}", e)
            false
        }
    }

    /**
     * 释放 WakeLock
     */
    fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    FwLog.d("$TAG: WakeLock 已释放")
                }
            }
        } catch (e: Exception) {
            FwLog.e("$TAG: 释放 WakeLock 失败 - ${e.message}", e)
        }
    }

    /**
     * 检查 WakeLock 状态
     *
     * 短暂获取然后释放 WakeLock，保持 CPU 活跃片刻。
     * 这是一种低侵入性的保活技巧。
     */
    fun checkWakeLock() {
        FwLog.d("$TAG: 检查 WakeLock")

        // 短暂获取 WakeLock（1秒）
        if (acquireWakeLock(timeout = 1000L)) {
            // Native 层检查
            if (isLoaded) {
                nativeCheckWakeLock()
            }
        }
    }

    /**
     * 检查 WakeLock 是否被持有
     */
    fun isWakeLockHeld(): Boolean {
        return wakeLock?.isHeld == true
    }

    // ==================== 服务状态监控 ====================

    /**
     * 通知 Native 层 MediaRouteProviderService 已启动
     *
     * @param packageName 应用包名
     * @param serviceName 服务类名
     */
    fun onServiceStarted(packageName: String, serviceName: String) {
        FwLog.d("$TAG: 服务已启动 - $serviceName")
        if (isLoaded) {
            nativeOnServiceStarted(packageName, serviceName)
        }
    }

    /**
     * 通知 Native 层 MediaRouteProviderService 已停止
     */
    fun onServiceStopped() {
        FwLog.d("$TAG: 服务已停止")
        if (isLoaded) {
            nativeOnServiceStopped()
        }
    }

    /**
     * 通知 Native 层 MediaRoute2ProviderService 已启动
     *
     * @param packageName 应用包名
     * @param serviceName 服务类名
     */
    fun onService2Started(packageName: String, serviceName: String) {
        FwLog.d("$TAG: MediaRoute2 服务已启动 - $serviceName")
        if (isLoaded) {
            nativeOnService2Started(packageName, serviceName)
        }
    }

    /**
     * 通知 Native 层 MediaRoute2ProviderService 已停止
     */
    fun onService2Stopped() {
        FwLog.d("$TAG: MediaRoute2 服务已停止")
        if (isLoaded) {
            nativeOnService2Stopped()
        }
    }

    // ==================== 心跳检测 ====================

    /**
     * 执行心跳检测
     *
     * 在 Native 层执行心跳逻辑，检查服务存活状态。
     *
     * @return 心跳是否成功
     */
    fun performHeartbeat(): Boolean {
        return if (isLoaded) {
            nativePerformHeartbeat()
        } else {
            FwLog.w("$TAG: Native 模块未加载，跳过心跳")
            false
        }
    }

    /**
     * 获取服务状态
     *
     * @return 状态码：0=正常, 1=警告, 2=异常
     */
    fun getServiceStatus(): Int {
        return if (isLoaded) {
            nativeGetServiceStatus()
        } else {
            2  // 未加载视为异常
        }
    }

    // ==================== Native 方法声明 ====================

    /**
     * Native 层初始化
     */
    @JvmStatic
    private external fun nativeInit()

    /**
     * Native 层 WakeLock 检查
     */
    @JvmStatic
    private external fun nativeCheckWakeLock()

    /**
     * Native 层服务启动通知
     */
    @JvmStatic
    private external fun nativeOnServiceStarted(packageName: String, serviceName: String)

    /**
     * Native 层服务停止通知
     */
    @JvmStatic
    private external fun nativeOnServiceStopped()

    /**
     * Native 层 MediaRoute2 服务启动通知
     */
    @JvmStatic
    private external fun nativeOnService2Started(packageName: String, serviceName: String)

    /**
     * Native 层 MediaRoute2 服务停止通知
     */
    @JvmStatic
    private external fun nativeOnService2Stopped()

    /**
     * Native 层心跳检测
     */
    @JvmStatic
    private external fun nativePerformHeartbeat(): Boolean

    /**
     * Native 层获取服务状态
     */
    @JvmStatic
    private external fun nativeGetServiceStatus(): Int
}
