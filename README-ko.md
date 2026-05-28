# Fw — Android Keep-Alive Framework Encyclopedia

> 기본 README 는 계속 [간체 중국어](README.md) 입니다. 이 한국어 문서는 설치, 설정, 권한 흐름을 빠르게 확인하기 위한 진입점입니다. 전체 전략 설명은 [README.md](README.md) 또는 [English](README-en.md)를 참고하세요.

[简体中文](README.md) | [繁體中文](README-zh-Hant.md) | [English](README-en.md) | [日本語](README-ja.md) | **한국어**

---

## 빠른 시작

### 1. 의존성 추가

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

Fw 는 Maven Central 에 배포되어 있습니다. 기본 `mavenCentral()` 저장소 설정만 있으면 사용할 수 있습니다.

### 2. Application 에서 초기화

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Fw.init(this)
    }
}
```

저침습 전략은 기본 설정으로 시작됩니다. VPN, Companion Device, Device Admin, 플로팅 윈도우, 1픽셀 Activity, 연락처/SMS 옵저버 등은 명시적으로 켜거나 사용자 승인이 필요합니다.

---

## 기본 설정

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

## 런타임 API

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

## 사용자 승인이 필요한 전략

```kotlin
FwVpnService.prepareIntent(activity)?.let { intent ->
    activity.startActivityForResult(intent, 1001)
}

BatteryOptimizationManager.requestIgnoreBatteryOptimizations(context)
AutoStartPermissionManager.openAutoStartSettings(context)
```

`enableContactsContentObserver` 또는 `enableSmsContentObserver` 를 켜는 경우, 호스트 앱에서 `READ_CONTACTS` 또는 `READ_SMS` 권한을 직접 선언하고 런타임 승인을 받아야 합니다. Fw 는 이 민감 권한들을 기본으로 병합하지 않습니다.

---

## 빌드와 테스트

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

## 더 보기

- 전체 간체 중국어 문서: [README.md](README.md)
- English documentation: [README-en.md](README-en.md)
- Android 버전 대응, 전체 전략 목록, 벤더 ROM 대응, FAQ 는 전체 문서를 참고하세요.
