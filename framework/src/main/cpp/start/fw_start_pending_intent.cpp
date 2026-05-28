/**
 * ============================================================================
 * fw_start_pending_intent.cpp - PendingIntent startActivity 策略
 * ============================================================================
 *
 * 功能简介：
 *   实现放大镜 qumeng 的 PendingIntent.getActivity(...).send() 路径，并按
 *   Android 10+ / Android 14+ 的 BAL API 构造 ActivityOptions。
 *
 * 主要函数：
 *   - fw_start_pending_intent_send：创建并发送 Activity PendingIntent
 *   - fw_start_notification_bal：登记 Notification BAL 文章策略并执行安全跳过
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
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static int fw_start_pending_intent_flags(int sdkInt) {
    int flags = 0x08000000;
    if (sdkInt >= 23) {
        flags |= 0x04000000;
    }
    return flags;
}

FwStartResult fw_start_pending_intent_send(FwStartContext& ctx) {
    jobject clonedIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (clonedIntent == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_PENDING_INTENT_SEND,
                "复制 Intent 失败");
    }
    fw_start_add_new_task_flag(ctx.env, clonedIntent);
    jclass pendingIntentClass = ctx.env->FindClass("android/app/PendingIntent");
    if (pendingIntentClass == nullptr) {
        fw_start_clear_exception(ctx.env, "FindClass(PendingIntent)");
        ctx.env->DeleteLocalRef(clonedIntent);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_PENDING_INTENT_SEND,
                "PendingIntent 类查找失败");
    }
    jmethodID getActivity = ctx.env->GetStaticMethodID(
            pendingIntentClass,
            "getActivity",
            "(Landroid/content/Context;ILandroid/content/Intent;ILandroid/os/Bundle;)Landroid/app/PendingIntent;");
    if (getActivity == nullptr) {
        fw_start_clear_exception(ctx.env, "PendingIntent.getActivity(Context,int,Intent,int,Bundle)");
        getActivity = ctx.env->GetStaticMethodID(
                pendingIntentClass,
                "getActivity",
                "(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;");
        if (getActivity == nullptr) {
            fw_start_clear_exception(ctx.env, "PendingIntent.getActivity");
            ctx.env->DeleteLocalRef(clonedIntent);
            ctx.env->DeleteLocalRef(pendingIntentClass);
            return fw_start_failure(
                    FW_START_CODE_JNI_EXCEPTION,
                    FW_START_PENDING_INTENT_SEND,
                    "PendingIntent.getActivity 方法查找失败");
        }
    }
    int requestCode = static_cast<int>(getpid() & 0x7fffffff);
    jobject creatorOptions = fw_start_get_activity_options_bundle(ctx.env, true, ctx.sdkInt);
    jobject pendingIntent = nullptr;
    if (creatorOptions != nullptr) {
        pendingIntent = ctx.env->CallStaticObjectMethod(
                pendingIntentClass,
                getActivity,
                ctx.context,
                requestCode,
                clonedIntent,
                fw_start_pending_intent_flags(ctx.sdkInt),
                creatorOptions);
        ctx.env->DeleteLocalRef(creatorOptions);
    } else {
        jmethodID getActivityLegacy = ctx.env->GetStaticMethodID(
                pendingIntentClass,
                "getActivity",
                "(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;");
        if (getActivityLegacy != nullptr) {
            pendingIntent = ctx.env->CallStaticObjectMethod(
                    pendingIntentClass,
                    getActivityLegacy,
                    ctx.context,
                    requestCode,
                    clonedIntent,
                    fw_start_pending_intent_flags(ctx.sdkInt));
        } else {
            fw_start_clear_exception(ctx.env, "PendingIntent.getActivity legacy lookup");
        }
    }
    ctx.env->DeleteLocalRef(clonedIntent);
    if (fw_start_clear_exception(ctx.env, "PendingIntent.getActivity()") || pendingIntent == nullptr) {
        ctx.env->DeleteLocalRef(pendingIntentClass);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_PENDING_INTENT_SEND,
                "创建 Activity PendingIntent 失败");
    }
    jobject optionsBundle = fw_start_get_activity_options_bundle(ctx.env, true, ctx.sdkInt);
    bool sent = false;
    if (optionsBundle != nullptr) {
        jmethodID sendWithBundle = ctx.env->GetMethodID(
                pendingIntentClass,
                "send",
                "(Landroid/content/Context;ILandroid/content/Intent;Landroid/app/PendingIntent$OnFinished;Landroid/os/Handler;Ljava/lang/String;Landroid/os/Bundle;)V");
        if (sendWithBundle != nullptr) {
            ctx.env->CallVoidMethod(pendingIntent, sendWithBundle, ctx.context, 0, nullptr, nullptr, nullptr, nullptr, optionsBundle);
            sent = !fw_start_clear_exception(ctx.env, "PendingIntent.send(Bundle)");
        } else {
            fw_start_clear_exception(ctx.env, "PendingIntent.send(Bundle) lookup");
        }
        ctx.env->DeleteLocalRef(optionsBundle);
    }
    if (!sent) {
        jmethodID send = ctx.env->GetMethodID(pendingIntentClass, "send", "()V");
        if (send == nullptr) {
            fw_start_clear_exception(ctx.env, "PendingIntent.send()");
            ctx.env->DeleteLocalRef(pendingIntent);
            ctx.env->DeleteLocalRef(pendingIntentClass);
            return fw_start_failure(
                    FW_START_CODE_JNI_EXCEPTION,
                    FW_START_PENDING_INTENT_SEND,
                    "PendingIntent.send 方法查找失败");
        }
        ctx.env->CallVoidMethod(pendingIntent, send);
        sent = !fw_start_clear_exception(ctx.env, "PendingIntent.send()");
    }
    ctx.env->DeleteLocalRef(pendingIntent);
    ctx.env->DeleteLocalRef(pendingIntentClass);
    if (!sent) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_PENDING_INTENT_SEND,
                "PendingIntent.send 抛出异常");
    }
    LOGI("PendingIntent.getActivity(...).send() 执行成功");
    return fw_start_success(FW_START_PENDING_INTENT_SEND, "PendingIntent send 启动成功");
}

FwStartResult fw_start_notification_bal(FwStartContext& ctx) {
    (void) ctx;
    LOGW("Notification BAL 策略已纳入策略表：publicVersion.mAllowlistToken 路径属于漏洞复现，不执行漏洞利用");
    return fw_start_failure(
            FW_START_CODE_SKIPPED_BY_POLICY,
            FW_START_NOTIFICATION_BAL,
            "Notification publicVersion BAL 仅记录版本判断和跳过原因，不执行漏洞利用");
}
