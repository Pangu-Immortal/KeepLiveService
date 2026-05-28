//! ============================================================================
//! fw_rust - Rust Native 骨架库
//! ============================================================================
//!
//! 功能简介：
//!   1. 提供 Rust JNI 动态注册入口，验证 Android Rust so 构建链路。
//!   2. 暂不承载 C++ 业务迁移，避免影响现有 fw_native 行为。
//!   3. 后续可逐步迁移 JNI 外层、字符串保护和高适配度 Native 模块。
//!
//! 函数简介：
//!   - JNI_OnLoad：加载 so 后注册 Kotlin 内部探测方法。
//!   - native_is_available：返回 Rust Native 骨架可用状态。
//!   - native_version_code：返回 Rust Native 骨架版本号。
//!   - native_get_oom_adj：读取当前进程 OOM adj。
//!   - native_get_memory_info：读取系统内存信息。
//!   - native_get_process_status：读取当前进程关键状态。
//!   - native_check_root：检测 root / Magisk 痕迹。
//!   - native_get_process_count：统计 /proc 中的进程数量。
//!   - MediaRoute native 系列函数：迁移 MediaRoute 服务状态与心跳逻辑。
//! ============================================================================

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong, jlongArray, jstring, JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM, NativeMethod};
use std::ffi::c_void;
use std::fs;
use std::path::Path;
use std::ptr;
use std::sync::atomic::{AtomicBool, AtomicI32, AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};
use std::time::Instant;

const FW_RUST_VERSION_CODE: jint = 1;
const FW_RUST_NATIVE_CLASS: &str = "com/service/framework/rust/FwRustNative";
const FW_RUST_MEDIA_ROUTE_CLASS: &str = "com/service/framework/rust/FwRustMediaRouteNative";
const DEFAULT_OOM_SCORE_ADJ: jint = 1000;

/// MediaRoute 服务名称状态。
#[derive(Default)]
struct MediaRouteNames {
    package_name: String,
    service1_name: String,
    service2_name: String,
}

/// Rust 版 MediaRoute 服务状态。
struct MediaRouteState {
    service1_running: AtomicBool,
    service2_running: AtomicBool,
    initialized: AtomicBool,
    boot_time: Instant,
    last_heartbeat_ms: AtomicI64,
    heartbeat_count: AtomicI32,
    names: Mutex<MediaRouteNames>,
}

impl MediaRouteState {
    /// 创建默认服务状态。
    fn new() -> Self {
        Self {
            service1_running: AtomicBool::new(false),
            service2_running: AtomicBool::new(false),
            initialized: AtomicBool::new(false),
            boot_time: Instant::now(),
            last_heartbeat_ms: AtomicI64::new(0),
            heartbeat_count: AtomicI32::new(0),
            names: Mutex::new(MediaRouteNames::default()),
        }
    }

    /// 获取单调时间毫秒。
    fn now_ms(&self) -> i64 {
        self.boot_time.elapsed().as_millis() as i64
    }

    /// 初始化状态。
    fn init(&self) {
        self.service1_running.store(false, Ordering::SeqCst);
        self.service2_running.store(false, Ordering::SeqCst);
        self.last_heartbeat_ms
            .store(self.now_ms(), Ordering::SeqCst);
        self.heartbeat_count.store(0, Ordering::SeqCst);
        self.initialized.store(true, Ordering::SeqCst);
        if let Ok(mut names) = self.names.lock() {
            *names = MediaRouteNames::default();
        }
    }

    /// 记录 WakeLock 检查心跳。
    fn check_wake_lock(&self) {
        self.last_heartbeat_ms
            .store(self.now_ms(), Ordering::SeqCst);
        self.heartbeat_count.fetch_add(1, Ordering::SeqCst);
    }

    /// 记录 MediaRouteProviderService 启动。
    fn on_service_started(&self, package_name: String, service_name: String) {
        self.service1_running.store(true, Ordering::SeqCst);
        self.last_heartbeat_ms
            .store(self.now_ms(), Ordering::SeqCst);
        if let Ok(mut names) = self.names.lock() {
            names.package_name = package_name;
            names.service1_name = service_name;
        }
    }

