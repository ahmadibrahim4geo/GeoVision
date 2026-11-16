# Qiusheng Wu GIS software ecosystem — implementation guide

**Official software index:** https://gishub.org/software/  
**Author:** Dr. Qiusheng Wu (https://gishub.org/about/)  
**GitHub group:** https://github.com/opengeos  
**Reviewed:** 2026-10-24  
**Relationship:** Independent curation, not endorsed by Dr. Wu or Open Geospatial Solutions.

This page converts the official list into platform-specific engineering pathways. GIS Development Skills links upstream tools but does not bundle their source code, model weights, datasets, or hosted imagery. Every runtime, version and third-party license must be verified before adoption. The project catalog adds 40 nonduplicate verified repository URLs.

## Decision matrix

| Developer goal | Recommended starting sources | Critical validation |
| --- | --- | --- |
| ArcGIS Pro Python Toolbox | [WhiteboxTools-ArcGIS](https://github.com/opengeos/WhiteboxTools-ArcGIS), [Whitebox Next Gen](https://github.com/opengeos/Whitebox-Next-Gen-ArcGIS), ArcPy | Python/runtime compatibility, tool metadata, geoprocessor executable, licenses |
| ArcMap legacy toolbox | WhiteboxTools-ArcGIS only after ArcGIS 10.6+ and Python compatibility verification; otherwise legacy ArcPy | No unverified Python 3 syntax, pre-10.6 support is **not** claimed |
| QGIS plugin from scratch | [QGIS plugin template](https://github.com/opengeos/qgis-plugin-template) | Match QGIS version, PyQt/Qt5 or Qt6, metadata, packaging and GUI click tests |
| Search satellite scenes in QGIS | [QGIS STAC Explorer](https://github.com/opengeos/qgis-stac-plugin), [Copernicus](https://github.com/opengeos/qgis-cdse-plugin), [NASA OPERA](https://github.com/opengeos/qgis-nasa-opera-plugin) | STAC asset existence, credentials, CORS/network, cloud/date filters and georeferencing |
| Desktop GIS geoprocessing engine | [geolibre-rust](https://github.com/opengeos/geolibre-rust), [whitebox-wasm](https://github.com/opengeos/whitebox-wasm), GDAL/PROJ | WASM memory, supported CRS/formats, binary licensing, NoData and regression comparisons |
| Web GIS drawing, layers, and temporal visualization | [MapLibre editor](https://github.com/opengeos/maplibre-gl-geo-editor), [layer manager](https://github.com/opengeos/maplibre-gl-layer-manager), [time slider](https://github.com/opengeos/maplibre-gl-time-slider) | MapLibre major version, styles, event wiring, edit persistence, time index correctness |
| Multi-source satellite imagery in Python | [geemap](https://github.com/gee-community/geemap), [leafmap](https://github.com/opengeos/leafmap), [segment-geospatial](https://github.com/opengeos/segment-geospatial), [HyperCoast](https://github.com/opengeos/HyperCoast) | Earth Engine authentication, dataset coverage, licensing, imagery band metadata |
| Terrain, DEM and hydrology | [lidar](https://github.com/opengeos/lidar), [whitebox-python](https://github.com/opengeos/whitebox-python), [whiteboxgui](https://github.com/opengeos/whiteboxgui) | DEM vertical datum/units, sinks/NoData, hydrologic method, cells and resolution |
| Geospatial agents | [GeoAgent](https://github.com/opengeos/GeoAgent) | Tools registered with metadata, explicit confirmation for destructive/expensive actions, secret handling |
| Geospatial dataset discovery | [geospatial-data-catalogs](https://github.com/opengeos/geospatial-data-catalogs), [NASA CMR STAC](https://github.com/opengeos/NASA-CMR-STAC), [STAC indexes](https://github.com/opengeos/stac-index-catalogs) | Catalog entry is **not** a free API; verify live endpoint and redistribution rights |
| Publish GIS Python dashboards | [streamlit-geospatial](https://github.com/opengeos/streamlit-geospatial), [streamlit-map-template](https://github.com/opengeos/streamlit-map-template) | Hosting, secrets, maximum data size, responsiveness and export paths |

## Recommended workflow: source discovery

1. Define the scientific variable, area, period, resolution, license and target data type.
2. Query the **repository catalog** (`references/project-catalog.json`) for tools, and the **API catalog** (`references/free-geospatial-apis.json`) for service access restrictions.
3. Query machine-readable indexes from `geospatial-data-catalogs`, `NASA-CMR-STAC` or `stac-index-catalogs` as *discovery metadata only*.
4. Check the provider's current API documentation, authorization, rate limits, coverage and attribution.
5. Validate a sample item's geometry/CRS, time metadata, units, bands, NoData and source citation before writing the full downloader.
6. Make retries, timeouts, caching and offline errors explicit; do not hardcode credentials or assume free-for-commercial use.

## ArcGIS Pro and ArcMap: do not conflate versions

`WhiteboxTools-ArcGIS` explicitly says ArcGIS Pro and ArcGIS 10.6 or newer are supported; no promise for 10.5 or earlier. Older ArcMap 10.x environments typically use Python 2.x and old third-party dependencies. Never import a modern Python 3 package just because a repo's title says ArcGIS. Check the exact installed interpreter and toolbox code before proposing migration. Validate a processing tool with a tiny input raster, inspect georeferenced outputs, then run it inside the target app.

## QGIS plugin architecture

Use `qgis-plugin-template` as a *study template*, not as a drop-in universal binary. Its README describes QGIS 3.28+ and QGIS 4 compatibility. Verify the actual QGIS/Qt version, metadata, Processing algorithm declaration, dock panels, configuration, package ZIP, installation, unload, and failures. Prefer a genuine result layer and an observable user action over empty placeholder toolbar buttons.

### Remote-sensing QGIS plugins

- `qgis-stac-plugin`: STAC search, COG streaming, temporal raster comparison.
- `qgis-cdse-plugin`: Copernicus data workflows.
- `qgis-nasa-opera-plugin`: NASA OPERA imagery.
- `qgis-timelapse-plugin`: Earth Engine temporal animation (requires eligible authentication).
- `qgis-basemaps`: basemap discovery; provider terms govern tiles and caching.

## Web mapping and standalone GIS

The MapLibre ecosystem offers composable controls, but no single plugin guarantees an entire GIS: geometry editing, undo/redo, layer state, symbology, timeline, 2D/3D switching, and project persistence must be separately integrated and click-tested. See the official `maplibre-gl-plugin-template` for versioned packaging and `maplibre-gl-components` for common widgets.

`maplibre-gl-time-slider` documents a **breaking v1** migration; choose the latest documented API rather than old `labels[]` examples. Distinguish daily/monthly time sampling from simply animating imagery layers. A time-slider is not proof that historical imagery coverage or a specific year's data exists.

`geolibre-rust` and `whitebox-wasm` are powerful browser/desktop backends. Verify memory use on real GeoTIFF and LiDAR inputs, worker execution, WASI runtime, missing CRS and existing downstream licenses. `geolibre-rust` is a geoprocessing engine for GeoLibre, while `RustGIS` is a separate full application.

## Geospatial AI agent safety

`GeoAgent` supports tool metadata and action confirmation. Expose safe, typed GIS actions and reject arbitrary file deletion or network access without user consent. Keep geospatial calculations deterministic; use the LLM to **select and orchestrate** verified tools rather than hallucinate elevation, geodesic area or satellite measurements. Log provenance and versions.

## Quality gates

- [ ] Verify canonical owner/repository and whether it has runnable code, only demos, or data metadata.
- [ ] Verify license, installation/version, ArcGIS/QGIS/Python/Rust/WebAssembly compatibility.
- [ ] Never imply an indexed STAC catalog is a guaranteed working free API.
- [ ] Test projected and geographic CRS, nodata and raster alignment with small fixtures.
- [ ] Use real data and click-test all advertised plugin controls and file export.
- [ ] Preserve attribution and separate third-party imagery/terrain service terms.
- [ ] Report integration tests as `not run` until the actual target GIS runtime is tested.

## Teaching resources

- https://gispro.gishub.org/ — GIS Python programming, practical reference (check license before redistributing book material).
- https://gishub.org/teaching/ — GIS programming and software design course catalog.
- https://github.com/opengeos/python-geospatial — reference inventory, not an execution engine.
- https://github.com/opengeos/Awesome-GEE — Earth Engine references; Earth Engine access may require authentication.

**Source and attribution:** https://gishub.org/software/ and linked original repositories. This guide is independently authored.

## 2026-10-25 expansion: agent automation and separated data directories

- [OpenGeoS advanced engineering integration](opengeos-advanced-integration.md) compares 25 additional OpenGeoS implementation candidates including QGIS CLI, STAC agents, raster rendering, DuckDB/GeoParquet and agent safety.
- [Geospatial data repositories](geospatial-data-repositories.md) provides a **separate 24-entry searchable source directory**, linked to the user's [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026) rather than duplicating thousands of third-party links.
- The [software catalog](project-catalog.json), [API providers catalog](free-geospatial-apis.json), and [data repository catalog](geospatial-data-repositories.json) have different purposes: code selection, tested-or-untested service discovery, and broad datasets/portals. No directory establishes API entitlements.
