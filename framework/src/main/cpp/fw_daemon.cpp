/**
 * ============================================================================
 * fw_daemon.cpp - Native 层守护进程
 * ============================================================================
 *
 * 功能简介：
 *   实现 Native 层守护进程，通过 fork() 创建子进程监控主进程存活状态，
 *   在主进程被杀死时尝试通过 am 命令或广播重新拉起服务。
 *
 * 核心机制：
 *   1. 使用 fork() 创建子进程
 *   2. 子进程监控父进程（Java 层）存活状态
 *   3. 父进程死亡时通过多种方式尝试拉起
 *   4. 与 Java 层的双进程守护互补
 *
 * 安全研究要点：
 *   - Native 层进程不受 Java 层限制
 *   - 可以在 Java 层被杀后继续存活
 *   - 但在强制停止时，系统会杀死整个进程组
 *   - 某些 ROM 对 Native 守护进程有额外检测
 *
 * 实现原理：
 *   - 子进程通过检测父进程 PID 是否存在来判断父进程存活
 *   - 使用 waitpid() 或 /proc/[pid] 检测
 *   - 父进程死亡后，通过 am 命令或 socket 尝试唤醒
 *
 * 注意事项：
 *   - Android 5.0+ SELinux 限制了很多操作
 *   - Android 8.0+ 后台启动限制更严格
 *   - 某些厂商 ROM 有专门的守护进程检测
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>
#include <android/log.h>
#include <cstdlib>
#include <cstring>

#define LOG_TAG "FwNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 守护进程配置
struct DaemonConfig {
    char package_name[256];      // 包名
    char service_name[512];      // 服务完整类名
    int check_interval_ms;       // 检查间隔（毫秒）
    int parent_pid;              // 父进程 PID
    bool use_am_command;         // 是否使用 am 命令启动
    bool use_socket;             // 是否使用 socket 通信
    char socket_path[256];       // socket 文件路径
};

static DaemonConfig g_config;
static volatile bool g_daemon_running = false;

/**
 * 检查进程是否存活
 *
 * 方法1：检查 /proc/[pid] 目录是否存在
 * 方法2：发送信号 0 检测（kill(pid, 0)）
 */
static bool is_process_alive(pid_t pid) {
    if (pid <= 0) return false;

    // 方法1：检查 /proc/[pid] 目录
    char proc_path[64];
    snprintf(proc_path, sizeof(proc_path), "/proc/%d", pid);

    struct stat st;
    if (stat(proc_path, &st) == 0 && S_ISDIR(st.st_mode)) {
        return true;
    }

    // 方法2：发送信号 0
    if (kill(pid, 0) == 0) {
        return true;
    }

    return false;
}

/**
 * 通过 am 命令启动服务
 *
 * 使用 Android 的 am (Activity Manager) 命令行工具
 * 这是最常用的从 Native 层启动 Java 组件的方式
 *
 * 限制：
 * - Android 8.0+ 后台启动服务有限制
 * - 需要使用 startForegroundService
 * - SELinux 可能阻止执行
 */
static bool start_service_via_am(const char* package_name, const char* service_name) {
    LOGI("尝试通过 am 命令启动服务: %s/%s", package_name, service_name);

    // 构建命令
    // am startservice 在 Android 8.0+ 需要 --user 参数
    // 使用 startforegroundservice 更可靠
    char cmd[1024];
    snprintf(cmd, sizeof(cmd),
             "am startservice --user 0 -n %s/%s "
             "--es start_reason native_daemon 2>&1",
             package_name, service_name);

    int ret = system(cmd);

    if (ret == 0) {
        LOGI("am 命令执行成功");
        return true;
    } else {
        LOGW("am 命令执行失败，返回码: %d，尝试 startforegroundservice", ret);

        // 尝试使用 startforegroundservice（Android 8.0+）
        snprintf(cmd, sizeof(cmd),
                 "am start-foreground-service --user 0 -n %s/%s "
                 "--es start_reason native_daemon 2>&1",
                 package_name, service_name);

        ret = system(cmd);

        if (ret == 0) {
            LOGI("am start-foreground-service 执行成功");
            return true;
        } else {
            LOGE("am start-foreground-service 执行失败，返回码: %d", ret);
            return false;
        }
    }
}

/**
 * 通过广播启动
 *
 * 发送自定义广播，由静态广播接收器接收并启动服务
 * 这种方式可能绕过某些后台启动限制
 */
static bool start_via_broadcast(const char* package_name) {
    LOGI("尝试通过广播启动: %s", package_name);

    char cmd[512];
    snprintf(cmd, sizeof(cmd),
             "am broadcast --user 0 -a %s.NATIVE_WAKEUP -p %s 2>&1",
             package_name, package_name);

    int ret = system(cmd);

    if (ret == 0) {
        LOGI("广播发送成功");
        return true;
    } else {
        LOGW("广播发送失败，返回码: %d", ret);
        return false;
    }
}