    /// 记录 MediaRouteProviderService 停止。
    fn on_service_stopped(&self) {
        self.service1_running.store(false, Ordering::SeqCst);
    }

    /// 记录 MediaRoute2ProviderService 启动。
    fn on_service2_started(&self, package_name: String, service_name: String) {
        self.service2_running.store(true, Ordering::SeqCst);
        self.last_heartbeat_ms
            .store(self.now_ms(), Ordering::SeqCst);
        if let Ok(mut names) = self.names.lock() {
            names.package_name = package_name;
            names.service2_name = service_name;
        }
    }

    /// 记录 MediaRoute2ProviderService 停止。
    fn on_service2_stopped(&self) {
        self.service2_running.store(false, Ordering::SeqCst);
    }

    /// 执行 MediaRoute 心跳。
    fn perform_heartbeat(&self) -> bool {
        self.last_heartbeat_ms
            .store(self.now_ms(), Ordering::SeqCst);
        self.heartbeat_count.fetch_add(1, Ordering::SeqCst);
        self.service1_running.load(Ordering::SeqCst) || self.service2_running.load(Ordering::SeqCst)
    }

    /// 获取服务状态码：0=正常，1=警告，2=异常。
    fn service_status(&self) -> jint {
        let service1_ok = self.service1_running.load(Ordering::SeqCst);
        let service2_ok = self.service2_running.load(Ordering::SeqCst);
        match (service1_ok, service2_ok) {
            (true, true) => 0,
            (true, false) | (false, true) => 1,
            (false, false) => 2,
        }
    }
}

/// 获取全局 MediaRoute 状态。
fn media_route_state() -> &'static MediaRouteState {
    static STATE: OnceLock<MediaRouteState> = OnceLock::new();
    STATE.get_or_init(MediaRouteState::new)
}

/// 批量注册 JNI 方法。
fn register_methods(
    env: &mut JNIEnv<'_>,
    class_name: &str,
    methods: &[NativeMethod],
) -> jni::errors::Result<()> {
    // 查找 Kotlin 内部桥接类。
    let target_class = env.find_class(class_name)?;
    // 统一使用动态注册隐藏具体业务函数名。
    env.register_native_methods(target_class, methods)?;
    Ok(())
}

/// 注册 Rust 通用进程探测方法。
fn register_process_native_methods(env: &mut JNIEnv<'_>) -> jni::errors::Result<()> {
    let methods = [
        NativeMethod {
            name: "nativeIsAvailable".into(),
            sig: "()Z".into(),
            fn_ptr: native_is_available as *mut c_void,
        },
        NativeMethod {
            name: "nativeVersionCode".into(),
            sig: "()I".into(),
            fn_ptr: native_version_code as *mut c_void,
        },
        NativeMethod {
            name: "nativeGetOomAdj".into(),
            sig: "()I".into(),
            fn_ptr: native_get_oom_adj as *mut c_void,
        },
        NativeMethod {
            name: "nativeGetMemoryInfo".into(),
            sig: "()[J".into(),
            fn_ptr: native_get_memory_info as *mut c_void,
        },
        NativeMethod {
            name: "nativeGetProcessStatus".into(),
            sig: "()Ljava/lang/String;".into(),
            fn_ptr: native_get_process_status as *mut c_void,
        },
        NativeMethod {
            name: "nativeCheckRoot".into(),
            sig: "()Z".into(),
            fn_ptr: native_check_root as *mut c_void,
        },
        NativeMethod {
            name: "nativeGetProcessCount".into(),
            sig: "()I".into(),
            fn_ptr: native_get_process_count as *mut c_void,
        },
    ];
    register_methods(env, FW_RUST_NATIVE_CLASS, &methods)
}

