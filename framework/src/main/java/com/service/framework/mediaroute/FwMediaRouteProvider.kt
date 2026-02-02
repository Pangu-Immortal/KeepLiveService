/**
 * ============================================================================
 * FwMediaRouteProvider.kt - 媒体路由提供者
 * ============================================================================
 *
 * 功能简介：
 *   自定义 MediaRouteProvider，用于向系统注册虚拟媒体路由。
 *   通过声明虚拟的投屏/音频路由，使应用获得"媒体类应用"身份。
 *
 * 核心机制：
 *   - 继承 MediaRouteProvider 实现自定义路由
 *   - 声明 CATEGORY_LIVE_AUDIO 和 CATEGORY_LIVE_VIDEO 能力
 *   - 系统会认为此应用具备媒体投屏能力，提升进程优先级
 *
 * 安全研究要点：
 *   - 这是酷狗音乐等App的核心保活技术之一
 *   - 利用了系统对"媒体类应用"的特殊优待
 *   - 不会真正进行投屏，只是声明能力
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteDescriptor
import androidx.mediarouter.media.MediaRouteDiscoveryRequest
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderDescriptor
import androidx.mediarouter.media.MediaRouter
import com.service.framework.util.FwLog

/**
 * 媒体路由提供者
 *
 * 通过向系统注册虚拟媒体路由，使应用获得"媒体类应用"的身份标识。
 * 系统对媒体类应用有特殊优待：
 * 1. 允许通过蓝牙广播唤醒
 * 2. 后台进程优先级提升
 * 3. 系统清理时更不容易被杀死
 *
 * @param context 应用上下文
 */
class FwMediaRouteProvider(context: Context) : MediaRouteProvider(context) {

    companion object {
        private const val TAG = "FwMediaRouteProvider"

        // 虚拟路由ID
        private const val ROUTE_ID = "fw_media_route"

        // 虚拟路由名称（用户不可见）
        private const val ROUTE_NAME = "Fw Media Route"

        // 虚拟路由描述
        private const val ROUTE_DESCRIPTION = "Framework Keep-Alive Media Route"
    }

    // 是否正在发现设备
    private var isDiscovering = false

    init {
        FwLog.d("$TAG: MediaRouteProvider 初始化")
        // 初始化时立即发布路由描述
        publishRoutes()
    }

    /**
     * 当路由发现请求变化时调用
     *
     * @param request 发现请求，可能为null表示停止发现
     */
    override fun onDiscoveryRequestChanged(request: MediaRouteDiscoveryRequest?) {
        FwLog.d("$TAG: onDiscoveryRequestChanged - request=$request")

        isDiscovering = request != null && request.isActiveScan

        // 无论是否在发现，都保持路由发布
        publishRoutes()
    }

    /**
     * 发布虚拟媒体路由
     *
     * 创建并发布一个虚拟的媒体路由描述，包含音频和视频投屏能力。
     * 这会让系统认为此应用是一个媒体类应用。
     */
    private fun publishRoutes() {
        try {
            // 构建路由支持的媒体控制类型
            val audioFilter = IntentFilter().apply {
                addCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)  // 实时音频投屏
            }

            val videoFilter = IntentFilter().apply {
                addCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)  // 实时视频投屏
            }

            val remotePlaybackFilter = IntentFilter().apply {
                addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)  // 远程播放
            }

            // 构建路由描述
            val routeDescriptor = MediaRouteDescriptor.Builder(ROUTE_ID, ROUTE_NAME)
                .setDescription(ROUTE_DESCRIPTION)
                .addControlFilter(audioFilter)      // 添加音频能力
                .addControlFilter(videoFilter)      // 添加视频能力
                .addControlFilter(remotePlaybackFilter)  // 添加远程播放能力
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)  // 远程播放类型
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)  // 可变音量
                .setVolumeMax(100)  // 最大音量
                .setVolume(50)      // 当前音量
                .setEnabled(true)   // 启用路由
                .setConnecting(false)  // 不处于连接中状态
                .build()

            // 构建提供者描述
            val providerDescriptor = MediaRouteProviderDescriptor.Builder()
                .addRoute(routeDescriptor)
                .build()

            // 发布到系统
            descriptor = providerDescriptor

            FwLog.d("$TAG: 虚拟媒体路由已发布")
        } catch (e: Exception) {
            FwLog.e("$TAG: 发布媒体路由失败 - ${e.message}", e)
        }
    }

    /**
     * 创建路由控制器
     *
     * 当用户选择此路由时会调用，返回一个控制器实例。
     * 由于是虚拟路由，实际不需要真正的控制逻辑。
     *
     * @param routeId 路由ID
     * @return 路由控制器
     */
    override fun onCreateRouteController(routeId: String): RouteController? {
        FwLog.d("$TAG: onCreateRouteController - routeId=$routeId")
        return if (routeId == ROUTE_ID) {
            FwMediaRouteController()
        } else {
            null
        }
    }

    /**
     * 虚拟路由控制器
     *
     * 处理路由的选择、取消选择和音量控制等操作。
     * 由于是虚拟路由，这些方法基本是空实现。
     */
    private inner class FwMediaRouteController : RouteController() {

        override fun onSelect() {
            FwLog.d("$TAG: RouteController onSelect")
        }

        override fun onUnselect() {
            FwLog.d("$TAG: RouteController onUnselect")
        }

        override fun onUnselect(reason: Int) {
            FwLog.d("$TAG: RouteController onUnselect - reason=$reason")
        }

        override fun onSetVolume(volume: Int) {
            FwLog.d("$TAG: RouteController onSetVolume - volume=$volume")
        }

        override fun onUpdateVolume(delta: Int) {
            FwLog.d("$TAG: RouteController onUpdateVolume - delta=$delta")
        }

        override fun onControlRequest(intent: Intent, callback: MediaRouter.ControlRequestCallback?): Boolean {
            FwLog.d("$TAG: RouteController onControlRequest - intent=$intent")
            // 返回false表示不处理此控制请求
            return false
        }

        override fun onRelease() {
            FwLog.d("$TAG: RouteController onRelease")
        }
    }
}
