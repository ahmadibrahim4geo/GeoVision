# Standalone PyQGIS GIS-engine round-trip (native runtime required)

This **illustrative** script [standalone_roundtrip.py](standalone_roundtrip.py) initializes installed PyQGIS without QGIS Desktop UI, creates an in-memory WGS84 point layer, writes a **new** GeoPackage, reopens it and asserts coordinates, attributes, feature count and EPSG:4326. It uses temporary output by default, performs no network calls and refuses overwrite.

Run it **inside the QGIS-supplied Python environment with native providers/Qt/PROJ configured**, not arbitrary `pip install qgis`:

```bash
# Linux headless example where Qt requires an offscreen platform:
export QT_QPA_PLATFORM=offscreen
python examples/qgis-engine/standalone_roundtrip.py
# Optional new path (the file must NOT exist):
python examples/qgis-engine/standalone_roundtrip.py --output /tmp/qgis_engine_roundtrip.gpkg
```

For Windows use the installed QGIS/OSGeo4W configured environment; the POSIX paths are illustrative. Report the exact QGIS version/OS and output/exception. Successful JSON output alone is a T2 smoke check, not T3 desktop navigation or T4 distribution. If PyQGIS is missing, record **T2 not run**. See [architecture/reference](../../references/qgis-engine-integration.md).
