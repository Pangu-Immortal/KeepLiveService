/**
 * ============================================================================
 * RestartProtection.kt - 连续重启保护机制
 * ============================================================================
 *
 * 功能简介：
 *   防止保活策略在极端情况下导致无限循环重启，耗尽电池。
 *   记录服务重启频率，超过阈值自动禁用守护策略一段时间。
 *
 * 核心机制：
 *   - SharedPreferences 记录最近 N 次重启时间戳
 *   - 如果在指定时间窗口内重启次数超过阈值，触发冷却期
 *   - 冷却期间所有主动拉起策略暂停
 *   - 冷却结束后自动恢复
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.1
 */
package com.service.framework.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 连续重启保护，防止无限循环耗电
 */
object RestartProtection {

    private const val PREFS_NAME = "fw_restart_protection"        // SP 文件名
    private const val KEY_RESTART_TIMESTAMPS = "restart_timestamps" // 重启时间戳列表
    private const val KEY_COOLDOWN_UNTIL = "cooldown_until"        // 冷却期截止时间
    private const val MAX_RESTARTS = 10                            // 时间窗口内最大重启次数
    private const val TIME_WINDOW_MS = 60_000L                     // 时间窗口：60秒
    private const val COOLDOWN_DURATION_MS = 5 * 60_000L           // 冷却期：5分钟

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 记录一次重启事件，返回是否允许继续重启
     * @return true 允许重启，false 触发冷却期应暂停
     */
    fun recordRestart(context: Context): Boolean {
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        // 检查是否在冷却期
        val cooldownUntil = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        if (now < cooldownUntil) {
            FwLog.w("处于冷却期，剩余 ${(cooldownUntil - now) / 1000}s，暂停重启")
            return false // 冷却期内不允许重启
        }

        // 获取历史时间戳
        val timestamps = getTimestamps(prefs)
        timestamps.add(now) // 添加当前时间

        // 移除时间窗口之外的记录
        val windowStart = now - TIME_WINDOW_MS
        timestamps.removeAll { it < windowStart }

        // 保存更新后的时间戳
        saveTimestamps(prefs, timestamps)

        // 检查是否超过阈值
        if (timestamps.size >= MAX_RESTARTS) {
            FwLog.w("${TIME_WINDOW_MS / 1000}s 内重启 ${timestamps.size} 次，触发 ${COOLDOWN_DURATION_MS / 1000}s 冷却期")
            prefs.edit().putLong(KEY_COOLDOWN_UNTIL, now + COOLDOWN_DURATION_MS).apply()
            timestamps.clear()                     // 清空计数器
            saveTimestamps(prefs, timestamps)
            return false // 触发冷却，不允许重启
        }

        FwLog.d("重启记录: ${timestamps.size}/$MAX_RESTARTS（${TIME_WINDOW_MS / 1000}s 窗口）")
        return true // 允许重启
    }

    /**
     * 检查当前是否处于冷却期
     */
    fun isInCooldown(context: Context): Boolean {
        val cooldownUntil = getPrefs(context).getLong(KEY_COOLDOWN_UNTIL, 0L)
        return System.currentTimeMillis() < cooldownUntil
    }

    /**
     * 手动重置保护状态（用于调试）
     */
    fun reset(context: Context) {
        getPrefs(context).edit().clear().apply()
        FwLog.d("重启保护已重置")
    }

    /**
     * 从 SharedPreferences 读取时间戳列表
     */
    private fun getTimestamps(prefs: SharedPreferences): MutableList<Long> {
        val raw = prefs.getString(KEY_RESTART_TIMESTAMPS, "") ?: ""
        if (raw.isEmpty()) return mutableListOf()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
    }

    /**
     * 保存时间戳列表到 SharedPreferences
     */
    private fun saveTimestamps(prefs: SharedPreferences, timestamps: List<Long>) {
        prefs.edit().putString(KEY_RESTART_TIMESTAMPS, timestamps.joinToString(",")).apply()
    }
}
