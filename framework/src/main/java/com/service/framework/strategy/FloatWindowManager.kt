/**
 * ============================================================================
 * FloatWindowManager.kt - 悬浮窗保活管理器
 * ============================================================================
 *
 * 功能简介：
 *   管理系统悬浮窗的显示与隐藏，支持 1 像素透明悬浮窗和可见悬浮窗两种模式。
 *   通过悬浮窗保持应用处于"使用中"状态，从而提升进程优先级。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.service.framework.util.FwLog

/**
 * 悬浮窗保活策略
 *
 * 安全研究要点：
 * 1. 悬浮窗需要 SYSTEM_ALERT_WINDOW 权限
 * 2. 有悬浮窗的应用，系统会认为其在"使用中"
 * 3. 即使是 0 像素或完全透明的悬浮窗也有效
 * 4. 配合 TYPE_APPLICATION_OVERLAY 使用
 *
 * 常见应用场景：
 * - 悬浮球（如 360 手机助手）
 * - 悬浮时钟/天气
 * - 游戏工具箱
 * - 录屏工具
 *
 * 这些悬浮窗看起来是"功能"，实际也是保活手段
 *
 * 注意：
 * - 需要用户手动授权悬浮窗权限
 * - Android 8.0+ 使用 TYPE_APPLICATION_OVERLAY
 * - 某些厂商可能对悬浮窗有额外限制
 */
object FloatWindowManager {

    private const val TAG = "FloatWindowManager"
    private var floatView: View? = null
    private var windowManager: WindowManager? = null

    /**
     * 检查是否有悬浮窗权限
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 显示 1 像素悬浮窗（完全隐藏）
     *
     * 这是最隐蔽的保活方式之一
     */
    fun showOnePixelFloat(context: Context): Boolean {
        if (!canDrawOverlays(context)) {
            FwLog.w("$TAG: 没有悬浮窗权限")
            return false
        }

        if (floatView != null) {
            FwLog.d("$TAG: 悬浮窗已存在")
            return true
        }

        return try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 创建 1x1 像素的透明 View
            floatView = View(context).apply {
                setBackgroundColor(0x00000000) // 完全透明
            }

            val params = createLayoutParams(1, 1)
            windowManager?.addView(floatView, params)

            FwLog.d("$TAG: 1 像素悬浮窗已显示")
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 显示悬浮窗失败 - ${e.message}", e)
            false
        }
    }

    /**
     * 显示可见的悬浮窗（如悬浮球）
     *
     * 可以伪装成有用的功能
     */
    fun showVisibleFloat(context: Context, text: String = "⚡"): Boolean {
        if (!canDrawOverlays(context)) {
            FwLog.w("$TAG: 没有悬浮窗权限")
            return false
        }

        if (floatView != null) {
            FwLog.d("$TAG: 悬浮窗已存在")
            return true
        }

        return try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 创建悬浮球
            floatView = TextView(context).apply {
                this.text = text
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x80000000.toInt())
                setPadding(20, 20, 20, 20)
                gravity = Gravity.CENTER
            }

            val params = createLayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 200
            }

            windowManager?.addView(floatView, params)

            FwLog.d("$TAG: 可见悬浮窗已显示")
            true
        } catch (e: Exception) {
            FwLog.e("$TAG: 显示悬浮窗失败 - ${e.message}", e)
            false
        }
    }

    /**
     * 创建 LayoutParams
     */
    @Suppress("DEPRECATION")
    private fun createLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
    }

    /**
     * 隐藏悬浮窗
     */
    fun hide() {
        try {
            floatView?.let {
                windowManager?.removeView(it)
                FwLog.d("$TAG: 悬浮窗已隐藏")
            }
        } catch (e: Exception) {
            FwLog.e("$TAG: 隐藏悬浮窗失败 - ${e.message}", e)
        }
        floatView = null
        windowManager = null
    }

    /**
     * 检查悬浮窗是否显示
     */
    fun isShowing(): Boolean = floatView != null
}
