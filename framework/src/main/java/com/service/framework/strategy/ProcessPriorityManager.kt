/**
 * ============================================================================
 * ProcessPriorityManager.kt - 进程优先级管理器
 * ============================================================================
 *
 * 功能简介：
 *   监控和管理进程优先级状态，提供进程重要性级别查询、OOM adj 值读取、
 *   内存使用情况获取、进程被杀风险评估等功能。帮助应用了解当前进程状态
 *   并采取相应的保活策略。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.service.framework.util.FwLog
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * 进程优先级提升策略
 *
 * 核心机制：
 * 1. 通过各种方式提升进程优先级
 * 2. 降低被系统杀死的概率
 * 3. 监控进程状态
 *
 * 安全研究要点：
 * - Android 使用 OOM Killer 管理进程
 * - 进程优先级由多种因素决定
 * - 普通应用无法直接修改 OOM adj
 * - 但可以通过其他方式间接提升优先级
 *
 * 进程优先级影响因素：
 * 1. 是否有前台 Activity
 * 2. 是否有前台服务
 * 3. 是否被其他应用绑定
 * 4. 是否有活跃的 BroadcastReceiver
 * 5. 是否有活跃的 ContentProvider
 * 6. 内存使用量
 * 7. CPU 使用量
 */
object ProcessPriorityManager {

    // 进程重要性级别（数值越小越重要）
    object Importance {
        const val FOREGROUND = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND           // 100
        const val FOREGROUND_SERVICE = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE // 125
        const val VISIBLE = ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE                 // 200
        const val PERCEPTIBLE = ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE         // 230
        const val SERVICE = ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE                 // 300
        const val CACHED = ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED                   // 400
        const val GONE = ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE                       // 1000
    }

    /**
     * 获取当前进程的重要性级别
     */
    fun getCurrentImportance(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return Importance.GONE

        val myPid = Process.myPid()
        val runningProcesses = am.runningAppProcesses ?: return Importance.GONE

        for (processInfo in runningProcesses) {
            if (processInfo.pid == myPid) {
                return processInfo.importance
            }
        }

        return Importance.GONE
    }

    /**
     * 获取重要性级别的可读名称
     */
    fun getImportanceName(importance: Int): String {
        return when {
            importance <= Importance.FOREGROUND -> "前台"
            importance <= Importance.FOREGROUND_SERVICE -> "前台服务"
            importance <= Importance.VISIBLE -> "可见"
            importance <= Importance.PERCEPTIBLE -> "可感知"
            importance <= Importance.SERVICE -> "服务"
            importance <= Importance.CACHED -> "缓存"
            else -> "已停止"
        }
    }

    /**
     * 获取当前进程的 OOM adj 值
     *
     * 读取 /proc/self/oom_score_adj
     * 值越小，越不容易被杀
     *
     * 常见值：
     * - 前台进程: 0
     * - 可见进程: 100
     * - 后台服务: 500+
     * - 缓存进程: 900+
     */
    fun getOomAdj(): Int {
        return try {
            val file = File("/proc/self/oom_score_adj")
            if (file.exists()) {
                BufferedReader(FileReader(file)).use { reader ->
                    reader.readLine()?.trim()?.toIntOrNull() ?: 1000
                }
            } else {
                // 尝试旧版本路径
                val oldFile = File("/proc/self/oom_adj")
                if (oldFile.exists()) {
                    BufferedReader(FileReader(oldFile)).use { reader ->
                        val adj = reader.readLine()?.trim()?.toIntOrNull() ?: 15
                        // 转换旧版本值到新版本
                        adj * 1000 / 17
                    }
                } else {
                    1000
                }
            }
        } catch (e: Exception) {
            FwLog.e("读取 OOM adj 失败: ${e.message}", e)
            1000
        }
    }

    /**
     * 获取当前进程的内存使用情况
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        return MemoryInfo(
            totalSystemMemory = memInfo.totalMem,
            availableSystemMemory = memInfo.availMem,
            lowMemoryThreshold = memInfo.threshold,
            isLowMemory = memInfo.lowMemory,
            appUsedMemory = usedMemory,
            appMaxMemory = maxMemory
        )
    }

    /**
     * 获取进程状态摘要
     */
    fun getProcessSummary(context: Context): String {
        val importance = getCurrentImportance(context)
        val importanceName = getImportanceName(importance)
        val oomAdj = getOomAdj()
        val memInfo = getMemoryInfo(context)

        return buildString {
            appendLine("=== 进程状态 ===")
            appendLine("进程 PID: ${Process.myPid()}")
            appendLine("进程 UID: ${Process.myUid()}")
            appendLine("重要性级别: $importanceName ($importance)")
            appendLine("OOM adj: $oomAdj")
            appendLine()
            appendLine("=== 内存信息 ===")
            appendLine("系统总内存: ${memInfo.totalSystemMemory / 1024 / 1024} MB")
            appendLine("系统可用内存: ${memInfo.availableSystemMemory / 1024 / 1024} MB")
            appendLine("低内存阈值: ${memInfo.lowMemoryThreshold / 1024 / 1024} MB")
            appendLine("是否低内存: ${memInfo.isLowMemory}")
            appendLine("应用已用内存: ${memInfo.appUsedMemory / 1024 / 1024} MB")
            appendLine("应用最大内存: ${memInfo.appMaxMemory / 1024 / 1024} MB")
        }
    }

    /**
     * 检查是否有可能被杀
     *
     * 根据当前状态评估被杀风险
     */
    fun checkKillRisk(context: Context): KillRisk {
        val importance = getCurrentImportance(context)
        val oomAdj = getOomAdj()
        val memInfo = getMemoryInfo(context)

        val risk = when {
            importance <= Importance.FOREGROUND -> KillRisk.VERY_LOW
            importance <= Importance.FOREGROUND_SERVICE -> KillRisk.LOW
            importance <= Importance.VISIBLE -> KillRisk.MEDIUM
            importance <= Importance.SERVICE && !memInfo.isLowMemory -> KillRisk.MEDIUM
            importance <= Importance.SERVICE && memInfo.isLowMemory -> KillRisk.HIGH
            else -> KillRisk.VERY_HIGH
        }

        FwLog.d("进程被杀风险: $risk (重要性=$importance, OOM adj=$oomAdj, 低内存=${memInfo.isLowMemory})")
        return risk
    }

    /**
     * 内存信息
     */
    data class MemoryInfo(
        val totalSystemMemory: Long,
        val availableSystemMemory: Long,
        val lowMemoryThreshold: Long,
        val isLowMemory: Boolean,
        val appUsedMemory: Long,
        val appMaxMemory: Long
    )

    /**
     * 被杀风险等级
     */
    enum class KillRisk {
        VERY_LOW,   // 极低风险（前台）
        LOW,        // 低风险（前台服务）
        MEDIUM,     // 中等风险（可见/服务）
        HIGH,       // 高风险（低内存）
        VERY_HIGH   // 极高风险（缓存进程）
    }
}
