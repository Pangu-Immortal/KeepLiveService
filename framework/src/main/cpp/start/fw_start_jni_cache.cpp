/**
 * ============================================================================
 * fw_start_jni_cache.cpp - startActivity JNI 辅助实现
 * ============================================================================
 *
 * 功能简介：
 *   封装 Native startActivity 策略需要的 JNI 公共逻辑。
 *
 * 主要函数：
 *   - fw_start_clear_exception：统一记录并清理 JNI 异常
 *   - fw_start_get_activity_options_bundle：构造 ActivityOptions Bundle
 *   - fw_start_new_intent_array：构造 Intent 数组
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_jni_cache.h"
#include "fw_start_result.h"
#include <android/log.h>

#define LOG_TAG "FwStart"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

bool fw_start_clear_exception(JNIEnv* env, const char* stage) {
    if (!env->ExceptionCheck()) {
        return false;
    }
    LOGW("JNI 异常已清理，阶段=%s", stage);
    env->ExceptionDescribe();
    env->ExceptionClear();
    return true;
}

bool fw_start_has_strategy(int modeMask, int strategyMask) {
    return (modeMask & strategyMask) == strategyMask;
}

bool fw_start_is_activity(JNIEnv* env, jobject context) {
    jclass activityClass = env->FindClass(FW_PROTECT_STR("android/app/Activity").c_str());
    if (activityClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return false;
    }
    bool result = env->IsInstanceOf(context, activityClass) == JNI_TRUE;
    env->DeleteLocalRef(activityClass);
    return result;
}

bool fw_start_add_new_task_flag(JNIEnv* env, jobject intent) {
    return fw_start_add_intent_flags(env, intent, 0x10000000, "stage");
}

bool fw_start_add_intent_flags(JNIEnv* env, jobject intent, int flags, const char* stage) {
    jclass intentClass = env->GetObjectClass(intent);
    if (intentClass == nullptr) {
        return false;
    }
    jmethodID addFlags = env->GetMethodID(
            intentClass,
            FW_PROTECT_STR("addFlags").c_str(),
            FW_PROTECT_STR("(I)Landroid/content/Intent;").c_str());
    if (addFlags == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(intentClass);
        return false;
    }
    env->CallObjectMethod(intent, addFlags, flags);
    bool failed = fw_start_clear_exception(env, stage);
    env->DeleteLocalRef(intentClass);
    return !failed;
}

bool fw_start_call_context_start_activity(JNIEnv* env, jobject context, jobject intent, jobject bundle) {
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        return false;
    }
    if (bundle == nullptr) {
        jmethodID startActivity = env->GetMethodID(
                contextClass,
                FW_PROTECT_STR("startActivity").c_str(),
                FW_PROTECT_STR("(Landroid/content/Intent;)V").c_str());
        if (startActivity == nullptr) {
            fw_start_clear_exception(env, "stage");
            env->DeleteLocalRef(contextClass);
            return false;
        }
        env->CallVoidMethod(context, startActivity, intent);
    } else {
        jmethodID startActivity = env->GetMethodID(
                contextClass,
                FW_PROTECT_STR("startActivity").c_str(),
                FW_PROTECT_STR("(Landroid/content/Intent;Landroid/os/Bundle;)V").c_str());
        if (startActivity == nullptr) {
            fw_start_clear_exception(env, "stage");
            env->DeleteLocalRef(contextClass);
            return false;
        }
        env->CallVoidMethod(context, startActivity, intent, bundle);
    }
    bool failed = fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(contextClass);
    return !failed;
}

std::string fw_start_get_context_package(JNIEnv* env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        return "";
    }
    jmethodID getPackageName = env->GetMethodID(
            contextClass,
            FW_PROTECT_STR("getPackageName").c_str(),
            FW_PROTECT_STR("()Ljava/lang/String;").c_str());
    if (getPackageName == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(contextClass);
        return "";
    }
    auto packageName = static_cast<jstring>(env->CallObjectMethod(context, getPackageName));
    if (fw_start_clear_exception(env, "stage") || packageName == nullptr) {
        env->DeleteLocalRef(contextClass);
        return "";
    }
    const char* rawPackageName = env->GetStringUTFChars(packageName, nullptr);
    std::string result = rawPackageName == nullptr ? "" : rawPackageName;
    if (rawPackageName != nullptr) {
        env->ReleaseStringUTFChars(packageName, rawPackageName);
    }
    env->DeleteLocalRef(packageName);
    env->DeleteLocalRef(contextClass);
    return result;
}

int fw_start_get_context_user_id(JNIEnv* env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        return 0;
    }
    jmethodID getUserId = env->GetMethodID(
            contextClass,
            FW_PROTECT_STR("getUserId").c_str(),
            FW_PROTECT_STR("()I").c_str());
    if (getUserId == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(contextClass);
        return 0;
    }
    jint userId = env->CallIntMethod(context, getUserId);
    if (fw_start_clear_exception(env, "stage")) {
        env->DeleteLocalRef(contextClass);
        return 0;
    }
    env->DeleteLocalRef(contextClass);
    return static_cast<int>(userId);
}

int fw_start_get_static_int_field(JNIEnv* env, const char* className, const char* fieldName, int fallbackValue) {
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        fw_start_clear_exception(env, className);
        return fallbackValue;
    }
    jfieldID field = env->GetStaticFieldID(clazz, fieldName, FW_PROTECT_STR("I").c_str());
    if (field == nullptr) {
        fw_start_clear_exception(env, fieldName);
        env->DeleteLocalRef(clazz);
        return fallbackValue;
    }
    jint value = env->GetStaticIntField(clazz, field);
    if (fw_start_clear_exception(env, fieldName)) {
        env->DeleteLocalRef(clazz);
        return fallbackValue;
    }
    env->DeleteLocalRef(clazz);
    return static_cast<int>(value);
}

jobject fw_start_get_activity_options_bundle(JNIEnv* env, bool allowBal, int sdkInt) {
    jclass optionsClass = env->FindClass(FW_PROTECT_STR("android/app/ActivityOptions").c_str());
    if (optionsClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jmethodID makeBasic = env->GetStaticMethodID(
            optionsClass,
            FW_PROTECT_STR("makeBasic").c_str(),
            FW_PROTECT_STR("()Landroid/app/ActivityOptions;").c_str());
    if (makeBasic == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    jobject options = env->CallStaticObjectMethod(optionsClass, makeBasic);
    if (fw_start_clear_exception(env, "stage") || options == nullptr) {
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    if (allowBal && sdkInt >= 34) {
        std::string modeField = sdkInt >= 36
                                ? FW_PROTECT_STR("MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE")
                                : FW_PROTECT_STR("MODE_BACKGROUND_ACTIVITY_START_ALLOWED");
        int fallbackMode = sdkInt >= 36 ? 4 : 1;
        int allowMode = fw_start_get_static_int_field(
                env,
                FW_PROTECT_STR("android/app/ActivityOptions").c_str(),
                modeField.c_str(),
                fallbackMode);
        jmethodID setMode = env->GetMethodID(
                optionsClass,
                FW_PROTECT_STR("setPendingIntentBackgroundActivityStartMode").c_str(),
                FW_PROTECT_STR("(I)Landroid/app/ActivityOptions;").c_str());
        if (setMode != nullptr) {
            env->CallObjectMethod(options, setMode, allowMode);
            fw_start_clear_exception(env, "stage");
        } else {
            fw_start_clear_exception(env, "stage");
        }
        jmethodID setCreatorMode = env->GetMethodID(
                optionsClass,
                FW_PROTECT_STR("setPendingIntentCreatorBackgroundActivityStartMode").c_str(),
                FW_PROTECT_STR("(I)Landroid/app/ActivityOptions;").c_str());
        if (setCreatorMode != nullptr) {
            env->CallObjectMethod(options, setCreatorMode, allowMode);
            fw_start_clear_exception(env, "stage");
        } else {
            fw_start_clear_exception(env, "stage");
        }
    } else if (allowBal && sdkInt >= 29) {
        jmethodID setAllowed = env->GetMethodID(
                optionsClass,
                FW_PROTECT_STR("setPendingIntentBackgroundActivityLaunchAllowed").c_str(),
                FW_PROTECT_STR("(Z)V").c_str());
        if (setAllowed != nullptr) {
            env->CallVoidMethod(options, setAllowed, JNI_TRUE);
            fw_start_clear_exception(env, "stage");
        } else {
            fw_start_clear_exception(env, "stage");
        }
    }
    jmethodID toBundle = env->GetMethodID(
            optionsClass,
            FW_PROTECT_STR("toBundle").c_str(),
            FW_PROTECT_STR("()Landroid/os/Bundle;").c_str());
    if (toBundle == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(options);
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    jobject bundle = env->CallObjectMethod(options, toBundle);
    if (fw_start_clear_exception(env, "stage")) {
        env->DeleteLocalRef(options);
        env->DeleteLocalRef(optionsClass);
        return nullptr;
    }
    env->DeleteLocalRef(options);
    env->DeleteLocalRef(optionsClass);
    return bundle;
}

jobject fw_start_clone_intent(JNIEnv* env, jobject intent) {
    jclass intentClass = env->FindClass(FW_PROTECT_STR("android/content/Intent").c_str());
    if (intentClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jmethodID constructor = env->GetMethodID(
            intentClass,
            FW_PROTECT_STR("<init>").c_str(),
            FW_PROTECT_STR("(Landroid/content/Intent;)V").c_str());
    if (constructor == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(intentClass);
        return nullptr;
    }
    jobject clone = env->NewObject(intentClass, constructor, intent);
    if (fw_start_clear_exception(env, "stage")) {
        env->DeleteLocalRef(intentClass);
        return nullptr;
    }
    env->DeleteLocalRef(intentClass);
    return clone;
}

jobjectArray fw_start_new_intent_array(JNIEnv* env, jobject firstIntent, jobject secondIntent) {
    jclass intentClass = env->FindClass(FW_PROTECT_STR("android/content/Intent").c_str());
    if (intentClass == nullptr) {
        fw_start_clear_exception(env, "stage");
        return nullptr;
    }
    jobjectArray array = env->NewObjectArray(2, intentClass, nullptr);
    if (array == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(intentClass);
        return nullptr;
    }
    env->SetObjectArrayElement(array, 0, firstIntent);
    env->SetObjectArrayElement(array, 1, secondIntent);
    if (fw_start_clear_exception(env, "stage")) {
        env->DeleteLocalRef(array);
        env->DeleteLocalRef(intentClass);
        return nullptr;
    }
    env->DeleteLocalRef(intentClass);
    return array;
}

jobject fw_start_get_system_service(JNIEnv* env, jobject context, const char* serviceName) {
    jclass contextClass = env->GetObjectClass(context);
    if (contextClass == nullptr) {
        return nullptr;
    }
    jmethodID getSystemService = env->GetMethodID(
            contextClass,
            FW_PROTECT_STR("getSystemService").c_str(),
            FW_PROTECT_STR("(Ljava/lang/String;)Ljava/lang/Object;").c_str());
    if (getSystemService == nullptr) {
        fw_start_clear_exception(env, "stage");
        env->DeleteLocalRef(contextClass);
        return nullptr;
    }
    jstring service = env->NewStringUTF(serviceName);
    jobject result = env->CallObjectMethod(context, getSystemService, service);
    fw_start_clear_exception(env, "stage");
    env->DeleteLocalRef(service);
    env->DeleteLocalRef(contextClass);
    return result;
}
