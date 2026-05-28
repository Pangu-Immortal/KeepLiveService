/**
 * ============================================================================
 * fw_start_start_for_result.cpp - startActivityForResult 策略
 * ============================================================================
 *
 * 功能简介：
 *   吸收放大镜 Android 10+ 主线程 startActivityForResult 思路。隐藏
 *   ActivityThread.mH.mCallback hook 属于私有实现细节，本实现只调用公开 API。
 *
 * 主要函数：
 *   - fw_start_start_for_result_hook：Activity.startActivityForResult 安全路径
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include "fw_start_jni_cache.h"
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "FwStart"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_start_for_result_hook(FwStartContext& ctx) {
    if (!fw_start_is_activity(ctx.env, ctx.context)) {
        return fw_start_failure(
                FW_START_CODE_NOT_ACTIVITY_CONTEXT,
                FW_START_START_FOR_RESULT_HOOK,
                "stage skipped");
    }
    jclass activityClass = ctx.env->GetObjectClass(ctx.context);
    jmethodID startActivityForResult = ctx.env->GetMethodID(
            activityClass,
            FW_PROTECT_STR("startActivityForResult").c_str(),
            FW_PROTECT_STR("(Landroid/content/Intent;I)V").c_str());
    if (startActivityForResult == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(activityClass);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_START_FOR_RESULT_HOOK,
                "stage failed");
    }
    int requestCode = 17908 + static_cast<int>(getpid() % 1000);
    ctx.env->CallVoidMethod(ctx.context, startActivityForResult, ctx.intent, requestCode);
    bool failed = fw_start_clear_exception(ctx.env, "stage");
    ctx.env->DeleteLocalRef(activityClass);
    if (failed) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_START_FOR_RESULT_HOOK,
                "stage failed");
    }
    LOGI("start strategy executed: mask=%d, requestCode=%d", FW_START_START_FOR_RESULT_HOOK, requestCode);
    return fw_start_success(FW_START_START_FOR_RESULT_HOOK, "stage success");
}
