# Fw - Android 保活技術百科全書

> 預設 README 仍為 [簡體中文](README.md)。本頁是繁體中文入口，保留最常用的接入、配置與授權說明；完整策略細節請參考 [README.md](README.md) 或 [English](README-en.md)。

[简体中文](README.md) | **繁體中文** | [English](README-en.md) | [日本語](README-ja.md) | [한국어](README-ko.md)

---

## 快速接入

### 1. 加入依賴

```kotlin
dependencies {
    implementation("io.github.pangu-immortal:keeplive-framework:2.0.1")
}
```

```groovy
dependencies {
    implementation 'io.github.pangu-immortal:keeplive-framework:2.0.1'
}
```

本庫已發布到 Maven Central，使用專案預設的 `mavenCentral()` 即可解析，無需額外倉庫。

### 2. 在 Application 初始化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Fw.init(this)
    }
}
```

低侵入策略會使用預設配置啟動；VPN、伴侶裝置、設備管理員、懸浮窗、1 像素 Activity、聯絡人/簡訊觀察等策略需要明確開啟或取得使用者授權。

---

## 常用配置

```kotlin
Fw.init(this) {
    enableForegroundService = true
    enableDualProcess = true
    enableNativeDaemon = true
    enableMediaRouteProvider = true
    enableMediaBrowserService = true
    enableSilentAudio = true
    aggressiveLevel = AggressiveLevel.MEDIUM

    enableVpnService = false
    enableCompanionDevice = false
    enableCallStyleNotification = false
    enableDeviceAdmin = false
    enableForceStopResistance = false

    notificationChannelId = "fw_channel"
    notificationChannelName = "守護服務"
    notificationTitle = "服務執行中"
    notificationContent = "點擊開啟應用"
    notificationActivityClass = MainActivity::class.java
}
```

---

## 執行時 API

```kotlin
val report = Fw.check()
Fw.stop()
Fw.isInitialized()
```

```kotlin
val result = FwStart.start(context, targetIntent)
val audit = FwStart.startAuditAll(context, targetIntent)
```

---

## 授權型策略

```kotlin
FwVpnService.prepareIntent(activity)?.let { intent ->
    activity.startActivityForResult(intent, 1001)
}

BatteryOptimizationManager.requestIgnoreBatteryOptimizations(context)
AutoStartPermissionManager.openAutoStartSettings(context)
```

如果開啟 `enableContactsContentObserver` 或 `enableSmsContentObserver`，宿主應用需要自行宣告並請求 `READ_CONTACTS` 或 `READ_SMS`。Fw 預設不合併這兩類敏感權限。

---

## 建置與測試

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

./gradlew build
./gradlew :framework:publishReleasePublicationToMavenLocal

./kill_alive.sh
adb logcat | grep -E "(Fw|FwStart|FwHealth|ServiceStarter)"
```

---

## 更多文件

- 完整簡體中文文件：[README.md](README.md)
- English documentation: [README-en.md](README-en.md)
- Android 版本適配、完整策略表、廠商 ROM 適配與 FAQ 請查看完整文件。
