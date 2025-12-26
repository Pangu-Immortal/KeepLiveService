/**
 * ============================================================================
 * fw_force_stop.cpp - 无法强制停止策略 Native 实现
 * ============================================================================
 *
 * 功能简介：
 *   通过 C++ 层直接操作 Binder 驱动实现"无法强制停止"功能。
 *   核心原理是利用文件锁(flock)监控进程死亡，检测到后立即通过
 *   直接 Binder transact 调用 AMS 启动服务，速度极快。
 *
 * 教学说明（历史背景）：
 *   这是 Android 5.0 - 9.0 时代的保活技术复现，展示了早期 Android
 *   系统的漏洞：
 *   1. 应用可以直接打开 /dev/binder 设备
 *   2. 可以通过 ioctl 直接与 Binder 驱动通信
 *   3. 强制停止进程是逐个杀死，存在时间窗口
 *
 *   Android 10+ 已封堵：
 *   1. 强制停止改为 cgroup 进程组整体杀死
 *   2. SELinux 限制了 Binder 设备的直接访问
 *   3. 后台进程的 Binder 调用受限
 *
 * 核心机制：
 *   1. 多进程文件锁监控 - 使用 flock() 互相监控进程存活
 *   2. AMS Binder 直接调用 - 跳过 Java 层，直接构造 Parcel 调用
 *   3. fork() 创建守护进程 - 与主进程互相守护
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#include <jni.h>
#include <sys/wait.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/file.h>
#include <linux/android/binder.h>
#include <sys/mman.h>
#include <string.h>
#include "binder/data_transact.h"
#include "binder/cParcel.h"

using namespace android;

// 日志标签
#define LOG_TAG "FwForceStop"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// ==================== Intent 构造函数 ====================

/**
 * 构造 Intent 数据写入 Parcel
 *
 * Intent 的 Parcel 格式（简化版，仅包含 ComponentName）：
 * - mAction: String16 (null)
 * - mData: int32 (0 = null URI)
 * - mType: String16 (null)
 * - mIdentifier: String16 (null)
 * - mFlags: int32 (0)
 * - mPackage: String16 (null)
 * - mComponent: String16 (packageName) + String16 (className)
 * - mSourceBounds: int32 (0 = null)
 * - mCategories: int32 (0 = null set)
 * - mSelector: int32 (0 = null)
 * - mClipData: int32 (0 = null)
 * - mContentUserHint: int32 (-2 = UserHandle.USER_CURRENT)
 * - mExtras: int32 (-1 = null Bundle)
 */
static void writeIntent(Parcel &out, const char *packageName, const char *className) {
    out.writeString16(NULL, 0);             // mAction = null
    out.writeInt32(0);                      // mData (URI) = null
    out.writeString16(NULL, 0);             // mType = null
    out.writeString16(NULL, 0);             // mIdentifier = null (API 29+)
    out.writeInt32(0);                      // mFlags = 0
    out.writeString16(NULL, 0);             // mPackage = null
    out.writeString16(String16(packageName)); // mComponent.packageName
    out.writeString16(String16(className));   // mComponent.className
    out.writeInt32(0);                      // mSourceBounds = null
    out.writeInt32(0);                      // mCategories = null (empty set)
    out.writeInt32(0);                      // mSelector = null
    out.writeInt32(0);                      // mClipData = null
    out.writeInt32(-2);                     // mContentUserHint = USER_CURRENT
    out.writeInt32(-1);                     // mExtras = null Bundle
}

/**
 * 构造 startService 调用的 Parcel 数据
 *
 * 不同 Android 版本的 startService 接口略有不同：
 * - API 26+: 增加了 requireForeground 参数
 * - API 23+: 增加了 callingPackage 参数
 *
 * @param out 输出 Parcel
 * @param packageName 包名
 * @param className 服务类名
 * @param sdk_version SDK 版本号
 */
