/**
 * ============================================================================
 * ContextExtensions.kt - Context 扩展函数
 * ============================================================================
 *
 * 功能简介：
 *   提供 Context 和 Intent 相关的扩展函数，简化常用操作。
 *
 * 主要功能：
 *   - getBluetoothDevice(): 安全获取 Intent 中的蓝牙设备对象
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.util

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build

/**
 * 从 Intent 中安全地获取 [BluetoothDevice] 对象。
 *
 * 此扩展函数处理了 Android 13 (Tiramisu) 及以上版本对 [Intent.getParcelableExtra] 的 API 变更，
 * 避免了在代码中出现版本判断和废弃警告。
 *
 * @return 获取到的 [BluetoothDevice] 对象，如果 Intent 中不包含该对象则返回 `null`。
 */
fun Intent.getBluetoothDevice(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
