/**
 * ============================================================================
 * fw_start_media_button.cpp - MediaButton BAL 策略登记
 * ============================================================================
 *
 * 功能简介：
 *   吸收微信收藏 835 中 MediaButtonReceiverHolder 传播 BAL/FGS 能力的
 *   方法。该路径依赖系统旧实现和真实媒体按键链路，Native 侧仅保留策略
 *   登记、版本判断和跳过日志。
 *
 * 主要函数：
 *   - fw_start_media_button_bal：MediaButton BAL 策略占位
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_media_button_bal(FwStartContext& ctx) {
    if (ctx.sdkInt < 31 || ctx.sdkInt > 34) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_MEDIA_BUTTON_BAL,
                "MediaButton BAL 研究路径仅针对 Android 12-14 旧实现窗口");
    }
    LOGW("MediaButton BAL 策略已纳入策略表：依赖系统媒体键 PendingIntent 权限传播，不在 SDK 内执行");
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_MEDIA_BUTTON_BAL,
            "MediaButtonReceiverHolder BAL 仅记录版本判断和跳过原因");
}
