/**
 * ============================================================================
 * FwTileService.kt - 快捷设置磁贴保活策略（API 24+）
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Android Quick Settings TileService 的保活策略。用户将磁贴添加到
 *   快捷设置面板后，系统会维持服务绑定，从而提升进程优先级。同时提供便捷的
 *   一键触发保活检查功能。
 *
 * 主要功能：
 *   - onStartListening(): 更新磁贴状态（激活/未激活）
 *   - onClick(): 点击磁贴触发保活检查或提示未初始化
 *   - onTileAdded(): 磁贴被添加时记录日志
 *   - onTileRemoved(): 磁贴被移除时记录日志
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.service.framework.Fw
import com.service.framework.R
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 快捷设置磁贴保活策略（API 24+）
 *
 * 核心机制：
 * 1. 用户添加磁贴后，系统会绑定 TileService
 * 2. 绑定期间进程优先级提升
 * 3. 用户可通过磁贴快速查看和控制保活状态
 *
 * 安全研究要点：
 * - 需要用户手动将磁贴添加到快捷设置面板
 * - 系统会在下拉面板时绑定服务
 * - 磁贴可见期间进程受到保护
 * - 是一种用户可感知的、合法的保活方式
 *
 * 注意：
 * - 需要 API 24+（Android 7.0）
 * - 需要在 AndroidManifest 中声明
 * - 需要 BIND_QUICK_SETTINGS_TILE 权限
 */
@RequiresApi(Build.VERSION_CODES.N)
class FwTileService : TileService() {

    override fun onCreate() {
        super.onCreate()
        FwLog.d("FwTileService: onCreate")
    }

    /**
     * 磁贴开始监听（面板可见时）
     *
     * 更新磁贴状态：Fw 已初始化显示 ACTIVE，否则显示 INACTIVE
     */
    override fun onStartListening() {
        super.onStartListening()
        FwLog.d("FwTileService: onStartListening，更新磁贴状态")
        // 更新磁贴显示状态
        updateTileState()
    }

    /**
     * 磁贴停止监听（面板隐藏时）
     */
    override fun onStopListening() {
        super.onStopListening()
        FwLog.d("FwTileService: onStopListening")
    }

    /**
     * 用户点击磁贴
     *
     * 如果 Fw 已初始化，触发保活检查并拉起服务；否则提示未初始化
     */
    override fun onClick() {
        super.onClick()
        FwLog.d("FwTileService: onClick，用户点击磁贴")

        if (Fw.isInitialized()) {
            // Fw 已初始化，执行保活检查
            FwLog.d("FwTileService: Fw 已初始化，触发保活检查")
            Fw.check()
            // 拉起前台保活服务
            ServiceStarter.startForegroundService(this, "快捷磁贴点击")
            // 更新磁贴状态
            updateTileState()
        } else {
            // Fw 未初始化，提示用户
            FwLog.w("FwTileService: Fw 未初始化，无法执行保活检查")
            Toast.makeText(this, "保活服务未初始化", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 磁贴被添加到快捷设置面板
     */
    override fun onTileAdded() {
        super.onTileAdded()
        FwLog.d("FwTileService: onTileAdded，磁贴已添加到快捷设置面板")
        // 添加后立即更新状态
        updateTileState()
    }

    /**
     * 磁贴被从快捷设置面板移除
     */
    override fun onTileRemoved() {
        super.onTileRemoved()
        FwLog.d("FwTileService: onTileRemoved，磁贴已从快捷设置面板移除")
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("FwTileService: onDestroy")
    }

    /**
     * 更新磁贴显示状态
     *
     * 根据 Fw 初始化状态设置磁贴的激活/未激活状态、标签和图标
     */
    private fun updateTileState() {
        try {
            val tile = qsTile ?: return // 获取当前磁贴实例
            if (Fw.isInitialized()) {
                // Fw 已初始化 - 显示激活状态
                tile.state = Tile.STATE_ACTIVE
                tile.label = "保活服务"
                tile.contentDescription = "Fw 保活服务运行中"
                // 使用盾牌图标表示保护中
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_shield)
            } else {
                // Fw 未初始化 - 显示未激活状态
                tile.state = Tile.STATE_INACTIVE
                tile.label = "保活服务"
                tile.contentDescription = "Fw 保活服务未启动"
                // 使用播放图标表示可启动
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_shield)
            }
            // 提交磁贴更新
            tile.updateTile()
            FwLog.d("FwTileService: 磁贴状态已更新 -> ${if (Fw.isInitialized()) "ACTIVE" else "INACTIVE"}")
        } catch (e: Exception) {
            FwLog.e("FwTileService: 更新磁贴状态失败: ${e.message}", e)
        }
    }
}
