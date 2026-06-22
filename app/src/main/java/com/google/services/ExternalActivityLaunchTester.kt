package com.google.services

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.service.framework.start.FwStart
import com.service.framework.start.FwStartResult
import com.service.framework.start.FwStartStrategy

/**
 * 体外 Activity 周期启动测试器。
 *
 * 功能：在 app 层每 10 秒通过 FwStart 全量审计入口尝试启动体外 Activity，
 * 用于 Home 后观察最新 Android 对后台 startActivity 的真实拦截行为。
 */
object ExternalActivityLaunchTester {

    private const val TAG = "FwExternalTester"
    private const val INTERVAL_MS = 10_000L
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var launchCount = 0
    private var running = false

    private val launchRunnable = object : Runnable {
        override fun run() {
            val context = appContext
            if (!running || context == null) {
                Log.w(TAG, "循环已停止或 Context 为空，跳过本轮")
                return
            }
            launchCount += 1
            Log.d(TAG, "第 $launchCount 轮：开始 10 秒周期体外 Activity 全量启动测试")
            startAuditOnce(context, "loop-$launchCount")
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    /** 启动每 10 秒一次的后台拉起测试循环。 */
    fun startLoop(context: Context) {
        appContext = context.applicationContext
        if (running) {
            Log.d(TAG, "10 秒周期测试已在运行，当前次数=$launchCount")
            return
        }
        running = true
        launchCount = 0
        Log.d(TAG, "启动 10 秒周期测试；请按 Home 键把应用置于后台后继续观察日志")
        handler.removeCallbacks(launchRunnable)
        handler.post(launchRunnable)
    }

    /** 停止周期测试循环。 */
    fun stopLoop() {
        running = false
        handler.removeCallbacks(launchRunnable)
        Log.d(TAG, "停止 10 秒周期测试，累计执行次数=$launchCount")
    }

    /** 查询周期测试是否正在运行。 */
    fun isRunning(): Boolean = running

    /** 使用默认可执行策略单次启动体外 Activity。 */
    fun startDefaultOnce(context: Context): FwStartResult {
        val intent = buildExternalIntent(context, "default-once")
        Log.d(TAG, "单次默认策略启动：mask=${FwStartStrategy.defaultExecutableMask}")
        val result = FwStart.start(context, intent)
        Log.d(TAG, "单次默认策略结果：${formatResult(result)}")
        return result
    }

    /** 使用全量审计策略单次启动体外 Activity。 */
    fun startAuditOnce(context: Context, reason: String = "manual"): FwStartResult {
        val intent = buildExternalIntent(context, reason)
        Log.d(TAG, "单次全量审计启动：reason=$reason, allMask=${FwStartStrategy.allMask}")
        val auditResult = FwStart.startAuditAll(context, intent)
        Log.d(TAG, "startAuditAll 结果：${formatResult(auditResult)}")
        return auditResult
    }

    /** 验证 FwStart 的全部公开入口，显式由全量 API 验证器调用。 */
    fun verifyAllStartEntrypoints(context: Context, reason: String = "verify"): List<FwStartResult> {
        val auditResult = startAuditOnce(context, "$reason-audit")

        // 额外调用 startWithStrategies，验证 Iterable 策略入口真实可调用。
        val strategyResult = FwStart.startWithStrategies(
            context = context,
            intent = buildExternalIntent(context, "$reason-strategies"),
            strategies = FwStartStrategy.allStrategies
        )
        Log.d(TAG, "startWithStrategies 结果：${formatResult(strategyResult)}")

        // 额外调用 startWithMask，验证位掩码入口真实可调用。
        val maskResult = FwStart.startWithMask(
            context = context,
            intent = buildExternalIntent(context, "$reason-mask"),
            modeMask = FwStartStrategy.allMask
        )
        Log.d(TAG, "startWithMask 结果：${formatResult(maskResult)}")
        return listOf(auditResult, strategyResult, maskResult)
    }

    /** 构造体外 Activity Intent，使用独立任务栈与弹窗 Activity。 */
    private fun buildExternalIntent(context: Context, reason: String): Intent {
        return Intent(context, ExternalActivity::class.java).apply {
            putExtra("fw_external_reason", reason)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
    }

    /** 统一格式化 FwStart 执行结果，保证日志可检索。 */
    private fun formatResult(result: FwStartResult): String {
        return "success=${result.success}, nativeCode=${result.nativeCode}, " +
            "strategy=${result.strategy?.name}/${result.strategy?.displayName}, message=${result.message}"
    }
}
