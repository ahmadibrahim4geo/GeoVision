---
name: gis-development
description: Develop, debug, audit, test or migrate GIS software using ArcGIS Pro/ArcPy, legacy ArcMap, QGIS/PyQGIS, GDAL, GeoRust, RustGIS, GeoLibre R, MapLibre and CesiumJS. Use for GIS Python toolboxes, QGIS plugins, geospatial desktop/web/3D apps, remote sensing, GeoAI and free/public geospatial data APIs. Verify version compatibility, CRS, geometry, source licenses, service quotas, real command wiring and tests before calling a feature complete.
license: MIT
---

# GIS Development — Agent Skill

**Project:** [GIS Development Skills](https://github.com/ahmadibrahim4geo/GIS-Development-Skills) · [Interactive website source](../index.html) · [Project catalog](../references/project-catalog.json) · [GIS API directory](../references/free-geospatial-apis.md)

**Purpose:** Make software-building agents choose a compatible geospatial stack, write working tools and verify spatial correctness. This is a development guide and reference skill, not an installed GIS engine, free-data entitlement, or a guarantee that third-party libraries will work unchanged.

Use this skill when designing or implementing a GIS **tool, add-in, plugin, desktop app, raster/vector processing pipeline, web map, or 3D globe**. This skill supplies decision and QA rules. It is **not** an installable substitute for ArcPy, QGIS, GDAL or vendor SDKs.

## Procedure: do this in order

1. **Identify the deliverable and deployment target.** Exact product + version (ArcMap 10.x, ArcGIS Pro major version, QGIS version, or standalone); OS; programming language; data formats; offline needs; license constraints; intended user interaction; test environment. If a crucial value is unknown, state an assumption, mark it unverified, and use the conservative route.
2. **Choose the right pathway.** Read one platform reference and `../../references/geospatial-foundations.md` for file and coordinate considerations. Use `../../references/project-catalog.json` to find *verified links*, not to uncritically import projects.
3. **For hosted GIS, elevation, imagery, boundaries, population, hazards, routing, terrain or climate data, select a legitimate service.** Consult `../geospatial-data-discovery/SKILL.md`, then `../../references/free-geospatial-apis.md` and the offline JSON catalog (39 services); if coding a remote client use `../geospatial-api-integration/SKILL.md`. Distinguish no-key, personal-key, free-noncommercial, limited public, restricted and temporarily unavailable services. Validate live terms before proposing a runtime integration.
4. **Write a minimal architecture and acceptance contract.** Input schema, geometry type, expected CRS and units, optional elevation/Z/M, invalid/empty geometry behavior, nodata/masked raster behavior, output format/fields/metadata, working directory, dependencies and licensing.
5. **Protect portability.** ArcMap and ArcGIS Pro have separate runtime and API assumptions. Isolate GIS analysis from UI/ribbon wrappers; choose an engine only if allowed in that environment.
6. **Implement a vertical slice.** Start with an example from `../../templates/` if appropriate. Make a *functional end-to-end action* before adding extra commands; no placeholder commands presented as implemented.
7. **Verify source and licenses.** Re-check upstream APIs against the exact pinned release, its license and deployment constraints. Avoid unverified code recipes, private tokens or unsupported imagery endpoints.
8. **Test geospatial correctness.** Load known examples; verify geometry count and types, area/distance units, datum transformations, invalid features, NoData, output location, field names, and repeatability. Test user interface commands as actually wired, not just displayed.
9. **Deliver an auditable report.** Changed files, target version, what runs, what was tested (and where), what remains unverified, permissions/licenses, and exact next steps.

## Platform selector

| Target | Read | Typical API | Forbidden shortcut |
|---|---|---|---|
| ArcMap / ArcCatalog 10.x | `../../references/arcmap-legacy.md` | ArcPy + Python 2.7 in legacy installs | Claim Python 3 features work or use `arcpy.mp` |
| ArcGIS Pro | `../../references/arcgis-pro.md` | ArcPy / `.pyt` / Python 3 | Pretend `arcpy.mapping` equals `arcpy.mp` |
| QGIS | `../../references/qgis.md` | PyQGIS / Processing algorithms | Assume every version shares one Qt/PyQGIS API |
| Desktop / 2D / 3D | `../../references/desktop-and-web.md` | Tauri/Rust, Qt, MapLibre, CesiumJS | Treat rendering engine as geoprocessing engine |
| Independent GIS Desktop executable / toolbox migration | `../gis-desktop-application-development/SKILL.md`, `../../references/desktop-application-engineering.md` | PySide6/Qt, Tauri/Rust, WPF + independent GIS I/O | Mistake a shell UI or an ArcPy-hosted toolbox for a tested standalone installer |
| Remote sensing / AI | `../../references/remote-sensing.md`, `../../references/geoai-satellite-map.md` | GDAL, STAC, raster processing, model inference | Ignore radiometry, alignment, licensing and reproducibility |
| .NET GIS desktop / Pro SDK | `../../references/dotnet-gis.md` | WPF, WebView2, NetTopologySuite, ArcGIS Pro SDK | Assume .NET GIS components or ArcGIS SDK are free of platform and license constraints |
| R-based mapping and Rust GIS architecture | `../../references/rustgis-and-geolibre-r.md` | GeoLibre R, RustGIS | Assume a client rendering library grants permissions for its underlying web data |
| Hosted GIS services / APIs | `../../references/free-geospatial-apis.md` | Open-Meteo, NASA POWER, STAC, OpenFreeMap, DEM providers | Treat any public URL as unlimited, keyless, commercial or offline-safe |
| Spatial libraries | `../../references/geospatial-foundations.md` | GDAL, PROJ, GEOS, GeoRust | Do metric buffers on longitude/latitude degrees |

## Public API safety rules

- Read the exact provider access/limit/attribution entry and current official terms before integration.
- Do not generate generic integrations that use the **public Nominatim endpoint**; provider policy explicitly restricts use by code-generating platforms. Use self-hosted or separately authorized geocoders.
- Avoid embedding personal OpenTopography keys, scraping/bulk public tile services, or relying on paused SoilGrids REST.
- Check reproducible datasets and elevation vertical datum before presenting numerical results.
- For GEBCO_2026 land and ocean terrain, use official numeric CEDA OPeNDAP/NetCDF or official raster downloads for analysis; WMS rendered images are visualization only. Store the companion TID source type and explicitly forbid navigation/safety-at-sea uses.
- Distinguish observed (OpenAQ, NOAA stations), modeled (Open-Meteo/GloFAS/WorldPop), curated events (EONET), metadata search (CMR/STAC), and legal administrative boundary editions (geoBoundaries).
- Use current HeiGIT host/v2 APIs, not deprecated ohsome v1 or routing host; migrate USGS WaterServices by its early-2027 retirement.

## GeoLibre-specific development routing

When the target is a GeoLibre map, `.geolibre.json` project, plugin, or hosted/desktop GeoLibre deployment, consult `../../references/geolibre-integration.md` and the official upstream `https://github.com/opengeos/GeoLibre/tree/main/skills/geolibre`. Use the official GeoLibre skill with its MCP/Python authoring paths instead of guessing an undocumented file schema. For plugins use the official template, `plugin.json`, declared compatible rendering engines and real host integration tests. For desktop architecture reuse **patterns** (renderer-neutral state, command capability checks, data-source licensing) without treating external code or imagery as automatically reusable. Do not claim GeoLibre code runs as an ArcMap, ArcGIS Pro or QGIS plugin. Verify portability, CRS, credentials, live-vs-file workflow and provider terms.

## Esri Projection Engine, SpatialReference.org and reusable Esri projects

For CRS selection, projection/coordinate-system UI, WKID/latestWKID compatibility, datum transformation, WKT/.prj, axis order, 3D Z or local/vertical reference systems **always route first** to `../coordinate-reference-systems/SKILL.md` and `../../references/esri-crs-and-spatialreference.md`.

Use ArcGIS Pro's *exact official top-level names* **Favorites, Layers, Geographic Coordinate Systems, Projected Coordinate Systems, Vertical Coordinate Systems, Custom**, split into Horizontal and Vertical tabs. Never claim `Favorites` or `Layers` are Esri factory database files; they represent user state and active map layers. Esri `projection-engine-db-doc` metadata supplies CRS names/WKIDs/extents/WKT/transformations, but **not** a complete machine-readable export of the ArcGIS Pro geographic folder hierarchy. Require verified provenance for every region/country folder mapping.

The portable, optional-source, standard-library Esri catalog inspector is `../../scripts/crs_catalog.py`; it queries locally supplied PE CSV/JSON and user-scoped Favorites, but it does not transform coordinates. For transformations, choose an installed validated PROJ/pyproj/GDAL/ArcPy/QGIS engine and real datum grid availability; read SpatialReference.org for authoritative definitions and interchange.

For the other **14 reviewed Esri/PROJ complementary repositories**, read `../../references/esri-open-source-integration.md` before designing integration:
- ArcGIS Pro UI commands: `Esri/arcgis-pro-sdk-community-samples`
- Python portal workflows: `Esri/arcgis-python-api` (already in catalog; not ArcPy)
- Raster/geometry/performance: `Esri/raster-functions`, `Esri/geometry-api-java`, `Esri/lerc`
- .NET desktop: `Esri/arcgis-maps-sdk-dotnet-samples`, `Esri/arcgis-maps-sdk-dotnet-toolkit`
- Web services: `Esri/esri-leaflet`, `Esri/maplibre-arcgis`, `Esri/arcgis-rest-js`
- Cartography/3D: `Esri/cim-spec`, `Esri/i3s-spec`
- Discovery/CRS UX: `Esri/awesome-arcgis-developers`, `OSGeo/PROJ-CRS-Explorer`.

Catalog entries and source examples are **not installed dependencies, proprietary SDK entitlements or evidence of host-level tests**. Verify upstream repository licenses and Esri hosted service terms separately before borrowing code or shipping binaries.

## Specialist routing from Qiusheng Wu's GIS software ecosystem

For tasks matching https://gishub.org/software/, consult `../../references/gishub-software.md` first, then the verified GitHub repository entry in `../../references/project-catalog.json`.

- **Data discovery:** `../geospatial-data-discovery/SKILL.md` for STAC, NASA, imagery, climate/DEM and legitimate API access.
- **QGIS plugins:** `../qgis-plugin-development/SKILL.md` and `opengeos/qgis-plugin-template` for PyQGIS, Qt and functional toolbar/Processing acceptance.
- **Web GIS and standalone maps:** `../geospatial-web-mapping/SKILL.md` for MapLibre component integration, layer state, geometry editing, time sliders and actual data availability.
- **ArcGIS/Whitebox:** the older `opengeos/WhiteboxTools-ArcGIS` documentation specifies ArcGIS Pro and ArcGIS **10.6+**, not ArcGIS 10.5 and earlier; verify Python runtime for the exact target.
- **GeoAI:** `opengeos/GeoAgent` is an orchestration framework; agents must call verified spatial tools and require confirmations before destructive operations.
- **Catalogs:** `geospatial-data-catalogs`, `NASA-CMR-STAC` and `stac-index-catalogs` list candidate data endpoints, not guaranteed working/free APIs.

Public source availability is **not** runtime integration testing. Verify current software releases, licenses, provider authentication, coordinate systems, scientific provenance and end-to-end feature behavior; do not copy third-party code without rights.

## Real-world ArcGIS toolboxes and native QA

For building or evaluating ArcGIS Pro/ArcMap Python Toolboxes, prefer the specialized `../arcgis-toolbox-engineering/SKILL.md` and `../../references/ahmad-ibrahim-gis-case-studies.md`. Reference actual public implementations by Ahmad Ibrahim rather than invented demonstration code when appropriate:

- MIRSAD: climate and station tools, `mirsad_core` scientific and environment management, separate development-only MIRSAD Desktop.
- CreateFlowArrow: geodesy/slope, pipe digitizing and topology; **do not execute optional in-place auto-flip without explicit consent and safe backup**.
- GeologicalCrossSectionToolbox: DEM elevation profiles, geological intersections, DXF/HTML/CSV exports and fixture-based checks.
- Also inspect ArcMap Style Collection (verify asset licenses) and the GIS Data Repositories data discovery catalog.

Distinguish **T0 source review**, **T1 unit/mock ArcPy test**, **T2 licensed native toolbox load**, **T3 real geoprocessing execution and output validation**, and **T4 packaged standalone deployment**. A GitHub Actions check cannot establish T3 unless it really ran the target licensed GIS runtime with fixture readback. Report exact tested versions and known gaps.

## Required pre-delivery check

- [ ] Specify exact target runtime and versions; prove compatibility or clearly label assumptions.
- [ ] Validate input schema, field sizes (including 10-character Shapefile limits), output field aliases, Z/M and date/time behavior.
- [ ] Confirm CRS, transformation pipeline, axis order, distance/area units and rasters' pixel alignment.
- [ ] Make output deterministic, avoid overwriting raw inputs; use temporary files safely.
- [ ] Report data provenance, provider terms, secrets handling, and third-party licenses.
- [ ] Run static tests locally and application-level integration checks where the required app exists.
- [ ] For GUI: click-test each advertised ribbon/button/panel action; never count visual placement as functionality.
- [ ] Document incomplete tests, required installations, and known limitations.

## Catalog behavior

Open `../../references/project-catalog.json` for tools matching the exact need. For exact EarthMapper source review, read `../../references/earthmapper-case-studies.md`. For public services, separately consult `../../references/free-geospatial-apis.json`, and for R/Rust review `../../references/rustgis-and-geolibre-r.md`. The catalog includes **multiple EarthMapper repositories with different meanings**. Match the user's intended project by owner and purpose; do not assume one name maps to one software system. `GeoRust` is a Rust library ecosystem; `GeoLibre` is a geospatial app platform; `GeoLibra` is a distinct GitHub user and must not be conflated.

When new sources are needed, consult their official repositories/docs, verify the exact URL, and propose a catalog pull request with the required metadata. Source links are references and do not imply endorsed or guaranteed functionality. Distinguish **README-only product showcases**, **research papers with no released code**, **archived source repositories** and **actually inspectable source libraries** before suggesting code reuse.

## Example user requests

- "Create a Python toolbox for ArcGIS Pro that clips raster outputs to a study boundary."
- "Port this ArcMap geoprocessing script to ArcGIS Pro without breaking its parameter dialog."
- "Build a QGIS Processing plugin to create spatial buffers using a projected working CRS."
- "Select an open 2D+3D stack for a Windows GIS desktop viewer, including KML and GeoPackage."
- "Audit coordinate systems and identify why output area measurements are wrong."
## Additive specialized routes for production GIS

Use [Spatial Databases](../skills/spatial-databases/SKILL.md) for PostGIS/SpatiaLite index design, SQL and QA; use [Geospatial Data Processing](../skills/geospatial-data-processing/SKILL.md) for independent raster/vector pipelines, deterministic geometry tests and CRS checks; use [Remote Sensing Pipelines](../skills/remote-sensing-pipelines/SKILL.md) for STAC/COG, Earth Engine and QA-driven pixel processing. Start at the [examples index](../examples/README.md) and consult the [production workflow reference](../references/production-workflows.md) and [GIS Quality Benchmark](../docs/QUALITY_BENCHMARK.md). Keep all prior skills; a generic CI test cannot establish native GIS T3 readiness.
