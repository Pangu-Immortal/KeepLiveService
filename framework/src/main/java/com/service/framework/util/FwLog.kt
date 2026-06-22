/**
 * ===============================================================================
 * Fw Android Keep-Alive Framework - Enhanced Logging Utility
 * ===============================================================================
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal
 * @createDate 2025-12-09
 *
 * @description
 * 增强型日志工具类，提供详细的调试信息输出
 * Enhanced logging utility class providing detailed debug information output
 *
 * @features
 * - 多级别日志输出（VERBOSE, DEBUG, INFO, WARN, ERROR, WTF）
 * - 自动获取调用者信息（类名、方法名、行号）
 * - 线程信息显示
 * - 时间戳精确到毫秒
 * - 进程ID和线程ID显示
 * - 堆栈跟踪信息
 * - 性能计时功能
 * - 条件日志输出
 * - JSON格式化输出
 * - 大文本分段输出
 *
 * @usage
 * ```kotlin
 * // 基础用法
 * FwLog.d("调试信息")
 * FwLog.i("普通信息")
 * FwLog.w("警告信息")
 * FwLog.e("错误信息", exception)
 *
 * // 详细用法
 * FwLog.verbose("详细调试信息")
 * FwLog.debug("策略名称", "调试信息")
 * FwLog.info("策略名称", "普通信息")
 *
 * // 性能计时
 * FwLog.startTimer("任务名称")
 * // ... 执行任务 ...
 * FwLog.endTimer("任务名称")
 *
 * // 堆栈跟踪
 * FwLog.printStackTrace("自定义消息")
 * ```
 *
 * @securityResearch
 * 详细的日志输出用于安全研究，分析保活机制的执行流程
 * Detailed logging for security research to analyze keep-alive mechanism execution flow
 *
 * ===============================================================================
 */

package com.service.framework.util

import android.os.Build
import android.os.Process
import android.util.Log
import com.service.framework.Fw
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 增强型日志工具类
 *
 * 提供详细的日志输出功能，用于调试和安全研究分析
 *
 * Enhanced logging utility providing detailed log output for debugging and security research
 *
 * @author https://github.com/Pangu-Immortal/KeepAlivePerfect
 * @since 2.1.0
 */
object FwLog {

    // ==================== 常量定义 ====================

    /**
     * 日志消息最大长度（Android Logcat 限制约 4000 字符）
     * Maximum log message length (Android Logcat limits to ~4000 characters)
     */
    private const val MAX_LOG_LENGTH = 3800

    /**
     * 日志分隔线
     * Log separator line
     */
    private const val SEPARATOR = "════════════════════════════════════════════════════════════════"

    /**
     * 日志分隔线（细）
     * Log separator line (thin)
     */
    private const val SEPARATOR_THIN = "────────────────────────────────────────────────────────────────"

    /**
     * 日志边框
     * Log border
     */
    private const val BORDER_TOP = "┌$SEPARATOR"
    private const val BORDER_BOTTOM = "└$SEPARATOR"
    private const val BORDER_MIDDLE = "├$SEPARATOR_THIN"
    private const val BORDER_LEFT = "│ "

    // ==================== 配置属性 ====================

    /**
     * 获取日志标签
     * 优先使用配置的标签，默认为 "Fw"
     *
     * Get log tag, use configured tag first, default to "Fw"
     */
    private val tag: String
        get() = if (Fw.isInitialized()) Fw.config.logTag else "Fw"

    /**
     * 是否启用调试日志
     * Whether debug logging is enabled
     */
    private val isDebug: Boolean
        get() = if (Fw.isInitialized()) Fw.config.enableDebugLog else true

    /**
     * 是否显示线程信息
     * Whether to show thread information
     */
    var showThreadInfo: Boolean = true

    /**
     * 是否显示调用者信息（类名、方法名、行号）
     * Whether to show caller information (class name, method name, line number)
     */
    var showCallerInfo: Boolean = true

    /**
     * 是否显示时间戳
     * Whether to show timestamp
     */
    var showTimestamp: Boolean = true

