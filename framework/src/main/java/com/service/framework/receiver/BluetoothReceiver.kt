/**
 * ============================================================================
 * BluetoothReceiver.kt - 蓝牙广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听蓝牙相关系统广播，是实现"连接蓝牙耳机自动唤醒 App"功能的核心组件。
 *   支持 ACL 链路连接、A2DP 音频通道、蓝牙耳机 Profile 及蓝牙适配器状态变化等事件。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

package com.service.framework.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.getBluetoothDevice

/**
 * 蓝牙广播接收器，是实现“连接蓝牙耳机自动唤醒 App”功能的核心。
 *
 * **安全研究要点 - 酷狗音乐等 App 的核心唤醒机制**:
 * 1.  **静态注册**: 此 Receiver 在 `AndroidManifest.xml` 中静态注册，监听蓝牙相关的系统广播。
 * 2.  **系统豁免**: 对于声明了 `MediaBrowserService` 或拥有活跃 `MediaSession` 的“媒体类”应用，系统允许其通过静态广播在后台被唤醒，以响应蓝牙设备的连接和媒体按键。
 * 3.  **多事件监听**: 监听了从底层 ACL 连接到上层 Profile（A2DP, Headset）变化的多个广播，确保在不同蓝牙事件下都能被触发。
 * 4.  **唤醒逻辑**: `onReceive` 的职责是**捕获事件**并调用 `Fw.check()`，将具体的保活操作交由 `Fw` 模块统一处理，实现逻辑解耦。
 *
 * **注意**:
 * - 此机制在用户“强制停止”(Force Stop)应用后会失效，这是 Android 系统为保护用户而设计的机制。
 * - 需要 `BLUETOOTH_CONNECT` 运行时权限 (Android 12+)。
 *
 * @author https://github.com/Pangu-Immortal/KeepAlivePerfect
 * @since 2.1.0
 */
@SuppressLint("MissingPermission")
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // 统一处理唤醒逻辑，避免在每个 case 中重复调用
        var shouldWakeUp = false
        var wakeUpReason: String? = null

        when (action) {
            // 底层 ACL 链路连接，通常最早触发
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val deviceName = intent.getBluetoothDevice()?.name ?: "未知设备"
                FwLog.d("Bluetooth event: ACL Connected - $deviceName")
                shouldWakeUp = true
                wakeUpReason = "蓝牙设备连接"
            }

            // A2DP 音频通道连接状态变化
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    val deviceName = intent.getBluetoothDevice()?.name ?: "未知设备"
                    FwLog.d("Bluetooth event: A2DP Connected - $deviceName")
                    shouldWakeUp = true
                    wakeUpReason = "A2DP 音频连接"
                }
            }

            // 耳机 Profile 连接状态变化
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    val deviceName = intent.getBluetoothDevice()?.name ?: "未知设备"
                    FwLog.d("Bluetooth event: Headset Connected - $deviceName")
                    shouldWakeUp = true
                    wakeUpReason = "蓝牙耳机连接"
                }
            }

            // 蓝牙适配器开启
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    FwLog.d("Bluetooth event: Adapter Enabled")
                    shouldWakeUp = true
                    wakeUpReason = "蓝牙开启"
                }
            }

            // 用于测试的广播
            "com.service.framework.TEST_WAKEUP" -> {
                FwLog.d("Bluetooth event: Received Test Wakeup Broadcast")
                shouldWakeUp = true
                wakeUpReason = "测试唤醒"
            }
        }

        // 如果满足唤醒条件，则通知 Fw 框架进行检查
        if (shouldWakeUp && Fw.isInitialized()) {
            FwLog.i("Triggering keep-alive check. Reason: $wakeUpReason")
            Fw.check()
        }
    }
}
