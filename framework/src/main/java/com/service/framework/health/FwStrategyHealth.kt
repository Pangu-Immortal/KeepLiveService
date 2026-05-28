/**
 * ============================================================================
 * FwStrategyHealth.kt - 保活策略健康状态模型
 * ============================================================================
 *
 * 功能简介：
 *   定义保活策略的统一标识、运行状态、健康检查结果和汇总报告。
 *   这些模型用于把 Job、Work、Alarm、Native、VPN、MediaRoute 等能力
 *   从“只启动一次”升级为“可巡检、可补偿、可追踪”。
 *
 * 主要函数：
 *   - FwHealthReport.toLogString(): 输出完整巡检日志
 *   - FwStrategyHealth.isHealthy: 判断单项策略是否健康
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.health

/**
 * 保活策略统一标识。
 *
 * 每个枚举对应一个可巡检的策略或环境能力。
 */
enum class FwStrategyKey(val displayName: String) {
    FOREGROUND_SERVICE("核心前台服务"),
    SILENT_AUDIO("静默音频"),
    DUAL_PROCESS("Java 双进程"),
    JOB_SCHEDULER("JobScheduler"),
    WORK_MANAGER("WorkManager"),
    ALARM_MANAGER("AlarmManager"),
    ACCOUNT_SYNC("账户同步"),
    CONTENT_OBSERVER("ContentObserver"),
    FILE_OBSERVER("FileObserver"),
    NATIVE_DAEMON("Native 守护"),
    NATIVE_SOCKET("Native Socket"),
    MEDIA_ROUTE("MediaRoute"),
    MEDIA_BROWSER("MediaBrowser"),
    VPN_SERVICE("VPN 服务"),
    COMPANION_DEVICE("CompanionDevice"),
    MEDIA_SESSION_NOTIFICATION("MediaSession 通知"),
    CALL_STYLE_NOTIFICATION("CallStyle 通知"),
    DEVICE_ADMIN("设备管理员"),
    ACCESSIBILITY_SERVICE("无障碍服务"),
    NOTIFICATION_LISTENER("通知监听服务"),
    TILE_SERVICE("快捷磁贴"),
    WIDGET_PROVIDER("桌面小组件"),
    DREAM_SERVICE("屏保服务"),
    BATTERY_OPTIMIZATION("电池优化"),
    VENDOR_INTEGRATION("厂商环境")
}

/**
 * 策略健康状态。
 */
enum class FwStrategyHealthState {
    DISABLED,
    RUNNING,
    DEGRADED,
    STOPPED,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    ERROR
}

/**
 * 策略运行时统计。
 *
 * @property status 最近一次运行状态
 * @property startTimeMillis 最近一次启动时间
 * @property lastActiveTimeMillis 最近一次活跃时间
 * @property triggerCount 触发次数
 * @property errorCount 错误次数
 * @property lastError 最后一次错误
 */
data class FwStrategyRuntimeState(
    val status: FwStrategyHealthState = FwStrategyHealthState.STOPPED,
    val startTimeMillis: Long = 0L,
    val lastActiveTimeMillis: Long = 0L,
    val triggerCount: Int = 0,
    val errorCount: Int = 0,
    val lastError: String = ""
)

/**
 * 单项策略巡检结果。
 *
 * @property key 策略标识
 * @property state 当前健康状态
 * @property enabled 配置层是否启用
 * @property supported 当前系统是否支持
 * @property message 可读说明
 * @property runtimeState 运行时统计
 * @property recovered 本次巡检是否触发补偿动作
 */
data class FwStrategyHealth(
    val key: FwStrategyKey,
    val state: FwStrategyHealthState,
    val enabled: Boolean,
    val supported: Boolean,
    val message: String,
    val runtimeState: FwStrategyRuntimeState = FwStrategyRuntimeState(),
    val recovered: Boolean = false
) {
    val isHealthy: Boolean
        get() = state == FwStrategyHealthState.RUNNING || state == FwStrategyHealthState.DISABLED
}

/**
 * 全量巡检报告。
 *
 * @property generatedAtMillis 生成时间
 * @property processImportance 进程重要性
 * @property oomAdj OOM 调整值
 * @property killRisk 被杀风险
 * @property strategies 策略结果列表
 */
data class FwHealthReport(
    val generatedAtMillis: Long,
    val processImportance: Int,
    val oomAdj: Int,
    val killRisk: String,
    val strategies: List<FwStrategyHealth>
) {
    val healthyCount: Int
        get() = strategies.count { it.isHealthy }

    val issueCount: Int
        get() = strategies.size - healthyCount

    /**
     * 把巡检报告转换为日志文本。
     */
    fun toLogString(): String {
        return buildString {
            appendLine("================= Fw 健康巡检报告 ================")
            appendLine("生成时间: $generatedAtMillis")
            appendLine("进程重要性: $processImportance")
            appendLine("OOM adj: $oomAdj")
            appendLine("被杀风险: $killRisk")
            appendLine("健康策略: $healthyCount/${strategies.size}")
            appendLine("问题策略: $issueCount")
            strategies.forEach { health ->
                appendLine(
                    "- ${health.key.displayName}: ${health.state} | " +
                        "启用=${health.enabled}, 支持=${health.supported}, " +
                        "补偿=${health.recovered}, 说明=${health.message}"
                )
            }
            appendLine("==================================================")
        }
    }
}
