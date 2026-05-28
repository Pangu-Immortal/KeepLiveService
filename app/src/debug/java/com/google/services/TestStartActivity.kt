/**
 * ============================================================================
 * TestStartActivity.kt - Debug 专用 startActivity 测试 Activity
 * ============================================================================
 *
 * 功能简介：
 *   提供一个无动画、无权限请求、无后台任务的极简 Activity，专门用于
 *   instrumentation 测试真实触发 Native startActivity 策略。
 *
 * 函数简介：
 *   - onCreate：创建一个简单 View，确保主线程可以快速进入 idle 状态。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 */
package com.google.services

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View

/**
 * Debug 专用 startActivity 测试 Activity。
 */
class TestStartActivity : Activity() {

    /**
     * 创建无动画测试界面。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 先执行 Activity 标准初始化。
        super.onCreate(savedInstanceState)
        // 创建一个简单 View，避免 Compose 动画导致 instrumentation 等不到 idle。
        val contentView = View(this)
        // 使用纯色背景，保证绘制成本最低。
        contentView.setBackgroundColor(Color.BLACK)
        // 设置测试内容视图。
        setContentView(contentView)
    }
}
