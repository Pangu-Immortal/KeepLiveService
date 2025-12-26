/**
 * ============================================================================
 * FwJobService.kt - JobScheduler 保活策略
 * ============================================================================
 *
 * 功能简介：
 *   利用系统 JobScheduler 定期执行任务，检查并拉起服务。
 *
 * 核心机制：
 *   - 利用 JobScheduler 定期执行任务
 *   - 最小间隔 15 分钟（Android 系统限制）
 *   - 支持设备重启后保留任务（setPersisted）
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * JobScheduler 保活策略
 *
 * 核心机制：
 * 1. 利用系统的 JobScheduler 定期执行任务
 * 2. 任务执行时检查并拉起服务
 * 3. 最小间隔 15 分钟（Android 限制）
 *
 * 安全研究要点：
 * - JobScheduler 是 Android 推荐的后台任务方式
 * - 但如果仅用于唤醒应用而非真正的后台任务，则属于滥用
 * - 强制停止后，JobScheduler 任务会被取消
 * - 但安装后首次启动会注册任务
 */
class FwJobService : JobService() {

    companion object {
        private const val JOB_ID = 10001

        /**
         * 调度 Job
         */
        fun schedule(context: Context) {
            val config = Fw.config ?: return
            if (!config.enableJobScheduler) return

            try {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                    ?: return

                val componentName = ComponentName(context, FwJobService::class.java)

                val builder = JobInfo.Builder(JOB_ID, componentName)
                    .setPersisted(true) // 设备重启后保留
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)

                // Android 7.0+ 设置周期
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 最小间隔 15 分钟
                    builder.setPeriodic(
                        maxOf(config.jobSchedulerInterval, 15 * 60 * 1000L),
                        5 * 60 * 1000L // flex 5分钟
                    )
                } else {
                    builder.setPeriodic(config.jobSchedulerInterval)
                }

                // Android 8.0+ 允许在后台启动
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setRequiresBatteryNotLow(false)
                    builder.setRequiresStorageNotLow(false)
                }

                val result = jobScheduler.schedule(builder.build())
                if (result == JobScheduler.RESULT_SUCCESS) {
                    FwLog.d("JobScheduler 调度成功")
                } else {
                    FwLog.e("JobScheduler 调度失败")
                }
            } catch (e: Exception) {
                FwLog.e("JobScheduler 调度异常: ${e.message}", e)
            }
        }

        /**
         * 取消 Job
         */
        fun cancel(context: Context) {
            try {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                jobScheduler?.cancel(JOB_ID)
                FwLog.d("JobScheduler 已取消")
            } catch (e: Exception) {
                FwLog.e("取消 JobScheduler 失败: ${e.message}", e)
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        FwLog.d("JobScheduler onStartJob 执行")

        // 拉起服务
        ServiceStarter.startForegroundService(applicationContext, "JobScheduler唤醒")

        // 返回 false 表示任务已完成
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        FwLog.d("JobScheduler onStopJob")
        // 返回 true 表示任务被中断需要重新调度
        return true
    }
}
