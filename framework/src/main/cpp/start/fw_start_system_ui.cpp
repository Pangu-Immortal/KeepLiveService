/**
 * ============================================================================
 * fw_start_system_ui.cpp - 系统 UI PendingIntent 策略登记
 * ============================================================================
 *
 * 功能简介：
 *   吸收微信收藏 833/834 中 CredentialManager 和 PrintManager 系统 UI
 *   PendingIntent 方法。两者不是通用任意 Intent 启动通道，Native 侧仅
 *   做策略登记、版本判断和安全跳过。
 *
 * 主要函数：
 *   - fw_start_credential_manager：CredentialManager 系统 UI 路径
 *   - fw_start_print_manager：PrintManager 系统 UI 路径
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_credential_manager(FwStartContext& ctx) {
    if (ctx.sdkInt < 34) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_CREDENTIAL_MANAGER,
                "CredentialManager UI 路径主要影响 Android 14");
    }
    LOGW("CredentialManager 策略已纳入策略表：该路径只适用于系统凭据 UI，不作为任意 Activity 启动通道");
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_CREDENTIAL_MANAGER,
            "CredentialManager createPendingIntent 路径仅登记，不执行系统 UI 滥用");
}

FwStartResult fw_start_print_manager(FwStartContext& ctx) {
    if (ctx.sdkInt < 23 || ctx.sdkInt > 34) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_PRINT_MANAGER,
                "PrintManager PendingIntent 研究路径仅覆盖 Android 6-14 旧实现窗口");
    }
    LOGW("PrintManager 策略已纳入策略表：print dialog PendingIntent 属于系统 UI 场景，不在 SDK 内执行");
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_PRINT_MANAGER,
            "PrintManager print dialog 路径仅登记，不执行系统 UI 滥用");
}
