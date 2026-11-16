# GIS Icon Manager — offline reference, for new icons only

This folder illustrates a **single general-use GIS symbol generator** and a **separate new-KMZ exporter**. No packaged third-party icon artwork, downloads, API keys or proprietary Google Earth assets.

- Open [index.html](index.html) locally in a modern browser to pick a marker shape, symbol, fill, border and size. Preview and download your own generated SVG or transparent PNG. The page is independent and **not** a real integrated ZEarth/Icon Manager UI.
- Run the Python standard-library utility with a PNG from the page to make a **new** KML/KMZ placemark. Example:

```bash
python examples/icon-manager/export_new_kmz.py --png new-marker.png --output custom-place.kmz --name "New Place" --lon 31.23 --lat 30.05 --color "#FFFFFF" --scale 1.1 --anchor-y 0
python -m unittest discover -s tests -p "test_icon_manager.py" -v
```

For non-pin symbols (e.g. circles), pass `--anchor-y 0.5`; the browser demo shows the suggested fraction. The default `#FFFFFF` tint leaves colored PNGs unaltered, while choosing any other tint is an explicit recoloring decision.

The generated KMZ includes `doc.kml` and `files/icons/marker.png`, with its own `IconStyle`/relative `href`, hotspot, scale and KML **aabbggrr** colors. The exporter checks PNG signature/basic structure and input bounds, writes XML with Python ElementTree and intentionally creates a **brand-new** file; it cannot load or modify someone else's KML/KMZ. It is an integration example, not a general purpose production KML writer/security sanitizer.

For existing imported KMZ, **do not apply this exporter**: preserve original icon/image bytes and colors, StyleMap and hrefs exactly where possible. Follow [KML/KMZ style preservation](../../references/kml-kmz-symbol-preservation.md). Review [Icon Manager architecture](../../references/unified-gis-icon-manager.md) and [license-aware provider catalog](../../references/cartographic-style-sources.json) before integrating any external source.

**Known limitations:** No source asset syncing, no Arabic glyph catalog, no SVG sanitization/upload, no import or edit of external KML, no live map/globe render or Cesium/WPF adapter, no exact visual matching test across Google Earth. Real product integration and security hardening remain separate tasks.