static void writeStartServiceParcel(Parcel &out, const char *packageName,
                                    const char *className, int sdk_version) {
    // 写入接口描述符
    out.writeInterfaceToken(String16("android.app.IActivityManager"));

    // 写入 caller IBinder (null)
    out.writeNullBinder();

    if (sdk_version >= 26) {
        // Android 8.0+ (API 26+)
        out.writeInt32(1);                          // Intent 标记
        writeIntent(out, packageName, className);   // Intent 数据
        out.writeString16(NULL, 0);                 // resolvedType = null
        out.writeInt32(0);                          // requireForeground = false
        out.writeString16(String16(packageName));   // callingPackage
        out.writeInt32(0);                          // userId = 0 (当前用户)
    } else if (sdk_version >= 23) {
        // Android 6.0-7.x (API 23-25)
        out.writeInt32(1);                          // Intent 标记
        writeIntent(out, packageName, className);
        out.writeString16(NULL, 0);                 // resolvedType
        out.writeString16(String16(packageName));   // callingPackage
        out.writeInt32(0);                          // userId
    } else {
        // Android 5.x (API 21-22)
        out.writeInt32(1);                          // Intent 标记
        writeIntent(out, packageName, className);
        out.writeString16(NULL, 0);                 // resolvedType
        out.writeInt32(0);                          // userId
    }
}

// ==================== Binder 服务获取 ====================

#define CHECK_SERVICE_TRANSACTION 1  // ServiceManager 的 checkService 事务码

/**
 * 通过 ServiceManager 获取指定服务的 Binder handle
 *
 * 原理：
 * 1. ServiceManager 的 Binder handle 固定为 0
 * 2. 通过 CHECK_SERVICE_TRANSACTION 查询服务
 * 3. 返回的 flat_binder_object 包含目标服务的 handle
 *
 * @param serviceName 服务名（如 "activity"）
 * @param driverFD Binder 驱动文件描述符
 * @return 服务的 Binder handle，失败返回 0
 */
static uint32_t getServiceHandle(const char *serviceName, int driverFD) {
    Parcel *data = new Parcel;
    Parcel *reply = new Parcel;

    // 构造查询数据
    data->writeInterfaceToken(String16("android.os.IServiceManager"));
    data->writeString16(String16(serviceName));

    // 调用 ServiceManager (handle=0) 的 checkService
    status_t status = write_transact(0, CHECK_SERVICE_TRANSACTION, *data, reply, 0, driverFD);

    // 从返回数据中读取服务的 Binder 对象
    const flat_binder_object *flat = reply->readObject(false);
    uint32_t handle = 0;
    if (flat) {
        handle = flat->handle;
        LOGD("获取服务 [%s] handle = %u", serviceName, handle);
    } else {
        LOGE("获取服务 [%s] 失败", serviceName);
    }

    delete data;
    delete reply;
    return handle;
}

// ==================== 文件锁操作 ====================

/**
 * 创建文件（如果不存在）
 */
static void createFileIfNotExist(const char *path) {
    FILE *fp = fopen(path, "ab+");
    if (fp) {
        fclose(fp);
    }
}

/**
 * 尝试获取文件的排他锁
 *
 * 使用 flock() 系统调用获取文件锁：
 * - LOCK_EX: 排他锁（独占）
 * - LOCK_NB: 非阻塞（立即返回）
 *
 * @param lockFilePath 锁文件路径
 * @return 成功返回 1，失败返回 0
 */
static int lockFile(const char *lockFilePath) {
    LOGD("尝试锁定文件: %s", lockFilePath);

    // 打开文件
    int fd = open(lockFilePath, O_RDONLY | O_LARGEFILE);
    if (fd == -1) {
        fd = open(lockFilePath, O_CREAT, S_IRUSR | S_IWUSR);
    }

    if (fd == -1) {
        LOGE("无法打开锁文件: %s", lockFilePath);
        return 0;
    }

    // 尝试获取排他锁（非阻塞）
    int result = flock(fd, LOCK_EX | LOCK_NB);
    if (result == -1) {
        LOGE("锁定文件失败: %s", lockFilePath);
        return 0;
    }

    LOGD("锁定文件成功: %s (fd=%d)", lockFilePath, fd);
    return 1;
}