    /**
     * 是否使用美化格式
     * Whether to use pretty format with borders
     */
    var usePrettyFormat: Boolean = false

    // ==================== 计时器存储 ====================

    /**
     * 计时器存储
     * Timer storage for performance measurement
     */
    private val timers = ConcurrentHashMap<String, Long>()

    /**
     * 日期格式化器
     * Date formatter for timestamp
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    // ==================== 基础日志方法 ====================

    /**
     * 输出 VERBOSE 级别日志
     * 最低级别，用于最详细的调试信息
     *
     * Output VERBOSE level log
     * Lowest level, for most detailed debug information
     *
     * @param message 日志消息 / Log message
     */
    fun v(message: String) {
        if (isDebug) {
            logInternal(Log.VERBOSE, tag, buildMessage(message))
        }
    }

    /**
     * 输出 VERBOSE 级别日志（带子标签）
     *
     * Output VERBOSE level log with sub-tag
     *
     * @param subTag 子标签（如策略名称）/ Sub-tag (e.g., strategy name)
     * @param message 日志消息 / Log message
     */
    fun verbose(subTag: String, message: String) {
        if (isDebug) {
            logInternal(Log.VERBOSE, "$tag:$subTag", buildMessage(message))
        }
    }

    /**
     * 输出 DEBUG 级别日志
     * 用于调试信息
     *
     * Output DEBUG level log
     * For debug information
     *
     * @param message 日志消息 / Log message
     */
    fun d(message: String) {
        if (isDebug) {
            logInternal(Log.DEBUG, tag, buildMessage(message))
        }
    }

    /**
     * 输出 DEBUG 级别日志（带子标签）
     *
     * Output DEBUG level log with sub-tag
     *
     * @param subTag 子标签（如策略名称）/ Sub-tag (e.g., strategy name)
     * @param message 日志消息 / Log message
     */
    fun debug(subTag: String, message: String) {
        if (isDebug) {
            logInternal(Log.DEBUG, "$tag:$subTag", buildMessage(message))
        }
    }

    /**
     * 输出 INFO 级别日志
     * 用于一般信息
     *
     * Output INFO level log
     * For general information
     *
     * @param message 日志消息 / Log message
     */
    fun i(message: String) {
        if (isDebug) {
            logInternal(Log.INFO, tag, buildMessage(message))
        }
    }

    /**
     * 输出 INFO 级别日志（带子标签）
     *
     * Output INFO level log with sub-tag
     *
     * @param subTag 子标签（如策略名称）/ Sub-tag (e.g., strategy name)
     * @param message 日志消息 / Log message
     */
    fun info(subTag: String, message: String) {
        if (isDebug) {
            logInternal(Log.INFO, "$tag:$subTag", buildMessage(message))
        }
    }

    /**
     * 输出 WARN 级别日志
     * 用于警告信息（始终输出，不受 isDebug 控制）
     *
     * Output WARN level log
     * For warning information (always output, not controlled by isDebug)
     *
     * @param message 日志消息 / Log message
     */
    fun w(message: String) {
        logInternal(Log.WARN, tag, buildMessage(message))
    }

    /**
     * 输出 WARN 级别日志（带子标签）
     *
     * Output WARN level log with sub-tag
     *
     * @param subTag 子标签（如策略名称）/ Sub-tag (e.g., strategy name)
     * @param message 日志消息 / Log message
     */
    fun warn(subTag: String, message: String) {
        logInternal(Log.WARN, "$tag:$subTag", buildMessage(message))
    }

