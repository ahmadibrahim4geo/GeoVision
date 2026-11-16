# OpenGeoS advanced integration for GIS Development Skills

**Review: 2026-10-25.** Independent technical notes based on linked OpenGeoS READMEs/source trees. The OpenGeoS group and Dr. Qiusheng Wu do not endorse this integration. The [source directory](geospatial-data-repositories.md) is separate from software selection.

## Choose by task (do not cargo-cult every plugin)

| GIS task | Upstream reference | Key implementation gate |
| --- | --- | --- |
| Safe PyQGIS agent command execution | [cli-anything-qgis](https://github.com/opengeos/cli-anything-qgis) | QGIS Python bindings, `--json` output, project and layer IDs, real QGIS E2E acceptance |
| General GIS agent with context and confirmation | [GeoAgent](https://github.com/opengeos/GeoAgent) | Typed registry and explicit human consent for destructive/paid actions |
| Model-assisted satellite/raster actions | [geoai-skills](https://github.com/opengeos/geoai-skills) | Its source skills are Claude-specific; port concepts to other agents and test model dependencies |
| STAC natural-language discovery | [agentic-stac](https://github.com/opengeos/agentic-stac) | Real STAC collection and signed asset, auth, latency/quotas; model doesn't supply imagery |
| Earth Engine automation | [gee-agents](https://github.com/opengeos/gee-agents) | Earth Engine account/roles, collections, versioned scripts, exports |
| QGIS Processing via natural-language AI | [qgis-whitebox-agent](https://github.com/opengeos/qgis-whitebox-agent) | QGIS/Qt/Python + Whitebox provider + parameter schema |
| Data collection inside QGIS | [qgis-nasa-earthdata-plugin](https://github.com/opengeos/qgis-nasa-earthdata-plugin), [qgis-gee-data-catalogs-plugin](https://github.com/opengeos/qgis-gee-data-catalogs-plugin) | Asset availability, external login, license and preview-vs-data distinction |
| Raster in Web GIS | [maplibre-gl-raster](https://github.com/opengeos/maplibre-gl-raster) | GeoTIFF band handling, HTTP range/CORS, nodata, source pixel inspection |
| Source browse in map view | [geolibre-stac-browser](https://github.com/opengeos/geolibre-stac-browser) | Auth, collection paging and actual raw COG asset accessibility |
| GeoParquet and DuckDB browser spatial SQL | [maplibre-gl-geoparquet](https://github.com/opengeos/maplibre-gl-geoparquet), [maplibre-gl-duckdb](https://github.com/opengeos/maplibre-gl-duckdb) | CRS transformations, memory constraints, input security and persisted layer metadata |
| Advanced geometry editing | [maplibre-gl-geo-editor](https://github.com/opengeos/maplibre-gl-geo-editor) | MapLibre/Geoman compatibility, topology, undo/redo and save/load |
| Nested layers and symbology | [maplibre-gl-layer-control](https://github.com/opengeos/maplibre-gl-layer-control) | Layer adapters, project persistence and accessibility |
| Historical visual releases | [maplibre-gl-esri-wayback](https://github.com/opengeos/maplibre-gl-esri-wayback) | A release date is not an acquisition date; service usage rights |
| 3D Tiles and splats | [maplibre-gl-3d-tiles](https://github.com/opengeos/maplibre-gl-3d-tiles), [maplibre-gl-splat](https://github.com/opengeos/maplibre-gl-splat) | Compatible renderers/three.js, data rights and geo-registration |
| Rust data reading/conversion | [terrio](https://github.com/opengeos/terrio), [whitebox-wasm](https://github.com/opengeos/whitebox-wasm) | Early-stage terrio: independently compare CRS, formats, accuracy and benchmarks against GDAL |
| Elevation and hydrological QA | [geolibre-elevation-profile](https://github.com/opengeos/geolibre-elevation-profile), [whitebox-python](https://github.com/opengeos/whitebox-python) | Vertical datum, sample spacing and real-world DEM uncertainty |
| UI localization data | [geolibre-language-packs](https://github.com/opengeos/geolibre-language-packs) | Stable identifiers, missing Arabic translations, schema-valid pack |

## Source-of-truth and examples

* Search the [129-project catalog](project-catalog.json) for implementation patterns; review versions/third-party licenses before embedding any code.
* Search the [24-data-repository catalog](geospatial-data-repositories.json) for portals and data metadata. For broad sources, delegate to the separately maintained 2,660+ item [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026). Do **not** duplicate that dataset in a skill's context.
* Browse [52 API providers](free-geospatial-apis.json) if you need code-facing data access, checking quotas and authentication.
* See [offline catalog search example](../examples/data-catalogs/README.md) for a deterministic CLI used in unit tests. Do not confuse local catalog searching with a live STAC API.
* Preserve current **lightweight skill router**, load only a domain specialist per task, and run the acceptance report (T0–T4) rather than claiming GUI clicks work from source review.

## Three validation scenarios

1. **Automated QGIS pipeline:** open a local fixture, add vector and raster, buffer projected geometry, verify geometry/count, export map, save/reopen project in target QGIS runtime.
2. **STAC scene:** query official collection with AOI+time, validate asset href/bands/CRS and pixel-level QA, perform bounded byte-range read and export provenance.
3. **Web map:** load local COG, verify clicked raw source pixel value, edit a real polygon with undo, persist/reopen project, test release-date metadata on historical map layer.

No runtime test described here is claimed to have been executed. Licensing of data, hosted basemaps, commercial-use rights, and optional model weights must be checked separately from the source repository's code license.
