

# Fw - Android 保活框架

<div align="center">

![萌萌计数器](https://count.getloli.com/get/@KeepLiveService?theme=rule34)

</div>

<p align="center">
  <b>🌟 如果觉得有帮助，请点击 <a href="https://github.com/Pangu-Immortal/KeepLiveService/stargazers">Star</a> 支持一下，关注不迷路！🌟</b>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![16K Page Size](https://img.shields.io/badge/16K%20Page%20Size-Compatible-orange.svg)](https://developer.android.com/guide/practices/page-sizes)
[![Google Play](https://img.shields.io/badge/Google%20Play-Ready-success.svg)](https://developer.android.com/distribute/best-practices/develop/64-bit)

> 安全研究用途：完整复现市面上所有的保活机制，穷尽展示所有保活手段，适配所有的主流机型和 ROM。
> 
> 为了拉齐全网共同认知，让小团队开发不在迷茫，开源了全套所有私密函数和私密策略。会长期持续迭代，会陆陆续续的公开所有的隐私策略，ecpm 策略等等，欢迎 star🌟 持续关注。
> 
> 更多详细内容，可以查阅项目：https://github.com/Pangu-Immortal/KeepAlivePerfect

🔥 **Telegram 群组**： [点击加群讨论，这里只是冰山一角。](https://t.me/+V7HSo1YNzkFkY2M1)

Tips：
有大量的人给我留言，让我关掉这几个开源库，还说维护这个方向的技术“不是人”，还有人张口闭口就是“畜生”，“一个人破坏了整个 Android 行业”，“Android 生态本来还有本份做的，这下都知道原来别人都不好好做，行业彻底乱了”，“本来那些被行业淘汰的团队，让你又救回来了，真TM无语”，“本来自己偷偷的做海外ovm，基本没有竞对，TMD这下全来搞了” …… 

> 还有一部分人一直在吐槽：“看不懂作者，根本找不到联系方式，怎么也联系不到作者。不知道作者怎么卖代码的。”
> 我如果说我根本就不卖代码呢？而且就不想让那 99% 的人联系上我呢？能筛选出 1% 的聪明人就回复不过来了。
> 吐槽也改变不了这个库的维护，而且会持续的迭代，后面还会把所有暗刷广告的策略全开源。水上和水下几十种用户无感知的暗刷。
> 用户都没看到，我已经刷了几十个广告，那不比弹出来打扰用户的强吗？谁更高尚？好心+无知无能=做坏事，俗称烂好人。

## 项目亮点

- **🆕 适配 Google Play 最新要求** - 完全兼容 2026 年 Google Play 商店的所有技术要求
- **📱 Android 16K 页面大小支持** - 原生代码已适配 16KB 页面对齐，兼容 Android 15+ 的 16K 页面设备
- **🔧 最新开发工具链** - 使用 AGP 8.14.3、Kotlin 2.2.21、JDK 21、NDK 27 等最新稳定版开发
- **📦 64 位架构全覆盖** - 支持 arm64-v8a、armeabi-v7a、x86_64、x86 四种架构
- **🛡️ 生产级代码质量** - 通过 Lint 检查、ProGuard 混淆优化，可直接上架应用商店

### **Star ⭐ 这个项目如果对你有帮助！欢迎 Start 🌟**

## 项目简介

Fw（Framework）是一个模块化的 Android 保活框架，复现了所有的商业应用的后台保活技术。当蓝牙设备连接、USB 设备插入、NFC 标签发现等事件发生时，即使应用在后台或进程被杀死，也能自动唤醒并恢复服务。

**特性：**

- 🚀 一行代码初始化
- 📦 模块化设计，策略可独立开关
- 🔧 支持 20+ 种保活策略
- 📱 适配 Android 7.0 - 16（API 24 - 36.1）
- 🏭 支持主流厂商（小米、华为、OPPO、vivo、三星、Google、传音等）
- 🔨 包含 Native C++ 层保活
- 📊 提供厂商集成分析工具

---

## 开发环境

| 项目 | 版本 |
|-----|------|
| Android Studio | Android Studio Otter 2 Feature Drop 2025.2.2|
| Gradle | 8.14.3 |
| AGP (Android Gradle Plugin) | 8.14.3 |
| Kotlin | 2.2.21 |
| JVM | 21 |
| NDK | 27.0.12077973 |
| CMake | 3.22.1 |

---

## SDK 版本

| 项目 | 版本                |
|-----|-------------------|
| compileSdk | 36.1 (Android 16) |
| targetSdk | 36.1              |
| minSdk | 24 (Android 7.0)  |

---

## Android 版本适配

| Android 版本 | API   | 适配要点 |
|-------------|-------|---------|
| 7.x | 24-25 | `startService()` |
| 8.0+ | 26+   | `startForegroundService()` + 通知渠道，静态广播受限 |
| 9.0+ | 28+   | 后台限制加强 |
| 10+ | 29+   | 后台启动 Activity 受限 |
| 11+ | 30+   | 前台服务类型必须声明 |
| 12+ | 31+   | `BLUETOOTH_CONNECT` 运行时权限，精确闹钟权限 |
| 13+ | 33+   | `POST_NOTIFICATIONS` 运行时权限 |
| 14+ | 34+   | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限 |
| 15+ | 35+   | 更严格的后台限制，**16KB 页面大小设备支持** |
| 16 | 36.1  | 最新 API |

### Android 16K 页面大小适配

从 Android 15 开始，部分设备使用 16KB 页面大小（而非传统的 4KB）。本项目已完成 16K 适配：

**适配方式：**

1. **CMake 链接选项** - 在 `CMakeLists.txt` 中添加：

   ```cmake
   set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
   ```

2. **ELF 段对齐** - 编译后的 Native 库 LOAD 段对齐值为 `0x4000` (16384 = 16KB)

**验证方法：**

```bash
# 使用 NDK 的 llvm-readelf 检查 ELF 对齐
llvm-readelf -l libfw_native.so | grep LOAD
# 输出应显示对齐值为 0x4000
```

**参考文档：** [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)

---

## 快速开始

### 一行代码初始化

```kotlin
// 在 Application.onCreate() 中
Fw.init(this)
```

### 自定义配置

```kotlin
Fw.init(this) {
    // 基础策略
    enableForegroundService(true)
    enableMediaSession(true)
    enableOnePixelActivity(true)

    // 定时唤醒
    enableJobScheduler(true)
    enableWorkManager(true)
    enableAlarmManager(true)

    // 账户同步
    enableAccountSync(true)

    // 广播监听
    enableBluetoothBroadcast(true)
    enableUsbBroadcast(true)
    enableNfcBroadcast(true)
    enableMediaMountBroadcast(true)

    // 内容观察者
    enableMediaContentObserver(true)
    enableFileObserver(true)

    // 双进程守护
    enableDualProcess(true)

    // 无法强制停止策略（仅 Android 5.0-11.0 有效）
    enableForceStopResistance(false) // 默认关闭，侵入性较强

    // Native 层保活
    enableNativeDaemon(true)
    enableNativeSocket(true)

    // 通知配置
    notificationTitle("音乐播放中")
    notificationContent("点击打开应用")
    notificationActivityClass(MainActivity::class.java)
}
```

### 控制方法

```kotlin
// 手动触发保活检查
Fw.check()

// 停止所有保活策略
Fw.stop()

// 检查是否已初始化
Fw.isInitialized()

// 锁屏 Activity（类似墨迹天气的锁屏天气）
LockScreenActivity.start(context)

// 悬浮窗保活
FloatWindowManager.showOnePixelFloat(context)  // 隐藏的 1 像素
FloatWindowManager.showVisibleFloat(context)    // 可见的悬浮球

// 电池优化豁免
BatteryOptimizationManager.requestIgnoreBatteryOptimizations(context)

// 打开厂商自启动设置
AutoStartPermissionManager.openAutoStartSettings(context)

// 厂商集成分析（分析目标应用的保活机制）
VendorIntegrationAnalyzer.getFullAnalysisReport(context, "com.moji.mjweather")
```

---
![二维码](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

🔥 **Telegram 群组**： [点击加群讨论，这里只是冰山一角。](https://t.me/+V7HSo1YNzkFkY2M1)

## 保活策略完整列表

### 1. 基础策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 前台服务 | `FwForegroundService` | `foregroundServiceType="mediaPlayback"`，系统认为是媒体应用 | ⭐⭐⭐⭐⭐ |
| MediaSession | `MediaSessionManager` | 创建媒体会话，获得系统特殊保护 | ⭐⭐⭐⭐⭐ |
| 1 像素 Activity | `OnePixelActivity` | 屏幕关闭时启动透明 Activity，提升进程优先级 | ⭐⭐⭐⭐ |
| 锁屏 Activity | `LockScreenActivity` | 在锁屏界面显示（如锁屏天气），保持前台状态 | ⭐⭐⭐⭐⭐ |
| 悬浮窗 | `FloatWindowManager` | 1 像素悬浮窗或悬浮球，系统认为应用在使用中 | ⭐⭐⭐⭐ |

### 2. 定时唤醒策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| JobScheduler | `FwJobService` | 系统级任务调度，最小间隔 15 分钟 | ⭐⭐⭐⭐ |
| WorkManager | `FwWorker` | Jetpack 任务调度，兼容性好 | ⭐⭐⭐⭐ |
| AlarmManager | `AlarmStrategy` | 精确闹钟唤醒，需要 `SCHEDULE_EXACT_ALARM` 权限 | ⭐⭐⭐ |

### 3. 账户同步策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| SyncAdapter | `FwSyncAdapter` | 账户同步机制，系统会定期触发同步 | ⭐⭐⭐⭐ |
| AccountAuthenticator | `FwAuthenticator` | 账户认证服务，配合 SyncAdapter 使用 | ⭐⭐⭐⭐ |

### 4. 广播监听策略（静态注册）

| 策略 | 类名 | 监听的广播 | 有效性 |
|-----|------|----------|-------|
| 蓝牙广播 | `BluetoothReceiver` | ACL_CONNECTED, A2DP, HEADSET, AUDIO_BECOMING_NOISY | ⭐⭐⭐⭐⭐ |
| USB 广播 | `UsbReceiver` | USB_DEVICE_ATTACHED, USB_ACCESSORY_ATTACHED | ⭐⭐⭐⭐ |
| NFC 广播 | `NfcReceiver` | TAG_DISCOVERED, TECH_DISCOVERED, NDEF_DISCOVERED | ⭐⭐⭐⭐ |
| 媒体按键 | `MediaButtonReceiver` | MEDIA_BUTTON（蓝牙耳机按键） | ⭐⭐⭐⭐ |
| 媒体挂载 | `MediaMountReceiver` | MEDIA_MOUNTED, MEDIA_EJECT, MEDIA_SCANNER | ⭐⭐⭐⭐ |
| 系统事件 | `SystemEventReceiver` | BOOT_COMPLETED, MY_PACKAGE_REPLACED | ⭐⭐⭐⭐⭐ |

### 5. 内容观察者策略

| 策略 | 类名 | 监听内容 | 有效性 |
|-----|------|---------|-------|
| 相册变化 | `ContentObserverManager` | MediaStore.Images, Videos, Audio | ⭐⭐⭐ |
| 联系人变化 | `ContentObserverManager` | ContactsContract | ⭐⭐⭐ |
| 短信变化 | `ContentObserverManager` | Telephony.Sms | ⭐⭐⭐ |
| 文件系统 | `FileObserverManager` | Download, DCIM, Screenshots, Documents | ⭐⭐⭐ |

### 6. 系统级服务策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 无障碍服务 | `FwAccessibilityService` | 系统级服务，优先级最高，需用户手动开启 | ⭐⭐⭐⭐⭐ |
| 通知监听服务 | `FwNotificationListenerService` | 系统级服务，被杀后系统自动重启 | ⭐⭐⭐⭐⭐ |

### 7. 双进程守护策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| Java 双进程 | `DaemonService` | 独立进程 `:daemon`，互相守护 | ⭐⭐⭐⭐ |
| Native 守护进程 | `FwNative` | C++ fork() 子进程监控，使用 am 命令重启 | ⭐⭐⭐⭐ |
| Socket 心跳 | `FwNative` | Unix Domain Socket 进程间通信 | ⭐⭐⭐ |

### 8. 无法强制停止策略
原理介绍，阅读地址：https://mp.weixin.qq.com/s/-9L6XOfrzh69hOQ9puK6iQ

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 多进程文件锁监控 | `ForceStopResistance` | 多个辅助进程通过文件锁互相监控 | ⭐⭐⭐⭐ |
| app_process 拉活 | `AppProcessLauncher` | 通过 app_process 命令启动 Java 进程 | ⭐⭐⭐⭐ |
| AMS Binder 直接调用 | `AmsBinderInvoker` | 直接调用 AMS Binder 启动服务/广播/Instrumentation | ⭐⭐⭐⭐⭐ |
| Instrumentation 拉活 | `FwInstrumentation` | 通过 am instrument 命令拉起进程 | ⭐⭐⭐⭐ |

> **适用范围**：此策略在 **Android 5.0 - 12.0** (API 21 - 31) 上测试有效。Android 10+ 官方声称已封堵，实际在 Android 13 才开始下发的补丁，实测大部分的设备仍可用。
>
> **默认关闭**：由于侵入性较强，此功能默认关闭，需手动启用。
>
> **教学意义**：此功能复现了早期 Android 生态的安全漏洞，展示系统演进过程中的攻防博弈。

#### 核心原理：5ms 时间差竞争

无法强制停止的关键在于 **杀死与唤醒的时间差**。当用户点击「强制停止」时，系统需要逐个杀死应用的所有进程，每个进程之间存在约 **5ms** 的时间间隔。

**为什么存在时间差？**

Android 系统强制停止应用时的执行流程：

1. AMS 遍历应用的所有进程
2. 对每个进程调用 `Process.killProcess()` 或发送 `SIGKILL`
3. 等待进程死亡确认
4. 继续杀死下一个进程

这个「遍历 → 杀死 → 确认」的循环需要时间，每个进程大约 **5ms**。如果应用有 4 个进程（主进程 + 3 个辅助进程），总时间窗口约 **15-20ms**。

**为什么 5ms 足够？**

关键在于使用 **C++ Native 层直接 Binder 调用**，而不是 Java 层：

| 方式 | 调用链 | 耗时 |
|-----|-------|-----|
| Java `startService()` | Intent 创建 → Parcel 序列化 → Binder 代理 → AMS 处理 | **10-50ms** |
| C++ 直接 Binder | `open("/dev/binder")` → 预构造 Parcel → `ioctl()` 发送 | **< 1ms** |

C++ 层的优势：

- **预先构造 Parcel**：服务启动参数在初始化时就序列化好，检测到死亡时直接发送
- **跳过 Intent 解析**：不需要创建 Intent 对象，省去对象创建开销
- **直接驱动调用**：通过 `ioctl(fd, BINDER_WRITE_READ, &bwr)` 直接与 Binder 驱动通信
- **无 GC 开销**：纯 C++ 代码，不受 Java 垃圾回收影响

**时间线示例**：

```
T=0ms    : 用户点击「强制停止」
T=1ms    : 系统开始杀死进程1（主进程）
T=2ms    : 进程1 文件锁释放，进程2 检测到
T=2.5ms  : 进程2 通过 Binder 直接调用 AMS.startService ← 关键！
T=3ms    : AMS 收到请求，开始启动服务
T=6ms    : 系统杀死进程2
T=8ms    : 新服务进程启动完成 ← 拉活成功！
T=11ms   : 系统杀死进程3
T=16ms   : 系统杀死进程4
T=20ms   : 强制停止流程结束，但服务已重新运行
```

**AMS Binder 直接调用是核心**：
- 传统方式：`startService()` → 创建 Intent → Binder 代理 → AMS → 启动服务（耗时长）
- 直接调用：打开 /dev/binder → 预构造 Parcel → `ioctl()` 直接发送（耗时极短）

通过跳过 Intent 解析、权限检查等中间步骤，直接向 AMS 发送启动请求，可以在进程被完全杀死前完成拉活操作。

#### 工作流程

**阶段一：初始化（应用启动时）**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           应用启动                                        │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
            │  主进程      │ │ 辅助进程1    │ │ 辅助进程2    │
            │  :main      │ │ :assist1    │ │ :assist2    │
            └─────────────┘ └─────────────┘ └─────────────┘
                    │               │               │
                    ▼               ▼               ▼
            ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
            │ 1.锁定文件A  │ │ 1.锁定文件B  │ │ 1.锁定文件C  │
            │ 2.打开Binder │ │ 2.打开Binder │ │ 2.打开Binder │
            │ 3.获取AMS句柄│ │ 3.获取AMS句柄│ │ 3.获取AMS句柄│
            │ 4.预构造Parcel│ │ 4.预构造Parcel│ │ 4.预构造Parcel│
            │ 5.fork守护进程│ │ 5.fork守护进程│ │ 5.fork守护进程│
            └─────────────┘ └─────────────┘ └─────────────┘
                    │               │               │
                    └───────┬───────┴───────┬───────┘
                            ▼               ▼
                    ┌─────────────────────────────┐
                    │  所有进程互相监控对方的文件锁  │
                    │  （阻塞在 flock() 等待锁释放） │
                    └─────────────────────────────┘
```

**阶段二：强制停止触发时的时间竞争**

```
时间轴（毫秒）
────────────────────────────────────────────────────────────────────────────

T=0ms   │ 用户点击「强制停止」
        │
T=1ms   │ ┌─────────────────────────────────────────────────────────────┐
        │ │ AMS 开始杀死主进程 (:main)                                   │
        │ │ → Process.killProcess() / SIGKILL                           │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=2ms   │ ┌─────────────────────────────────────────────────────────────┐
        │ │ 主进程死亡 → 文件锁A自动释放                                  │
        │ │ → 辅助进程1 的 flock() 立即返回（检测到主进程死亡！）          │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=2.5ms │ ┌─────────────────────────────────────────────────────────────┐
        │ │ 【关键】辅助进程1 通过预构造的 Parcel 直接 ioctl() 调用      │
        │ │ → Binder transact 到 AMS.startService()                     │
        │ │ → 耗时 < 1ms（因为 Parcel 已预构造，无需创建对象）            │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=3ms   │ ┌─────────────────────────────────────────────────────────────┐
        │ │ AMS 收到 startService 请求                                   │
        │ │ → 开始创建新的服务进程                                       │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=6ms   │ ┌─────────────────────────────────────────────────────────────┐
        │ │ AMS 继续杀死辅助进程1 (:assist1)                             │
        │ │ → 但 startService 请求已发出！                               │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=8ms   │ ┌─────────────────────────────────────────────────────────────┐
        │ │ 【拉活成功】新服务进程启动完成                                │
        │ │ → 应用已重新运行！                                           │
        │ └─────────────────────────────────────────────────────────────┘
        │
T=11ms  │ AMS 杀死辅助进程2 (:assist2)
        │
T=16ms  │ AMS 杀死辅助进程3 (:assist3)
        │
T=20ms  │ 强制停止流程结束
        │ → 但应用服务已在 T=8ms 重新运行！
        │
────────────────────────────────────────────────────────────────────────────
```

**关键点总结**：

| 步骤 | 操作 | 耗时 | 说明 |
|-----|------|-----|------|
| 1 | 检测进程死亡 | ~0ms | flock() 阻塞等待，锁释放立即返回 |
| 2 | 发送 Binder 请求 | <1ms | 预构造 Parcel + 直接 ioctl() |
| 3 | AMS 处理请求 | ~5ms | 系统创建新进程 |
| **总计** | **从检测到拉活** | **<6ms** | **小于进程杀死间隔（~5ms）** |

#### 为什么 Android 10+ 理论上失效？

Android 10 开始引入了以下限制：

1. **cgroup 进程组杀死**：强制停止时使用 `killProcessGroup()` 杀死整个 cgroup，理论上所有进程同时死亡
2. **后台启动限制**：后台进程无法启动 Activity/Service
3. **Binder 调用限制**：非前台进程的 Binder 调用受限
4. **SELinux 加强**：app_process 等命令执行受限

**但实测情况**：在部分 Android 12 设备上，此策略仍然有效。可能原因：

- 某些厂商 ROM 未完全实现 cgroup 进程组杀死
- `killProcessGroup()` 实际执行仍存在微小时间差
- 设备内核版本差异导致行为不一致

建议在目标设备上实际测试验证效果。

#### Native 层实现（C++）

为了最大化时间窗口利用率，核心逻辑使用 C++ 实现：

```
framework/src/main/cpp/
├── fw_force_stop.cpp          # 无法强制停止核心实现
├── binder/
│   ├── common.h               # 公共定义
│   ├── cParcel.cpp/h          # Parcel 数据封装
│   └── data_transact.cpp/h    # Binder 事务处理
└── utils/
    ├── String16.cpp/h         # UTF-16 字符串
    ├── Unicode.cpp/h          # Unicode 编解码
    └── SharedBuffer.cpp/h     # 共享缓冲区
```

关键技术点：

1. **直接打开 /dev/binder** - 通过 `open("/dev/binder")` 直接访问 Binder 驱动
2. **手动构造 Parcel** - 不依赖 Android Framework，手动序列化数据
3. **ioctl 系统调用** - 使用 `ioctl(fd, BINDER_WRITE_READ, &bwr)` 直接通信
4. **flock 文件锁** - 使用 POSIX 文件锁检测进程死亡

### 9. 进程优先级管理

| 功能 | 类名 | 说明 |
|-----|------|------|
| 进程状态监控 | `ProcessPriorityManager` | 获取当前进程 importance、OOM adj 值 |
| 被杀风险评估 | `ProcessPriorityManager` | 评估进程被系统杀死的风险等级 |
| 内存信息获取 | `ProcessPriorityManager` | 获取系统和应用内存使用情况 |

### 10. 厂商集成策略

| 功能 | 类名 | 说明 |
|-----|------|------|
| 电池优化豁免 | `BatteryOptimizationManager` | 请求加入 Doze 白名单 |
| 厂商自启动管理 | `AutoStartPermissionManager` | 打开各厂商的自启动设置页面 |
| 厂商集成分析 | `VendorIntegrationAnalyzer` | 分析应用的推送 SDK 和系统权限 |

---

## 厂商推送通道复用（高级策略）

墨迹天气等应用"永生不死"的核心秘密之一：**厂商推送通道**。

### 原理

厂商推送服务（小米推送、华为推送、FCM 等）是系统级常驻服务，即使应用被杀，推送到达时也会拉起应用。

### 集成方式（举例子）

```kotlin
// 1. 集成厂商推送 SDK
// 小米推送
implementation("com.xiaomi.mipush:sdk:5.1.2")

// 华为推送
implementation("com.huawei.hms:push:6.11.0.300")

// OPPO 推送
implementation("com.heytap.msp:push:3.1.0")

// vivo 推送
implementation("com.vivo.push:vivo-push:3.0.0.6")

// Google FCM
implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

// 2. 在应用中注册推送
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化保活框架
        Fw.init(this)

        // 初始化厂商推送（根据设备自动选择）
        when {
            isMiui() -> MiPushClient.registerPush(this, APP_ID, APP_KEY)
            isEmui() -> HmsMessaging.getInstance(this).isAutoInitEnabled = true
            isColorOS() -> HeytapPushManager.init(this, true)
            isFuntouchOS() -> PushClient.getInstance(this).initialize()
            else -> {
                // 对于原生、Google、传音等其他设备，统一使用 FCM
                Firebase.messaging.isAutoInitEnabled = true
            }
        }
    }
}
```

### 推送 SDK 包名参考

| 厂商 | 推送 SDK | 包名 |
|-----|---------|------|
| 小米 | MiPush | `com.xiaomi.mipush.sdk` |
| 华为 | HMS Push | `com.huawei.hms.push` |
| OPPO | OPPO Push | `com.heytap.msp` |
| vivo | vivo Push | `com.vivo.push` |
| Google | FCM | `com.google.firebase.messaging` |
| 魅族 | Flyme Push | `com.meizu.cloud.pushsdk` |
| 个推 | GeTui | `com.igexin.sdk` |
| 极光 | JPush | `cn.jpush.android` |

### FCM 数据消息 (Data Message) 模式建议（只有 data 模式可以拉活）

对于通过 FCM (Firebase Cloud Messaging) 进行推送，强烈建议使用 **数据消息 (Data Message)** 而不是通知消息 (Notification Message)。

| 类型 | `notification` 消息 | `data` 消息 |
|---|---|---|
| **优点** | 简单，由系统自动处理通知显示。 | 灵活性高，应用完全控制消息处理和通知显示。 |
| **缺点** | 应用在后台时，消息由系统处理，无法自定义，**可能不会唤醒应用**。 | 需要应用自己实现 `FirebaseMessagingService` 来接收和处理消息。 |
| **唤醒能力** | 弱（后台时由系统决定） | **强（应用在后台或被杀时，可以高优先级唤醒应用并执行代码）** |

**为什么使用 `data` 消息？**

为了确保后台唤醒的可靠性，`data` 消息是必须的。当应用收到 `data` 消息时，`onMessageReceived` 回调会被触发，即使应用在后台。这给了我们执行代码、启动服务、弹出自定义通知的机会，从而实现可靠的保活。

**实现示例:**

1.  **在 `AndroidManifest.xml` 中注册服务:**
    ```xml
    <service
        android:name=".MyFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
    ```

2.  **实现 `FirebaseMessagingService`:**
    ```kotlin
    class MyFirebaseMessagingService : FirebaseMessagingService() {
        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            // 收到数据消息
            FwLog.d("FCM", "From: ${remoteMessage.from}")

            // 检查消息中是否包含 data payload
            remoteMessage.data.isNotEmpty().let {
                FwLog.d("FCM", "Message data payload: " + remoteMessage.data)

                // 在这里执行唤醒逻辑
                // 例如：启动一个前台服务
                Fw.check()
            }
        }
    }
    ```

3.  **服务端发送 `data` 消息 (JSON 格式):**
    ```json
    {
      "to": "DEVICE_TOKEN",
      "priority": "high",
      "data": {
        "title": "后台任务",
        "body": "正在执行后台任务...",
        "action": "KEEP_ALIVE_CHECK"
      }
    }
    ```
    **关键点:**
    - `priority` 设置为 `high`，以获得最及时的传递（即使在 Doze 模式下）。
    - 消息体中只包含 `data` 字段，不包含 `notification` 字段。

通过这种方式，FCM 就不再仅仅是一个通知通道，而是一个强大的后台唤醒工具。

---

### 检测命令

```bash
# 检查系统白名单
adb shell cat /system/etc/sysconfig/*.xml | grep -i moji

# 检查应用签名
adb shell dumpsys package com.moji.mjweather | grep -A5 "signatures"

# 检查是否预装
adb shell pm path com.moji.mjweather

# 检查应用权限
adb shell dumpsys package com.moji.mjweather | grep permission
```

---

## 项目架构

```
KeepLiveService/
├── app/                           # 示例应用模块
│   └── src/main/
│       ├── java/.../
│       │   ├── KeepLiveApp.kt     # Application 入口
│       │   └── MainActivity.kt    # 主界面
│       └── AndroidManifest.xml
│
├── framework/                     # 保活框架模块
│   └── src/main/
│       ├── java/com/service/framework/
│       │   ├── Fw.kt                        # 框架入口（一行代码初始化）
│       │   ├── core/
│       │   │   └── FwConfig.kt              # 配置类（Builder 模式）
│       │   ├── service/
│       │   │   ├── FwForegroundService.kt   # 主前台服务
│       │   │   └── DaemonService.kt         # 守护进程服务
│       │   ├── receiver/
│       │   │   ├── BluetoothReceiver.kt     # 蓝牙广播（核心）
│       │   │   ├── UsbReceiver.kt           # USB 设备广播
│       │   │   ├── NfcReceiver.kt           # NFC 标签广播
│       │   │   ├── MediaButtonReceiver.kt   # 媒体按键广播
│       │   │   ├── MediaMountReceiver.kt    # 媒体挂载广播
│       │   │   ├── SystemEventReceiver.kt   # 系统事件广播
│       │   │   └── WifiReceiver.kt          # WiFi 状态广播
│       │   ├── observer/
│       │   │   ├── ContentObserverManager.kt # 内容观察者管理
│       │   │   └── FileObserverManager.kt    # 文件系统观察者
│       │   ├── account/
│       │   │   ├── FwAuthenticator.kt       # 账户认证器
│       │   │   ├── FwSyncAdapter.kt         # 同步适配器
│       │   │   ├── AuthenticatorService.kt  # 认证服务
│       │   │   ├── SyncService.kt           # 同步服务
│       │   │   └── StubContentProvider.kt   # 同步用 Provider
│       │   ├── strategy/
│       │   │   ├── FwJobService.kt          # JobScheduler 策略
│       │   │   ├── FwWorker.kt              # WorkManager 策略
│       │   │   ├── AlarmStrategy.kt         # AlarmManager 策略
│       │   │   ├── OnePixelActivity.kt      # 1 像素 Activity
│       │   │   ├── LockScreenActivity.kt    # 锁屏 Activity（新增）
│       │   │   ├── FloatWindowManager.kt    # 悬浮窗管理（新增）
│       │   │   ├── BatteryOptimizationManager.kt  # 电池优化管理（新增）
│       │   │   ├── VendorIntegrationAnalyzer.kt   # 厂商集成分析（新增）
│       │   │   ├── FwAccessibilityService.kt      # 无障碍服务
│       │   │   ├── FwNotificationListenerService.kt # 通知监听服务
│       │   │   ├── ProcessPriorityManager.kt # 进程优先级管理
│       │   │   └── forcestop/               # 无法强制停止策略（C++ 实现）
│       │   │       ├── ForceStopResistance.kt    # 策略入口（调用 Native）
│       │   │       ├── AssistService1/2/3.kt     # 辅助进程服务
│       │   │       ├── ProcessUtils.kt           # 进程工具类
│       │   │       ├── HiddenApiBypass.kt        # 隐藏 API 绕过
│       │   │       ├── ForceStopReceiver.kt      # 唤醒广播接收器
│       │   │       └── FwInstrumentation.kt      # Instrumentation 组件
│       │   ├── native/
│       │   │   └── FwNative.kt              # Native 层 JNI 接口
│       │   └── util/
│       │       ├── ServiceStarter.kt        # 服务启动器
│       │       └── FwLog.kt                 # 日志工具
│       ├── cpp/                             # Native C++ 层
│       │   ├── CMakeLists.txt               # CMake 构建配置
│       │   ├── fw_daemon.cpp                # 守护进程（fork）
│       │   ├── fw_process.cpp               # 进程管理（OOM adj）
│       │   ├── fw_socket.cpp                # Socket 通信
│       │   ├── fw_jni.cpp                   # JNI 入口
│       │   ├── fw_force_stop.cpp            # 无法强制停止核心实现
│       │   ├── binder/                      # Binder 直接调用
│       │   │   ├── common.h                 # 公共定义
│       │   │   ├── cParcel.cpp/h            # Parcel 数据封装
│       │   │   └── data_transact.cpp/h      # Binder 事务处理
│       │   └── utils/                       # 工具类
│       │       ├── String16.cpp/h           # UTF-16 字符串
│       │       ├── Unicode.cpp/h            # Unicode 编解码
│       │       └── SharedBuffer.cpp/h       # 共享缓冲区
│       └── res/
│           └── xml/
│               ├── authenticator.xml        # 账户认证配置
│               ├── syncadapter.xml          # 同步适配器配置
│               ├── nfc_tech_list.xml        # NFC 技术列表
│               └── accessibility_service_config.xml # 无障碍服务配置
│
├── build.gradle.kts               # 根项目构建脚本
├── settings.gradle.kts            # 项目设置
└── gradle/libs.versions.toml      # 依赖版本管理
```

---

## 厂商适配

| 厂商 | 特殊限制 | 解决方案 |
|-----|---------|---------|
| 小米 (MIUI) | 自启动管理、电池优化 | 引导用户开启自启动权限 |
| 华为 (EMUI) | 高级电池管理 | 引导用户关闭电池优化 |
| OPPO (ColorOS) | 后台冻结 | 引导用户添加省电白名单 |
| vivo (Funtouch) | i管家限制 | 引导用户开启后台运行权限 |
| 三星 (OneUI) | 设备维护优化 | 相对宽松 |
| Google (Pixel) | Doze 模式严格 | 请求 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`，使用高优先级 FCM 消息。 |
| 传音 (Tecno) | 后台管理类似原生，但有内存清理 | 引导用户锁定应用，加入自启动列表。 |

### 厂商自启动设置入口

```kotlin
// 自动打开当前厂商的自启动设置
AutoStartPermissionManager.openAutoStartSettings(context)

// 获取引导文案
val guideText = AutoStartPermissionManager.getGuideText()
// 返回：请在「自启动管理」中开启本应用的自启动权限
```

---

## 权限说明

### Manifest 权限（自动授予）

```xml
<!-- 前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- 蓝牙 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

<!-- NFC -->
<uses-permission android:name="android.permission.NFC" />

<!-- 网络 -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- 电源 -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- 闹钟 -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- 开机广播 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 账户同步 -->
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.READ_SYNC_SETTINGS" />
<uses-permission android:name="android.permission.WRITE_SYNC_SETTINGS" />

<!-- 悬浮窗 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 锁屏显示 -->
<uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

### 运行时权限（需用户授予）

```kotlin
// Android 12+ 蓝牙连接权限
Manifest.permission.BLUETOOTH_CONNECT

// Android 13+ 通知权限
Manifest.permission.POST_NOTIFICATIONS

// 存储权限（用于 ContentObserver）
Manifest.permission.READ_MEDIA_IMAGES
Manifest.permission.READ_MEDIA_VIDEO
Manifest.permission.READ_MEDIA_AUDIO

// 联系人权限（用于 ContentObserver）
Manifest.permission.READ_CONTACTS

// 短信权限（用于 ContentObserver）
Manifest.permission.READ_SMS

// 悬浮窗权限
Settings.canDrawOverlays(context)
```

---

## 核心原理

### 为什么酷狗能被蓝牙唤醒？

| 机制 | 说明 |
|-----|------|
| 静态广播接收器 | 在 `AndroidManifest.xml` 中静态注册蓝牙广播 |
| MediaSession | 创建媒体会话让系统认为这是媒体应用 |
| 前台服务类型 | 声明 `foregroundServiceType="mediaPlayback"` |
| 永不 stopped | 有常驻组件的应用不会进入真正的 stopped 状态 |

### 为什么墨迹天气"永生不死"？

| 机制 | 说明 |
|-----|------|
| 厂商白名单 | 与厂商签署商业合作，被加入系统级白名单 |
| 推送通道 | 集成厂商推送 SDK，推送到达时拉起应用 |
| 预装合作 | 预装应用有特殊签名和权限 |
| 锁屏功能 | 提供"锁屏天气"，保持前台状态 |

### 强制停止 vs 进程被杀

| 情况 | FLAG_STOPPED | 广播接收器 | 保活效果 |
|-----|-------------|-----------|---------|
| 进程被杀（内存不足） | 不设置 | 可接收 | 可被唤醒 |
| 强制停止（Force Stop） | 设置 | 被禁用 | 无法唤醒 |
| 用户主动杀死（最近任务） | 不设置 | 可接收 | 可被唤醒 |

---

## 使用方法

### 1. 添加依赖

```kotlin
// settings.gradle.kts
include(":framework")

// app/build.gradle.kts
dependencies {
    implementation(project(":framework"))
}
```

### 2. 初始化

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fw.init(this)
    }
}
```

### 3. 构建运行

**Debug 构建：**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Release 构建（带时间戳）：**

```bash
# 一键打包，自动生成带时间戳的 Release APK 和 mapping 文件
./gradlew buildTimestampedReleaseApk

# 输出路径：release/app-202512101118.apk
#           release/mapping-202512101118.txt
```

**自定义 Gradle Tasks：**

| Task | 说明 |
|------|------|
| `buildTimestampedReleaseApk` | 构建 Release APK，输出到根目录 `release/` 文件夹，包含 APK 和 mapping 文件 |
| `generateProguardDictionary` | 生成新的随机混淆字典（警告：会覆盖现有字典） |

### 4. 测试蓝牙唤醒

```bash
# 模拟蓝牙设备连接
./test_bluetooth_broadcast.sh connect

# 模拟蓝牙耳机连接
./test_bluetooth_broadcast.sh headset

# 模拟音频输出变化
./test_bluetooth_broadcast.sh noisy
```

### 5. 查看日志

```bash
adb logcat | grep -E "(Fw|BluetoothReceiver|UsbReceiver|NfcReceiver)"
```

---

## 常见问题

**Q: 为什么强制停止后应用不能被唤醒？**

强制停止会设置 `FLAG_STOPPED`，导致所有静态广播接收器被禁用。这是 Android 的安全机制，无法绕过。

**Q: 为什么某些厂商手机效果不好？**

国产厂商（小米、华为、OPPO、vivo）有额外的后台管理机制，需要引导用户：

1. 开启自启动权限
2. 关闭电池优化
3. 添加省电白名单
4. 锁定最近任务卡片

**Q: Android 14+ 前台服务启动失败？**

需要声明对应的前台服务类型权限：
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

**Q: Native 守护进程有效吗？**

Native 守护进程（fork）在普通应用中效果有限，因为：

1. 强制停止会杀死整个进程组
2. 部分厂商对 Native 守护有额外检测
3. SELinux 限制 am 命令执行

但配合 Java 层双进程可以提高存活率。

**Q: 如何像墨迹天气一样"永生不死"？**

普通应用很难达到墨迹天气的效果，因为它们可能：

1. 与厂商有商业合作，被加入系统级白名单
2. 是预装应用，有特殊权限
3. 集成了厂商推送 SDK

建议：集成厂商推送 SDK + 引导用户开启自启动权限 + 请求电池优化豁免。

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 更新日志

### v1.0.0 (2024-12)

- 初始版本
- 支持 20+ 种保活策略
- 包含 Native C++ 层
- 厂商集成分析工具

---

## License

```text
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   Copyright 2024 KeepLiveService Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

**简单说明：**

- ✅ 允许商业使用
- ✅ 允许修改
- ✅ 允许分发
- ✅ 允许私有使用
- ✅ 允许专利使用
---

## 免责声明

本项目仅供安全研究和学习使用。使用者应遵守当地法律法规，不得将本项目用于任何非法用途。作者不对使用本项目造成的任何后果负责。

---

## 致谢

- 感谢所有为 Android 安全研究做出贡献的研究者
- 感谢开源社区的支持

---

**Star ⭐ 这个项目如果对你有帮助！欢迎 Start 🌟**

![二维码](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

🔥 **Telegram 群组**： [点击加群讨论，这里只是冰山一角。](https://t.me/+V7HSo1YNzkFkY2M1)

---
（所有功能均提供对 Android 16 版本的适配，添加联系时请备注需求）

- **应用自启动**：应用安装后无需用户点击即可自动启动。
- **暗刷 H5 广告**：偷偷刷 ADX、adsense 等 H5 广告，可以开启 20+ view 同时刷。
- **无权限后台弹出 Activity**：无需权限即可在后台任意时机弹出 Activity，无需锁屏。
- **应用保活**：应用可在多次强制停止操作后仍保持运行，完美抵抗强制停止操作。
- **应用拉活**：在应用彻底死亡的状态下，可在15分钟内唤醒自身。
- **防卸载**：防止用户卸载应用，点击卸载无反应。
- **无感知卸载竞品**：可无感知地卸载手机中任意应用。
- **隐藏桌面图标**：应用安装后立即隐藏自身，或在需要时随时隐藏，支持 Android 16。
- **马甲包服务**：彻底解决关联问题，为批量马甲包提供服务。
- **报病毒优化**：无需重新打包，净化应用，处理所有应用的报毒问题。
- **账号隔离**：为开发者提供完善的账号隔离体系，防止账号关联。
- **IP 漂移**：支持拉取高 eCPM 地区的 AdMob 广告。
- **模拟 iOS**：支持 Android 设备模拟并拉取 iOS 的 AdMob 广告，大幅提高 eCPM。
- **机型模拟工具**：支持批量刷下载量，可无成本快速刷百万下载量，迅速提高商店排名。
- **国内机型保活**：为运动类、外卖类、聊天类等应用实现永生不死，不被系统杀死，已为多款应用接入。
- **防抓包处理**：数据脱敏，适用于棋牌类大规模上架等操作。
- **多开、双开工具**：支持无限分身等功能。
- **大模型定制开发**：提供私有数据训练、NSFW 模型开发、成人模型制作、成人话术、成人照片、成人视频等私有化专属大模型训练制作。
- **数字人、换脸、图生图、图生视频**：制作明星、自己、家人的数字人，老照片复活，与已逝去的亲人对话。
- **云游戏、云手机搭建**：提供全套云端容器方案，涵盖云原生 GPU、定制化服务器、全光网络、协同渲染、AI 内容生成、云原生工具包等核心技术路径。
- **定制化播放器**：提供加密播放器、3D 播放器、云播放器等，可为任意视频编解码提供定制服务，为 AR、VR、MR 场景提供服务。
- **滤镜定制**：提供视频、相机、图片等滤镜处理，可根据竞品效果进行模仿。
- **AI 多场景定制**：多年 AI 行业经验，可为小团队提供定制化的 AI 服务。
- **ROM 定制**：提供各类定制化功能的 Android 系统，也可提供车载系统的定制化，提供软硬件交互的外包服务。

---
**Star ⭐ 这个项目如果对你有帮助！欢迎 Start 🌟**

---

## ⭐ Star 趋势

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
