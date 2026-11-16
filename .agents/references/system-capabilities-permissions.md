# System Capabilities and Device Permissions: shared engineering guide

Reusable for GIS and general applications. Host APIs and explicit user permissions are separate from this skill's written instructions. [Skill](../skills/system-capabilities-and-permissions/SKILL.md) · [Interop](system-skill-interop.md) · [Candidate registry](system-capability-registry.json).

| Target | Candidate mechanisms | Constraint |
| --- | --- | --- |
| Windows WPF/.NET | DateTimeOffset, TimeZoneInfo, Windows.Devices.Geolocation, Windows file picker | Location RequestAccessAsync must run foreground UI thread; packaged/unpackaged policy differs |
| Qt 6.5+ | QPermission, QLocationPermission, QtSerialPort/QtBluetooth | Version, Python bindings, native manifest and device must be checked |
| Browser | Date/Intl, navigator.geolocation, Permissions API, scoped file picker | HTTPS, Permissions Policy, user/OS consent and supported browser |
| Electron/WebView2 | Session permission handlers and narrowly scoped native bridge | Both permission check/request and origins restricted; no generic native execution |
| Tauri v2 | Per-window WebView capabilities and plugins | Frontend command permissions do NOT grant OS-level device rights |
| Linux | XDG portals / OS service and device access | Session backend, distro, sandbox and explicit consent vary |
| macOS/iOS | Native Core Location/OS privacy entitlements | Usage descriptions, app-store and OS policies |
| Android | Runtime permission APIs | Fine/coarse/background access distinct |
| Python | datetime/zoneinfo, optional psutil/pyserial/Bleak | Imports are not GPS, serial or Bluetooth authorization |

Official platform references: [Windows](https://learn.microsoft.com/en-us/windows/apps/develop/maps-and-location/get-location) · [Qt](https://doc.qt.io/qt-6/qpermission.html) · [Browser](https://developer.mozilla.org/en-US/docs/Web/API/Geolocation_API) · [Electron](https://www.electronjs.org/docs/latest/api/session) · [Tauri](https://tauri.app/security/capabilities/) · [Linux](https://flatpak.github.io/xdg-desktop-portal/) · [Apple](https://developer.apple.com/documentation/corelocation) · [Android](https://developer.android.com/guide/topics/permissions/overview).

**One action contract:** feature/user purpose, target OS/version, capability ID, exact provider+API, declared scope/origin, availability, OS/app permission, user trigger, output schema, timeout/cancel/unsubscribe, rate limits, accuracy/timestamp, errors/fallback, retention, test evidence. Never assume installed sensor from SDK availability or precise location from locale/IP/timezone.

**Failure paths:** permission denied/revoked -> nonblocking manual entry; stale GPS -> show age and accuracy; missing serial/BLE -> GNSS file import; no network -> offline fallback; camera/microphone denied -> no capture; files -> native pickers with minimal scope. No invisible telemetry, unique device identifiers or forced admin elevation.

**Native release gate:** T0 links/schema, T1 offline deterministic states, T2 actual adapter/device and real permission, T3 real UI user flow including keyboard/RTL, T4 clean-install/package manifest and resume. Unavailable tests MUST be NOT RUN. In GIS, camera map target and physical GPS position stay independent; solar calculations need UTC instant, target location and verified sun engine, not automatic user position.
