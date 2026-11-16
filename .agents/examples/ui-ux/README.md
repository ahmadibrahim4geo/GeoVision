# GIS UI/UX acceptance checklist — reusable across all platforms

This is a review worksheet, **not a finished design nor proof of accessibility**. Begin with the user's approved interface identity/theme and real GIS workflow. Document which elements are desktop-native, web, mobile or host-provided.

| User-facing feature | Design/functional acceptance | Test method |
| --- | --- | --- |
| App navigation / Ribbon | Consistent command grouping, readable tooltips and findable actions | User walkthrough; Command Audit |
| Layers and Places | Search, reorder, collapse, visibility changes reflected in map | Real UI E2E and GIS assertions |
| Import and export | Correct file type options, progress, error messages, cancel | Real GIS fixture and independent readback |
| Coordinate grid / labels | Independent controls, CRS/axis units clear, correct LOD | CRS specialist and real renderer |
| Map search/gazetteer | Responsive results, keyboard choices, correct place zoom | E2E plus API/Gazetteer providers |
| Arabic and English | RTL/LTR mirroring where needed, mixed Latin IDs readable, no unintended language bleed | Locale-driven native/browser tests |
| Keyboard/accessibility | Focus indicators, reachable dialogs, icon accessible names, no focus traps | UIA/axe, real keyboard and screen reader |
| Window sizes / themes | No collision/overflow at supported DPI, high contrast/light/dark | Pinned screenshot and manual review |
| Errors and empty states | Clear nontechnical action to recover, not a silent failure | Fault injection / E2E |
| Project persistence | Stable map state and restored panels on reopen | E2E / Data Flow |

Design tokens must be reusable and recorded with their platform mapping (CSS, Qt, WPF/WinUI); avoid forcing one UI library on every user. Label every actionable mockup with a stable `command_id`; hand to [Command Audit](../../skills/gis-ui-command-audit/SKILL.md) and [E2E](../../skills/gis-desktop-e2e-testing/SKILL.md). Track keyboard and contrast issues without assuming automated scanner coverage is complete.

**Testing tiers:** T0 design/semantic contract, T1 component/browser simulation, T2 GIS engine integration, T3 real interactive UI, T4 packaged user scenario. Report NOT RUN when missing environment. Do not alter user-owned branding or author footer without their requested scope.
