/**
 * ============================================================================
 * WifiReceiver.kt - WiFi 广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听 WiFi 相关事件，包括 WiFi 状态变化、网络连接状态、扫描结果可用、
 *   信号强度变化等，需要动态注册（Android 8.0+ 无法静态注册 WiFi 相关广播）。
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
import android.net.wifi.WifiManager
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * WiFi 广播接收器
 *
 * 监听 WiFi 相关事件
 *
 * 安全研究要点：
 * - WiFi 相关广播在 Android 8.0+ 无法静态注册
 * - 需要动态注册
 * - WiFi 扫描结果可用等事件可以触发
 */
class WifiReceiver : BroadcastReceiver() {

    companion object {
        /**
         * 获取动态注册的 IntentFilter
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                // WiFi 状态变化
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                // WiFi 连接状态变化
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                // WiFi 扫描结果可用
                addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                // RSSI（信号强度）变化
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                // 网络 ID 变化
                addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION)
                // 超级用户状态变化
                addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
                // 连接状态变化
                addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        FwLog.d("WifiReceiver 收到广播: $action")

        when (action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
                val stateName = when (state) {
                    WifiManager.WIFI_STATE_ENABLED -> "已开启"
                    WifiManager.WIFI_STATE_DISABLED -> "已关闭"
                    WifiManager.WIFI_STATE_ENABLING -> "正在开启"
                    WifiManager.WIFI_STATE_DISABLING -> "正在关闭"
                    else -> "未知"
                }
                FwLog.d("WiFi 状态: $stateName")

                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    ServiceStarter.startForegroundService(context, "WiFi开启")
                }
            }

            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                FwLog.d("WiFi 网络状态变化")
                ServiceStarter.startForegroundService(context, "WiFi网络变化")
            }

            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                FwLog.d("WiFi 扫描结果可用")
            }

            WifiManager.RSSI_CHANGED_ACTION -> {
                val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -1)
                FwLog.v("WiFi 信号强度变化: $rssi")
            }

            WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                FwLog.d("WiFi Supplicant 状态变化")
            }

            WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION -> {
                val connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)
                FwLog.d("WiFi Supplicant 连接状态: ${if (connected) "已连接" else "已断开"}")
                if (connected) {
                    ServiceStarter.startForegroundService(context, "WiFi连接")
                }
            }
        }
    }
}
