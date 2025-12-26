/**
 * ============================================================================
 * fw_process.cpp - Native 层进程管理
 * ============================================================================
 *
 * 功能简介：
 *   提供 Native 层进程管理功能，包括进程优先级设置、OOM adj 值读写、
 *   内存信息获取、root 权限检测等系统级操作。
 *
 * 核心机制：
 *   1. 提升进程优先级
 *   2. 设置进程 OOM adj 值
 *   3. 监控系统资源
 *
 * 安全研究要点：
 *   - Android 使用 OOM Killer 管理进程
 *   - OOM adj 值越低，进程越不容易被杀
 *   - 前台进程 adj = 0，后台进程 adj 较高
 *   - 需要 root 权限才能修改其他进程的 adj
 *
 * 进程优先级（OOM adj 值）：
 *   - NATIVE_ADJ (-1000): Native 进程
 *   - SYSTEM_ADJ (-900): 系统进程
 *   - PERSISTENT_PROC_ADJ (-800): 持久化进程
 *   - PERSISTENT_SERVICE_ADJ (-700): 持久化服务
 *   - FOREGROUND_APP_ADJ (0): 前台应用
 *   - VISIBLE_APP_ADJ (100): 可见应用
 *   - PERCEPTIBLE_APP_ADJ (200): 可感知应用
 *   - BACKUP_APP_ADJ (300): 备份应用
 *   - HEAVY_WEIGHT_APP_ADJ (400): 重量级应用
 *   - SERVICE_ADJ (500): 服务
 *   - HOME_APP_ADJ (600): Home 应用
 *   - PREVIOUS_APP_ADJ (700): 上一个应用
 *   - SERVICE_B_ADJ (800): 后台服务
 *   - CACHED_APP_MIN_ADJ (900): 缓存应用最小值
 *   - CACHED_APP_MAX_ADJ (999): 缓存应用最大值
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dirent.h>

#define LOG_TAG "FwNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 获取当前进程的 OOM adj 值
 *
 * 读取 /proc/self/oom_score_adj 或 /proc/self/oom_adj
 */
extern "C" int get_oom_adj() {
    int adj = 1000; // 默认值

    // 尝试读取 oom_score_adj (新版本)
    FILE* fp = fopen("/proc/self/oom_score_adj", "r");
    if (fp != nullptr) {
        if (fscanf(fp, "%d", &adj) != 1) {
            adj = 1000;
        }
        fclose(fp);
        LOGD("当前进程 OOM score adj: %d", adj);
        return adj;
    }

    // 回退到 oom_adj (旧版本)
    fp = fopen("/proc/self/oom_adj", "r");
    if (fp != nullptr) {
        if (fscanf(fp, "%d", &adj) != 1) {
            adj = 15;
        }
        fclose(fp);
        // 转换旧版本值到新版本
        adj = adj * 1000 / 17;
        LOGD("当前进程 OOM adj: %d (转换后)", adj);
        return adj;
    }

    LOGW("无法读取 OOM adj 值");
    return adj;
}

/**
 * 尝试设置 OOM adj 值
 *
 * 注意：普通应用没有权限修改此值
 * 仅供研究参考，实际上不会生效
 *
 * @param adj 目标 adj 值
 * @return 是否成功
 */
extern "C" bool set_oom_adj(int adj) {
    LOGI("尝试设置 OOM adj 为: %d", adj);

    // 尝试写入 oom_score_adj
    FILE* fp = fopen("/proc/self/oom_score_adj", "w");
    if (fp != nullptr) {
        int ret = fprintf(fp, "%d", adj);
        fclose(fp);

        if (ret > 0) {
            LOGI("OOM adj 设置成功（可能被系统覆盖）");
            return true;
        }
    }

    LOGW("无法设置 OOM adj（需要 root 权限）");
    return false;
}

/**
 * 设置进程优先级
 *
 * 使用 setpriority() 系统调用
 * nice 值范围：-20（最高优先级）到 19（最低优先级）
 *
 * 注意：普通应用只能降低优先级，不能提高
 */
extern "C" bool set_process_priority(int priority) {
    LOGI("尝试设置进程优先级为: %d", priority);

    // priority 应该是 nice 值，范围 -20 到 19
    if (priority < -20) priority = -20;
    if (priority > 19) priority = 19;

    int ret = setpriority(PRIO_PROCESS, 0, priority);

    if (ret == 0) {
        LOGI("进程优先级设置成功: %d", priority);
        return true;
    } else {
        LOGW("进程优先级设置失败: %s", strerror(errno));
        return false;
    }
}

