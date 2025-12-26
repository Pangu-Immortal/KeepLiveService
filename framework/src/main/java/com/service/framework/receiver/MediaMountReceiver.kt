/**
 * ============================================================================
 * MediaMountReceiver.kt - 媒体/存储广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听外部存储（SD卡/U盘）挂载/卸载事件及媒体扫描完成事件，
 *   适用于文件管理器、相册应用、备份应用、媒体播放器等场景。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

package com.service.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 媒体/存储广播接收器
 *
 * 核心机制：
 * 1. 监听外部存储（SD卡/U盘）挂载/卸载事件
 * 2. 监听媒体扫描完成事件
 * 3. 这些广播可以静态注册
 *
 * 安全研究要点：
 * - 媒体挂载事件是可以静态注册的重要广播
 * - SD 卡插拔、U盘连接都会触发
 * - 媒体扫描完成也可以触发（用户拍照、下载等）
 * - 文件管理器、相册应用常用此机制
 *
 * 使用场景：
 * - 文件管理器
 * - 相册应用
 * - 备份应用
 * - 媒体播放器
 */
class MediaMountReceiver : BroadcastReceiver() {

    companion object {
        // 媒体挂载相关
        const val ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED"
        const val ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED"
        const val ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT"
        const val ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED"
        const val ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL"
        const val ACTION_MEDIA_CHECKING = "android.intent.action.MEDIA_CHECKING"
        const val ACTION_MEDIA_NOFS = "android.intent.action.MEDIA_NOFS"
        const val ACTION_MEDIA_SHARED = "android.intent.action.MEDIA_SHARED"
        const val ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED"

        // 媒体扫描相关
        const val ACTION_MEDIA_SCANNER_STARTED = "android.intent.action.MEDIA_SCANNER_STARTED"
        const val ACTION_MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED"
        const val ACTION_MEDIA_SCANNER_SCAN_FILE = "android.intent.action.MEDIA_SCANNER_SCAN_FILE"

        // 媒体按钮（重复定义，但放在这里便于理解）
        const val ACTION_MEDIA_BUTTON = "android.intent.action.MEDIA_BUTTON"

        /**
         * 获取动态注册的 IntentFilter（媒体挂载需要 data scheme）
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_MEDIA_MOUNTED)
                addAction(ACTION_MEDIA_UNMOUNTED)
                addAction(ACTION_MEDIA_EJECT)
                addAction(ACTION_MEDIA_REMOVED)
                addAction(ACTION_MEDIA_BAD_REMOVAL)
                addAction(ACTION_MEDIA_CHECKING)
                addAction(ACTION_MEDIA_NOFS)
                addAction(ACTION_MEDIA_SHARED)
                addAction(ACTION_MEDIA_UNSHARED)
                addAction(ACTION_MEDIA_SCANNER_STARTED)
                addAction(ACTION_MEDIA_SCANNER_FINISHED)
                addAction(ACTION_MEDIA_SCANNER_SCAN_FILE)

                // 媒体挂载广播需要 file scheme
                addDataScheme("file")
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val data = intent.data?.path ?: "未知路径"

        FwLog.d("MediaMountReceiver: 收到广播 - $action")
        FwLog.d("  - 路径: $data")

        when (action) {
            ACTION_MEDIA_MOUNTED -> {
                FwLog.d("MediaMountReceiver: 存储已挂载 - $data")
                ServiceStarter.startForegroundService(context, "存储已挂载")
            }
            ACTION_MEDIA_UNMOUNTED -> {
                FwLog.d("MediaMountReceiver: 存储已卸载 - $data")
            }
            ACTION_MEDIA_EJECT -> {
                FwLog.d("MediaMountReceiver: 存储即将弹出 - $data")
            }
            ACTION_MEDIA_REMOVED -> {
                FwLog.d("MediaMountReceiver: 存储已移除 - $data")
            }
            ACTION_MEDIA_BAD_REMOVAL -> {
                FwLog.d("MediaMountReceiver: 存储异常移除（未安全弹出）- $data")
                ServiceStarter.startForegroundService(context, "存储异常移除")
            }
            ACTION_MEDIA_CHECKING -> {
                FwLog.d("MediaMountReceiver: 正在检查存储 - $data")
            }
            ACTION_MEDIA_NOFS -> {
                FwLog.d("MediaMountReceiver: 存储无文件系统 - $data")
            }
            ACTION_MEDIA_SHARED -> {
                FwLog.d("MediaMountReceiver: 存储已共享（USB 大容量存储模式）- $data")
            }
            ACTION_MEDIA_UNSHARED -> {
                FwLog.d("MediaMountReceiver: 存储已取消共享 - $data")
                ServiceStarter.startForegroundService(context, "存储取消共享")
            }
            ACTION_MEDIA_SCANNER_STARTED -> {
                FwLog.d("MediaMountReceiver: 媒体扫描开始 - $data")
            }
            ACTION_MEDIA_SCANNER_FINISHED -> {
                FwLog.d("MediaMountReceiver: 媒体扫描完成 - $data")
                ServiceStarter.startForegroundService(context, "媒体扫描完成")
            }
            ACTION_MEDIA_SCANNER_SCAN_FILE -> {
                FwLog.d("MediaMountReceiver: 单文件扫描 - $data")
            }
        }
    }
}
