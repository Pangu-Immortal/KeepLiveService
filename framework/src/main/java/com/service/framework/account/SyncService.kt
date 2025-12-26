/**
 * ============================================================================
 * SyncService.kt - 同步服务
 * ============================================================================
 *
 * 功能简介：
 *   提供系统同步服务，系统通过此服务与同步适配器通信。
 *   使用单例模式确保同步适配器实例唯一。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 同步服务
 *
 * 系统通过此服务与同步适配器通信
 */
class SyncService : Service() {

    companion object {
        private val lock = Any()
        private var syncAdapter: FwSyncAdapter? = null
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(lock) {
            if (syncAdapter == null) {
                syncAdapter = FwSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter?.syncAdapterBinder
    }
}
