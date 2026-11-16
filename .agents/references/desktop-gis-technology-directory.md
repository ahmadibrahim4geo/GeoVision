# Desktop GIS Technology Directory

A curated, GIS-first entrypoint for selecting real engines, renderers, UI shells, data services, and authoritative documentation. **This is a source directory, not proof that any component has been built or integrated.** Confirm the exact upstream release, supported platforms, dependency versions, redistribution terms, and test evidence before adopting.

## Quick choice

| Need | Start with | Next step |
| --- | --- | --- |
| Full desktop GIS editor and processing | QGIS Core / GUI / Processing | [QGIS integration skill](../skills/qgis-engine-integration/SKILL.md) |
| Read/write raster and vector | GDAL/OGR | [Data processing skill](../skills/geospatial-data-processing/SKILL.md) |
| Coordinate reference systems | PROJ | [CRS skill](../skills/coordinate-reference-systems/SKILL.md) |
| Spatial geometry operations | GEOS | [Data processing skill](../skills/geospatial-data-processing/SKILL.md) |
| Interactive 2D map | MapLibre | [Web mapping skill](../skills/geospatial-web-mapping/SKILL.md) |
| 3D globe and terrain | CesiumJS | [Web mapping skill](../skills/geospatial-web-mapping/SKILL.md) |
| Advanced geoprocessing | GRASS GIS / SAGA GIS / WhiteboxTools | [Terrain specialist](../skills/terrain-hydrology-lidar/SKILL.md) |
| Spatial persistence | PostGIS / SpatiaLite | [Spatial databases skill](../skills/spatial-databases/SKILL.md) |
| Point cloud / LiDAR | PDAL | [Terrain specialist](../skills/terrain-hydrology-lidar/SKILL.md) |
| Rust-native GIS application reference | RustGIS | Evaluate upstream architecture and tests; not a drop-in engine |

## Authoritative engine and library sources

| Priority | Project | Upstream source | Official docs / site | Role |
| --- | --- | --- | --- | --- |
| 1 | QGIS | https://github.com/qgis/QGIS | https://docs.qgis.org/ | Desktop GIS platform and reusable native GIS components |
| 2 | GDAL/OGR | https://github.com/OSGeo/gdal | https://gdal.org/ | Raster/vector I/O and drivers |
| 3 | PROJ | https://github.com/OSGeo/PROJ | https://proj.org/ | CRS and datum transformations |
| 4 | GEOS | https://github.com/libgeos/geos | https://libgeos.org/ | Geometry and topology |
| 5 | MapLibre GL JS | https://github.com/maplibre/maplibre-gl-js | https://maplibre.org/ | Web-based 2D rendering in desktop shells |
| 6 | CesiumJS | https://github.com/CesiumGS/cesium | https://cesium.com/learn/cesiumjs/ | 3D globe and terrain rendering |
| 7 | GRASS GIS | https://github.com/OSGeo/grass | https://grass.osgeo.org/ | Advanced GIS analysis |
| 8 | PostGIS | https://github.com/postgis/postgis | https://postgis.net/ | Spatial PostgreSQL |
| 8 | SpatiaLite | https://www.gaia-gis.it/fossil/libspatialite/index | https://www.gaia-gis.it/fossil/libspatialite/index | Embedded spatial SQLite |
| 9 | SAGA GIS | https://sourceforge.net/projects/saga-gis/ | https://saga-gis.sourceforge.io/ | Terrain and raster analysis |
| 10 | PDAL | https://github.com/PDAL/PDAL | https://pdal.io/ | Point-cloud pipelines |
| 11 | RustGIS | https://github.com/opengeos/RustGIS | https://rustgis.app/ | Rust GIS application architecture reference |
| 12 | WhiteboxTools | https://github.com/jblindsay/whitebox-tools | https://www.whiteboxgeo.com/ | Specialized terrain/hydrology; review current terms |

## Desktop UI shells and frameworks (not GIS engines)

| Framework | Official source | Choose when | Important boundary |
| --- | --- | --- | --- |
| Qt / Qt for Python (PySide6) | https://doc.qt.io/qtforpython-6/ | Native GIS desktop windows, docks, menus, internationalization | Check Qt/PySide licensing, Qt plugins and QGIS ABI |
| Tauri | https://tauri.app/ | Rust backend with web frontend | Needs independent GIS and rendering libraries |
| egui / eframe | https://github.com/emilk/egui | Rust-native immediate-mode GUI | Not a GIS analysis or renderer by itself |
| WPF / .NET | https://learn.microsoft.com/dotnet/desktop/wpf/ | Windows-first desktop GIS shells | GIS engines and WebView2 remain separate dependencies |
| WebView2 | https://learn.microsoft.com/microsoft-edge/webview2/ | Hosting MapLibre or CesiumJS in Windows app | Validate installed runtime, bridge permissions and offline behavior |
| Electron | https://www.electronjs.org/ | Web-first cross-platform shell when justified | Assess runtime size, updates, sandbox and memory costs |

Do not choose a UI framework until the target GIS workflows, supported operating systems, performance envelope and engine integration constraints are known.

## Recommended learning and implementation order

1. Read [the desktop specialist](../skills/gis-desktop-application-development/SKILL.md) to select the application route.
2. Use [the engineering playbook](desktop-application-engineering.md) for architecture, contracts, formats, UI, testing and packaging.
3. Choose one GIS stack from the quick-choice matrix, then consult the linked specialist and upstream documentation.
4. Build a real vertical slice: open GIS data, transform/analyze, render, save, reopen and read back.
5. Apply [Security Guardian](../skills/gis-security-guardian/SKILL.md), [shared CRS policy](../skills/coordinate-reference-systems/SKILL.md) and [acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md).
6. Only advertise native compatibility or release stability after actual T2–T4 tests on the intended environment.

## Selection record template

Record project type, OS/CPU, GIS functions, renderer, UI shell, file formats, offline/network requirements, required licenses, engine and framework versions, dependency ABI, alternatives considered, build and test evidence, and explicit unknowns.

**Avoid:** copying an upstream project without license review; treating a map renderer as a full GIS engine; adding every listed library by default; assuming documentation links prove runtime support; shipping buttons with no functioning handler.
