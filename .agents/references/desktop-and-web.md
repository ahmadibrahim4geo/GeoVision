# Standalone GIS, mapping and globe applications

A GIS application needs more than a map view: data loading, CRS handling, layer state, style, selection, query, analysis, export, error recovery, tests, packaging and licensing.

## Commercial Global Mapper SDK vs open-source alternatives

The [Blue Marble reference](blue-marble-global-mapper.md) distinguishes licensed Global Mapper SDK/DLL use from independently licensed Cesium Terrain Builder, 3D Tiles Tools, openMVG/OpenMVS, CGAL and model I/O forks. They are **not** a free implementation of Global Mapper or proof of engine source access.

## Design study: Earth View's imagery globe

The [Earth View review](earth-view-architecture.md) describes a NASA GIBS 3D globe, Copernicus Sentinel regional imagery/time-lapses, React/Three.js state and Electron packaging. It is a **source-architecture reference only** because no upstream LICENSE was observed. Its renderer and OS shell are not drop-in equivalents for CesiumJS/WPF/WebView2.

## Google Earth Pro behavioral reference, not a source engine

The independent [Google Earth Pro pattern study](google-earth-pro-engineering.md) consolidates [Google Earth Blog](https://www.gearthblog.com/) observations on Places bulk selection, elevation profiles, tours, overlays and region-based network content. The [Historical Imagery Timeline contract](historical-imagery-timeline.md) specifies a non-blocking date control and truthful scene acquisition/provenance, with explicit supplier licenses. Neither the blog nor Google Earth footage is a freely embeddable API for standalone ZEarth. Treat live integrations and pixel-perfect UX as unverified until runtime acceptance.

## Stack options

| Need | Relevant project | Important boundary |
|---|---|---|
| Cross-platform desktop shell with Rust backend | Tauri + Rust Geo ecosystem, using GeoLibre as reference | Rendering, backend analysis and offline files are separate layers |
| Full-featured existing GIS core | QGIS / PyQGIS | QGIS bindings and redistribution carry license/deployment obligations |
| 2D browser map canvas | MapLibre GL JS | Requires verified style/tiles provider or offline data |
| 3D global viewer | CesiumJS | Imagery/terrain service may have separate terms/keys |
| Browser/desktop multi-engine inspiration | GeoLibre | Do not claim feature parity without testing the referenced release |

Official sources: https://github.com/opengeos/GeoLibre ; https://github.com/maplibre/maplibre-gl-js ; https://github.com/CesiumGS/cesium

**Detailed implementation and release checklist:** [desktop-application-engineering.md](desktop-application-engineering.md). For an independent Windows executable, activate [gis-desktop-application-development](../skills/gis-desktop-application-development/SKILL.md) rather than treating a web-map renderer as a complete app. Real installer and UI acceptance are separate from static code tests.

## Workflow for a desktop app

1. Document explicit functional acceptance criteria: open GeoPackage/GeoTIFF/Shapefile/KML; CRS; lat/lon status; zoom/pan/rotate; project save/open; large layers.
2. Separate **renderer** from **data services**, **analysis engine**, **UI command bus**, and **project persistence**.
3. For each ribbon command, name handler, input data, output change, observable success, failure behavior and test case.
4. Verify real basemap terms and offline access (availability and caching are provider-specific). No automatically assumed access to proprietary satellite imagery.
5. Instrument file imports and timing; never show dead commands that simply log or display a placeholder success message.
6. Test export, project roundtrips, coordinate display, layer order, cache cleanup and recovery from corrupt project files.

## Provider caveat

A rendering SDK's open-source license does **not** make its imagery, style, tile server, terrain tiles or geocoding endpoint free or redistributable. Verify each upstream service independently.

## Windows .NET GIS and the EarthMapper examples

Read [`dotnet-gis.md`](dotnet-gis.md) and [`earthmapper-case-studies.md`](earthmapper-case-studies.md). **AliAlkazraji/EarthMapper** is a README-only Windows GIS product overview, not a reusable code repository in the inspected state. **HIT-SIRS/EarthMapper** is separate satellite/map generation research.

## GeoLibre R / RustGIS and real API service requirements

Read [`rustgis-and-geolibre-r.md`](rustgis-and-geolibre-r.md) for inspectable RustGIS and R GeoLibre integration examples. For climate data, DEM, imagery, tiles and geocoding use [`free-geospatial-apis.md`](free-geospatial-apis.md). Never assume a free/open renderer guarantees rights to a hosted map tile or imagery service.