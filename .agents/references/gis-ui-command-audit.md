# GIS UI Command Audit — complete engineering playbook

The [skill](../skills/gis-ui-command-audit/SKILL.md) owns **inventory and binding traceability**. Read the [shared handoff](gis-ui-quality-coordination.md) first. This is *not* a substitute for real desktop [E2E](../skills/gis-desktop-e2e-testing/SKILL.md).

## 1. Source-first discovery

| UI framework | Primary surfaces | Handler resolution and caveats |
| --- | --- | --- |
| WPF / WinUI XAML | Button, MenuItem, ToggleButton, RibbonButton, ToolBar, ContextMenu, CommandBar | Parse XAML ID, Command, Click, CommandParameter, InputBindings and template styles. Command may be registered indirectly; dynamic templates need runtime enumeration |
| Qt / PySide / PyQt | QAction, QPushButton, QToolButton, QMenu, QShortcut, generated .ui | Check .ui widgets plus Python/C++ source `triggered.connect`, `clicked.connect`, menus, actions and plugins |
| Electron / Tauri / WebView2 | HTML button/link, custom elements, keyboard handlers, context menu | DOM static declarations plus listener registrations and host bridge IPC; SPA-generated controls appear only after runtime |
| WinForms | *.Designer.cs, MenuStrip/ToolStripButton and Click events | Inspect generated initialization and actual registered event delegates |
| ArcGIS Pro / QGIS plugin | DAML/Qt layouts, Python, C# | Inspect host-specific command IDs and active map/project requirements; host availability may block test |

Scan read-only on a pinned commit, capture patterns and unsupported syntax. The example command below **only** finds declarations in basic HTML and XAML; it cannot discover dynamically created C#/Qt commands. Never inflate static coverage statistics as though that entire app was audited.

```bash
python scripts/gis_ui_command_audit.py --root examples/ui-command-audit/fixture --format json
python scripts/gis_ui_command_audit.py --root path/to/approved/project --format json
```

## 2. Inventory fields

Minimum: `command_id` (stable, or a clearly labeled generated candidate), `surface`, `label`, `icon_ref`, `source_file`, `ui_element_id`, `binding_kind`, `binding_target`, `static_status`, `runtime_status`, `expected_outcome`, `verification_id`. A missing binding should be `NO_BINDING`; a declared handler should be `DECLARED_BINDING`; a template/dynamic unknown should be `UNINSPECTED`. All entries start with `runtime_status: NOT RUN`.

Optional metadata: shortcut, enable condition, engine service, command preconditions, UI locale, window/panel, permission, read/write side effects, rollback, keyboard accessibility. For each row record *which source* supports it, not a made-up handler name.

## 3. Real binding validation

1. Resolve XAML / Qt / HTML action declaration to handler, command object or event listener.
2. Resolve the handler to the application service, IPC bridge, GIS engine call and eventually the layer/model mutation.
3. Verify exception/error handling, concurrency/cancel paths, authorization, thread affinity, progress indicator and disabled state when unavailable.
4. Confirm icon content, path, license and visual label match command semantics. Missing SVG/path != missing handler. Unlabeled icon-only controls fail accessibility review.
5. Inspect grouped commands, keyboard alternatives and undo/redo behavior; detect multiple UI controls sharing a handler inappropriately.
6. Run interactive E2E with a deterministic fixture; assert the actual GIS output, not just dispatch.

## 4. Defect severities

- **Blocker:** destructive/wrong dataset change, hidden unintended network call, command corrupts files, unsafe arbitrary code.
- **High:** command does nothing, incorrect action, crashes, drops data silently, KML styles lost in roundtrip.
- **Medium:** disabled/wrong precondition, unreachable shortcut, icon/label mismatch, missing error feedback.
- **Low:** missing tooltip, awkward focus or icon padding where operation is otherwise fully usable.
- **NOT RUN:** source not present, unsupported generator, no native host. Do not label this failure without evidence.

## 5. Reporting

Create a reproducible summary: counts by surface and static classification, counts by native outcome, files scanned, unsupported frameworks, test IDs and required Windows/QGIS/GIS hosts. A new registry of commands should be generated from code, not a duplicated hand-maintained inventory divorced from the UI. Use the [common handoff](gis-ui-quality-coordination.md), [GIS Desktop](../skills/gis-desktop-application-development/SKILL.md), [UX](../skills/gis-ui-ux-design-testing/SKILL.md) and [Data Flow](../skills/gis-data-flow-diagnostics/SKILL.md).
