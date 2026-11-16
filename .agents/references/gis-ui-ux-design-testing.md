# GIS UI/UX Design and Testing — design system to user acceptance

The [specialist](../skills/gis-ui-ux-design-testing/SKILL.md) owns application experience, not map geometry semantics. Use [quality coordination](gis-ui-quality-coordination.md), [upstream UI libraries](gis-ui-quality-upstream-sources.md), [GIS Cartography](../skills/gis-cartography-symbology/SKILL.md) and [Internationalization](../skills/gis-internationalization-arabic/SKILL.md).

## 1. User research and information architecture

Identify concrete GIS user stories: load datasets, inspect attributes, select/filter features, manage layer tree, navigate 2D/3D, find a gazetteer place, draw/edit, set symbology, compute a spatial analysis, save/reopen and export. Document target audience (novice vs professional), primary task frequency and accessible alternatives. Keep controls in the user's GIS mental model, not the source-code folder tree.

Design a navigation hierarchy for menus/Ribbon, panels, map canvas, Layers/Places, context menus, dialogs, search, status bar, legend, coordinate readouts and actions. Avoid hiding destructive operations behind visually ambiguous icons. Make disabled commands honest, with reason and guidance.

## 2. Reusable design system

Record per supported host:
- Design tokens: 4/8-point spacing scale where suitable, typography families/fallback, density, radius, contrast, neutral accent, theme mapping and visual hierarchy.
- Component contract: `name`, `semantic_role`, `accessible_name`, `command_id`, `states` (idle, focused, hovered, disabled, loading, error, success), `shortcut`, `aria`/UIA properties and localization behavior.
- Large project/long task support: progress and cancellation, save/dirty-state indicators, nonblocking alerts, recovery from invalid input, display of CRS warnings and unit selection.
- Responsiveness: dock panels and map never collapse at minimum supported width; symbols remain visible at 100/125/150/200% DPI; test multi-monitor transitions on supported desktops.
- Two-language parity: English LTR avoids leftover Arabic UI strings; Arabic RTL supports BiDi, mixed numeric CRS identifiers and actual map label shaping. Names of technical engines may legitimately remain untranslated.

Map styles/feature classes are governed by Cartography, and font direction/search normalization by Internationalization. UX owns how they are **presented and controlled**.

## 3. Usability and accessibility evaluation

Test more than static visuals:

| Dimension | Observable check |
| --- | --- |
| Task success | User can finish the intended GIS operation with correct file and map outcome |
| Navigation | Search and layer tree discoverable; back/undo/cancel clear |
| Input | Mouse/touch/keyboard, no focus traps, accessible labels for icon-only controls |
| Feedback | Loading, empty, error, done and disabled reason states informative |
| Contrast and color | Readable labels and non-color status encoding; map and UI assessed separately |
| Layout | Text expansion, long feature names, RTL/LTR, high DPI and multiple screen sizes |
| Behavior | Undo/redo, dialog close, keyboard shortcuts, scale controls, map interaction and persistence |
| Regression | Visual diff in pinned environment plus semantic interaction/assertions |

Use axe-core/Storybook for eligible web components; Accessibility Insights/axe-windows for accessible Windows UI; real keyboard and screen-reader smoke checks cannot be substituted by automated axe alone. False visual diffs may result from fonts, GPU antialiasing, locale, DPI and tile/network changes: freeze test input and environment. Do not copy another project's branding or icons without permission.

## 4. UX → command → E2E handoff

Every actionable mock or implementation should include stable `command_id`, expected UI state and acceptance criteria. [Command Audit](../skills/gis-ui-command-audit/SKILL.md) determines whether it is bound, [Data Flow](../skills/gis-data-flow-diagnostics/SKILL.md) traces the GIS payload, and [E2E](../skills/gis-desktop-e2e-testing/SKILL.md) verifies the journey. **Do not mark UX finished if a beautiful button does nothing.**

## 5. Acceptance and evidence

Prepare a one-page user journey matrix, design token list, accessibility/keyboard/RTL checks, pinned screenshot baselines, observed discrepancies, blocked/native tests and proposed remediations. The [example checklist](../examples/ui-ux/README.md) can be used for desktop, browser or plugin applications. UI design artifacts don't authorize source edits; respect the host-approved plan and user-controlled visual identity.
