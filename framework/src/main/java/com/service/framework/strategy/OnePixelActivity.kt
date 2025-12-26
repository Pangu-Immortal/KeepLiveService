/**
 * ============================================================================
 * OnePixelActivity.kt - 1 像素 Activity 保活策略
 * ============================================================================
 *
 * 功能简介：
 *   在屏幕关闭时启动 1x1 像素的透明 Activity，提升进程优先级。
 *
 * 核心机制：
 *   - 屏幕关闭时启动 1x1 像素透明 Activity
 *   - 利用前台 Activity 提升进程优先级
 *   - 屏幕点亮时自动关闭
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 1 像素 Activity
 *
 * 核心机制：
 * 1. 在屏幕关闭时启动一个 1x1 像素的透明 Activity
 * 2. 这样可以提升进程优先级（前台进程不易被杀）
 * 3. 屏幕点亮时关闭这个 Activity
 *
 * 安全研究要点：
 * - 这是一个经典的保活技巧
 * - 利用了 Android 进程优先级机制
 * - 有前台 Activity 的进程优先级最高
 * - 1 像素 Activity 对用户几乎不可见
 * - 现代 Android 版本对此有一定检测和限制
 * - MIUI 等定制系统可能有专门的检测机制
 */
class OnePixelActivity : Activity() {

    companion object {
        private var instance: OnePixelActivity? = null

        /**
         * 启动 1 像素 Activity
         */
        fun start(context: Context) {
            val config = Fw.config ?: return
            if (!config.enableOnePixelActivity) return

            try {
                val intent = Intent(context, OnePixelActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
                FwLog.d("启动 1 像素 Activity")
            } catch (e: Exception) {
                FwLog.e("启动 1 像素 Activity 失败: ${e.message}", e)
            }
        }

        /**
         * 关闭 1 像素 Activity
         */
        fun finish() {
            try {
                instance?.finish()
                FwLog.d("关闭 1 像素 Activity")
            } catch (e: Exception) {
                FwLog.e("关闭 1 像素 Activity 失败: ${e.message}", e)
            }
        }

        /**
         * 检查屏幕是否关闭
         */
        fun isScreenOff(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isInteractive == false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        // 设置窗口属性
        setupWindow()

        FwLog.d("1 像素 Activity onCreate")
    }

    override fun onResume() {
        super.onResume()

        // 如果屏幕已经点亮，关闭自己
        if (!isScreenOff(this)) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        FwLog.d("1 像素 Activity onDestroy")
    }

    /**
     * 设置窗口属性
     */
    private fun setupWindow() {
        // 请求无标题窗口
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.apply {
            // 设置窗口背景透明
            setBackgroundDrawableResource(android.R.color.transparent)

            // 设置窗口属性
            val layoutParams = attributes.apply {
                // 设置为 1x1 像素
                width = 1
                height = 1

                // 设置到屏幕左上角
                gravity = Gravity.START or Gravity.TOP
                x = 0
                y = 0

                // 设置窗口类型和标志
                flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                // 设置透明度为 0（完全透明）
                alpha = 0f
            }

            attributes = layoutParams

            // 设置窗口透明
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }
}
