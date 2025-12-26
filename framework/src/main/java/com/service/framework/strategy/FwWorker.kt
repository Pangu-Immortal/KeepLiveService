/**
 * ============================================================================
 * FwWorker.kt - WorkManager 保活策略
 * ============================================================================
 *
 * 功能简介：
 *   利用 WorkManager 定期执行后台任务，智能调度任务执行时机。
 *
 * 核心机制：
 *   - 利用 WorkManager 周期性执行任务
 *   - 支持约束条件（网络、充电状态等）
 *   - 最小间隔 15 分钟
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter
import java.util.concurrent.TimeUnit

/**
 * WorkManager 保活策略
 *
 * 核心机制：
 * 1. 利用 WorkManager 定期执行后台任务
 * 2. WorkManager 会智能调度任务执行时机
 * 3. 支持约束条件（网络、充电状态等）
 *
 * 安全研究要点：
 * - WorkManager 是 Google 推荐的后台任务方案
 * - 但同样被滥用于保活
 * - 最小间隔 15 分钟
 * - 强制停止后任务会被取消
 */
class FwWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "fw_periodic_work"

        /**
         * 调度周期性任务
         */
        fun schedule(context: Context) {
            val config = Fw.config ?: return
            if (!config.enableWorkManager) return

            try {
                // 约束条件
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .setRequiresStorageNotLow(false)
                    .build()

                // 创建周期性任务请求
                val workRequest = PeriodicWorkRequestBuilder<FwWorker>(
                    maxOf(config.workManagerIntervalMinutes, 15L), // 最小 15 分钟
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .addTag(WORK_NAME)
                    .build()

                // 调度任务
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )

                FwLog.d("WorkManager 调度成功")
            } catch (e: Exception) {
                FwLog.e("WorkManager 调度失败: ${e.message}", e)
            }
        }

        /**
         * 取消任务
         */
        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                FwLog.d("WorkManager 已取消")
            } catch (e: Exception) {
                FwLog.e("取消 WorkManager 失败: ${e.message}", e)
            }
        }
    }

    override fun doWork(): Result {
        FwLog.d("WorkManager doWork 执行")

        try {
            // 拉起服务
            ServiceStarter.startForegroundService(applicationContext, "WorkManager唤醒")
        } catch (e: Exception) {
            FwLog.e("WorkManager 执行失败: ${e.message}", e)
            return Result.retry()
        }

        return Result.success()
    }
}
