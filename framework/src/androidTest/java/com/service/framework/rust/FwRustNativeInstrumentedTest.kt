/**
 * ============================================================================
 * FwRustNativeInstrumentedTest.kt - Rust Native 设备侧真实触发测试
 * ============================================================================
 *
 * 功能简介：
 *   在 Android 设备或模拟器上真实加载 Native so，验证 Rust 进程探测、
 *   Rust MediaRoute 状态机，以及公共 MediaRoute Native 回退入口。
 *
 * 函数简介：
 *   - rustProcessSnapshot_whenRustSoPackaged_returnsRealProcData：验证 Rust 读取 /proc。
 *   - rustMediaRouteNative_stateMachine_transitions：验证 Rust MediaRoute 状态迁移。
 *   - mediaRoutePublicNative_backendTransitions_withoutCrash：验证公共入口可真实触发。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 */
package com.service.framework.rust

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.service.framework.Fw
import com.service.framework.mediaroute.FwMediaRouteNative
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rust Native 设备侧真实触发测试。
 */
@RunWith(AndroidJUnit4::class)
class FwRustNativeInstrumentedTest {

    /**
     * 每个用例前重置 Rust MediaRoute 服务位，避免状态串扰。
     */
    @Before
    fun resetMediaRouteState() {
        // 初始化最小化 Fw 配置，保证 FwLog 在测试进程中可读取 logTag。
        ensureFwConfigReady()
        // Rust so 未打包时跳过直接 Rust 状态重置，公共入口测试会覆盖 C++ 回退。
        if (!FwRustNative.init(logUnavailable = false)) {
            return
        }
        // 初始化 Rust MediaRoute 动态注册方法。
        FwRustMediaRouteNative.init()
        // 清空 MediaRouteProviderService 运行位。
        FwRustMediaRouteNative.onServiceStopped()
        // 清空 MediaRoute2ProviderService 运行位。
        FwRustMediaRouteNative.onService2Stopped()
    }

    /**
     * 每个用例后释放 WakeLock，避免影响后续测试和模拟器状态。
     */
    @After
    fun releaseWakeLock() {
        // 公共入口可能短暂获取 WakeLock，测试结束必须释放。
        FwMediaRouteNative.releaseWakeLock()
    }

    /**
     * 初始化测试专用最小配置，只打开日志，不启动任何保活策略。
     */
    private fun ensureFwConfigReady() {
        // 已初始化时不重复触发框架入口。
        if (Fw.isInitialized()) {
            return
        }
        // 获取测试进程的 Application。
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        // 使用全关闭配置，避免 instrumentation 测试启动后台策略。
        Fw.init(app) {
            enableForegroundService = false
            enableMediaSession = false
            enableOnePixelActivity = false
            enableJobScheduler = false
            enableWorkManager = false
            enableAlarmManager = false
            enableAccountSync = false
            enableSystemBroadcast = false
            enableBluetoothBroadcast = false
            enableMediaButtonReceiver = false
            enableUsbBroadcast = false
            enableNfcBroadcast = false
            enableMediaMountBroadcast = false
            enableMediaContentObserver = false
            enableContactsContentObserver = false
            enableSmsContentObserver = false
            enableSettingsContentObserver = false
            enableFileObserver = false
            enableDualProcess = false
            enableNativeDaemon = false
            enableNativeSocket = false
            enableLockScreenActivity = false
            enableFloatWindow = false
            enableForceStopResistance = false
            enableMediaRouteProvider = false
            enableMediaRoute2Provider = false
            enableMediaIntentActivity = false
            enableMediaBrowserService = false
            enableSilentAudio = false
            enableVpnService = false
            enableCompanionDevice = false
            enableCallStyleNotification = false
            enableMediaSessionNotification = false
            enableDeviceAdmin = false
            enableTileService = false
            enableWidget = false
            enableDreamService = false
            enableDebugLog = true
            logTag = "FwTest"
        }
    }

