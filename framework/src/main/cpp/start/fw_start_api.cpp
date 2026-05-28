/**
 * ============================================================================
 * fw_start_api.cpp - Native 统一 startActivity 策略编排
 * ============================================================================
 *
 * 功能简介：
 *   聚合微信收藏、放大镜 qumeng、虚拟屏 so 逆向得到的 startActivity 方法，
 *   按版本和策略位依次执行，并输出完整日志。
 *
 * 主要函数：
 *   - fw_start_activity：对外暴露的 start 函数
 *   - fw_start_code_name：返回码转日志文本
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include "fw_start_jni_cache.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const char* fw_start_code_name(int code) {
    switch (code) {
        case FW_START_CODE_SUCCESS:
            return "SUCCESS";
        case FW_START_CODE_INVALID_ARGUMENT:
            return "INVALID_ARGUMENT";
        case FW_START_CODE_JNI_EXCEPTION:
            return "JNI_EXCEPTION";
        case FW_START_CODE_UNSUPPORTED_SDK:
            return "UNSUPPORTED_SDK";
        case FW_START_CODE_SKIPPED_BY_POLICY:
            return "SKIPPED_BY_POLICY";
        case FW_START_CODE_REQUIRES_PRIVILEGE:
            return "REQUIRES_PRIVILEGE";
        case FW_START_CODE_NOT_ACTIVITY_CONTEXT:
            return "NOT_ACTIVITY_CONTEXT";
        case FW_START_CODE_SYSTEM_API_BLOCKED:
            return "SYSTEM_API_BLOCKED";
        case FW_START_CODE_NO_STRATEGY_SUCCEEDED:
            return "NO_STRATEGY_SUCCEEDED";
        default:
            return "UNKNOWN";
    }
}

typedef FwStartResult (*FwStartStrategyFn)(FwStartContext&);

struct FwStartStrategyEntry {
    int mask;
    FwStartStrategyFn function;
};

static const FwStartStrategyEntry kStrategies[] = {
        {FW_START_VIRTUAL_DISPLAY, fw_start_virtual_display},
        {FW_START_NOTIFICATION_BAL, fw_start_notification_bal},
        {FW_START_MEDIA_BUTTON_BAL, fw_start_media_button_bal},
        {FW_START_BINDER_START_ACTIVITIES, fw_start_binder_start_activities},
        {FW_START_PENDING_INTENT_SEND, fw_start_pending_intent_send},
        {FW_START_DOUBLE_START_ACTIVITIES, fw_start_double_start_activities},
        {FW_START_START_NEXT_MATCHING, fw_start_next_matching},
        {FW_START_START_FOR_RESULT_HOOK, fw_start_start_for_result_hook},
        {FW_START_CREDENTIAL_MANAGER, fw_start_credential_manager},
        {FW_START_PRINT_MANAGER, fw_start_print_manager},
        {FW_START_SHELL_START_IN_VSYNC, fw_start_shell_start_in_vsync},
        {FW_START_MOVE_TASK_TO_FRONT, fw_start_move_task_to_front},
        {FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS, fw_start_context_new_task_exclude_recents},
        {FW_START_CONTEXT_DIRECT, fw_start_context_direct},
        {FW_START_CONTEXT_NEW_TASK, fw_start_context_new_task}
};

int start(JNIEnv* env, jobject context, jobject intent, int modeMask, int sdkInt) {
    return fw_start_activity(env, context, intent, modeMask, sdkInt);
}

int fw_start_activity(JNIEnv* env, jobject context, jobject intent, int modeMask, int sdkInt) {
    if (env == nullptr || context == nullptr || intent == nullptr) {
        LOGE("start 参数无效: env/context/intent 为空");
        return FW_START_CODE_INVALID_ARGUMENT;
    }
    FwStartContext ctx{env, context, intent, modeMask, sdkInt};
    LOGI("native start begin: modeMask=%d, sdkInt=%d", modeMask, sdkInt);
    for (const FwStartStrategyEntry& entry : kStrategies) {
        if (!fw_start_has_strategy(modeMask, entry.mask)) {
            LOGD("策略跳过: mask=%d 未在 modeMask 中启用", entry.mask);
            continue;
        }
        LOGI("策略开始: mask=%d", entry.mask);
        FwStartResult result = entry.function(ctx);
        if (fw_start_is_success(result)) {
            LOGI("策略成功: mask=%d", entry.mask);
            return entry.mask;
        }
        LOGW("策略未成功: mask=%d, code=%s", entry.mask, fw_start_code_name(result.code));
        fw_start_clear_exception(env, "strategy");
    }
    LOGE("native start end: no strategy succeeded");
    return FW_START_CODE_NO_STRATEGY_SUCCEEDED;
}
