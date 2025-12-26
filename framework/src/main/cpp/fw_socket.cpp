/**
 * ============================================================================
 * fw_socket.cpp - Native 层 Socket 保活通道
 * ============================================================================
 *
 * 功能简介：
 *   实现基于 Unix Domain Socket 的进程间通信和心跳检测机制，
 *   用于主进程和守护进程之间的存活状态监控。
 *
 * 核心机制：
 *   1. 创建本地 Unix Domain Socket
 *   2. 主进程和守护进程通过 socket 通信
 *   3. 心跳检测，断开时触发重连
 *
 * 安全研究要点：
 *   - Unix Domain Socket 比 TCP/IP 更高效
 *   - 可用于进程间通信（IPC）
 *   - 守护进程可以通过 socket 检测主进程存活
 *   - 也可以用于与系统服务保持连接
 *
 * 实现方式：
 *   1. 主进程创建 server socket，守护进程连接
 *   2. 定期发送心跳，检测连接状态
 *   3. 连接断开表示对方可能已死
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/select.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>
#include <cstdlib>
#include <cstring>
#include <pthread.h>

#define LOG_TAG "FwNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 心跳消息
#define HEARTBEAT_MSG "HB"
#define HEARTBEAT_ACK "OK"

// Socket 配置
static int g_server_socket = -1;
static int g_client_socket = -1;
static char g_socket_path[256] = {0};
static volatile bool g_socket_running = false;
static pthread_t g_socket_thread;

// 回调函数指针
typedef void (*on_connection_lost_callback)(void);
static on_connection_lost_callback g_connection_lost_callback = nullptr;

/**
 * 设置 socket 为非阻塞模式
 */
static bool set_nonblocking(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) return false;
    return fcntl(fd, F_SETFL, flags | O_NONBLOCK) != -1;
}

/**
 * 创建 Unix Domain Socket 服务器
 *
 * 使用 abstract namespace（名字以 @ 开头）
 * 这样不需要文件系统权限
 */
extern "C" int create_socket_server(const char* socket_name) {
    if (socket_name == nullptr || strlen(socket_name) == 0) {
        LOGE("无效的 socket 名称");
        return -1;
    }

    // 保存 socket 路径（使用 abstract namespace）
    snprintf(g_socket_path, sizeof(g_socket_path), "\0%s", socket_name);

    // 创建 socket
    g_server_socket = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_server_socket < 0) {
        LOGE("创建 socket 失败: %s", strerror(errno));
        return -1;
    }

    // 设置地址
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    // Abstract namespace: 第一个字节是 \0
    memcpy(addr.sun_path, g_socket_path, strlen(socket_name) + 1);

    // 绑定
    int path_len = offsetof(struct sockaddr_un, sun_path) + strlen(socket_name) + 1;
    if (bind(g_server_socket, (struct sockaddr*)&addr, path_len) < 0) {
        LOGE("绑定 socket 失败: %s", strerror(errno));
        close(g_server_socket);
        g_server_socket = -1;
        return -1;
    }

    // 监听
    if (listen(g_server_socket, 5) < 0) {
        LOGE("监听 socket 失败: %s", strerror(errno));
        close(g_server_socket);
        g_server_socket = -1;
        return -1;
    }

    LOGI("Socket 服务器创建成功: %s (fd=%d)", socket_name, g_server_socket);
    return g_server_socket;
}

/**
 * 连接到 Socket 服务器
 */
extern "C" int connect_socket_server(const char* socket_name) {
    if (socket_name == nullptr || strlen(socket_name) == 0) {
        LOGE("无效的 socket 名称");
        return -1;
    }

    // 创建 socket
    g_client_socket = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_client_socket < 0) {
        LOGE("创建 socket 失败: %s", strerror(errno));
        return -1;
    }

    // 设置地址
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    // Abstract namespace
    addr.sun_path[0] = '\0';
    strcpy(addr.sun_path + 1, socket_name);

    // 连接
    int path_len = offsetof(struct sockaddr_un, sun_path) + strlen(socket_name) + 1;
    if (connect(g_client_socket, (struct sockaddr*)&addr, path_len) < 0) {
        LOGE("连接 socket 失败: %s", strerror(errno));
        close(g_client_socket);
        g_client_socket = -1;
        return -1;
    }

    LOGI("Socket 连接成功: %s (fd=%d)", socket_name, g_client_socket);
    return g_client_socket;
}

/**
 * 发送心跳
 */
extern "C" bool send_heartbeat(int socket_fd) {
    if (socket_fd < 0) return false;

    ssize_t sent = send(socket_fd, HEARTBEAT_MSG, strlen(HEARTBEAT_MSG), MSG_NOSIGNAL);
    if (sent <= 0) {
        LOGW("发送心跳失败: %s", strerror(errno));
        return false;
    }

    LOGD("心跳已发送");
    return true;
}

/**
 * 接收数据（带超时）
 */
