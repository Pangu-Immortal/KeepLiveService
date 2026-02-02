/**
 * ============================================================================
 * FwMediaRoute2ProviderService.kt - MediaRouter2 路由提供者服务
 * ============================================================================
 *
 * 功能简介：
 *   Android R (API 30) 引入的新媒体路由 API。
 *   提供比 MediaRouteProviderService 更现代化的路由发现和控制能力。
 *
 * 核心机制：
 *   - 继承 MediaRoute2ProviderService（需要 API 30+）
 *   - 向系统注册 MediaRoute2 路由描述
 *   - 双重保活：与旧版 MediaRouteProviderService 并存
 *
 * 安全研究要点：
 *   - 这是 Android 11+ 的新 API
 *   - 某些厂商 ROM 对此 API 有额外限制
 *   - 与旧版 API 并存可提高兼容性
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.content.Intent
import android.media.MediaRoute2Info
import android.media.MediaRoute2ProviderService
import android.media.RouteDiscoveryPreference
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * MediaRoute2 提供者服务
 *
 * Android 11 (API 30) 引入的新媒体路由 API，提供：
 * 1. 更精细的路由类型控制
 * 2. 更好的系统集成
 * 3. 支持动态路由发现
 *
 * 此服务与 FwMediaRouteProviderService 并存，实现双重保活。
 */
@RequiresApi(Build.VERSION_CODES.R)
class FwMediaRoute2ProviderService : MediaRoute2ProviderService() {

    companion object {
        private const val TAG = "FwMediaRoute2ProviderService"

        // 虚拟路由ID
        private const val ROUTE_ID = "fw_media_route2"

        // 虚拟路由名称
        private const val ROUTE_NAME = "Fw Media Route 2"

        // 心跳间隔（毫秒）
        private const val HEARTBEAT_INTERVAL = 30_000L

        // 路由功能类型
        private const val ROUTE_FEATURE_LIVE_AUDIO = "android.media.route.feature.LIVE_AUDIO"
        private const val ROUTE_FEATURE_LIVE_VIDEO = "android.media.route.feature.LIVE_VIDEO"
        private const val ROUTE_FEATURE_REMOTE_PLAYBACK = "android.media.route.feature.REMOTE_PLAYBACK"
    }

    // 心跳 Handler
    private val heartbeatHandler = Handler(Looper.getMainLooper())

    // 心跳 Runnable
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            performHeartbeat()
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    /**
     * 服务创建时调用
     */
    override fun onCreate() {
        super.onCreate()
        FwLog.i("$TAG: onCreate - MediaRoute2 服务启动")

        // 发布路由
        publishRoutes()

        // 启动心跳
        startHeartbeat()

        // 通知 Native 层
        notifyNativeServiceStarted()
    }

    /**
     * 服务绑定时调用
     */
    override fun onBind(intent: Intent): IBinder? {
        FwLog.d("$TAG: onBind - action=${intent.action}")

        // 每次绑定时触发保活检查
        triggerKeepAliveCheck("onBind")

        return super.onBind(intent)
    }

    /**
     * 服务启动命令
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("$TAG: onStartCommand - action=${intent?.action}")

        // 触发保活检查
        triggerKeepAliveCheck("onStartCommand")

        return START_STICKY
    }

    /**
     * 服务销毁时调用
     */
    override fun onDestroy() {
        FwLog.w("$TAG: onDestroy - MediaRoute2 服务被销毁")

        // 停止心跳
        stopHeartbeat()

        // 通知 Native 层
        notifyNativeServiceStopped()

        // 尝试重启
        tryRestartService()

        super.onDestroy()
    }

    /**
     * 会话创建请求
     * 注意：此方法的签名在不同 Android 版本中有所不同
     */
    override fun onCreateSession(
        requestId: Long,
        packageName: String,
        routeId: String,
        sessionHints: Bundle?
    ) {
        FwLog.d("$TAG: onCreateSession - requestId=$requestId, packageName=$packageName, routeId=$routeId")
        // 虚拟路由，不创建真实会话，触发保活检查
        triggerKeepAliveCheck("onCreateSession")
    }

    /**
     * 会话释放请求
     */
    override fun onReleaseSession(sessionId: Long, routeId: String) {
        FwLog.d("$TAG: onReleaseSession - sessionId=$sessionId, routeId=$routeId")
        // 虚拟会话，无需实际操作
    }