/**
 * 守护进程主循环
 *
 * 这个函数在 fork() 出的子进程中运行
 * 定期检查父进程（Java 层）是否存活
 * 如果父进程死亡，尝试多种方式拉起
 */
static void daemon_main_loop() {
    LOGI("Native 守护进程启动，监控父进程 PID: %d", g_config.parent_pid);

    // 设置进程名（某些工具可能会检测）
    // prctl(PR_SET_NAME, "fw_daemon", 0, 0, 0);

    // 忽略 SIGPIPE 信号
    signal(SIGPIPE, SIG_IGN);

    int consecutive_failures = 0;
    const int max_consecutive_failures = 3;

    while (g_daemon_running) {
        // 等待指定间隔
        usleep(g_config.check_interval_ms * 1000);

        // 检查父进程是否存活
        if (!is_process_alive(g_config.parent_pid)) {
            LOGW("检测到父进程已死亡（PID: %d），尝试唤醒...", g_config.parent_pid);

            bool success = false;

            // 尝试方式1：am 命令启动服务
            if (g_config.use_am_command) {
                if (start_service_via_am(g_config.package_name, g_config.service_name)) {
                    success = true;
                }
            }

            // 尝试方式2：发送广播
            if (!success) {
                if (start_via_broadcast(g_config.package_name)) {
                    success = true;
                }
            }

            if (success) {
                LOGI("唤醒尝试完成，等待进程重启...");
                consecutive_failures = 0;

                // 等待一段时间让进程启动
                sleep(5);

                // 重新获取父进程 PID（这里需要通过其他方式获取，暂时简化处理）
                // 实际实现中可以通过 socket 或文件通信获取新的 PID
            } else {
                consecutive_failures++;
                LOGE("唤醒失败，连续失败次数: %d", consecutive_failures);

                if (consecutive_failures >= max_consecutive_failures) {
                    LOGE("连续失败次数过多，守护进程退出");
                    break;
                }
            }
        } else {
            // 父进程存活，重置失败计数
            consecutive_failures = 0;
        }
    }

    LOGI("Native 守护进程退出");
}

/**
 * 启动守护进程
 *
 * 使用 fork() 创建子进程
 * 子进程执行守护逻辑，父进程继续执行
 *
 * @return 0 成功，-1 失败
 */
extern "C" int start_daemon(const char* package_name,
                            const char* service_name,
                            int check_interval_ms) {
    LOGI("准备启动 Native 守护进程");

    if (g_daemon_running) {
        LOGW("守护进程已在运行");
        return 0;
    }

    // 保存配置
    strncpy(g_config.package_name, package_name, sizeof(g_config.package_name) - 1);
    strncpy(g_config.service_name, service_name, sizeof(g_config.service_name) - 1);
    g_config.check_interval_ms = check_interval_ms > 0 ? check_interval_ms : 3000;
    g_config.parent_pid = getpid();
    g_config.use_am_command = true;
    g_config.use_socket = false;

    // fork 子进程
    pid_t pid = fork();

    if (pid < 0) {
        // fork 失败
        LOGE("fork 失败: %s", strerror(errno));
        return -1;
    } else if (pid == 0) {
        // 子进程
        LOGI("子进程已创建，PID: %d", getpid());

        // 创建新会话，脱离父进程
        setsid();

        // 关闭标准输入输出
        close(STDIN_FILENO);
        close(STDOUT_FILENO);
        close(STDERR_FILENO);

        // 重定向到 /dev/null
        int fd = open("/dev/null", O_RDWR);
        if (fd >= 0) {
            dup2(fd, STDIN_FILENO);
            dup2(fd, STDOUT_FILENO);
            dup2(fd, STDERR_FILENO);
            if (fd > STDERR_FILENO) close(fd);
        }

        // 设置标志
        g_daemon_running = true;

        // 进入守护循环
        daemon_main_loop();

        // 守护循环退出，子进程也退出
        _exit(0);
    } else {
        // 父进程
        LOGI("守护子进程 PID: %d", pid);

        // 不等待子进程，让子进程独立运行
        // waitpid(pid, NULL, WNOHANG);

        return 0;
    }
}

/**
 * 停止守护进程
 */
extern "C" void stop_daemon() {
    LOGI("请求停止 Native 守护进程");
    g_daemon_running = false;
}

/**
 * 检查守护进程是否在运行
 */
extern "C" bool is_daemon_running() {
    return g_daemon_running;
}
