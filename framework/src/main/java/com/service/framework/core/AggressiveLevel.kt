/**
 * ============================================================================
 * AggressiveLevel.kt - 保活能耗等级枚举
 * ============================================================================
 *
 * 功能简介：
 *   控制保活策略的激进程度，平衡保活效果与电量消耗。
 *
 * 等级说明：
 *   - LOW：节能模式，最小化电量消耗，适合普通后台任务
 *   - MEDIUM：均衡模式，兼顾保活效果和电量消耗（默认）
 *   - HIGH：激进模式，最大化保活效果，适合即时通讯类应用
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.1
 */
package com.service.framework.core

/**
 * 保活能耗等级，控制策略激进程度
 */
enum class AggressiveLevel {
    LOW,    // 节能模式：静默音频 10s 间隔，AlarmManager 10min，守护进程 10s 检查
    MEDIUM, // 均衡模式：静默音频 5s 间隔，AlarmManager 5min，守护进程 3s 检查（默认）
    HIGH    // 激进模式：静默音频立即重播，AlarmManager 1min，守护进程 1s 检查
}
