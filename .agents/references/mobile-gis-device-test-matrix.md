# Mobile GIS Android/iOS Device Acceptance Matrix

Companion to [GIS Mobile Development](../skills/gis-mobile-application-development/SKILL.md). Static scripts and contract checks are **T0/T1 only**, not live mobile app evidence. Check actual SDK/OS, Android min/target SDK and iOS deployment targets against current official platform documentation when implementing.

| ID | Platform / mode | Real action and observable PASS criterion | Minimum evidence |
| --- | --- | --- | --- |
| MOB-01 | Android + iOS | Clean install; launch and open test GeoPackage layer with correct features and CRS | Device identifier category, OS/app build, screenshots/logs |
| MOB-02 | Both | Pan, zoom, select, identify, edit geometry/attributes; reopen persisted edit | UI recording, fixture diff/readback |
| MOB-03 | Both | Allow foreground location; display actual fix age, accuracy, EPSG/data source; reject stale fix | Permission prompts and device-reported diagnostics |
| MOB-04 | Both | Deny/revoke location; manual coordinates still work, no phantom location state | Denial screen and state log |
| MOB-05 | Android | Validate approximate versus precise, background permission only if specifically required, app resume | Version-specific manifest and recorded permission states |
| MOB-06 | iOS | Validate When In Use versus Always only if required, usage descriptions and settings revocation | OS privacy prompt and action log |
| MOB-07 | Both | Airplane mode; open local maps, edit 2+ features, force-close, relaunch and confirm no lost edits | Database checksums, queue record count |
| MOB-08 | Both | Restore connectivity, server conflict and duplicate retry; verify idempotency without silent overwrite | Request IDs, server revisions and conflict UI |
| MOB-09 | Both | Login/PKCE callback; token expiry/refresh; sign-out and offline unlock separated | Redacted protocol logs, protected-store validation |
| MOB-10 | Both | Import KML/KMZ/GeoJSON; export and independent readback with attributes, CRS and styles when supported | Before/after file comparisons and format loss report |
| MOB-11 | Both | Camera/photo/attachment permission granted/denied; app recovers safely | Prompt, denied fallback and attachment provenance |
| MOB-12 | Both | Low disk/storage pressure and large/offline package; never lose unsynced work | Storage metrics, warning screenshot |
| MOB-13 | Both | Background/resume, rotation, process kill, battery saver and slow/absent network | Lifecycle traces, pending queue integrity |
| MOB-14 | Both | Arabic RTL and English LTR, large font, accessibility, keyboard and tablet layout | Screen reader/focus review and screenshots |
| MOB-15 | Both | Optional Bluetooth/external GNSS: connect, disconnect, fallback, distinguish internal/external accuracy | Recorded device source, fix provenance |
| MOB-16 | Both | Signed release package: install, launch, update with migration, backup/restore and uninstall retention policy | Package signature, build SHA, app/device versions |

**Evidence status validator:** run python scripts/validate_mobile_device_evidence.py path/to/report.json. PASS requires device, OS, build SHA, observed result and evidence; physical hardware/privacy cases cannot PASS as a CI fixture. The example report is [explicitly NOT_RUN](../examples/mobile-gis/device-acceptance.example.json).

## Native automation choice

Android: instrumentation tests, AndroidX Test/Espresso or UI Automator and emulator/device as supported. iOS: XCTest/XCUITest on simulator **and** device for OS/hardware-specific GNSS and entitlement evidence. Shared-flow frameworks (for example Appium) are optional and do not prove native feature fidelity without physical evidence. Run both online and no-network cases; synthetic mock GPS never counts as GNSS hardware verification.

## Test status contract

Each case records PASS, FAIL, BLOCKED or NOT_RUN; tool, platform, device model or class, OS, API/SDK version, build SHA, input fixtures, observed output, screenshots/logs, and a reviewer. A scenario requiring physical GPS, camera, iOS entitlements or Bluetooth is NOT_RUN in Linux-only CI. Android emulator passing does not imply iOS passing. Never claim App Store/Play Store publication unless actual distribution checks succeeded.
