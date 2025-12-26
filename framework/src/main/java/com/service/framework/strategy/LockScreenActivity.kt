/**
 * ============================================================================
 * LockScreenActivity.kt - 锁屏界面保活 Activity
 * ============================================================================
 *
 * 功能简介：
 *   实现锁屏界面上的 Activity 显示，通过在锁屏上显示内容来提升进程优先级。
 *   支持锁屏时显示、解锁键盘锁、保持屏幕常亮等功能。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter
import java.lang.ref.WeakReference

/**
 * 锁屏 Activity
 *
 * 安全研究要点：
 * 1. 在锁屏界面上显示，绕过锁屏
 * 2. 使用 FLAG_SHOW_WHEN_LOCKED 等标志
 * 3. 保持应用在前台状态，即使屏幕锁定
 * 4. 可以显示时钟、天气等"有用"信息来伪装
 *
 * 这是墨迹天气等应用常用的策略：
 * - 提供"锁屏天气"功能
 * - 用户主动开启，但实际上是保活手段
 * - 因为有前台 Activity，进程优先级极高
 *
 * 注意：
 * - Android 8.0+ 对锁屏 Activity 有限制
 * - 需要用户主动开启此功能
 * - 部分厂商可能禁止此行为
 */
class LockScreenActivity : Activity() {

    companion object {
        private const val TAG = "LockScreenActivity"
        private var instanceRef: WeakReference<LockScreenActivity>? = null

        /**
         * 启动锁屏 Activity
         */
        fun start(context: Context) {
            try {
                val intent = Intent(context, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                context.startActivity(intent)
                FwLog.d("$TAG: 启动锁屏 Activity")
            } catch (e: Exception) {
                FwLog.e("$TAG: 启动失败 - ${e.message}", e)
            }
        }

        /**
         * 关闭锁屏 Activity
         */
        fun finish() {
            instanceRef?.get()?.let {
                it.finish()
                FwLog.d("$TAG: 关闭锁屏 Activity")
            }
            instanceRef = null
        }

        /**
         * 检查是否处于锁屏状态
         */
        fun isKeyguardLocked(context: Context): Boolean {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            return km?.isKeyguardLocked == true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instanceRef = WeakReference(this)

        FwLog.d("$TAG: onCreate")

        // 设置窗口标志，允许在锁屏上显示
        setupWindowFlags()

        // 设置简单的 UI（可以替换为天气、时钟等）
        setupUI()

        // 确保服务运行
        ServiceStarter.startForegroundService(this, "锁屏 Activity")
    }

    /**
     * 设置窗口标志
     */
    @Suppress("DEPRECATION")
    private fun setupWindowFlags() {
        window.apply {
            // 在锁屏上显示
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }

            // 解锁键盘锁（仅对非安全锁屏有效）
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

            // 保持屏幕常亮
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 全屏显示
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            // 允许在状态栏上绘制
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
        }

        // 解锁键盘锁
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        }
    }

    /**
     * 设置 UI
     * 实际应用中可以显示天气、时钟等有用信息
     */
    private fun setupUI() {
        val textView = TextView(this).apply {
            text = "锁屏保活\n\n点击任意位置退出"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0x80000000.toInt()) // 半透明黑色背景
            setOnClickListener {
                finish()
            }
        }
        setContentView(textView)
    }

    override fun onResume() {
        super.onResume()
        FwLog.d("$TAG: onResume - 进程优先级已提升")
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("$TAG: onDestroy")
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
    }

    override fun onBackPressed() {
        // 禁用返回键，用户必须点击屏幕退出
        // 或者可以调用 super.onBackPressed() 允许返回
        finish()
    }
}
