/**
 * ============================================================================
 * FwStartInstrumentedTest.kt - Native startActivity 设备侧真实触发测试
 * ============================================================================
 *
 * 功能简介：
 *   在 Android 设备或模拟器上真实调用 C++ start 文件夹的统一入口，
 *   覆盖 application context、Activity context、只登记策略和异常入参。
 *
 * 函数简介：
 *   - startWithZeroMask_returnsInvalidArgument：验证空策略受控失败。
 *   - applicationContextStrategies_triggerNativeStart：验证应用上下文可执行策略。
 *   - activityContextStrategies_triggerNativeStart：验证 Activity 上下文可执行策略。
 *   - systemConstrainedStrategies_returnControlledResult：验证系统限制策略受控返回。
 *   - allStartStrategies_areCoveredByInstrumentedTests：验证所有策略均被测试覆盖。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 */
package com.google.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.service.framework.start.FwStart
import com.service.framework.start.FwStartResult
import com.service.framework.start.FwStartStrategy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native startActivity 设备侧真实触发测试。
 */
@RunWith(AndroidJUnit4::class)
class FwStartInstrumentedTest {

    // application context 下稳定成功的公开策略。
    private val applicationStableStrategies = listOf(
        FwStartStrategy.CONTEXT_NEW_TASK,
        FwStartStrategy.CONTEXT_NEW_TASK_EXCLUDE_RECENTS,
        FwStartStrategy.DOUBLE_START_ACTIVITIES
    )

    // Activity context 下稳定成功的公开策略。
    private val activityStableStrategies = listOf(
        FwStartStrategy.CONTEXT_DIRECT,
        FwStartStrategy.START_FOR_RESULT_HOOK
    )

    // 受 Android 版本、权限、前后台状态或安全策略限制的策略。
    private val systemConstrainedStrategies = listOf(
        FwStartStrategy.PENDING_INTENT_SEND,
        FwStartStrategy.BINDER_START_ACTIVITIES,
        FwStartStrategy.VIRTUAL_DISPLAY,
        FwStartStrategy.START_NEXT_MATCHING,
        FwStartStrategy.MOVE_TASK_TO_FRONT,
        FwStartStrategy.NOTIFICATION_BAL,
        FwStartStrategy.MEDIA_BUTTON_BAL,
        FwStartStrategy.CREDENTIAL_MANAGER,
        FwStartStrategy.PRINT_MANAGER,
        FwStartStrategy.SHELL_START_IN_VSYNC
    )

    // 当前测试启动的 Activity，结束时统一关闭。
    private var launchedActivity: Activity? = null

    /**
     * 测试结束后关闭由用例启动的 Activity。
     */
    @After
    fun finishLaunchedActivity() {
        // 避免多次 startActivity 后影响后续用例焦点。
        launchedActivity?.finish()
        // 清空 Activity 引用。
        launchedActivity = null
    }

