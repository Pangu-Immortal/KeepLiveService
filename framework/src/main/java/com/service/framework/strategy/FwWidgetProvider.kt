/**
 * ============================================================================
 * FwWidgetProvider.kt - 桌面小组件保活策略
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android AppWidgetProvider 的保活策略。用户将小组件添加到桌面后，
 *   系统会定期触发 onUpdate 回调，利用此机制定期拉起保活服务。同时在小组件上
 *   展示保活状态信息，方便用户直观了解服务运行情况。
 *
 * 主要功能：
 *   - onUpdate(): 定期更新小组件并拉起保活服务
 *   - onEnabled(): 首个小组件被添加时记录日志
 *   - onDisabled(): 最后一个小组件被移除时记录日志
 *   - updateWidgetViews(): 使用 RemoteViews 更新小组件显示内容
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.service.framework.Fw
import com.service.framework.R
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 桌面小组件保活策略
 *
 * 核心机制：
 * 1. 小组件添加到桌面后，系统定期调用 onUpdate（默认 30 分钟）
 * 2. 每次 onUpdate 时拉起保活服务
 * 3. 小组件显示当前保活状态
 *
 * 安全研究要点：
 * - 需要用户手动添加桌面小组件
 * - 系统定期触发更新，可作为定时唤醒机制
 * - updatePeriodMillis 最小 30 分钟（系统限制）
 * - 是一种用户可感知的合法保活方式
 *
 * 注意：
 * - 需要在 AndroidManifest 中声明 receiver
 * - 需要关联 widget_info.xml 配置文件
 * - 需要提供 fw_widget_layout.xml 布局文件
 */
class FwWidgetProvider : AppWidgetProvider() {

    /**
     * 小组件更新回调
     *
     * 系统定期调用此方法，或在小组件首次添加时调用。
     * 利用此时机拉起保活服务并更新小组件显示状态。
     *
     * @param context 上下文
     * @param appWidgetManager 小组件管理器
     * @param appWidgetIds 需要更新的小组件 ID 数组
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        FwLog.d("FwWidgetProvider: onUpdate，更新 ${appWidgetIds.size} 个小组件")

        // 拉起前台保活服务
        ServiceStarter.startForegroundService(context, "桌面小组件更新")

        // 遍历所有需要更新的小组件实例
        for (appWidgetId in appWidgetIds) {
            // 更新每个小组件的视图
            updateWidgetViews(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * 首个小组件被添加到桌面
     *
     * 当用户首次添加此类型的小组件时调用
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FwLog.d("FwWidgetProvider: onEnabled，首个小组件已添加")
    }

    /**
     * 最后一个小组件被从桌面移除
     *
     * 当用户移除最后一个此类型的小组件时调用
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        FwLog.d("FwWidgetProvider: onDisabled，所有小组件已移除")
    }

    /**
     * 小组件被删除回调
     *
     * @param context 上下文
     * @param appWidgetIds 被删除的小组件 ID 数组
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        FwLog.d("FwWidgetProvider: onDeleted，删除 ${appWidgetIds.size} 个小组件")
    }

    /**
     * 更新小组件视图
     *
     * 使用 RemoteViews 更新小组件上的状态文本，
     * 根据 Fw 初始化状态显示不同的提示信息。
     *
     * @param context 上下文
     * @param appWidgetManager 小组件管理器
     * @param appWidgetId 要更新的小组件 ID
     */
    private fun updateWidgetViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            // 创建 RemoteViews 实例
            val remoteViews = RemoteViews(context.packageName, R.layout.fw_widget_layout)

            // 根据 Fw 初始化状态设置不同的显示文本
            val statusText = if (Fw.isInitialized()) {
                "Fw 保活运行中" // Fw 已初始化，保活服务运行中
            } else {
                "Fw 保活未启动" // Fw 未初始化，保活服务未启动
            }

            // 更新状态文本
            remoteViews.setTextViewText(R.id.widget_status_text, statusText)

            // 提交小组件更新
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            FwLog.d("FwWidgetProvider: 小组件 $appWidgetId 已更新 -> $statusText")
        } catch (e: Exception) {
            FwLog.e("FwWidgetProvider: 更新小组件视图失败: ${e.message}", e)
        }
    }
}
