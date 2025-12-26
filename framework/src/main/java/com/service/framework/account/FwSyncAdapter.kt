/**
 * ============================================================================
 * FwSyncAdapter.kt - 同步适配器
 * ============================================================================
 *
 * 功能简介：
 *   基于 AbstractThreadedSyncAdapter 的同步适配器实现。
 *   系统会定期调用 onPerformSync 方法，在其中拉起服务。
 *   同步间隔可设置为 60 秒，比 JobScheduler 更频繁。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 同步适配器
 *
 * 核心机制：
 * 1. 系统会定期调用 onPerformSync
 * 2. 在 onPerformSync 中拉起服务
 * 3. 同步间隔可以设置为 60 秒（比 JobScheduler 更频繁）
 *
 * 安全研究要点：
 * - 账户同步是早期常用的保活手段
 * - 可以绕过 JobScheduler 的 15 分钟限制
 * - 但在新版 Android 中受到更多限制
 * - 强制停止后同步会被暂停
 */
class FwSyncAdapter @JvmOverloads constructor(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean = false
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {
        FwLog.d("SyncAdapter onPerformSync 执行")

        // 拉起服务
        ServiceStarter.startForegroundService(context, "账户同步唤醒")
    }

    companion object {
        /**
         * 启用同步
         */
        fun enableSync(context: Context) {
            val config = Fw.config ?: return
            if (!config.enableAccountSync) return

            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType(config.accountType)

                if (accounts.isEmpty()) {
                    FwLog.w("没有找到账户，先添加账户")
                    FwAuthenticator.addAccount(context)
                }

                val account = accounts.firstOrNull()
                    ?: Account("Fw", config.accountType)

                // 启用自动同步
                ContentResolver.setIsSyncable(account, config.accountType, 1)
                ContentResolver.setSyncAutomatically(account, config.accountType, true)

                // 设置同步间隔
                ContentResolver.addPeriodicSync(
                    account,
                    config.accountType,
                    Bundle.EMPTY,
                    config.syncIntervalSeconds
                )

                FwLog.d("同步已启用，间隔: ${config.syncIntervalSeconds}秒")
            } catch (e: Exception) {
                FwLog.e("启用同步失败: ${e.message}", e)
            }
        }

        /**
         * 禁用同步
         */
        fun disableSync(context: Context) {
            val config = Fw.config ?: return

            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType(config.accountType)

                for (account in accounts) {
                    ContentResolver.setSyncAutomatically(account, config.accountType, false)
                    ContentResolver.removePeriodicSync(account, config.accountType, Bundle.EMPTY)
                }

                FwLog.d("同步已禁用")
            } catch (e: Exception) {
                FwLog.e("禁用同步失败: ${e.message}", e)
            }
        }

        /**
         * 立即触发同步
         */
        fun requestSync(context: Context) {
            val config = Fw.config ?: return

            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType(config.accountType)

                for (account in accounts) {
                    val extras = Bundle().apply {
                        putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    }
                    ContentResolver.requestSync(account, config.accountType, extras)
                }

                FwLog.d("已请求立即同步")
            } catch (e: Exception) {
                FwLog.e("请求同步失败: ${e.message}", e)
            }
        }
    }
}
