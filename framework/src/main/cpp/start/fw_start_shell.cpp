/**
 * ============================================================================
 * fw_start_shell.cpp - shell start-in-vsync 策略登记
 * ============================================================================
 *
 * 功能简介：
 *   吸收微信收藏 830 中 am start-in-vsync 方法。普通应用不具备 shell/root
 *   身份，Native 侧只记录版本和权限判断，不执行命令注入或提权。
 *
 * 主要函数：
 *   - fw_start_shell_start_in_vsync：shell/root 条件策略占位
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "FwStart"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_shell_start_in_vsync(FwStartContext& ctx) {
    if (ctx.sdkInt < 35) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_SHELL_START_IN_VSYNC,
                "am start-in-vsync 研究路径仅针对新系统 shell 命令");
    }
    if (geteuid() != 0 && geteuid() != 2000) {
        LOGW("start strategy requires privilege: mask=%d, euid=%d", FW_START_SHELL_START_IN_VSYNC, geteuid());
        return fw_start_failure(
                FW_START_CODE_REQUIRES_PRIVILEGE,
                FW_START_SHELL_START_IN_VSYNC,
                "普通应用无 shell/root 身份，不能执行 am start-in-vsync");
    }
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_SHELL_START_IN_VSYNC,
            "检测到 shell/root 身份，但 SDK 不执行命令型任意 Activity 启动");
}
