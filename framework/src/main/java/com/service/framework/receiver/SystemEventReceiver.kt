package com.service.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 系统关键事件广播接收器，**专用于静态注册**。
 *
 * **设计**:
 * 此 Receiver 被设计为在 `AndroidManifest.xml` 中静态注册，用于捕获那些只有静态注册才能接收到的、对于应用自启动至关重要的系统广播。
 * - `BOOT_COMPLETED`: 设备开机完成。这是最经典的自启动入口。
 * - `LOCKED_BOOT_COMPLETED`: 在“直接启动”模式下（设备重启后用户首次解锁前）触发，可以更早地启动服务。
 * - `MY_PACKAGE_REPLACED`: 应用自身被更新后触发，用于在新版本安装后立即恢复服务。
 *
 * **唤醒逻辑**:
 * 监听到上述任何事件后，此 Receiver 的唯一职责是调用 `Fw.check()`，将具体的保活任务交由框架统一处理。
 *
 * **注意**: 动态注册的广播（如网络变化、屏幕亮灭等）已被移至 [DynamicEventReceiver]，以保持职责分离。
 *
 * @author qihao (Pangu-Immortal)
 * @see DynamicEventReceiver
 * @since 1.0.0
 */
class SystemEventReceiver : BroadcastReceiver() {

    companion object {
        /**
         * @return 一个包含所有可被此 Receiver 处理的静态广播 Action 的 IntentFilter。
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
                addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        var wakeUpReason: String? = null

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> wakeUpReason = "设备开机完成"
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> wakeUpReason = "设备锁定启动完成"
            Intent.ACTION_MY_PACKAGE_REPLACED -> wakeUpReason = "应用版本更新"
        }

        if (wakeUpReason != null && Fw.isInitialized()) {
            FwLog.i("System event captured: $wakeUpReason. Triggering keep-alive check.")
            Fw.check()
        }
    }
}
