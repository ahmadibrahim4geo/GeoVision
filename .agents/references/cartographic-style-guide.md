# Cartographic style selection — operational guide

## Scientific/visual decision tree

| Source semantics | Renderer | Palette | Pitfall |
| --- | --- | --- | --- |
| Categorical land cover, soils, geology | Unique values or rule-based renderer, optional hatching | Qualitative, discriminable colors | Never order nominal classes by an arbitrary gradient |
| Elevation, rainfall, temperature | Continuous stretch or classified numeric | Sequential if monotonic, diverging only around a meaningful midpoint | NoData must not appear as a real minimum |
| Anomalies, positive vs negative changes | Diverging | Clearly marked zero/reference | Do not center on zero if zero has no significance |
| Flow direction (river/network) | Directional line symbols/arrows | Contrast to basemap | Preserve digitizing direction vs true flow attribute |
| Station measurements | Point size, classes, or small multiples | Magnitude/ordered color | Distinguish circle radius from area proportionality |
| DEM/slope/aspect | Stretch/hillshade/aspect cyclic palette | Scientific perceptual palette | Aspect is cyclic; multiband RGB is not a single-band physical ramp |

Use data distribution and units to set range/class breaks; include nulls, outliers, basemap contrast, color-vision accessibility, line widths at export scale, label collisions, Arabic and English legibility and accurate legend metadata. Prevent categorical/continuous mismatch. A heatmap is a smoothing operation: preserve documented kernel, radius and normalization; it does not reveal individual values. For precipitation, do not casually reuse a global diverging palette.

## Host adapter matrix

| Platform | Typical artifact/API | Notes |
| --- | --- | --- |
| ArcMap 10.x | .style, .lyr, arcpy.mapping | Python 2.7 legacy; Pro-only .stylx/.lyrx not backward-compatible |
| ArcGIS Pro | .stylx, .lyrx, arcpy.mp/CIM | Pin major version and save/re-open real layer to verify |
| QGIS | .qml, .xml, QgsRenderer, QgsStyle | Verify version; SVG/embedded font paths and QGIS expressions may not translate |
| MapLibre | Style JSON, circle/line/fill/raster/heatmap/color-relief, expressions | Validate spec version, data-driven support, tile/source rules |
| CesiumJS | Entity and material/imagery styling | Separate 3D terrain/lighting and geometry primitives from 2D style JSON |

## General GIS Icon Manager and KML/KMZ adapter

A single [Unified GIS Icon Manager](unified-gis-icon-manager.md) serves general GIS point markers and host-specific adapters. Search Arabic/English categories and licenses, create custom SVG/PNG markers, preview and reuse them only when rights allow. Include [Map Icons Collection SVG/PNG, Streamline mixed-license discovery, and the Mapular generator workflow](map-marker-icon-library.md); do not embed Streamline's non-open-source free assets as a reusable download/picker catalog, or copy Mapular site code. **Imported** KML/KMZ remains preservation-first; the library is for new or explicitly user-restyled features. A [standalone bilingual offline generator example](../examples/icon-manager/README.md) illustrates a working slice, not native ZEarth integration.

## Map placemark icons and KML/KMZ priority

- [8 map-icon libraries and CesiumJS PinBuilder](map-marker-icon-library.md) are recorded in the source catalog with per-library code/font/icon licensing and KMZ export roles. For new user-authored placemarks, prefer CC0 Pinhead/Maki/Temaki and export a PNG in a relative `IconStyle/Icon/href` when writing portable KMZ.
- **Do not use these icon libraries when importing another person's styled KMZ.** Preserve original author colors, icon resources, hotspot, line width, fill/outline, `StyleMap` states, IDs and `styleUrl` exactly or equivalently. User must explicitly choose a restyle operation. Follow [KML/KMZ style preservation contract](kml-kmz-symbol-preservation.md).
- Canvas/Leaflet icon fonts and SVGs require adapter/rasterization; KML viewers cannot be assumed to render browser icon classes. Cesium `PinBuilder` is for newly generated pins or explicit overrides, not generic replacement on import.

## Ingesting style collections without getting lost

1. Select usage platform, geometry, theme, language, required license and accessible visual contrast.
2. Search [style source catalog](cartographic-style-sources.json) first, then only the chosen upstream collection and its actual file(s). Do not bulk-copy/re-host all styles or fonts.
3. Record file format, download URL, repository/ref/path, creator, version, preview, palette provenance, required fonts/SVGs, target host compatibility, redistribution rights and last validation status.
4. Mark `unverified-per-item` and do not redistribute when the exact asset license is missing. The user's ArcMap Style Collection organizes third-party Esri/USGS/BGS/DGGS files under their original rights; its own MIT organizing code cannot relicense them.
5. Import in isolated/test layer, run sample preview/legend QA, save/reload and record host-version evidence.

Example MapLibre styling fixture: [examples/cartography](../examples/cartography/README.md). Scientific color sources and licenses are inventoried in the JSON catalog; links are external dependencies, not bundled code.
