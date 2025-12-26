/**
 * ============================================================================
 * VendorIntegrationAnalyzer.kt - 厂商集成分析器
 * ============================================================================
 *
 * 功能简介：
 *   分析和检测应用与厂商系统的集成情况，包括推送 SDK 检测、系统权限分析、
 *   厂商白名单检测等。支持小米、华为、OPPO、vivo、魅族等主流厂商的推送
 *   SDK 识别，帮助了解应用的保活能力来源。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.service.framework.util.FwLog

/**
 * 厂商推送 SDK 集成检测与利用
 *
 * 安全研究要点：
 * 墨迹天气等应用"永生不死"的核心秘密之一可能就是：
 *
 * 1. 厂商推送通道
 *    - 小米推送、华为推送、OPPO 推送等是系统级服务
 *    - 即使应用被杀，推送到达时也会拉起应用
 *    - 这些推送服务本身就是常驻的
 *
 * 2. 与厂商合作
 *    - 签署商业合作协议
 *    - 被加入系统级白名单（不在用户可见的自启动管理中）
 *    - 可能在 /system/etc/ 下有 XML 白名单
 *    - 可能通过厂商私有 API 注册
 *
 * 3. 预装合作
 *    - 预装应用有特殊签名或 UID
 *    - 可能有 system 权限
 *    - 不受普通应用的限制
 *
 * 这个类用于检测这些可能的机制
 */
object VendorIntegrationAnalyzer {

    private const val TAG = "VendorAnalyzer"

    /**
     * 常见厂商推送 SDK 包名
     */
    private val PUSH_SDK_PACKAGES = mapOf(
        "小米推送" to listOf(
            "com.xiaomi.mipush.sdk",
            "com.xiaomi.push"
        ),
        "华为推送" to listOf(
            "com.huawei.hms.push",
            "com.huawei.android.pushagent"
        ),
        "OPPO 推送" to listOf(
            "com.heytap.msp",
            "com.coloros.mcs"
        ),
        "vivo 推送" to listOf(
            "com.vivo.push",
            "com.vivo.pushservice"
        ),
        "魅族推送" to listOf(
            "com.meizu.cloud.pushsdk"
        ),
        "个推" to listOf(
            "com.igexin.sdk"
        ),
        "极光推送" to listOf(
            "cn.jpush.android"
        ),
        "友盟推送" to listOf(
            "com.umeng.message"
        )
    )

    /**
     * 可能的系统白名单位置
     */
    private val WHITELIST_PATHS = listOf(
        "/system/etc/permissions/privapp-permissions-platform.xml",
        "/system/etc/permissions/platform.xml",
        "/system/etc/sysconfig/hiddenapi-package-whitelist.xml",
        "/vendor/etc/sysconfig/",
        "/product/etc/sysconfig/"
    )

    /**
     * 分析应用集成的推送 SDK
     */
    fun analyzePushSdks(context: Context, packageName: String): List<String> {
        val result = mutableListOf<String>()

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val metaData = appInfo.metaData

            // 检查 meta-data 中的推送配置
            metaData?.keySet()?.forEach { key ->
                PUSH_SDK_PACKAGES.forEach { (sdkName, packages) ->
                    packages.forEach { pkg ->
                        if (key.contains(pkg, ignoreCase = true)) {
                            result.add("$sdkName (via meta-data: $key)")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            FwLog.e("$TAG: 分析推送 SDK 失败 - ${e.message}", e)
        }

        return result
    }

    /**
     * 检查应用是否可能有系统级权限
     */
    fun analyzeSystemPrivileges(context: Context, packageName: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

            // 检查是否是系统应用
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            result["isSystemApp"] = isSystemApp
            result["isUpdatedSystemApp"] = isUpdatedSystemApp
            result["sourceDir"] = appInfo.sourceDir
            result["uid"] = appInfo.uid

            // 检查是否有特殊签名
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners?.map { it.hashCode().toString(16) }
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                    .signatures?.map { it.hashCode().toString(16) }
            }
            result["signatureHashes"] = signatures ?: emptyList<String>()

            // 检查敏感权限
            val sensitivePermissions = listOf(
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.WAKE_LOCK",
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                // 厂商私有权限
                "com.miui.permission.AUTO_START",
                "com.huawei.permission.external_app_settings.USE_COMPONENT",
                "oppo.permission.OPPO_COMPONENT_SAFE"
            )

            val grantedSensitivePermissions = pkgInfo.requestedPermissions
                ?.filter { it in sensitivePermissions }
                ?: emptyList()

            result["sensitivePermissions"] = grantedSensitivePermissions

        } catch (e: Exception) {
            FwLog.e("$TAG: 分析系统权限失败 - ${e.message}", e)
            result["error"] = e.message ?: "Unknown error"
        }

        return result
    }

    /**
     * 检测可能的厂商白名单
     *
     * 注意：需要 root 权限才能读取系统文件
     */
    fun detectWhitelistMechanism(context: Context): String {
        return buildString {
            appendLine("=== 厂商白名单检测 ===")
            appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()

            // 检查已知的白名单路径
            appendLine("白名单文件检查:")
            WHITELIST_PATHS.forEach { path ->
                val exists = java.io.File(path).exists()
                appendLine("  $path: ${if (exists) "存在" else "不存在/无权限"}")
            }

            appendLine()
            appendLine("可能的保活机制:")
            appendLine("1. 厂商推送 SDK（需要集成对应 SDK）")
            appendLine("2. 厂商白名单合作（需要商务合作）")
            appendLine("3. 预装合作（需要厂商预装）")
            appendLine("4. 系统级签名（需要厂商签名）")
        }
    }

    /**
     * 获取完整分析报告
     */
    fun getFullAnalysisReport(context: Context, targetPackage: String? = null): String {
        val pkg = targetPackage ?: context.packageName

        return buildString {
            appendLine("========================================")
            appendLine("厂商集成分析报告")
            appendLine("========================================")
            appendLine()
            appendLine("目标应用: $pkg")
            appendLine()

            // 推送 SDK 分析
            appendLine("--- 推送 SDK 检测 ---")
            val pushSdks = analyzePushSdks(context, pkg)
            if (pushSdks.isEmpty()) {
                appendLine("未检测到已知推送 SDK")
            } else {
                pushSdks.forEach { appendLine("  - $it") }
            }
            appendLine()

            // 系统权限分析
            appendLine("--- 系统权限分析 ---")
            val privileges = analyzeSystemPrivileges(context, pkg)
            privileges.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
            appendLine()

            // 白名单机制检测
            append(detectWhitelistMechanism(context))
        }
    }
}
