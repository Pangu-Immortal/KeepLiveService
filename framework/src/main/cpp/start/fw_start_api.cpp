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
    const char* name;
    FwStartStrategyFn function;
};

static const FwStartStrategyEntry kStrategies[] = {
        {FW_START_VIRTUAL_DISPLAY, "VIRTUAL_DISPLAY", fw_start_virtual_display},
        {FW_START_NOTIFICATION_BAL, "NOTIFICATION_BAL", fw_start_notification_bal},
        {FW_START_MEDIA_BUTTON_BAL, "MEDIA_BUTTON_BAL", fw_start_media_button_bal},
        {FW_START_BINDER_START_ACTIVITIES, "BINDER_START_ACTIVITIES", fw_start_binder_start_activities},
        {FW_START_PENDING_INTENT_SEND, "PENDING_INTENT_SEND", fw_start_pending_intent_send},
        {FW_START_DOUBLE_START_ACTIVITIES, "DOUBLE_START_ACTIVITIES", fw_start_double_start_activities},
        {FW_START_START_NEXT_MATCHING, "START_NEXT_MATCHING", fw_start_next_matching},
        {FW_START_START_FOR_RESULT_HOOK, "START_FOR_RESULT_HOOK", fw_start_start_for_result_hook},
        {FW_START_CREDENTIAL_MANAGER, "CREDENTIAL_MANAGER", fw_start_credential_manager},
        {FW_START_PRINT_MANAGER, "PRINT_MANAGER", fw_start_print_manager},
        {FW_START_SHELL_START_IN_VSYNC, "SHELL_START_IN_VSYNC", fw_start_shell_start_in_vsync},
        {FW_START_MOVE_TASK_TO_FRONT, "MOVE_TASK_TO_FRONT", fw_start_move_task_to_front},
        {FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS, "CONTEXT_NEW_TASK_EXCLUDE_RECENTS", fw_start_context_new_task_exclude_recents},
        {FW_START_CONTEXT_DIRECT, "CONTEXT_DIRECT", fw_start_context_direct},
        {FW_START_CONTEXT_NEW_TASK, "CONTEXT_NEW_TASK", fw_start_context_new_task}
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
    LOGI("统一 startActivity 开始: modeMask=%d, sdkInt=%d", modeMask, sdkInt);
    for (const FwStartStrategyEntry& entry : kStrategies) {
        if (!fw_start_has_strategy(modeMask, entry.mask)) {
            LOGD("策略跳过: %s 未在 modeMask 中启用", entry.name);
            continue;
        }
        LOGI("策略开始: %s", entry.name);
        FwStartResult result = entry.function(ctx);
        if (fw_start_is_success(result)) {
            LOGI("策略成功: %s, message=%s", entry.name, result.message);
            return entry.mask;
        }
        LOGW("策略未成功: %s, code=%s, message=%s",
             entry.name,
             fw_start_code_name(result.code),
             result.message);
        fw_start_clear_exception(env, entry.name);
    }
    LOGE("统一 startActivity 结束: 所有启用策略均未成功");
    return FW_START_CODE_NO_STRATEGY_SUCCEEDED;
}
