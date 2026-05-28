/**
 * ============================================================================
 * fw_jni_register.h - JNI 动态注册工具
 * ============================================================================
 *
 * 功能简介：
 *   提供统一的 RegisterNatives 封装，让 native 方法通过 JNI_OnLoad 动态注册，
 *   避免导出 Java_com_xxx 静态符号，降低 so 静态分析时直接定位入口的概率。
 *
 * 主要函数：
 *   - fw::jni::registerNativeMethods: 批量注册 native 方法
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 * ============================================================================
 */

#pragma once

#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include "fw_jni_protect.h"

namespace fw {
namespace jni {

/**
 * 单个 JNI 方法注册描述。
 */
struct NativeMethodSpec {
    std::string name;
    std::string signature;
    void* fnPtr;
};

/**
 * 批量动态注册 JNI 方法。
 */
inline bool registerNativeMethods(
        JNIEnv* env,
        const std::string& className,
        const std::vector<NativeMethodSpec>& specs,
        const char* logTag) {
    // 查找 Java/Kotlin 侧 native 方法所在类。
    jclass clazz = env->FindClass(className.c_str());
    if (clazz == nullptr) {
        __android_log_print(
                ANDROID_LOG_ERROR,
                logTag,
                "%s",
                FW_PROTECT_STR("jni bridge class lookup failed").c_str());
        return false;
    }

    // 将防护后的 std::string 注册描述转换为 JNI 所需的结构体数组。
    std::vector<JNINativeMethod> methods;
    methods.reserve(specs.size());
    for (const auto& spec : specs) {
        methods.push_back({
            const_cast<char*>(spec.name.c_str()),
            const_cast<char*>(spec.signature.c_str()),
            spec.fnPtr
        });
    }

    // 统一执行动态注册。
    const jint result = env->RegisterNatives(
            clazz,
            methods.data(),
            static_cast<jint>(methods.size()));
    env->DeleteLocalRef(clazz);

    if (result != JNI_OK) {
        __android_log_print(
                ANDROID_LOG_ERROR,
                logTag,
                "%s",
                FW_PROTECT_STR("jni bridge bind failed").c_str());
        return false;
    }
    return true;
}

} // namespace jni
} // namespace fw
