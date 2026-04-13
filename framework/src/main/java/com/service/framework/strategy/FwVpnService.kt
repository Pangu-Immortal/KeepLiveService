/**
 * ============================================================================
 * FwVpnService.kt - VPN 隧道保活策略
 * ============================================================================
 *
 * 功能简介：
 *   通过建立本地回环 VPN 隧道获取系统绑定保护，提升进程优先级。
 *   不路由任何真实流量，仅持有 VPN 文件描述符以获得系统保护。
 *   此策略利用 VpnService 的系统级绑定特性实现高优先级保活。
 *
 * 核心机制：
 *   - 建立最小化 VPN 接口（地址 10.0.0.2/32）
 *   - 不路由真实网络流量，仅持有文件描述符
 *   - 利用系统对 VPN 服务的绑定保护提升进程优先级
 *   - START_STICKY 保证被杀后自动重启
 *   - 销毁时通过 ServiceStarter 自救拉起
 *
 * 主要功能：
 *   - start(): 启动 VPN 保活服务
 *   - stop(): 停止 VPN 保活服务
 *   - isRunning(): 检查服务是否运行中
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.3.0
 */
package com.service.framework.strategy

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * VPN 隧道保活服务
 *
 * 核心机制：
 * 1. 建立本地回环 VPN 隧道，获取系统绑定保护
 * 2. 不路由真实流量，仅持有文件描述符占位
 * 3. VpnService 拥有系统级绑定，进程优先级极高
 * 4. 被销毁后通过 START_STICKY 和 ServiceStarter 双重自救
 *
 * 安全研究要点：
 * - VPN 服务需要用户授权（系统弹窗确认）
 * - 系统状态栏会显示 VPN 图标，用户可感知
 * - 适合本身就需要 VPN 功能的应用（如代理、防火墙）
 * - foregroundServiceType 需声明为 specialUse
 * - 最低支持 API 24（Android 7.0）
 */
class FwVpnService : VpnService() {

    companion object {
        private const val TAG = "FwVpnService" // 日志子标签
        @Volatile
        private var isRunning = false // 服务运行状态标记

        /**
         * 启动 VPN 保活服务
         * @param context 上下文
         */
        fun start(context: Context) {
            // API 24 以下不支持
            if (Build.VERSION.SDK_INT < 24) {
                FwLog.w("$TAG: 当前系统版本低于 API 24，跳过 VPN 保活策略")
                return
            }
            FwLog.d("$TAG: 请求启动 VPN 保活服务")
            try {
                val intent = Intent(context, FwVpnService::class.java) // 构建服务启动意图
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent) // Android 8.0+ 使用前台服务启动
                } else {
                    context.startService(intent) // 低版本直接启动
                }
            } catch (e: Exception) {
                FwLog.e("$TAG: 启动 VPN 服务失败: ${e.message}", e)
            }
        }

        /**
         * 停止 VPN 保活服务
         * @param context 上下文
         */
        fun stop(context: Context) {
            FwLog.d("$TAG: 请求停止 VPN 保活服务")
            try {
                val intent = Intent(context, FwVpnService::class.java) // 构建服务停止意图
                context.stopService(intent) // 停止服务
            } catch (e: Exception) {
                FwLog.e("$TAG: 停止 VPN 服务失败: ${e.message}", e)
            }
        }

        /**
         * 检查 VPN 服务是否正在运行
         * @return 是否运行中
         */
        fun isRunning(): Boolean = isRunning
    }

    private var vpnInterface: ParcelFileDescriptor? = null // VPN 隧道文件描述符

    override fun onCreate() {
        super.onCreate()
        FwLog.d("$TAG: onCreate - VPN 保活服务初始化")
        isRunning = true // 标记服务已启动
        establishVpnTunnel() // 建立 VPN 隧道
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("$TAG: onStartCommand - 收到启动命令")
        // 如果隧道未建立则重新建立
        if (vpnInterface == null) {
            FwLog.d("$TAG: VPN 隧道未建立，重新建立")
            establishVpnTunnel() // 重新建立隧道
        }
        return START_STICKY // 被杀后系统自动重启
    }

    override fun onRevoke() {
        FwLog.w("$TAG: onRevoke - 用户撤销了 VPN 权限")
        closeVpnTunnel() // 关闭隧道
        stopSelf() // 停止自身
    }

    override fun onDestroy() {
        FwLog.w("$TAG: onDestroy - VPN 保活服务被销毁，尝试自救")
        closeVpnTunnel() // 关闭隧道释放资源
        isRunning = false // 标记服务已停止
        // 通过 ServiceStarter 拉起前台服务进行自救
        ServiceStarter.startForegroundService(this, "VPN服务被杀后自救")
        super.onDestroy()
    }

    /**
     * 建立最小化 VPN 隧道
     * 仅创建接口并持有文件描述符，不路由真实流量
     */
    private fun establishVpnTunnel() {
        try {
            FwLog.d("$TAG: 开始建立 VPN 隧道...")
            vpnInterface = Builder()
                .setSession("FwKeepAlive") // 设置会话名称（系统设置中显示）
                .addAddress("10.0.0.2", 32) // 设置本地回环地址
                .addRoute("0.0.0.0", 0) // 添加默认路由（实际不转发流量）
                .setBlocking(false) // 非阻塞模式，避免线程阻塞
                .setMtu(1500) // 设置最大传输单元
                .also { builder ->
                    // Android 10+ 允许设置是否计入流量统计
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        builder.setMetered(false) // 标记为非计费网络
                    }
                }
                .establish() // 建立隧道，返回文件描述符

            if (vpnInterface != null) {
                FwLog.d("$TAG: VPN 隧道建立成功，fd=${vpnInterface?.fd}")
                // 隧道已建立，持有文件描述符即可获得系统绑定保护
                // 不需要读写任何数据，仅占位保持 VPN 连接状态
            } else {
                FwLog.e("$TAG: VPN 隧道建立失败，establish() 返回 null（可能缺少 VPN 权限）")
            }
        } catch (e: Exception) {
            FwLog.e("$TAG: 建立 VPN 隧道异常: ${e.message}", e)
        }
    }

    /**
     * 关闭 VPN 隧道并释放文件描述符
     */
    private fun closeVpnTunnel() {
        try {
            vpnInterface?.close() // 关闭文件描述符
            FwLog.d("$TAG: VPN 隧道已关闭")
        } catch (e: Exception) {
            FwLog.e("$TAG: 关闭 VPN 隧道异常: ${e.message}", e)
        }
        vpnInterface = null // 清空引用
    }
}
