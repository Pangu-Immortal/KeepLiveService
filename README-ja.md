# Fw — Android バックグラウンド常駐フレームワーク百科

> 既定で表示される README は引き続き [簡体中文](README.md) です。このページは、セットアップ、設定、権限フローを確認するための日本語入口です。完全な戦略リファレンスは [README.md](README.md) または [English](README-en.md) を参照してください。

> Maven Central の現在の安定版は `2.0.1` です。この入口文書は最短の導入手順を優先し、詳細な戦略表と Android バージョン別の説明は完全版ドキュメントに集約しています。

[简体中文](README.md) | [繁體中文](README-zh-Hant.md) | [English](README-en.md) | **日本語** | [한국어](README-ko.md)

---

## 目次

- [クイックスタート](#クイックスタート)
- [体外 Activity 戦略](#体外-activity-戦略)
- [基本設定](#基本設定)
- [実行時 API](#実行時-api)
- [ユーザー許可が必要な戦略](#ユーザー許可が必要な戦略)
- [ビルドとテスト](#ビルドとテスト)
- [さらに読む](#さらに読む)

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

## 体外 Activity 戦略

Fw は `FwStart.start(context, intent)` で統一された体外 Activity 起動入口を提供し、`FwStart.startAuditAll(context, intent)` で全量監査入口を提供します。既定入口は通常アプリで実行可能かつリスクを制御できるパスだけを実行し、監査入口は特別なシステム条件、特権権限、または研究用登録のみのパスをログに記録して明確なスキップ理由を返します。

```kotlin
val result = FwStart.start(context, targetIntent)
if (result.success) {
    FwLog.d("${result.strategy?.displayName}")
}

val audit = FwStart.startAuditAll(context, targetIntent)
```

- **Activity Context 直接起動：全バージョン対応。`context` が Activity の場合に `startActivity(intent)` を直接呼びます。**
- **`FLAG_ACTIVITY_NEW_TASK` フォールバック：非 Activity Context で新規タスクフラグを付けて起動します。**
- **`NEW_TASK + EXCLUDE_FROM_RECENTS + NO_ANIMATION`：新規タスク起動時に最近のタスク表示と遷移アニメーションを抑えます。**
- **PendingIntent Activity：`PendingIntent.getActivity(...).send()` による代理起動パスです。**
- **二重 Intent `startActivities(Intent[])`：Android 4.1+ でより完全なタスクスタックを構築します。**
- **Binder `startActivities`：主に Android 5.0-11 の Native/Binder 互換分岐をカバーします。**
- **`startActivityForResult`：Activity Context のみ。公開 API の結果返却パスを保持します。**
- **VirtualDisplay + Presentation：Android 8.0+ で `setLaunchDisplayId` により仮想画面起動を試みます。**
- **`ActivityManager.moveTaskToFront`：既存タスクと `REORDER_TASKS` 権限がある場合に前面へ移動します。**
- **Shell / Root コマンド登録：shell、root、システムアプリ、または許可済みテスト環境の監査用です。**
- **Notification BAL token 登録：Android 10-14 の研究ウィンドウで、登録とログ出力のみ行います。**
- **`startNextMatchingActivity`：Activity Context のみ。公開 Activity マッチング起動分岐です。**
- **CredentialManager UI 登録：Android 14 のシステム UI パスを監査し、システム UI を濫用しません。**
- **PrintManager UI PendingIntent 登録：Android 6.0-14 の研究ウィンドウで、登録とログ出力のみ行います。**
- **MediaButton BAL 伝播登録：Android 12-14 の研究ウィンドウで、メディアキーと BAL 伝播境界を監査します。**

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

rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
./gradlew :framework:checkFwRustToolchain
./gradlew :framework:assembleRelease -PfwBuildRust=true

./kill_alive.sh
adb logcat | grep -E "(Fw|FwStart|FwHealth|ServiceStarter)"
```

---

## さらに読む

- 完全な簡体中文ドキュメント: [README.md](README.md)
- English documentation: [README-en.md](README-en.md)
- Android バージョン対応、全戦略一覧、ベンダー ROM 対応、FAQ は完全版ドキュメントを参照してください。
