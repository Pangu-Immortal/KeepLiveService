/**
 * ============================================================================
 * HiddenApiBypass.kt - 隐藏 API 绕过工具
 * ============================================================================
 *
 * 功能简介：
 *   绕过 Android 9+ 的隐藏 API 访问限制，允许反射调用系统隐藏方法。
 *
 * 原理：
 *   通过反射调用 VMRuntime.setHiddenApiExemptions() 方法，
 *   将所有以 "L" 开头的类（即所有类）加入豁免列表。
 *
 * 函数列表：
 *   - exemptAll()：豁免所有隐藏 API
 *   - setExemptions()：设置指定的豁免列表
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.strategy.forcestop

import com.service.framework.util.FwLog
import java.lang.reflect.Method

object HiddenApiBypass {

    private const val TAG = "HiddenApiBypass" // 日志标签

    private var setHiddenApiExemptions: Method? = null // 缓存反射方法
    private var vmRuntime: Any? = null // 缓存 VMRuntime 实例

    init {
        try {
            // 获取 Class.forName 方法
            val forNameMethod = Class::class.java.getDeclaredMethod("forName", String::class.java)
            // 获取 Class.getDeclaredMethod 方法
            // 注意：Array<Class<*>>::class.java 在 Kotlin 中语法无效，使用 Java 等效类型
            val getDeclaredMethodMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                emptyArray<Class<*>>()::class.java  // Class[] 的 Kotlin 表示
            )

            // 获取 VMRuntime 类
            val vmRuntimeClass = forNameMethod.invoke(null, "dalvik.system.VMRuntime") as Class<*>

            // 获取 setHiddenApiExemptions 方法
            setHiddenApiExemptions = getDeclaredMethodMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf(Array<String>::class.java)
            ) as Method

            // 获取 getRuntime 方法
            val getRuntimeMethod = getDeclaredMethodMethod.invoke(
                vmRuntimeClass,
                "getRuntime",
                null
            ) as Method

            // 获取 VMRuntime 实例
            vmRuntime = getRuntimeMethod.invoke(null)

            FwLog.d("$TAG: 初始化成功")
        } catch (e: Throwable) {
            FwLog.e("$TAG: 初始化失败", e)
        }
    }

    /**
     * 设置隐藏 API 豁免列表
     *
     * @param methods 要豁免的方法签名前缀列表
     * @return 是否设置成功
     */
    fun setExemptions(vararg methods: String): Boolean {
        if (setHiddenApiExemptions == null || vmRuntime == null) {
            FwLog.e("$TAG: 未初始化，无法设置豁免")
            return false
        }

        return try {
            setHiddenApiExemptions!!.invoke(vmRuntime, arrayOf(methods)) // 调用设置豁免方法
            FwLog.d("$TAG: 设置豁免成功: ${methods.toList()}")
            true
        } catch (e: Throwable) {
            FwLog.e("$TAG: 设置豁免失败", e)
            false
        }
    }

    /**
     * 豁免所有隐藏 API
     * 通过传入 "L" 前缀，豁免所有以 L 开头的类签名（即所有类）
     *
     * @return 是否设置成功
     */
    fun exemptAll(): Boolean {
        FwLog.d("$TAG: 开始豁免所有隐藏 API...")
        return setExemptions("L") // "L" 是所有类签名的前缀
    }
}
