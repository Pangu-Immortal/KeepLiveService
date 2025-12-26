/**
 * ============================================================================
 * DynamicEventReceiver.kt - 动态系统事件广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   专用于运行时动态注册的系统事件广播接收器，监听网络连接状态变化、
 *   电源连接/断开、用户解锁屏幕等事件，用于判断唤醒时机。
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
import android.net.ConnectivityManager
import android.os.Build
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 动态系统事件广播接收器，**专用于运行时动态注册**。
 *
 * **设计**:
 * 此 Receiver 用于监听那些不能或不适合静态注册的系统事件。通过在 [com.service.framework.Fw] 模块中动态注册，
 * 它可以在应用运行时响应一系列有助于判断唤醒时机的事件。
 *
 * **监听的关键事件**:
 * - `CONNECTIVITY_ACTION`: 网络连接状态发生变化。这是一个非常有效的唤醒时机。
 * - `ACTION_POWER_CONNECTED` / `DISCONNECTED`: 设备连接或断开电源。
 * - `ACTION_USER_PRESENT`: 用户解锁屏幕。这是一个明确的用户交互信号，适合执行检查。
 *
 * **被排除的事件**:
 * - `TIME_TICK`, `BATTERY_CHANGED`: 因触发过于频繁而被排除，以避免不必要的资源消耗。
 * - `SCREEN_ON` / `OFF`: 已由 `OnePixelActivity` 和 `LockScreenActivity` 的专用 Receiver 处理，为避免逻辑重叠而排除。
 *
 * @author https://github.com/Pangu-Immortal/KeepAlivePerfect
 * @see SystemEventReceiver
 * @since 2.1.0
 */
class DynamicEventReceiver : BroadcastReceiver() {

    companion object {
        /**
         * @return 一个包含所有需要动态注册的系统广播 Action 的 IntentFilter。
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                // 网络变化 (Android 7.0+ 必须动态注册)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)

                // 电源连接状态
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)

                // 用户解锁
                addAction(Intent.ACTION_USER_PRESENT)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        var wakeUpReason: String? = null

        when (action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> wakeUpReason = "网络状态变化"
            Intent.ACTION_POWER_CONNECTED -> wakeUpReason = "电源已连接"
            Intent.ACTION_POWER_DISCONNECTED -> wakeUpReason = "电源已断开"
            Intent.ACTION_USER_PRESENT -> wakeUpReason = "用户解锁屏幕"
        }

        if (wakeUpReason != null && Fw.isInitialized()) {
            FwLog.i("Dynamic event captured: $wakeUpReason. Triggering keep-alive check.")
            Fw.check()
        }
    }
}
