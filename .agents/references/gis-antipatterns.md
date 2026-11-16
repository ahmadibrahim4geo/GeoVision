# GIS Failure Patterns: Before and After

| Before (incorrect/unsafe) | After (required pattern) |
| --- | --- |
| `geometry.buffer(500)` on longitude/latitude degrees | Choose a valid projected metric CRS, reproject with tested datum transformation, buffer 500 metres; or use proper geodesic method |
| `arcpy.mapping` as though ArcGIS Pro uses it | Use version-appropriate Pro `arcpy.mp` vs ArcMap Python 2 APIs |
| Unlimited calls to a public imagery/geocoder endpoint | Check terms, auth, quotas and attribution; approved provider and throttling |
| QGIS Plugin button visible ⇒ feature works | Run the callback with real layers, validate output and UI state in named QGIS version |
| Rendered WMS image treated as bathymetry/elevation raster | Request authorized numerical GeoTIFF/NetCDF/OPeNDAP, verify NoData and vertical datum |
| Snap/reverse original pipes without user approval | Read-only QA first, then opt-in edit, backup, transaction/rollback and read-back |
| Concatenate unsafe SQL, unpack `../` archive members | SQL parameters, identifier allowlist, safe paths, input size and archive limits |
| Combine differently aligned rasters by array index | Define common projection, grid origin/resolution, resampling and nodata policy |

Review [security](geospatial-security.md), [performance](geospatial-performance.md) and the [GIS decision tree](agent-decision-tree.md).