    /**
     * 选择路由
     */
    override fun onSelectRoute(sessionId: Long, routeId: String, routeIdToDeselect: String) {
        FwLog.d("$TAG: onSelectRoute - sessionId=$sessionId, routeId=$routeId")
        // 虚拟路由，触发保活检查
        triggerKeepAliveCheck("onSelectRoute")
    }

    /**
     * 取消选择路由
     */
    override fun onDeselectRoute(sessionId: Long, routeId: String, routeIdToSelect: String) {
        FwLog.d("$TAG: onDeselectRoute - sessionId=$sessionId, routeId=$routeId")
    }

    /**
     * 传输到路由
     */
    override fun onTransferToRoute(sessionId: Long, routeId: String, routeIdToDeselect: String) {
        FwLog.d("$TAG: onTransferToRoute - sessionId=$sessionId, routeId=$routeId")
    }

    /**
     * 设置路由音量
     */
    override fun onSetRouteVolume(sessionId: Long, routeId: String, volume: Int) {
        FwLog.d("$TAG: onSetRouteVolume - routeId=$routeId, volume=$volume")
    }

    /**
     * 设置会话音量
     */
    override fun onSetSessionVolume(sessionId: Long, routeId: String, volume: Int) {
        FwLog.d("$TAG: onSetSessionVolume - sessionId=$sessionId, volume=$volume")
    }

    /**
     * 发现请求变化
     */
    override fun onDiscoveryPreferenceChanged(preference: RouteDiscoveryPreference) {
        FwLog.d("$TAG: onDiscoveryPreferenceChanged")
        // 重新发布路由
        publishRoutes()
    }

    /**
     * 发布虚拟媒体路由
     */
    private fun publishRoutes() {
        try {
            // 构建路由信息
            val routeInfo = MediaRoute2Info.Builder(ROUTE_ID, ROUTE_NAME)
                .addFeature(ROUTE_FEATURE_LIVE_AUDIO)      // 实时音频
                .addFeature(ROUTE_FEATURE_LIVE_VIDEO)      // 实时视频
                .addFeature(ROUTE_FEATURE_REMOTE_PLAYBACK) // 远程播放
                .setVolumeMax(100)
                .setVolume(50)
                .setVolumeHandling(MediaRoute2Info.PLAYBACK_VOLUME_VARIABLE)
                .setConnectionState(MediaRoute2Info.CONNECTION_STATE_DISCONNECTED)
                .build()

            // 通知系统路由变化
            notifyRoutes(listOf(routeInfo))

            FwLog.d("$TAG: 虚拟 MediaRoute2 路由已发布")
        } catch (e: Exception) {
            FwLog.e("$TAG: 发布 MediaRoute2 路由失败 - ${e.message}", e)
        }
    }

    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        FwLog.d("$TAG: 启动心跳 - 间隔=${HEARTBEAT_INTERVAL}ms")
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    /**
     * 停止心跳
     */
    private fun stopHeartbeat() {
        FwLog.d("$TAG: 停止心跳")
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }

    /**
     * 执行心跳
     */
    private fun performHeartbeat() {
        FwLog.d("$TAG: 心跳执行")
        if (Fw.isInitialized()) {
            Fw.check()
        }
    }

    /**
     * 触发保活检查
     */
    private fun triggerKeepAliveCheck(source: String) {
        FwLog.d("$TAG: 触发保活检查 - 来源=$source")
        if (Fw.isInitialized()) {
            Fw.check()
        }
    }

    /**
     * 通知 Native 层服务已启动
     */
    private fun notifyNativeServiceStarted() {
        try {
            FwMediaRouteNative.onService2Started(packageName, javaClass.name)
        } catch (e: Exception) {
            FwLog.e("$TAG: 通知 Native 层失败 - ${e.message}", e)
        }
    }

    /**
     * 通知 Native 层服务已停止
     */
    private fun notifyNativeServiceStopped() {
        try {
            FwMediaRouteNative.onService2Stopped()
        } catch (e: Exception) {
            FwLog.e("$TAG: 通知 Native 层失败 - ${e.message}", e)
        }
    }

    /**
     * 尝试重启服务
     */
    private fun tryRestartService() {
        FwLog.d("$TAG: 尝试重启服务")
        try {
            val intent = Intent(this, FwMediaRoute2ProviderService::class.java)
            startService(intent)
        } catch (e: Exception) {
            FwLog.e("$TAG: 重启服务失败 - ${e.message}", e)
        }
    }
}
