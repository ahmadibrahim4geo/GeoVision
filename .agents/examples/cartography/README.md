# Offline cartography/symbology example

A self-contained [MapLibre Style JSON sample](maplibre-demo-style.json) using **inline synthetic GeoJSON** for Point, LineString and Polygon. All colors and geometries in this example were authored for this repository and have no third-party asset dependencies. No terrain/raster data is provided; a raster ramp requires a real raster with verified pixel ranges and NoData.

A minimal web MapLibre runtime can load the JSON as a style (subject to verified renderer version and local/file origin restrictions). The JSON is independently parseable offline and serves as a deterministic fixture for checking layer types, property names and values, not as proof of a screenshot or native GIS rendering.

Run repository tests: `python -m unittest discover -s tests -v`.

Use the [cartographic guide](../../references/cartographic-style-guide.md) to choose the right renderer, license-aware styles and optional exports to host-native formats.