    /**
     * 获取被测应用上下文。
     */
    private fun targetContext(): Context {
        // targetContext 指向 app 模块真实安装包。
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * 构造指向 debug 测试 Activity 的真实 Intent。
     */
    private fun mainIntent(context: Context): Intent {
        // 使用字符串类名，避免 androidTest 编译期依赖 debug 源集类。
        return Intent().setClassName(context.packageName, "com.google.services.TestStartActivity")
    }

    /**
     * 启动一个真实 debug 测试 Activity，并返回 Activity context。
     */
    private fun launchMainActivity(): Activity {
        // 使用 instrumentation 在目标进程真实启动 Activity。
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Activity 启动需要 NEW_TASK 标志。
        val intent = mainIntent(targetContext()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // 同步等待 Activity 创建完成。
        val activity = instrumentation.startActivitySync(intent)
        // 保存 Activity，便于测试结束关闭。
        launchedActivity = activity
        // 等待主线程空闲，确保窗口和 taskId 就绪。
        instrumentation.waitForIdleSync()
        return activity
    }

    /**
     * 验证 modeMask=0 时不会进入 Native 执行，并返回参数错误。
     */
    @Test
    fun startWithZeroMask_returnsInvalidArgument() {
        // 获取真实应用上下文。
        val context = targetContext()
        // 执行空策略启动。
        val result = FwStart.startWithMask(context, mainIntent(context), 0)
        // 空策略必须返回参数错误。
        assertEquals("空策略应返回参数错误", FwStartResult.INVALID_ARGUMENT, result.nativeCode)
    }

    /**
     * 验证 application context 下的公开可执行 startActivity 策略能真实触发。
     */
    @Test
    fun applicationContextStrategies_triggerNativeStart() {
        // 获取真实应用上下文。
        val context = targetContext()

        // 逐个策略单独触发，确保真实打到每个 Native 分支。
        for (strategy in applicationStableStrategies) {
            // 每次使用新 Intent，避免前一个策略修改 flags 后影响后一个策略。
            val result = FwStart.startWithStrategies(context, mainIntent(context), listOf(strategy))
            // 公开 application context 策略应命中对应策略。
            assertEquals("${strategy.displayName} 应成功命中", strategy, result.strategy)
            // 成功结果必须携带正数 Native 策略位。
            assertTrue("${strategy.displayName} 返回码应为正数", result.nativeCode > 0)
        }
    }

    /**
     * 验证 Activity context 下的公开可执行 startActivity 策略能真实触发或受控失败。
     */
    @Test
    fun activityContextStrategies_triggerNativeStart() {
        // 启动真实 Activity context。
        val activity = launchMainActivity()

        // 逐个策略单独触发，确保真实打到每个 Native 分支。
        for (strategy in activityStableStrategies) {
            // 每次使用 Activity 构造新 Intent。
            val result = FwStart.startWithStrategies(activity, mainIntent(activity), listOf(strategy))
            // Activity 直启和 startActivityForResult 属于公开稳定 API，应成功命中。
            assertEquals("${strategy.displayName} 应成功命中", strategy, result.strategy)
            // 成功结果必须携带正数 Native 策略位。
            assertTrue("${strategy.displayName} 返回码应为正数", result.nativeCode > 0)
        }
    }

    /**
     * 验证受系统限制的策略会真实进入 Native 分支，并以成功或受控失败结束。
     */
    @Test
    fun systemConstrainedStrategies_returnControlledResult() {
        // 启动真实 Activity context，供需要 Activity 的系统策略使用。
        val activity = launchMainActivity()

        // 逐个策略单独触发，确保真实打到每个 Native 分支。
        for (strategy in systemConstrainedStrategies) {
            // Activity 相关策略使用 Activity context，其余策略使用 application context。
            val context = when (strategy) {
                FwStartStrategy.START_NEXT_MATCHING,
                FwStartStrategy.MOVE_TASK_TO_FRONT -> activity
                else -> targetContext()
            }
            // 每次使用新 Intent，避免策略修改 flags 后影响下一个策略。
            val result = FwStart.startWithStrategies(context, mainIntent(context), listOf(strategy))
            // 这些策略必须真实进入 Native，不能被 Kotlin 层空策略拦截。
            assertNotEquals("${strategy.displayName} 不应返回参数错误", FwStartResult.INVALID_ARGUMENT, result.nativeCode)
            // 系统限制策略允许成功或受控失败，但必须有明确返回码。
            assertTrue("${strategy.displayName} 应返回明确结果", result.success || result.nativeCode < 0)
        }
    }

    /**
     * 验证当前枚举中的每一个 startActivity 策略都被设备侧测试覆盖。
     */
    @Test
    fun allStartStrategies_areCoveredByInstrumentedTests() {
        // 汇总所有测试矩阵中声明覆盖的策略。
        val coveredStrategies = (
            applicationStableStrategies +
                activityStableStrategies +
                systemConstrainedStrategies
            ).toSet()
        // 当前枚举中的所有策略必须都在测试矩阵中。
        assertEquals("所有 startActivity 策略都必须被设备侧测试覆盖", FwStartStrategy.entries.toSet(), coveredStrategies)
    }
}
