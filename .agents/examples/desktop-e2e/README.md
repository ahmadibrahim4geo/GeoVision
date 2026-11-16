# GIS Windows Desktop E2E — acceptance scenario catalog

**Not a native test result.** This is a platform-neutral scenario description for a real authorized Windows GUI harness, with a [native testing playbook](../../references/gis-desktop-e2e-testing.md). Generic skills must not claim that a simulated Node.js DOM test covers a WPF/Qt window.

## Scenario A: launch and base layout

**Preflight:** interactive Windows desktop, pinned app executable, supported GIS engine, appropriate WebView2 runtime and nonprivate test profile.

1. Launch executable under an isolated user profile. Wait for main window and map readiness.
2. Enumerate Ribbon/menu, Layers/Places and status bar via UI Automation. Assert required buttons have stable AutomationId/accessibility names, proper states and no phantom commands.
3. Check initial map extent and state, resizing and 100–200% DPI where supported.
4. Close and confirm normal process exit. Mark FAIL if app exits with unhandled exception.

## Scenario B: real user import → engine → save → reopen → export

1. Open a synthetic KML/KMZ or GeoJSON fixture through the actual UI.
2. Inspect application layer manager and live rendered map; verify count, CRS, style and extents independently of toast messages.
3. Pan/zoom/fit extent, toggle layers, re-order and test cancellation of expensive operations.
4. Save the project, close app and reopen using normal UI.
5. Assert layer state, visibility, camera, symbols, project source URIs and coordinate display.
6. Export and independently reopen the result in GDAL/QGIS/format reader; compare approved metadata invariants.
7. Capture `command_id` → `trace_id` handoffs; label first failing boundary.

## Scenario C: native WebView2 communication

Use a test-enabled temporary, **loopback-only** DevTools endpoint only when the target app explicitly supports it. Verify DOM/UI event reaches native bridge and GIS operation, map refresh and readback. Playwright cannot inspect the native file picker or WPF Ribbon; use FlaUI or pywinauto in tandem. Never export cookies, OAuth tokens, sensitive coords or private screenshots.

## Scenario D: errors, accessibility and packaging

Test corrupted/unavailable files, offline basemap, errors, high DPI, RTL/LTR, keyboard-only, focus order, accessible icons, low memory, crash restart and installer/upgrade/uninstall on an authorized clean Windows image. Collect actual PASS/FAIL evidence with OS/app/runtime versions. When no Windows runner or target app is available, **T3/T4 = NOT RUN**, not skipped-and-passed.

## Report row schema

```json
{"test_id":"E2E-IMPORT-001","command_id":"import.kml","trace_id":"TRACE-REDACTED",
 "platform":"Windows (version needed)","app_build":"unknown",
 "expected":"Imported layer renders and survives save/reopen/export",
 "actual":"NOT RUN (native target not supplied)","status":"NOT RUN",
 "tier":"T3","evidence_uri":null}
```

The [Command Audit](../ui-command-audit/README.md) and [Data Flow](../data-flow-diagnostics/README.md) provide synthetic test artifacts for upstream inputs.
