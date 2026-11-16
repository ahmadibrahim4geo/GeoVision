# GIS Code Discovery & Reuse — Examples and Scenarios

Use [GIS Code Discovery & Reuse](../../skills/gis-code-discovery/SKILL.md) through the [Main GIS Router](../../skills/gis-development/SKILL.md).

## Copyable Prompts

### English
> Discover, evaluate, and adapt a verified code recipe to sample raster elevation from a DEM at coordinate points. Verify the Coordinate Reference System (CRS) transformation, handle NoData values explicitly, and provide a testable function with MIT/Apache license compliance.

### العربية
> ابحث عن وصفة برمجية موثوقة ومجربة لاستخراج قيم الارتفاع من نموذج تضاريس DEM عند مجموعة إحداثيات. تحقق من تحويل نظام الإحداثيات (CRS)، وعالج قيم NoData بوضوح، وقدّم دالة قابلة للاختبار مع الالتزام بتراخيص MIT أو Apache.

---

## Acceptance Scenarios

| Scenario | Request | Expected Discovery & Output | Failure Caught |
| :--- | :--- | :--- | :--- |
| **ArcGIS Pro Ribbon Button** | "Add a custom map exploration button in ArcGIS Pro" | Routes to `esri-arcgis-pro-sdk-snippets`, generates .NET C# Button class with `QueuedTask.Run`. | Suggesting Python ArcPy script for a .NET desktop add-in. |
| **PyQGIS Processing Tool** | "Write a custom QGIS processing algorithm" | Routes to `qgis-pyqgis-developer-cookbook`, subclasses `QgsProcessingAlgorithm` with `parameterAsSource`. | Writing a standalone script without proper QGIS parameter/sink lifecycle. |
| **Raster Elevation Sampling** | "Extract DEM elevation at given points" | Routes to `gdal-python-samples` or `intro-gispro` Rasterio recipe; enforces CRS reprojection and NoData check. | Returning raw pixel indices without spatial coordinate transformation or ignoring NoData. |
| **Interactive WebGIS** | "Display vector tiles on a web map" | Routes to MapLibre GL JS / Leafmap; provides clean initialization with EPSG:3857/4326 handling. | Suggesting desktop ArcMap or proprietary C# snippet for a browser UI. |
| **KML Placemark Colors** | "Parse KMZ styles without losing custom icon colors" | Routes to KML element tree recipe, inverting `aabbggrr` hex color strings to `#rrggbbaa`. | Dropping `<Style>` tags and defaulting all icons to standard pushpins. |
| **Unverified License** | "Copy code from an untrusted forum post with no license" | Refuses direct copy; extracts mathematical logic conceptually and labels as `adapted` with license note. | Blindly pasting proprietary or unverified code into a production repository. |

---

## Test Evidence Boundary

Run `python -m unittest tests/test_code_discovery.py -v` to verify query parsing, technical platform routing, recipe retrieval, and license evaluation. Unit tests provide T0/T1 evidence. Live SDK add-in execution and desktop QGIS runtime require T2–T4 observed evidence.
