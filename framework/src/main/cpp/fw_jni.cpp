/**
 * ============================================================================
 * fw_jni.cpp - JNI 接口层
 * ============================================================================
 *
 * 功能简介：
 *   提供 Java 层调用 Native 函数的 JNI 接口，封装了守护进程、进程管理、
 *   Socket 通信等所有 Native 功能的 Java 调用入口。
 *
 * 主要函数：
 *   - startDaemon / stopDaemon / isDaemonRunning: 守护进程管理
 *   - getOomAdj / setOomAdj: OOM adj 值操作
 *   - setProcessPriority / getProcessPriority: 进程优先级操作
 *   - getProcessStatus / getMemoryInfo: 进程和内存信息获取
 *   - checkRoot / getProcessCount: 系统状态检测
 *   - startSocketServer / stopSocketServer / connectSocket / sendHeartbeat: Socket 操作
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "FwNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 外部函数声明
extern "C" {
    // fw_daemon.cpp
    int start_daemon(const char* package_name, const char* service_name, int check_interval_ms);
    void stop_daemon();
    bool is_daemon_running();

    // fw_process.cpp
    int get_oom_adj();
    bool set_oom_adj(int adj);
    bool set_process_priority(int priority);
    int get_process_priority();
    void get_process_status(char* buffer, int buffer_size);
    void get_memory_info(long* total_kb, long* free_kb, long* available_kb);
    bool check_root();
    int get_process_count();

    // fw_socket.cpp
    int create_socket_server(const char* socket_name);
    int connect_socket_server(const char* socket_name);
    bool send_heartbeat(int socket_fd);
    int receive_with_timeout(int socket_fd, char* buffer, int buffer_size, int timeout_ms);
    bool start_socket_server_thread(const char* socket_name);
    void stop_socket_server();
}

// JNI 类路径
#define JNI_CLASS_PATH "com/service/framework/native/FwNative"

/**
 * JNI 方法: startDaemon
 *
 * 启动 Native 守护进程
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_startDaemon(
        JNIEnv* env,
        jobject /* this */,
        jstring packageName,
        jstring serviceName,
        jint checkIntervalMs) {

    const char* pkg = env->GetStringUTFChars(packageName, nullptr);
    const char* svc = env->GetStringUTFChars(serviceName, nullptr);

    LOGI("JNI: startDaemon - 包名=%s, 服务=%s, 间隔=%d", pkg, svc, checkIntervalMs);

    int result = start_daemon(pkg, svc, checkIntervalMs);

    env->ReleaseStringUTFChars(packageName, pkg);
    env->ReleaseStringUTFChars(serviceName, svc);

    return result == 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: stopDaemon
 *
 * 停止 Native 守护进程
 */
extern "C" JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_stopDaemon(
        JNIEnv* /* env */,
        jobject /* this */) {

    LOGI("JNI: stopDaemon");
    stop_daemon();
}

/**
 * JNI 方法: isDaemonRunning
 *
 * 检查守护进程是否在运行
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_isDaemonRunning(
        JNIEnv* /* env */,
        jobject /* this */) {

    return is_daemon_running() ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: getOomAdj
 *
 * 获取当前进程的 OOM adj 值
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_service_framework_native_FwNative_getOomAdj(
        JNIEnv* /* env */,
        jobject /* this */) {

    return get_oom_adj();
}

/**
 * JNI 方法: setOomAdj
 *
 * 尝试设置 OOM adj 值（需要 root 权限）
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_setOomAdj(
        JNIEnv* /* env */,
        jobject /* this */,
        jint adj) {

    return set_oom_adj(adj) ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: setProcessPriority
 *
 * 设置进程优先级
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_setProcessPriority(
        JNIEnv* /* env */,
        jobject /* this */,
        jint priority) {

    return set_process_priority(priority) ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: getProcessPriority
 *
 * 获取进程优先级
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_service_framework_native_FwNative_getProcessPriority(
        JNIEnv* /* env */,
        jobject /* this */) {

    return get_process_priority();
}

/**
 * JNI 方法: getProcessStatus
 *
 * 获取进程状态信息
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_service_framework_native_FwNative_getProcessStatus(
        JNIEnv* env,
        jobject /* this */) {

    char buffer[4096];
    get_process_status(buffer, sizeof(buffer));
    return env->NewStringUTF(buffer);
}

/**
 * JNI 方法: getMemoryInfo
 *
 * 获取系统内存信息
 * 返回数组: [total, free, available]
 */
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_service_framework_native_FwNative_getMemoryInfo(
        JNIEnv* env,
        jobject /* this */) {

    long total_kb = 0, free_kb = 0, available_kb = 0;
    get_memory_info(&total_kb, &free_kb, &available_kb);

    jlongArray result = env->NewLongArray(3);
    if (result == nullptr) {
        return nullptr;
    }

    jlong values[3] = {total_kb, free_kb, available_kb};
    env->SetLongArrayRegion(result, 0, 3, values);

    return result;
}

/**
 * JNI 方法: checkRoot
 *
 * 检查是否有 root 权限
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_checkRoot(
        JNIEnv* /* env */,
        jobject /* this */) {

    return check_root() ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: getProcessCount
 *
 * 获取系统进程数量
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_service_framework_native_FwNative_getProcessCount(
        JNIEnv* /* env */,
        jobject /* this */) {

    return get_process_count();
}

/**
 * JNI 方法: startSocketServer
 *
 * 启动 Socket 服务
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_startSocketServer(
        JNIEnv* env,
        jobject /* this */,
        jstring socketName) {

    const char* name = env->GetStringUTFChars(socketName, nullptr);
    LOGI("JNI: startSocketServer - %s", name);

    bool result = start_socket_server_thread(name);

    env->ReleaseStringUTFChars(socketName, name);

    return result ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI 方法: stopSocketServer
 *
 * 停止 Socket 服务
 */
extern "C" JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_stopSocketServer(
        JNIEnv* /* env */,
        jobject /* this */) {

    LOGI("JNI: stopSocketServer");
    stop_socket_server();
}

/**
 * JNI 方法: connectSocket
 *
 * 连接到 Socket 服务
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_service_framework_native_FwNative_connectSocket(
        JNIEnv* env,
        jobject /* this */,
        jstring socketName) {

    const char* name = env->GetStringUTFChars(socketName, nullptr);
    LOGI("JNI: connectSocket - %s", name);

    int fd = connect_socket_server(name);

    env->ReleaseStringUTFChars(socketName, name);

    return fd;
}

/**
 * JNI 方法: sendHeartbeat
 *
 * 发送心跳
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_service_framework_native_FwNative_sendHeartbeat(
        JNIEnv* /* env */,
        jobject /* this */,
        jint socketFd) {

    return send_heartbeat(socketFd) ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI_OnLoad
 *
 * 库加载时调用
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv 失败");
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad: fw_native 库已加载");
    return JNI_VERSION_1_6;
}

/**
 * JNI_OnUnload
 *
 * 库卸载时调用
 */
JNIEXPORT void JNI_OnUnload(JavaVM* /* vm */, void* /* reserved */) {
    LOGI("JNI_OnUnload: fw_native 库已卸载");

    // 清理资源
    stop_daemon();
    stop_socket_server();
}
