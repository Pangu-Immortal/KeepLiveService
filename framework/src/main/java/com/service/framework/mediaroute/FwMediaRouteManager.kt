/**
 * ============================================================================
 * FwMediaRouteManager.kt - 媒体路由模块管理器
 * ============================================================================
 *
 * 功能简介：
 *   统一管理 MediaRoute 保活模块的所有组件，包括：
 *   - FwMediaRouteProviderService
 *   - FwMediaRoute2ProviderService
 *   - Native 层初始化
 *
 * 核心机制：
 *   - 作为 MediaRoute 模块的统一入口
 *   - 根据配置和系统版本选择性启动组件
 *   - 提供模块状态查询接口
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.core.FwConfig
import com.service.framework.util.FwLog

/**
 * 媒体路由模块管理器
 *
 * 负责管理 MediaRoute 保活模块的生命周期：
 * 1. 初始化 Native 层
 * 2. 启动/停止 MediaRouteProviderService
 * 3. 启动/停止 MediaRoute2ProviderService（Android 11+）
 * 4. 提供模块状态查询
 */
object FwMediaRouteManager {

    private const val TAG = "FwMediaRouteManager"

    // 模块是否已初始化
    private var isInitialized = false

    // 应用上下文
    private var appContext: Context? = null

    // 配置
    private var config: FwConfig? = null

    /**
     * 初始化 MediaRoute 模块
     *
     * @param context 应用上下文
     * @param config 框架配置
     * @return 是否初始化成功
     */
    fun init(context: Context, config: FwConfig): Boolean {
        if (isInitialized) {
            FwLog.d("$TAG: 已初始化，跳过")
            return true
        }

        appContext = context.applicationContext
        this.config = config

        FwLog.i("$TAG: 开始初始化 MediaRoute 模块")

        // 检查配置是否启用
        if (!config.enableMediaRouteProvider) {
            FwLog.d("$TAG: MediaRouteProvider 未启用，跳过初始化")
            return true
        }

        // 初始化 Native 层
        val nativeResult = FwMediaRouteNative.init(context)
        if (!nativeResult) {
            FwLog.w("$TAG: Native 层初始化失败，但继续运行 Java 层")
        }

        isInitialized = true
        FwLog.i("$TAG: MediaRoute 模块初始化完成")

        return true
    }

    /**
     * 启动 MediaRoute 保活服务
     *
     * @param context 上下文
     */
    fun start(context: Context) {
        val cfg = config ?: return
        if (!cfg.enableMediaRouteProvider) {
            FwLog.d("$TAG: MediaRouteProvider 未启用，跳过启动")
            return
        }

        FwLog.i("$TAG: 启动 MediaRoute 保活服务")

        // 启动 MediaRouteProviderService
        startMediaRouteProviderService(context)

        // Android 11+ 启动 MediaRoute2ProviderService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && cfg.enableMediaRoute2Provider) {
            startMediaRoute2ProviderService(context)
        }
    }

    /**
     * 停止 MediaRoute 保活服务
     *
     * @param context 上下文
     */
    fun stop(context: Context) {
        FwLog.i("$TAG: 停止 MediaRoute 保活服务")

        // 停止 MediaRouteProviderService
        stopMediaRouteProviderService(context)

        // 停止 MediaRoute2ProviderService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stopMediaRoute2ProviderService(context)
        }

        // 释放 WakeLock
        FwMediaRouteNative.releaseWakeLock()
    }

    /**
     * 检查模块状态
     *
     * @return 模块是否正常运行
     */
    fun check(): Boolean {
        if (!isInitialized) {
            FwLog.w("$TAG: 模块未初始化")
            return false
        }

        val status = FwMediaRouteNative.getServiceStatus()
        FwLog.d("$TAG: 服务状态=$status")

        return status == 0
    }

    /**
     * 执行心跳
     */
    fun heartbeat() {
        if (!isInitialized) return

        // 执行 Native 层心跳
        FwMediaRouteNative.performHeartbeat()

        // 检查 WakeLock
        FwMediaRouteNative.checkWakeLock()
    }

    /**
     * 启动 MediaRouteProviderService
     */
    private fun startMediaRouteProviderService(context: Context) {
        try {
            val intent = Intent(context, FwMediaRouteProviderService::class.java)
            context.startService(intent)
            FwLog.d("$TAG: MediaRouteProviderService 启动请求已发送")
        } catch (e: Exception) {
            FwLog.e("$TAG: 启动 MediaRouteProviderService 失败 - ${e.message}", e)
        }
    }

    /**
     * 停止 MediaRouteProviderService
     */
    private fun stopMediaRouteProviderService(context: Context) {
        try {
            val intent = Intent(context, FwMediaRouteProviderService::class.java)
            context.stopService(intent)
            FwLog.d("$TAG: MediaRouteProviderService 已停止")
        } catch (e: Exception) {
            FwLog.e("$TAG: 停止 MediaRouteProviderService 失败 - ${e.message}", e)
        }
    }

    /**
     * 启动 MediaRoute2ProviderService
     */
    private fun startMediaRoute2ProviderService(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        try {
            val intent = Intent(context, FwMediaRoute2ProviderService::class.java)
            context.startService(intent)
            FwLog.d("$TAG: MediaRoute2ProviderService 启动请求已发送")
        } catch (e: Exception) {
            FwLog.e("$TAG: 启动 MediaRoute2ProviderService 失败 - ${e.message}", e)
        }
    }

    /**
     * 停止 MediaRoute2ProviderService
     */
    private fun stopMediaRoute2ProviderService(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        try {
            val intent = Intent(context, FwMediaRoute2ProviderService::class.java)
            context.stopService(intent)
            FwLog.d("$TAG: MediaRoute2ProviderService 已停止")
        } catch (e: Exception) {
            FwLog.e("$TAG: 停止 MediaRoute2ProviderService 失败 - ${e.message}", e)
        }
    }

    /**
     * 获取模块是否已初始化
     */
    fun isModuleInitialized(): Boolean = isInitialized

    /**
     * 获取 Native 层是否可用
     */
    fun isNativeAvailable(): Boolean = FwMediaRouteNative.isAvailable()
}
