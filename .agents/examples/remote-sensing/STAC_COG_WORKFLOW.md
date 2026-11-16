# STAC/COG satellite and raster workflow verification

This is a reproducibility **procedure**, not a claim of a processed scene or working live API.

1. Define AOI, sensor/platform, product processing level, cloud/QA policy, time window, expected resolution, licenses and reuse rights.
2. Record official STAC catalog and item IDs, asset URLs, item acquisition times, checksum or immutable version, band units, scale and offset.
3. Validate COG compression, tiling, overviews, coordinate transform, nodata and HTTP Range support; a rendered preview cannot replace numeric raster values.
4. Design a fixed target grid: projected CRS, extent, resolution, origin, resampling and vertical datum/units for elevation.
5. Apply sensor-specific per-pixel quality flags, shadow/saturation rules and reflectance scaling; `CLOUD_COVER < 5` alone cannot remove cloudy pixels.
6. Test small known pixels and compare output statistics/footprints, alignment and reproducibility. Handle missing granules, API limits and batch failures.
7. Store acquisition provenance, environment, processing recipe, output sample values, license and independent acceptance tier.

See [Remote Sensing Pipelines skill](../../skills/remote-sensing-pipelines/SKILL.md) and [quality benchmark](../../docs/QUALITY_BENCHMARK.md). Prefer project-scoped, versioned environments built from consistent conda-forge spatial binary packages; never blindly mix incompatible GDAL/PROJ binaries.