extern "C" int receive_with_timeout(int socket_fd, char* buffer, int buffer_size, int timeout_ms) {
    if (socket_fd < 0 || buffer == nullptr || buffer_size <= 0) return -1;

    fd_set read_fds;
    FD_ZERO(&read_fds);
    FD_SET(socket_fd, &read_fds);

    struct timeval tv;
    tv.tv_sec = timeout_ms / 1000;
    tv.tv_usec = (timeout_ms % 1000) * 1000;

    int ret = select(socket_fd + 1, &read_fds, nullptr, nullptr, &tv);
    if (ret < 0) {
        LOGE("select 失败: %s", strerror(errno));
        return -1;
    } else if (ret == 0) {
        // 超时
        return 0;
    }

    ssize_t received = recv(socket_fd, buffer, buffer_size - 1, 0);
    if (received <= 0) {
        if (received == 0) {
            LOGW("对方关闭了连接");
        } else {
            LOGW("接收数据失败: %s", strerror(errno));
        }
        return -1;
    }

    buffer[received] = '\0';
    LOGD("收到数据: %s", buffer);
    return (int)received;
}

/**
 * Socket 服务线程
 *
 * 接受连接并处理心跳
 */
static void* socket_server_thread(void* arg) {
    (void)arg;
    LOGI("Socket 服务线程启动");

    while (g_socket_running && g_server_socket >= 0) {
        // 接受连接
        struct sockaddr_un client_addr;
        socklen_t client_len = sizeof(client_addr);

        // 使用 select 实现超时
        fd_set read_fds;
        FD_ZERO(&read_fds);
        FD_SET(g_server_socket, &read_fds);

        struct timeval tv;
        tv.tv_sec = 1;
        tv.tv_usec = 0;

        int ret = select(g_server_socket + 1, &read_fds, nullptr, nullptr, &tv);
        if (ret <= 0) {
            continue;
        }

        int client_fd = accept(g_server_socket, (struct sockaddr*)&client_addr, &client_len);
        if (client_fd < 0) {
            if (errno != EAGAIN && errno != EWOULDBLOCK) {
                LOGW("接受连接失败: %s", strerror(errno));
            }
            continue;
        }

        LOGI("新客户端连接: fd=%d", client_fd);

        // 处理心跳
        char buffer[64];
        while (g_socket_running) {
            int received = receive_with_timeout(client_fd, buffer, sizeof(buffer), 5000);
            if (received < 0) {
                LOGW("客户端断开连接");
                break;
            } else if (received > 0) {
                // 收到心跳，回复
                send(client_fd, HEARTBEAT_ACK, strlen(HEARTBEAT_ACK), MSG_NOSIGNAL);
            }
        }

        close(client_fd);
    }

    LOGI("Socket 服务线程退出");
    return nullptr;
}

/**
 * 启动 Socket 服务（在后台线程）
 */
extern "C" bool start_socket_server_thread(const char* socket_name) {
    if (g_socket_running) {
        LOGW("Socket 服务已在运行");
        return true;
    }

    if (create_socket_server(socket_name) < 0) {
        return false;
    }

    g_socket_running = true;

    int ret = pthread_create(&g_socket_thread, nullptr, socket_server_thread, nullptr);
    if (ret != 0) {
        LOGE("创建 socket 线程失败: %s", strerror(ret));
        g_socket_running = false;
        close(g_server_socket);
        g_server_socket = -1;
        return false;
    }

    LOGI("Socket 服务线程已启动");
    return true;
}

/**
 * 停止 Socket 服务
 */
extern "C" void stop_socket_server() {
    LOGI("停止 Socket 服务");

    g_socket_running = false;

    if (g_server_socket >= 0) {
        close(g_server_socket);
        g_server_socket = -1;
    }

    if (g_client_socket >= 0) {
        close(g_client_socket);
        g_client_socket = -1;
    }

    // 等待线程结束
    pthread_join(g_socket_thread, nullptr);

    LOGI("Socket 服务已停止");
}

/**
 * 设置连接丢失回调
 */
extern "C" void set_connection_lost_callback(on_connection_lost_callback callback) {
    g_connection_lost_callback = callback;
}

/**
 * 心跳检测客户端
 *
 * 连接到服务器并定期发送心跳
 * 如果心跳失败，调用回调
 */
extern "C" bool start_heartbeat_client(const char* socket_name, int interval_ms) {
    LOGI("启动心跳客户端: %s, 间隔: %d ms", socket_name, interval_ms);

    if (connect_socket_server(socket_name) < 0) {
        return false;
    }

    // 简化实现：同步发送心跳
    // 实际应用中应该在单独线程中运行
    while (g_client_socket >= 0) {
        if (!send_heartbeat(g_client_socket)) {
            LOGW("心跳发送失败，连接可能已断开");
            if (g_connection_lost_callback != nullptr) {
                g_connection_lost_callback();
            }
            break;
        }

        // 等待响应
        char buffer[64];
        int received = receive_with_timeout(g_client_socket, buffer, sizeof(buffer), interval_ms);
        if (received < 0) {
            LOGW("未收到心跳响应，连接可能已断开");
            if (g_connection_lost_callback != nullptr) {
                g_connection_lost_callback();
            }
            break;
        }

        // 等待下一次心跳
        usleep(interval_ms * 1000);
    }

    return true;
}
