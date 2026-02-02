/**
 * ============================================================================
 * fw_mediaroute_jni.cpp - MediaRoute 模块 JNI 接口
 * ============================================================================
 *
 * 功能简介：
 *   MediaRoute 保活模块的 Native 层实现，提供：
 *   - WakeLock 状态检查
 *   - 服务状态监控
 *   - 心跳检测
 *
 * 核心机制：
 *   - 通过 JNI 与 Java 层交互
 *   - 记录服务启停状态
 *   - 提供底层心跳机制
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.2.0
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <ctime>
#include <atomic>

// 日志标签
#define TAG "FwMediaRouteNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 命名空间
namespace fw {
namespace mediaroute {

// 服务状态
struct ServiceState {
    std::atomic<bool> isService1Running{false};   // MediaRouteProviderService 运行状态
    std::atomic<bool> isService2Running{false};   // MediaRoute2ProviderService 运行状态
    std::atomic<long> lastHeartbeatTime{0};       // 上次心跳时间戳
    std::atomic<int> heartbeatCount{0};           // 心跳计数
    std::string packageName;                       // 应用包名
    std::string service1Name;                      // 服务1类名
    std::string service2Name;                      // 服务2类名
};

// 全局服务状态
static ServiceState g_serviceState;

// 是否已初始化
static std::atomic<bool> g_initialized{false};

/**
 * 获取当前时间戳（毫秒）
 */
static long getCurrentTimeMs() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000L + ts.tv_nsec / 1000000L;
}

/**
 * 初始化
 */
void init() {
    if (g_initialized.exchange(true)) {
        LOGD("Already initialized, skip");
        return;
    }

    g_serviceState.isService1Running = false;
    g_serviceState.isService2Running = false;
    g_serviceState.lastHeartbeatTime = getCurrentTimeMs();
    g_serviceState.heartbeatCount = 0;

    LOGI("MediaRoute Native module initialized");
}

/**
 * WakeLock 检查
 *
 * 在 Native 层执行 WakeLock 相关的检查逻辑
 */
void checkWakeLock() {
    LOGD("Checking WakeLock status");

    // 更新心跳时间
    g_serviceState.lastHeartbeatTime = getCurrentTimeMs();
    g_serviceState.heartbeatCount++;

    LOGD("WakeLock check completed, heartbeat count: %d",
         g_serviceState.heartbeatCount.load());
}

/**
 * 服务1启动通知
 */
void onServiceStarted(const std::string& packageName, const std::string& serviceName) {
    LOGI("Service1 started: %s", serviceName.c_str());

    g_serviceState.isService1Running = true;
    g_serviceState.packageName = packageName;
    g_serviceState.service1Name = serviceName;
    g_serviceState.lastHeartbeatTime = getCurrentTimeMs();
}

/**
 * 服务1停止通知
 */
void onServiceStopped() {
    LOGW("Service1 stopped");
    g_serviceState.isService1Running = false;
}

/**
 * 服务2启动通知
 */
void onService2Started(const std::string& packageName, const std::string& serviceName) {
    LOGI("Service2 started: %s", serviceName.c_str());

    g_serviceState.isService2Running = true;
    g_serviceState.packageName = packageName;
    g_serviceState.service2Name = serviceName;
    g_serviceState.lastHeartbeatTime = getCurrentTimeMs();
}

/**
 * 服务2停止通知
 */
void onService2Stopped() {
    LOGW("Service2 stopped");
    g_serviceState.isService2Running = false;
}

/**
 * 执行心跳
 *
 * @return 心跳是否成功
 */
bool performHeartbeat() {
    long now = getCurrentTimeMs();
    long lastHeartbeat = g_serviceState.lastHeartbeatTime.load();
    long elapsed = now - lastHeartbeat;

    LOGD("Performing heartbeat, elapsed since last: %ld ms", elapsed);

    // 更新心跳时间
    g_serviceState.lastHeartbeatTime = now;
    g_serviceState.heartbeatCount++;

    // 检查服务状态
    bool service1OK = g_serviceState.isService1Running.load();
    bool service2OK = g_serviceState.isService2Running.load();

    if (!service1OK && !service2OK) {
        LOGW("Both services are not running!");
        return false;
    }

    LOGD("Heartbeat OK, count: %d, service1: %d, service2: %d",
         g_serviceState.heartbeatCount.load(),
         service1OK ? 1 : 0,
         service2OK ? 1 : 0);

    return true;
}

/**
 * 获取服务状态
 *
 * @return 状态码：0=正常, 1=警告, 2=异常
 */
int getServiceStatus() {
    bool service1OK = g_serviceState.isService1Running.load();
    bool service2OK = g_serviceState.isService2Running.load();

    if (service1OK && service2OK) {
        return 0;  // 正常
    } else if (service1OK || service2OK) {
        return 1;  // 警告（只有一个服务在运行）
    } else {
        return 2;  // 异常（没有服务在运行）
    }
}

} // namespace mediaroute
} // namespace fw

// ==================== JNI 方法实现 ====================

extern "C" {

/**
 * Native 初始化
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeInit(
        JNIEnv *env,
        jclass clazz) {
    fw::mediaroute::init();
}

/**
 * WakeLock 检查
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeCheckWakeLock(
        JNIEnv *env,
        jclass clazz) {
    fw::mediaroute::checkWakeLock();
}

/**
 * 服务启动通知
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeOnServiceStarted(
        JNIEnv *env,
        jclass clazz,
        jstring packageName,
        jstring serviceName) {
    const char *pkgName = env->GetStringUTFChars(packageName, nullptr);
    const char *svcName = env->GetStringUTFChars(serviceName, nullptr);

    fw::mediaroute::onServiceStarted(std::string(pkgName), std::string(svcName));

    env->ReleaseStringUTFChars(packageName, pkgName);
    env->ReleaseStringUTFChars(serviceName, svcName);
}

/**
 * 服务停止通知
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeOnServiceStopped(
        JNIEnv *env,
        jclass clazz) {
    fw::mediaroute::onServiceStopped();
}

/**
 * MediaRoute2 服务启动通知
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeOnService2Started(
        JNIEnv *env,
        jclass clazz,
        jstring packageName,
        jstring serviceName) {
    const char *pkgName = env->GetStringUTFChars(packageName, nullptr);
    const char *svcName = env->GetStringUTFChars(serviceName, nullptr);

    fw::mediaroute::onService2Started(std::string(pkgName), std::string(svcName));

    env->ReleaseStringUTFChars(packageName, pkgName);
    env->ReleaseStringUTFChars(serviceName, svcName);
}

/**
 * MediaRoute2 服务停止通知
 */
JNIEXPORT void JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeOnService2Stopped(
        JNIEnv *env,
        jclass clazz) {
    fw::mediaroute::onService2Stopped();
}

/**
 * 心跳检测
 */
JNIEXPORT jboolean JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativePerformHeartbeat(
        JNIEnv *env,
        jclass clazz) {
    return fw::mediaroute::performHeartbeat() ? JNI_TRUE : JNI_FALSE;
}

/**
 * 获取服务状态
 */
JNIEXPORT jint JNICALL
Java_com_service_framework_mediaroute_FwMediaRouteNative_nativeGetServiceStatus(
        JNIEnv *env,
        jclass clazz) {
    return fw::mediaroute::getServiceStatus();
}

} // extern "C"
