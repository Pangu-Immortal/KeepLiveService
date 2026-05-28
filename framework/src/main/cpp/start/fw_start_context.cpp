/**
 * ============================================================================
 * fw_start_context.cpp - Context 系 startActivity 策略
 * ============================================================================
 *
 * 功能简介：
 *   实现 Activity Context 直启、非 Activity 加 NEW_TASK、双 Intent
 *   startActivities 等放大镜 qumeng 中的基础路径。
 *
 * 主要函数：
 *   - fw_start_context_direct：Activity 上下文直接启动
 *   - fw_start_context_new_task：非 Activity 添加 NEW_TASK 后启动
 *   - fw_start_double_start_activities：双 Intent startActivities
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

FwStartResult fw_start_context_direct(FwStartContext& ctx) {
    if (!fw_start_is_activity(ctx.env, ctx.context)) {
        return fw_start_failure(
                FW_START_CODE_NOT_ACTIVITY_CONTEXT,
                FW_START_CONTEXT_DIRECT,
                "当前 Context 不是 Activity，直启策略不适用");
    }
    bool started = fw_start_call_context_start_activity(ctx.env, ctx.context, ctx.intent, nullptr);
    if (!started) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_DIRECT,
                "stage failed");
    }
    return fw_start_success(FW_START_CONTEXT_DIRECT, "Activity Context 直启成功");
}

FwStartResult fw_start_context_new_task(FwStartContext& ctx) {
    jobject clonedIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (clonedIntent == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_NEW_TASK,
                "复制 Intent 失败");
    }
    fw_start_add_new_task_flag(ctx.env, clonedIntent);
    bool started = fw_start_call_context_start_activity(ctx.env, ctx.context, clonedIntent, nullptr);
    ctx.env->DeleteLocalRef(clonedIntent);
    if (!started) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_NEW_TASK,
                "stage failed");
    }
    return fw_start_success(FW_START_CONTEXT_NEW_TASK, "Context + FLAG_ACTIVITY_NEW_TASK 启动成功");
}

FwStartResult fw_start_context_new_task_exclude_recents(FwStartContext& ctx) {
    jobject clonedIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (clonedIntent == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS,
                "复制 Intent 失败");
    }
    bool flagsAdded = fw_start_add_intent_flags(
            ctx.env,
            clonedIntent,
            0x10000000 | 0x00800000 | 0x00010000,
            "stage");
    if (!flagsAdded) {
        ctx.env->DeleteLocalRef(clonedIntent);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS,
                "添加 NEW_TASK/EXCLUDE_FROM_RECENTS/NO_ANIMATION 标志失败");
    }
    bool started = fw_start_call_context_start_activity(ctx.env, ctx.context, clonedIntent, nullptr);
    ctx.env->DeleteLocalRef(clonedIntent);
    if (!started) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS,
                "stage failed");
    }
    return fw_start_success(
            FW_START_CONTEXT_NEW_TASK_EXCLUDE_RECENTS,
            "Context + NEW_TASK + EXCLUDE_FROM_RECENTS + NO_ANIMATION 启动成功");
}

FwStartResult fw_start_double_start_activities(FwStartContext& ctx) {
    jobject firstIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    jobject secondIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (firstIntent == nullptr || secondIntent == nullptr) {
        if (firstIntent != nullptr) {
            ctx.env->DeleteLocalRef(firstIntent);
        }
        if (secondIntent != nullptr) {
            ctx.env->DeleteLocalRef(secondIntent);
        }
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_DOUBLE_START_ACTIVITIES,
                "复制双 Intent 失败");
    }
    fw_start_add_new_task_flag(ctx.env, firstIntent);
    fw_start_add_new_task_flag(ctx.env, secondIntent);
    jobjectArray intentArray = fw_start_new_intent_array(ctx.env, firstIntent, secondIntent);
    ctx.env->DeleteLocalRef(firstIntent);
    ctx.env->DeleteLocalRef(secondIntent);
    if (intentArray == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_DOUBLE_START_ACTIVITIES,
                "构造 Intent[] 失败");
    }
    jclass contextClass = ctx.env->GetObjectClass(ctx.context);
    jmethodID startActivities = ctx.env->GetMethodID(
            contextClass,
            FW_PROTECT_STR("startActivities").c_str(),
            FW_PROTECT_STR("([Landroid/content/Intent;)V").c_str());
    if (startActivities == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(intentArray);
        ctx.env->DeleteLocalRef(contextClass);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_DOUBLE_START_ACTIVITIES,
                "Context.startActivities 方法查找失败");
    }
    ctx.env->CallVoidMethod(ctx.context, startActivities, intentArray);
    bool failed = fw_start_clear_exception(ctx.env, "stage");
    ctx.env->DeleteLocalRef(intentArray);
    ctx.env->DeleteLocalRef(contextClass);
    if (failed) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_DOUBLE_START_ACTIVITIES,
                "双 Intent startActivities 抛出异常");
    }
    LOGI("start strategy executed: mask=%d", FW_START_DOUBLE_START_ACTIVITIES);
    return fw_start_success(FW_START_DOUBLE_START_ACTIVITIES, "双 Intent startActivities 启动成功");
}
