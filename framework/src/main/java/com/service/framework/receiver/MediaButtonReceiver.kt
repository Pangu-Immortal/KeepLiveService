/**
 * ============================================================================
 * MediaButtonReceiver.kt - 媒体按键广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听媒体按键事件（播放、暂停、上一曲、下一曲等），包括蓝牙耳机按键触发的
 *   媒体按键事件，是音乐应用响应蓝牙耳机按键的标准方式。
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
import android.view.KeyEvent
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 媒体按键广播接收器
 *
 * 核心机制：
 * 1. 监听媒体按键事件（播放、暂停、上一曲、下一曲等）
 * 2. 蓝牙耳机的按键也会触发媒体按键事件
 * 3. 当用户按下蓝牙耳机按键时，系统会广播媒体按键事件
 *
 * 安全研究要点：
 * - 这是音乐应用响应蓝牙耳机按键的标准方式
 * - 静态注册可以在应用被杀后接收事件
 * - 但强制停止后也会被禁用
 * - 结合 MediaSession 使用效果更好
 */
class MediaButtonReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MEDIA_BUTTON = Intent.ACTION_MEDIA_BUTTON

        /**
         * 获取动态注册的 IntentFilter
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_MEDIA_BUTTON)
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action != ACTION_MEDIA_BUTTON) return

        val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (keyEvent == null) {
            FwLog.d("MediaButtonReceiver: 收到媒体按键事件，但 KeyEvent 为空")
            return
        }

        // 只处理按下事件，忽略抬起事件
        if (keyEvent.action != KeyEvent.ACTION_DOWN) return

        val keyCode = keyEvent.keyCode
        val keyName = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> "播放"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "暂停"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "播放/暂停"
            KeyEvent.KEYCODE_MEDIA_STOP -> "停止"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "下一曲"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "上一曲"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "快退"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "快进"
            KeyEvent.KEYCODE_HEADSETHOOK -> "耳机按键"
            else -> "未知按键($keyCode)"
        }

        FwLog.d("MediaButtonReceiver: 收到媒体按键 - $keyName")

        // 拉起服务
        ServiceStarter.startForegroundService(context, "媒体按键:$keyName")
    }
}
