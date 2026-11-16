# Google Earth Pro engineering patterns — curated blog study

Reviewed **2026-10-25**. Primary historical source: [Google Earth Blog](https://www.gearthblog.com/). Use this as a **feature-behavior case study**, not a maintained API, code library or current Google Earth release guide. The blog homepage shows a 2021 post and older substantive product coverage. Independent Google Earth / KML documentation controls current feature and file semantics.

**Status:** T0 documentary research only. No Google Earth Pro, ZEarth, CesiumJS, desktop runtime, or live imagery provider was executed in this review. This reference neither reproduces proprietary source code nor conveys imagery redistribution rights. Google Earth Blog is independent of Google.

## Source-to-feature decisions

| Observed blog example | Independent GIS application requirement | Verification |
| --- | --- | --- |
| [2018 Places panel update](https://www.gearthblog.com/) | Multi-select (Shift/Ctrl), drag/drop between folders, bulk delete with undo and order preservation; keep Temporary Places distinct from saved My Places | Select ten mixed items, move, undo, save/reopen and compare tree |
| [Animating historical imagery with tours (2015)](https://www.gearthblog.com/blog/archives/2015/07/animating-historical-imagery-using-google-earth-tours.html) | Separate camera pose from imagery date; timeline advances among **available dates**, never pretending a scene exists on every calendar step | Keep camera fixed; skip unavailable days; show each acquisition ID and source |
| [Elevation profiles (2015)](https://www.gearthblog.com/blog/archives/2015/04/google-earth-elevation-profiles.html) | Profile for polyline, cumulative ground distance, sampled DEM heights, local grade and cursor-linked map position | Unit/CRS/Z tests, NoData segments, bridge/tunnel warnings, marker synchronization |
| [Tour builder: circling (2017)](https://www.gearthblog.com/blog/archives/2017/03/google-earth-tour-builder-part-1-circling.html) | Placemark-to-placemark fly-to, orbit, dwell time, orientation and deterministic play/pause/stop | Camera path, altitude, interrupted playback and round-trip tests |
| [Tiled maps in Earth (2016)](https://www.gearthblog.com/blog/archives/2016/10/getting-tiled-maps-google-earth.html) | Raster overlays with bounds/opacity/rotation and optional tiled or super-overlay loading by scale | Tile culling, reprojection, alpha edges, attribution, no spurious full-image downloads |
| Google Earth KML network behavior | Region/LOD and NetworkLink with explicit refresh policy, user visibility overrides, bounded remote retrieval | No refresh storms, no unintended requests, offline fallback, graceful broken links |

Related internal guides: [KML/KMZ fidelity](google-kml-kmz-compatibility.md), [symbol preservation](kml-kmz-symbol-preservation.md), [World Gazetteer](world-gazetteer-reference-layer.md), [historical imagery implementation](historical-imagery-timeline.md), [desktop engineering](desktop-application-engineering.md), and [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md).

## Components and boundaries

1. **Places tree:** data model owns stable IDs, folder order, visibility, nesting, and source KML provenance. Renderer must not silently flatten directories or restyle imported features; distinguish transient and explicitly persisted elements.
2. **Layers / reference labels:** basemap, terrain, imported overlays, temporary places and optional World Gazetteer are separate logical systems. Gazetteer is an independently toggled, scale-aware Reference Layer; it is not a side-effect of search results. Cull names by viewport/3D horizon and avoid requests on every camera frame.
3. **NetworkLink loader:** honor KML `Link`, `refreshMode`, `viewRefreshMode`, `viewBoundScale` and `Region/Lod` where supported. Apply domain allowlists or opt-in; timeouts, request caps, backoff, cache headers, URL sanitization, SSRF defenses and loop detection. Never execute network-supplied balloons as active HTML/JS. A disabled parent layer triggers no downloads.
4. **Overlays and tiled imagery:** preserve original `GroundOverlay`, `LatLonBox`, `gx:LatLonQuad`, `ScreenOverlay`, alpha and source URL. Reproject for display, keep original references; permit raster tiles only where provider rights allow.
5. **3D tours:** maintain one canonical camera pose (longitude, latitude, heading, pitch, roll, range/height), separate tour camera from user working camera, stop on explicit user navigation if desired. Preserve unsupported `gx:Tour` primitives for export; never falsely claim playback is implemented.
6. **Profiles:** source terrain samples from a declared legal DEM, not hypothetical access to Google's elevation tiles. Resolve projected path distance in metres and document vertical datum, sampling interval and vertical exaggeration. Crosshair in profile must match position on map.
7. **Project persistence:** save layers, reference-label toggle, timeline provider/selection, camera, network-source consent and My Places. Loading a project must not trigger unchecked network requests; imported KML source documents stay available for faithful export.

## Google Earth and Google Earth Blog are not licensed source engines

- [Google Earth historical imagery documentation](https://developers.google.com/maps/documentation/earth/historical-imagery) covers the modern history UI. Older blog guidance about "desktop-only" features may be outdated.
- [Google KML reference](https://developers.google.com/kml/documentation/kmlreference) is the normative parser/serialization aid for `NetworkLink`, `Region`, `Lod`, `gx:Tour`, camera and time extensions.
- [Google Geo Guidelines](https://about.google/brand-resource-center/products-and-services/geo-guidelines/) do **not** grant rights to embed or bulk download Google Earth imagery in a new standalone GIS product. Build ZEarth or other GIS software using separately authorized imagery/terrain providers and verified terms.
- Do not scrape Google Earth to construct an "imagery provider," copy its interface pixel-for-pixel, or package blog examples without checking their individual license. Design functional equivalents independently.

## Acceptance contract for Google-Earth-like desktop applications

| Feature | Minimum executable acceptance |
| --- | --- |
| Places and Layers | Import mixed styled KML/KMZ; open tree, reorder, toggle, edit copy, save/reopen with equal semantics |
| Navigation | Pan, zoom, orbit, reset north, 2D/3D switch; globe stays visible; camera remains consistent after modal/drawer open/close |
| Timeline | See [dated imagery contract](historical-imagery-timeline.md); no full-screen blocking modal and no invented years |
| World Gazetteer | Toggle independently from search; correctly prioritized bilingual labels across zoom levels |
| Dynamic KML | Refresh only on supported conditions and within bounds; show stale/offline/error states |
| Elevation | Profile derived from explicitly named DEM; map/profile linked; NoData and vertical datum reported |
| Tours | Valid sequenced camera playback, interruption, state restoration and KML export capability report |

**Evidence tiers:** T0 sources, T1 schema/static and fixture-driven checks, T2 observed integration in a named Google Earth/Cesium/target runtime or provider adapter, T3 actual GUI/provider operation plus state/output readback, and T4 a clean packaged Windows executable. Never upgrade the claimed tier without evidence.
