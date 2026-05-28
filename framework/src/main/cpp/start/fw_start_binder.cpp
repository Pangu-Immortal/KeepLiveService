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
    jclass parcelClass = env->FindClass(FW_PROTECT_STR("android/os/Parcel").c_str());
    if (parcelClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jmethodID obtain = env->GetStaticMethodID(
            parcelClass,
            FW_PROTECT_STR("obtain").c_str(),
            FW_PROTECT_STR("()Landroid/os/Parcel;").c_str());
    if (obtain == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(parcelClass);
        return nullptr;
    }
    jobject parcel = env->CallStaticObjectMethod(parcelClass, obtain);
    fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(parcelClass);
    return parcel;
}

static void fw_start_recycle_parcel(JNIEnv* env, jobject parcel) {
    if (parcel == nullptr) {
        return;
    }
    jclass parcelClass = env->GetObjectClass(parcel);
    jmethodID recycle = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("recycle").c_str(),
            FW_PROTECT_STR("()V").c_str());
    if (recycle != nullptr) {
        env->CallVoidMethod(parcel, recycle);
        fw_start_clear_exception(env, "stage");
    } else {
        fw_start_clear_exception(env, "stage");
    }
    env->DeleteLocalRef(parcelClass);
}

static bool fw_start_write_binder_parcel(FwStartContext& ctx, jobject dataParcel, jobjectArray intentArray) {
    JNIEnv* env = ctx.env;
    jclass parcelClass = env->GetObjectClass(dataParcel);
    jmethodID writeInterfaceToken = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeInterfaceToken").c_str(),
            FW_PROTECT_STR("(Ljava/lang/String;)V").c_str());
    jmethodID writeStrongBinder = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeStrongBinder").c_str(),
            FW_PROTECT_STR("(Landroid/os/IBinder;)V").c_str());
    jmethodID writeString = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeString").c_str(),
            FW_PROTECT_STR("(Ljava/lang/String;)V").c_str());
    jmethodID writeTypedArray = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeTypedArray").c_str(),
            FW_PROTECT_STR("([Landroid/os/Parcelable;I)V").c_str());
    jmethodID writeStringArray = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeStringArray").c_str(),
            FW_PROTECT_STR("([Ljava/lang/String;)V").c_str());
    jmethodID writeInt = env->GetMethodID(
            parcelClass,
            FW_PROTECT_STR("writeInt").c_str(),
            FW_PROTECT_STR("(I)V").c_str());
    if (writeInterfaceToken == nullptr || writeStrongBinder == nullptr || writeString == nullptr ||
        writeTypedArray == nullptr || writeStringArray == nullptr || writeInt == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(parcelClass);
        return false;
    }
    std::string descriptor = ctx.sdkInt >= 29
                             ? FW_PROTECT_STR("android.app.IActivityTaskManager")
                             : FW_PROTECT_STR("android.app.IActivityManager");
    jstring descriptorString = env->NewStringUTF(descriptor.c_str());
    std::string packageName = fw_start_get_context_package(env, ctx.context);
    jstring packageString = env->NewStringUTF(packageName.c_str());
    env->CallVoidMethod(dataParcel, writeInterfaceToken, descriptorString);
    env->CallVoidMethod(dataParcel, writeStrongBinder, nullptr);
    env->CallVoidMethod(dataParcel, writeString, packageString);
    if (ctx.sdkInt >= 30) {
        env->CallVoidMethod(dataParcel, writeString, nullptr);
    }
    env->CallVoidMethod(dataParcel, writeTypedArray, intentArray, 0);
    jclass stringClass = env->FindClass(FW_PROTECT_STR("java/lang/String").c_str());
    jobjectArray resolvedTypes = env->NewObjectArray(2, stringClass, nullptr);
    env->CallVoidMethod(dataParcel, writeStringArray, resolvedTypes);
    env->CallVoidMethod(dataParcel, writeStrongBinder, nullptr);
    env->CallVoidMethod(dataParcel, writeInt, 0);
    env->CallVoidMethod(dataParcel, writeInt, fw_start_get_context_user_id(env, ctx.context));
    bool failed = fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(resolvedTypes);
    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(packageString);
    env->DeleteLocalRef(descriptorString);
    env->DeleteLocalRef(parcelClass);
    return !failed;
}

