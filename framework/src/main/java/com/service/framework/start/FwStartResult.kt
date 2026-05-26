/**
 * ============================================================================
 * FwStartResult.kt - 统一 startActivity 结果模型
 * ============================================================================
 *
 * 功能简介：
 *   把 Native start 函数返回的策略 bit 或错误码转换为 Kotlin 可读结果。
 *
 * 主要函数：
 *   - fromNativeCode: Native 返回码转换
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.start

/**
 * 统一 startActivity 执行结果。
 *
 * @property success 是否命中任一策略
 * @property nativeCode Native 原始返回码
 * @property strategy 成功命中的策略
 * @property message 可读说明
 */
data class FwStartResult(
    val success: Boolean,
    val nativeCode: Int,
    val strategy: FwStartStrategy?,
    val message: String
) {
    companion object {
        const val INVALID_ARGUMENT: Int = -1
        const val JNI_EXCEPTION: Int = -2
        const val UNSUPPORTED_SDK: Int = -3
        const val SKIPPED_BY_POLICY: Int = -4
        const val REQUIRES_PRIVILEGE: Int = -5
        const val NOT_ACTIVITY_CONTEXT: Int = -6
        const val SYSTEM_API_BLOCKED: Int = -7
        const val NO_STRATEGY_SUCCEEDED: Int = -8

        /**
         * 把 Native 返回码转换成 Kotlin 结果。
         */
        fun fromNativeCode(code: Int): FwStartResult {
            val strategy = if (code > 0) FwStartStrategy.fromMask(code) else null
            val message = when {
                strategy != null -> "命中策略：${strategy.displayName}"
                code == INVALID_ARGUMENT -> "参数无效：Context 或 Intent 为空"
                code == JNI_EXCEPTION -> "JNI 调用异常或 Native 符号不可用"
                code == UNSUPPORTED_SDK -> "当前 Android 版本不支持该策略"
                code == SKIPPED_BY_POLICY -> "策略已登记，但按安全策略跳过"
                code == REQUIRES_PRIVILEGE -> "缺少 shell/root/系统权限"
                code == NOT_ACTIVITY_CONTEXT -> "当前 Context 不是 Activity"
                code == SYSTEM_API_BLOCKED -> "系统 API 或 ROM 策略拦截"
                code == NO_STRATEGY_SUCCEEDED -> "所有启用策略均未成功"
                else -> "未知 Native 返回码：$code"
            }
            return FwStartResult(
                success = strategy != null,
                nativeCode = code,
                strategy = strategy,
                message = message
            )
        }
    }
}
