/**
 * ============================================================================
 * FwStart.kt - 统一 startActivity 对外入口
 * ============================================================================
 *
 * 功能简介：
 *   对外暴露 start 函数，内部调用 C++ start 文件夹的统一策略编排。
 *
 * 主要函数：
 *   - start: 默认可执行策略启动 Activity
 *   - startAuditAll: 全量策略审计启动 Activity
 *   - startWithStrategies: 指定策略集合启动 Activity
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.start

import android.content.Context
import android.content.Intent
import com.service.framework.native.FwNative
import com.service.framework.util.FwLog

/**
 * 统一 startActivity 入口。
 *
 * 默认 modeMask 只包含可执行策略；全量策略可通过 startAuditAll 显式进入审计编排。
 */
object FwStart {

    /**
     * 默认可执行策略启动 Activity。
     *
     * @param context 调用方上下文
     * @param intent 目标 Activity Intent
     * @return 统一结果
     */
    @JvmStatic
    fun start(context: Context, intent: Intent): FwStartResult {
        return startWithStrategies(
            context = context,
            intent = intent,
            strategies = FwStartStrategy.defaultExecutableStrategies
        )
    }

    /**
     * 全量策略审计启动 Activity。
     *
     * 该入口会把仅登记/安全跳过的研究路径也交给 Native 层记录版本判断和跳过原因。
     *
     * @param context 调用方上下文
     * @param intent 目标 Activity Intent
     * @return 统一结果
     */
    @JvmStatic
    fun startAuditAll(context: Context, intent: Intent): FwStartResult {
        return startWithStrategies(
            context = context,
            intent = intent,
            strategies = FwStartStrategy.allStrategies
        )
    }

    /**
     * 指定策略集合启动 Activity。
     *
     * @param context 调用方上下文
     * @param intent 目标 Activity Intent
     * @param strategies 策略集合
     * @return 统一结果
     */
    @JvmStatic
    fun startWithStrategies(
        context: Context,
        intent: Intent,
        strategies: Iterable<FwStartStrategy>
    ): FwStartResult {
        val modeMask = FwStartStrategy.toMask(strategies)
        return startWithMask(
            context = context,
            intent = intent,
            modeMask = modeMask
        )
    }

    /**
     * 指定位掩码启动 Activity。
     *
     * @param context 调用方上下文
     * @param intent 目标 Activity Intent
     * @param modeMask Native 策略位掩码
     * @return 统一结果
     */
    @JvmStatic
    fun startWithMask(context: Context, intent: Intent, modeMask: Int): FwStartResult {
        if (modeMask == 0) {
            FwLog.w("FwStart: modeMask=0，没有可执行策略")
            return FwStartResult.fromNativeCode(FwStartResult.INVALID_ARGUMENT)
        }
        FwLog.d("FwStart: 开始统一 startActivity, modeMask=$modeMask")
        val nativeCode = FwNative.start(
            context = context,
            intent = intent,
            modeMask = modeMask
        )
        val result = FwStartResult.fromNativeCode(nativeCode)
        FwLog.d("FwStart: 统一 startActivity 结束, success=${result.success}, message=${result.message}")
        return result
    }
}
