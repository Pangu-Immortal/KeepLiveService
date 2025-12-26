/**
 * ============================================================================
 * ProcessUtils.kt - 进程工具类
 * ============================================================================
 *
 * 功能简介：
 *   提供进程相关的工具方法，包括获取当前进程名、检查进程存活等。
 *
 * 函数列表：
 *   - getProcessName()：获取当前进程名
 *   - isMainProcess()：检查是否为主进程
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import android.app.Application
import com.service.framework.util.FwLog
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object ProcessUtils {

    private const val TAG = "ProcessUtils" // 日志标签

    /**
     * 获取当前进程名
     * 通过读取 /proc/self/cmdline 文件获取
     *
     * @return 进程名，失败返回 null
     */
    fun getProcessName(): String? {
        var reader: BufferedReader? = null
        return try {
            val file = File("/proc/self/cmdline") // 进程命令行文件
            reader = BufferedReader(FileReader(file))
            reader.readLine()?.trim()?.replace("\u0000", "") // 读取并清理空字符
        } catch (e: Exception) {
            FwLog.e("$TAG: 获取进程名失败", e)
            null
        } finally {
            try {
                reader?.close() // 关闭读取器
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 检查当前是否为主进程
     *
     * @param app 应用实例
     * @return 是否为主进程
     */
    fun isMainProcess(app: Application): Boolean {
        val processName = getProcessName() // 获取当前进程名
        val packageName = app.packageName // 获取包名
        return processName == packageName // 进程名等于包名即为主进程
    }
}