/**
 * 等待文件锁被释放
 *
 * 阻塞式等待：当持有锁的进程死亡时，锁会自动释放
 * 这是检测进程死亡的核心机制
 *
 * @param lockFilePath 锁文件路径
 * @return 成功获取锁返回 true
 */
static bool waitForFileLock(const char *lockFilePath) {
    int fd = open(lockFilePath, O_RDONLY | O_LARGEFILE);
    if (fd == -1) {
        fd = open(lockFilePath, O_CREAT, S_IRUSR | S_IWUSR);
    }

    // 先尝试非阻塞获取，如果成功说明对方进程已死
    while (flock(fd, LOCK_EX | LOCK_NB) != -1) {
        usleep(1000);  // 1ms
    }

    // 阻塞等待锁
    int result = flock(fd, LOCK_EX);
    LOGD("等待文件锁完成: %s, result=%d", lockFilePath, result);

    return result != -1;
}

/**
 * 通知并等待对方进程就绪
 *
 * 使用观察者文件进行进程间同步：
 * 1. 创建自己的观察者文件
 * 2. 等待对方的观察者文件出现
 * 3. 删除对方的观察者文件（表示已收到）
 */
static void notifyAndWaitFor(const char *observerSelfPath, const char *observerDaemonPath) {
    // 创建自己的观察者文件
    int fd = open(observerSelfPath, O_RDONLY | O_LARGEFILE);
    if (fd == -1) {
        fd = open(observerSelfPath, O_CREAT, S_IRUSR | S_IWUSR);
    }

    // 等待对方的观察者文件
    while (open(observerDaemonPath, O_RDONLY | O_LARGEFILE) == -1) {
        usleep(1000);  // 1ms
    }

    // 删除对方的文件，表示同步完成
    remove(observerDaemonPath);
    LOGI("进程同步完成");
}

// ==================== 守护进程主逻辑 ====================

/**
 * 获取不同 Android 版本的 startService 事务码
 *
 * AMS 的 Binder 接口在不同版本有变化：
 * - API 26-27: TRANSACTION_startService = 26
 * - API 28: TRANSACTION_startService = 30
 * - API 29: TRANSACTION_startService = 24
 * - 其他: 34 (默认)
 */
static uint32_t getStartServiceTransactionCode(int sdkVersion) {
    switch (sdkVersion) {
        case 26:
        case 27:
            return 26;
        case 28:
            return 30;
        case 29:
            return 24;
        default:
            return 34;
    }
}

/**
 * 执行守护进程的核心逻辑
 *
 * 工作流程：
 * 1. 锁定自己的指示器文件
 * 2. 等待对方进程就绪
 * 3. 打开 Binder 驱动，获取 AMS handle
 * 4. 构造 startService 调用数据
 * 5. 阻塞等待对方的锁文件（检测进程死亡）
 * 6. 检测到死亡后，立即通过 Binder 拉活服务
 */
static void doDaemon(const char *indicatorSelfPath,
                     const char *indicatorDaemonPath,
                     const char *observerSelfPath,
                     const char *observerDaemonPath,
                     const char *packageName,
                     const char *serviceName,
                     int sdkVersion,
                     uint32_t transactCode) {

    // 1. 锁定自己的指示器文件
    int lockStatus = 0;
    int tryCount = 0;
    while (tryCount < 5 && !(lockStatus = lockFile(indicatorSelfPath))) {
        tryCount++;
        LOGD("锁定失败，重试第 %d 次", tryCount);
        usleep(10000);  // 10ms
    }

    if (!lockStatus) {
        LOGE("无法锁定指示器文件，退出");
        return;
    }

    // 2. 与对方进程同步
    notifyAndWaitFor(observerSelfPath, observerDaemonPath);

    // 3. 打开 Binder 驱动
    int driverFD = open_driver();
    void *vmStart = MAP_FAILED;
    initProcessState(driverFD, vmStart);

    // 4. 获取 AMS 的 Binder handle
    uint32_t amsHandle = getServiceHandle("activity", driverFD);

    // 5. 预先构造 startService 调用数据
    Parcel *data = new Parcel;
    writeStartServiceParcel(*data, packageName, serviceName, sdkVersion);

    // 6. 等待对方进程死亡（阻塞在 flock）
    LOGI("开始监控对方进程...");
    lockStatus = lockFile(indicatorDaemonPath);

    if (lockStatus) {
        // 检测到对方进程死亡！
        LOGW("检测到守护进程死亡，立即拉活！");

        // 7. 通过 Binder 直接调用 AMS.startService
        status_t status = write_transact(amsHandle, transactCode, *data, NULL, 1, driverFD);
        LOGD("startService 调用结果: %d", status);

        // 清理观察者文件，防止死锁
        remove(observerSelfPath);

        // 自杀，让对方守护进程重新启动自己
        int pid = getpid();
        if (pid > 0) {
            killpg(pid, SIGTERM);
        }
    }

    delete data;
}

