# Fw — Android Keep-Alive Framework Encyclopedia

> 既定で表示される README は引き続き [簡体中文](README.md) です。このページは、セットアップ、設定、権限フローを確認するための日本語入口です。完全な戦略リファレンスは [README.md](README.md) または [English](README-en.md) を参照してください。

[简体中文](README.md) | [繁體中文](README-zh-Hant.md) | [English](README-en.md) | **日本語** | [한국어](README-ko.md)

---

## クイックスタート

### 1. 依存関係を追加

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

Fw は Maven Central で公開されています。通常の `mavenCentral()` 設定だけで利用できます。

### 2. Application で初期化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Fw.init(this)
    }
}
```

低侵襲の戦略はデフォルト設定で起動します。VPN、Companion Device、Device Admin、フローティングウィンドウ、1 ピクセル Activity、連絡先/SMS 監視などは、明示的な有効化またはユーザー許可が必要です。

---

## 基本設定

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
    notificationChannelName = "Keep Alive Service"
    notificationTitle = "Service running"
    notificationContent = "Tap to open"
    notificationActivityClass = MainActivity::class.java
}
```

---

## 実行時 API

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

## ユーザー許可が必要な戦略

```kotlin
FwVpnService.prepareIntent(activity)?.let { intent ->
    activity.startActivityForResult(intent, 1001)
}

BatteryOptimizationManager.requestIgnoreBatteryOptimizations(context)
AutoStartPermissionManager.openAutoStartSettings(context)
```

`enableContactsContentObserver` または `enableSmsContentObserver` を有効にする場合、ホストアプリ側で `READ_CONTACTS` または `READ_SMS` を宣言し、実行時に許可を取得してください。Fw はこれらの機密権限をデフォルトではマージしません。

---

## ビルドとテスト

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

./gradlew build
./gradlew :framework:publishReleasePublicationToMavenLocal

./kill_alive.sh
adb logcat | grep -E "(Fw|FwStart|FwHealth|ServiceStarter)"
```

---

## さらに読む

- 完全な簡体中文ドキュメント: [README.md](README.md)
- English documentation: [README-en.md](README-en.md)
- Android バージョン対応、全戦略一覧、ベンダー ROM 対応、FAQ は完全版ドキュメントを参照してください。