    /**
     * 验证 Rust so 打包时可以真实读取 /proc 进程信息。
     */
    @Test
    fun rustProcessSnapshot_whenRustSoPackaged_returnsRealProcData() {
        // 默认 C++ 构建不包含 Rust so，此用例只在 -PfwBuildRust=true 矩阵执行。
        assumeTrue("Rust so 未打包，跳过 Rust 专属进程探测测试", FwRustNative.init(logUnavailable = false))

        // 真实触发 Rust JNI 读取当前进程快照。
        val snapshot = FwRustNative.processSnapshotOrNull()

        // Rust 进程快照必须返回有效对象。
        assertNotNull("Rust 进程快照不应为空", snapshot)
        // Rust Native 当前版本号固定为 1。
        assertEquals("Rust Native 版本号应匹配", 1, snapshot!!.versionCode)
        // 真实系统总内存必须大于 0。
        assertTrue("系统总内存必须大于 0", snapshot.memoryTotalKb > 0L)
        // 真实系统可用内存必须大于 0。
        assertTrue("系统可用内存必须大于 0", snapshot.memoryAvailableKb > 0L)
        // /proc/self/status 必须包含 Pid 字段。
        assertTrue("进程状态必须包含 Pid", snapshot.processStatus.contains("Pid:"))
        // /proc 进程数量必须大于 0。
        assertTrue("系统进程数量必须大于 0", snapshot.processCount > 0)
    }

    /**
     * 验证 Rust MediaRoute 状态机能真实完成 2 -> 1 -> 0 -> 1 -> 2 状态迁移。
     */
    @Test
    fun rustMediaRouteNative_stateMachine_transitions() {
        // 默认 C++ 构建不包含 Rust so，此用例只在 -PfwBuildRust=true 矩阵执行。
        assumeTrue("Rust so 未打包，跳过 Rust MediaRoute 状态机测试", FwRustNative.init(logUnavailable = false))

        // 初始化 Rust MediaRoute 入口。
        assertTrue("Rust MediaRoute 应初始化成功", FwRustMediaRouteNative.init())
        // 两个服务都停止时应为异常状态。
        assertEquals("初始停止状态应为异常", 2, FwRustMediaRouteNative.getServiceStatus())

        // 真实触发 MediaRouteProviderService 启动状态。
        FwRustMediaRouteNative.onServiceStarted("com.google.services", "FwMediaRouteProviderService")
        // 单服务运行时应为警告状态。
        assertEquals("单服务运行应为警告", 1, FwRustMediaRouteNative.getServiceStatus())
        // 单服务运行时心跳应返回成功。
        assertTrue("单服务运行时心跳应成功", FwRustMediaRouteNative.performHeartbeat())

        // 真实触发 MediaRoute2ProviderService 启动状态。
        FwRustMediaRouteNative.onService2Started("com.google.services", "FwMediaRoute2ProviderService")
        // 双服务运行时应为正常状态。
        assertEquals("双服务运行应为正常", 0, FwRustMediaRouteNative.getServiceStatus())
        // WakeLock 检查计数应能真实调用且不抛异常。
        FwRustMediaRouteNative.checkWakeLock()

        // 停止第一个服务后应降级为警告。
        FwRustMediaRouteNative.onServiceStopped()
        assertEquals("停止一个服务后应为警告", 1, FwRustMediaRouteNative.getServiceStatus())
        // 停止第二个服务后应降级为异常。
        FwRustMediaRouteNative.onService2Stopped()
        assertEquals("停止两个服务后应为异常", 2, FwRustMediaRouteNative.getServiceStatus())
    }

    /**
     * 验证公共 MediaRoute Native 入口在当前打包矩阵下可真实触发并完成状态迁移。
     */
    @Test
    fun mediaRoutePublicNative_backendTransitions_withoutCrash() {
        // 获取被测应用上下文。
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 初始化公共 Native 入口；Rust 存在时走 Rust，不存在时走 C++ 回退。
        assertTrue("公共 MediaRoute Native 入口应可初始化", FwMediaRouteNative.init(context))
        // 清空两个服务状态。
        FwMediaRouteNative.onServiceStopped()
        FwMediaRouteNative.onService2Stopped()
        // 两个服务都停止时应为异常状态。
        assertEquals("公共入口停止态应为异常", 2, FwMediaRouteNative.getServiceStatus())

        // 真实触发第一个服务启动状态。
        FwMediaRouteNative.onServiceStarted(context.packageName, "FwMediaRouteProviderService")
        // 单服务运行时应为警告状态。
        assertEquals("公共入口单服务运行应为警告", 1, FwMediaRouteNative.getServiceStatus())
        // 真实触发第二个服务启动状态。
        FwMediaRouteNative.onService2Started(context.packageName, "FwMediaRoute2ProviderService")
        // 双服务运行时应为正常状态。
        assertEquals("公共入口双服务运行应为正常", 0, FwMediaRouteNative.getServiceStatus())
        // 真实触发心跳。
        assertTrue("公共入口双服务运行时心跳应成功", FwMediaRouteNative.performHeartbeat())
        // 真实触发 WakeLock 检查，验证不会产生 JNI 或 Runtime 崩溃。
        FwMediaRouteNative.checkWakeLock()
    }
}