// ==================== JNI 接口 ====================

extern "C" {

/**
 * 设置进程名
 */
static void setProcessName(JNIEnv *env, const char *name) {
    jclass processClass = env->FindClass("android/os/Process");
    jmethodID setArgV0 = env->GetStaticMethodID(processClass, "setArgV0", "(Ljava/lang/String;)V");
    jstring jname = env->NewStringUTF(name);
    env->CallStaticVoidMethod(processClass, setArgV0, jname);
}

/**
 * JNI 方法: 锁定文件
 */
JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_lockFile(
        JNIEnv *env, jobject /* this */, jstring lockFilePath) {
    const char *path = env->GetStringUTFChars(lockFilePath, 0);
    lockFile(path);
    env->ReleaseStringUTFChars(lockFilePath, path);
}

/**
 * JNI 方法: 设置会话 ID（脱离父进程）
 */
JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_nativeSetSid(JNIEnv *env, jobject /* this */) {
    setsid();
}

/**
 * JNI 方法: 等待文件锁
 */
JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_waitFileLock(
        JNIEnv *env, jobject /* this */, jstring lockFilePath) {
    const char *path = env->GetStringUTFChars(lockFilePath, 0);
    LOGD("waitFileLock: %s", path);
    waitForFileLock(path);
    env->ReleaseStringUTFChars(lockFilePath, path);
}

/**
 * JNI 方法: 启动守护进程
 *
 * 核心入口，通过 fork() 创建子进程：
 * 1. 第一次 fork: 创建子进程
 * 2. 第二次 fork: 托孤（让孙进程成为孤儿进程，由 init 收养）
 * 3. 父子进程各自运行 doDaemon，互相监控
 *
 * @param indicatorSelfPath 自己的指示器文件路径
 * @param indicatorDaemonPath 对方的指示器文件路径
 * @param observerSelfPath 自己的观察者文件路径
 * @param observerDaemonPath 对方的观察者文件路径
 * @param packageName 包名
 * @param serviceName 服务类全名
 * @param sdkVersion SDK 版本号
 */
JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_startForceStopDaemon(
        JNIEnv *env,
        jobject /* this */,
        jstring indicatorSelfPath,
        jstring indicatorDaemonPath,
        jstring observerSelfPath,
        jstring observerDaemonPath,
        jstring packageName,
        jstring serviceName,
        jint sdkVersion) {

    if (indicatorSelfPath == NULL || indicatorDaemonPath == NULL ||
        observerSelfPath == NULL || observerDaemonPath == NULL) {
        LOGE("参数不能为 NULL");
        return;
    }

    // 获取事务码
    uint32_t transactCode = getStartServiceTransactionCode(sdkVersion);

    // 转换 Java 字符串
    const char *indicatorSelf = env->GetStringUTFChars(indicatorSelfPath, 0);
    const char *indicatorDaemon = env->GetStringUTFChars(indicatorDaemonPath, 0);
    const char *observerSelf = env->GetStringUTFChars(observerSelfPath, 0);
    const char *observerDaemon = env->GetStringUTFChars(observerDaemonPath, 0);
    const char *pkgName = env->GetStringUTFChars(packageName, 0);
    const char *svcName = env->GetStringUTFChars(serviceName, 0);

    LOGI("启动无法强制停止守护进程");
    LOGD("indicatorSelf: %s", indicatorSelf);
    LOGD("indicatorDaemon: %s", indicatorDaemon);
    LOGD("packageName: %s, serviceName: %s", pkgName, svcName);

    // ===== 第一次 fork =====
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork 失败");
        return;
    } else if (pid == 0) {
        // ===== 子进程 =====

        // ===== 第二次 fork（托孤） =====
        pid_t pid2 = fork();
        if (pid2 < 0) {
            LOGE("第二次 fork 失败");
            exit(-1);
        } else if (pid2 > 0) {
            // 中间进程立即退出，让孙进程被 init 收养
            exit(0);
        }

        // ===== 孙进程（守护进程） =====
        LOGD("守护进程启动, pid=%d", getpid());

        // 构造子进程专用的文件路径
        const int MAX_PATH = 256;
        char indicatorSelfChild[MAX_PATH];
        char indicatorDaemonChild[MAX_PATH];
        char observerSelfChild[MAX_PATH];
        char observerDaemonChild[MAX_PATH];

        snprintf(indicatorSelfChild, MAX_PATH, "%s-c", indicatorSelf);
        snprintf(indicatorDaemonChild, MAX_PATH, "%s-c", indicatorDaemon);
        snprintf(observerSelfChild, MAX_PATH, "%s-c", observerSelf);
        snprintf(observerDaemonChild, MAX_PATH, "%s-c", observerDaemon);

        // 创建锁文件
        createFileIfNotExist(indicatorSelfChild);
        createFileIfNotExist(indicatorDaemonChild);

        // 设置进程名
        setProcessName(env, "fw_daemon");

        // 执行守护逻辑
        doDaemon(indicatorSelfChild, indicatorDaemonChild,
                 observerSelfChild, observerDaemonChild,
                 pkgName, svcName, sdkVersion, transactCode);
    }

    // ===== 父进程 =====

    // 等待中间子进程退出
    if (waitpid(pid, NULL, 0) != pid) {
        LOGE("waitpid 失败");
    }

    LOGD("主进程继续执行守护逻辑, pid=%d", getpid());

    // 父进程也执行守护逻辑
    doDaemon(indicatorSelf, indicatorDaemon,
             observerSelf, observerDaemon,
             pkgName, svcName, sdkVersion, transactCode);

    // 释放字符串
    env->ReleaseStringUTFChars(indicatorSelfPath, indicatorSelf);
    env->ReleaseStringUTFChars(indicatorDaemonPath, indicatorDaemon);
    env->ReleaseStringUTFChars(observerSelfPath, observerSelf);
    env->ReleaseStringUTFChars(observerDaemonPath, observerDaemon);
    env->ReleaseStringUTFChars(packageName, pkgName);
    env->ReleaseStringUTFChars(serviceName, svcName);
}

