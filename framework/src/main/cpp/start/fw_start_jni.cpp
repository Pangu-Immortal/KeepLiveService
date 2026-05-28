/**
 * ============================================================================
 * fw_start_jni.cpp - 统一 startActivity JNI 桥接
 * ============================================================================
 *
 * 功能简介：
 *   为 Kotlin FwNative.nativeStartActivity 暴露 Native 统一 start 函数。
 *
 * 主要函数：
 *   - registerStartNative: 动态注册 nativeStartActivity
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"
#include "fw_jni_protect.h"
#include "fw_jni_register.h"

static jint JNICALL
nativeStartActivity(
        JNIEnv* env,
        jobject /* this */,
        jobject context,
        jobject intent,
        jint modeMask,
        jint sdkInt) {
    return start(
            env,
            context,
            intent,
            static_cast<int>(modeMask),
            static_cast<int>(sdkInt));
}

/**
 * 动态注册统一 startActivity JNI 入口。
 */
bool registerStartNative(JNIEnv* env) {
    return fw::jni::registerNativeMethods(env, FW_PROTECT_STR("com/service/framework/native/FwNative"), {
            {FW_PROTECT_STR("nativeStartActivity"), FW_PROTECT_STR("(Landroid/content/Context;Landroid/content/Intent;II)I"), reinterpret_cast<void*>(nativeStartActivity)}
    }, "FwStartJNI");
}
