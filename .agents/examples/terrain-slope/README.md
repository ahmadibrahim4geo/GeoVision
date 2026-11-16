# Free DEM slope example: Horn 3×3 on projected ESRI ASCII Grid

A **fully offline, standard-library Python** slope workflow with genuine raster output and a deterministic synthetic fixture. No ArcGIS, Global Mapper, paid LLM, API key, Google Earth Engine or cloud account is required.

```sh
python examples/terrain-slope/slope_ascii.py \
  --input examples/terrain-slope/fixtures/planar_slope.asc \
  --output /tmp/horn_slope.asc \
  --horizontal-unit m --vertical-unit m --ack-projected
python -m unittest tests.test_terrain_slope -v
```

On Windows choose your own new writable output file path instead of `/tmp`. The fixture is **artificial**, with 10 m raster cells and a 10 m rise per cell eastward. All 9 interior cells should be **45°**, and outer 16 cells `NODATA_value=-9999`. The tool writes a companion `.provenance.json` file, recording algorithm, input/output, units and clear CRS uncertainty.

## Constraints that matter for GIS

- A plain ASCII Grid header has no authoritative CRS or vertical datum; **you** must verify the source and run with `--ack-projected`. This flag is an acknowledgement, **not automated CRS validation**. Never input EPSG:4326 degrees as if they were meters/feet. Reproject it first using QGIS/GDAL, with selected raster resolution and sampling.
- `--horizontal-unit` is the cell-size linear unit; `--vertical-unit` is DEM Z unit. The example converts feet/meters consistently and calculates slope as `atan(hypot(dzdx,dzdy))` in **degrees**.
- Horn's algorithm uses 3×3 differences. Outer edge and any kernel touching input NoData become NoData; the example does not fill gaps, extrapolate edges, account for map distortion, or produce survey-grade terrain accuracy.
- Example grids are deliberately limited to 2,000,000 cells in memory. For large real-world rasters prefer tiled/windowed GDAL, GRASS GIS or QGIS Processing and independently compare outputs on the same test grid.
- The tool refuses overwrites and leaves input raster untouched; it does not contact external services. Verify outputs with QGIS and real CRS when using geographic studies.
- Evidence level: deterministic Python/offline test (**T2**). **T3 GIS engine** and **T4 user deployment** remain unverified.

See [free-first policy](../../references/free-first-policy.md) and [terrain specialist](../../skills/terrain-hydrology-lidar/SKILL.md).
