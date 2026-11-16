# World Gazetteer: Place Search and Scale-Aware Reference Layer

**Purpose:** GIS desktop/web/ArcGIS/QGIS agents should implement two distinct capabilities from a shared source-aware gazetteer model: (1) name-based search and (2) an independently switchable, scale-dependent map-label overlay. See [offline working prototype](../examples/gazetteer/README.md), [geocoding providers](geocoding-place-search.md), [GIS Desktop skill](../skills/gis-desktop-application-development/SKILL.md) and [Cartography skill](../skills/gis-cartography-symbology/SKILL.md).

## Source choice (free first)

- **Default:** [GeoNames](https://www.geonames.org/export/), downloaded country files (e.g. EG.txt) or allCountries.txt, in local indexed SQLite/GeoPackage. CC BY 4.0 attribution, WGS84 EPSG:4326, download date/version/checksum and completeness caveats required. Updates should be explicit and non-destructive. Keep place IDs, language-specific names, feature class/code, administrative hierarchy and population. Download raw datasets separately; do not distribute huge gazetteer dumps as part of this skill.
- **Arabic/English names:** import [GeoNames alternateNamesV2](https://download.geonames.org/export/dump/) when needed, retaining original name, language code, preferred/historic/colloquial flags. Do not assume untagged aliases are Arabic or modern. Fallback selected language → local name → original; don't rewrite provider names.
- **Small-scale supplement:** [Natural Earth populated places](https://www.naturalearthdata.com/downloads/), with curated capital/city points and SCALERANK; it's a separate source with its own coverage, license and attribution.
- **Optional hosted search:** [GeoNames API](https://www.geonames.org/export/web-services.html) for explicit searches/nearby, with user-owned username, quotas, attribution and safe errors. **Do not send a hosted API call on every camera pan/zoom frame.**
- **Optional licensed adapter:** Esri ArcGIS Data Appliance: World Gazetteer is a separately licensed **locator**, not an unrestricted map-label layer. Do not copy or bundle Esri .loc/.loz/services. An organization's authorized Enterprise endpoint is a distinct option. Our local reference layer is an independent implementation from open datasets.

## UI contract: layer versus search

\`\`\`text
Search Bar  -> GazetteerSearchService -> Local names index / permitted remote adapter

Layers
  Reference Layers
    [ ] World Gazetteer                   (independent layer toggle)
         Language: Auto | Arabic | English | Local
         Categories: Countries | Regions | Cities | Villages | Terrain | Water
         Detail: Automatic | Fewer | More
         Scale limits / label size / declutter preferences
\`\`\`

Search must work even if the layer is off. Clicking a label shows a source-tagged tooltip/fly-to, **not** a newly created My Places feature. Explicit Save as Placemark is the only action that copies a result to My Places. Save enabled state, language/categories and density in project settings; never persist the transient viewport buffer or replace KML/KMZ imports. Default disabled on pre-existing projects pending user choice. Never override imported KML/KMZ styles, icon colors or placemark hierarchy.

Suggested normalized model: \`source_id\`, \`place_id\`, \`name_original\`, \`localized_names[{lang,value,historic,preferred}]\`, \`lat\`, \`lon\`, \`feature_class\`, \`feature_code\`, \`country_code\`, \`admin1..4\`, \`population?\`, \`importance\`, \`license\`, \`dataset_date\`. GeoJSON coordinates are **[lon,lat]**. Named river/lake/mountain points do **not** supply a river polyline/lake boundary. Quality/confidence and same-name disambiguation are necessary.

## Proposed label level of detail: configurable examples only

| Current *2D map-scale denominator* | Visible categories |
| --- | --- |
| Less detailed than 1:20,000,000 | Country names and primary capitals |
| 1:20,000,000 to 1:5,000,000 | Provincial capitals and major cities |
| 1:5,000,000 to 1:1,000,000 | Secondary towns, prominent water/terrain |
| 1:1,000,000 to 1:250,000 | Towns, villages and selected wadis, mountains and local water |
| More detailed than 1:250,000 | Smaller settlements/local named features |

Do not treat these numbers as official Esri/GeoNames thresholds. For a 2D map derive scale from view resolution and display DPI/ground latitude as appropriate; for a **3D globe**, camera altitude alone is not a rigorous scale. Use visible tiles, frustum bounds, globe horizon/occlusion and screen-space projection for final decisions.

**Render algorithm:** (1) enabled check; (2) camera-settled callback/debounce/cancel stale jobs; (3) derive visible bbox/tiles with dateline and polar handling; (4) indexed local spatial query with margin, feature filters, zoom/priority tiers and bounded candidate count; (5) resolve language and source attribution, avoid false name-based deduplication; (6) project label anchors to screen, cull behind horizon and outside view, prioritize by feature codes/admin/population, apply font-measured pixel collision and density budget; (7) place independently styled labels in their own reference data source. Use hysteresis/fade to reduce flicker, and safe async cancellation to keep navigation smooth. Cache by tile/LOD/language/source revision; invalidate on data/language/CRS changes. Offline works from the local DB.

**ZEarth Desktop integration interfaces:**
\`\`\`text
SearchBox -> GazetteerSearchService -> GazetteerStore
ReferenceLayers.WorldGazetteer.Toggle -> GazetteerLayerController
CameraSettled -> VisibleExtent/LevelOfDetail -> cancelable GazetteerQueryWorker
QueryWorker -> language selection + screen project/declutter -> CesiumJS label renderer
ProjectState -> visibility/categories/language/thresholds (not viewport cache)
\`\`\`
For WPF/WebView2, isolate data/query/service code from the JavaScript renderer/bridge. For ArcGIS Pro and QGIS provide host-native label visibility rules; do not reuse incorrect 3D altitude thresholds as a 2D scale. APIs are optional backends, not prerequisites to offline display.

## Acceptance checklist (T0–T4 evidence)

- [ ] Local country import and worldwide chunked/Tile/RTree index proven; no API key required for offline layer.
- [ ] Search and layer are **independent**; layer toggle works, project save/reopen persists preference.
- [ ] Zoom out hides minor places, zoom in adds detail; Nile Delta density readable, no label swarm.
- [ ] Arabic and English selected labels, font shaping/RTL, aliases, historic flags and homonym disambiguation tested.
- [ ] Dateline, poles, 2D/3D globe horizon, viewport cancellation, missing coverage and offline error paths tested.
- [ ] Source credits/date/license visible; no license-prohibited Esri redistribution, no secret in browser.
- [ ] Imported KML/KMZ styles, feature names and My Places unchanged.
- [ ] Unit tests ≠ working ZEarth UI: verify installed app interaction (T2–T4) before calling integration complete.

The [stdlib proof of concept](../examples/gazetteer/viewport_labels.py) implements only base names, a 2D bbox, simple SQLite and approximate pixel collisions; it deliberately does **not** claim production-scale indexing, language-tagged alternatives, globe horizon or native application success.

Official sources: [GeoNames license and download](https://www.geonames.org/export/), [GeoNames API](https://www.geonames.org/export/web-services.html), [Natural Earth](https://www.naturalearthdata.com/downloads/), [Esri World Gazetteer](https://doc.arcgis.com/en/data-appliance/latest/world-gazetteer/use-world-gazetteer.htm).
