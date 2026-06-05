# Fw — Android 백그라운드 상주 프레임워크 백과

> 기본 README 는 계속 [간체 중국어](README.md) 입니다. 이 한국어 문서는 설치, 설정, 권한 흐름을 빠르게 확인하기 위한 진입점입니다. 전체 전략 설명은 [README.md](README.md) 또는 [English](README-en.md)를 참고하세요.

> Maven Central 의 현재 안정 버전은 `2.0.1` 입니다. 이 문서는 빠른 통합 흐름을 우선으로 다루며, 전체 전략표와 Android 버전별 설명은 전체 문서에 정리되어 있습니다.

[简体中文](README.md) | [繁體中文](README-zh-Hant.md) | [English](README-en.md) | [日本語](README-ja.md) | **한국어**

---

## 목차

- [빠른 시작](#빠른-시작)
- [체외 Activity 전략](#체외-activity-전략)
- [기본 설정](#기본-설정)
- [런타임 API](#런타임-api)
- [사용자 승인이 필요한 전략](#사용자-승인이-필요한-전략)
- [빌드와 테스트](#빌드와-테스트)
- [더 보기](#더-보기)

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

## 체외 Activity 전략

Fw 는 `FwStart.start(context, intent)` 로 통합 체외 Activity 시작 진입점을 제공하고, `FwStart.startAuditAll(context, intent)` 로 전체 감사 진입점을 제공합니다. 기본 진입점은 일반 앱에서 실제로 실행 가능하고 위험을 제어할 수 있는 경로만 실행하며, 감사 진입점은 특수한 시스템 조건, 특권 권한 또는 연구용 등록 경로를 로그에 남기고 명확한 건너뜀 사유를 반환합니다.

```kotlin
val result = FwStart.start(context, targetIntent)
if (result.success) {
    FwLog.d("${result.strategy?.displayName}")
}

val audit = FwStart.startAuditAll(context, targetIntent)
```

- **Activity Context 직접 시작**: 모든 Android 버전에서 `context` 가 Activity 일 때 `startActivity(intent)` 를 직접 호출합니다.
- **`FLAG_ACTIVITY_NEW_TASK` 폴백**: Activity 가 아닌 Context 에서 새 작업 플래그를 붙여 시작합니다.
- **`NEW_TASK + EXCLUDE_FROM_RECENTS + NO_ANIMATION`**: 새 작업 시작 시 최근 작업 노출과 전환 애니메이션을 줄입니다.
- **PendingIntent Activity**: `PendingIntent.getActivity(...).send()` 를 통한 대리 시작 경로입니다.
- **이중 Intent `startActivities(Intent[])`**: Android 4.1+ 에서 더 완전한 작업 스택을 구성합니다.
- **Binder `startActivities`**: 주로 Android 5.0-11 의 Native/Binder 호환 분기를 다룹니다.
- **`startActivityForResult`**: Activity Context 전용이며 공개 API 결과 반환 경로를 유지합니다.
- **VirtualDisplay + Presentation**: Android 8.0+ 에서 `setLaunchDisplayId` 로 가상 화면 시작을 시도합니다.
- **`ActivityManager.moveTaskToFront`**: 기존 작업과 `REORDER_TASKS` 권한이 있을 때 작업을 전면으로 이동합니다.
- **Shell / Root 명령 등록**: shell, root, 시스템 앱 또는 승인된 테스트 환경 감사용입니다.
- **Notification BAL token 등록**: Android 10-14 연구 구간에서 등록과 로그만 수행합니다.
- **`startNextMatchingActivity`**: Activity Context 전용 공개 Activity 매칭 시작 분기입니다.
- **CredentialManager UI 등록**: Android 14 시스템 UI 경로를 감사하며 시스템 UI 를 남용하지 않습니다.
- **PrintManager UI PendingIntent 등록**: Android 6.0-14 연구 구간에서 등록과 로그만 수행합니다.
- **MediaButton BAL 전파 등록**: Android 12-14 연구 구간에서 미디어 키와 BAL 전파 경계를 감사합니다.

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
