# Google KML / KMZ interoperability and preservation contract

Reviewed: 2026-10-25. Source of truth: [Google KML documentation](https://developers.google.com/kml), [KML Reference](https://developers.google.com/kml/documentation/kmlreference), [KMZ files](https://developers.google.com/kml/documentation/kmzarchives), [OGC KML standards 2.2 and 2.3](https://www.ogc.org/standards/kml/). This is an **engineering guide**, not a Google endorsement or evidence of a native application test. Google `gx:` extensions must be negotiated separately from core OGC KML.

## Earth explorer feature integration

For imported Earth documents in a native globe, use the [Google Earth Pro engineering reference](google-earth-pro-engineering.md) to implement Places bulk selection/undo, lazy NetworkLink Region/LOD, overlays and camera tours. Use the [Historical Imagery Timeline contract](historical-imagery-timeline.md) **only for separately sourced, properly dated imagery**; it must not rewrite a KML document's own TimeStamp/TimeSpan or gx:Track timeline. Links with external resources require safety limits and opt-in; no provider metadata or imagery date may be fabricated.

## Non-negotiable import behavior

**Import is not re-styling.** A foreign KML/KMZ remains user-authored content. On open/reopen, preserve original style, hierarchy, geometry, labels, visibility, metadata, time, elevation semantics, camera and resources **as far as the runtime supports them**. Never force the application's default marker/color/line width on features that already specify KML styling. A new style is applied only when requested by the user, and never destructively to the source file.

Three distinct modes:
1. **Open for viewing (lossless source retention):** keep the original bytes and resource manifest, parse a semantic DOM, resolve references, render supported constructs; retain unsupported elements/namespace data for future export. Preserve `<Style>`, `<StyleMap>`, inline styles and `styleUrl` references, including `normal`/`highlight` states.
2. **Edit KML document:** update only the explicitly edited feature or property in a copy-on-write DOM; keep unrelated XML/extension nodes intact. Do not canonicalize/reorder entire documents unnecessarily. A byte-identical export is feasible only for fully untouched content; use semantic equivalence after a deliberate edit.
3. **Convert to GIS layer:** warn that a feature-table conversion can lose folder grouping, shared style references, overlays, tours, dynamic links, altitude/time and unknown XML. Produce a structured **loss report**. Never label this mode a faithful KML round-trip.

Prioritize the application sequence **open -> render -> toggle folder visibility -> inspect balloon -> edit on copy -> save KML/KMZ -> reopen -> compare**. If a renderer cannot faithfully show an item, preserve source and surface a warning instead of fabricating a replacement.

## Support matrix — define per host, do not assume

| Construct | Parser/store | Display behavior to implement | Export / test |
| --- | --- | --- | --- |
| `Document`, `Folder`, `Placemark`, `name`, `visibility`, `open`, `id` | Preserve order, folder tree, node IDs | Explorer tree, child visibility, selection | Identical hierarchy/flags/IDs unless edited |
| `Point`, `LineString`, `LinearRing`, `Polygon` (holes), `MultiGeometry` | WGS84 lon,lat[,alt], coordinates and topology | 2D/3D geometry; no swapped axes | Coordinates and polygon rings survive |
| `Style`, `StyleMap`, `styleUrl`, `IconStyle`, `LabelStyle`, `LineStyle`, `PolyStyle`, `BalloonStyle`, `ListStyle` | Resolve shared/inline styles, normal vs highlight | Symbol, label, size, line width, polygon fill and outline, opacity, balloon/list behavior | Compare resolved effective style *and* original references |
| `Icon`, `hotSpot`, resources | Preserve URL and relative path, scale, anchor, rotation | Correct image location and pixel anchor | Keep bundled assets and links; no replacement marker |
| `description`, `Snippet`, `ExtendedData`, `Schema`, `SchemaData` | Preserve mixed text, CDATA, custom XML and attributes | Escape/sanitize balloon HTML; preserve meaningful content | Reopen without dropped fields, typed data or extensions |
| `GroundOverlay`, `ScreenOverlay`, `PhotoOverlay` | Preserve raster resource, geo bounds, quad/rotation, positioning | Render only when available; mark unsupported | Export imagery and overlay metadata |
| `TimeStamp`, `TimeSpan`, `gx:Track`, `gx:MultiTrack` | Keep temporal instants, ranges, ordered pairs | Timeline/animation where implemented, otherwise warn | Preserve actual times and ordered track positions |
| `Camera`, `LookAt`, `Region`, `Lod`, `NetworkLink` | Keep camera, hierarchy, region bounds, URL and refresh config | Viewpoint/LOD/remote links only after safe policy checks | Save config; do not silently fetch or strip |
| `Model` / COLLADA, `gx:Tour`, `gx:AnimatedUpdate`, unknown extension nodes | Keep asset/dependency graph and source XML | Explicitly report unsupported 3D/tour behavior | Pass through unsupported XML where safe and structurally possible |

**Scope:** OGC KML 2.2 baseline with documented 2.3 and Google `gx` differences. Implement only features actually supported by tested host versions. ArcGIS Pro, QGIS, CesiumJS and Google Earth do not guarantee identical interpretation of every element.

## Exact styling rules

- KML `<color>` uses **`aabbggrr` hex byte order**, unlike common web CSS `#rrggbb`. Example `ff0000ff` is **fully opaque red**; `7fff0000` is **half-transparent blue**. Parse alpha, blue, green, red separately; do not swap channels. Distinguish KML `colorMode=random` from deterministic color rendering.
- Apply inheritance and override resolution correctly: shared `styleUrl`, inline `Style`, `StyleMap` normal/highlight pairs; preserve unrecognized child style properties. Resolve `#id` only in the correct document/package context and handle missing/cyclic references gracefully.
- `IconStyle`: keep URL, size/`scale`, heading, `hotSpot` x/y units (fraction, pixels, insetPixels), original icon image and alpha. Offline missing external icons remain **unresolved** with a visible warning, not a silent swap to app-specific imagery.
- `LabelStyle`, `LineStyle`, `PolyStyle`: preserve labels, width units, fill/outline switches, random color mode, transparency, and highlight state. Keep user-applied manual styles separate from untouched imported styles.
- `BalloonStyle`: treat description HTML/links as **untrusted**. Sanitize against scripts, dangerous URLs and event attributes; rendering must not grant file access or arbitrary code execution. Preserve the raw stored description when exporting the unchanged document.

## Geometry, height, time and view state

- KML coordinates are longitude,latitude[,altitude] in WGS84; altitude is in meters where applicable. Preserve all original coordinates, altitude modes, `tessellate`, `extrude`, and dateline-crossing geometry. Reproject **only for display**; do not overwrite stored WGS84 coordinates on import.
- Distinguish `clampToGround`, `relativeToGround`, `absolute` and Google extension `gx:clampToSeaFloor`/`gx:relativeToSeaFloor`. Different DEM/vertical-datum or seafloor sources can alter visual height; retain source semantics even if the host can't reproduce the appearance.
- Keep `TimeStamp`, `TimeSpan`, `gx:Track`, `gx:MultiTrack` and `gx:Tour` intact. Do not equate scene clock with device-local clock. Keep `Camera` and `LookAt` view states as document attributes, distinct from the application's working camera.
- For `Region`/`Lod` and Super-Overlays, use lazy loading/culling only after respecting spatial/zoom conditions. Do not flatten large dynamic hierarchies into memory as a default.

## KMZ packing, links and threat model

- Preferred portable archive layout: a single entry-point `doc.kml` at the archive root and asset subfolders such as `files/icons/`, `files/images/`, `files/models/`. Google's guidance permits other root KML filenames but recommends one entry-point file; additional nested KML via explicit references are advanced cases.
- Maintain a manifest `archive path -> original byte hash -> MIME/source URI -> reference count`. Resolve local relative paths against the KML file location without archive escapes; use special handling for COLLADA `ResourceMap/Alias/sourceHref` paths relative to the model.
- Validate ZIP central directory, entry count, expanded size, individual entries, compression ratios, path normalization including percent decoding, duplicate/case-clashing names, symlinks, and path traversal before extraction. Cap resources and disallow `file:`, script URLs, localhost/private-address SSRF and redirects to forbidden destinations.
- Never auto-follow `NetworkLink` or download image/model URLs on import; provide per-source opt-in, clear network/refresh state, timeouts, retry budgets, quotas and offline fallback. Treat refresh cycles and untrusted description links carefully.
- For a changed KMZ, package edited `doc.kml` plus required local assets; preserve previously referenced unmodified assets. Never point to an absolute path on the developer's PC or embed secrets.

## Architecture for ZEarth Desktop and other products

```text
File / remote input
  -> untrusted file gates (XML/ZIP quotas, namespaces, URL policy)
  -> KML document model + immutable original + asset manifest
  -> style resolver (shared/inline/normal/highlight)
  -> adapters: Explorer folders | 2D features | 3D objects | timeline | overlays
  -> explicit copy-on-write edits / per-item unsupported warnings
  -> semantic comparison + KML/KMZ exporter -> reopen test
```

- Keep **My Places** persistence distinct from **Temporary Places**. Import never mutates an original; add imported files to temporary state first unless user explicitly saves them. Respect per-file folder order, checkboxes, names and selected camera. Exporting a project is different from exporting a portable KML/KMZ.
- Adapter contract should expose `parseCapabilities`, `renderCapabilities`, `exportCapabilities` independently. Use the target engine's tested capabilities; Cesium KML loader support is not universal `gx` coverage. Provide source-preserving fallback for unknown nodes.
- Snapshot the original `styleUrl`, effective style, assets, raw unsupported extension nodes, selected/unselected state, and `inputHash` before editing. Prefer reversible changes and accessible UI warnings.
- Do not distribute copyrighted Google icons merely because a user's source KML refers to them. A user's embedded assets remain theirs, subject to original reuse rights.

## Acceptance and regression gates

Use [the offline styled KML fixture](../examples/kml-kmz/styled-roundtrip.kml) and [its smoke test](../tests/test_kml_preservation.py), then add independently licensed real-world user-provided test KMZ archives.
1. Untouched re-save: compare SHA-256 of original bytes when no serialization is required; otherwise compare semantic XML including unknown nodes and package assets.
2. Changed copy: verify intended edit only; original remains untouched. Import/re-export/reimport does not reassign style IDs, change color channels or strip `ExtendedData`.
3. Visual cross-host: record screenshots of original file in Google Earth and target product for point/line/polygon, transparency, anchors, highlight, overlays and labels. Differences are issues, not proof of fidelity.
4. Safety: malicious XML (DTD/entities), archive traversal/bombs, remote links, HTML XSS, missing resources; rejection without crash or requests.
5. Performance: deeply nested folders, large points/track datasets, NetworkLinks, overlays, cancellation, progress and memory budgets.
6. Evidence report records input hash, output hash, parse/render/export support, counts, warnings, exact versions and T0–T4 host-test level. The included offline fixture verifies **documentation and XML invariants only**, not desktop rendering or Google Earth equivalence.

## Original sources

- [Google KML documentation index](https://developers.google.com/kml/documentation)
- [KML element reference](https://developers.google.com/kml/documentation/kmlreference)
- [Google KML tutorial](https://developers.google.com/kml/documentation/kml_tut)
- [KMZ archives, packaging and linked resources](https://developers.google.com/kml/documentation/kmzarchives)
- [ExtendedData and typed schemas](https://developers.google.com/kml/documentation/extendeddata)
- [Time and animation](https://developers.google.com/kml/documentation/time)
- [Regions, LOD and super-overlays](https://developers.google.com/kml/documentation/regions)
- [Altitude modes](https://developers.google.com/kml/documentation/altitudemode)
- [Touring and Google gx namespace](https://developers.google.com/kml/documentation/touring)
- [Open Geospatial Consortium: KML 2.2 / 2.3 and schemas](https://www.ogc.org/standards/kml/)
