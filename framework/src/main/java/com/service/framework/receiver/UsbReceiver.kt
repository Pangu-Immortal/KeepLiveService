/**
 * ============================================================================
 * UsbReceiver.kt - USB 设备广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听 USB 设备连接/断开事件、USB 配件（Accessory Mode）连接事件以及
 *   USB 状态变化，支持多种 USB 设备类型的识别与分析。
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
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * USB 设备广播接收器
 *
 * 核心机制：
 * 1. 监听 USB 设备连接/断开事件
 * 2. 监听 USB 配件（Accessory）连接事件
 * 3. 这些广播可以静态注册，在 Android 8.0+ 仍可工作
 *
 * 安全研究要点：
 * - USB 设备连接是可以静态注册的广播
 * - 包括 USB 存储、打印机、摄像头、键盘鼠标等
 * - USB 配件模式（AOA）也可以触发
 * - 某些设备（如车载系统）频繁使用 USB 连接
 *
 * 使用场景：
 * - 打印机应用
 * - USB 存储管理应用
 * - 车载应用
 * - 外设管理应用
 */
class UsbReceiver : BroadcastReceiver() {

    companion object {
        // USB 设备连接/断开
        const val ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"

        // USB 配件（Accessory Mode）连接/断开
        const val ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED"
        const val ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED"

        // USB 状态变化（Android 12+）
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

        /**
         * 获取动态注册的 IntentFilter
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_USB_DEVICE_ATTACHED)
                addAction(ACTION_USB_DEVICE_DETACHED)
                addAction(ACTION_USB_ACCESSORY_ATTACHED)
                addAction(ACTION_USB_ACCESSORY_DETACHED)
                addAction(ACTION_USB_STATE)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return

        FwLog.d("UsbReceiver: 收到广播 - $action")

        when (action) {
            ACTION_USB_DEVICE_ATTACHED -> {
                handleUsbDeviceAttached(context, intent)
            }
            ACTION_USB_DEVICE_DETACHED -> {
                handleUsbDeviceDetached(context, intent)
            }
            ACTION_USB_ACCESSORY_ATTACHED -> {
                handleUsbAccessoryAttached(context, intent)
            }
            ACTION_USB_ACCESSORY_DETACHED -> {
                FwLog.d("UsbReceiver: USB 配件已断开")
            }
            ACTION_USB_STATE -> {
                handleUsbStateChanged(context, intent)
            }
        }
    }

    /**
     * 处理 USB 设备连接
     */
    private fun handleUsbDeviceAttached(context: Context, intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        val deviceInfo = device?.let {
            "名称=${it.deviceName}, 厂商=${it.vendorId}, 产品=${it.productId}"
        } ?: "未知设备"

        FwLog.d("UsbReceiver: USB 设备已连接 - $deviceInfo")

        // 分析设备类型
        device?.let { analyzeDeviceType(it) }

        // 拉起服务
        ServiceStarter.startForegroundService(context, "USB设备已连接")
    }

    /**
     * 处理 USB 设备断开
     */
    private fun handleUsbDeviceDetached(context: Context, intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        val deviceName = device?.deviceName ?: "未知设备"
        FwLog.d("UsbReceiver: USB 设备已断开 - $deviceName")
    }

    /**
     * 处理 USB 配件连接（Android Open Accessory）
     */
    private fun handleUsbAccessoryAttached(context: Context, intent: Intent) {
        FwLog.d("UsbReceiver: USB 配件已连接（AOA 模式）")

        // USB 配件模式通常用于车载系统、智能硬件等
        ServiceStarter.startForegroundService(context, "USB配件已连接")
    }

    /**
     * 处理 USB 状态变化
     */
    private fun handleUsbStateChanged(context: Context, intent: Intent) {
        val connected = intent.getBooleanExtra("connected", false)
        val configured = intent.getBooleanExtra("configured", false)
        val usbFunction = intent.getStringExtra("usb_functions") ?: "未知"

        FwLog.d("UsbReceiver: USB 状态变化 - 连接=$connected, 配置=$configured, 功能=$usbFunction")

        if (connected) {
            ServiceStarter.startForegroundService(context, "USB状态变化")
        }
    }

    /**
     * 分析 USB 设备类型
     *
     * USB 设备类别（bDeviceClass）：
     * - 0x00: 设备类在接口描述符中定义
     * - 0x01: 音频设备
     * - 0x02: 通信设备（调制解调器、网卡）
     * - 0x03: HID 设备（键盘、鼠标）
     * - 0x05: 物理设备
     * - 0x06: 图像设备（相机、扫描仪）
     * - 0x07: 打印机
     * - 0x08: 大容量存储设备（U盘、硬盘）
     * - 0x09: USB Hub
     * - 0x0A: CDC 数据
     * - 0x0B: 智能卡
     * - 0x0D: 内容安全
     * - 0x0E: 视频设备
     * - 0x0F: 个人医疗设备
     * - 0xDC: 诊断设备
     * - 0xE0: 无线控制器（蓝牙）
     * - 0xEF: 杂项
     * - 0xFE: 应用特定
     * - 0xFF: 厂商特定
     */
    private fun analyzeDeviceType(device: UsbDevice) {
        val deviceClass = device.deviceClass
        val deviceTypeName = when (deviceClass) {
            0x00 -> "复合设备"
            0x01 -> "音频设备"
            0x02 -> "通信设备（调制解调器/网卡）"
            0x03 -> "HID设备（键盘/鼠标）"
            0x05 -> "物理设备"
            0x06 -> "图像设备（相机/扫描仪）"
            0x07 -> "打印机"
            0x08 -> "大容量存储设备（U盘/硬盘）"
            0x09 -> "USB Hub"
            0x0A -> "CDC数据"
            0x0B -> "智能卡"
            0x0D -> "内容安全"
            0x0E -> "视频设备（摄像头）"
            0x0F -> "个人医疗设备"
            0xDC -> "诊断设备"
            0xE0 -> "无线控制器（蓝牙适配器）"
            0xEF -> "杂项设备"
            0xFE -> "应用特定设备"
            0xFF -> "厂商特定设备"
            else -> "未知类型(0x${deviceClass.toString(16)})"
        }

        FwLog.d("UsbReceiver: 设备类型 - $deviceTypeName")

        // 打印机特别处理
        if (deviceClass == 0x07) {
            FwLog.d("UsbReceiver: 检测到打印机设备！")
        }
    }
}
