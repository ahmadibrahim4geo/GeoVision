# Remote sensing and geospatial AI

For an in-depth view of usable source, see [`earthmapper-case-studies.md`](earthmapper-case-studies.md) and [`geoai-satellite-map.md`](geoai-satellite-map.md).

Do not conflate named software. **EarthMapper** is shared by multiple independent repositories:

- https://github.com/rmkemker/EarthMapper — earlier segmentation/classification pipeline for multispectral and hyperspectral imagery; code references older Python dependencies and must not be assumed compatible with a modern environment.
- https://github.com/HIT-SIRS/EarthMapper — EarthMapper visual autoregressive research for satellite/map translation; **README, license and assets only** when checked, without implementation/checkpoints.

## Workflow

1. Record sensor, acquisition dates, cloud masks, bands, nodata, resolutions, radiometric scaling and calibration.
2. Check licensing and source attribution for satellite scenes, derived labels, base maps and training datasets.
3. Align datasets using verified CRS, pixel-grid registration, resampling strategy and train/validation spatial separation.
4. Document normalization, model version, data leakage prevention, reproducible seeds, metric definitions and confidence/uncertainty where possible.
5. Distinguish research papers from tested software; mark models or pretrained weights as unavailable until verified.

Related runtime libraries: GDAL, Rasterio, GRASS and geo-enabled Python or Rust utilities.