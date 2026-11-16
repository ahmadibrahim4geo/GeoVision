# GeoLibre R and RustGIS — practical developer guide

Verified upstream repositories (2026-10-24):

- [GeoLibre R](https://github.com/opengeos/geolibre-r) — MIT, inspectable R package including R source, htmlwidgets integration, tests and examples. It **embeds GeoLibre** in RStudio, R Markdown, Quarto and Shiny rather than replacing a native GIS engine. `add_sf()` transforms `sf` data to WGS84; use `add_raster()` for accessible COGs and `add_wms()`/`add_pmtiles()` for services. Cloud rasters require CORS and HTTP range requests. Do not paste third-party tokenized services into public widgets or published project files.
- [RustGIS](https://github.com/opengeos/RustGIS) — Apache-2.0, inspectable native Rust workspace with `crates/`, `apps/`, `skills/` and tests. A desktop/web GIS product plus extensibility architecture; its `Cargo.toml` declares Rust edition 2024 and minimum Rust 1.90 at review time. Includes a command-driven ribbon, Python bridge and WASM/plugin capabilities per project documentation.

## Practical selection

| Need | Choose | Implementation rule |
|---|---|---|
| Reproducible maps in R analysis, Shiny or Quarto | GeoLibre R | Keep `sf`/CRS workflows explicit, test hosted widget and CORS behavior |
| GIS desktop ribbon, local Rust geometry/analysis, wasm plugin host | RustGIS as implementation/architecture reference | Identify core crates and command handlers before extracting ideas or porting logic |
| Rust geometry primitives only | GeoRust ecosystem | Do not confuse the broad ecosystem with opengeos/RustGIS application |
| Hosted tiles, terrain, imagery in either system | Independent API catalog | Check provider access/attribution/caching; renderer licensing is unrelated |

## Example R integration (upstream package APIs)

```r
install.packages("geolibre")
library(geolibre)
library(sf)
polygons <- st_read("boundaries.gpkg", quiet = TRUE)
map <- geolibre() |> add_sf(polygons, name = "Boundaries")
save_project(map, "boundaries.geolibre.json")
```

This is illustrative, not executed against a local GIS installation. Use WGS84-compatible data in GeoJSON and verify field and geometry handling.

## Implementer checks

1. Confirm upstream pinned commit, Rust/R/package version, source code, license and dependencies before adopting.
2. Separate file readers and reprojection from rendering, command routing, scientific calculations, data services and application state.
3. Use small reproducible tests for point/line/polygon validity, raster NoData, 2D/3D coordinates and project reopening.
4. For desktop ribbons, confirm every button is connected to a real handler and produces a testable result.
5. Refer to [`free-geospatial-apis.md`](free-geospatial-apis.md) and use robust provider fallback rules for basemaps and imagery.