# Free / open map-marker icons — selection guide (2026-10-25)

This reference extends the [cartographic style source catalog](cartographic-style-sources.json) and the [GIS Cartography & Symbology skill](../skills/gis-cartography-symbology/SKILL.md). Its links are **discovery**, not automatic downloads; always respect original licenses, included font licenses, and the current upstream version.

## Prioritized options

| Source | License / caution | GIS and KML/KMZ use |
| --- | --- | --- |
| [Pinhead](https://github.com/waysidemapping/pinhead) | CC0 as declared by maintainers | Excellent broad POI SVG library for *new* markers; rasterize licensed SVG to PNG and embed for portable KMZ |
| [Mapbox Maki](https://github.com/mapbox/maki) | CC0; [original license](https://github.com/mapbox/maki/blob/main/LICENSE.txt) | Lightweight map POI symbols; Cesium PinBuilder supports Maki IDs; PNG output can be embedded in a *new* KMZ |
| [Temaki](https://github.com/rapideditor/temaki) | CC0 / public-domain dedication | Wider specialist POI catalog; verify names/formats against pinned version |
| [Map Icons Collection](https://mapicons.mapsmarker.com/) | CC BY-SA 3.0, [terms](https://mapicons.mapsmarker.com/about/license/); credit and ShareAlike for adapted icons | Colored icons similar in use to classic Google Earth placemarks; include correct source and attribution when republishing |
| [Scott de Jonge Map Icons](https://github.com/scottdejonge/map-icons) | **Mixed**: MIT JavaScript, SIL OFL 1.1 icon font, CC BY 3.0 documentation; **deprecated** | Historic marker shapes/icons; font→PNG rasterization when compatible, but code MIT does NOT grant rights over font files |
| [Leaflet.ExtraMarkers](https://github.com/coryasilva/Leaflet.ExtraMarkers) | MIT plugin; icon-font providers separately licensed | Generate marker appearances for Leaflet; capture exportable transparent PNG for new KMZ; plugin HTML/CSS is not KML symbology |
| [Google Material Symbols](https://github.com/google/material-design-icons) | Apache 2.0 icons; [official documentation](https://developers.google.com/fonts/docs/material_symbols) | SVG/PNG icons for new map placemarks; no license implication for Google Earth's own pushpins/artwork |
| [Leaflet.awesome-markers](https://github.com/lennardv2/Leaflet.awesome-markers) | MIT plugin/colored marker shapes; Font Awesome/Glyphicons/Ionicons dependencies licensed separately; older plugin | Leaflet runtime renderer, not a ready-made KML style bundle |
| [CesiumJS PinBuilder](https://cesium.com/learn/cesiumjs/ref-doc/PinBuilder.html) | Cesium engine under its own license; generated icons may contain independently licensed icon assets | Runtime generator for solid-color, Maki-ID, text or image pins; to export KMZ, create PNG and package *only for newly authored or explicitly restyled* features |

For license evidence and machine-filtering see each record's `license_evidence_url`, `kmz_use`, `compatibility` and `notes` in [JSON catalog](cartographic-style-sources.json).

## Three-site update (2026-10-25) and unified manager

- **Map Icons Collection** now supports SVG alongside PNG (2026-10-14) and user-defined colors; continue treating its assets as CC BY-SA 3.0 with attribution, adaptation conditions and no hotlinking.
- **Streamline — Google Earth Placemark** is a mixed-license search/discovery entry. Never bulk-import its standard free assets into an in-app selectable/reusable/downloadable icon library: [Streamline Free License](https://help.streamlinehq.com/en/articles/5354376-streamline-free-license) forbids that. Verify explicitly named open-source sets and CC BY 4.0 credits separately; premium packs require their own license.
- **Mapular Icon Creator** is a reference for designing a **new** generator (shape, brand/icon, fill/border, SVG/PNG); its own site source code is not shown to be open source, and any uploaded logo has independent rights. Use the independent [general GIS Icon Manager plan](unified-gis-icon-manager.md) and [offline working reference](../examples/icon-manager/README.md) instead of copying the website.

The library is **for general GIS symbolization**, and KMZ exporting is only a dedicated adapter. Imported KML/KMZ never receives these replacement icons automatically: [style preservation](kml-kmz-symbol-preservation.md).

## Decision procedure (agent-ready)

1. **Importing existing KML/KMZ?** Do **not** choose anything from this catalog. Start with the original document and bundled icon resources, preserve its Style / StyleMap / styleUrl, color, scale and assets. See [preservation contract](kml-kmz-symbol-preservation.md).
2. **Creating a new GIS feature or a user explicitly clicked Replace style?** Prefer Pinhead, Maki or Temaki for broad CC0 SVG symbols. For a Google Earth-like colored placemark, generate a new custom pin with Cesium PinBuilder (where Cesium is the actual renderer) or a separately licensed marker template. Record the chosen icon's license.
3. For KML/KMZ delivery, use portable transparent PNG icons for broad compatibility, include them in the archive with safe relative paths (`files/icons/...`), define `IconStyle/Icon/href`, `scale`, `heading`, `hotSpot`, and color according to the user's choice. KML itself supports other image types but **SVG/font/icon classes are not a cross-viewer portable replacement**.
4. Keep a mapping between source asset ID, URL/ref/version, exported PNG filename and credit/license; provide attribution where required. Cache/rasterize only if upstream terms permit; do not bundle commercial fonts or scrape default Google Earth artwork.
5. Preview across contrasting basemaps and zoom levels. If exporter cannot represent a chosen symbol, ask before **lossy** export; never silently substitute a blue pushpin.
6. If user wants the exact imported appearance, preserve imported PNG/JPG bytes and KML styling **instead of generating new assets**. Imported package provenance belongs to its original author, not the icon catalog.

## Platform notes
- **ArcMap / ArcGIS Pro / QGIS:** use each host's native symbol classes for *new* layers; KML conversion may be lossy and must not replace preservation-first import.
- **Cesium / ZEarth-like desktop globe:** keep source KML/StyleMap model independent of UI theme, fallback symbol library and editable styling palette. Generated PinBuilder icons apply only when no imported style exists **or** user explicitly overrides it; missing-icon placeholder must be transient and must **never** be saved over source style.
- **MapLibre / Leaflet:** no direct guarantee that their circle/marker/CSS style can be round-tripped to KML; export through a deliberate adapter and compare the artifact.
