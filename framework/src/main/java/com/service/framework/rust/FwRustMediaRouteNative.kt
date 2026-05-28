/**
 * ============================================================================
 * FwRustMediaRouteNative.kt - Rust MediaRoute Native 桥接入口
 * ============================================================================
 *
 * 功能简介：
 *   封装 Rust 版 MediaRoute 服务状态与心跳逻辑。当前只迁移状态记录、
 *   WakeLock 检查计数和心跳判断，真实 WakeLock 获取仍由 Kotlin 层完成。
 *
 * 函数简介：
 *   - init：初始化 Rust MediaRoute 状态。
 *   - checkWakeLock：记录 WakeLock 检查心跳。
 *   - onServiceStarted / onServiceStopped：记录 MediaRouteProviderService 状态。
 *   - onService2Started / onService2Stopped：记录 MediaRoute2ProviderService 状态。
 *   - performHeartbeat：执行 Rust 心跳判断。
 *   - getServiceStatus：读取服务状态码。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 */
package com.service.framework.rust

import com.service.framework.util.FwLog

/**
 * Rust MediaRoute Native 桥接入口。
 */
internal object FwRustMediaRouteNative {

    private const val TAG = "FwRustMediaRouteNative"

    // Rust MediaRoute 状态是否已经初始化。
    private var isInitialized = false

    /**
     * 初始化 Rust MediaRoute 状态。
     */
    fun init(): Boolean {
        if (isInitialized) {
            FwLog.d("$TAG: Rust MediaRoute 已初始化，跳过")
            return true
        }
        if (!FwRustNative.isAvailable()) {
            FwLog.d("$TAG: Rust Native 不可用，MediaRoute 使用 C++ 回退")
            return false
        }
        return try {
            nativeInit()
            isInitialized = true
            FwLog.d("$TAG: Rust MediaRoute 初始化成功")
            true
        } catch (e: UnsatisfiedLinkError) {
            FwLog.e("$TAG: Rust MediaRoute 符号不可用 - ${e.message}", e)
            false
        } catch (e: RuntimeException) {
            FwLog.e("$TAG: Rust MediaRoute 初始化异常 - ${e.message}", e)
            false
        }
    }

    /**
     * 检查 Rust MediaRoute 是否可用。
     */
    fun isAvailable(): Boolean = isInitialized || init()

    /**
     * 记录 WakeLock 检查。
     */
    fun checkWakeLock() {
        if (isAvailable()) {
            nativeCheckWakeLock()
        }
    }

    /**
     * 记录 MediaRouteProviderService 启动。
     */
    fun onServiceStarted(packageName: String, serviceName: String) {
        if (isAvailable()) {
            nativeOnServiceStarted(packageName, serviceName)
        }
    }

    /**
     * 记录 MediaRouteProviderService 停止。
     */
    fun onServiceStopped() {
        if (isAvailable()) {
            nativeOnServiceStopped()
        }
    }

    /**
     * 记录 MediaRoute2ProviderService 启动。
     */
    fun onService2Started(packageName: String, serviceName: String) {
        if (isAvailable()) {
            nativeOnService2Started(packageName, serviceName)
        }
    }

    /**
     * 记录 MediaRoute2ProviderService 停止。
     */
    fun onService2Stopped() {
        if (isAvailable()) {
            nativeOnService2Stopped()
        }
    }

    /**
     * 执行 Rust MediaRoute 心跳。
     */
    fun performHeartbeat(): Boolean {
        return isAvailable() && nativePerformHeartbeat()
    }

    /**
     * 获取 Rust MediaRoute 服务状态。
     */
    fun getServiceStatus(): Int {
        return if (isAvailable()) {
            nativeGetServiceStatus()
        } else {
            2
        }
    }

    @JvmStatic
    private external fun nativeInit()

    @JvmStatic
    private external fun nativeCheckWakeLock()

    @JvmStatic
    private external fun nativeOnServiceStarted(packageName: String, serviceName: String)

    @JvmStatic
    private external fun nativeOnServiceStopped()

    @JvmStatic
    private external fun nativeOnService2Started(packageName: String, serviceName: String)

    @JvmStatic
    private external fun nativeOnService2Stopped()

    @JvmStatic
    private external fun nativePerformHeartbeat(): Boolean

    @JvmStatic
    private external fun nativeGetServiceStatus(): Int
}
