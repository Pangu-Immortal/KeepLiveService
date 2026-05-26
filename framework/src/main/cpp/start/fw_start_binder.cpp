/**
 * ============================================================================
 * fw_start_binder.cpp - Binder startActivities 策略
 * ============================================================================
 *
 * 功能简介：
 *   吸收放大镜 qumeng 的 IActivityManager / IActivityTaskManager
 *   startActivities 版本分支。为避免手写 Intent Parcel 造成版本不兼容，
 *   当前通过系统 Binder 代理和 Java Parcel 完成 transact。
 *
 * 主要函数：
 *   - fw_start_binder_start_activities：按 API 选择 activity/activity_task 服务
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

static jobject fw_start_obtain_parcel(JNIEnv* env) {
    jclass parcelClass = env->FindClass("android/os/Parcel");
    if (parcelClass == nullptr) {
        fw_start_clear_exception(env, "FindClass(Parcel)");
        return nullptr;
    }
    jmethodID obtain = env->GetStaticMethodID(parcelClass, "obtain", "()Landroid/os/Parcel;");
    if (obtain == nullptr) {
        fw_start_clear_exception(env, "Parcel.obtain");
        env->DeleteLocalRef(parcelClass);
        return nullptr;
    }
    jobject parcel = env->CallStaticObjectMethod(parcelClass, obtain);
    fw_start_clear_exception(env, "Parcel.obtain()");
    env->DeleteLocalRef(parcelClass);
    return parcel;
}

static void fw_start_recycle_parcel(JNIEnv* env, jobject parcel) {
    if (parcel == nullptr) {
        return;
    }
    jclass parcelClass = env->GetObjectClass(parcel);
    jmethodID recycle = env->GetMethodID(parcelClass, "recycle", "()V");
    if (recycle != nullptr) {
        env->CallVoidMethod(parcel, recycle);
        fw_start_clear_exception(env, "Parcel.recycle()");
    } else {
        fw_start_clear_exception(env, "Parcel.recycle lookup");
    }
    env->DeleteLocalRef(parcelClass);
}

static bool fw_start_write_binder_parcel(FwStartContext& ctx, jobject dataParcel, jobjectArray intentArray) {
    JNIEnv* env = ctx.env;
    jclass parcelClass = env->GetObjectClass(dataParcel);
    jmethodID writeInterfaceToken = env->GetMethodID(parcelClass, "writeInterfaceToken", "(Ljava/lang/String;)V");
    jmethodID writeStrongBinder = env->GetMethodID(parcelClass, "writeStrongBinder", "(Landroid/os/IBinder;)V");
    jmethodID writeString = env->GetMethodID(parcelClass, "writeString", "(Ljava/lang/String;)V");
    jmethodID writeTypedArray = env->GetMethodID(parcelClass, "writeTypedArray", "([Landroid/os/Parcelable;I)V");
    jmethodID writeStringArray = env->GetMethodID(parcelClass, "writeStringArray", "([Ljava/lang/String;)V");
    jmethodID writeInt = env->GetMethodID(parcelClass, "writeInt", "(I)V");
    if (writeInterfaceToken == nullptr || writeStrongBinder == nullptr || writeString == nullptr ||
        writeTypedArray == nullptr || writeStringArray == nullptr || writeInt == nullptr) {
        fw_start_clear_exception(env, "Parcel write methods");
        env->DeleteLocalRef(parcelClass);
        return false;
    }
    const char* descriptor = ctx.sdkInt >= 29
                             ? "android.app.IActivityTaskManager"
                             : "android.app.IActivityManager";
    jstring descriptorString = env->NewStringUTF(descriptor);
    std::string packageName = fw_start_get_context_package(env, ctx.context);
    jstring packageString = env->NewStringUTF(packageName.c_str());
    env->CallVoidMethod(dataParcel, writeInterfaceToken, descriptorString);
    env->CallVoidMethod(dataParcel, writeStrongBinder, nullptr);
    env->CallVoidMethod(dataParcel, writeString, packageString);
    if (ctx.sdkInt >= 30) {
        env->CallVoidMethod(dataParcel, writeString, nullptr);
    }
    env->CallVoidMethod(dataParcel, writeTypedArray, intentArray, 0);
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resolvedTypes = env->NewObjectArray(1, stringClass, nullptr);
    env->CallVoidMethod(dataParcel, writeStringArray, resolvedTypes);
    env->CallVoidMethod(dataParcel, writeStrongBinder, nullptr);
    env->CallVoidMethod(dataParcel, writeInt, 0);
    env->CallVoidMethod(dataParcel, writeInt, fw_start_get_context_user_id(env, ctx.context));
    bool failed = fw_start_clear_exception(env, "write startActivities parcel");
    env->DeleteLocalRef(resolvedTypes);
    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(packageString);
    env->DeleteLocalRef(descriptorString);
    env->DeleteLocalRef(parcelClass);
    return !failed;
}

static int fw_start_get_transaction_start_activities(JNIEnv* env, int sdkInt) {
    const char* stubClassName = sdkInt >= 29
                                ? "android/app/IActivityTaskManager$Stub"
                                : "android/app/IActivityManager$Stub";
    int fallbackCode = sdkInt >= 29 ? 5 : 7;
    return fw_start_get_static_int_field(
            env,
            stubClassName,
            "TRANSACTION_startActivities",
            fallbackCode);
}

FwStartResult fw_start_binder_start_activities(FwStartContext& ctx) {
    if (ctx.sdkInt >= 31) {
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_BINDER_START_ACTIVITIES,
                "Android 12+ 后台 Activity 启动限制增强，Binder startActivities 仅记录不强行执行");
    }
    jobject clonedIntent = fw_start_clone_intent(ctx.env, ctx.intent);
    if (clonedIntent == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "复制 Intent 失败");
    }
    fw_start_add_new_task_flag(ctx.env, clonedIntent);
    jobjectArray intentArray = fw_start_new_intent_array(ctx.env, clonedIntent, clonedIntent);
    ctx.env->DeleteLocalRef(clonedIntent);
    if (intentArray == nullptr) {
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "构造 Binder Intent[] 失败");
    }
    jclass serviceManagerClass = ctx.env->FindClass("android/os/ServiceManager");
    if (serviceManagerClass == nullptr) {
        fw_start_clear_exception(ctx.env, "FindClass(ServiceManager)");
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "ServiceManager 类查找失败");
    }
    jmethodID getService = ctx.env->GetStaticMethodID(serviceManagerClass, "getService", "(Ljava/lang/String;)Landroid/os/IBinder;");
    if (getService == nullptr) {
        fw_start_clear_exception(ctx.env, "ServiceManager.getService");
        ctx.env->DeleteLocalRef(serviceManagerClass);
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "ServiceManager.getService 方法查找失败");
    }
    const char* serviceName = ctx.sdkInt >= 29 ? "activity_task" : "activity";
    jstring serviceString = ctx.env->NewStringUTF(serviceName);
    jobject activityBinder = ctx.env->CallStaticObjectMethod(serviceManagerClass, getService, serviceString);
    ctx.env->DeleteLocalRef(serviceString);
    ctx.env->DeleteLocalRef(serviceManagerClass);
    if (fw_start_clear_exception(ctx.env, "ServiceManager.getService()") || activityBinder == nullptr) {
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "获取 activity/activity_task Binder 失败");
    }
    jobject dataParcel = fw_start_obtain_parcel(ctx.env);
    jobject replyParcel = fw_start_obtain_parcel(ctx.env);
    if (dataParcel == nullptr || replyParcel == nullptr ||
        !fw_start_write_binder_parcel(ctx, dataParcel, intentArray)) {
        fw_start_recycle_parcel(ctx.env, dataParcel);
        fw_start_recycle_parcel(ctx.env, replyParcel);
        ctx.env->DeleteLocalRef(activityBinder);
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "写入 startActivities Parcel 失败");
    }
    jclass binderClass = ctx.env->FindClass("android/os/IBinder");
    jmethodID transact = ctx.env->GetMethodID(binderClass, "transact", "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z");
    int transactionCode = fw_start_get_transaction_start_activities(ctx.env, ctx.sdkInt);
    jboolean transactOk = JNI_FALSE;
    if (transact != nullptr) {
        transactOk = ctx.env->CallBooleanMethod(activityBinder, transact, transactionCode, dataParcel, replyParcel, 0);
    } else {
        fw_start_clear_exception(ctx.env, "IBinder.transact lookup");
    }
    bool failed = fw_start_clear_exception(ctx.env, "IBinder.transact(startActivities)");
    fw_start_recycle_parcel(ctx.env, dataParcel);
    fw_start_recycle_parcel(ctx.env, replyParcel);
    ctx.env->DeleteLocalRef(binderClass);
    ctx.env->DeleteLocalRef(activityBinder);
    ctx.env->DeleteLocalRef(intentArray);
    if (failed || transactOk != JNI_TRUE) {
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_BINDER_START_ACTIVITIES,
                "Binder startActivities transact 失败或被系统拦截");
    }
    LOGI("Binder startActivities transact 已发送，service=%s, code=%d", serviceName, transactionCode);
    return fw_start_success(FW_START_BINDER_START_ACTIVITIES, "Binder startActivities transact 成功");
}
