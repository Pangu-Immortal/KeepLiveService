/**
 * ============================================================================
 * FwDreamService.kt - 屏保服务保活策略（API 17+）
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android DreamService（屏保/互动屏保）的保活策略。当设备处于充电且
 *   空闲状态时，系统会启动屏保服务，利用此机制保持应用进程存活。显示简洁的
 *   黑色背景加居中 "Fw" 文字作为屏保内容。
 *
 * 主要功能：
 *   - onAttachedToWindow(): 配置屏保属性（可交互、全屏、亮屏）
 *   - onDreamingStarted(): 屏保开始时拉起保活服务并显示屏保界面
 *   - onDreamingStopped(): 屏保结束时记录日志
 *   - onDetachedFromWindow(): 窗口分离时清理资源
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.graphics.Color
import android.graphics.Typeface
import android.service.dreams.DreamService
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 屏保服务保活策略
 *
 * 核心机制：
 * 1. 设备充电且空闲时，系统自动启动屏保
 * 2. 屏保运行期间，应用进程保持前台优先级
 * 3. 可通过系统设置选择此屏保
 *
 * 安全研究要点：
 * - 需要用户在系统设置中选择此屏保
 * - 仅在充电且空闲时触发
 * - 屏保运行期间进程受系统保护
 * - 是一种合法的保活方式
 *
 * 注意：
 * - API 17+ 支持（项目 minSdk 24 满足）
 * - 需要在 AndroidManifest 中声明
 * - 需要 BIND_DREAM_SERVICE 权限
 * - 用户需在 设置 > 显示 > 屏保 中选择
 */
class FwDreamService : DreamService() {

    override fun onCreate() {
        super.onCreate()
        FwLog.d("FwDreamService: onCreate")
    }

    /**
     * 窗口附加回调
     *
     * 配置屏保的基本属性：可交互、全屏显示、保持屏幕常亮
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        FwLog.d("FwDreamService: onAttachedToWindow，配置屏保属性")

        // 设置屏保可交互（用户触摸不会立即退出）
        isInteractive = true
        // 设置全屏显示
        isFullscreen = true
        // 保持屏幕常亮
        isScreenBright = false // 降低亮度，避免烧屏
    }

    /**
     * 屏保开始回调
     *
     * 屏保正式开始显示时调用，拉起保活服务并设置屏保界面
     */
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        FwLog.d("FwDreamService: onDreamingStarted，屏保已启动")

        // 拉起前台保活服务
        ServiceStarter.startForegroundService(this, "屏保服务启动")

        // 设置屏保界面：黑色背景 + 居中 "Fw" 文字
        setupDreamView()
    }

    /**
     * 屏保结束回调
     *
     * 用户交互或系统中断屏保时调用
     */
    override fun onDreamingStopped() {
        super.onDreamingStopped()
        FwLog.d("FwDreamService: onDreamingStopped，屏保已停止")
    }

    /**
     * 窗口分离回调
     *
     * 屏保窗口从窗口管理器中移除时调用，清理资源
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        FwLog.d("FwDreamService: onDetachedFromWindow，窗口已分离")
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("FwDreamService: onDestroy，屏保服务已销毁")
    }

    /**
     * 设置屏保显示界面
     *
     * 创建简洁的黑色背景 + 居中白色 "Fw" 文字的屏保界面
     */
    private fun setupDreamView() {
        try {
            // 创建根布局：黑色背景的 FrameLayout
            val rootLayout = FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK) // 纯黑背景，OLED 友好
            }

            // 创建居中文字：显示 "Fw"
            val textView = TextView(this).apply {
                text = "Fw" // 显示框架标识
                setTextColor(Color.WHITE) // 白色文字
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f) // 48sp 大字号
                typeface = Typeface.DEFAULT_BOLD // 粗体
                gravity = Gravity.CENTER // 文字居中对齐
            }

            // 设置文字居中的布局参数
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER // 在 FrameLayout 中居中
            )

            // 将文字添加到根布局
            rootLayout.addView(textView, layoutParams)

            // 设置为屏保内容视图
            setContentView(rootLayout)
            FwLog.d("FwDreamService: 屏保界面已设置")
        } catch (e: Exception) {
            FwLog.e("FwDreamService: 设置屏保界面失败: ${e.message}", e)
        }
    }
}
