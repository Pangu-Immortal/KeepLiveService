/**
 * ============================================================================
 * FwStartStrategy.kt - 统一 startActivity 策略枚举
 * ============================================================================
 *
 * 功能简介：
 *   定义 C++ start 文件夹支持的全部 startActivity 策略位，覆盖微信收藏
 *   830-835、放大镜 qumeng 和虚拟屏 so 逆向方法。
 *
 * 主要函数：
 *   - toMask: 把策略列表转换为 Native modeMask
 *   - fromMask: 把 Native 返回 bit 转回策略枚举
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.start

/**
 * 统一 startActivity 策略枚举。
 *
 * @property mask Native 策略位
 * @property displayName 日志显示名称
 * @property source 来源说明
 * @property minSdk 最低适配版本
 * @property maxSdk 最高适配版本，null 表示不限
 * @property executable 是否默认执行真实调用，false 表示只登记和安全跳过
 */
enum class FwStartStrategy(
    val mask: Int,
    val displayName: String,
    val source: String,
    val minSdk: Int,
    val maxSdk: Int?,
    val executable: Boolean
) {
    CONTEXT_DIRECT(
        mask = 1 shl 0,
        displayName = "Activity Context 直启",
        source = "放大镜 qumeng: Activity.startActivity",
        minSdk = 1,
        maxSdk = null,
        executable = true
    ),
    CONTEXT_NEW_TASK(
        mask = 1 shl 1,
        displayName = "Context + NEW_TASK",
        source = "放大镜 qumeng: 非 Activity Context 添加 FLAG_ACTIVITY_NEW_TASK",
        minSdk = 1,
        maxSdk = null,
        executable = true
    ),
    PENDING_INTENT_SEND(
        mask = 1 shl 2,
        displayName = "PendingIntent send",
        source = "放大镜 qumeng: PendingIntent.getActivity(...).send()",
        minSdk = 1,
        maxSdk = null,
        executable = true
    ),
    DOUBLE_START_ACTIVITIES(
        mask = 1 shl 3,
        displayName = "双 Intent startActivities",
        source = "放大镜 qumeng: context.startActivities(new Intent[]{intent,intent})",
        minSdk = 16,
        maxSdk = null,
        executable = true
    ),
    BINDER_START_ACTIVITIES(
        mask = 1 shl 4,
        displayName = "Binder startActivities",
        source = "放大镜 qumeng: IActivityManager/IActivityTaskManager.startActivities",
        minSdk = 21,
        maxSdk = 30,
        executable = true
    ),
    START_FOR_RESULT_HOOK(
        mask = 1 shl 5,
        displayName = "startActivityForResult",
        source = "放大镜 qumeng: Android 10+ startActivityForResult 主线程路径",
        minSdk = 1,
        maxSdk = null,
        executable = true
    ),
    VIRTUAL_DISPLAY(
        mask = 1 shl 6,
        displayName = "VirtualDisplay",
        source = "so 逆向: VirtualDisplay + Presentation + launchDisplayId",
        minSdk = 26,
        maxSdk = null,
        executable = true
    ),
    NOTIFICATION_BAL(
        mask = 1 shl 7,
        displayName = "Notification BAL",
        source = "微信收藏 831: publicVersion.mAllowlistToken / PendingIntent BAL",
        minSdk = 29,
        maxSdk = 34,
        executable = false
    ),
    MEDIA_BUTTON_BAL(
        mask = 1 shl 8,
        displayName = "MediaButton BAL",
        source = "微信收藏 835: MediaButtonReceiverHolder BAL/FGS 能力传播",
        minSdk = 31,
        maxSdk = 34,
        executable = false
    ),
    START_NEXT_MATCHING(
        mask = 1 shl 9,
        displayName = "startNextMatchingActivity",
        source = "微信收藏 832: Activity.startNextMatchingActivity",
        minSdk = 1,
        maxSdk = null,
        executable = true
    ),
    CREDENTIAL_MANAGER(
        mask = 1 shl 10,
        displayName = "CredentialManager UI",
        source = "微信收藏 833: CredentialManagerUi.createPendingIntent",
        minSdk = 34,
        maxSdk = 34,
        executable = false
    ),
    PRINT_MANAGER(
        mask = 1 shl 11,
        displayName = "PrintManager UI",
        source = "微信收藏 834: PrintManager.print dialog PendingIntent",
        minSdk = 23,
        maxSdk = 34,
        executable = false
    ),
    SHELL_START_IN_VSYNC(
        mask = 1 shl 12,
        displayName = "shell start-in-vsync",
        source = "微信收藏 830: adb shell am start-in-vsync",
        minSdk = 35,
        maxSdk = null,
        executable = false
    );

    companion object {
        /**
         * Native 层固定执行顺序。
         */
        val nativeOrder: List<FwStartStrategy> = listOf(
            VIRTUAL_DISPLAY,
            NOTIFICATION_BAL,
            MEDIA_BUTTON_BAL,
            BINDER_START_ACTIVITIES,
            PENDING_INTENT_SEND,
            DOUBLE_START_ACTIVITIES,
            START_NEXT_MATCHING,
            START_FOR_RESULT_HOOK,
            CREDENTIAL_MANAGER,
            PRINT_MANAGER,
            SHELL_START_IN_VSYNC,
            CONTEXT_DIRECT,
            CONTEXT_NEW_TASK
        )

        /**
         * 全量策略，包含可执行路径和安全登记路径。
         */
        val allStrategies: List<FwStartStrategy> = nativeOrder

        /**
         * 默认策略位，确保微信收藏、放大镜和虚拟屏方法全部进入编排。
         */
        val allMask: Int = toMask(allStrategies)

        /**
         * 把策略列表转换成 Native modeMask。
         */
        fun toMask(strategies: Iterable<FwStartStrategy>): Int {
            var result = 0
            for (strategy in strategies) {
                result = result or strategy.mask
            }
            return result
        }

        /**
         * 根据 Native 返回 bit 查找命中策略。
         */
        fun fromMask(mask: Int): FwStartStrategy? {
            return entries.firstOrNull { strategy -> strategy.mask == mask }
        }
    }
}
