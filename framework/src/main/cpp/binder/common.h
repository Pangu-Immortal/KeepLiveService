/**
 * ============================================================================
 * common.h - 公共头文件
 * ============================================================================
 *
 * 功能简介：
 *   定义公共类型和日志宏，用于 Binder 直接调用相关的 C++ 代码。
 *   包含日志输出宏、调试开关、JNI 类路径定义以及 UTF 字符类型定义。
 *
 * 主要内容：
 *   - LOGW/LOGE/LOGI/LOGD：日志输出宏
 *   - FW_DEBUG：调试开关
 *   - FORCE_STOP_JNI_CLASS：JNI 类路径
 *   - Char16/Char32：UTF 字符类型定义
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
#ifndef FW_BINDER_COMMON_H
#define FW_BINDER_COMMON_H

#include <android/log.h>
#include <stdint.h>
#include <sys/types.h>

// 日志标签
#define TAG        "FwForceStop"

// 日志宏定义
#define LOGW(...)    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 调试开关
#define FW_DEBUG 1

#ifdef FW_DEBUG
#define LOGI(...)    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(format, ...)   __android_log_print(ANDROID_LOG_DEBUG, TAG, \
                                    "[%s] : %d ---> " format "%s", \
                                    __FUNCTION__, __LINE__, \
                                    ##__VA_ARGS__, "\n")
#else
#define LOGI(...)
#define LOGD(...)
#endif

// JNI 类路径
#define FORCE_STOP_JNI_CLASS "com/service/framework/native/FwNative"

// 类型定义（UTF-16/UTF-32 字符类型）
typedef unsigned short Char16;
typedef unsigned int Char32;

#endif // FW_BINDER_COMMON_H
