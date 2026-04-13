# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-04-13

### Added
- `FwVpnService` — Local loopback VPN keep-alive with system-level BIND_VPN_SERVICE binding protection
- `FwCompanionService` — CompanionDeviceService (API 31+) for BLE device association keep-alive
- `FwCallStyleManager` — Notification.CallStyle bypass, no POST_NOTIFICATIONS permission needed
- `MediaSessionNotificationManager` — MediaSession notification exemption mechanism (Android 13+)
- `FwDeviceAdminService` — Device Admin service (API 26+), Device Owner mode with highest process priority
- `FwTileService` — Quick Settings tile keep-alive, triggers on notification shade pull-down
- `FwWidgetProvider` — Home screen widget keep-alive, 30-minute system-level wake-up
- `FwDreamService` — Screen saver keep-alive, activates during charging + idle
- 8 new FwConfig fields for new strategies (50+ total config options)
- Competitive comparison table (vs MarsDaemon, Leoric, Cactus)
- 16 GitHub topic tags for SEO optimization

### Changed
- Upgraded toolchain: Gradle 9.4.1, AGP 9.1.0, Kotlin 2.3.20
- Strategy count increased from 27 to 35+
- Published to Maven Central as v2.0.0

## [1.11.56] - 2026-04-01

### Added
- First Maven Central release — one-line dependency integration
- Sources JAR and Javadoc JAR included
- GPG signed artifacts meeting Maven Central requirements
- Enriched README documentation with quick start guide

## [2.2.1] - 2026-02-20

### Added
- `SilentAudioStrategy` — Silent audio playback keep-alive, loops 1-second silent WAV to prevent CPU sleep
- `AggressiveLevel` — Three-tier energy control (LOW/MEDIUM/HIGH) balancing keep-alive vs battery
- `RestartProtection` — Continuous restart protection, 5-minute cooldown after 10 restarts in 60 seconds
- `BIND_ABOVE_CLIENT` — DaemonService binds main service with highest priority
- `kill_alive.sh` — Automated keep-alive testing script (loop force-kill and verify recovery)
- FwConfig fields: `enableSilentAudio`, `aggressiveLevel`

## [2.2.0] - 2025-02-15

### Added
- `FwMediaRouteProviderService` — Register media route provider with system MediaRouter
- `FwMediaRoute2ProviderService` — Android 11+ MediaRouter2 API support
- `FwMediaRouteNative` — C++ service state monitoring, heartbeat detection, WakeLock management
- `FwMediaRouteManager` — Unified module lifecycle management
- `FwMediaActivity` — Media intent handling Activity
- FwConfig fields: `enableMediaRouteProvider`, `enableMediaRoute2Provider`, `enableMediaIntentActivity`

## [2.1.0] - 2025-01-10

### Added
- `ForceStopResistance` — Multi-process file lock monitoring for anti-force-stop
- `AppProcessLauncher` — Process revival via app_process command
- `AmsBinderInvoker` — C++ direct AMS Binder call for < 1ms service restart
- `FwInstrumentation` — Process revival via am instrument command
- 5ms race condition exploit against system force-stop mechanism
- Native C++ Binder implementation (open /dev/binder, pre-constructed Parcel, ioctl)

[2.0.0]: https://github.com/Pangu-Immortal/KeepLiveService/releases/tag/v2.0.0
[1.11.56]: https://github.com/Pangu-Immortal/KeepLiveService/releases/tag/v1.11.56
[2.2.1]: https://github.com/Pangu-Immortal/KeepLiveService/releases/tag/v2.2.1
[2.2.0]: https://github.com/Pangu-Immortal/KeepLiveService/releases/tag/v2.2.0
[2.1.0]: https://github.com/Pangu-Immortal/KeepLiveService/releases/tag/v2.1.0