/**
 * JNI 方法: 测试 Binder 直接调用
 *
 * 仅用于测试 Binder 驱动访问是否正常
 */
JNIEXPORT void JNICALL
Java_com_service_framework_native_FwNative_testBinderCall(
        JNIEnv *env,
        jobject /* this */,
        jstring packageName,
        jstring serviceName,
        jint sdkVersion) {

    // 打开 Binder 驱动
    int driverFD = open_driver();
    void *vmStart = MAP_FAILED;
    initProcessState(driverFD, vmStart);

    // 获取 AMS handle
    uint32_t amsHandle = getServiceHandle("activity", driverFD);
    LOGI("AMS handle = %u", amsHandle);

    // 构造并发送 startService
    const char *pkgName = env->GetStringUTFChars(packageName, 0);
    const char *svcName = env->GetStringUTFChars(serviceName, 0);

    Parcel *data = new Parcel;
    writeStartServiceParcel(*data, pkgName, svcName, sdkVersion);

    uint32_t transactCode = getStartServiceTransactionCode(sdkVersion);
    status_t status = write_transact(amsHandle, transactCode, *data, NULL, 1, driverFD);
    LOGI("测试调用结果: %d", status);

    delete data;
    unInitProcessState(driverFD, vmStart);

    env->ReleaseStringUTFChars(packageName, pkgName);
    env->ReleaseStringUTFChars(serviceName, svcName);
}

} // extern "C"
