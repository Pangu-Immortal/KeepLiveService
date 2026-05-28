/**
 * ============================================================================
 * fw_start_result.h - 统一 startActivity 结果定义
 * ============================================================================
 *
 * 功能简介：
 *   定义 Native 统一 startActivity 入口使用的策略位、返回码和结果结构。
 *
 * 主要函数：
 *   - FwStartResult：承载单个策略的执行结果
 *   - fw_start_code_name：把返回码转换成可读日志
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#ifndef FW_START_RESULT_H
#define FW_START_RESULT_H

#include <jni.h>

/**
 * 统一 startActivity 策略位。
 *
 * 每个策略保持独立 bit，方便 Kotlin 层自由组合执行范围。
 */
enum FwStartStrategyMask {
    FW_START_CONTEXT_DIRECT = 1 << 0,
    FW_START_CONTEXT_NEW_TASK = 1 << 1,
    FW_START_PENDING_INTENT_SEND = 1 << 2,
    FW_START_DOUBLE_START_ACTIVITIES = 1 << 3,
    FW_START_BINDER_START_ACTIVITIES = 1 << 4,
    FW_START_START_FOR_RESULT_HOOK = 1 << 5,
    FW_START_VIRTUAL_DISPLAY = 1 << 6,
    FW_START_NOTIFICATION_BAL = 1 << 7,
    FW_START_MEDIA_BUTTON_BAL = 1 << 8,
    FW_START_START_NEXT_MATCHING = 1 << 9,
    FW_START_CREDENTIAL_MANAGER = 1 << 10,
    FW_START_PRINT_MANAGER = 1 << 11,
    FW_START_SHELL_START_IN_VSYNC = 1 << 12,
    FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS = 1 << 13,
    FW_START_MOVE_TASK_TO_FRONT = 1 << 14
};

/**
 * 统一 startActivity 返回码。
 *
 * 正数表示成功策略 bit，负数表示失败或跳过原因。
 */
enum FwStartCode {
    FW_START_CODE_SUCCESS = 0,
    FW_START_CODE_INVALID_ARGUMENT = -1,
    FW_START_CODE_JNI_EXCEPTION = -2,
    FW_START_CODE_UNSUPPORTED_SDK = -3,
    FW_START_CODE_SKIPPED_BY_POLICY = -4,
    FW_START_CODE_REQUIRES_PRIVILEGE = -5,
    FW_START_CODE_NOT_ACTIVITY_CONTEXT = -6,
    FW_START_CODE_SYSTEM_API_BLOCKED = -7,
    FW_START_CODE_NO_STRATEGY_SUCCEEDED = -8
};

/**
 * 单个策略执行结果。
 */
struct FwStartResult {
    int code;
    int strategyMask;
    const char* message;
};

/**
 * Native 统一 startActivity 的执行上下文。
 */
struct FwStartContext {
    JNIEnv* env;
    jobject context;
    jobject intent;
    int modeMask;
    int sdkInt;
};

/**
 * 判断策略是否成功。
 */
inline bool fw_start_is_success(const FwStartResult& result) {
    return result.code == FW_START_CODE_SUCCESS;
}

/**
 * 构造成功结果。
 */
inline FwStartResult fw_start_success(int strategyMask, const char* message) {
    (void) message;
    return FwStartResult{FW_START_CODE_SUCCESS, strategyMask, ""};
}

/**
 * 构造失败或跳过结果。
 */
inline FwStartResult fw_start_failure(int code, int strategyMask, const char* message) {
    (void) message;
    return FwStartResult{code, strategyMask, ""};
}

const char* fw_start_code_name(int code);

#endif // FW_START_RESULT_H
