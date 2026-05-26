/**
 * ============================================================================
 * FwStart.kt - 统一 startActivity 对外入口
 * ============================================================================
 *
 * 功能简介：
 *   对外暴露 start 函数，内部调用 C++ start 文件夹的统一策略编排。
 *
 * 主要函数：
 *   - start: 全量策略启动 Activity
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
 * 默认 modeMask 包含所有策略；Native 层会按版本、权限和安全边界逐项执行。
 */
object FwStart {

    /**
     * 全量策略启动 Activity。
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
