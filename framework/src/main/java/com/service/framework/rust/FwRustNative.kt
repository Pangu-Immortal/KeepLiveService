/**
 * ============================================================================
 * FwRustNative.kt - Rust Native 迁移桥接入口
 * ============================================================================
 *
 * 功能简介：
 *   封装 Rust Native 骨架库的加载和能力探测，当前只用于验证 Rust so
 *   构建、打包、JNI 动态注册链路，不替换现有 C++ JNI 业务逻辑。
 *
 * 函数简介：
 *   - init：加载 libfw_rust.so，并完成 JNI 动态注册。
 *   - isAvailable：检查 Rust Native 骨架是否可用。
 *   - versionCode：读取 Rust Native 骨架版本号。
 *   - processSnapshotOrNull：读取 Rust 侧进程状态快照。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 */
package com.service.framework.rust

import com.service.framework.util.FwLog

/**
 * Rust Native 迁移桥接入口。
 *
 * 当前类保持内部使用，不接入 `Fw.init()` 主链路，避免缺少 Rust so 时影响已有用户。
 */
internal object FwRustNative {

    private const val TAG = "FwRustNative"

    // 记录 Rust so 是否已经成功加载。
    private var isLoaded = false

    // 记录是否已经尝试过加载，避免重复输出失败日志。
    private var hasTriedLoad = false

    /**
     * 初始化 Rust Native 骨架库。
     *
     * @return Rust so 是否加载并完成 JNI 注册
     */
    fun init(logUnavailable: Boolean = true): Boolean {
        if (isLoaded) {
            FwLog.d("$TAG: Rust Native 已初始化，跳过")
            return true
        }
        if (hasTriedLoad) {
            FwLog.d("$TAG: Rust Native 已尝试加载且当前不可用，跳过重复加载")
            return false
        }
        hasTriedLoad = true
        return try {
            // 加载可选 Rust 骨架库；未启用 -PfwBuildRust=true 的本地构建不会包含该 so。
            System.loadLibrary("fw_rust")
            // 调用动态注册方法，确保 JNI_OnLoad 已完成注册。
            val available = nativeIsAvailable()
            isLoaded = available
            FwLog.d("$TAG: Rust Native 加载${if (available) "成功" else "失败"}")
            available
        } catch (e: UnsatisfiedLinkError) {
            if (logUnavailable) {
                FwLog.w("$TAG: Rust Native 库不可用 - ${e.message}")
            } else {
                FwLog.d("$TAG: Rust Native 库未打包，跳过 Rust 进程探测")
            }
            isLoaded = false
            false
        } catch (e: RuntimeException) {
            FwLog.e("$TAG: Rust Native 初始化异常 - ${e.message}", e)
            isLoaded = false
            false
        }
    }

    /**
     * 检查 Rust Native 骨架是否可用。
     */
    fun isAvailable(): Boolean = isLoaded || init(logUnavailable = false)

    /**
     * 获取 Rust Native 骨架版本号。
     */
    fun versionCode(): Int {
        if (!isAvailable()) {
            return 0
        }
        return try {
            nativeVersionCode()
        } catch (e: UnsatisfiedLinkError) {
            FwLog.e("$TAG: nativeVersionCode 符号不可用 - ${e.message}", e)
            0
        }
    }

    /**
     * 读取 Rust Native 进程状态快照。
     *
     * 当前快照只覆盖只读系统信息，不涉及 Binder、fork、Socket 等高风险路径。
     */
    fun processSnapshotOrNull(): FwRustProcessSnapshot? {
        if (!isAvailable()) {
            return null
        }
        return try {
            val memoryInfo = nativeGetMemoryInfo()
            if (memoryInfo.size < 3) {
                FwLog.w("$TAG: Rust Native 内存数组长度异常：${memoryInfo.size}")
                return null
            }
            FwRustProcessSnapshot(
                versionCode = nativeVersionCode(),
                oomAdj = nativeGetOomAdj(),
                memoryTotalKb = memoryInfo[0],
                memoryFreeKb = memoryInfo[1],
                memoryAvailableKb = memoryInfo[2],
                processStatus = nativeGetProcessStatus(),
                hasRoot = nativeCheckRoot(),
                processCount = nativeGetProcessCount()
            )
        } catch (e: UnsatisfiedLinkError) {
            FwLog.e("$TAG: Rust Native 进程探测符号不可用 - ${e.message}", e)
            null
        } catch (e: RuntimeException) {
            FwLog.e("$TAG: Rust Native 进程探测异常 - ${e.message}", e)
            null
        }
    }

    @JvmStatic
    private external fun nativeIsAvailable(): Boolean

    @JvmStatic
    private external fun nativeVersionCode(): Int

    @JvmStatic
    private external fun nativeGetOomAdj(): Int

    @JvmStatic
    private external fun nativeGetMemoryInfo(): LongArray

    @JvmStatic
    private external fun nativeGetProcessStatus(): String

    @JvmStatic
    private external fun nativeCheckRoot(): Boolean

    @JvmStatic
    private external fun nativeGetProcessCount(): Int
}

/**
 * Rust Native 进程状态快照。
 */
internal data class FwRustProcessSnapshot(
    val versionCode: Int,
    val oomAdj: Int,
    val memoryTotalKb: Long,
    val memoryFreeKb: Long,
    val memoryAvailableKb: Long,
    val processStatus: String,
    val hasRoot: Boolean,
    val processCount: Int
) {
    /**
     * 转换为健康巡检日志摘要。
     */
    fun toHealthMessage(): String {
        return "Rust Native v$versionCode 可用，OOM=$oomAdj，进程数=$processCount，Root=$hasRoot，内存可用=${memoryAvailableKb / 1024}MB"
    }
}