/// 注册 Rust MediaRoute 方法。
fn register_media_route_native_methods(env: &mut JNIEnv<'_>) -> jni::errors::Result<()> {
    let methods = [
        NativeMethod {
            name: "nativeInit".into(),
            sig: "()V".into(),
            fn_ptr: native_media_route_init as *mut c_void,
        },
        NativeMethod {
            name: "nativeCheckWakeLock".into(),
            sig: "()V".into(),
            fn_ptr: native_media_route_check_wake_lock as *mut c_void,
        },
        NativeMethod {
            name: "nativeOnServiceStarted".into(),
            sig: "(Ljava/lang/String;Ljava/lang/String;)V".into(),
            fn_ptr: native_media_route_on_service_started as *mut c_void,
        },
        NativeMethod {
            name: "nativeOnServiceStopped".into(),
            sig: "()V".into(),
            fn_ptr: native_media_route_on_service_stopped as *mut c_void,
        },
        NativeMethod {
            name: "nativeOnService2Started".into(),
            sig: "(Ljava/lang/String;Ljava/lang/String;)V".into(),
            fn_ptr: native_media_route_on_service2_started as *mut c_void,
        },
        NativeMethod {
            name: "nativeOnService2Stopped".into(),
            sig: "()V".into(),
            fn_ptr: native_media_route_on_service2_stopped as *mut c_void,
        },
        NativeMethod {
            name: "nativePerformHeartbeat".into(),
            sig: "()Z".into(),
            fn_ptr: native_media_route_perform_heartbeat as *mut c_void,
        },
        NativeMethod {
            name: "nativeGetServiceStatus".into(),
            sig: "()I".into(),
            fn_ptr: native_media_route_get_service_status as *mut c_void,
        },
    ];
    register_methods(env, FW_RUST_MEDIA_ROUTE_CLASS, &methods)
}

/// 注册 Kotlin 内部桥接类使用的 Native 方法。
fn register_native_methods(vm: &JavaVM) -> jni::errors::Result<()> {
    // 通过当前加载线程获取 JNI 环境，保证注册动作发生在系统加载 so 的生命周期内。
    let mut env = vm.get_env()?;
    // 分组注册，便于后续继续迁移其他 Native 模块。
    register_process_native_methods(&mut env)?;
    register_media_route_native_methods(&mut env)?;
    Ok(())
}

/// 读取 UTF-8 文本文件，失败时返回空字符串。
fn read_text(path: &str) -> String {
    // /proc 文件通常很小，直接读取可以减少 JNI 外层复杂度。
    fs::read_to_string(path).unwrap_or_default()
}

/// 读取单行整数，失败时返回默认值。
fn read_first_i32(path: &str, default_value: jint) -> jint {
    // 只解析第一行，避免 /proc 文件带来的额外内容干扰。
    read_text(path)
        .lines()
        .next()
        .and_then(|line| line.trim().parse::<jint>().ok())
        .unwrap_or(default_value)
}

/// 读取当前进程 OOM adj，兼容新旧 /proc 路径。
fn read_oom_adj() -> jint {
    // Android 新版本优先使用 oom_score_adj。
    if Path::new("/proc/self/oom_score_adj").exists() {
        return read_first_i32("/proc/self/oom_score_adj", DEFAULT_OOM_SCORE_ADJ);
    }
    // 旧版本 oom_adj 取值范围较小，转换成近似 oom_score_adj。
    if Path::new("/proc/self/oom_adj").exists() {
        let old_adj = read_first_i32("/proc/self/oom_adj", 15);
        return old_adj * 1000 / 17;
    }
    DEFAULT_OOM_SCORE_ADJ
}

/// 从 /proc/meminfo 中解析指定字段。
fn parse_meminfo_value(line: &str, key: &str) -> Option<jlong> {
    // 每行格式通常是 `MemTotal: 123456 kB`。
    line.strip_prefix(key)?
        .split_whitespace()
        .next()
        .and_then(|value| value.parse::<jlong>().ok())
}

