/**
 * ============================================================================
 * FwCallStyleManager.kt - CallStyle 通知豁免管理器
 * ============================================================================
 *
 * 功能简介：
 *   利用 Notification.CallStyle（API 31+）发送通话样式通知，绕过
 *   POST_NOTIFICATIONS 权限限制。通过 ConnectionService + PhoneAccount
 *   注册为自管理通话应用，实现无需通知权限即可显示前台通知的保活效果。
 *
 * 核心机制：
 *   - 注册 PhoneAccount（CAPABILITY_SELF_MANAGED）标记为自管理通话应用
 *   - 通过 ConnectionService 添加 Connection 建立虚拟通话
 *   - 使用 Notification.CallStyle 构建通话样式通知
 *   - CallStyle 通知在 API 31+ 豁免 POST_NOTIFICATIONS 权限
 *   - API 31 以下降级为普通前台通知
 *
 * 主要功能：
 *   - show(): 显示 CallStyle 通知
 *   - dismiss(): 移除 CallStyle 通知
 *   - FwConnectionService: 内部 ConnectionService 实现
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.3.0
 */
package com.service.framework.strategy

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * CallStyle 通知豁免管理器（单例）
 *
 * 核心机制：
 * 1. 注册 PhoneAccount 标记为自管理通话应用
 * 2. 通过 ConnectionService 创建虚拟通话连接
 * 3. 使用 Notification.CallStyle 发送豁免通知
 * 4. API 31+ CallStyle 通知不需要 POST_NOTIFICATIONS 权限
 *
 * 安全研究要点：
 * - CallStyle 通知是 Android 12+ 引入的通知样式
 * - 用于来电/通话中场景，享有系统级展示优先级
 * - 不需要 POST_NOTIFICATIONS 权限即可显示
 * - 需要 MANAGE_OWN_CALLS 权限
 * - 适合即时通讯、VoIP 等通话类应用
 */
object FwCallStyleManager {

    private const val TAG = "FwCallStyleManager"            // 日志子标签
    private const val CHANNEL_ID = "fw_call_channel"        // 通知渠道 ID
    private const val CHANNEL_NAME = "通话服务"              // 通知渠道名称
    private const val NOTIFICATION_ID = 10010                // 通知 ID
    private const val PHONE_ACCOUNT_ID = "fw_self_managed"   // PhoneAccount 标识

    @Volatile
    private var isShowing = false // 通知是否正在显示

    private var phoneAccountHandle: PhoneAccountHandle? = null // PhoneAccount 句柄
    private var currentConnection: FwCallConnection? = null     // 当前虚拟通话连接

