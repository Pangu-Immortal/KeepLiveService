package com.google.services

import android.app.Application
import android.util.Log
import com.service.framework.Fw

/**
 * Application 类
 *
 * 核心：在这里一行代码初始化 Framework 模块
 *
 * 安全研究说明：
 * - 使用独立的 framework 模块实现所有策略
 * - 在 Application.onCreate() 中初始化，确保进程唤醒后立即启动
 * - 所有策略可通过配置开关控制
 */
class MyApp : Application() {

    companion object {
        private const val TAG = "FwApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - 进程已唤醒")

        // 一行代码初始化 Framework 模块
        Fw.init(this) {
            // ==================== 基础策略 ====================
            enableForegroundService = true      // 前台服务
            enableMediaSession = true           // MediaSession（核心：让系统认为是媒体应用）
            enableOnePixelActivity = true       // 1像素Activity

            // ==================== 定时唤醒策略 ====================
            enableJobScheduler = true           // JobScheduler
            jobSchedulerInterval = 15 * 60 * 1000L  // 15分钟

            enableWorkManager = true            // WorkManager
            workManagerIntervalMinutes = 15L    // 15分钟

            enableAlarmManager = true           // AlarmManager
            alarmManagerInterval = 5 * 60 * 1000L   // 5分钟

            // ==================== 账户同步策略 ====================
            enableAccountSync = true            // 账户同步
            syncIntervalSeconds = 60L           // 60秒

            // ==================== 广播策略 ====================
            enableSystemBroadcast = true        // 系统广播
            enableBluetoothBroadcast = true     // 蓝牙广播（核心：酷狗音乐的关键）
            enableMediaButtonReceiver = true    // 媒体按键

            // ==================== 内容观察者策略 ====================
            enableMediaContentObserver = true   // 相册变化
            enableContactsContentObserver = false // 联系人变化（需要权限）
            enableSmsContentObserver = false    // 短信变化（需要权限）
            enableSettingsContentObserver = true // 设置变化

            // ==================== 双进程策略 ====================
            enableDualProcess = true            // 双进程守护

            // ==================== 通知配置 ====================
            notificationChannelId = "fw_channel"
            notificationChannelName = "守护精灵"
            notificationTitle = "守护精灵运行中"
            notificationContent = "正在守护你的应用~"
            notificationIconResId = R.drawable.ic_notification
            notificationActivityClass = MainActivity::class.java

            // ==================== 日志配置 ====================
            enableDebugLog = true
            logTag = "FwApp"
        }

        Log.d(TAG, "Framework 模块初始化完成")
    }
}