/**
 * 获取当前进程优先级
 */
extern "C" int get_process_priority() {
    errno = 0;
    int priority = getpriority(PRIO_PROCESS, 0);

    if (errno != 0) {
        LOGW("获取进程优先级失败: %s", strerror(errno));
        return 0;
    }

    LOGD("当前进程优先级: %d", priority);
    return priority;
}

/**
 * 获取进程状态信息
 *
 * 读取 /proc/self/status 获取详细信息
 */
extern "C" void get_process_status(char* buffer, int buffer_size) {
    if (buffer == nullptr || buffer_size <= 0) return;

    buffer[0] = '\0';

    FILE* fp = fopen("/proc/self/status", "r");
    if (fp == nullptr) {
        snprintf(buffer, buffer_size, "无法读取进程状态");
        return;
    }

    char line[256];
    char result[4096] = {0};
    int offset = 0;

    // 只读取关键信息
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, "Name:", 5) == 0 ||
            strncmp(line, "State:", 6) == 0 ||
            strncmp(line, "Pid:", 4) == 0 ||
            strncmp(line, "PPid:", 5) == 0 ||
            strncmp(line, "Threads:", 8) == 0 ||
            strncmp(line, "VmSize:", 7) == 0 ||
            strncmp(line, "VmRSS:", 6) == 0 ||
            strncmp(line, "VmPeak:", 7) == 0) {

            int len = strlen(line);
            if (offset + len < (int)sizeof(result) - 1) {
                strcpy(result + offset, line);
                offset += len;
            }
        }
    }

    fclose(fp);

    strncpy(buffer, result, buffer_size - 1);
    buffer[buffer_size - 1] = '\0';
}

/**
 * 获取系统内存信息
 *
 * 读取 /proc/meminfo
 */
extern "C" void get_memory_info(long* total_kb, long* free_kb, long* available_kb) {
    if (total_kb == nullptr || free_kb == nullptr || available_kb == nullptr) return;

    *total_kb = 0;
    *free_kb = 0;
    *available_kb = 0;

    FILE* fp = fopen("/proc/meminfo", "r");
    if (fp == nullptr) {
        LOGW("无法读取内存信息");
        return;
    }

    char line[256];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, "MemTotal:", 9) == 0) {
            sscanf(line + 9, "%ld", total_kb);
        } else if (strncmp(line, "MemFree:", 8) == 0) {
            sscanf(line + 8, "%ld", free_kb);
        } else if (strncmp(line, "MemAvailable:", 13) == 0) {
            sscanf(line + 13, "%ld", available_kb);
        }
    }

    fclose(fp);

    LOGD("系统内存: 总计=%ld KB, 空闲=%ld KB, 可用=%ld KB",
         *total_kb, *free_kb, *available_kb);
}

/**
 * 检查是否有 root 权限
 */
extern "C" bool check_root() {
    // 检查 uid
    if (getuid() == 0 || geteuid() == 0) {
        LOGI("检测到 root 权限 (uid=0)");
        return true;
    }

    // 尝试访问 root 专属文件
    if (access("/system/app/Superuser.apk", F_OK) == 0 ||
        access("/system/xbin/su", F_OK) == 0 ||
        access("/system/bin/su", F_OK) == 0 ||
        access("/data/local/bin/su", F_OK) == 0 ||
        access("/sbin/su", F_OK) == 0) {
        LOGI("检测到 su 二进制文件存在");
        return true;
    }

    // 检查 Magisk
    if (access("/sbin/.magisk", F_OK) == 0 ||
        access("/data/adb/magisk", F_OK) == 0) {
        LOGI("检测到 Magisk");
        return true;
    }

    LOGD("未检测到 root 权限");
    return false;
}

/**
 * 获取进程数量
 */
extern "C" int get_process_count() {
    int count = 0;
    DIR* dir = opendir("/proc");

    if (dir == nullptr) {
        LOGW("无法打开 /proc 目录");
        return -1;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        // 检查是否是数字目录（进程 PID）
        if (entry->d_type == DT_DIR) {
            bool is_pid = true;
            for (const char* p = entry->d_name; *p != '\0'; p++) {
                if (*p < '0' || *p > '9') {
                    is_pid = false;
                    break;
                }
            }
            if (is_pid && entry->d_name[0] != '\0') {
                count++;
            }
        }
    }

    closedir(dir);

    LOGD("系统进程数量: %d", count);
    return count;
}