/// 读取系统内存信息，单位保持 KB，与现有 C++ 返回值一致。
fn read_memory_info() -> [jlong; 3] {
    let mut total_kb: jlong = 0;
    let mut free_kb: jlong = 0;
    let mut available_kb: jlong = 0;
    for line in read_text("/proc/meminfo").lines() {
        if let Some(value) = parse_meminfo_value(line, "MemTotal:") {
            total_kb = value;
        } else if let Some(value) = parse_meminfo_value(line, "MemFree:") {
            free_kb = value;
        } else if let Some(value) = parse_meminfo_value(line, "MemAvailable:") {
            available_kb = value;
        }
    }
    [total_kb, free_kb, available_kb]
}

/// 判断 /proc/self/status 的行是否需要返回给 Kotlin。
fn should_keep_status_line(line: &str) -> bool {
    // 与现有 C++ 逻辑保持一致，只返回调用方实际会展示的关键字段。
    line.starts_with("Name:")
        || line.starts_with("State:")
        || line.starts_with("Pid:")
        || line.starts_with("PPid:")
        || line.starts_with("Threads:")
        || line.starts_with("VmSize:")
        || line.starts_with("VmRSS:")
        || line.starts_with("VmPeak:")
}

/// 读取当前进程关键状态。
fn read_process_status() -> String {
    let status = read_text("/proc/self/status");
    if status.is_empty() {
        return "无法读取进程状态".to_string();
    }
    let mut result = String::new();
    for line in status.lines().filter(|line| should_keep_status_line(line)) {
        result.push_str(line);
        result.push('\n');
    }
    result
}

/// 从 /proc/self/status 中读取真实 UID。
fn read_real_uid() -> Option<u32> {
    // Uid 行格式通常为 `Uid:\t10000\t10000\t10000\t10000`。
    read_text("/proc/self/status")
        .lines()
        .find_map(|line| line.strip_prefix("Uid:"))
        .and_then(|rest| rest.split_whitespace().next())
        .and_then(|value| value.parse::<u32>().ok())
}

/// 检测 root 或 Magisk 痕迹。
fn has_root_marker() -> bool {
    if read_real_uid() == Some(0) {
        return true;
    }
    [
        "/system/app/Superuser.apk",
        "/system/xbin/su",
        "/system/bin/su",
        "/data/local/bin/su",
        "/sbin/su",
        "/sbin/.magisk",
        "/data/adb/magisk",
    ]
    .iter()
    .any(|path| Path::new(path).exists())
}

/// 统计 /proc 数字目录数量。
fn count_processes() -> jint {
    let entries = match fs::read_dir("/proc") {
        Ok(entries) => entries,
        Err(_) => return -1,
    };
    entries
        .filter_map(Result::ok)
        .filter_map(|entry| entry.file_name().into_string().ok())
        .filter(|name| !name.is_empty() && name.bytes().all(|byte| byte.is_ascii_digit()))
        .count() as jint
}

