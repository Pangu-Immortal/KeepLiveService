# Fw - Android 保活技術百科全書

> 預設 README 仍為 [簡體中文](README.md)。本頁是繁體中文入口，保留最常用的接入、配置與授權說明；完整策略細節請參考 [README.md](README.md) 或 [English](README-en.md)。

> Maven Central 目前最新穩定版為 `2.0.1`。本入口文件優先保留快速接入流程，完整策略表、Android 版本適配與 FAQ 請以完整文件為準。

[简体中文](README.md) | **繁體中文** | [English](README-en.md) | [日本語](README-ja.md) | [한국어](README-ko.md)

---

## 文件導覽

- [快速接入](#快速接入)
- [體外 Activity 策略](#體外-activity-策略)
- [常用配置](#常用配置)
- [執行時 API](#執行時-api)
- [授權型策略](#授權型策略)
- [建置與測試](#建置與測試)
- [更多文件](#更多文件)

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

## 體外 Activity 策略

Fw 透過 `FwStart.start(context, intent)` 提供統一的體外 Activity 啟動入口，並透過 `FwStart.startAuditAll(context, intent)` 提供全量審計入口。預設入口只執行普通應用可落地、風險可控的路徑；審計入口會記錄需要特殊系統條件、特權權限或僅供研究登記的路徑，並在日誌中返回明確跳過原因。

```kotlin
val result = FwStart.start(context, targetIntent)
if (result.success) {
    FwLog.d("${result.strategy?.displayName}")
}

val audit = FwStart.startAuditAll(context, targetIntent)
```

- **Activity Context 直啟：全版本可用，當 `context` 是 Activity 時直接呼叫 `startActivity(intent)`。**
- **`FLAG_ACTIVITY_NEW_TASK` 兜底：非 Activity Context 時追加新任務旗標後啟動。**
- **`NEW_TASK + EXCLUDE_FROM_RECENTS + NO_ANIMATION`：新任務啟動時減少最近任務展示與切換動畫。**
- **PendingIntent Activity：透過 `PendingIntent.getActivity(...).send()` 走代理啟動路徑。**
- **雙 Intent `startActivities(Intent[])`：Android 4.1+ 用於建立更完整的任務棧。**
- **Binder `startActivities`：主要覆蓋 Android 5.0-11 的 Native/Binder 相容分支。**
- **`startActivityForResult`：僅 Activity Context，保留公開 API 結果回傳路徑。**
- **VirtualDisplay + Presentation：Android 8.0+ 嘗試透過 `setLaunchDisplayId` 啟動到虛擬螢幕。**
- **`ActivityManager.moveTaskToFront`：已有任務且具備 `REORDER_TASKS` 權限時把任務移到前台。**
- **Shell / Root 命令登記：僅供 shell、root、系統應用或授權測試環境審計。**
- **Notification BAL token 登記：Android 10-14 研究窗口，只登記與記錄日誌。**
- **`startNextMatchingActivity`：僅 Activity Context，走公開 Activity 匹配啟動分支。**
- **CredentialManager UI 登記：Android 14 系統 UI 路徑，只審計不濫用系統 UI。**
- **PrintManager UI PendingIntent 登記：Android 6.0-14 研究窗口，只登記與記錄日誌。**
- **MediaButton BAL 傳播登記：Android 12-14 研究窗口，只審計媒體按鍵與 BAL 傳播邊界。**

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

rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
./gradlew :framework:checkFwRustToolchain
./gradlew :framework:assembleRelease -PfwBuildRust=true

./kill_alive.sh
adb logcat | grep -E "(Fw|FwStart|FwHealth|ServiceStarter)"
```

---

## 更多文件

- 完整簡體中文文件：[README.md](README.md)
- English documentation: [README-en.md](README-en.md)
- Android 版本適配、完整策略表、廠商 ROM 適配與 FAQ 請查看完整文件。
