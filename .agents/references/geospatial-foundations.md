# Spatial algorithms and file interoperability

## Library decision rules

- **GDAL / OGR**: raster/vector drivers, projection, warping, raster pipelines, metadata. https://github.com/OSGeo/gdal
- **PROJ**: CRS/datum transformations and coordinate operations. https://github.com/OSGeo/PROJ
- **GEOS**: planar computational geometry, topology. https://github.com/libgeos/geos
- **GeoRust**: Rust `geo` geometry, `gdal` bindings, `proj` bindings, `geozero` streaming and `rstar` indexing. https://github.com/georust
- **Rasterio**: Pythonic GDAL-based raster IO. https://github.com/rasterio/rasterio
- **GRASS GIS**: geoprocessing engine for raster, temporal, hydrology and vector analysis. https://github.com/OSGeo/grass

## Mandatory invariants

1. Specify and preserve spatial reference for every output. A shapefile's `.prj` is necessary but not always sufficient for all transformation details.
2. Check whether x/y axis order and coordinate operation differ between libraries; test a known coordinate.
3. Do not treat geographic degrees as meters. For buffers/areas/distance, select a suitable projected/geodesic method for the study area and error tolerance.
4. Preserve NoData and masks in raster operations. Honor pixel alignment, origin, resampling, compression, bands, units and data type.
5. Validate geometry emptiness, Z/M values, multipart input, intersections, holes, corrupt polygons and precision tolerances.
6. When targeting Shapefile, names limited to **10 characters**; check field type, text encoding, dates and truncation. Prefer GeoPackage/GeoParquet when supported.
7. Distinguish `z` coordinate, vertical datum, and DEM-derived elevation with explicit provenance.
8. Use a neutral analysis core separate from host application UI whenever practical.