static int fw_start_get_transaction_start_activities(JNIEnv* env, int sdkInt) {
    std::string stubClassName = sdkInt >= 29
                                ? FW_PROTECT_STR("android/app/IActivityTaskManager$Stub")
                                : FW_PROTECT_STR("android/app/IActivityManager$Stub");
    int fallbackCode = 0;
    return fw_start_get_static_int_field(
            env,
            stubClassName.c_str(),
            FW_PROTECT_STR("TRANSACTION_startActivities").c_str(),
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
    jclass serviceManagerClass = ctx.env->FindClass(FW_PROTECT_STR("android/os/ServiceManager").c_str());
    if (serviceManagerClass == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "ServiceManager 类查找失败");
    }
    jmethodID getService = ctx.env->GetStaticMethodID(
            serviceManagerClass,
            FW_PROTECT_STR("getService").c_str(),
            FW_PROTECT_STR("(Ljava/lang/String;)Landroid/os/IBinder;").c_str());
    if (getService == nullptr) {
        fw_start_clear_exception(ctx.env, "stage");
        ctx.env->DeleteLocalRef(serviceManagerClass);
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_JNI_EXCEPTION,
                FW_START_BINDER_START_ACTIVITIES,
                "ServiceManager.getService 方法查找失败");
    }
    std::string serviceName = ctx.sdkInt >= 29 ? FW_PROTECT_STR("activity_task") : FW_PROTECT_STR("activity");
    jstring serviceString = ctx.env->NewStringUTF(serviceName.c_str());
    jobject activityBinder = ctx.env->CallStaticObjectMethod(serviceManagerClass, getService, serviceString);
    ctx.env->DeleteLocalRef(serviceString);
    ctx.env->DeleteLocalRef(serviceManagerClass);
    if (fw_start_clear_exception(ctx.env, "stage") || activityBinder == nullptr) {
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
    jclass binderClass = ctx.env->FindClass(FW_PROTECT_STR("android/os/IBinder").c_str());
    jmethodID transact = ctx.env->GetMethodID(
            binderClass,
            FW_PROTECT_STR("transact").c_str(),
            FW_PROTECT_STR("(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z").c_str());
    int transactionCode = fw_start_get_transaction_start_activities(ctx.env, ctx.sdkInt);
    if (transactionCode <= 0) {
        fw_start_recycle_parcel(ctx.env, dataParcel);
        fw_start_recycle_parcel(ctx.env, replyParcel);
        ctx.env->DeleteLocalRef(activityBinder);
        ctx.env->DeleteLocalRef(intentArray);
        return fw_start_failure(
                FW_START_CODE_SYSTEM_API_BLOCKED,
                FW_START_BINDER_START_ACTIVITIES,
                "无法读取 TRANSACTION_startActivities，拒绝使用魔数 fallback");
    }
    jboolean transactOk = JNI_FALSE;
    if (transact != nullptr) {
        transactOk = ctx.env->CallBooleanMethod(activityBinder, transact, transactionCode, dataParcel, replyParcel, 0);
    } else {
        fw_start_clear_exception(ctx.env, "stage");
    }
    bool failed = fw_start_clear_exception(ctx.env, "stage");
    if (!failed && transactOk == JNI_TRUE) {
        jclass parcelClass = ctx.env->GetObjectClass(replyParcel);
        jmethodID readException = ctx.env->GetMethodID(
                parcelClass,
                FW_PROTECT_STR("readException").c_str(),
                FW_PROTECT_STR("()V").c_str());
        if (readException != nullptr) {
            ctx.env->CallVoidMethod(replyParcel, readException);
            failed = fw_start_clear_exception(ctx.env, "stage");
        } else {
            fw_start_clear_exception(ctx.env, "stage");
        }
        ctx.env->DeleteLocalRef(parcelClass);
    }
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
    LOGI("start strategy executed: mask=%d, code=%d", FW_START_BINDER_START_ACTIVITIES, transactionCode);
    return fw_start_success(FW_START_BINDER_START_ACTIVITIES, "Binder startActivities transact 成功");
}
