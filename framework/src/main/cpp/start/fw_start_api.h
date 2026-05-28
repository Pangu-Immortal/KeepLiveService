/**
 * ============================================================================
 * fw_start_api.h - Native 统一 startActivity 对外入口
 * ============================================================================
 *
 * 功能简介：
 *   声明 C++ start 文件夹对外暴露的 start 函数和各策略函数。
 *
 * 主要函数：
 *   - fw_start_activity：按版本和策略位统一尝试 startActivity
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#ifndef FW_START_API_H
#define FW_START_API_H

#include "fw_start_result.h"

int start(JNIEnv* env, jobject context, jobject intent, int modeMask, int sdkInt);
int fw_start_activity(JNIEnv* env, jobject context, jobject intent, int modeMask, int sdkInt);

FwStartResult fw_start_context_direct(FwStartContext& ctx);
FwStartResult fw_start_context_new_task(FwStartContext& ctx);
FwStartResult fw_start_context_new_task_exclude_recents(FwStartContext& ctx);
FwStartResult fw_start_pending_intent_send(FwStartContext& ctx);
FwStartResult fw_start_double_start_activities(FwStartContext& ctx);
FwStartResult fw_start_binder_start_activities(FwStartContext& ctx);
FwStartResult fw_start_start_for_result_hook(FwStartContext& ctx);
FwStartResult fw_start_virtual_display(FwStartContext& ctx);
FwStartResult fw_start_notification_bal(FwStartContext& ctx);
FwStartResult fw_start_media_button_bal(FwStartContext& ctx);
FwStartResult fw_start_next_matching(FwStartContext& ctx);
FwStartResult fw_start_credential_manager(FwStartContext& ctx);
FwStartResult fw_start_print_manager(FwStartContext& ctx);
FwStartResult fw_start_shell_start_in_vsync(FwStartContext& ctx);
FwStartResult fw_start_move_task_to_front(FwStartContext& ctx);

#endif // FW_START_API_H
