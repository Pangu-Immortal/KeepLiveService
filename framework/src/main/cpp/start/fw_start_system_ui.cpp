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
#include "fw_start_jni_cache.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

FwStartResult fw_start_credential_manager(FwStartContext& ctx) {
    if (ctx.sdkInt < 34) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_CREDENTIAL_MANAGER,
                "CredentialManager UI 路径主要影响 Android 14");
    }
    LOGW("start strategy skipped: mask=%d", FW_START_CREDENTIAL_MANAGER);
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
    LOGW("start strategy skipped: mask=%d", FW_START_PRINT_MANAGER);
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_PRINT_MANAGER,
            "PrintManager print dialog 路径仅登记，不执行系统 UI 滥用");
}

FwStartResult fw_start_move_task_to_front(FwStartContext& ctx) {
    jobject activityManager = fw_start_get_system_service(ctx.env, ctx.context, FW_PROTECT_STR("activity").c_str());
    if (activityManager == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_MOVE_TASK_TO_FRONT,
                "ActivityManager 获取失败");
    }
    jclass activityClass = ctx.env->FindClass(FW_PROTECT_STR("android/app/Activity").c_str());
    if (activityClass == nullptr || !ctx.env->IsInstanceOf(ctx.context, activityClass)) {
        fw_start_clear_exception(ctx.env, "stage");
        if (activityClass != nullptr) {
            ctx.env->DeleteLocalRef(activityClass);
        }
        ctx.env->DeleteLocalRef(activityManager);
        return fw_start_failure(
                FW_START_CODE_NOT_ACTIVITY_CONTEXT,
                FW_START_MOVE_TASK_TO_FRONT,
                "moveTaskToFront 需要 Activity Context 获取 taskId");
    }
    jmethodID getTaskId = ctx.env->GetMethodID(
            activityClass,
            FW_PROTECT_STR("getTaskId").c_str(),
            FW_PROTECT_STR("()I").c_str());
    if (getTaskId == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(activityClass);
        ctx.env->DeleteLocalRef(activityManager);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_MOVE_TASK_TO_FRONT,
                "Activity.getTaskId 方法查找失败");
    }
    int taskId = ctx.env->CallIntMethod(ctx.context, getTaskId);
    fw_start_clear_exception(ctx.env, "stage");
    jclass activityManagerClass = ctx.env->GetObjectClass(activityManager);
    jmethodID moveTaskToFront = ctx.env->GetMethodID(
            activityManagerClass,
            FW_PROTECT_STR("moveTaskToFront").c_str(),
            FW_PROTECT_STR("(II)V").c_str());
    if (moveTaskToFront == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(activityManagerClass);
        ctx.env->DeleteLocalRef(activityClass);
        ctx.env->DeleteLocalRef(activityManager);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_MOVE_TASK_TO_FRONT,
                "ActivityManager.moveTaskToFront 方法查找失败");
    }
    ctx.env->CallVoidMethod(activityManager, moveTaskToFront, taskId, 0);
    bool failed = fw_start_clear_exception(ctx.env, "stage");
    ctx.env->DeleteLocalRef(activityManagerClass);
    ctx.env->DeleteLocalRef(activityClass);
    ctx.env->DeleteLocalRef(activityManager);
    if (failed) {
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_MOVE_TASK_TO_FRONT,
                "moveTaskToFront 失败，可能缺少 REORDER_TASKS 或系统限制");
    }
    LOGI("start strategy executed: mask=%d, taskId=%d", FW_START_MOVE_TASK_TO_FRONT, taskId);
    return fw_start_success(FW_START_MOVE_TASK_TO_FRONT, "moveTaskToFront 执行成功");
}
