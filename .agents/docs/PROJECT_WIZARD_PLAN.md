# GIS Project Wizard — implementation plan

## Scope and architecture
- Bilingual Arabic/English guided project selection with accessible keyboard-operable controls.
- Paths: Desktop GIS, ArcGIS Pro toolbox, ArcMap toolbox, dual-host toolbox, QGIS plugin, Web GIS, Remote Sensing, Spatial Database and GeoAI.
- Select free/open-source first; disclose ArcGIS and third-party service licensing. Never promise that a proprietary host is free.
- Generate an editable Markdown plan and AI-agent instructions, with explicit skill paths, risks, implementation phases and acceptance checks.
- Recommend a compatible stack, not a universal stack. For dual ArcGIS hosts, keep a Python-2.7-compatible core where feasible and isolate Python 3/ArcGIS Pro adapters; verify real host APIs.
- Include a mandatory reliability plan: input sizes, memory budget, CPU and I/O measurements, cancellation, progress, logging, crash recovery, corrupt files, stress and regression testing.
- Add language selection and Arabic-first RTL/BiDi, Unicode attribute, map label and export acceptance checks; see skills/gis-internationalization-arabic/SKILL.md.\n- Include KML/KMZ whenever file interchange is selected. Clearly separate feature conversion from document-fidelity rendering.

## Delivery gates
1. Static UI and bilingual workflow, no server required.
2. Reference-driven recommendations and machine-readable manifest.
3. Real-world fixture suite: valid and malformed KML/KMZ, huge inputs, nested folders, altitude, overlays and extensions.
4. Unit tests and CI for validation; report exact test coverage and limitations.
5. Native host acceptance in ArcGIS Pro, ArcMap, QGIS, Cesium and desktop shells only when environments are available.
6. Browser accessibility and responsive/mobile smoke tests before release.

## Acceptance criteria
- A user can complete the wizard offline and export a meaningful plan.
- All suggested paths resolve to existing files or are explicitly marked planned.
- English and Arabic have equivalent meaning, without mixed UI labels.
- No performance or compatibility claim without measured evidence.
- KMZ processing never extracts untrusted archive members without path/size checks.

## Interactive behavior and language contract (2026-10-25)

- The complete form is local-first: no fetch requests, APIs, user tracking, model calls or server-side state.
- English and Arabic switch all project-type options, field labels and instructions. Plan headings and the downloadable Markdown match the active interface language.
- A language change with an existing plan regenerates the same requirements in the selected language without discarding typed text.
- Edits to project type, application language, KML/KMZ, licensing or project description invalidate prior output and disable download until a fresh plan is generated.
- Each project type selects its corresponding specialist skill; KML/KMZ and Arabic localization references are included only when the relevant options are selected.
- Markdown output uses plain text via `textContent`, and quoted user requirements: the wizard never interprets input as HTML or executes it.
- CI runs Node-backed simulated DOM interaction scenarios for generation, language switching, invalidation and download. **Native GIS runtimes and real visual browser compatibility remain separate acceptance levels.**

## Implementation status
- This document is a roadmap, not proof of implementation.
- Static wizard and linked engineering references are the first implementation increment.
- Native GIS runtime and load/stress acceptance remain outstanding.

## Optional GIS UI quality workflow (2026-10-26)

The wizard now offers a bilingual **quality focus** choice: general (default), UI Command Audit, Desktop E2E, GIS Data Flow Diagnostics, UI/UX Design & Testing, or all four. Selecting one adds the corresponding specialist link to the human Markdown plan and the machine-readable **draft** JSON workflow; it does not install tools or claim tests. Changing the choice invalidates prior plan downloads. End-to-end Windows tests are only planned unless run in a real interactive Windows session; other platforms do not silently acquire Windows-only dependencies.

See [coordination contract](../references/gis-ui-quality-coordination.md) and [public upstream sources](../references/gis-ui-quality-upstream-sources.md). Build/packaging checks remain separate from static syntax and UI simulation checks.
