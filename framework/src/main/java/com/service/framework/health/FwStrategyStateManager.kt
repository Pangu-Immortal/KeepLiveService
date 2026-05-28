/**
 * ============================================================================
 * FwStrategyStateManager.kt - 保活策略运行时状态管理器
 * ============================================================================
 *
 * 功能简介：
 *   保存每个保活策略的运行状态、启动时间、最近活跃时间、触发次数、
 *   错误次数和最后错误，吸收逆向 SDK 中 StrategyState 的状态化管理思想。
 *
 * 主要函数：
 *   - markStarted(): 标记策略启动
 *   - markTriggered(): 标记策略触发
 *   - markError(): 标记策略错误
 *   - snapshot(): 获取策略状态快照
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.health

import com.service.framework.util.FwLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 保活策略运行时状态管理器。
 */
object FwStrategyStateManager {

    private val states = ConcurrentHashMap<FwStrategyKey, FwStrategyRuntimeState>()

    /**
     * 标记策略已启动。
     */
    fun markStarted(key: FwStrategyKey, message: String = "") {
        val now = System.currentTimeMillis()
        val oldState = states[key] ?: FwStrategyRuntimeState()
        states[key] = oldState.copy(
            status = FwStrategyHealthState.RUNNING,
            startTimeMillis = if (oldState.startTimeMillis == 0L) now else oldState.startTimeMillis,
            lastActiveTimeMillis = now,
            triggerCount = oldState.triggerCount + 1,
            lastError = ""
        )
        FwLog.d("FwState: ${key.displayName} 已启动 $message")
    }

    /**
     * 标记策略发生触发事件。
     */
    fun markTriggered(key: FwStrategyKey, message: String = "") {
        val now = System.currentTimeMillis()
        val oldState = states[key] ?: FwStrategyRuntimeState()
        states[key] = oldState.copy(
            status = if (oldState.status == FwStrategyHealthState.DISABLED) {
                FwStrategyHealthState.DISABLED
            } else {
                FwStrategyHealthState.RUNNING
            },
            lastActiveTimeMillis = now,
            triggerCount = oldState.triggerCount + 1
        )
        FwLog.d("FwState: ${key.displayName} 已触发 $message")
    }

    /**
     * 标记策略已停止。
     */
    fun markStopped(key: FwStrategyKey, message: String = "") {
        val now = System.currentTimeMillis()
        val oldState = states[key] ?: FwStrategyRuntimeState()
        states[key] = oldState.copy(
            status = FwStrategyHealthState.STOPPED,
            lastActiveTimeMillis = now
        )
        FwLog.d("FwState: ${key.displayName} 已停止 $message")
    }

    /**
     * 标记策略被配置关闭。
     */
    fun markDisabled(key: FwStrategyKey, message: String = "") {
        val now = System.currentTimeMillis()
        val oldState = states[key] ?: FwStrategyRuntimeState()
        states[key] = oldState.copy(
            status = FwStrategyHealthState.DISABLED,
            lastActiveTimeMillis = now
        )
        FwLog.d("FwState: ${key.displayName} 已禁用 $message")
    }

    /**
     * 标记策略出现错误。
     */
    fun markError(key: FwStrategyKey, error: String, throwable: Throwable? = null) {
        val now = System.currentTimeMillis()
        val oldState = states[key] ?: FwStrategyRuntimeState()
        states[key] = oldState.copy(
            status = FwStrategyHealthState.ERROR,
            lastActiveTimeMillis = now,
            errorCount = oldState.errorCount + 1,
            lastError = error
        )
        if (throwable == null) {
            FwLog.e("FwState: ${key.displayName} 错误 - $error")
        } else {
            FwLog.e("FwState: ${key.displayName} 错误 - $error", throwable)
        }
    }

    /**
     * 获取指定策略状态快照。
     */
    fun snapshot(key: FwStrategyKey): FwStrategyRuntimeState {
        return states[key] ?: FwStrategyRuntimeState()
    }

    /**
     * 清空全部状态。
     */
    fun clear() {
        states.clear()
        FwLog.d("FwState: 已清空所有策略状态")
    }
}
