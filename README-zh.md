# Fw - Android 保活技术百科全书

<div align="center">

![Android 保活框架 KeepLiveService 访问计数](https://count.getloli.com/get/@KeepLiveService?theme=rule34)

<p>
  <b>如果觉得有帮助，请点击 <a href="https://github.com/Pangu-Immortal/KeepLiveService/stargazers">Star</a> 支持一下，关注不迷路！</b>
</p>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.pangu-immortal/keeplive-framework.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.pangu-immortal/keeplive-framework)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org)
[![16K Page Size](https://img.shields.io/badge/16K%20Page%20Size-Compatible-orange.svg)](https://developer.android.com/guide/practices/page-sizes)
[![Google Play](https://img.shields.io/badge/Google%20Play-Ready-success.svg)](https://developer.android.com/distribute/best-practices/develop/64-bit)

</div>

[English](README.md) | **简体中文**

> **TL;DR — 为什么选择 Fw？**
>
> Fw 是目前开源社区中策略最全、版本覆盖最广的 Android 后台常驻框架。只需一行代码 `Fw.init(this)` 即可接入 35+ 种进程保护策略，覆盖 Android 7.0 到 16 全版本以及小米、华为、OPPO 等 10+ 厂商 ROM。无论是 IM 即时通讯、音乐播放、IoT 设备监控还是健康运动追踪，Fw 都能通过灵活的配置项为你的应用量身定制防杀进程方案，让应用在后台保持运行不被系统回收。已发布至 Maven Central，开箱即用。

---

## 什么是 Android 保活？

在 Android 系统中，当用户切换到其他应用或锁屏后，系统会根据内存压力和电池策略自动回收后台进程。所谓 **Android 保活**（也称为"后台常驻""进程保护""后台保持运行""防杀进程"），是指通过一系列技术手段让应用在后台持续运行、不被系统或厂商的资源管理机制杀死的技术。

**为什么需要保活？**

- **即时通讯 (IM)**：微信、钉钉等 App 需要在后台实时接收消息推送，一旦进程被杀，用户将无法收到新消息通知。
- **音乐/音频播放**：用户期望锁屏后音乐继续播放，进程被回收意味着播放中断。
- **IoT 设备控制**：智能家居、蓝牙外设等场景需要应用持续保持与设备的连接。
- **健康与运动追踪**：步数统计、心率监测、GPS 轨迹记录等功能需要后台持续采集数据。
- **定位与导航服务**：实时定位、地理围栏、物流追踪等需要应用在后台不间断运行。

然而，从 Android 8.0 开始，系统对[后台执行](https://developer.android.com/about/versions/oreo/background)施加了越来越严格的限制；各厂商（小米 MIUI、华为 EMUI、OPPO ColorOS、vivo Funtouch）还额外叠加了自家的省电策略和自启动管理。普通开发者如果不借助系统化的后台保持运行方案，应用进程极容易在后台被杀。

**Fw 框架正是为解决这一问题而生** —— 它将市面上所有已知的后台常驻技术（前台服务、双进程守护、Native 守护、MediaRoute、VPN 保护、通知豁免等）整合为一个开箱即用的 SDK，让开发者通过一行代码就能获得商业级的进程保护能力。

---

> **一行代码接入 35+ 种后台保持运行策略，覆盖 Android 7.0 - 16 全版本，适配 10+ 厂商 ROM。**
>
> 完整复现市面上所有商业应用的防杀进程机制——前台服务、双进程守护、Native C++ fork 守护、MediaRoute 媒体路由（酷狗音乐核心方案）、账户同步、[JobScheduler](https://developer.android.com/reference/android/app/job/JobScheduler)、[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)、[AlarmManager](https://developer.android.com/reference/android/app/AlarmManager)、1 像素 Activity、静默音频、悬浮窗、[无障碍服务](https://developer.android.com/guide/topics/ui/accessibility/service)、通知监听、蓝牙/WiFi/USB/NFC 广播唤醒、ContentObserver、FileObserver、Binder 直调 AMS 防强停……穷尽展示所有进程保护手段。
>
> 为了拉齐全网共同认知，让小团队开发不再迷茫，开源了全套所有私密函数和私密策略。会长期持续迭代，会陆陆续续的公开所有的隐私策略，ecpm 策略等等，欢迎 star 持续关注。

---

## v2.0 重大升级 — 8 大全新保活策略

> **2026 年 4 月发布**，策略总数从 27 到 35+，覆盖 Android 8.0 - 16 的最新系统特性。这是本项目自开源以来最大规模的一次更新。

### 新增策略一览

| # | 策略 | 类名 | 核心原理 | API 要求 | 有效性 |
|---|------|------|---------|---------|-------|
| 1 | **VPN 系统级保活** | `FwVpnService` | 本地回环 VPN，[BIND_VPN_SERVICE](https://developer.android.com/reference/android/net/VpnService) 绑定保护 | 24+ | 极高 |
| 2 | **伴侣设备后台常驻** | `FwCompanionService` | [CompanionDeviceService](https://developer.android.com/reference/android/companion/CompanionDeviceService)，BLE 设备关联 | 31+ | 高 |
| 3 | **CallStyle 通知豁免** | `FwCallStyleManager` | [Notification.CallStyle](https://developer.android.com/reference/android/app/Notification.CallStyle)，无需 POST_NOTIFICATIONS | 31+ | 极高 |
| 4 | **MediaSession 通知豁免** | `MediaSessionNotificationManager` | [MediaSession](https://developer.android.com/reference/android/media/session/MediaSession) + MediaStyle，Android 13+ 豁免机制 | 33+ | 极高 |
| 5 | **设备管理员进程保护** | `FwDeviceAdminService` | [Device Owner](https://developer.android.com/work/dpc/build-dpc) 模式，系统自动绑定 | 26+ | 极高 |
| 6 | **Quick Settings 磁贴** | `FwTileService` | [TileService](https://developer.android.com/reference/android/service/quicksettings/TileService) 下拉通知栏磁贴，用户交互触发 | 24+ | 高 |
| 7 | **桌面小组件** | `FwWidgetProvider` | [AppWidgetProvider](https://developer.android.com/guide/topics/appwidgets)，30 分钟系统级定时唤醒 | 24+ | 高 |
| 8 | **屏保防杀进程** | `FwDreamService` | [DreamService](https://developer.android.com/reference/android/service/dreams/DreamService) 充电待机时系统自动激活 | 24+ | 高 |

### 新策略详解

#### 1. VPN 系统级保活 — `FwVpnService`

通过建立本地回环 VPN 隧道（127.0.0.1 -> 127.0.0.1），获得系统级 [`BIND_VPN_SERVICE`](https://developer.android.com/reference/android/net/VpnService) 绑定保护。VPN 服务是 Android 系统中进程优先级最高的服务之一，几乎不会被系统杀死。

- **核心机制**：VpnService 建立虚拟网络接口，所有流量通过本地回环，不影响正常网络
- **进程优先级**：与系统 VPN 服务同级，OOM adj 值极低
- **用户感知**：状态栏显示 VPN 图标（可配合通知引导用户理解）
- **默认关闭**：需用户手动授权 VPN 权限

#### 2. 伴侣设备后台常驻 — `FwCompanionService`

利用 Android 12+ 的 [`CompanionDeviceService`](https://developer.android.com/reference/android/companion/CompanionDeviceService) API，通过 BLE 设备关联获得 `REQUEST_COMPANION_RUN_IN_BACKGROUND` 和 `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` 两大系统级豁免权限。

- **核心机制**：注册为伴侣设备应用，获得后台运行豁免
- **系统绑定**：配对设备在附近时，系统自动绑定服务
- **默认关闭**：需要 BLE 设备配对

#### 3. CallStyle 通知豁免 — `FwCallStyleManager`

注册为自管理通话应用（Self-Managed Connection），通过 [`Notification.CallStyle`](https://developer.android.com/reference/android/app/Notification.CallStyle) 发送通话样式通知。Android 系统对通话通知有特殊豁免——**无需 POST_NOTIFICATIONS 权限即可显示通知**。

- **核心机制**：[ConnectionService](https://developer.android.com/reference/android/telecom/ConnectionService) + PhoneAccount + Notification.CallStyle
- **通知豁免**：通话通知不受 Android 13+ 通知权限限制
- **进程保护**：通话进程优先级极高
- **默认关闭**：侵入性较强（会显示通话样式通知）

#### 4. MediaSession 通知豁免 — `MediaSessionNotificationManager`

这是 Android 13+ 上绕过 [`POST_NOTIFICATIONS`](https://developer.android.com/develop/ui/views/notifications/notification-permission) 权限限制的核心策略。通过创建活跃的 `MediaSession` 并使用 `MediaStyle` 通知，系统将应用识别为正在播放媒体的应用，自动豁免通知权限要求。

- **核心机制**：MediaSessionCompat + NotificationCompat.MediaStyle
- **权限豁免**：Android 13+ 无需 POST_NOTIFICATIONS 即可显示通知
- **商业应用实践**：酷狗音乐、QQ 音乐、网易云音乐等均使用此机制
- **默认开启**：侵入性低，推荐使用

#### 5. 设备管理员进程保护 — `FwDeviceAdminService`

当应用被设置为 [Device Owner](https://developer.android.com/work/dpc/build-dpc) 时，系统会自动绑定 `DeviceAdminService`，使进程获得极高优先级。此服务完全由系统管理生命周期，无需手动启动。

- **核心机制**：[DeviceAdminService](https://developer.android.com/reference/android/app/admin/DeviceAdminService)（API 26+），系统自动绑定
- **激活方式**：通过 `adb shell dpm set-device-owner` 或 NFC provisioning
- **适用场景**：企业 MDM、自有设备管理
- **默认关闭**：需要设备管理员权限

#### 6. Quick Settings 磁贴 — `FwTileService`

在下拉通知栏[快速设置](https://developer.android.com/reference/android/service/quicksettings/TileService)中添加自定义磁贴。每次用户下拉通知栏、点击磁贴，都会触发保活检查。磁贴显示保活状态（活跃/休眠）。

- **核心机制**：TileService，系统级 UI 组件
- **触发方式**：用户下拉通知栏或点击磁贴
- **默认开启**：用户需手动添加磁贴到通知栏

#### 7. 桌面小组件 — `FwWidgetProvider`

利用 [`AppWidgetProvider`](https://developer.android.com/guide/topics/appwidgets) 在桌面显示保活状态小组件。系统每 30 分钟自动调用 `onUpdate()`，触发保活检查。

- **核心机制**：AppWidgetProvider + 30 分钟 updatePeriodMillis
- **系统保证**：即使应用被杀，系统也会按时触发 onUpdate
- **默认开启**：用户需手动添加小组件到桌面

#### 8. 屏保防杀进程 — `FwDreamService`

利用 Android 的[屏保（Dream）框架](https://developer.android.com/reference/android/service/dreams/DreamService)，在设备充电且空闲时系统自动激活屏保服务，触发保活检查。

- **核心机制**：DreamService，系统自动管理
- **触发条件**：设备充电 + 空闲（用户可在设置中选择屏保）
- **默认开启**：需用户在系统设置中选择此屏保

---

## 与同类项目对比

| 特性 | **Fw (本项目)** | MarsDaemon | Leoric | 其他方案 |
|------|:-----------:|:----------:|:------:|:--------:|
| 策略数量 | **35+** | 2-3 | 3-5 | 1-5 |
| Native C++ 守护 | 支持 | 支持 | 支持 | 不支持 |
| MediaRoute 后台常驻 | 支持 | 不支持 | 不支持 | 不支持 |
| VPN 系统级保活 | 支持 | 不支持 | 不支持 | 不支持 |
| CompanionDevice 防杀进程 | 支持 | 不支持 | 不支持 | 不支持 |
| 通知权限豁免 (2 种) | 支持 | 不支持 | 不支持 | 不支持 |
| 设备管理员保活 | 支持 | 不支持 | 不支持 | 不支持 |
| Quick Settings 磁贴 | 支持 | 不支持 | 不支持 | 不支持 |
| 桌面小组件 | 支持 | 不支持 | 不支持 | 不支持 |
| 屏保后台保持运行 | 支持 | 不支持 | 不支持 | 不支持 |
| Android 16 适配 | 支持 | 不支持 | 不支持 | 不支持 |
| Maven Central 一行集成 | 支持 | 不支持 | 不支持 | 不支持 |
| 厂商 ROM 适配 | **10+ 厂商** | 有限 | 有限 | 无 |
| 持续维护 (2026) | 活跃 | 已停更 | 已停更 | 不定 |

> MarsDaemon（2018 年停更）和 Leoric（2020 年停更）是 Android 后台常驻领域的先驱，但由于 Android 系统不断加强后台限制，这些项目已无法适配新版本系统。**Fw 是目前唯一持续维护并覆盖 Android 16 的开源后台保持运行框架。**

---

## 快速集成

### Step 1：添加依赖

```kotlin
// build.gradle.kts (Kotlin DSL)
dependencies {
    implementation("io.github.pangu-immortal:keeplive-framework:2.0.0")
}
```

```groovy
// build.gradle (Groovy DSL)
dependencies {
    implementation 'io.github.pangu-immortal:keeplive-framework:2.0.0'
}
```

> 仓库说明：本库已发布到 **Maven Central**，项目默认的 `mavenCentral()` 仓库即可拉取，**无需额外配置仓库地址**。

### Step 2：一行代码启动

```kotlin
// Application.onCreate()
Fw.init(this)
```

搞定。35+ 种后台保持运行策略全部自动启用，无需额外配置。

### Step 3（可选）：精细控制

```kotlin
Fw.init(this) {
    enableForegroundService = true       // 前台服务（核心）
    enableDualProcess = true             // 双进程守护
    enableNativeDaemon = true            // Native C++ 守护进程
    enableMediaRouteProvider = true      // MediaRoute 保活（酷狗方案）
    enableSilentAudio = true             // 静默音频
    aggressiveLevel = AggressiveLevel.MEDIUM  // 能耗等级：LOW/MEDIUM/HIGH
    enableForceStopResistance = false    // 防强停（侵入性强，按需开启）
    // ... 50+ 可配置项，详见下方完整配置
}
```

### 运行时控制

```kotlin
Fw.check()           // 手动触发防杀进程检查
Fw.stop()            // 停止所有后台常驻策略
Fw.isInitialized()   // 查询框架状态
```

![Pangu-Immortal 微信二维码](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

**Telegram 群组**： [点击加群讨论，这里只是冰山一角。](https://t.me/+V7HSo1YNzkFkY2M1)

---

## 项目简介

Fw（Framework）是一个模块化的 Android 进程保护框架，也是目前开源社区最完整的 **Android 保活技术百科全书**。项目完整复现了市面上所有商业应用（酷狗音乐、墨迹天气、QQ 音乐等）的后台保持运行技术，采用 Kotlin + Native C++17 双层架构，通过 `Fw.init()` 一行代码即可启用全部 35+ 种防杀进程策略。

所有策略通过 `FwConfig` 的 50+ 配置项独立控制开关，`ServiceStarter` 作为唯一拉起汇聚点，`RestartProtection` 防止无限重启耗电。最多同时运行 5 个进程（主进程 + :daemon + :assist1 + :assist2 + :assist3）形成环形互保。

**核心特性：**

- **一行代码集成** — `implementation("io.github.pangu-immortal:keeplive-framework:2.0.0")`
- **模块化设计** — 35+ 种策略独立开关，50+ 配置项精细控制
- **Native C++ 层** — fork 守护进程、Socket 心跳、文件锁互监控、Binder 直调 AMS
- **全版本适配** — Android 7.0 - 16（API 24 - 36），包括 [16KB 页面大小](https://developer.android.com/guide/practices/page-sizes)
- **全厂商覆盖** — 小米、华为、OPPO、vivo、三星、魅族、一加等 10+ 厂商，16 个自启动管理 Intent
- **商业级方案** — 酷狗音乐 MediaRoute、墨迹天气锁屏、QQ 音乐静默音频等核心后台常驻技术
- **通知权限豁免** — MediaSession + CallStyle 双重豁免 POST_NOTIFICATIONS 权限
- **VPN 系统级保护** — 本地回环 VPN，系统级 BIND_VPN_SERVICE 绑定保护
- **厂商分析工具** — 检测目标应用的推送 SDK 和进程保护机制
- **生产级质量** — 通过 Lint 检查、ProGuard 混淆优化、Google Play Ready

### **Star 这个项目如果对你有帮助！**

---

## 开发环境

| 项目 | 版本 |
|-----|------|
| Android Studio | Android Studio Otter 2 Feature Drop 2025.2.2 |
| Gradle | 9.4.1 |
| AGP (Android Gradle Plugin) | 9.1.0 |
| Kotlin | 2.3.20 |
| JVM | 21 |
| NDK | 27.0.12077973 |
| CMake | 3.22.1 |
| compileSdk | 36 (Android 16) |
| targetSdk | 36 |
| minSdk | 24 (Android 7.0) |

---

## Android 版本适配

| Android 版本 | API   | 适配要点 |
|-------------|-------|---------|
| 7.x | 24-25 | `startService()` |
| 8.0+ | 26+   | [`startForegroundService()`](https://developer.android.com/about/versions/oreo/background) + 通知渠道，静态广播受限 |
| 9.0+ | 28+   | [后台限制加强](https://developer.android.com/about/versions/pie/power) |
| 10+ | 29+   | [后台启动 Activity 受限](https://developer.android.com/guide/components/activities/background-starts) |
| 11+ | 30+   | [前台服务类型必须声明](https://developer.android.com/about/versions/11/privacy/foreground-services) |
| 12+ | 31+   | `BLUETOOTH_CONNECT` 运行时权限，[精确闹钟权限](https://developer.android.com/about/versions/12/behavior-changes-12#exact-alarm-permission)，CompanionDeviceService 可用 |
| 13+ | 33+   | [`POST_NOTIFICATIONS` 运行时权限](https://developer.android.com/develop/ui/views/notifications/notification-permission)，**MediaSession 通知豁免生效** |
| 14+ | 34+   | [`FOREGROUND_SERVICE_MEDIA_PLAYBACK`](https://developer.android.com/about/versions/14/changes/fgs-types-required) 权限 |
| 15+ | 35+   | 更严格的后台限制，**[16KB 页面大小](https://developer.android.com/guide/practices/page-sizes)设备支持** |
| 16 | 36 | 最新 API |

### Android 16K 页面大小适配

从 Android 15 开始，部分设备使用 [16KB 页面大小](https://developer.android.com/guide/practices/page-sizes)（而非传统的 4KB）。本项目已完成 16K 适配：

```cmake
# CMakeLists.txt 中添加
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
```

```bash
# 验证 ELF 对齐
llvm-readelf -l libfw_native.so | grep LOAD
# 输出应显示对齐值为 0x4000 (16384 = 16KB)
```

---

## 完整配置参考

> `FwConfig` 全部 50+ 配置项，按策略分类。快速上手请看 [快速集成](#快速集成)。

```kotlin
Fw.init(this) {
    // ==================== 基础策略 ====================
    enableForegroundService = true      // 前台服务（核心）
    enableMediaSession = true           // MediaSession（让系统认为是媒体应用）
    enableOnePixelActivity = true       // 1像素Activity

    // ==================== 定时唤醒策略 ====================
    enableJobScheduler = true           // JobScheduler
    jobSchedulerInterval = 15 * 60 * 1000L  // 15分钟
    enableWorkManager = true            // WorkManager
    workManagerIntervalMinutes = 15L    // 15分钟
    enableAlarmManager = true           // AlarmManager
    alarmManagerInterval = 5 * 60 * 1000L   // 5分钟

    // ==================== 账户同步策略 ====================
    enableAccountSync = true            // 账户同步
    syncIntervalSeconds = 60L           // 60秒

    // ==================== 广播策略 ====================
    enableSystemBroadcast = true        // 系统广播
    enableBluetoothBroadcast = true     // 蓝牙广播（核心：酷狗音乐的关键）
    enableMediaButtonReceiver = true    // 媒体按键
    enableUsbBroadcast = true           // USB 广播
    enableNfcBroadcast = true           // NFC 广播
    enableMediaMountBroadcast = true    // 媒体挂载广播

    // ==================== 内容观察者策略 ====================
    enableMediaContentObserver = true   // 相册变化
    enableContactsContentObserver = false // 联系人变化（需要权限）
    enableSmsContentObserver = false    // 短信变化（需要权限）
    enableSettingsContentObserver = true // 设置变化
    enableFileObserver = true           // 文件系统变化

    // ==================== 双进程守护策略 ====================
    enableDualProcess = true            // Java 双进程守护

    // ==================== Native 层保活 ====================
    enableNativeDaemon = true           // Native 守护进程
    nativeDaemonCheckInterval = 3000    // 检查间隔 3 秒
    enableNativeSocket = true           // Socket 心跳
    nativeSocketName = "fw_native_socket"

    // ==================== 无法强制停止策略 ====================
    enableForceStopResistance = false   // 默认关闭（侵入性强，仅 Android 5.0-12.0 有效）

    // ==================== MediaRoute 保活策略 ====================
    enableMediaRouteProvider = true     // MediaRouteProviderService
    enableMediaRoute2Provider = true    // MediaRoute2ProviderService (Android 11+)
    enableMediaIntentActivity = true    // 媒体意图处理 Activity

    // ==================== 静默音频策略 ====================
    enableSilentAudio = true            // 静默音频播放（防止 CPU 休眠）
    aggressiveLevel = AggressiveLevel.MEDIUM // 能耗等级：LOW/MEDIUM/HIGH

    // ==================== v2.0 新增策略 ====================
    enableVpnService = false            // VPN 保活（默认关闭，需用户授权 VPN 权限）
    enableCompanionDevice = false       // 伴侣设备保活（默认关闭，需 BLE 设备配对）
    enableCallStyleNotification = false // CallStyle 通知豁免（默认关闭，侵入性强）
    enableMediaSessionNotification = true // MediaSession 通知豁免（默认开启，推荐使用）
    enableDeviceAdmin = false           // 设备管理员（默认关闭，需手动激活）
    enableTileService = true            // Quick Settings 磁贴（默认开启）
    enableWidget = true                 // 桌面小组件（默认开启，30分钟唤醒）
    enableDreamService = true           // 屏保保活（默认开启，充电待机时激活）

    // ==================== 高级侵入性策略 ====================
    enableLockScreenActivity = false    // 锁屏 Activity（默认关闭）
    enableFloatWindow = false           // 悬浮窗（默认关闭）
    floatWindowHidden = true            // 隐藏悬浮窗（1像素）

    // ==================== 通知配置 ====================
    notificationChannelId = "fw_channel"
    notificationChannelName = "守护服务"
    notificationTitle = "音乐播放中"
    notificationContent = "点击打开应用"
    notificationIconResId = R.drawable.ic_notification
    notificationActivityClass = MainActivity::class.java

    // ==================== 日志配置 ====================
    enableDebugLog = true
    logTag = "Fw"
}
```

### 高级控制 API

```kotlin
// 基础控制
Fw.check()           // 手动触发后台常驻检查
Fw.stop()            // 停止所有进程保护策略
Fw.isInitialized()   // 查询框架状态

// 锁屏 Activity（类似墨迹天气的锁屏天气）
LockScreenActivity.start(context)

// 悬浮窗保活
FloatWindowManager.showOnePixelFloat(context)  // 隐藏的 1 像素
FloatWindowManager.showVisibleFloat(context)    // 可见的悬浮球

// 电池优化豁免
BatteryOptimizationManager.requestIgnoreBatteryOptimizations(context)

// 打开厂商自启动设置
AutoStartPermissionManager.openAutoStartSettings(context)

// 厂商集成分析（分析目标应用的防杀进程机制）
VendorIntegrationAnalyzer.getFullAnalysisReport(context, "com.moji.mjweather")
```

---

## 保活策略完整列表

### 1. 基础策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| [前台服务](https://developer.android.com/develop/background-work/services/foreground-services) | `FwForegroundService` | `foregroundServiceType="mediaPlayback"`，系统认为是媒体应用 | 极高 |
| MediaSession | `MediaSessionManager` | 创建[媒体会话](https://developer.android.com/guide/topics/media-apps/working-with-a-media-session)，获得系统特殊保护 | 极高 |
| 1 像素 Activity | `OnePixelActivity` | 屏幕关闭时启动透明 Activity，提升进程优先级 | 高 |
| 锁屏 Activity | `LockScreenActivity` | 在锁屏界面显示（如锁屏天气），保持前台状态 | 极高 |
| 悬浮窗 | `FloatWindowManager` | 1 像素悬浮窗或悬浮球，系统认为应用在使用中 | 高 |

### 2. 定时唤醒策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| [JobScheduler](https://developer.android.com/reference/android/app/job/JobScheduler) | `FwJobService` | 系统级任务调度，最小间隔 15 分钟 | 高 |
| [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | `FwWorker` | Jetpack 任务调度，兼容性好 | 高 |
| [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager) | `AlarmStrategy` | 精确闹钟唤醒，需要 `SCHEDULE_EXACT_ALARM` 权限 | 中 |

### 3. 账户同步策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| [SyncAdapter](https://developer.android.com/training/sync-adapters) | `FwSyncAdapter` | 账户同步机制，系统会定期触发同步 | 高 |
| AccountAuthenticator | `FwAuthenticator` | 账户认证服务，配合 SyncAdapter 使用 | 高 |

### 4. 广播监听策略（静态注册）

| 策略 | 类名 | 监听的广播 | 有效性 |
|-----|------|----------|-------|
| 蓝牙广播 | `BluetoothReceiver` | ACL_CONNECTED, A2DP, HEADSET, AUDIO_BECOMING_NOISY | 极高 |
| USB 广播 | `UsbReceiver` | USB_DEVICE_ATTACHED, USB_ACCESSORY_ATTACHED | 高 |
| NFC 广播 | `NfcReceiver` | TAG_DISCOVERED, TECH_DISCOVERED, NDEF_DISCOVERED | 高 |
| 媒体按键 | `MediaButtonReceiver` | MEDIA_BUTTON（蓝牙耳机按键） | 高 |
| 媒体挂载 | `MediaMountReceiver` | MEDIA_MOUNTED, MEDIA_EJECT, MEDIA_SCANNER | 高 |
| 系统事件 | `SystemEventReceiver` | BOOT_COMPLETED, MY_PACKAGE_REPLACED | 极高 |

### 5. 内容观察者策略

| 策略 | 类名 | 监听内容 | 有效性 |
|-----|------|---------|-------|
| 相册变化 | `ContentObserverManager` | MediaStore.Images, Videos, Audio | 中 |
| 联系人变化 | `ContentObserverManager` | ContactsContract | 中 |
| 短信变化 | `ContentObserverManager` | Telephony.Sms | 中 |
| 文件系统 | `FileObserverManager` | Download, DCIM, Screenshots, Documents | 中 |

### 6. 系统级服务策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| [无障碍服务](https://developer.android.com/guide/topics/ui/accessibility/service) | `FwAccessibilityService` | 系统级服务，优先级最高，需用户手动开启 | 极高 |
| [通知监听服务](https://developer.android.com/reference/android/service/notification/NotificationListenerService) | `FwNotificationListenerService` | 系统级服务，被杀后系统自动重启 | 极高 |

### 7. 双进程守护策略

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| Java 双进程 | `DaemonService` | 独立进程 `:daemon`，互相守护 | 高 |
| Native 守护进程 | `FwNative` | C++ fork() 子进程监控，使用 am 命令重启 | 高 |
| Socket 心跳 | `FwNative` | Unix Domain Socket 进程间通信 | 中 |

### 8. MediaRoute 后台常驻策略

这是**酷狗音乐**等应用的核心防杀进程技术之一。通过向系统注册虚拟媒体路由，获得"媒体类应用"身份，系统会维护服务连接，不容易被杀死。

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| [MediaRouteProviderService](https://developer.android.com/reference/androidx/mediarouter/media/MediaRouteProviderService) | `FwMediaRouteProviderService` | 向 MediaRouter 注册媒体路由提供者，系统维护 Binder 连接 | 极高 |
| MediaRoute2ProviderService | `FwMediaRoute2ProviderService` | Android 11+ 新 MediaRouter2 API，双重后台常驻 | 极高 |
| MediaRoute Native | `FwMediaRouteNative` | C++ 层服务状态监控、心跳检测、WakeLock 管理 | 高 |
| MediaRoute Manager | `FwMediaRouteManager` | 统一管理模块生命周期，根据系统版本选择性启动 | 高 |

**核心机制：**

```
┌─────────────────────────────────────────────────────────────────┐
│                    系统 MediaRouter 服务                         │
│   ┌───────────────────┐     ┌───────────────────────┐          │
│   │ MediaRouter (旧版) │     │ MediaRouter2 (Android 11+) │      │
│   └────────┬──────────┘     └───────────┬───────────┘          │
└────────────┼────────────────────────────┼───────────────────────┘
             │ Binder 连接                 │ Binder 连接
             ▼                             ▼
┌────────────────────────┐    ┌─────────────────────────────┐
│ FwMediaRouteProvider   │    │ FwMediaRoute2ProviderService │
│ Service                │    │                             │
│  - 注册虚拟媒体路由      │    │  - 发布 MediaRoute2 路由     │
│  - 30秒心跳检查         │    │  - 30秒心跳检查              │
│  - WakeLock 管理        │    │  - 触发保活检查              │
└────────────────────────┘    └─────────────────────────────┘
```

### 9. 静默音频后台保持运行策略

通过 MediaPlayer 循环播放无声音频文件，防止 CPU 休眠。音量设为 0，用户完全无感知。参考**酷狗音乐**、**QQ音乐**等主流音乐 App 的进程保护方案。

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 静默音频播放 | `SilentAudioStrategy` | 循环播放 1 秒无声 WAV（仅 8KB），防止 CPU 休眠 | 极高 |
| 能耗等级控制 | `AggressiveLevel` | LOW/MEDIUM/HIGH 三档，控制重播间隔和策略激进度 | 高 |

**能耗等级说明：**

| 等级 | 静默音频间隔 | AlarmManager 间隔 | 守护进程检查间隔 | 适用场景 |
|-----|-----------|------------------|--------------|---------|
| `LOW` | 10 秒 | 10 分钟 | 10 秒 | 普通后台任务，最省电 |
| `MEDIUM` | 5 秒 | 5 分钟 | 3 秒 | 均衡模式（默认） |
| `HIGH` | 立即重播 | 1 分钟 | 1 秒 | 即时通讯类，最大化后台常驻 |

### 10. 连续重启保护

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 重启频率监控 | `RestartProtection` | 60 秒内重启超过 10 次触发 5 分钟冷却期 | 高 |

### 11. BIND_ABOVE_CLIENT 绑定

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 高优先级绑定 | `DaemonService` | 守护进程以 [`BIND_ABOVE_CLIENT`](https://developer.android.com/reference/android/content/Context#BIND_ABOVE_CLIENT) 绑定主服务，告诉系统被绑定服务比客户端更重要 | 高 |

### 12. 无法强制停止策略

原理介绍，阅读地址：https://mp.weixin.qq.com/s/-9L6XOfrzh69hOQ9puK6iQ

| 策略 | 类名 | 说明 | 有效性 |
|-----|------|------|-------|
| 多进程文件锁监控 | `ForceStopResistance` | 多个辅助进程通过文件锁互相监控 | 高 |
| app_process 拉活 | `AppProcessLauncher` | 通过 app_process 命令启动 Java 进程 | 高 |
| AMS Binder 直接调用 | `AmsBinderInvoker` | 直接调用 AMS Binder 启动服务/广播/Instrumentation | 极高 |
| Instrumentation 拉活 | `FwInstrumentation` | 通过 am instrument 命令拉起进程 | 高 |

> **适用范围**：Android 5.0 - 12.0 (API 21 - 31) 测试有效。Android 13 开始下发补丁，实测大部分设备仍可用。
>
> **默认关闭**：侵入性较强，需手动启用。

<details>
<summary><b>展开：5ms 时间差竞争核心原理</b></summary>

#### 核心原理：5ms 时间差竞争

无法强制停止的关键在于 **杀死与唤醒的时间差**。当用户点击「强制停止」时，系统需要逐个杀死应用的所有进程，每个进程之间存在约 **5ms** 的时间间隔。

**为什么存在时间差？**

Android 系统强制停止应用时的执行流程：

1. AMS 遍历应用的所有进程
2. 对每个进程调用 `Process.killProcess()` 或发送 `SIGKILL`
3. 等待进程死亡确认
4. 继续杀死下一个进程

这个「遍历 -> 杀死 -> 确认」的循环需要时间，每个进程大约 **5ms**。如果应用有 4 个进程（主进程 + 3 个辅助进程），总时间窗口约 **15-20ms**。

**为什么 5ms 足够？**

| 方式 | 调用链 | 耗时 |
|-----|-------|-----|
| Java `startService()` | Intent 创建 -> Parcel 序列化 -> Binder 代理 -> AMS 处理 | **10-50ms** |
| C++ 直接 Binder | `open("/dev/binder")` -> 预构造 Parcel -> `ioctl()` 发送 | **< 1ms** |

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

| 步骤 | 操作 | 耗时 | 说明 |
|-----|------|-----|------|
| 1 | 检测进程死亡 | ~0ms | flock() 阻塞等待，锁释放立即返回 |
| 2 | 发送 Binder 请求 | <1ms | 预构造 Parcel + 直接 ioctl() |
| 3 | AMS 处理请求 | ~5ms | 系统创建新进程 |
| **总计** | **从检测到拉活** | **<6ms** | **小于进程杀死间隔（~5ms）** |

#### Native 层实现（C++）

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

</details>

### 13. 进程优先级管理

| 功能 | 类名 | 说明 |
|-----|------|------|
| 进程状态监控 | `ProcessPriorityManager` | 获取当前进程 importance、OOM adj 值 |
| 被杀风险评估 | `ProcessPriorityManager` | 评估进程被系统杀死的风险等级 |
| 内存信息获取 | `ProcessPriorityManager` | 获取系统和应用内存使用情况 |

### 14. 厂商集成策略

| 功能 | 类名 | 说明 |
|-----|------|------|
| [电池优化豁免](https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases) | `BatteryOptimizationManager` | 请求加入 Doze 白名单 |
| 厂商自启动管理 | `AutoStartPermissionManager` | 打开各厂商的自启动设置页面 |
| 厂商集成分析 | `VendorIntegrationAnalyzer` | 分析应用的推送 SDK 和系统权限 |

### 15. v2.0 新增后台保持运行策略

> 以下 8 种策略均为 v2.0 版本新增，覆盖 Android 8.0 - 16 的最新系统特性。详细原理说明见 [v2.0 重大升级](#v20-重大升级--8-大全新保活策略) 章节。

| 策略 | 类名 | 说明 | API | 默认 | 有效性 |
|-----|------|------|-----|------|-------|
| VPN 系统级保活 | `FwVpnService` | 本地回环 VPN，BIND_VPN_SERVICE 绑定保护 | 24+ | 关闭 | 极高 |
| 伴侣设备后台常驻 | `FwCompanionService` | CompanionDeviceService，BLE 设备关联 | 31+ | 关闭 | 高 |
| CallStyle 通知豁免 | `FwCallStyleManager` | Notification.CallStyle，绕过 POST_NOTIFICATIONS | 31+ | 关闭 | 极高 |
| MediaSession 通知豁免 | `MediaSessionNotificationManager` | MediaSession + MediaStyle，Android 13+ 豁免 | 33+ | **开启** | 极高 |
| 设备管理员进程保护 | `FwDeviceAdminService` | Device Owner 模式，系统自动绑定 | 26+ | 关闭 | 极高 |
| Quick Settings 磁贴 | `FwTileService` | 下拉通知栏磁贴，用户交互触发 | 24+ | **开启** | 高 |
| 桌面小组件 | `FwWidgetProvider` | 30 分钟系统级定时唤醒 | 24+ | **开启** | 高 |
| 屏保防杀进程 | `FwDreamService` | 充电待机时系统自动激活 | 24+ | **开启** | 高 |

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
        Fw.init(this) // 初始化保活框架

        // 初始化厂商推送（根据设备自动选择）
        when {
            isMiui() -> MiPushClient.registerPush(this, APP_ID, APP_KEY)
            isEmui() -> HmsMessaging.getInstance(this).isAutoInitEnabled = true
            isColorOS() -> HeytapPushManager.init(this, true)
            isFuntouchOS() -> PushClient.getInstance(this).initialize()
            else -> Firebase.messaging.isAutoInitEnabled = true // FCM
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

### FCM 数据消息 (Data Message) 模式

> 只有 data 模式可以拉活。

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.isNotEmpty().let {
            Fw.check() // 收到推送时触发保活检查
        }
    }
}
```

```json
// 服务端发送 data 消息
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

> `priority` 设为 `high` 获得最及时传递（[Doze 模式](https://developer.android.com/training/monitoring-device-state/doze-standby)下也能唤醒）。消息体只包含 `data` 字段，不包含 `notification` 字段。

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
├── framework/                     # 保活框架核心模块
│   └── src/main/
│       ├── java/com/service/framework/
│       │   ├── Fw.kt                        # 框架入口（一行代码初始化）
│       │   ├── core/
│       │   │   ├── FwConfig.kt              # 配置类（50+ 配置项）
│       │   │   └── AggressiveLevel.kt       # 能耗等级枚举
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
│       │   │   ├── LockScreenActivity.kt    # 锁屏 Activity
│       │   │   ├── FloatWindowManager.kt    # 悬浮窗管理
│       │   │   ├── SilentAudioStrategy.kt   # 静默音频保活
│       │   │   ├── BatteryOptimizationManager.kt  # 电池优化管理
│       │   │   ├── VendorIntegrationAnalyzer.kt   # 厂商集成分析
│       │   │   ├── FwAccessibilityService.kt      # 无障碍服务
│       │   │   ├── FwNotificationListenerService.kt # 通知监听服务
│       │   │   ├── ProcessPriorityManager.kt # 进程优先级管理
│       │   │   ├── FwVpnService.kt          # VPN 系统级保活
│       │   │   ├── FwCompanionService.kt    # 伴侣设备后台常驻
│       │   │   ├── FwCallStyleManager.kt    # CallStyle 通知豁免
│       │   │   ├── MediaSessionNotificationManager.kt # MediaSession 通知豁免
│       │   │   ├── FwDeviceAdminReceiver.kt # 设备管理员接收器
│       │   │   ├── FwDeviceAdminService.kt  # 设备管理员服务
│       │   │   ├── FwTileService.kt         # Quick Settings 磁贴
│       │   │   ├── FwWidgetProvider.kt      # 桌面小组件
│       │   │   ├── FwDreamService.kt        # 屏保保活
│       │   │   └── forcestop/               # 无法强制停止策略（C++ 实现）
│       │   │       ├── ForceStopResistance.kt    # 策略入口
│       │   │       ├── AssistService1/2/3.kt     # 辅助进程服务
│       │   │       ├── AmsBinderInvoker.kt       # AMS Binder 直调
│       │   │       ├── AppProcessLauncher.kt     # app_process 拉活
│       │   │       └── FwInstrumentation.kt      # Instrumentation 组件
│       │   ├── mediaroute/                # MediaRoute 保活模块
│       │   │   ├── FwMediaRouteManager.kt       # 模块统一管理器
│       │   │   ├── FwMediaRouteProviderService.kt # MediaRoute 服务
│       │   │   ├── FwMediaRoute2ProviderService.kt # MediaRoute2 服务
│       │   │   ├── FwMediaRouteProvider.kt      # 自定义路由提供者
│       │   │   ├── FwMediaRouteNative.kt        # Native 层 JNI 接口
│       │   │   └── FwMediaActivity.kt           # 媒体意图处理 Activity
│       │   ├── native/
│       │   │   └── FwNative.kt              # Native 层 JNI 接口
│       │   └── util/
│       │       ├── ServiceStarter.kt        # 服务启动器
│       │       ├── RestartProtection.kt     # 连续重启保护
│       │       └── FwLog.kt                 # 日志工具
│       ├── cpp/                             # Native C++ 层
│       │   ├── CMakeLists.txt               # CMake 构建配置
│       │   ├── fw_daemon.cpp                # 守护进程（fork）
│       │   ├── fw_process.cpp               # 进程管理（OOM adj）
│       │   ├── fw_socket.cpp                # Socket 通信
│       │   ├── fw_jni.cpp                   # JNI 入口
│       │   ├── fw_force_stop.cpp            # 无法强制停止核心实现
│       │   ├── binder/                      # Binder 直接调用
│       │   │   ├── common.h
│       │   │   ├── cParcel.cpp/h
│       │   │   └── data_transact.cpp/h
│       │   ├── utils/                       # 工具类
│       │   │   ├── String16.cpp/h
│       │   │   ├── Unicode.cpp/h
│       │   │   └── SharedBuffer.cpp/h
│       │   └── mediaroute/                  # MediaRoute Native 层
│       │       ├── CMakeLists.txt
│       │       └── fw_mediaroute_jni.cpp
│       └── res/
│           ├── raw/
│           │   └── silent.wav               # 1 秒无声音频（8KB）
│           ├── layout/
│           │   └── fw_widget_layout.xml     # 小组件布局
│           ├── drawable/
│           │   └── ic_tile_shield.xml       # 磁贴图标
│           └── xml/
│               ├── authenticator.xml        # 账户认证配置
│               ├── syncadapter.xml          # 同步适配器配置
│               ├── nfc_tech_list.xml        # NFC 技术列表
│               ├── accessibility_service_config.xml # 无障碍服务配置
│               ├── device_admin.xml         # 设备管理员策略
│               └── widget_info.xml          # 小组件配置
│
├── build.gradle.kts               # 根项目构建脚本
├── kill_alive.sh                  # 保活测试脚本（循环强杀验证恢复）
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
| Google (Pixel) | [Doze 模式](https://developer.android.com/training/monitoring-device-state/doze-standby)严格 | 请求 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`，高优先级 FCM |
| 传音 (Tecno) | 内存清理 | 引导用户锁定应用，加入自启动列表 |

```kotlin
// 自动打开当前厂商的自启动设置
AutoStartPermissionManager.openAutoStartSettings(context)
```

---

## 权限说明

### Manifest 权限（自动授予）

```xml
<!-- 前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />

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

<!-- v2.0 新增权限 -->
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
<uses-permission android:name="android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND" />
<uses-permission android:name="android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND" />
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
```

### 运行时权限（需用户授予）

```kotlin
// Android 12+ 蓝牙连接权限
Manifest.permission.BLUETOOTH_CONNECT

// Android 13+ 通知权限（MediaSession/CallStyle 通知豁免可绕过）
Manifest.permission.POST_NOTIFICATIONS

// 存储权限（用于 ContentObserver）
Manifest.permission.READ_MEDIA_IMAGES
Manifest.permission.READ_MEDIA_VIDEO
Manifest.permission.READ_MEDIA_AUDIO

// 联系人 / 短信权限（用于 ContentObserver）
Manifest.permission.READ_CONTACTS
Manifest.permission.READ_SMS

// 悬浮窗权限
Settings.canDrawOverlays(context)

// VPN 权限（v2.0 新增）
VpnService.prepare(context)
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

| 情况 | FLAG_STOPPED | 广播接收器 | 后台常驻效果 |
|-----|-------------|-----------|---------|
| 进程被杀（内存不足） | 不设置 | 可接收 | 可被唤醒 |
| 强制停止（Force Stop） | 设置 | 被禁用 | 无法唤醒 |
| 用户主动杀死（最近任务） | 不设置 | 可接收 | 可被唤醒 |

---

## 构建运行

```bash
# Debug 构建
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release 构建（带时间戳）
./gradlew buildTimestampedReleaseApk
# 输出：release/app-202604101118.apk + mapping 文件
```

### 保活测试

```bash
# 默认：强杀 100 次，间隔 5 秒
./kill_alive.sh

# 自定义参数
./kill_alive.sh 50 3 com.your.package
```

### 查看日志

```bash
adb logcat | grep -E "(Fw|BluetoothReceiver|UsbReceiver|NfcReceiver)"
```

### 检测命令

```bash
# 检查系统白名单
adb shell cat /system/etc/sysconfig/*.xml | grep -i moji

# 检查应用签名/权限/安装路径
adb shell dumpsys package com.moji.mjweather | grep -A5 "signatures"
adb shell pm path com.moji.mjweather
```

---

## 使用场景推荐配置

不同业务场景对后台保持运行的需求不同。以下矩阵给出了 5 种典型场景的推荐策略配置，帮助你快速选择最佳方案。

| 策略配置项 | IM / 即时通讯 | 音乐播放 | IoT 设备监控 | 健康/运动追踪 | 定位服务 |
|-----------|:----------:|:------:|:----------:|:----------:|:------:|
| **aggressiveLevel** | `HIGH` | `MEDIUM` | `MEDIUM` | `LOW` | `MEDIUM` |
| enableForegroundService | 开启 | 开启 | 开启 | 开启 | 开启 |
| enableMediaSession | 开启 | 开启 | 关闭 | 关闭 | 关闭 |
| enableDualProcess | 开启 | 开启 | 开启 | 关闭 | 开启 |
| enableNativeDaemon | 开启 | 关闭 | 开启 | 关闭 | 开启 |
| enableSilentAudio | 开启 | 不需要(有真实音频) | 关闭 | 关闭 | 关闭 |
| enableMediaRouteProvider | 关闭 | 开启 | 关闭 | 关闭 | 关闭 |
| enableVpnService | 可选 | 关闭 | 开启 | 关闭 | 关闭 |
| enableCompanionDevice | 关闭 | 关闭 | 开启 | 开启 | 关闭 |
| enableCallStyleNotification | 关闭 | 关闭 | 关闭 | 关闭 | 关闭 |
| enableMediaSessionNotification | 开启 | 开启 | 关闭 | 关闭 | 关闭 |
| enableJobScheduler | 开启 | 开启 | 开启 | 开启 | 开启 |
| enableWorkManager | 开启 | 开启 | 开启 | 开启 | 开启 |
| enableAlarmManager | 开启 | 关闭 | 开启 | 开启 | 开启 |
| enableAccountSync | 开启 | 关闭 | 关闭 | 关闭 | 关闭 |
| enableWidget | 关闭 | 开启 | 开启 | 开启 | 关闭 |
| enableTileService | 开启 | 开启 | 开启 | 开启 | 开启 |
| enableForceStopResistance | 可选 | 关闭 | 关闭 | 关闭 | 关闭 |
| **核心关注点** | 消息实时性 | 播放不中断 | 连接稳定性 | 数据持续采集 | 轨迹不断点 |
| **推荐理由** | 最高优先级保障消息送达，双进程+Native+静默音频三重守护 | MediaRoute + MediaSession 获得媒体应用身份，系统天然保护 | VPN + 伴侣设备获得系统级绑定，蓝牙保持长连接 | 低能耗为主，伴侣设备获取运动传感器持续访问权 | 前台服务+定时唤醒确保定位不中断，双进程兜底 |

**配置示例 — IM/即时通讯场景：**

```kotlin
Fw.init(this) {
    aggressiveLevel = AggressiveLevel.HIGH
    enableForegroundService = true
    enableMediaSession = true
    enableDualProcess = true
    enableNativeDaemon = true
    enableSilentAudio = true
    enableMediaSessionNotification = true
    enableJobScheduler = true
    enableWorkManager = true
    enableAlarmManager = true
    enableAccountSync = true
    enableTileService = true
}
```

**配置示例 — 音乐播放场景：**

```kotlin
Fw.init(this) {
    aggressiveLevel = AggressiveLevel.MEDIUM
    enableForegroundService = true
    enableMediaSession = true
    enableMediaRouteProvider = true
    enableMediaSessionNotification = true
    enableDualProcess = true
    enableJobScheduler = true
    enableWorkManager = true
    enableWidget = true
    enableTileService = true
}
```

**配置示例 — IoT 设备监控场景：**

```kotlin
Fw.init(this) {
    aggressiveLevel = AggressiveLevel.MEDIUM
    enableForegroundService = true
    enableDualProcess = true
    enableNativeDaemon = true
    enableVpnService = true
    enableCompanionDevice = true
    enableJobScheduler = true
    enableWorkManager = true
    enableAlarmManager = true
    enableWidget = true
    enableTileService = true
}
```

---

## 常见问题

**Q1: 什么是 Android 保活？为什么需要？**

Android 保活（后台常驻/进程保护/防杀进程）是指通过技术手段让应用在后台持续运行、不被系统或厂商的资源管理机制杀死。IM、音乐播放、IoT 设备控制、健康追踪、导航定位等场景都需要应用在后台保持运行，否则用户体验会严重受损——比如收不到消息、音乐中断、设备失联等。

**Q2: 保活和 WorkManager 有什么区别？**

[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) 是 Google 官方推荐的后台任务调度方案，适用于可延迟的、不需要实时性的任务（如日志上传、数据同步）。它不保证任务立即执行，且受系统 Doze 模式影响。而保活框架的目标是让进程持续存活，适用于需要实时响应的场景（如 IM 消息推送、音频播放）。两者互补——Fw 框架内部已集成 WorkManager 作为定时唤醒策略之一，与其他 34 种策略协同工作。

**Q3: Google Play 审核会拒绝保活应用吗？**

Google Play 审核重点关注的是**用户体验**和**资源滥用**，而非保活技术本身。只要你的应用有合理的后台运行理由（如音乐播放、导航、即时通讯），并正确声明了[前台服务类型](https://developer.android.com/about/versions/14/changes/fgs-types-required)和相应权限，就可以通过审核。Fw 框架所有策略均基于 Android 公开 API，badge 区域的 "Google Play Ready" 标识说明框架设计已考虑合规性。建议：避免在没有合理业务理由时开启高侵入性策略（如 VPN、CallStyle 通知）。

**Q4: 框架对 APK 体积有多大影响？**

Fw 框架核心 AAR 体积约 **300-500KB**（含 Native SO 库约 150KB）。静默音频 WAV 文件仅 8KB。由于采用模块化设计，ProGuard/R8 混淆后未使用的策略代码会被自动移除，实际增量通常在 **200-400KB** 之间。对于一个几十 MB 的商业应用来说，影响可以忽略不计。

**Q5: IM/即时通讯场景推荐什么配置？**

IM 场景对消息实时性要求最高，推荐使用 `aggressiveLevel = HIGH`，并同时开启前台服务、MediaSession、双进程守护、Native 守护、静默音频、MediaSession 通知豁免、JobScheduler、WorkManager、AlarmManager、账户同步等策略。详细配置请参考上方[使用场景推荐配置](#使用场景推荐配置)矩阵中的 IM 列。此外，强烈建议集成厂商推送 SDK（小米推送、华为推送等）作为最后一道兜底。

**Q6: 音乐播放场景推荐什么配置？**

音乐播放场景天然属于"媒体类应用"，系统对其后台运行相对宽容。推荐使用 `aggressiveLevel = MEDIUM`，核心开启前台服务（`foregroundServiceType="mediaPlayback"`）、MediaSession、MediaRoute、MediaSession 通知豁免。静默音频无需开启（因为已有真实音频播放）。详见[使用场景推荐配置](#使用场景推荐配置)中的音乐播放列。

**Q7: IoT 设备监控场景推荐什么配置？**

IoT 场景需要保持与蓝牙/WiFi 外设的长连接。推荐 `aggressiveLevel = MEDIUM`，核心开启 VPN 系统级保活（获得系统级绑定保护）和伴侣设备后台常驻（通过 CompanionDeviceService 获取 BLE 持续访问权）。同时开启双进程守护和 Native 守护作为兜底。详见[使用场景推荐配置](#使用场景推荐配置)中的 IoT 列。

**Q8: Flutter/React Native 项目能用吗？**

可以。Fw 框架是一个标准的 Android AAR 库，通过 Maven Central 分发。在 Flutter 项目中，你需要在 `android/app/build.gradle` 中添加依赖并在 `Application` 的 `onCreate` 中调用 `Fw.init(this)`；在 React Native 项目中同理，在 `MainApplication.java/kt` 的 `onCreate` 中初始化即可。框架运行在 Android 原生层，与 Flutter/RN 的 Dart/JS 层互不干扰。

**Q9: v2.0 的通知豁免机制安全吗？会不会被 Google 封杀？**

MediaSession 通知豁免和 CallStyle 通知豁免都是基于 Android 系统**公开且有文档记录的 API**。MediaSession 豁免是 Android 13+ 系统有意为之的设计——为了确保音乐播放器等媒体应用在用户未授予通知权限时仍能显示播放控制通知。酷狗音乐、QQ 音乐、Spotify 等主流应用都在使用此机制。只要你的应用确实有媒体播放功能，使用此豁免就是合规的。CallStyle 通知则需要更谨慎——仅在应用确实有通话功能时才建议开启。

**Q10: 为什么强制停止后应用不能被唤醒？**

强制停止会设置 `FLAG_STOPPED`，导致所有静态广播接收器被禁用。这是 Android 的安全机制，普通手段无法绕过。如果需要在强制停止后仍能恢复，可以考虑开启 `enableForceStopResistance`（仅 Android 5.0-12.0 有效），该策略通过多进程文件锁监控 + C++ Binder 直调 AMS 实现 5ms 时间差竞争拉活。

**Q11: 为什么某些厂商手机效果不好？**

国产厂商（小米、华为、OPPO、vivo）有额外的后台管理机制，需要引导用户：
1. 开启自启动权限
2. 关闭电池优化
3. 添加省电白名单
4. 锁定最近任务卡片

可调用 `AutoStartPermissionManager.openAutoStartSettings(context)` 自动跳转到对应厂商的设置页面。

**Q12: Android 14+ 前台服务启动失败？**

Android 14 开始要求声明[前台服务类型权限](https://developer.android.com/about/versions/14/changes/fgs-types-required)：
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```
Fw 框架的 AAR 已在 Manifest 中声明了所有必要权限，正常情况下会自动合并。如遇冲突，请检查你的 `AndroidManifest.xml` 是否有 `tools:node="remove"` 误删了相关权限。

**Q13: Native 守护进程有效吗？**

Native 守护进程（C++ fork）在普通应用中效果有限——Android 5.0+ 系统会同时杀死应用的所有关联进程。但配合 Java 层双进程守护、Socket 心跳、文件锁互监控，可以显著提高存活率。在 Android 7.0-10 的非厂商定制 ROM 上效果最佳。

**Q14: 如何像墨迹天气一样"永生不死"？**

墨迹天气的"永生不死"主要依赖**厂商级商业合作**（系统白名单、预装）而非纯技术手段。对于独立开发者，建议综合使用以下方案：集成厂商推送 SDK + 引导用户开启自启动权限 + 请求电池优化豁免 + 提供有价值的常驻功能（如桌面小组件、锁屏信息）让用户主动保护你的应用。

**Q15: v2.0 的 MediaSession 通知豁免真的不需要通知权限吗？**

是的。Android 13+ 系统对活跃的 MediaSession 应用有特殊豁免——通过 `MediaStyle` 通知绑定活跃的 `MediaSession`，系统会自动允许通知显示，无需用户授予 `POST_NOTIFICATIONS` 权限。这是酷狗音乐、QQ 音乐等应用的实际做法。参见 [Android 官方文档](https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions-media-sessions)。

**Q16: 如何验证保活效果？**

框架自带测试脚本 `kill_alive.sh`，默认强杀 100 次、每次间隔 5 秒，自动统计恢复成功率。也可以使用 `adb shell am force-stop <package>` 手动测试，并通过 `adb logcat | grep Fw` 查看保活日志输出。

**Q17: 能否只开启部分策略，不全部启用？**

完全可以。`FwConfig` 提供 50+ 独立开关，每个策略都可以单独启用或关闭。默认调用 `Fw.init(this)` 时，只有低侵入性的策略会自动开启；VPN、伴侣设备、CallStyle、设备管理员等高侵入性策略默认关闭，需手动开启。

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

### v2.0.0 (2026-04)

**重大版本发布** — 8 大全新保活策略，策略总数突破 35+

- 新增 `FwVpnService` — 本地回环 VPN 保活，系统级 BIND_VPN_SERVICE 绑定保护
- 新增 `FwCompanionService` — CompanionDeviceService（API 31+），BLE 设备关联保活
- 新增 `FwCallStyleManager` — Notification.CallStyle 通知豁免，无需 POST_NOTIFICATIONS 权限
- 新增 `MediaSessionNotificationManager` — MediaSession 通知豁免（Android 13+ 豁免机制）
- 新增 `FwDeviceAdminService` — 设备管理员服务（API 26+），Device Owner 模式极高进程优先级
- 新增 `FwTileService` — Quick Settings 磁贴保活，下拉通知栏即可触发
- 新增 `FwWidgetProvider` — 桌面小组件保活，30 分钟定时唤醒
- 新增 `FwDreamService` — 屏保保活，充电待机时自动激活
- FwConfig 新增 8 个配置项，总配置项数量超过 50+
- 全面升级工具链：Gradle 9.4.1 + AGP 9.1.0 + Kotlin 2.3.20
- GitHub SEO 优化：16 个 Topic 标签、竞品对比表
- 发布到 Maven Central：`implementation("io.github.pangu-immortal:keeplive-framework:2.0.0")`

### v1.11.56 (2026-04)

**Maven Central 首次发布** — 一行代码引用

- 发布到 **Maven Central**：`implementation("io.github.pangu-immortal:keeplive-framework:1.11.56")`
- 无需额外配置仓库地址，项目默认的 `mavenCentral()` 即可拉取
- 附带 Sources JAR 和 Javadoc JAR，IDE 可直接查看源码和文档

### v2.2.1 (2026-02)

**新增保活策略增强** - 借鉴 KeepAlivePerfect 核心技术

- 新增 `SilentAudioStrategy` - 静默音频播放保活
- 新增 `AggressiveLevel` - 三档能耗控制
- 新增 `RestartProtection` - 连续重启保护机制
- 新增 `BIND_ABOVE_CLIENT` - 最高优先级绑定
- 新增 `kill_alive.sh` - 保活效果自动化测试脚本

### v2.2.0 (2025-02)

**新增 MediaRoute 保活策略** - 酷狗音乐核心保活技术

- 新增 `FwMediaRouteProviderService` / `FwMediaRoute2ProviderService`
- 新增 `FwMediaRouteNative` - C++ 层服务状态监控
- 新增 `FwMediaRouteManager` - 统一生命周期管理

### v2.1.0 (2025-01)

**新增无法强制停止策略** - 5ms 时间差竞争技术

- 新增 `ForceStopResistance` - 多进程文件锁监控
- 新增 `AppProcessLauncher` - app_process 命令拉活
- 新增 `AmsBinderInvoker` - C++ 直接调用 AMS Binder

---

## License

```
Copyright 2024-2026 Pangu-Immortal

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

---

## Star 趋势

[![Star History Chart](https://api.star-history.com/svg?repos=Pangu-Immortal/KeepLiveService&type=Date)](https://star-history.com/#Pangu-Immortal/KeepLiveService&Date)
