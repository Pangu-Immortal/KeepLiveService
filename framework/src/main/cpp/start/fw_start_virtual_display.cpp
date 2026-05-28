/**
 * ============================================================================
 * fw_start_virtual_display.cpp - 虚拟屏 startActivity 策略
 * ============================================================================
 *
 * 功能简介：
 *   吸收 so 中 QFR/XNK 的 VirtualDisplay + Presentation 思路，通过
 *   ActivityOptions.setLaunchDisplayId 在虚拟显示上尝试启动 Activity。
 *
 * 主要函数：
 *   - fw_start_virtual_display：创建虚拟屏并携带 launchDisplayId 启动
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

static void fw_start_release_virtual_display(JNIEnv* env, jobject virtualDisplay) {
    if (virtualDisplay == nullptr) {
        return;
    }
    jclass virtualDisplayClass = env->GetObjectClass(virtualDisplay);
    jmethodID release = env->GetMethodID(
            virtualDisplayClass,
            FW_PROTECT_STR("release").c_str(),
            FW_PROTECT_STR("()V").c_str());
    if (release != nullptr) {
        env->CallVoidMethod(virtualDisplay, release);
        fw_start_clear_exception(env, "stage");
    } else {
        fw_start_clear_exception(env, "stage");
    }
    env->DeleteLocalRef(virtualDisplayClass);
}

static jobject fw_start_create_surface(JNIEnv* env) {
    jclass surfaceTextureClass = env->FindClass(FW_PROTECT_STR("android/graphics/SurfaceTexture").c_str());
    if (surfaceTextureClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jmethodID textureConstructor = env->GetMethodID(
            surfaceTextureClass,
            FW_PROTECT_STR("<init>").c_str(),
            FW_PROTECT_STR("(I)V").c_str());
    if (textureConstructor == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(surfaceTextureClass);
        return nullptr;
    }
    jobject surfaceTexture = env->NewObject(surfaceTextureClass, textureConstructor, 0);
    env->DeleteLocalRef(surfaceTextureClass);
    if (fw_start_clear_exception(env, "stage") || surfaceTexture == nullptr) {
        return nullptr;
    }
    jclass surfaceClass = env->FindClass(FW_PROTECT_STR("android/view/Surface").c_str());
    if (surfaceClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(surfaceTexture);
        return nullptr;
    }
    jmethodID surfaceConstructor = env->GetMethodID(
            surfaceClass,
            FW_PROTECT_STR("<init>").c_str(),
            FW_PROTECT_STR("(Landroid/graphics/SurfaceTexture;)V").c_str());
    if (surfaceConstructor == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(surfaceTexture);
        env->DeleteLocalRef(surfaceClass);
        return nullptr;
    }
    jobject surface = env->NewObject(surfaceClass, surfaceConstructor, surfaceTexture);
    fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(surfaceTexture);
    env->DeleteLocalRef(surfaceClass);
    return surface;
}

static jobject fw_start_make_launch_display_bundle(JNIEnv* env, int displayId) {
    jclass optionsClass = env->FindClass(FW_PROTECT_STR("android/app/ActivityOptions").c_str());
    if (optionsClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jmethodID makeBasic = env->GetStaticMethodID(
            optionsClass,
            FW_PROTECT_STR("makeBasic").c_str(),
            FW_PROTECT_STR("()Landroid/app/ActivityOptions;").c_str());
    jmethodID setLaunchDisplayId = env->GetMethodID(
            optionsClass,
            FW_PROTECT_STR("setLaunchDisplayId").c_str(),
            FW_PROTECT_STR("(I)Landroid/app/ActivityOptions;").c_str());
    jmethodID toBundle = env->GetMethodID(
            optionsClass,
            FW_PROTECT_STR("toBundle").c_str(),
            FW_PROTECT_STR("()Landroid/os/Bundle;").c_str());
    if (makeBasic == nullptr || setLaunchDisplayId == nullptr || toBundle == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    jobject options = env->CallStaticObjectMethod(optionsClass, makeBasic);
    if (fw_start_clear_exception(env, "stage") || options == nullptr) {
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    env->CallObjectMethod(options, setLaunchDisplayId, displayId);
    fw_start_clear_exception(env, "stage");
    jobject bundle = env->CallObjectMethod(options, toBundle);
    fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(options);
    env->DeleteLocalRef(optionsClass);
    return bundle;
}

FwStartResult fw_start_virtual_display(FwStartContext& ctx) {
    if (ctx.sdkInt < 26) {
        return fw_start_failure(
                FW_START_CODE_UNSUPPORTED_SDK,
                FW_START_VIRTUAL_DISPLAY,
                "VirtualDisplay launchDisplayId 需要 Android 8.0+");
    }
    jobject displayManager = fw_start_get_system_service(ctx.env, ctx.context, FW_PROTECT_STR("display").c_str());
    if (displayManager == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_VIRTUAL_DISPLAY,
                "DisplayManager 获取失败");
    }
    jobject surface = fw_start_create_surface(ctx.env);
    if (surface == nullptr) {
        ctx.env->DeleteLocalRef(displayManager);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_VIRTUAL_DISPLAY,
                "Surface 创建失败");
    }
    jclass displayManagerClass = ctx.env->GetObjectClass(displayManager);
    jmethodID createVirtualDisplay = ctx.env->GetMethodID(
            displayManagerClass,
            FW_PROTECT_STR("createVirtualDisplay").c_str(),
            FW_PROTECT_STR("(Ljava/lang/String;IIILandroid/view/Surface;I)Landroid/hardware/display/VirtualDisplay;").c_str());
    if (createVirtualDisplay == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(displayManagerClass);
        ctx.env->DeleteLocalRef(surface);
        ctx.env->DeleteLocalRef(displayManager);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_VIRTUAL_DISPLAY,
                "createVirtualDisplay 方法查找失败");
    }
    std::string displayName = FW_PROTECT_STR("fw_start_virtual_display");
    jstring name = ctx.env->NewStringUTF(displayName.c_str());
    int flags = 0x00000002 | 0x00000008;
    jobject virtualDisplay = ctx.env->CallObjectMethod(
            displayManager,
            createVirtualDisplay,
            name,
            1,
            1,
            1,
            surface,
            flags);
    ctx.env->DeleteLocalRef(name);
    if (fw_start_clear_exception(ctx.env, "stage") || virtualDisplay == nullptr) {
        ctx.env->DeleteLocalRef(displayManagerClass);
        ctx.env->DeleteLocalRef(surface);
        ctx.env->DeleteLocalRef(displayManager);
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_VIRTUAL_DISPLAY,
                "创建 VirtualDisplay 失败，可能被系统或 ROM 限制");
    }
    jclass virtualDisplayClass = ctx.env->GetObjectClass(virtualDisplay);
    jmethodID getDisplay = ctx.env->GetMethodID(
            virtualDisplayClass,
            FW_PROTECT_STR("getDisplay").c_str(),
            FW_PROTECT_STR("()Landroid/view/Display;").c_str());
    jobject display = getDisplay == nullptr ? nullptr : ctx.env->CallObjectMethod(virtualDisplay, getDisplay);
    fw_start_clear_exception(ctx.env, "stage");
    int displayId = -1;
    if (display != nullptr) {
        jclass displayClass = ctx.env->GetObjectClass(display);
        jmethodID getDisplayId = ctx.env->GetMethodID(
                displayClass,
                FW_PROTECT_STR("getDisplayId").c_str(),
                FW_PROTECT_STR("()I").c_str());
        if (getDisplayId != nullptr) {
            displayId = ctx.env->CallIntMethod(display, getDisplayId);
            fw_start_clear_exception(ctx.env, "stage");
        } else {
            fw_start_clear_exception(ctx.env, "stage");
        }
        ctx.env->DeleteLocalRef(displayClass);
        ctx.env->DeleteLocalRef(display);
    }
    if (displayId < 0) {
        ctx.env->DeleteLocalRef(virtualDisplayClass);
        ctx.env->DeleteLocalRef(virtualDisplay);
        ctx.env->DeleteLocalRef(displayManagerClass);
        ctx.env->DeleteLocalRef(surface);
        ctx.env->DeleteLocalRef(displayManager);
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_VIRTUAL_DISPLAY,
                "VirtualDisplay displayId 无效");
    }
    if (ctx.sdkInt >= 29) {
        jobject activityManager = fw_start_get_system_service(ctx.env, ctx.context, FW_PROTECT_STR("activity").c_str());
        if (activityManager != nullptr) {
            jclass activityManagerClass = ctx.env->GetObjectClass(activityManager);
            jmethodID isAllowed = ctx.env->GetMethodID(
                    activityManagerClass,
                    FW_PROTECT_STR("isActivityStartAllowedOnDisplay").c_str(),
                    FW_PROTECT_STR("(Landroid/content/Context;ILandroid/content/Intent;)Z").c_str());
            if (isAllowed != nullptr) {
                jboolean allowed = ctx.env->CallBooleanMethod(activityManager, isAllowed, ctx.context, displayId, ctx.intent);
                bool checkFailed = fw_start_clear_exception(ctx.env, "stage");
                ctx.env->DeleteLocalRef(activityManagerClass);
                ctx.env->DeleteLocalRef(activityManager);
                if (checkFailed || allowed != JNI_TRUE) {
                    fw_start_release_virtual_display(ctx.env, virtualDisplay);
                    ctx.env->DeleteLocalRef(virtualDisplayClass);
                    ctx.env->DeleteLocalRef(virtualDisplay);
                    ctx.env->DeleteLocalRef(displayManagerClass);
                    ctx.env->DeleteLocalRef(surface);
                    ctx.env->DeleteLocalRef(displayManager);
                    return fw_start_failure(
                            FW_START_CODE_SYSTEM_API_BLOCKED,
                            FW_START_VIRTUAL_DISPLAY,
                            "系统不允许在该 VirtualDisplay 上启动 Activity");
                }
            } else {
                fw_start_clear_exception(ctx.env, "stage");
                ctx.env->DeleteLocalRef(activityManagerClass);
                ctx.env->DeleteLocalRef(activityManager);
            }
        }
    }
    jobject optionsBundle = fw_start_make_launch_display_bundle(ctx.env, displayId);
    jobject clonedIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (clonedIntent != nullptr) {
        fw_start_add_new_task_flag(ctx.env, clonedIntent);
    }
    bool started = optionsBundle != nullptr && clonedIntent != nullptr &&
                   fw_start_call_context_start_activity(ctx.env, ctx.context, clonedIntent, optionsBundle);
    if (clonedIntent != nullptr) {
        ctx.env->DeleteLocalRef(clonedIntent);
    }
    if (optionsBundle != nullptr) {
        ctx.env->DeleteLocalRef(optionsBundle);
    }
    fw_start_release_virtual_display(ctx.env, virtualDisplay);
    ctx.env->DeleteLocalRef(virtualDisplayClass);
    ctx.env->DeleteLocalRef(virtualDisplay);
    ctx.env->DeleteLocalRef(displayManagerClass);
    ctx.env->DeleteLocalRef(surface);
    ctx.env->DeleteLocalRef(displayManager);
    if (!started) {
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_VIRTUAL_DISPLAY,
                "VirtualDisplay + launchDisplayId 启动失败");
    }
    LOGI("start strategy executed: mask=%d, displayId=%d", FW_START_VIRTUAL_DISPLAY, displayId);
    return fw_start_success(FW_START_VIRTUAL_DISPLAY, "VirtualDisplay + launchDisplayId 启动成功");
}
