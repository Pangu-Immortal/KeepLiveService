/**
 * ============================================================================
 * fw_start_next_matching.cpp - startNextMatchingActivity 策略
 * ============================================================================
 *
 * 功能简介：
 *   吸收微信收藏 832 的 startNextMatchingActivity 路径。该 API 只能由
 *   Activity Context 在前台语义下调用，Native 层仅执行公开 API 安全路径。
 *
 * 主要函数：
 *   - fw_start_next_matching：调用 Activity.startNextMatchingActivity
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include "fw_start_jni_cache.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_next_matching(FwStartContext& ctx) {
    if (!fw_start_is_activity(ctx.env, ctx.context)) {
        return fw_start_failure(
                FW_START_CODE_NOT_ACTIVITY_CONTEXT,
                FW_START_START_NEXT_MATCHING,
                "startNextMatchingActivity 需要 Activity Context");
    }
    jclass activityClass = ctx.env->GetObjectClass(ctx.context);
    jmethodID startNextMatchingActivity = ctx.env->GetMethodID(
            activityClass,
            "startNextMatchingActivity",
            "(Landroid/content/Intent;)Z");
    if (startNextMatchingActivity == nullptr) {
        fw_start_clear_exception(ctx.env, "Activity.startNextMatchingActivity");
        ctx.env->DeleteLocalRef(activityClass);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_START_NEXT_MATCHING,
                "startNextMatchingActivity 方法查找失败");
    }
    jboolean result = ctx.env->CallBooleanMethod(ctx.context, startNextMatchingActivity, ctx.intent);
    bool failed = fw_start_clear_exception(ctx.env, "Activity.startNextMatchingActivity()");
    ctx.env->DeleteLocalRef(activityClass);
    if (failed || result != JNI_TRUE) {
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_START_NEXT_MATCHING,
                "startNextMatchingActivity 未找到下一个匹配 Activity 或被系统拦截");
    }
    LOGI("startNextMatchingActivity 公开 API 路径执行成功");
    return fw_start_success(FW_START_START_NEXT_MATCHING, "startNextMatchingActivity 启动成功");
}
