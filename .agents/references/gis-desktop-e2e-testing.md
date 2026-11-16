# GIS Desktop E2E Testing — Windows UIA & WebView2 playbook

Use the [E2E specialist](../skills/gis-desktop-e2e-testing/SKILL.md) after the common [prompt-to-skill orchestration](gis-ui-quality-coordination.md).

## 1. Platform-specific driver selection

- **WPF/WinUI/WinForms:** preferred .NET automation via [FlaUI](https://github.com/FlaUI/FlaUI) or Python [pywinauto](https://github.com/pywinauto/pywinauto) with UI Automation. UI Automation IDs and accessible names should be shipped in the app; window titles/localized labels are fallback locators, not stable primary IDs.
- **Qt/PySide:** Qt-level widget tests (e.g. pytest-qt) can validate widget behavior; actual Windows GUI acceptance should use a real OS/user session, Qt accessibility and native events. Native widgets can expose UIA, depending on application/toolkit.
- **WebView2/Electron:** Playwright can inspect a test-enabled browser/embedded web document under compatible debugging support; **native shell dialogs, Ribbon and file picker may still require FlaUI**. Do not pretend Playwright automatically sees native WPF controls.
- **ArcGIS Pro/QGIS hosted UI:** install the compatible real host/version and sample project. Plugin test harnesses are not proof that all Windows GUI paths work.

## 2. Controlled setup

- Pin the exact app build, commit, native Windows version, user-session type, UIA version, WebView2 runtime, GIS libraries, graphics driver, DPI, screen resolution, language and available RAM/VRAM.
- Generate fixtures from public/synthetic data with expected CRS, geometry, map extent, elevation variation, known icons and output hash. Do not use production GIS data as disposable test inputs.
- Wait on semantic readiness (`Ready`, window visible, render complete) instead of blind sleeps. Timeouts need logged stages and retries only where safe.
- For headless build agents where interactive desktop unavailable, skip with `NOT RUN`; don't mock a native GUI test into a PASS.

## 3. Priority scenarios

| Scenario | UI action | Independent assertion |
| --- | --- | --- |
| App lifecycle | Launch, maximize/minimize, close/reopen | Window, persisted state, no unhandled exception |
| Import (GeoJSON / KML / KMZ / TIFF) | Open file picker, select fixture | Layer is present, correct count/CRS/extent and original styles |
| Navigation | Pan, zoom, rotate, full extent, 2D/3D, reset north, previous/next | Camera state changes correctly; renderer never disappears |
| Layers & Places | Toggle, reorder, rename, search, expand | Tree and map synchronized; hidden layer actually not rendered |
| Save/restore | Save, exit, reopen the project | Extent, symbols, visibility and GIS file links restored |
| Export | Export feature/raster/KML/KMZ | Reopen output using independent reader; compare approved invariants |
| Errors | Invalid file, unavailable service, cancelled heavy job | Useful safe notification; no corrupt project or crash |
| Accessibility | Keyboard-only, screen reader, high-DPI, Arabic/English | Correct tab/focus order and no unlabeled controls |
| Installer | Install/upgrade/uninstall on clean Windows | Correct dependencies, no stale runtime, project data preserved |

## 4. WebView2 host-bridge assertions

Maintain two independent IDs: `command_id` for user action and `trace_id` for GIS data operation. Test `UI click -> JS handler -> native bridge -> service -> engine -> scene refresh -> save/export`, not only DOM state. Specify allowed bridge methods, schema validation, origin checks, cancellation and timeout. Any DevTools debugging endpoint is temporary, localhost-only, nonproduction and host-authorized; tests must shut it down and purge captured secrets.

## 5. Artifacts

Record one E2E case per user outcome: `test_id`, build hash, platform, app/OS logs, locator, gesture, expected/actual GIS state, data fixture, readback evidence, screenshot only when safe, `command_id`, `trace_id`, `status` and `tier`. The test owner's verdict must not be inferred from UI text alone.

`examples/desktop-e2e/` contains a portable acceptance checklist. An example or GitHub Linux test is not a real Windows E2E execution. Real T3/T4 require appropriate target app, interactive environment, necessary approvals and independent evidence.
