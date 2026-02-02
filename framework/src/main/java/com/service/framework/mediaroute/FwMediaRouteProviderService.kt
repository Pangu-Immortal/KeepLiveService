/**
 * ============================================================================
 * FwMediaRouteProviderService.kt - 媒体路由提供者服务
 * ============================================================================
 *
 * 功能简介：
 *   继承 MediaRouteProviderService，向系统注册自定义的媒体路由提供者。
 *   这是酷狗音乐等App实现保活的核心服务组件。
 *
 * 核心机制：
 *   - 作为 Service 在后台运行
 *   - 向 MediaRouter 系统服务注册 FwMediaRouteProvider
 *   - 系统会维护此服务的连接，不容易被杀死
 *   - 通过 Handler.postDelayed 实现周期性心跳
 *
 * 安全研究要点：
 *   - MediaRouteProviderService 是 Android 官方 API
 *   - 但将其用于保活目的属于"创意性滥用"
 *   - 此机制在用户强制停止应用后会失效
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.mediarouter.media.MediaRouteProvider
import androidx.mediarouter.media.MediaRouteProviderService
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 媒体路由提供者服务
 *
 * 这是保活框架的核心服务之一，通过以下机制实现保活：
 * 1. 向系统注册媒体路由提供者，获得"媒体类应用"身份
 * 2. 系统会维护与此服务的 Binder 连接
 * 3. 周期性心跳检查，确保框架存活
 * 4. 服务被杀死后通过 onStartCommand 返回值请求重启
 *
 * 配置要求：
 * 在 AndroidManifest.xml 中需要声明此服务，并配置 intent-filter：
 * ```xml
 * <service
 *     android:name=".mediaroute.FwMediaRouteProviderService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.media.MediaRouteProviderService" />
 *     </intent-filter>
 * </service>
 * ```
 */
class FwMediaRouteProviderService : MediaRouteProviderService() {

    companion object {
        private const val TAG = "FwMediaRouteProviderService"

        // 心跳间隔（毫秒）
        private const val HEARTBEAT_INTERVAL = 30_000L

        // WakeLock 检查间隔（毫秒）
        private const val WAKELOCK_CHECK_INTERVAL = 60_000L
    }

    // 心跳 Handler
    private val heartbeatHandler = Handler(Looper.getMainLooper())

    // 媒体路由提供者实例
    private var mediaRouteProvider: FwMediaRouteProvider? = null

    // 心跳 Runnable
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            performHeartbeat()
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    // WakeLock 检查 Runnable
    private val wakeLockCheckRunnable = object : Runnable {
        override fun run() {
            checkWakeLock()
            heartbeatHandler.postDelayed(this, WAKELOCK_CHECK_INTERVAL)
        }
    }

    /**
     * 服务创建时调用
     */
    override fun onCreate() {
        super.onCreate()
        FwLog.i("$TAG: onCreate - 媒体路由服务启动")

        // 启动心跳
        startHeartbeat()

        // 启动 WakeLock 检查
        startWakeLockCheck()

        // 通知 Native 层服务已启动
        notifyNativeServiceStarted()
    }

    /**
     * 创建媒体路由提供者
     *
     * 这是 MediaRouteProviderService 的核心方法，系统会调用此方法获取路由提供者。
     *
     * @return 媒体路由提供者实例
     */
    override fun onCreateMediaRouteProvider(): MediaRouteProvider {
        FwLog.d("$TAG: onCreateMediaRouteProvider")
        mediaRouteProvider = FwMediaRouteProvider(this)
        return mediaRouteProvider!!
    }

    /**
     * 服务绑定时调用
     *
     * @param intent 绑定意图
     * @return Binder 对象
     */
    override fun onBind(intent: Intent): IBinder? {
        FwLog.d("$TAG: onBind - action=${intent.action}")

        // 每次绑定时触发保活检查
        triggerKeepAliveCheck("onBind")

        return super.onBind(intent)
    }

    /**
     * 服务启动命令
     *
     * @param intent 启动意图
     * @param flags 启动标志
     * @param startId 启动ID
     * @return 服务重启策略
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("$TAG: onStartCommand - action=${intent?.action}, flags=$flags, startId=$startId")

        // 触发保活检查
        triggerKeepAliveCheck("onStartCommand")

        // 返回 START_STICKY 确保服务被杀死后会重启
        return START_STICKY
    }

    /**
     * 服务销毁时调用
     */
    override fun onDestroy() {
        FwLog.w("$TAG: onDestroy - 媒体路由服务被销毁")

        // 停止心跳
        stopHeartbeat()

        // 停止 WakeLock 检查
        stopWakeLockCheck()

        // 通知 Native 层服务已停止
        notifyNativeServiceStopped()

        // 尝试重启服务
        tryRestartService()

        super.onDestroy()
    }

    /**
     * 低内存时调用
     */
    override fun onLowMemory() {
        super.onLowMemory()
        FwLog.w("$TAG: onLowMemory - 系统内存不足")
    }

    /**
     * 内存清理等级回调
     *
     * @param level 清理等级
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        FwLog.d("$TAG: onTrimMemory - level=$level")

        // 高级别清理时触发保活检查
        if (level >= TRIM_MEMORY_MODERATE) {
            triggerKeepAliveCheck("onTrimMemory")
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
     *
     * 心跳的主要作用：
     * 1. 保持 Handler 消息队列活跃
     * 2. 定期检查框架状态
     * 3. 必要时触发保活策略
     */
    private fun performHeartbeat() {
        FwLog.d("$TAG: 心跳执行")

        // 检查 Fw 框架状态
        if (Fw.isInitialized()) {
            // 触发框架检查
            Fw.check()
        } else {
            FwLog.w("$TAG: Fw 框架未初始化，跳过心跳检查")
        }
    }

    /**
     * 启动 WakeLock 检查
     */
    private fun startWakeLockCheck() {
        FwLog.d("$TAG: 启动 WakeLock 检查 - 间隔=${WAKELOCK_CHECK_INTERVAL}ms")
        heartbeatHandler.postDelayed(wakeLockCheckRunnable, WAKELOCK_CHECK_INTERVAL)
    }

    /**
     * 停止 WakeLock 检查
     */
    private fun stopWakeLockCheck() {
        FwLog.d("$TAG: 停止 WakeLock 检查")
        heartbeatHandler.removeCallbacks(wakeLockCheckRunnable)
    }

    /**
     * 检查 WakeLock
     *
     * 通过 Native 层获取和释放 WakeLock，保持 CPU 活跃
     */
    private fun checkWakeLock() {
        FwLog.d("$TAG: WakeLock 检查")
        // 调用 Native 层的 WakeLock 检查
        FwMediaRouteNative.checkWakeLock()
    }

    /**
     * 触发保活检查
     *
     * @param source 触发来源
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
            FwMediaRouteNative.onServiceStarted(packageName, javaClass.name)
        } catch (e: Exception) {
            FwLog.e("$TAG: 通知 Native 层失败 - ${e.message}", e)
        }
    }

    /**
     * 通知 Native 层服务已停止
     */
    private fun notifyNativeServiceStopped() {
        try {
            FwMediaRouteNative.onServiceStopped()
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
            val intent = Intent(this, FwMediaRouteProviderService::class.java)
            startService(intent)
        } catch (e: Exception) {
            FwLog.e("$TAG: 重启服务失败 - ${e.message}", e)
        }
    }
}
