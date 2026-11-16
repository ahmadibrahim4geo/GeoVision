# Cross-skill GIS UI quality coordination

**Scope:** common language and handoff for `gis-ui-command-audit`, `gis-desktop-e2e-testing`, `gis-data-flow-diagnostics` and `gis-ui-ux-design-testing`. This applies equally to independent GIS desktop programs, Qt/WPF/WinUI, plugins, map websites and field applications. No named application is a required dependency.

## Prompt and permission gateway

The host follows [goal-first orchestration](repository-wide-skill-orchestration.md). User prompt → user goal and outcomes → reviewable task plan → GIS Router → minimal matching specialists + [Security Guardian](../skills/gis-security-guardian/SKILL.md) → authorized preflight → actual execution → evidence-based report. Skill documentation itself does not intercept commands, authenticate approvals or run native apps. Read-only analysis need not trigger repeated approval requests.

## One owner per operation

| Operation | Single accountable skill | Required consumers |
| --- | --- | --- |
| UI design tokens, layout, usability and keyboard flow | [UI/UX Design & Testing](../skills/gis-ui-ux-design-testing/SKILL.md) | Command Audit and E2E |
| Inventory, source declaration of bindings, icon paths, shortcut mapping | [UI Command Audit](../skills/gis-ui-command-audit/SKILL.md) | E2E and UX |
| True UI interaction and assertion in installed/running app | [Desktop E2E](../skills/gis-desktop-e2e-testing/SKILL.md) | Command Audit, UX, Data Flow |
| Import → engine → render → save/export lineage and metadata integrity | [Data Flow Diagnostics](../skills/gis-data-flow-diagnostics/SKILL.md) | E2E and GIS algorithm specialists |
| Geographic CRS accuracy | [CRS](../skills/coordinate-reference-systems/SKILL.md) | Data Flow |
| KML/KMZ document/style fidelity | [KML/KMZ](../skills/kml-kmz-engineering/SKILL.md) | Data Flow and E2E |
| Scientific map symbols | [Cartography](../skills/gis-cartography-symbology/SKILL.md) | UX |
| Arabic shaping and i18n storage | [Internationalization](../skills/gis-internationalization-arabic/SKILL.md) | UX/E2E |
| Risk, secrets, untrusted input, permissions | [Security Guardian](../skills/gis-security-guardian/SKILL.md) | All four |
| Host architecture and packaging | [Desktop Development](../skills/gis-desktop-application-development/SKILL.md) | All four as needed |
| Evidence and project history | [Development Journal](../skills/agent-development-journal/SKILL.md) | All four |

## Minimal common handoff envelope

Record in a project-local JSON report (or equivalent) using:
- `request_id`, `plan_revision`, `task_id`, `target_platform`, `git_commit` and `app_build`;
- `command_id` (stable through UI declaration, handler, service, engine operation and E2E assertion);
- `trace_id` for one user operation and its related data spans; `dataset_id` for non-sensitive source identity;
- `ui_element_id` or `automation_id` when supplied by the platform, but never invent one as evidence;
- `phase` for data flow; `source_file` and `handler` for static command bindings; `locale`, `dpi` and `driver` for UI tests;
- `expected_outcome`, `actual_outcome`, `test_tier`, `status`, `evidence_uri`, `error_code` and `limitations`.

Status vocabulary for tests: **PASS**, **FAIL**, **BLOCKED**, **NOT RUN**. Offline discovery statuses: **DECLARED_BINDING**, **NO_BINDING**, **UNINSPECTED**. Never turn `DECLARED_BINDING` into `PASS` automatically. A running GUI with a button click is not proof of GIS readback. A trace event can be emitted even if the actual renderer silently failed.

## Command-to-data trace

```text
User gesture / keyboard shortcut (command_id)
   -> UI command/binding (source file and automation_id)
   -> application controller / service (task_id and trace_id)
   -> GIS provider, parser or algorithm (dataset_id and phase)
   -> map renderer / layer store (visible state and CRS)
   -> project save / export writer (output path reference)
   -> independent readback and semantic acceptance evidence
   -> E2E assertion / log / Development Journal
```

Do not duplicate processing algorithms in UI. Any failed handoff identifies its **first observed failing boundary**, assigned to the owning specialist. Do not blindly reinstall a library or switch GIS engines.

## Phase and risk contract

- `IMPORT`, `PARSE`, `CRS`, `PROCESS`, `ENGINE`, `RENDER`, `SAVE`, `EXPORT`, `READBACK` describe logical stages. A specific workflow selects its required subset, but the order must be coherent.
- Preserve stable correlation IDs across C#, JS, Python, Qt and WebView2; pass opaque IDs, not raw coordinate contents.
- Before comparing features, record whether the operation is lossless, filter, aggregate, clip, simplify or reproject. Equal hashes are not a universal CRS or geometric correctness test.
- Never upload private GIS data, user positions, credentials, screenshots or whole project logs as telemetry by default. Opt-in debug collection uses redaction and retention limits.

## Acceptance matrix

| Tier | What can be asserted | What it cannot assert |
| --- | --- | --- |
| T0 | Skill links, command/trace schemas, static discovery and lint | Actual handler execution |
| T1 | Offline synthetic source inventory, trace order/count validators, deterministic fixture tests | Live map or GUI behavior |
| T2 | Actual GIS runtime I/O and engine integration with known fixture | Full end-user UI coverage |
| T3 | UIA/Playwright/Qt interaction, keyboard and visible GIS outcome | Clean-machine installation |
| T4 | Installer, first-run, package, saved project reopen and recovery | Unrelated platform behavior |

**Release rule:** Missing T2/T3/T4 evidence must be marked **NOT RUN**; do not downgrade scope to obtain an artificial green status. A native Windows runner is required for UI Automation; GitHub Linux CI tests only T0/T1.

## Conflict handling and reporting

If UX wants to relocate a button while Command Audit/E2E refer to an ID, preserve stable command ID and update UI locator mapping. If Data Flow detects a lossless feature-count mismatch, stop claiming successful import even when the UI looks correct. If one test fails, identify root ownership, make minimal reviewed repair and rerun that path plus regressions. Report affected files, target build, actual test logs and untested platforms.

**System integration ownership:** [System Capabilities & Permissions](../skills/system-capabilities-and-permissions/SKILL.md) defines API/permission/fallback contracts; Security owns authorization, UX designs consent, Desktop E2E tests real allow/deny/revoke. A static candidate registry does not prove OS consent.
