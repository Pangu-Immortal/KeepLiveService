/**
 * ============================================================================
 * fw_start_jni_cache.h - startActivity JNI 辅助函数
 * ============================================================================
 *
 * 功能简介：
 *   提供 Native startActivity 策略复用的 JNI 查找、异常清理和日志辅助。
 *
 * 主要函数：
 *   - fw_start_clear_exception：清理并记录 JNI 异常
 *   - fw_start_call_void：调用 Java void 方法并转换异常结果
 *   - fw_start_get_context_package：读取 Context 包名
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#ifndef FW_START_JNI_CACHE_H
#define FW_START_JNI_CACHE_H

#include <jni.h>
#include <string>

bool fw_start_clear_exception(JNIEnv* env, const char* stage);
bool fw_start_has_strategy(int modeMask, int strategyMask);
bool fw_start_is_activity(JNIEnv* env, jobject context);
bool fw_start_add_new_task_flag(JNIEnv* env, jobject intent);
bool fw_start_add_intent_flags(JNIEnv* env, jobject intent, int flags, const char* stage);
bool fw_start_call_context_start_activity(JNIEnv* env, jobject context, jobject intent, jobject bundle);
std::string fw_start_get_context_package(JNIEnv* env, jobject context);
int fw_start_get_context_user_id(JNIEnv* env, jobject context);
jobject fw_start_get_activity_options_bundle(JNIEnv* env, bool allowBal, int sdkInt);
jobject fw_start_clone_intent(JNIEnv* env, jobject intent);
jobjectArray fw_start_new_intent_array(JNIEnv* env, jobject firstIntent, jobject secondIntent);
jobject fw_start_get_system_service(JNIEnv* env, jobject context, const char* serviceName);
int fw_start_get_static_int_field(JNIEnv* env, const char* className, const char* fieldName, int fallbackValue);

#endif // FW_START_JNI_CACHE_H
