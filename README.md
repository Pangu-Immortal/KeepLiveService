# Fw — Android Keep-Alive Framework Encyclopedia

<div align="center">

![Android Keep-Alive Framework KeepLiveService Visitor Count](https://count.getloli.com/get/@KeepLiveService?theme=rule34)

<p>
  <b>If this helps you, please <a href="https://github.com/Pangu-Immortal/KeepLiveService/stargazers">Star</a> this repo!</b>
</p>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.pangu-immortal/keeplive-framework.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.pangu-immortal/keeplive-framework)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org)
[![16K Page Size](https://img.shields.io/badge/16K%20Page%20Size-Compatible-orange.svg)](https://developer.android.com/guide/practices/page-sizes)
[![Google Play](https://img.shields.io/badge/Google%20Play-Ready-success.svg)](https://developer.android.com/distribute/best-practices/develop/64-bit)

[English](README.md) | [简体中文](README-zh.md)

</div>

> **The most comprehensive open-source Android keep-alive library — 35+ background service strategies, Native C++ daemon process, covering Android 7.0–16, compatible with 10+ vendor ROMs.**

Fw (Framework) is a production-grade Android keep-alive library that prevents your background service from being killed by the system. It implements every known keep-alive strategy used by commercial apps like KuGou Music, Moji Weather, and QQ Music — including foreground service, dual-process daemon, Native C++ fork daemon, MediaRoute provider, silent audio playback, VPN system-level binding, notification permission bypass, and more.

**Why Fw?** Android's background execution limits get stricter with every release (Android 14 foreground service types, Android 15 16KB page size, Android 16 latest restrictions). MarsDaemon (last updated 2018), Leoric (2020), and Cactus (2022) are all abandoned. **Fw is the only actively maintained keep-alive framework supporting Android 7.0 through Android 16 (API 24–36) in 2026.**

---

## What is Android Keep-Alive?

Android **keep-alive** (also called process persistence, background service protection, or prevent process kill) refers to techniques that keep an app's background service running even when the system tries to reclaim resources. Without keep-alive strategies, Android's memory management (LMK/OOM killer), Doze mode, app standby buckets, and vendor-specific battery optimizations (Xiaomi MIUI, Huawei EMUI, OPPO ColorOS, vivo FuntouchOS) will terminate background processes — breaking features like real-time messaging, music playback, location tracking, IoT device connections, and health monitoring.

---

## v2.0 Major Release — 8 Brand-New Keep-Alive Strategies

> **Released April 2026** — Strategy count increased from 27 to 35+, covering the latest Android 8.0–16 system features. This is the largest update since the project was open-sourced.

| # | Strategy | Class | Core Mechanism | API | Effectiveness |
|---|----------|-------|---------------|-----|---------------|
| 1 | **VPN System-Level Keep-Alive** | `FwVpnService` | Local loopback VPN, BIND_VPN_SERVICE binding protection | 24+ | 5/5 |
| 2 | **Companion Device Keep-Alive** | `FwCompanionService` | CompanionDeviceService, BLE device association | 31+ | 4/5 |
| 3 | **CallStyle Notification Bypass** | `FwCallStyleManager` | Notification.CallStyle, bypasses POST_NOTIFICATIONS | 31+ | 5/5 |
| 4 | **MediaSession Notification Bypass** | `MediaSessionNotificationManager` | MediaSession + MediaStyle, Android 13+ exemption | 33+ | 5/5 |
| 5 | **Device Admin Keep-Alive** | `FwDeviceAdminService` | Device Owner mode, system auto-binds service | 26+ | 5/5 |
| 6 | **Quick Settings Tile** | `FwTileService` | Notification shade tile triggers keep-alive check | 24+ | 4/5 |
| 7 | **Home Screen Widget** | `FwWidgetProvider` | AppWidgetProvider, 30-minute system-level wake-up | 24+ | 4/5 |
| 8 | **Screen Saver (Dream) Keep-Alive** | `FwDreamService` | Activates during charging + idle | 24+ | 4/5 |

<details>
<summary><b>Click to expand: Detailed description of each new strategy</b></summary>

### VPN System-Level Keep-Alive — `FwVpnService`
Creates a local loopback VPN tunnel (127.0.0.1 → 127.0.0.1), gaining system-level `BIND_VPN_SERVICE` binding protection. VPN services have the highest process priority in Android — they are almost never killed by the system. Default: **OFF** (requires user VPN permission).

### Companion Device Keep-Alive — `FwCompanionService`
Uses Android 12+ `CompanionDeviceService` API to gain `REQUEST_COMPANION_RUN_IN_BACKGROUND` and `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` system-level exemptions through BLE device association. Default: **OFF** (requires BLE device pairing).

### CallStyle Notification Bypass — `FwCallStyleManager`
Registers as a self-managed calling app (ConnectionService + PhoneAccount) and sends `Notification.CallStyle` notifications. Android exempts call notifications from `POST_NOTIFICATIONS` permission — **no notification permission needed**. Default: **OFF** (shows call-style notification).

### MediaSession Notification Bypass — `MediaSessionNotificationManager`
Creates an active `MediaSession` with `MediaStyle` notification. Android 13+ exempts media session apps from `POST_NOTIFICATIONS` — this is how KuGou Music, QQ Music, and NetEase Cloud Music show notifications without permission. Default: **ON** (recommended).

### Device Admin Keep-Alive — `FwDeviceAdminService`
When the app is set as Device Owner, the system automatically binds `DeviceAdminService`, giving the process extremely high priority. Activated via `adb shell dpm set-device-owner` or NFC provisioning. Default: **OFF**.

### Quick Settings Tile — `FwTileService`
Adds a custom tile to the notification shade Quick Settings. Every time the user pulls down the notification shade, the tile's lifecycle triggers a keep-alive check. Default: **ON**.

### Home Screen Widget — `FwWidgetProvider`
`AppWidgetProvider` with 30-minute `updatePeriodMillis`. Even if the app is killed, the system guarantees `onUpdate()` callbacks at the configured interval. Default: **ON**.

### Screen Saver Keep-Alive — `FwDreamService`
Uses Android's Dream framework. When the device is charging and idle, the system automatically activates the screen saver service, triggering a keep-alive check. Default: **ON**.

</details>

---

## Comparison with Alternatives

| Feature | **Fw (This Project)** | MarsDaemon | Leoric | Cactus | Others |
|---------|:---------------------:|:----------:|:------:|:------:|:------:|
| Strategy Count | **35+** | 2-3 | 3-5 | 6 | 1-5 |
| Native C++ Daemon | Yes | Yes | Yes | No | No |
| MediaRoute Keep-Alive | Yes | No | No | No | No |
| VPN System-Level Keep-Alive | Yes | No | No | No | No |
| CompanionDevice Keep-Alive | Yes | No | No | No | No |
| Notification Permission Bypass (2 methods) | Yes | No | No | No | No |
| Device Admin Keep-Alive | Yes | No | No | No | No |
| Quick Settings Tile | Yes | No | No | No | No |
| Home Screen Widget | Yes | No | No | No | No |
| Screen Saver Keep-Alive | Yes | No | No | No | No |
| Android 16 Support | Yes | No | No | No | No |
| Maven Central One-Line Integration | Yes | No | No | No | No |
| Vendor ROM Adaptation | **10+ vendors** | Limited | Limited | Limited | None |
| Actively Maintained (2026) | **Yes** | Abandoned (2018) | Abandoned (2020) | Abandoned (2022) | Varies |

> MarsDaemon, Leoric, and Cactus were pioneers in Android keep-alive, but all have stopped maintenance. **Fw is the only open-source keep-alive framework that supports Android 16 and is actively maintained in 2026.**

---

## Quick Start

### Step 1: Add Dependency

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

> Published on **Maven Central** — no additional repository configuration needed. The default `mavenCentral()` repository will resolve it automatically.

### Step 2: One-Line Initialization

```kotlin
// In your Application.onCreate()
Fw.init(this)
```

That's it. All 35+ keep-alive strategies are automatically enabled.

### Step 3 (Optional): Fine-Grained Control

```kotlin
Fw.init(this) {
    // Core strategies
    enableForegroundService = true
    enableDualProcess = true
    enableNativeDaemon = true
    enableMediaRouteProvider = true
    enableSilentAudio = true
    aggressiveLevel = AggressiveLevel.MEDIUM  // LOW / MEDIUM / HIGH

    // v2.0 new strategies
    enableVpnService = false             // VPN keep-alive (requires user permission)
    enableMediaSessionNotification = true // Notification bypass (recommended)
    enableCallStyleNotification = false  // Call-style bypass (invasive)
    enableDeviceAdmin = false            // Device admin (requires activation)
    enableTileService = true             // Quick Settings tile
    enableWidget = true                  // Home screen widget (30-min wake)
    enableDreamService = true            // Screen saver keep-alive
    enableCompanionDevice = false        // BLE companion device

    // Aggressive strategies (off by default)
    enableForceStopResistance = false    // Anti-force-stop (Android 5-12)
    enableLockScreenActivity = false     // Lock screen activity
    enableFloatWindow = false            // Floating window

    // 50+ config options available — see full config reference below
}
```

### Runtime API

```kotlin
Fw.check()           // Manually trigger keep-alive check
Fw.stop()            // Stop all keep-alive strategies
Fw.isInitialized()   // Query framework status
```

---

## Use Case Recommendations

| Scenario | Recommended Strategies | AggressiveLevel | Key Config |
|----------|----------------------|-----------------|------------|
| **IM / Messaging** | ForegroundService + DualProcess + NativeDaemon + MediaSession + SilentAudio | `HIGH` | `enableForceStopResistance = true` |
| **Music Playback** | ForegroundService + MediaRoute + SilentAudio + MediaSession | `MEDIUM` | `enableMediaRouteProvider = true` |
| **IoT Device Connection** | ForegroundService + VPN + CompanionDevice + NativeDaemon | `MEDIUM` | `enableVpnService = true`, `enableCompanionDevice = true` |
| **Health / Fitness Tracking** | ForegroundService + WorkManager + AlarmManager + Widget | `LOW` | `enableWidget = true` |
| **Location Tracking** | ForegroundService + SilentAudio + JobScheduler + AlarmManager | `MEDIUM` | `enableAlarmManager = true` |
| **Enterprise MDM** | ForegroundService + DeviceAdmin + TileService | `LOW` | `enableDeviceAdmin = true` |

---

## Full Strategy List (35+)

### 1. Core Strategies

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| Foreground Service | `FwForegroundService` | `foregroundServiceType="mediaPlayback"` | 5/5 |
| MediaSession | `MediaSessionManager` | Creates media session for system protection | 5/5 |
| 1-Pixel Activity | `OnePixelActivity` | Transparent activity on screen-off | 4/5 |
| Lock Screen Activity | `LockScreenActivity` | Shows on lock screen (like Moji Weather) | 5/5 |
| Floating Window | `FloatWindowManager` | 1px floating window or visible float ball | 4/5 |

### 2. Scheduled Wake-Up

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| JobScheduler | `FwJobService` | System-level task scheduling, 15-min minimum | 4/5 |
| WorkManager | `FwWorker` | Jetpack task scheduling, best compatibility | 4/5 |
| AlarmManager | `AlarmStrategy` | Exact alarm wake-up | 3/5 |

### 3. Account Sync

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| SyncAdapter | `FwSyncAdapter` | System periodic sync trigger | 4/5 |
| AccountAuthenticator | `FwAuthenticator` | Account authentication service | 4/5 |

### 4. Broadcast Listeners (Static Registration)

| Strategy | Class | Broadcasts | Effectiveness |
|----------|-------|-----------|---------------|
| Bluetooth | `BluetoothReceiver` | ACL_CONNECTED, A2DP, HEADSET, AUDIO_BECOMING_NOISY | 5/5 |
| USB | `UsbReceiver` | USB_DEVICE_ATTACHED, USB_ACCESSORY_ATTACHED | 4/5 |
| NFC | `NfcReceiver` | TAG_DISCOVERED, TECH_DISCOVERED, NDEF_DISCOVERED | 4/5 |
| Media Button | `MediaButtonReceiver` | MEDIA_BUTTON (Bluetooth headset keys) | 4/5 |
| Media Mount | `MediaMountReceiver` | MEDIA_MOUNTED, MEDIA_EJECT | 4/5 |
| System Events | `SystemEventReceiver` | BOOT_COMPLETED, MY_PACKAGE_REPLACED | 5/5 |

### 5. Content Observers

| Strategy | Class | Content | Effectiveness |
|----------|-------|---------|---------------|
| Media Changes | `ContentObserverManager` | MediaStore.Images, Videos, Audio | 3/5 |
| Contacts Changes | `ContentObserverManager` | ContactsContract | 3/5 |
| File System | `FileObserverManager` | Download, DCIM, Screenshots | 3/5 |

### 6. System-Level Services

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| Accessibility Service | `FwAccessibilityService` | Highest process priority, user must enable | 5/5 |
| Notification Listener | `FwNotificationListenerService` | System auto-restarts after kill | 5/5 |

### 7. Dual-Process Guardian

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| Java Dual-Process | `DaemonService` | Separate `:daemon` process, mutual guarding | 4/5 |
| Native C++ Daemon | `FwNative` | C++ fork() child process monitoring | 4/5 |
| Socket Heartbeat | `FwNative` | Unix Domain Socket IPC | 3/5 |

### 8. MediaRoute Keep-Alive (KuGou Music Core Strategy)

Registers virtual media routes with the system MediaRouter, gaining "media app" identity. The system maintains the service connection and is reluctant to kill it.

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| MediaRouteProviderService | `FwMediaRouteProviderService` | Registers media route provider | 5/5 |
| MediaRoute2ProviderService | `FwMediaRoute2ProviderService` | Android 11+ MediaRouter2 API | 5/5 |
| MediaRoute Native | `FwMediaRouteNative` | C++ service monitoring + heartbeat | 4/5 |

### 9. Silent Audio Keep-Alive

Loops a 1-second silent WAV file (only 8KB) at zero volume to prevent CPU sleep. Used by KuGou Music, QQ Music, and other mainstream music apps.

| AggressiveLevel | Audio Interval | Alarm Interval | Daemon Check | Use Case |
|-----------------|---------------|----------------|--------------|----------|
| `LOW` | 10s | 10 min | 10s | Normal background tasks, battery-friendly |
| `MEDIUM` | 5s | 5 min | 3s | Balanced mode (default) |
| `HIGH` | Immediate | 1 min | 1s | IM apps, maximum keep-alive |

### 10. Anti-Force-Stop (5ms Race Condition)

Uses C++ Native Binder direct calls to race against the system's force-stop process. Multiple auxiliary processes monitor each other via file locks; when one dies, another sends a pre-constructed Parcel via `ioctl()` to AMS in < 1ms.

| Strategy | Class | Description | Effectiveness |
|----------|-------|-------------|---------------|
| File Lock Monitor | `ForceStopResistance` | Multi-process file lock mutual monitoring | 4/5 |
| AMS Binder Direct Call | `AmsBinderInvoker` | C++ direct `ioctl()` to AMS Binder | 5/5 |
| app_process Revive | `AppProcessLauncher` | Launch Java process via app_process | 4/5 |

> Effective on Android 5.0–12.0. Off by default due to invasiveness.

### 11. v2.0 New Strategies (Summary)

| Strategy | Class | API | Default | Effectiveness |
|----------|-------|-----|---------|---------------|
| VPN System-Level | `FwVpnService` | 24+ | OFF | 5/5 |
| Companion Device | `FwCompanionService` | 31+ | OFF | 4/5 |
| CallStyle Notification | `FwCallStyleManager` | 31+ | OFF | 5/5 |
| MediaSession Notification | `MediaSessionNotificationManager` | 33+ | **ON** | 5/5 |
| Device Admin | `FwDeviceAdminService` | 26+ | OFF | 5/5 |
| Quick Settings Tile | `FwTileService` | 24+ | **ON** | 4/5 |
| Home Screen Widget | `FwWidgetProvider` | 24+ | **ON** | 4/5 |
| Screen Saver | `FwDreamService` | 24+ | **ON** | 4/5 |

---

## Android Version Compatibility

| Android | API | Key Adaptations |
|---------|-----|-----------------|
| 7.x | 24-25 | `startService()` |
| 8.0+ | 26+ | `startForegroundService()` + notification channels |
| 9.0+ | 28+ | Stronger background restrictions |
| 10+ | 29+ | Background activity launch restrictions |
| 11+ | 30+ | Foreground service type required |
| 12+ | 31+ | `BLUETOOTH_CONNECT` runtime permission, CompanionDeviceService available |
| 13+ | 33+ | `POST_NOTIFICATIONS` permission, **MediaSession notification bypass works** |
| 14+ | 34+ | `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission |
| 15+ | 35+ | Stricter background limits, **16KB page size support** |
| 16 | 36 | Latest API |

---

## Vendor ROM Adaptation

| Vendor | Special Restrictions | Solution |
|--------|---------------------|----------|
| Xiaomi (MIUI) | Auto-start management, battery optimization | Guide user to enable auto-start |
| Huawei (EMUI) | Advanced battery management | Guide user to disable battery optimization |
| OPPO (ColorOS) | Background freeze | Guide user to add to power-saving whitelist |
| vivo (FuntouchOS) | iManager restrictions | Guide user to enable background running |
| Samsung (OneUI) | Device care optimization | Relatively lenient |
| Google (Pixel) | Strict Doze mode | Request `IGNORE_BATTERY_OPTIMIZATIONS`, use high-priority FCM |
| Transsion (Tecno) | Memory cleanup | Guide user to lock app |

```kotlin
// Auto-open vendor auto-start settings
AutoStartPermissionManager.openAutoStartSettings(context)
```

---

## Development Environment

| Tool | Version |
|------|---------|
| Gradle | 9.4.1 |
| AGP (Android Gradle Plugin) | 9.1.0 |
| Kotlin | 2.3.20 |
| JVM | 21 |
| NDK | 27.0.12077973 |
| compileSdk / targetSdk | 36 (Android 16) |
| minSdk | 24 (Android 7.0) |

---

## FAQ

**Q: What is Android keep-alive and why do I need it?**

Android keep-alive refers to techniques that prevent the OS from killing your app's background processes. Without it, Android's Low Memory Killer (LMK), Doze mode, and vendor-specific battery optimizations will terminate your background service — breaking real-time messaging, music playback, location tracking, and IoT connections.

**Q: How does Fw compare to WorkManager?**

WorkManager is for deferrable tasks that the system can schedule at its discretion. Fw is for scenarios where your service **must stay alive continuously** — like maintaining a socket connection for IM, playing music, or monitoring IoT devices. They serve different purposes and can be used together (Fw includes WorkManager as one of its 35+ strategies).

**Q: Will Google Play reject apps using keep-alive strategies?**

Fw is designed with Google Play compliance in mind. Core strategies like foreground service with proper `foregroundServiceType`, WorkManager, and JobScheduler are officially supported by Android. Invasive strategies (force-stop resistance, 1-pixel activity) are **off by default** and should only be used in sideloaded enterprise apps.

**Q: How much does Fw increase APK size?**

The framework adds approximately 800KB–1.2MB to your APK (including Native .so files for 4 architectures). Without native strategies, it's under 300KB.

**Q: What configuration do you recommend for IM / messaging apps?**

```kotlin
Fw.init(this) {
    aggressiveLevel = AggressiveLevel.HIGH
    enableForegroundService = true
    enableDualProcess = true
    enableNativeDaemon = true
    enableMediaSessionNotification = true
    enableSilentAudio = true
    enableForceStopResistance = true  // For sideloaded apps only
}
```

**Q: What about music playback apps?**

```kotlin
Fw.init(this) {
    aggressiveLevel = AggressiveLevel.MEDIUM
    enableForegroundService = true
    enableMediaRouteProvider = true
    enableMediaRoute2Provider = true
    enableSilentAudio = true
    enableMediaSessionNotification = true
}
```

**Q: Can I use Fw with Flutter or React Native?**

Yes. Fw is a pure Android library. In Flutter, add it to your `android/app/build.gradle.kts` and call `Fw.init()` from your custom `Application` class. In React Native, do the same in your `MainApplication.java/kt`.

**Q: Does the MediaSession notification bypass actually work without notification permission?**

Yes. Android 13+ has a special exemption for apps with an active `MediaSession` — `MediaStyle` notifications bound to an active session are displayed without `POST_NOTIFICATIONS` permission. This is how KuGou Music, QQ Music, and NetEase Cloud Music show notifications on Android 13+.

**Q: Is the notification bypass safe from Google crackdowns?**

The MediaSession exemption is an **intentional Android design** for media apps, documented in the [official Android 13 behavior changes](https://developer.android.com/about/versions/13/behavior-changes-13#notification-permission). It won't be "cracked down" — it's expected behavior for media applications.

**Q: How effective is Native C++ daemon?**

Native daemon (`fork()`) has limited effectiveness in normal apps because force-stop kills the entire process group. However, combined with Java dual-process guarding, it significantly improves survival rate against OOM kills and task-switcher swipe-away.

**Q: Does Fw support Android 16?**

Yes. Fw targets `compileSdk = 36` (Android 16) and is fully compatible with 16KB page size devices introduced in Android 15+. All native libraries are compiled with `-Wl,-z,max-page-size=16384`.

**Q: How do apps like Moji Weather stay alive "forever"?**

Commercial apps like Moji Weather typically have: (1) vendor whitelist agreements, (2) pre-installed on devices, (3) vendor push SDK integration, (4) lock screen features keeping them in foreground. For regular apps, the best approach is: Fw + vendor push SDK + guide user to enable auto-start + request battery optimization exemption.

**Q: What's the battery impact?**

Depends on `AggressiveLevel`: LOW mode adds < 1% daily battery drain. MEDIUM mode adds ~2-3%. HIGH mode can add 5-8% but provides maximum keep-alive. The `RestartProtection` mechanism prevents runaway restart loops that could drain the battery.

**Q: Can I use only specific strategies instead of all 35+?**

Absolutely. Every strategy has an independent on/off switch via `FwConfig`. You can enable as few as 1 strategy or as many as all 35+.

**Q: How do I test if keep-alive is working?**

```bash
# Kill the app 100 times with 5-second intervals
./kill_alive.sh

# Custom: 50 kills, 3-second interval
./kill_alive.sh 50 3 com.your.package

# Check logs
adb logcat | grep -E "(Fw|BluetoothReceiver|ServiceStarter)"
```

---

## Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md).

1. Fork this repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

```
Copyright 2024-2026 Pangu-Immortal

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Pangu-Immortal/KeepLiveService&type=Date)](https://star-history.com/#Pangu-Immortal/KeepLiveService&Date)

---

<div align="center">

**Telegram Group**: [Join the discussion — this is just the tip of the iceberg.](https://t.me/+V7HSo1YNzkFkY2M1)

![Pangu-Immortal WeChat QR Code](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

</div>
