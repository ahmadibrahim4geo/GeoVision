# Standalone desktop GIS: architecture, implementation and acceptance

This guide is an independently authored implementation framework. Consult [GeoLibre](https://github.com/opengeos/GeoLibre), [RustGIS](https://github.com/opengeos/RustGIS) and [MIRSAD Desktop](https://github.com/ahmadibrahim4geo/MIRSAD/tree/main/MIRSAD%20Desktop) for different design approaches, **not** as interchangeable engines or automatically redistributable code.

## Architecture boundaries

| Component | Responsibility | Contract |
| --- | --- | --- |
| UI shell (PySide6 / WPF / Tauri) | Ribbon/menu, dock panels, dialogs, language/theme and accessibility | Every visible command has a checked action or an explicit disabled explanation |
| Project model | Save/reload datasets, order, visibility, style, view and source provenance | Serializable and versioned; corruption/old versions handled safely |
| 2D/3D renderers | Show raster/vector/terrain data with camera and selection state | Capability-check each engine; do not confuse imagery tiles with an analysis backend |
| Analysis engine | Climate, hydrology, geometry, statistics or scientific algorithms | Deterministic, headless and independent of window events |
| Data adapters | Read/write GeoPackage, GeoJSON, GeoTIFF/COG, tables and other verified formats | CRS/datum, encoding, NoData, format fidelity and license checks |
| Web providers | Weather, elevations, satellite, basemaps, geocoding | Provider key/rights/quotas, provenance, timeout, authentication and retry policy |
| Workers | Long jobs, progress, cancellation, cache and errors | No blocked UI thread; partial work cleanup and recovery |
| Packaging | Installer, runtime files, license notices, updates | Clean-build test, complete dependencies and user-file preservation |

## A vertical slice that must work before UI expansion

1. Create/open a project from a user-selected path, without hardcoded developer locations.
2. Load a sample GeoPackage vector with known projected CRS and a small raster/DEM with known NoData.
3. Render both; zoom to extent, toggle layer, style feature, inspect attributes and test a second language.
4. Run one meaningful spatial tool; assert numerical output and persist a new layer using a valid CRS.
5. Save project, exit, relaunch installed build, reopen and confirm layer order, symbology, view and underlying source access.
6. Export meaningful CSV/GeoPackage/GeoTIFF where supported and read back output with an independent implementation.
7. Unplug network: verify local datasets work, provider failures are visible and never converted to fabricated defaults.

## ArcPy toolbox vs Desktop application

- **Toolbox:** hosted by ArcMap/ArcGIS Pro, depends on licensed ArcPy and host map/ArcGIS UI. Cross-version Python 2.7 / 3 requires careful isolation.
- **Standalone Desktop:** must use a legal standalone engine and bundle its *own* dependencies; do not depend on a local licensed ArcGIS installation unless the entire application expressly requires it.
- **Migration:** keep a pure scientific core; wrap GIS I/O and UI with adapters. Record differences in raster resampling, transformation pipeline, Z units, NoData, masks, output identifiers and metadata.
- **Acceptance:** numerical match on controlled fixtures with tolerances and sample output parity is required; visual resemblance is not parity.

## Critical performance and reliability tests

- Large raster memory estimates and cache eviction rather than loading all pixels at once.
- Cancel a processing job while it is writing output: no corrupted registered layer.
- Switch 2D/3D renderer repeatedly: saved model remains consistent, unsupported controls are disabled.
- Arabic paths, RTL labels, decimal formatting, missing fonts, Windows DPI 100/150/200% and high contrast mode.
- External source 401/403/429/500 errors, short network outage, invalid tile URL, missing datum grid, expired key.
- Confirm installation on clean x64 Windows and user-file preservation on update/removal.

## Required release report fields

Application version and commit SHA; tested Windows build, Python/.NET/Rust version, dependencies and exact licenses; test fixture/expected values; headless test count and UI click matrix; clean install/export/readback outcomes; provider/offline tests; known gaps; release decision (**development candidate** vs **approved stable**).

See also `references/ahmad-ibrahim-gis-case-studies.md` for example projects, and `skills/arcgis-toolbox-engineering/SKILL.md` for ArcGIS-hosted toolboxes.


## Detailed implementation playbook

### 1. Requirements and acceptance inventory

Create a requirements table before coding with columns: ID, user journey, input fixture, expected visible result, persisted state, failure behavior, security risk, target OS, responsible component, and T0–T4 verification evidence. Record *must-have* vs optional. Every visible control must map to a tested command; incomplete features remain disabled with an explanation, not fake success.

Use the following minimum user journeys: new/open project; import vector/raster; layer reorder/visibility/style; select/identify/measure; CRS transform; one deterministic analysis; export/readback; save/reopen project; offline provider failure; cancellation; crash recovery; installed executable launch. Add 3D and KML/KMZ fidelity only when requested.

### 2. Component boundaries and contracts

- **Application shell:** menu/ribbon, docks, dialogs, keyboard commands, localization and accessibility; no scientific calculations in UI event handlers.
- **Project service:** versioned schema, layer source URI (relative when possible), CRS, order, visibility, symbology, map camera, annotations, application settings and migrations. Never serialize secrets.
- **Data adapter:** `probe/open/read/write/close` plus declared format capabilities. Expose structured errors for invalid geometry, unsupported driver, encoding, NoData and CRS mismatch.
- **Processing service:** validated input schema, immutable job definition, worker isolation, progress/cancel token, atomic output commit, result provenance and deterministic fixture tests.
- **Renderer adapter:** `addLayer/removeLayer/setVisibility/setStyle/zoomTo/select/getView/setView`; declare 2D vs 3D, vector/raster/terrain and styling support rather than silently dropping unsupported features.
- **Provider adapter:** endpoint, license, credentials via OS secrets, attribution, rate limit, cache terms, timeout, retry and offline status.
- **Installer/updater:** pinned runtime files, checksums, third-party notices, user-data retention, rollback and uninstall tests.

Document input/output schemas and failure semantics for each boundary. Prefer file/IPC integration for incompatible native runtimes; do not share unmanaged QGIS/Qt pointers across arbitrary UI frameworks.

### 3. Engine choice and integration recipes

| Target application | Starting architecture | Mandatory cautions |
| --- | --- | --- |
| Full GIS workstation | Qt/C++ or version-matched PyQGIS with QGIS Core/GUI/Processing | QGIS GPL, Qt ABI, platform plugins, provider paths, native launch |
| Lightweight 2D GIS viewer | Desktop shell + MapLibre renderer + GDAL/PROJ/GEOS service | MapLibre does not supply analysis; validate vector/raster formats and map source rights |
| 3D terrain/globe explorer | Desktop shell + CesiumJS + independent geospatial data/processing worker | Renderer is not a GIS engine; terrain/imagery licenses, GPU/WebView and offline cache |
| Batch scientific desktop tool | PySide6 or suitable shell + isolated GDAL/PROJ/GEOS/GRASS jobs | Deterministic outputs, progress, cancellation, dependency bundling |
| Rust-first GIS application | Rust shell (egui/Tauri where justified) + verified Rust GIS crates or FFI/worker adapters | Audit crate maturity and unsafe FFI; do not assume RustGIS is a stable SDK |
| Local multi-layer editing | Qt/QGIS or another verified editor stack + GeoPackage/SpatiaLite | Transactions, geometry constraints, undo/redo, concurrent locks and schema migration |

An architecture selection must explain why each dependency is required, its release/version, OS support, license and a runnable proof of integration. Do not present example architectures as shipped products.

### 4. File I/O acceptance matrix

| Format | Open checks | Export/readback checks |
| --- | --- | --- |
| GeoPackage | Multiple layers, geometry type, CRS, UTF-8 names, spatial indexes | Row count, fields, geometry equality/tolerance, transaction rollback |
| Shapefile | Sidecars, DBF encoding, 10-character field limits, CRS file | Field truncation warnings, geometry and attributes preserved where representable |
| GeoJSON | RFC 7946 coordinates, properties, geometry collections | Axis order, precision, encoding, feature count |
| GeoTIFF / COG | Geotransform, bands, nodata, mask, overview, pixel type | Resolution, bounds, NoData, resampling choice, pixel statistics |
| KML / KMZ | Folder tree, styles, icon assets, altitude, time, gx extensions | Preserve supported semantics; explicitly report unsupported extensions |
| LAS / LAZ (optional) | CRS/VLR, point format, scale/offset, classification | Point count, attributes, bounds and lossy conversion warnings |

Maintain a per-format **capability ledger** with `read`, `write`, `style fidelity`, `3D`, `streaming`, and verified driver/runtime version. Never advertise universal import/export without actual fixtures.

### 5. CRS, geometry and numeric correctness

For every layer: detect source CRS; display missing/ambiguous CRS explicitly; require explicit assignment versus transformation; record source/target WKT or authority identifiers, coordinate epoch when applicable, datum-grid availability, axis ordering, geographic vs projected units and vertical reference. Validate transformation with known control points and tolerances. Measure areas/distances with documented planar/geodesic methods. For raster analysis, preserve affine transform, alignment, pixel type, NoData and units; never silently change vertical meters to feet or angular degrees to meters.

### 6. UI, performance and accessibility

- Test mouse pan/zoom, selection, extent history, layer drag/drop, undo/redo, disabled actions, error dialogs and keyboard navigation.
- Run raster tiling/overview generation, network calls and processing jobs off the UI thread. Bound memory, CPU concurrency, cache size and temporary disk use; expose cancellation and cleanup.
- Support Arabic/English translation dictionaries, RTL where applicable, mixed-direction coordinate strings, font fallback, DPI scaling, focus order, screen readers and high contrast.
- Test 2D/3D transitions for preserved project state and consistent layer visibility; unsupported renderer features must produce a clear message.
- Provide reproducible benchmarks: fixture dimensions, machine specs, warm/cold cache, peak RAM, startup time, render latency, job duration and error rate. Do not invent performance targets or measurements.

### 7. Security and supply-chain controls

Treat KML/KMZ archives, shapefile sidecars, raster metadata, style URLs, plugins, local project files and remote endpoints as untrusted. Apply the shared Security Guardian; additionally enforce archive traversal/size limits, path canonicalization, output directory permissions, process execution allowlists, restricted plugin permissions, network consent, TLS, protected credentials and signed/checksummed updates. Generate an SBOM and attribution/license notice list for distributed binaries; scan dependencies and document findings. A library's permissive license does not grant basemap, icon, font or dataset redistribution rights.

### 8. Fork and adaptation decision

Before reusing an upstream GIS desktop app: establish repository ownership and exact revision; inspect license and third-party assets, maintenance history, CI, tests, OS build instructions, architecture, extensibility and trademark restrictions. Classify as **fork full application**, **link a reusable library**, **separate worker/service**, or **reference-only**. For each route document build proof, source modifications, retained notices, upgrade strategy, dependency isolation and test coverage. Unknown licensing or broken upstream build blocks source reuse, not independent clean-room learning.

### 9. Test ladder and release gate

- **T0 static:** valid manifest, links, dependency graph, source inspection, license matrix, no placeholders or dead command handlers.
- **T1 headless:** fixtures for vector/raster/CRS/geometry, corrupted inputs, export/readback, serialization/migrations, worker cancellation and security guards.
- **T2 GIS native:** real installed QGIS/GDAL/PROJ/GEOS drivers, representative operations, environment and ABI logs.
- **T3 GUI native:** automated/manual click matrix on the actual desktop OS, UI responsiveness, dialogs, Arabic/English, 2D/3D and state persistence.
- **T4 packaged:** clean VM install, launch, offline operations, open/save/export/reopen, upgrade/rollback, uninstall, license notices and user-data preservation.

For each test record command, host OS, version, fixture, expected result, actual result, logs/screenshots, pass/fail/not-run and issue link. A release is blocked by confirmed critical security faults, destructive data loss, failed core journeys or missing legally required notices. Unknown T2–T4 evidence means **development candidate**, never verified stable.

### 10. Delivery checklist

Deliver: architecture decision record; engine/license compatibility matrix; source/lockfiles; format capability ledger; CRS policy; API/IPC contracts; runnable vertical slice; reproducible build instructions; fixture inventory; T0–T4 acceptance report; installer manifest; third-party notices; changelog; limitations and follow-up issues. Cross-reference the existing specialized skills rather than duplicating their full algorithms.
