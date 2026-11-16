# Unified GIS Icon Manager — general-purpose architecture with a dedicated KML/KMZ adapter

**Status:** implementation reference and acceptance specification. This document and its sample are NOT a shipped ZEarth plugin or evidence of real Cesium/Google Earth integration. The governing skills are [Cartography](../skills/gis-cartography-symbology/SKILL.md) and [KML/KMZ](../skills/kml-kmz-engineering/SKILL.md).

## One library, multiple consumers

The GIS Icon Manager provides searchable, categorized and license-aware icon definitions for **ordinary map layers and general application use** (ArcMap/Pro, QGIS, MapLibre, Leaflet, Cesium, standalone desktop GIS, reports and UI). KML/KMZ is a **specialized adapter**, not a separate icon library. Do not couple internal icon IDs to any KML namespace or specific renderer.

**Two different flows:**
- **Create / style new GIS object** → search library or generate custom icon → preview → choose renderer/export format → save user-defined style.
- **Open a received/imported KML or KMZ** → retain source KML styling/images/document hierarchy unchanged → render as authored (or show transient warning/fallback without mutation) → optionally allow explicit *Edit Icon* and *Reset to Original* for selected items → Save As preserving unedited features.

No library search, default profile, branding or theme selection can automatically override imported KML/KMZ styling. See [style preservation contract](kml-kmz-symbol-preservation.md).

## Proposed internal contract and persistence

Suggested portable icon definition (app adapter may use different schema):

```json
{
  "id": "local:places:airport",
  "title": {"en": "Airport", "ar": "مطار"},
  "category": "transport",
  "keywords": ["airfield", "flight", "airport", "مطار"],
  "source": {"provider_id": "mapbox-maki-icons", "source_url": "https://github.com/mapbox/maki", "version": "record-exact-ref", "asset_path": "icons/airport.svg"},
  "license": {"spdx_or_label": "CC0-1.0", "attribution_text": null, "verified_for_reuse": true},
  "visual": {"svg_or_local_asset": "assets/airport.svg", "color": "#C32F4B", "stroke": "#FFFFFF", "opacity": 1, "size_px": 32, "rotation_deg": 0, "anchor_fraction": [0.5, 1.0]},
  "origin": "new-user-feature",
  "export": {"kml_kmz_icon_png_relative_path": "files/icons/airport.png"}
}
```

Do not mark `verified_for_reuse: true` without exact upstream license evidence for **that version/asset**. The example is illustrative; `source`/asset path and icon ID must be checked before real use. Source files, rendered PNG derivatives and style metadata must be separately traceable. Keep unused source assets out of output.

## Icon Manager interface / API checklist

1. **Library:** search Arabic/English names and aliases; category filter (transport, medical, tourism, education, nature, infrastructure, administration, misc); source and license filter; grid/list preview; keyboard and screen-reader support; thumbnails.
2. **Controls:** create custom marker with shapes (pin, circle, badge, square), symbol/glyph or authorized user logo, fill and outline color, opacity, size, rotation, label, anchor/hotspot and preview on light/dark basemaps. Store explicit user edits in reversible project state.
3. **Inputs:** SVG/PNG assets with validated file type/size. Treat arbitrary SVG as potentially active/untrusted XML; disallow scripts, external href, DTD, event handlers, embedded HTML or automatic network loads; prefer rasterization in a sandbox or a vetted sanitizer. Confirm logo/asset rights; do not redistribute external icon fonts on a code license.
4. **Exports:** SVG or PNG for generally new map symbols where host supports it; renderer adapters for QGIS/ArcGIS/MapLibre/Leaflet/Cesium; for KML/KMZ, prefer transparent PNG with safe relative `IconStyle/Icon/href`, `hotSpot`, scale and heading plus credits when needed. Remember KML uses `aabbggrr`, not CSS `#rrggbb`; preserve alpha and original sizing.
5. **Favorites / recents / imports:** these are references and user content, not authorization to overwrite third-party assets; never silently download remote icons. Deduplicate with source+version+asset ID and cache with explicit provenance/rights.
6. **In-app controls for imported KML/KMZ:** default option **Use Original Icon** (immutable original), alongside **Choose New Icon** (explicit edit only), **Preview Only** and **Reset to Original**. A fallback marker due to a missing icon is temporary only and excluded from exports.
7. **Modes:** `new`, `original`, `preview`, `overridden`, `reset`. Disallow transitions `original -> overridden` without direct user command. Project reopen must honor original style refs; no last-used symbol injection.
8. **Audit/export:** record provider/license, chosen icon, changed feature IDs, `StyleMap` normal/highlight handling, exporter output files and errors/loss report. For imported KMZ without edits, preserve original image bytes and shared references when possible. Avoid mutating siblings when shared `Style` references are reused.

