/**
 * ============================================================================
 * fw_start_jni.cpp - 统一 startActivity JNI 桥接
 * ============================================================================
 *
 * 功能简介：
 *   为 Kotlin FwNative.nativeStartActivity 暴露 Native 统一 start 函数。
 *
 * 主要函数：
 *   - Java_com_service_framework_native_FwNative_nativeStartActivity
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include "fw_start_api.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_service_framework_native_FwNative_nativeStartActivity(
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
