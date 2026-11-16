# Public upstream references for four GIS UI quality skills

This directory records GitHub repository existence and *repository-level reported license identifiers* checked on **2026-10-26**. These are references only: **nothing here is installed, embedded, build-verified or license-cleared for redistribution.** Inspect exact upstream revision, LICENSE/NOTICE and transitive dependencies before source reuse. Do not copy third-party source code by assumption.

## Command discovery and static bindings

| Upstream | Use | GitHub license metadata |
| --- | --- | --- |
| [dotnet/roslyn](https://github.com/dotnet/roslyn) | C# syntax/semantic analysis, WPF command handlers, symbol references | MIT |
| [tree-sitter/tree-sitter](https://github.com/tree-sitter/tree-sitter) | Multi-language syntax parsing for Qt/JS/C++ tool declarations | MIT |
| [semgrep/semgrep](https://github.com/semgrep/semgrep) | Optional project-specific static checks for command wiring and unsafe patterns | LGPL-2.1 |
| [github/codeql](https://github.com/github/codeql) | Optional deep code data-flow queries for large codebases | MIT (repository metadata; deployment licensing may differ) |

## Windows desktop E2E and WebView2

| Upstream | Use | GitHub license metadata |
| --- | --- | --- |
| [FlaUI/FlaUI](https://github.com/FlaUI/FlaUI) | Windows UI Automation UIA2/UIA3 for .NET apps | MIT |
| [pywinauto/pywinauto](https://github.com/pywinauto/pywinauto) | Python Windows GUI automation | BSD-3-Clause |
| [microsoft/playwright](https://github.com/microsoft/playwright) | Browser or eligible embedded WebView2 DOM tests | Apache-2.0 |
| [microsoft/playwright-dotnet](https://github.com/microsoft/playwright-dotnet) | Playwright APIs for .NET | MIT |
| [MicrosoftEdge/WebView2Samples](https://github.com/MicrosoftEdge/WebView2Samples) | Windows host/WebView2 bridge and lifecycle examples | Unverified repository license |
| [appium/appium-windows-driver](https://github.com/appium/appium-windows-driver) | Optional Windows WebDriver route; verify WinAppDriver/runtime support | Apache-2.0 |
| [microsoft/WinAppDriver](https://github.com/microsoft/WinAppDriver) | Windows driver sample ecosystem; examine current compatibility/maintenance | MIT |
| [pytest-dev/pytest-qt](https://github.com/pytest-dev/pytest-qt) | Python Qt widget tests in suitable test environments | MIT |

**Hard boundary:** Native UIA requires Windows and a usable interactive user session. Playwright does not automatically control native WPF or Qt controls; DOM automation and Windows UIA may both be needed. An embedded WebView2 can expose a CDP endpoint only when the target host opts into appropriate debugging; never turn on remote debugging globally or expose it to public networks.

## GIS data-flow diagnostics and tracing

| Upstream | Use | GitHub license metadata |
| --- | --- | --- |
| [open-telemetry/opentelemetry-dotnet](https://github.com/open-telemetry/opentelemetry-dotnet) | Cross-component spans from WPF/WinUI/C# application to GIS adapters | Apache-2.0 |
| [open-telemetry/opentelemetry-python](https://github.com/open-telemetry/opentelemetry-python) | Trace IDs, spans and logs for Python GIS workers | Apache-2.0 |
| [open-telemetry/opentelemetry-js](https://github.com/open-telemetry/opentelemetry-js) | Webview/JS map-bridge traces | Apache-2.0 |
| [open-telemetry/opentelemetry-collector](https://github.com/open-telemetry/opentelemetry-collector) | Optional local/authorized trace collector | Apache-2.0 |
| [Serilog/serilog](https://github.com/serilog/serilog) | Structured .NET logs with correlation IDs | Apache-2.0 |
| [OSGeo/gdal](https://github.com/OSGeo/gdal) | Real raster/vector provider behavior and independent readback | Mixed / verify exact source LICENSE |
| [qgis/QGIS](https://github.com/qgis/QGIS) | QGIS runtime tests and API usage | GPL-2.0 per GitHub metadata |

## UI/UX design, accessibility and component tests

| Upstream | Use | GitHub license metadata |
| --- | --- | --- |
| [microsoft/fluentui](https://github.com/microsoft/fluentui) | Interface patterns, Fluent design components | Unverified repository license |
| [microsoft/microsoft-ui-xaml](https://github.com/microsoft/microsoft-ui-xaml) | Native WinUI styles and control examples | MIT |
| [microsoft/WindowsAppSDK](https://github.com/microsoft/WindowsAppSDK) | Windows app platform API samples | MIT |
| [penpot/penpot](https://github.com/penpot/penpot) | Open-source UI prototyping and reusable component design | MPL-2.0 |
| [storybookjs/storybook](https://github.com/storybookjs/storybook) | Documentation and testing web UI component states | MIT |
| [style-dictionary/style-dictionary](https://github.com/style-dictionary/style-dictionary) | Cross-platform design-token builds | Apache-2.0 |
| [dequelabs/axe-core](https://github.com/dequelabs/axe-core) | Web accessibility checks | MPL-2.0 |
| [microsoft/axe-windows](https://github.com/microsoft/axe-windows) | Automated Windows accessibility diagnostics | MIT |
| [microsoft/accessibility-insights-windows](https://github.com/microsoft/accessibility-insights-windows) | Inspection of Windows accessibility tree | Unverified repository license |

## Adoption policy

Choose tools by the target application, OS, language, real GIS renderer and permission boundary. Preserve free-first solutions but distinguish free testing libraries from commercial/premium IDE, OS, GIS-host and cloud service terms. Prefer no additional dependency in basic T0/T1 tests. Upgrade only after observing a functional gap with a narrow prototype. Documentation and screenshots are not proof of native interoperability. Record `reference-only`, `experimental`, `verified-in-target` explicitly; `verified-in-target` requires logs from a real target runtime and commit.