## User-provided reference sites and license constraints

- [Map Icons Collection](https://mapicons.mapsmarker.com/): since **2026-10-14**, provides customizable SVG alongside PNG. [License](https://mapicons.mapsmarker.com/about/license/) is **CC BY-SA 3.0**, requiring proper attribution and ShareAlike for adaptations. Check derivative assets and do not hotlink. Good cartographic symbols after explicit license-compliant ingestion for **new objects only**.
- [Streamline Google Earth Placemark icon search](https://www.streamlinehq.com/icons/tag/google-earth-placemark): a **mixed-license discovery site**, NOT a freely redistributable icon collection. The [Streamline Free License](https://help.streamlinehq.com/en/articles/5354376-streamline-free-license) prohibits including free (non-open-source) assets in an application that lets users select, customize, download or reuse them as standalone assets. Use only individually identified genuinely open-source sets with applicable terms (some CC BY 4.0) for in-app libraries; otherwise use link/discovery only. Premium requires a separate review.
- [Mapular Icon Creator](https://mapular.com/tools/map-icon-creator): excellent **feature/workflow inspiration** (shapes, colors, logo input, private browser rendering, SVG/PNG export). The site says icons created there may be used commercially. Do not infer the **website code/UI source** is open source; independently implement the manager. Third-party uploaded logos retain independent rights.
- [Maki](https://github.com/mapbox/maki), [Temaki](https://github.com/rapideditor/temaki), [Pinhead](https://github.com/waysidemapping/pinhead) remain free-first icon candidates; [Lucide](https://lucide.dev/license) can power a newly built generator under its own license. Consult [complete catalog](cartographic-style-sources.json).

## KML/KMZ adapter contract

For **new icon export**:
1. Let user select new point and icon, or upload user's own original PNG. Render to transparent PNG at chosen pixel size (e.g. 16/24/32/48/64/128 when appropriate), keep source metadata and attribution.
2. Emit `<Style id="...">` with `<IconStyle><color>aabbggrr</color><scale>...</scale><heading>...</heading><Icon><href>files/icons/....png</href></Icon><hotSpot x="0.5" y="0" xunits="fraction" yunits="fraction"/></IconStyle>`; assign corresponding `<styleUrl>#...</styleUrl>` to the new Placemark and package the PNG in KMZ. Explicitly distinguish icon **pixel size** from KML display `scale`, and use a center hotspot for centered circle/square artwork; use a bottom-tip hotspot for pin shapes. Default KML image tint is white `ffffffff` to avoid recoloring the original PNG.
3. Validate the image header, max file/expanded size, path and URL handling, ZIP member limits, XML escaping and unique href. Keep `description`/ExtendedData unrelated to icon metadata unchanged.
4. Round-trip export/reopen to verify placement, color, heading, hotspot, image bytes and StyleMap if used, noting target viewer differences.

For **existing external KMZ import**: **do not enter generator/export-normalization flow** unless the user requests explicit restyling. Preserve original Style/StyleMap/styleUrl and local image byte hashes; do not bundle a newly generated image over original icon, recompute style color, or reassign original shared style to other features. Report unsupported remote links without silently replacing them.

## Reference example and acceptance evidence

The **standalone offline** [Icon Manager demonstration](../examples/icon-manager/README.md) creates *new* icons as SVG/PNG without external icon assets. Its [PNG-to-new-KMZ utility](../examples/icon-manager/export_new_kmz.py) takes a user-controlled PNG and exports a single new Placemark with a packaged relative icon path; it intentionally has **no import/rewrite operation**. Existing imported KMZ fidelity is independently tested by the [KMZ style-preservation fixture](../examples/kml-style-preservation/README.md).

Acceptance: [ ] general GIS library is source/host neutral; [ ] icon inputs have rights and validation; [ ] generated files open; [ ] new KMZ includes relative PNG and original specified style; [ ] received/imported KMZ unchanged unless explicit edit; [ ] missing-asset fallback never persists; [ ] English/Arabic search, accessibility and native host testing logged accurately.
