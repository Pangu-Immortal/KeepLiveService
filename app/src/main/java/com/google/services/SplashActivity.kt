/**
 * ============================================================================
 * SplashActivity.kt - 启动页 Activity
 * ============================================================================
 *
 * 功能简介：
 *   独立的启动页 Activity，展示 Lottie 动画欢迎界面
 *   动画结束后自动跳转到 MainActivity
 *
 * 动画效果：
 *   - 粉紫渐变背景
 *   - 精灵飞舞动画
 *   - 星星闪烁动画
 *   - 标题呼吸效果
 *   - 游戏风格进度条
 *
 * 注意：时长配置统一在 SplashContent.SPLASH_DURATION_MS
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.services.ui.components.SPLASH_DURATION_MS
import com.google.services.ui.components.SplashContent
import com.google.services.ui.theme.KeepLiveServiceTheme
import kotlinx.coroutines.delay

/**
 * 启动页 Activity
 *
 * 显示 Lottie 动画后跳转到主界面
 * 时长由 SPLASH_DURATION_MS 统一控制
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()                       // 安装系统 Splash Screen

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                          // 启用边到边显示

        setContent {
            KeepLiveServiceTheme {
                SplashContent()                     // 显示 Splash 动画内容

                LaunchedEffect(Unit) {              // 延迟后跳转到主界面
                    delay(SPLASH_DURATION_MS.toLong())
                    navigateToMain()
                }
            }
        }
    }

    /** 跳转到主界面 */
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()                                    // 关闭启动页，防止返回
    }
}
