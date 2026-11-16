# GeoLibre integration: platform, agents, Python and plugins

> Curated reference for GIS Development Skills, reviewed 2026-10-24. GeoLibre is an independent upstream project. This page links official documentation and does not copy its source code.

**Upstream project:** https://github.com/opengeos/GeoLibre | **Official website:** https://geolibre.app/

## What to use for each GIS development task

| Request | Starting point |
| --- | --- |
| Agent creates an editable interactive GeoLibre map | Official skill in `opengeos/GeoLibre/skills/geolibre`, preferably its `geolibre-mcp` project tools |
| Python scripts, Jupyter notebooks and programmatic exports | Python `geolibre.Map` API |
| RStudio / Quarto / Shiny | Separate MIT `opengeos/geolibre-r` package |
| Add a new control, data service, or workflow to GeoLibre | GeoLibre Plugin API and official plugin template |
| Design your own desktop GIS app | Study GeoLibre's Tauri/React, renderer-neutral state, command and plugin architecture |
| Build a native GIS application in Rust | Compare separate `opengeos/RustGIS` project |
| ArcMap, ArcGIS Pro, QGIS tools | Use ArcPy/PyQGIS platform instructions; GeoLibre plugins are not drop-in substitutes |

## Use the official GeoLibre agent skill, not an imitation

- Upstream skill: https://github.com/opengeos/GeoLibre/tree/main/skills/geolibre
- Skill documentation: https://geolibre.app/agent-skill/
- GeoLibre's own skill specializes in authoring project files, styling, view state, Python, MCP and HTML exports. Delegate GeoLibre-specific details to it; GIS Development Skills continues to own platform selection, CRS, source quality and QA.
- The documented optional MCP installation is `pip install "geolibre[mcp]"`. Run `geolibre-mcp --root <approved-workspace>` through a supported MCP client. Restrict writable paths to an intended project directory.
- Project-authoring MCP tools create portable `.geolibre.json` files without an open browser. The separate `live_*` tools operate an existing GeoLibre Desktop map only when its Notebook relay is enabled. File writes are not automatically live edits.
- Follow upstream verification steps (for example `describe_project`) and test the resulting map in the real target before promising a working preview.

## Minimal Python example (documented APIs, not run here)

```python
from geolibre import Map

m = Map(center=(31.2, 30.0), zoom=6)
m.add_geojson('study_area.geojson', name='Study area')
m.save_project('study-area.geolibre.json')
m.to_html('study-area.html')
```

`pip install geolibre` provides the package. Ensure `study_area.geojson` exists and uses correct geographic coordinates. The exported HTML may still depend on hosted app assets and external tiles; test CORS, authorization, licenses and offline expectations. Never expose API keys or assume paths on the author's machine are accessible to other users.

## Plugin development

- Official Plugin API: https://geolibre.app/plugin-api/
- Plugin template: https://github.com/opengeos/geolibre-plugin-template
- External plugins use a `plugin.json` manifest, a bundled JS entry, version/identity matching and a compatible `engines` declaration. The template packages a ZIP for desktop installation; web deployments can load approved manifests subject to CORS and policy.
- Define an acceptance test for **each command**: click -> valid action -> observable GIS output -> project save/open -> plugin cleanup. Test renderer compatibility instead of assuming every plugin supports MapLibre, Mapbox, Cesium and ArcGIS equally.
- **Security:** external plugins run as trusted JavaScript in the app, not in a robust security sandbox. Review code, secrets exposure, network endpoints and CSS scope before installation.

## Architecture and GIS data providers

GeoLibre separates a React/Tauri application shell, portable project/layer state, MapEngine adapters (MapLibre, Mapbox, Cesium, ArcGIS Maps SDK JS), plugins and processing (including DuckDB-WASM/Whitebox). This is an architectural **reference**, not evidence that upstream parts can be copied into proprietary environments without separate licensing checks.

- For a standalone app, separate renderers, geoprocessing engines, layer store, UI commands and persistent project format.
- Disable or explain unsupported commands when switching rendering engines; do not show non-functional buttons.
- Its integrations (e.g. Sentinel-2, STAC, NASA, OSM, elevation, historical imagery) are a way to *discover* services. Verify each provider's current access, key requirements, quota, attribution, caching and redistribution permissions in `free-geospatial-apis.json` and provider documentation.
- Validate WGS84 longitude/latitude, raster nodata and units, path portability, geocoding/network behavior and saved project round trips.

## Upstream source links

- https://geolibre.app/architecture/
- https://geolibre.app/python/
- https://geolibre.app/mcp/
- https://geolibre.app/agent-skill/
- https://geolibre.app/plugin-api/
- https://geolibre.app/user-guide/plugins/
- https://geolibre.app/user-guide/processing/
- https://geolibre.app/user-guide/rendering-engines/
- https://github.com/opengeos/GeoLibre/tree/main/skills/geolibre
