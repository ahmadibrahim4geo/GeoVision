# Archify integration — 27 October 2026

## Result and invocation

The main GIS router now discovers [GIS Architecture & Workflow Visualization](../skills/gis-architecture-visualization/SKILL.md) as a conditional shared specialist. The intent orchestrator captures its purpose/type/evidence/language; the offline compiler validates the handoff while keeping `can_execute: false`. Domain specialists retain implementation and GIS test ownership. The visualization specialist owns only the explanatory artifact unless the entire requested task is a diagram.

Use for explicit diagram explanations, reviewed multi-component feature designs, cross-component diagnosis that benefits from a map, and structural changes. Skip isolated labels/colors/icons and honor text-only requests. End users ask in normal language, open the returned HTML and request revisions in the same conversation. The homepage provides bilingual copyable requests and a worked example.

## Actual verification

| Check | Observed result |
| --- | --- |
| Full offline regression | **345 tests passed**, including six new routing/handoff tests |
| English/Arabic discovery; isolated-edit exclusion | Passed |
| Existing tasks; retained domain ownership; unapproved plan gate | Passed |
| Missing/invalid visualization metadata and forged runtime flags | Rejected by validation as expected |
| Independent use on label edit, role explanation and KML diagnosis | Three handoffs compiled; no tool/application execution claimed |
| Skill pack, local links, manifest and skill-flow graph | Passed: **33 skills**, 188 project references, 52 APIs, 24 data repositories |
| Security Guardian offline audit | Passed: zero findings; external scanners **NOT RUN** |
| Inline JavaScript syntax | Passed for homepage, Explorer and both generated example scripts |
| Author panel | Exact markup unchanged from the inspected main snapshot |
| Real Archify workflow schema/layout validation | **PASS**, quality `showcase` |
| Real Archify HTML delivery and strict artifact check | **PASS** |
| Archify browser check | **SKIPPED / NOT RUN**: no Chrome/Chromium/Edge executable available |
| Human visual/Arabic RTL inspection | **NOT RUN** |
| Native ArcGIS/QGIS/Windows/mobile tests | **NOT RUN / outside this documentation integration** |

The full regression suite initially caught the router's size limit. Detailed visualization rules were kept in the new specialist/reference guide and the main router was shortened without relaxing the test; the final suite passed.

## Runtime and provenance

The actual render used Archify **3.2.0**, source commit `81c6be8d87090670c45d08a8f97f9d29c5af0bec`, and Node **v24.22.1**. A temporary runtime was retrieved from that immutable source; 135 runtime/license files matched their upstream Git blob hashes. No Archify engine was vendored into the skills pack and no paid renderer or API key was introduced. The [integration guide](../references/archify-integration.md) documents external-runtime discovery/setup, upstream Apache-2.0/component notices and incomplete full Arabic RTL support.

The [example HTML](../examples/architecture-visualization/skill-routing.html) is an authored explanation of the selected visualization branch, **not a live trace**. Its source is [editable workflow JSON](../examples/architecture-visualization/skill-routing.workflow.json). HTML SHA-256: `4bbf8de29079f67dc0b28dea84c6c83acdd33e6889901e0a58bb8d09b34d8ba9`. Upstream runtime/software notices remain embedded.

Actual finalize summary in the development log records three passed gates and the skipped browser gate. The diagram remains **PARTIAL for browser/visual acceptance**; schema/render success must not be described as browser inspection or native GIS proof. Installation of this GIS specialist does not automatically install the external renderer or browser. A missing optional diagram dependency does not block unrelated domain work.
