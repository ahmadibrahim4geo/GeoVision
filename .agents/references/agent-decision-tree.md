# GIS Agent Decision Tree

For general tasks start with the [free-first policy](free-first-policy.md). For a request explicitly tied to ArcGIS or Global Mapper, keep its intended host but disclose commercial licensing and independently suitable free alternatives.

For multi-step Arabic/English user requests, first use [Prompt Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) and the [plan/review contract](intent-orchestration-workflow.md). Review the task graph and obtain user approval for a specific revision before nontrivial changes; authenticate high-impact permissions separately in the actual tool host.

For multi-stage project work use [Agent Development Journal](../skills/agent-development-journal/SKILL.md), recovering and verifying project-local reports before the next action and logging a new event after each meaningful operation. A log never substitutes for authenticated approvals or T3/T4 native acceptance.

Select **the delivery target**, not just the programming language. The general [router](../skills/gis-development/SKILL.md) stays small; this file holds details.

1. Is the user building inside ArcGIS? **ArcMap 10.x:** Python 2.7 and `arcpy.mapping` where applicable. **ArcGIS Pro:** supported Python 3 / ArcPy / `arcpy.mp`; distinguish `.pyt`, script tool and Pro SDK add-in. Consult the [official Esri Python distribution checker](esri-python-environment-compatibility.md) for package compatibility, and use C# SDK snippets only for Pro-hosted add-ins.
2. Global Mapper from Blue Marble Geographics? Route to [Global Mapper Development](../skills/global-mapper-development/SKILL.md): distinguish `.gms`, embedded Python and commercially licensed SDK/DLL; check edition and feature entitlements. Source forks on GitHub are not the Global Mapper engine.
3. Inside QGIS? **Processing algorithm:** Processing provider interface. **Plugin:** PyQGIS/Qt with real action wiring. **Standalone QGIS libraries/engine:** [QGIS Engine Integration](../skills/qgis-engine-integration/SKILL.md) plus independent Desktop engineering. For a non-Qt host consider an optional headless PyQGIS worker, keeping the original renderer and applying license/runtime gates.
4. Is the output interactive web mapping (including time-aware satellite globes)? Read the [Earth View design review](earth-view-architecture.md) only when 3D imagery/temporal regional inspection is relevant.  **2D/vector:** MapLibre or Leaflet according to features and licensing. **3D terrain/globe:** CesiumJS or other verified engine. For raster/geometry analysis also load the independent processing specialist.
5. Building an Android/iOS GIS application? Route to [Mobile GIS Development](../skills/gis-mobile-application-development/SKILL.md) with [offline synchronization](mobile-gis-offline-sync.md), [secure sign-in](mobile-gis-authentication.md) and the [native mobile test matrix](mobile-gis-device-test-matrix.md). Do not substitute Windows Desktop E2E tests for Android/iOS.
6. Is there a database? **PostGIS/SpatiaLite:** schema, SRID, spatial index, transactional migrations, EXPLAIN plans. Don't transform coordinates by changing their SRID labels.
7. Raster/vector algorithms, massive features, reprojections? Route to **geospatial data processing**; use windowed raster reads and chunking where measurements justify them.
8. Satellite imagery/STAC/COG/Earth Engine? Route to **remote sensing pipelines** and data discovery. Verify sensor scales, per-pixel QA and target grid.
9. GIS data repositories, catalogs or provider directories? **Find source index:** data catalogs; **find individual provider:** data discovery. For STAC natural-language search add GeoAI agent engineering only when needed. GIS data or live services? **Find provider:** data discovery. **Use HTTP endpoints:** API integration, quotas, auth and provenance. Hosted WMS map tiles aren't necessarily analysis rasters.
10. GIS AI agent or automation? Route to **Geospatial AI Agents** for safe tools, confirmation and evidence. DEM, river networks, terrain or point-cloud work? Add **Terrain / Hydrology / LiDAR**. GIS desktop standalone? Use Desktop skill, with a renderer and spatial computation layer separated.
11. Coordinate systems, vertical units or datum transformation anywhere? Add CRS skill as **cross-cutting**, not an alternative.
12. Cartographic symbolization, unified GIS Icon Manager, free/open map icons, KMZ placemarks, scientific color ramps, legend or raster styling? Add [GIS Cartography & Symbology](../skills/gis-cartography-symbology/SKILL.md), then compose target ArcGIS, QGIS, web or desktop adapter. Device clock, time-zone, consent-based device location or Sunlight? Add [GIS Device Context](../skills/gis-device-context/SKILL.md); keep scene location independent. KML/KMZ fidelity or interoperability? Add [KML/KMZ Engineering](../skills/kml-kmz-engineering/SKILL.md) and [preserve original KMZ styling](kml-kmz-symbol-preservation.md). See [general GIS Icon Manager with specialized new-KMZ exporter](unified-gis-icon-manager.md); icon packs apply only to **new or explicitly restyled** placemarks, never to imported source styles.
13. **Always include [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md)**: preflight trust boundaries and permissions; use the offline security audit and release evidence. Risk-gated extra scanners are optional but must be reported as not run when absent. Continue using Performance (when scale matters) and Evidence Contract.