/// 创建 Java 字符串，失败时返回 null。
fn new_java_string(env: JNIEnv<'_>, value: String) -> jstring {
    match env.new_string(value) {
        Ok(java_string) => java_string.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

/// 读取 Java 字符串，失败时返回空字符串。
fn read_java_string(env: &mut JNIEnv<'_>, value: &JString<'_>) -> String {
    // JNI 字符串读取失败时降级为空字符串，避免 native 层 panic。
    env.get_string(value)
        .map(|java_string| java_string.to_string_lossy().into_owned())
        .unwrap_or_default()
}

/// Rust Native 骨架库加载入口。
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _reserved: *mut c_void) -> jint {
    // 注册成功后声明 JNI 1.6；失败时返回 0，让 System.loadLibrary 明确失败。
    match register_native_methods(&vm) {
        Ok(()) => JNI_VERSION_1_6,
        Err(_) => 0,
    }
}

/// 返回 Rust Native 骨架是否可用。
extern "system" fn native_is_available(_env: JNIEnv<'_>, _class: JClass<'_>) -> jboolean {
    // 能执行到这里说明 so 已加载、JNI 动态注册已完成。
    JNI_TRUE
}

/// 返回 Rust Native 骨架版本号。
extern "system" fn native_version_code(_env: JNIEnv<'_>, _class: JClass<'_>) -> jint {
    // 版本号用于 Kotlin 层和后续迁移任务做能力探测。
    FW_RUST_VERSION_CODE
}

/// 返回当前进程 OOM adj。
extern "system" fn native_get_oom_adj(_env: JNIEnv<'_>, _class: JClass<'_>) -> jint {
    // 与 C++ `get_oom_adj` 保持同一默认值和旧路径换算逻辑。
    read_oom_adj()
}

/// 返回系统内存信息：[总内存 KB, 空闲内存 KB, 可用内存 KB]。
extern "system" fn native_get_memory_info(env: JNIEnv<'_>, _class: JClass<'_>) -> jlongArray {
    // 数组结构与现有 C++ `getMemoryInfo` 保持一致。
    let values = read_memory_info();
    match env.new_long_array(values.len() as jint) {
        Ok(array) => {
            if env.set_long_array_region(&array, 0, &values).is_ok() {
                array.into_raw()
            } else {
                ptr::null_mut()
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// 返回当前进程关键状态。
extern "system" fn native_get_process_status(env: JNIEnv<'_>, _class: JClass<'_>) -> jstring {
    // Kotlin 层直接展示该字符串，因此保持 C++ 过滤字段一致。
    new_java_string(env, read_process_status())
}

/// 返回 root / Magisk 痕迹检测结果。
extern "system" fn native_check_root(_env: JNIEnv<'_>, _class: JClass<'_>) -> jboolean {
    // Rust 层不执行提权命令，只做只读文件和 UID 检测。
    if has_root_marker() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// 返回系统进程数量。
extern "system" fn native_get_process_count(_env: JNIEnv<'_>, _class: JClass<'_>) -> jint {
    // 与 C++ 逻辑一致，仅统计 /proc 下纯数字目录。
    count_processes()
}

/// Rust MediaRoute 初始化。
extern "system" fn native_media_route_init(_env: JNIEnv<'_>, _class: JClass<'_>) {
    // 初始化只重置 Rust 侧服务状态，不操作 WakeLock。
    media_route_state().init();
}

/// Rust MediaRoute WakeLock 检查。
extern "system" fn native_media_route_check_wake_lock(_env: JNIEnv<'_>, _class: JClass<'_>) {
    // Kotlin 层仍负责真实 WakeLock 获取，Rust 只记录状态心跳。
    media_route_state().check_wake_lock();
}

/// Rust MediaRouteProviderService 启动通知。
extern "system" fn native_media_route_on_service_started(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    package_name: JString<'_>,
    service_name: JString<'_>,
) {
    let package = read_java_string(&mut env, &package_name);
    let service = read_java_string(&mut env, &service_name);
    media_route_state().on_service_started(package, service);
}

/// Rust MediaRouteProviderService 停止通知。
extern "system" fn native_media_route_on_service_stopped(_env: JNIEnv<'_>, _class: JClass<'_>) {
    media_route_state().on_service_stopped();
}

/// Rust MediaRoute2ProviderService 启动通知。
extern "system" fn native_media_route_on_service2_started(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    package_name: JString<'_>,
    service_name: JString<'_>,
) {
    let package = read_java_string(&mut env, &package_name);
    let service = read_java_string(&mut env, &service_name);
    media_route_state().on_service2_started(package, service);
}

/// Rust MediaRoute2ProviderService 停止通知。
extern "system" fn native_media_route_on_service2_stopped(_env: JNIEnv<'_>, _class: JClass<'_>) {
    media_route_state().on_service2_stopped();
}

/// Rust MediaRoute 心跳。
extern "system" fn native_media_route_perform_heartbeat(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jboolean {
    if media_route_state().perform_heartbeat() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Rust MediaRoute 服务状态。
extern "system" fn native_media_route_get_service_status(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    media_route_state().service_status()
}