    /**
     * 输出 ERROR 级别日志
     * 用于错误信息（始终输出，不受 isDebug 控制）
     *
     * Output ERROR level log
     * For error information (always output, not controlled by isDebug)
     *
     * @param message 日志消息 / Log message
     * @param throwable 异常对象（可选）/ Exception object (optional)
     */
    fun e(message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${getStackTraceString(throwable)}"
        } else {
            message
        }
        logInternal(Log.ERROR, tag, buildMessage(fullMessage))
    }

    /**
     * 输出 ERROR 级别日志（带子标签）
     *
     * Output ERROR level log with sub-tag
     *
     * @param subTag 子标签（如策略名称）/ Sub-tag (e.g., strategy name)
     * @param message 日志消息 / Log message
     * @param throwable 异常对象（可选）/ Exception object (optional)
     */
    fun error(subTag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${getStackTraceString(throwable)}"
        } else {
            message
        }
        logInternal(Log.ERROR, "$tag:$subTag", buildMessage(fullMessage))
    }

    /**
     * 输出 WTF (What a Terrible Failure) 级别日志
     * 用于不应该发生的严重错误
     *
     * Output WTF (What a Terrible Failure) level log
     * For serious errors that should never happen
     *
     * @param message 日志消息 / Log message
     * @param throwable 异常对象（可选）/ Exception object (optional)
     */
    fun wtf(message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${getStackTraceString(throwable)}"
        } else {
            message
        }
        Log.wtf(tag, buildMessage(fullMessage))
    }

    // ==================== 特殊格式日志方法 ====================

    /**
     * 输出分隔线日志
     * 用于日志分组
     *
     * Output separator line log
     * For log grouping
     *
     * @param title 分隔标题（可选）/ Separator title (optional)
     */
    fun separator(title: String = "") {
        if (!isDebug) return

        if (title.isEmpty()) {
            Log.d(tag, SEPARATOR)
        } else {
            Log.d(tag, "═══════════════════ $title ═══════════════════")
        }
    }

    /**
     * 输出带边框的日志块
     * 用于重要信息突出显示
     *
     * Output log block with border
     * For highlighting important information
     *
     * @param title 标题 / Title
     * @param content 内容 / Content
     */
    fun block(title: String, content: String) {
        if (!isDebug) return

        Log.d(tag, BORDER_TOP)
        Log.d(tag, "$BORDER_LEFT$title")
        Log.d(tag, BORDER_MIDDLE)
        content.split("\n").forEach { line ->
            Log.d(tag, "$BORDER_LEFT$line")
        }
        Log.d(tag, BORDER_BOTTOM)
    }

    /**
     * 输出 JSON 格式化日志
     * 自动格式化 JSON 字符串
     *
     * Output formatted JSON log
     * Automatically format JSON string
     *
     * @param json JSON 字符串 / JSON string
     */
    fun json(json: String?) {
        if (!isDebug || json.isNullOrEmpty()) {
            d("JSON is null or empty")
            return
        }

        try {
            val formattedJson = when {
                json.startsWith("{") -> JSONObject(json).toString(2)
                json.startsWith("[") -> JSONArray(json).toString(2)
                else -> json
            }
            block("JSON", formattedJson)
        } catch (e: Exception) {
            e("JSON parse error: ${e.message}")
            d(json)
        }
    }

    /**
     * 输出堆栈跟踪日志
     * 打印当前调用栈
     *
     * Output stack trace log
     * Print current call stack
     *
     * @param message 附加消息（可选）/ Additional message (optional)
     */
    fun printStackTrace(message: String = "Stack Trace") {
        if (!isDebug) return

        val stackTrace = Thread.currentThread().stackTrace
            .drop(3) // 跳过 getStackTrace, printStackTrace, 和调用者
            .take(10) // 只取前10层
            .joinToString("\n") { element ->
                "    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
            }

        block(message, stackTrace)
    }

    // ==================== 性能计时方法 ====================

    /**
     * 开始计时
     * 记录任务开始时间
     *
     * Start timer
     * Record task start time
     *
     * @param timerName 计时器名称 / Timer name
     */
    fun startTimer(timerName: String) {
        timers[timerName] = System.currentTimeMillis()
        d("⏱️ Timer [$timerName] started")
    }

    /**
     * 结束计时并输出耗时
     *
     * End timer and output elapsed time
     *
     * @param timerName 计时器名称 / Timer name
     * @return 耗时（毫秒），如果计时器不存在返回 -1 / Elapsed time in milliseconds, -1 if timer not found
     */
    fun endTimer(timerName: String): Long {
        val startTime = timers.remove(timerName)
        return if (startTime != null) {
            val elapsed = System.currentTimeMillis() - startTime
            d("⏱️ Timer [$timerName] ended: ${elapsed}ms")
            elapsed
        } else {
            w("⏱️ Timer [$timerName] not found")
            -1
        }
    }

    /**
     * 测量代码块执行时间
     *
     * Measure code block execution time
     *
     * @param blockName 代码块名称 / Block name
     * @param block 要执行的代码块 / Code block to execute
     * @return 代码块返回值 / Code block return value
     */
    inline fun <T> measureTime(blockName: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        d("⏱️ [$blockName] took ${elapsed}ms")
        return result
    }

    // ==================== 条件日志方法 ====================

    /**
     * 条件日志输出
     * 只有当条件为真时才输出日志
     *
     * Conditional log output
     * Only output log when condition is true
     *
     * @param condition 条件 / Condition
     * @param message 日志消息 / Log message
     */
    fun dIf(condition: Boolean, message: () -> String) {
        if (isDebug && condition) {
            d(message())
        }
    }

    /**
     * 断言日志
     * 当条件为假时输出错误日志
     *
     * Assert log
     * Output error log when condition is false
     *
     * @param condition 条件（期望为真）/ Condition (expected to be true)
     * @param message 断言失败时的消息 / Message when assertion fails
     */
    fun assert(condition: Boolean, message: String) {
        if (!condition) {
            e("❌ Assertion failed: $message")
            printStackTrace("Assertion Stack Trace")
        }
    }

    // ==================== 系统信息日志方法 ====================

    /**
     * 输出设备信息日志
     *
     * Output device information log
     */
    fun logDeviceInfo() {
        if (!isDebug) return

        block("Device Information", """
            |Manufacturer: ${Build.MANUFACTURER}
            |Model: ${Build.MODEL}
            |Brand: ${Build.BRAND}
            |Device: ${Build.DEVICE}
            |Product: ${Build.PRODUCT}
            |SDK Version: ${Build.VERSION.SDK_INT}
            |Android Version: ${Build.VERSION.RELEASE}
            |Build ID: ${Build.ID}
            |Fingerprint: ${Build.FINGERPRINT}
        """.trimMargin())
    }

    /**
     * 输出进程信息日志
     *
     * Output process information log
     */
    fun logProcessInfo() {
        if (!isDebug) return

        block("Process Information", """
            |PID: ${Process.myPid()}
            |UID: ${Process.myUid()}
            |TID: ${Process.myTid()}
            |Thread Name: ${Thread.currentThread().name}
            |Thread ID: ${Thread.currentThread().id}
            |Thread Priority: ${Thread.currentThread().priority}
            |Is Main Thread: ${Thread.currentThread().name == "main"}
        """.trimMargin())
    }

    /**
     * 输出内存信息日志
     *
     * Output memory information log
     */
    fun logMemoryInfo() {
        if (!isDebug) return

        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory

        block("Memory Information", """
            |Max Memory: ${maxMemory}MB
            |Total Memory: ${totalMemory}MB
            |Used Memory: ${usedMemory}MB
            |Free Memory: ${freeMemory}MB
            |Usage: ${usedMemory * 100 / maxMemory}%
        """.trimMargin())
    }

    // ==================== 框架专用日志方法 ====================

    /**
     * 输出策略启动日志
     *
     * Output strategy start log
     *
     * @param strategyName 策略名称 / Strategy name
     * @param enabled 是否启用 / Whether enabled
     */
    fun logStrategyStart(strategyName: String, enabled: Boolean) {
        if (!isDebug) return

        val status = if (enabled) "✅ ENABLED" else "❌ DISABLED"
        d("📌 Strategy [$strategyName] $status")
    }

    /**
     * 输出策略执行结果日志
     *
     * Output strategy execution result log
     *
     * @param strategyName 策略名称 / Strategy name
     * @param success 是否成功 / Whether successful
     * @param details 详细信息（可选）/ Details (optional)
     */
    fun logStrategyResult(strategyName: String, success: Boolean, details: String = "") {
        if (!isDebug) return

        val status = if (success) "✅ SUCCESS" else "❌ FAILED"
        val detailsStr = if (details.isNotEmpty()) " - $details" else ""
        d("📊 Strategy [$strategyName] $status$detailsStr")
    }

    /**
     * 输出广播接收日志
     *
     * Output broadcast receive log
     *
     * @param receiverName 接收器名称 / Receiver name
     * @param action 广播 Action / Broadcast action
     * @param extras 额外信息（可选）/ Extra information (optional)
     */
    fun logBroadcastReceived(receiverName: String, action: String, extras: String = "") {
        if (!isDebug) return

        val extrasStr = if (extras.isNotEmpty()) "\n    Extras: $extras" else ""
        d("📡 Broadcast [$receiverName]\n    Action: $action$extrasStr")
    }

    /**
     * 输出服务生命周期日志
     *
     * Output service lifecycle log
     *
     * @param serviceName 服务名称 / Service name
     * @param lifecycle 生命周期事件 / Lifecycle event
     */
    fun logServiceLifecycle(serviceName: String, lifecycle: String) {
        if (!isDebug) return

        d("🔄 Service [$serviceName] => $lifecycle")
    }

    /**
     * 输出 Native 层日志
     *
     * Output Native layer log
     *
     * @param component 组件名称 / Component name
     * @param message 日志消息 / Log message
     */
    fun logNative(component: String, message: String) {
        if (!isDebug) return

        d("🔧 Native [$component] $message")
    }

    // ==================== 内部方法 ====================

    /**
     * 构建完整的日志消息
     * 包含时间戳、线程信息、调用者信息等
     *
     * Build complete log message
     * Including timestamp, thread info, caller info, etc.
     *
     * @param message 原始消息 / Original message
     * @return 完整消息 / Complete message
     */
    private fun buildMessage(message: String): String {
        val sb = StringBuilder()

        // 时间戳
        if (showTimestamp) {
            sb.append("[${dateFormat.format(Date())}] ")
        }

        // 线程信息
        if (showThreadInfo) {
            val thread = Thread.currentThread()
            sb.append("[${thread.name}:${thread.id}] ")
        }

        // 调用者信息
        if (showCallerInfo) {
            val caller = getCallerInfo()
            if (caller.isNotEmpty()) {
                sb.append("[$caller] ")
            }
        }

        // 消息内容
        sb.append(message)

        return sb.toString()
    }

    /**
     * 获取调用者信息
     * 返回格式：类名.方法名:行号
     *
     * Get caller information
     * Return format: ClassName.methodName:lineNumber
     *
     * @return 调用者信息字符串 / Caller information string
     */
    private fun getCallerInfo(): String {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            // 找到第一个不是 FwLog 类的调用者
            val caller = stackTrace.find { element ->
                !element.className.contains("FwLog") &&
                !element.className.contains("Thread") &&
                !element.className.contains("VMStack")
            }
            if (caller != null) {
                val simpleClassName = caller.className.substringAfterLast(".")
                "$simpleClassName.${caller.methodName}:${caller.lineNumber}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取异常堆栈跟踪字符串
     *
     * Get exception stack trace string
     *
     * @param throwable 异常对象 / Exception object
     * @return 堆栈跟踪字符串 / Stack trace string
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    /**
     * 内部日志输出方法
     * 处理超长日志分段输出
     *
     * Internal log output method
     * Handle long log message segmentation
     *
     * @param priority 日志优先级 / Log priority
     * @param tag 日志标签 / Log tag
     * @param message 日志消息 / Log message
     */
    private fun logInternal(priority: Int, tag: String, message: String) {
        // 如果消息长度超过限制，分段输出
        if (message.length > MAX_LOG_LENGTH) {
            var start = 0
            var end: Int
            var partIndex = 1

            while (start < message.length) {
                end = minOf(start + MAX_LOG_LENGTH, message.length)
                val part = message.substring(start, end)

                Log.println(priority, tag, "[Part $partIndex] $part")

                start = end
                partIndex++
            }
        } else {
            Log.println(priority, tag, message)
        }
    }
}
