/**
 * ============================================================================
 * MediaSessionNotificationManager.kt - MediaSession 通知豁免管理器
 * ============================================================================
 *
 * 功能简介：
 *   利用 Android 13+（API 33）的 MediaSession 通知豁免机制，在用户未授予
 *   POST_NOTIFICATIONS 权限的情况下，依然能够显示持久化通知。
 *
 *   Android 13 要求所有通知必须获得 POST_NOTIFICATIONS 权限，但与活跃的
 *   MediaSession 关联的 MediaStyle 通知完全豁免此限制。本管理器正是利用
 *   这一系统级豁免来保证通知栏的持久显示。
 *
 * 主要函数：
 *   - show(context): 在无通知权限时，通过 MediaSession 豁免显示通知
 *   - dismiss(context): 取消豁免通知并释放 MediaSession 资源
 *   - isNotificationPermissionGranted(context): 检查 POST_NOTIFICATIONS 权限状态
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * MediaSession 通知豁免管理器（单例）
 *
 * 在 Android 13+ 上，当用户拒绝 POST_NOTIFICATIONS 权限时，
 * 利用 MediaSession + MediaStyle 通知的系统豁免机制来显示持久通知。
 */
object MediaSessionNotificationManager {

    /** 豁免通知的唯一 ID，与 FwForegroundService 的 10001 区分 */
    private const val NOTIFICATION_ID = 10002

    /** 用于豁免的 MediaSession 实例 */
    private var mediaSession: MediaSessionCompat? = null

    /**
     * 显示 MediaSession 豁免通知
     *
     * 逻辑流程：
     * 1. 如果已有通知权限，走正常通知通道，无需豁免
     * 2. 仅在 API 33+ 且未获得权限时，构建 MediaSession + MediaStyle 通知
     *
     * @param context 上下文对象
     */
    fun show(context: Context) {
        FwLog.d("MediaSessionNotificationManager.show() 被调用")

        // 已有通知权限时，跳过豁免路径，由正常通知通道处理
        if (isNotificationPermissionGranted(context)) {
            FwLog.d("POST_NOTIFICATIONS 权限已授予，跳过 MediaSession 豁免路径")
            return
        }

        // 仅 Android 13+（API 33）才需要此豁免机制
        if (Build.VERSION.SDK_INT < 33) {
            FwLog.d("API < 33，不需要 MediaSession 通知豁免")
            return
        }

        try {
            // 确保通知渠道已创建
            ensureNotificationChannel(context)

            // 创建 MediaSession 并设置为活跃状态
            val session = MediaSessionCompat(context, "FwMediaSessionExempt") // 创建豁免专用 MediaSession
            mediaSession = session

            // 设置 MediaSession 为活跃状态，这是豁免的关键条件
            session.isActive = true
            FwLog.d("MediaSession 已创建并设为活跃状态")

            // 设置播放状态为 STATE_PLAYING，让系统认为正在播放媒体
            val playbackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING, // 播放中状态
                    0L,                                 // 播放位置
                    1.0f                                // 播放速度
                )
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE) // 支持的操作
                .build()
            session.setPlaybackState(playbackState) // 应用播放状态
            FwLog.d("MediaSession 播放状态已设置为 STATE_PLAYING")

            // 设置媒体元数据，标题取自框架配置
            val metadata = MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    Fw.config.notificationTitle // 使用配置中的通知标题作为媒体标题
                )
                .build()
            session.setMetadata(metadata) // 应用元数据
            FwLog.d("MediaSession 元数据已设置，标题: ${Fw.config.notificationTitle}")

            // 获取框架通知配置
            val config = Fw.config

            // 构建 MediaStyle 通知 —— 关联 MediaSession 是豁免的核心
            val notification = NotificationCompat.Builder(context, config.notificationChannelId)
                .setSmallIcon(config.notificationIconResId)       // 通知小图标
                .setContentTitle(config.notificationTitle)        // 通知标题
                .setContentText(config.notificationContent)       // 通知正文
                .setOngoing(true)                                 // 设为持久通知，用户无法滑动清除
                .setPriority(NotificationCompat.PRIORITY_LOW)     // 低优先级，避免打扰用户
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
                .setStyle(
                    MediaNotificationCompat.MediaStyle()
                        .setMediaSession(session.sessionToken)    // 关联 MediaSession Token，这是豁免的关键
                )
                .build()
            FwLog.d("MediaStyle 豁免通知已构建完成")

            // 通过 NotificationManager 发送通知
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification) // 发送通知，使用唯一 ID 10002

            FwLog.d("MediaSession 通知豁免：无需 POST_NOTIFICATIONS 权限即可显示通知")
        } catch (e: Exception) {
            FwLog.e("MediaSession 豁免通知显示失败", e)
        }
    }

    /**
     * 取消豁免通知并释放 MediaSession 资源
     *
     * @param context 上下文对象
     */
    fun dismiss(context: Context) {
        FwLog.d("MediaSessionNotificationManager.dismiss() 被调用")

        try {
            // 取消通知
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID) // 取消 ID 为 10002 的通知
            FwLog.d("豁免通知已取消，ID: $NOTIFICATION_ID")
        } catch (e: Exception) {
            FwLog.e("取消豁免通知失败", e)
        }

        // 释放 MediaSession 资源
        try {
            mediaSession?.let { session ->
                session.isActive = false // 先将 MediaSession 设为非活跃
                session.release()        // 释放 MediaSession 资源
                FwLog.d("豁免用 MediaSession 已释放")
            }
            mediaSession = null // 清空引用
        } catch (e: Exception) {
            FwLog.e("释放豁免 MediaSession 失败", e)
        }
    }

    /**
     * 检查 POST_NOTIFICATIONS 权限是否已授予
     *
     * - API < 33：始终返回 true（不需要此权限）
     * - API >= 33：检查 Manifest.permission.POST_NOTIFICATIONS 权限状态
     *
     * @param context 上下文对象
     * @return true 表示已授予权限或不需要权限，false 表示未授予
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        // Android 13 以下不需要 POST_NOTIFICATIONS 权限
        if (Build.VERSION.SDK_INT < 33) {
            FwLog.d("API ${Build.VERSION.SDK_INT} < 33，无需 POST_NOTIFICATIONS 权限")
            return true
        }

        // Android 13+ 检查 POST_NOTIFICATIONS 权限
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS // Android 13 新增的通知权限
        ) == PackageManager.PERMISSION_GRANTED

        FwLog.d("POST_NOTIFICATIONS 权限状态: ${if (granted) "已授予" else "未授予"}")
        return granted
    }

    /**
     * 确保通知渠道已创建
     *
     * 与 FwForegroundService 使用相同的渠道配置，避免重复创建
     * Android 8.0+（API 26）要求通知必须关联通知渠道
     *
     * @param context 上下文对象
     */
    private fun ensureNotificationChannel(context: Context) {
        // 仅 Android 8.0+（API 26）需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val config = Fw.config
            val channel = NotificationChannel(
                config.notificationChannelId,       // 渠道 ID，与 FwForegroundService 一致
                config.notificationChannelName,     // 渠道名称
                NotificationManager.IMPORTANCE_LOW  // 低重要性，不发出声音
            ).apply {
                description = "用于保持后台服务运行"  // 渠道描述
                setSound(null, null)                // 无声音
                enableLights(false)                 // 无灯光
                enableVibration(false)              // 无振动
                setShowBadge(false)                 // 不显示角标
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel) // 创建或更新通知渠道
            FwLog.d("通知渠道已确保创建: ${config.notificationChannelId}")
        }
    }
}
