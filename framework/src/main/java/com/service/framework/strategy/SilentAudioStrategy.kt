/**
 * ============================================================================
 * SilentAudioStrategy.kt - 静默音频播放策略
 * ============================================================================
 *
 * 功能简介：
 *   通过 MediaPlayer 循环播放无声音频文件，防止 CPU 休眠。
 *   屏幕关闭时开始播放，屏幕亮起时暂停播放以节省电量。
 *   此策略参考酷狗音乐、QQ音乐等主流音乐App的保活方案。
 *
 * 核心机制：
 *   - 播放 1 秒无声 WAV 文件（仅 8KB）
 *   - 音量设为 0，用户完全无感知
 *   - 屏幕灭屏时激活，亮屏时暂停
 *   - 根据 AggressiveLevel 控制播放间隔
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.1
 */
package com.service.framework.strategy

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.service.framework.R
import com.service.framework.core.AggressiveLevel
import com.service.framework.util.FwLog

object SilentAudioStrategy {

    private var mediaPlayer: MediaPlayer? = null // 媒体播放器实例
    private var isPlaying = false                 // 当前是否正在播放
    private var level: AggressiveLevel = AggressiveLevel.MEDIUM // 能耗等级
    private val handler = Handler(Looper.getMainLooper())       // 延迟重播用 Handler

    /**
     * 启动静默音频播放
     * @param context 上下文
     * @param aggressiveLevel 能耗等级，控制重播间隔
     */
    fun start(context: Context, aggressiveLevel: AggressiveLevel = AggressiveLevel.MEDIUM) {
        if (isPlaying) return // 已在播放则跳过
        level = aggressiveLevel
        FwLog.d("静默音频策略启动，能耗等级: $level")
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.silent)?.apply {
                setVolume(0f, 0f) // 音量设为 0，用户无感知
                setOnCompletionListener { onPlaybackComplete() } // 播放完成后重播
                start()
            }
            isPlaying = true
        } catch (e: Exception) {
            FwLog.e("静默音频启动失败", e)
        }
    }

    /**
     * 停止静默音频播放
     */
    fun stop() {
        FwLog.d("静默音频策略停止")
        isPlaying = false
        handler.removeCallbacksAndMessages(null) // 取消所有延迟任务
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop() // 停止播放
                it.release()                // 释放资源
            }
        } catch (e: Exception) {
            FwLog.e("静默音频停止异常", e)
        }
        mediaPlayer = null
    }

    /**
     * 暂停播放（屏幕亮起时调用）
     */
    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    FwLog.d("静默音频已暂停（屏幕亮起）")
                }
            }
        } catch (e: Exception) {
            FwLog.e("静默音频暂停异常", e)
        }
    }

    /**
     * 恢复播放（屏幕关闭时调用）
     */
    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                FwLog.d("静默音频已恢复（屏幕关闭）")
            }
        } catch (e: Exception) {
            FwLog.e("静默音频恢复异常", e)
        }
    }

    fun isActive(): Boolean = isPlaying

    /**
     * 播放完成回调，根据能耗等级决定重播间隔
     */
    private fun onPlaybackComplete() {
        if (!isPlaying) return // 已停止则不重播
        val delay = when (level) {
            AggressiveLevel.LOW -> 10_000L    // 低：10秒间隔
            AggressiveLevel.MEDIUM -> 5_000L  // 中：5秒间隔
            AggressiveLevel.HIGH -> 0L        // 高：立即重播
        }
        if (delay > 0) {
            handler.postDelayed({ replayIfActive() }, delay) // 延迟重播
        } else {
            replayIfActive() // 立即重播
        }
    }

    /**
     * 如果策略仍处于激活状态则重新播放
     */
    private fun replayIfActive() {
        if (!isPlaying) return
        try {
            mediaPlayer?.let {
                it.seekTo(0)  // 回到开头
                it.start()    // 重新播放
            }
        } catch (e: Exception) {
            FwLog.e("静默音频重播异常", e)
        }
    }
}
