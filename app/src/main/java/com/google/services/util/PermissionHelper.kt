/**
 * ============================================================================
 * PermissionHelper.kt - 权限管理工具类
 * ============================================================================
 *
 * 功能简介：
 *   根据不同 Android 版本管理所需权限：
 *   - Android 7-11 (API 24-30): 只需 manifest 声明的权限
 *   - Android 12+ (API 31+): 需要 BLUETOOTH_CONNECT 运行时权限
 *   - Android 13+ (API 33+): 需要 POST_NOTIFICATIONS 运行时权限
 *
 * 主要函数：
 *   - getRequiredPermissions(): 获取当前版本所需权限列表
 *   - hasPermission(): 检查单个权限
 *   - hasAllRequiredPermissions(): 检查所有必需权限
 *   - getMissingPermissions(): 获取缺失的权限列表
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 
 * 根据不同 Android 版本管理所需权限：
 * - Android 7-11 (API 24-30): 只需 manifest 声明的权限
 * - Android 12+ (API 31+): 需要 BLUETOOTH_CONNECT 运行时权限
 * - Android 13+ (API 33+): 需要 POST_NOTIFICATIONS 运行时权限
 */
class PermissionHelper(private val context: Context) {

    /**
     * 获取当前 Android 版本所需的运行时权限列表
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // Android 12+ (API 31+) 需要蓝牙连接权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Android 13+ (API 33+) 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions
    }

    /**
     * 检查是否拥有指定权限
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否拥有所有必需权限
     */
    fun hasAllRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { hasPermission(it) }
    }

    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { !hasPermission(it) }
    }
}
