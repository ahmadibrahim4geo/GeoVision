# Building native Windows GIS apps with .NET

Use this pathway for **independent Windows desktop GIS software** or ArcGIS Pro **C# add-ins**. These are different targets. **ArcGIS Pro SDK** add-ins require ArcGIS Pro; independent apps must not silently depend on proprietary ArcGIS libraries.

## Independent application architecture

| Layer | Typical tools to evaluate | Required practical test |
|---|---|---|
| Shell/UI | WPF or another supported .NET desktop stack, MVVM | Splash lifecycle, title bar, keyboard shortcuts, accessibility, cancel/recovery |
| Map view and 3D globe | WebView2 + MapLibre GL JS or CesiumJS | Confirm WebView process lifecycle, bidirectional event bridge, zoom/pan/tilt/extent |
| Geometry | NetTopologySuite | Buffer/overlay validity, snap precision, geometry type changes, empty/invalid shapes |
| CRS | ProjNET or a verified PROJ bridge | Real geographic-to-projected transformation with expected axis order and units |
| Raster/data I/O | GDAL and/or LibTiff.NET where justified | Large GeoTIFF open, geotransform, NoData, band metadata, raster pyramids |
| Data analysis | DuckDB with Spatial extension, where supported | Query real geospatial datasets, document extension version and memory usage |
| Imagery discovery | STAC-compliant client/services | Query by bbox/time/cloud; attribute providers; enforce service terms |
| GeoAI | ONNX Runtime with explicitly licensed models | Coordinate-transform results; log model ID/confidence; handle CPU-only fallback |

Repository references: https://github.com/NetTopologySuite/NetTopologySuite ; https://github.com/NetTopologySuite/ProjNet4GeoAPI ; https://github.com/MicrosoftEdge/WebView2Samples ; https://github.com/BitMiracle/libtiff.net ; https://github.com/duckdb/duckdb ; https://github.com/microsoft/onnxruntime.

For DXF, https://github.com/haplokuon/netDxf is a **currently archived** reference; verify a maintained option before choosing it for new products.

## ArcGIS Pro add-ins are not Python toolboxes

- **Python Toolbox (`.pyt`)**: Python 3/ArcPy geoprocessing tool and parameter UI.
- **ArcGIS Pro SDK for .NET**: C# add-ins and ribbon customization, DAML, dock panes and WPF/MVVM. Consult https://github.com/Esri/arcgis-pro-sdk and pin exact Pro/.NET version.
- Keep processing algorithms and user commands separate so they can be unit-tested outside the UI where licenses and APIs allow.

## Functional delivery gate

Implement **one command at a time** and record: command name, input, handler location, expected observable state, errors, unit/integration/manual test evidence, and project file roundtrip. Never count a visible ribbon button as a completed implementation.

## Esri Pro SDK versus standalone GIS

The official [ArcGIS Pro SDK snippets](https://github.com/Esri/arcgis-pro-sdk-snippets) offer C# patterns only for add-ins running **inside ArcGIS Pro**. Independent WPF/Cesium or MapLibre applications must not silently take ArcGIS.Desktop libraries, ribbon DAML, edit sessions or QueuedTask as standalone dependencies. Study UX ideas separately and choose independently licensable GIS engines, or explicitly declare an optional commercial ArcGIS Maps SDK integration.