    /**
     * 显示 CallStyle 通知
     * API 31+ 使用 CallStyle，低版本降级为普通通知
     *
     * @param context 上下文
     */
    fun show(context: Context) {
        if (isShowing) {
            FwLog.d("$TAG: CallStyle 通知已在显示中，跳过")
            return
        }
        FwLog.d("$TAG: 开始显示 CallStyle 通知...")

        try {
            // 创建通知渠道（Android 8.0+）
            createNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ 使用 CallStyle 通知
                registerPhoneAccount(context) // 注册 PhoneAccount
                showCallStyleNotification(context) // 显示 CallStyle 通知
            } else {
                // API 31 以下降级为普通通知
                FwLog.d("$TAG: 当前 API < 31，降级为普通前台通知")
                showFallbackNotification(context)
            }

            isShowing = true
            FwLog.d("$TAG: CallStyle 通知显示成功")
        } catch (e: Exception) {
            FwLog.e("$TAG: 显示 CallStyle 通知失败: ${e.message}", e)
        }
    }

    /**
     * 移除 CallStyle 通知
     *
     * @param context 上下文
     */
    fun dismiss(context: Context) {
        FwLog.d("$TAG: 开始移除 CallStyle 通知...")
        try {
            // 取消通知
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager // 获取通知管理器
            nm?.cancel(NOTIFICATION_ID) // 取消指定 ID 的通知
            FwLog.d("$TAG: 通知已取消")

            // 断开虚拟通话连接
            disconnectCall()

            isShowing = false
            FwLog.d("$TAG: CallStyle 通知已完全移除")
        } catch (e: Exception) {
            FwLog.e("$TAG: 移除 CallStyle 通知失败: ${e.message}", e)
        }
    }

    /**
     * 检查 CallStyle 通知是否正在显示
     * @return 是否显示中
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 创建通知渠道
     * @param context 上下文
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 高优先级，保证通知可见
            ).apply {
                description = "通话保活服务通知渠道" // 渠道描述
                setSound(null, null) // 无声音
                enableLights(false)  // 无闪光灯
                enableVibration(false) // 无振动
                setShowBadge(false)  // 不显示角标
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager // 获取通知管理器
            nm.createNotificationChannel(channel) // 创建渠道
            FwLog.d("$TAG: 通知渠道已创建: $CHANNEL_ID")
        }
    }

    /**
     * 注册 PhoneAccount（自管理通话）
     * 将应用注册为具备自管理通话能力的应用
     *
     * @param context 上下文
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerPhoneAccount(context: Context) {
        FwLog.d("$TAG: 注册 PhoneAccount（自管理通话）...")
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE)
                as? TelecomManager // 获取 TelecomManager 系统服务
            if (telecomManager == null) {
                FwLog.e("$TAG: 无法获取 TelecomManager 系统服务")
                return
            }

            // 构建 PhoneAccountHandle
            val componentName = ComponentName(context, FwConnectionService::class.java) // 指向 ConnectionService
            phoneAccountHandle = PhoneAccountHandle(componentName, PHONE_ACCOUNT_ID) // 创建句柄

            // 构建 PhoneAccount
            val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "FwCall")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED) // 标记为自管理通话
                .build()

            telecomManager.registerPhoneAccount(phoneAccount) // 注册到系统
            FwLog.d("$TAG: PhoneAccount 注册成功，ID: $PHONE_ACCOUNT_ID")
        } catch (e: Exception) {
            FwLog.e("$TAG: 注册 PhoneAccount 失败: ${e.message}", e)
        }
    }

    /**
     * 显示 CallStyle 通知（API 31+）
     * 使用 Notification.CallStyle 构建通话样式通知，豁免 POST_NOTIFICATIONS 权限
     *
     * @param context 上下文
     */
    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showCallStyleNotification(context: Context) {
        FwLog.d("$TAG: 构建 CallStyle 通知（API 31+）...")
        try {
            // 构建挂断 PendingIntent
            val hangupIntent = Intent(context, FwConnectionService::class.java).apply {
                action = FwConnectionService.ACTION_HANGUP // 挂断动作
            }
            val hangupPendingIntent = PendingIntent.getService(
                context,
                0,
                hangupIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // 不可变标志
            )

            // 构建通话人信息
            val caller = android.app.Person.Builder()
                .setName("服务保活") // 来电方名称
                .setImportant(true) // 标记为重要
                .build()

            // 构建 CallStyle 通知
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(Fw.config.notificationIconResId) // 通知小图标
                .setStyle(
                    Notification.CallStyle.forOngoingCall(
                        caller,           // 通话人
                        hangupPendingIntent // 挂断按钮意图
                    )
                )
                .setOngoing(true) // 常驻通知
                .setCategory(Notification.CATEGORY_CALL) // 通话类别
                .build()

            // 显示通知
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager // 获取通知管理器
            nm.notify(NOTIFICATION_ID, notification) // 发送通知
            FwLog.d("$TAG: CallStyle 通知已发送，ID: $NOTIFICATION_ID")
        } catch (e: Exception) {
            FwLog.e("$TAG: 显示 CallStyle 通知失败: ${e.message}", e)
        }
    }

    /**
     * 显示降级通知（API < 31）
     * 使用 NotificationCompat 构建普通前台通知作为降级方案
     *
     * @param context 上下文
     */
    private fun showFallbackNotification(context: Context) {
        FwLog.d("$TAG: 构建降级普通通知（API < 31）...")
        try {
            val config = Fw.config // 获取框架配置

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(config.notificationTitle) // 通知标题
                .setContentText(config.notificationContent) // 通知内容
                .setSmallIcon(config.notificationIconResId) // 通知小图标
                .setOngoing(true) // 常驻通知
                .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 公开可见
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager // 获取通知管理器
            nm.notify(NOTIFICATION_ID, notification) // 发送通知
            FwLog.d("$TAG: 降级通知已发送，ID: $NOTIFICATION_ID")
        } catch (e: Exception) {
            FwLog.e("$TAG: 显示降级通知失败: ${e.message}", e)
        }
    }

    /**
     * 断开虚拟通话连接
     */
    private fun disconnectCall() {
        try {
            currentConnection?.onDisconnect() // 断开连接
            currentConnection = null
            FwLog.d("$TAG: 虚拟通话连接已断开")
        } catch (e: Exception) {
            FwLog.e("$TAG: 断开虚拟通话连接失败: ${e.message}", e)
        }
    }

    /**
     * 设置当前虚拟通话连接（由 FwConnectionService 调用）
     * @param connection 通话连接
     */
    internal fun setCurrentConnection(connection: FwCallConnection?) {
        currentConnection = connection
    }

    /**
     * 虚拟通话连接
     * 实现最小化的 Connection，仅用于维持通话状态
     */
    class FwCallConnection : Connection() {
        init {
            // 设置连接属性为自管理
            connectionProperties = PROPERTY_SELF_MANAGED
            // 设置音频模式
            audioModeIsVoip = true
        }

        override fun onStateChanged(state: Int) {
            super.onStateChanged(state)
            FwLog.d("$TAG: 通话连接状态变更: $state")
        }

        override fun onDisconnect() {
            FwLog.d("$TAG: 通话连接断开")
            setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.LOCAL))
            destroy() // 销毁连接
        }

        override fun onAbort() {
            FwLog.d("$TAG: 通话连接中止")
            onDisconnect() // 中止时也执行断开逻辑
        }
    }

    /**
     * ============================================================================
     * FwConnectionService - 通话连接服务
     * ============================================================================
     *
     * 功能简介：
     *   实现 ConnectionService，用于创建和管理自管理通话连接。
     *   配合 PhoneAccount 和 CallStyle 通知实现保活效果。
     */
    class FwConnectionService : ConnectionService() {

        companion object {
            const val ACTION_HANGUP = "com.service.framework.ACTION_HANGUP" // 挂断动作
        }

        override fun onCreate() {
            super.onCreate()
            FwLog.d("$TAG: FwConnectionService onCreate")
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // 处理挂断动作
            if (intent?.action == ACTION_HANGUP) {
                FwLog.d("$TAG: 收到挂断请求")
                currentConnection?.onDisconnect() // 断开当前连接
                dismiss(applicationContext) // 移除通知
            }
            return super.onStartCommand(intent, flags, startId)
        }

        /**
         * 创建来电连接回调
         * 系统在有来电时调用此方法
         */
        override fun onCreateIncomingConnection(
            connectionManagerPhoneAccount: PhoneAccountHandle?,
            request: ConnectionRequest?
        ): Connection {
            FwLog.d("$TAG: onCreateIncomingConnection - 创建来电连接")
            val connection = FwCallConnection() // 创建虚拟通话连接
            connection.setInitialized() // 标记为已初始化
            connection.setActive() // 设置为活跃状态
            setCurrentConnection(connection) // 保存引用
            return connection
        }

        /**
         * 创建去电连接回调
         * 系统在拨出电话时调用此方法
         */
        override fun onCreateOutgoingConnection(
            connectionManagerPhoneAccount: PhoneAccountHandle?,
            request: ConnectionRequest?
        ): Connection {
            FwLog.d("$TAG: onCreateOutgoingConnection - 创建去电连接")
            val connection = FwCallConnection() // 创建虚拟通话连接
            connection.setInitialized() // 标记为已初始化
            connection.setDialing() // 设置为拨号状态
            connection.setActive() // 然后设置为活跃状态
            setCurrentConnection(connection) // 保存引用
            return connection
        }

        override fun onDestroy() {
            super.onDestroy()
            FwLog.d("$TAG: FwConnectionService onDestroy")
        }
    }
}
