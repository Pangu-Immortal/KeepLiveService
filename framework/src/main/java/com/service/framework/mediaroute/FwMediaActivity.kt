/**
 * ============================================================================
 * FwMediaActivity.kt - 媒体意图处理 Activity
 * ============================================================================
 *
 * 功能简介：
 *   处理系统媒体相关的隐式意图，如 VIEW/SEND 媒体文件等。
 *   通过注册媒体类型的 intent-filter，使应用可以被系统广播唤醒。
 *
 * 核心机制：
 *   - 声明处理各种媒体 MIME 类型的能力
 *   - 接收系统的 ACTION_VIEW、ACTION_SEND 等意图
 *   - 收到意图后触发保活检查，然后静默关闭
 *
 * 安全研究要点：
 *   - 这是利用 Android 意图系统的保活技巧
 *   - 用户通过其他应用分享媒体时可能触发此 Activity
 *   - Activity 应快速完成，避免影响用户体验
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */
package com.service.framework.mediaroute

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 媒体意图处理 Activity
 *
 * 这是一个透明的、无界面的 Activity，用于：
 * 1. 接收系统的媒体相关意图
 * 2. 触发保活检查
 * 3. 快速关闭，不影响用户体验
 */
class FwMediaActivity : Activity() {

    companion object {
        private const val TAG = "FwMediaActivity"

        // 自动关闭延迟（毫秒）
        private const val AUTO_FINISH_DELAY = 100L
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Activity 创建时调用
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FwLog.d("$TAG: onCreate")

        // 处理意图
        handleIntent(intent)

        // 延迟关闭 Activity
        scheduleFinish()
    }

    /**
     * 新意图到达时调用
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        FwLog.d("$TAG: onNewIntent")

        intent?.let { handleIntent(it) }
    }

    /**
     * 处理意图
     *
     * @param intent 接收到的意图
     */
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type
        val data = intent.data

        FwLog.i("$TAG: 收到意图 - action=$action, type=$type, data=$data")

        // 根据不同的意图类型进行处理
        when (action) {
            Intent.ACTION_VIEW -> handleViewIntent(intent)
            Intent.ACTION_SEND -> handleSendIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleSendMultipleIntent(intent)
            else -> FwLog.d("$TAG: 未知意图类型 - action=$action")
        }

        // 触发保活检查
        triggerKeepAliveCheck("MediaIntent: $action")
    }

    /**
     * 处理 VIEW 意图
     */
    private fun handleViewIntent(intent: Intent) {
        val uri = intent.data
        val type = intent.type

        FwLog.d("$TAG: 处理 VIEW 意图 - uri=$uri, type=$type")
    }

    /**
     * 处理 SEND 意图
     */
    private fun handleSendIntent(intent: Intent) {
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        val type = intent.type

        FwLog.d("$TAG: 处理 SEND 意图 - uri=$uri, type=$type")
    }

    /**
     * 处理 SEND_MULTIPLE 意图
     */
    private fun handleSendMultipleIntent(intent: Intent) {
        val uriList = intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        val type = intent.type

        FwLog.d("$TAG: 处理 SEND_MULTIPLE 意图 - count=${uriList?.size}, type=$type")
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
        } else {
            FwLog.w("$TAG: Fw 框架未初始化")
        }
    }

    /**
     * 安排关闭 Activity
     */
    private fun scheduleFinish() {
        handler.postDelayed({
            FwLog.d("$TAG: 自动关闭 Activity")
            finishAndRemoveTask()
        }, AUTO_FINISH_DELAY)
    }

    /**
     * Activity 销毁时调用
     */
    override fun onDestroy() {
        FwLog.d("$TAG: onDestroy")
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