**Named geographic labels / World Gazetteer?** Use [gazetteer reference and LOD rendering](world-gazetteer-reference-layer.md) plus API integration for search and GIS Desktop/Web Mapping for the map UI. A locator search is not a viewport label overlay; default to open offline GeoNames dumps, not a licensed Esri locator.

## Cross-platform requests

For an app that needs ArcGIS Toolbox interoperability, a PostGIS backend and a Cesium globe, route to **Desktop + ArcGIS + Spatial Databases + Web Mapping + CRS**; integrate one end-to-end vertical slice before adding more capabilities. Document which host tests are available. Load references progressively, never every specialist automatically.

## Response and test contract

Declare target, version, schema/CRS, expected outputs, source modifications and risk, then implementation/tests. Use [GIS Acceptance Report](../templates/GIS_ACCEPTANCE_REPORT.md), [quality tiers](../docs/QUALITY_BENCHMARK.md) and [evidence validation](../scripts/validate_evidence.py) before calling a feature completed.

## GIS software command, data and UX QA routing (optional)

If a GIS user requests a visual reorganization, usability, keyboard or accessible language improvement, route to [UI/UX Design & Testing](../skills/gis-ui-ux-design-testing/SKILL.md) plus any required i18n/Cartography specialist. If the user reports inert ribbon/buttons/icons or mismatched handlers, add [UI Command Audit](../skills/gis-ui-command-audit/SKILL.md). If layers disappear, CRS is wrong or save/export drops data, add [GIS Data Flow Diagnostics](../skills/gis-data-flow-diagnostics/SKILL.md), CRS/KML and a real provider readback. If success must be observed in the actual Windows application including WebView2, add [Desktop E2E Testing](../skills/gis-desktop-e2e-testing/SKILL.md).

The [quality coordination contract](gis-ui-quality-coordination.md) owns the common IDs, statuses and handoffs. Never route all four merely because a UI exists; call only specialists required for observable user goals, preserve the original approved scope and native tests as NOT RUN when the host is missing.

## Shared non-GIS system integrations

Device time, permission-controlled GPS, OS files, notifications, serial/BLE, cameras, microphones, screen capture, hardware diagnostics and external adapters: route [System Capabilities & Permissions](../skills/system-capabilities-and-permissions/SKILL.md) + target platform + Security Guardian. Add Device Context for GIS location/time/solar only. Do not load every adapter or claim working sensor/permission from documentation.

## Visual assets across project types

For thumbnails, source/brand logos, illustrations, photographs, covers, backgrounds or screenshots, select [Visual Asset Discovery & Integration](../skills/visual-asset-discovery-integration/SKILL.md) through the main router and add only the needed UI/UX, platform or scientific data owner. Use official sources and wider internet search through actual host tools; consult the [workflow](visual-asset-workflow.md) and [source/skill guide](visual-asset-sources.md). A terrain selector is one example, not a domain restriction. A genuine command icon remains with its existing UI/cartographic owner.

## Conditional architecture and workflow visualization

The [GIS Architecture & Workflow Visualization specialist](../skills/gis-architecture-visualization/SKILL.md) is shared support for explicit diagram explanations, reviewed multi-component feature designs, cross-component diagnostics and structural changes. The GIS router decides whether it helps; the domain specialist retains implementation/testing ownership. Follow the [Archify integration handoff](archify-integration.md), declare proposed/source-reviewed/runtime-observed evidence, and record actual renderer/browser availability. Skip simple isolated edits and honor text-only requests. Editable JSON and interactive HTML are explanatory artifacts, not execution permissions or native GIS test evidence.
