/**
 * ============================================================================
 * FwMediaBrowserService.kt - MediaBrowserService 保活策略
 * ============================================================================
 *
 * 功能简介：
 *   提供一个轻量级 MediaBrowserServiceCompat，吸收逆向 SDK 中媒体服务保活
 *   的公开系统服务路线。该服务只暴露空媒体目录，不播放真实媒体，用于让
 *   系统媒体框架能够识别和绑定应用的媒体服务能力。
 *
 * 主要函数：
 *   - start(): 请求启动媒体浏览服务
 *   - stop(): 停止媒体浏览服务
 *   - isRunning(): 查询服务运行状态
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.service.framework.health.FwStrategyKey
import com.service.framework.health.FwStrategyStateManager
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 轻量媒体浏览服务。
 */
class FwMediaBrowserService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "FwMediaBrowser"
        private const val ROOT_ID = "fw_media_root"

        @Volatile
        private var running = false

        /**
         * 启动媒体浏览服务。
         */
        fun start(context: Context) {
            FwLog.d("$TAG: 请求启动 MediaBrowserService")
            try {
                val intent = Intent(context, FwMediaBrowserService::class.java)
                context.startService(intent)
                FwStrategyStateManager.markStarted(FwStrategyKey.MEDIA_BROWSER, "startService")
            } catch (e: Exception) {
                FwStrategyStateManager.markError(FwStrategyKey.MEDIA_BROWSER, e.message ?: "启动失败", e)
                FwLog.e("$TAG: 启动 MediaBrowserService 失败: ${e.message}", e)
            }
        }

        /**
         * 停止媒体浏览服务。
         */
        fun stop(context: Context) {
            FwLog.d("$TAG: 请求停止 MediaBrowserService")
            try {
                val intent = Intent(context, FwMediaBrowserService::class.java)
                context.stopService(intent)
                running = false
                FwStrategyStateManager.markStopped(FwStrategyKey.MEDIA_BROWSER, "stopService")
            } catch (e: Exception) {
                FwStrategyStateManager.markError(FwStrategyKey.MEDIA_BROWSER, e.message ?: "停止失败", e)
                FwLog.e("$TAG: 停止 MediaBrowserService 失败: ${e.message}", e)
            }
        }

        /**
         * 检查服务是否运行。
         */
        fun isRunning(): Boolean = running
    }

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        FwLog.d("$TAG: onCreate - 媒体浏览服务初始化")
        mediaSession = MediaSessionCompat(this, TAG).apply {
            isActive = true
        }
        mediaSession?.sessionToken?.let { token ->
            setSessionToken(token)
        }
        FwStrategyStateManager.markStarted(FwStrategyKey.MEDIA_BROWSER, "onCreate")
        ServiceStarter.startForegroundService(this, "MediaBrowserService连接")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        FwLog.d("$TAG: onGetRoot - client=$clientPackageName, uid=$clientUid")
        FwStrategyStateManager.markTriggered(FwStrategyKey.MEDIA_BROWSER, "onGetRoot:$clientPackageName")
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        FwLog.d("$TAG: onLoadChildren - parentId=$parentId")
        FwStrategyStateManager.markTriggered(FwStrategyKey.MEDIA_BROWSER, "onLoadChildren:$parentId")
        result.sendResult(mutableListOf())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("$TAG: onStartCommand - startId=$startId")
        FwStrategyStateManager.markTriggered(FwStrategyKey.MEDIA_BROWSER, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        FwLog.w("$TAG: onDestroy - 媒体浏览服务被销毁")
        mediaSession?.release()
        mediaSession = null
        running = false
        FwStrategyStateManager.markStopped(FwStrategyKey.MEDIA_BROWSER, "onDestroy")
        super.onDestroy()
    }
}
