# KML/KMZ Style Preservation — mandatory import/export contract

**Rule: PRESERVE IMPORTED STYLES BY DEFAULT.** When an application loads an external KML/KMZ, retain the original document-level and feature-level styling, original icons, labels, line/poly colors and styles, transparency, positioning, dimensions, and folder organization. **Never apply project theme, standard palette, new Maki/Pinhead symbol, or default blue pushpin to imported content automatically.** Source KML/KMZ is authoritative for its presentation.

This guide supports the [KML/KMZ Engineering](../skills/kml-kmz-engineering/SKILL.md) and [Cartography/Symbology](../skills/gis-cartography-symbology/SKILL.md) specialists. See the [separate icon library](map-marker-icon-library.md) for creating **new** markers, not overwriting imports.

## Sharing general GIS icons safely

The [Unified GIS Icon Manager](unified-gis-icon-manager.md) is the one general-purpose symbol library for new point markers in any GIS host. Its [standalone example](../examples/icon-manager/README.md) supports creating new SVG/PNG markers and packaging a **new** KMZ. KML/KMZ has its own export adapter, not a duplicate icon library. For imported documents, `Use Original Icon` remains selected by default; `Choose New Icon` is an explicit reversible action. Even app-theme changes, project loading and lazy rendering must not apply a preset style to an imported source feature.

## Three distinct states, not one style field

| State | Meaning | Persistence |
| --- | --- | --- |
| `imported_source_style` | Parsed original KML style XML, shared styles, StyleMaps and asset IDs from exact imported file | Immutable source/provenance; retain unknown extension XML/resources where feasible |
| `presentation_fallback` | Temporary visible placeholder *only when original resource unavailable/unsupported* | Never change the original style model or save fallback into exported KMZ |
| `explicit_user_override` | New color, icon or thickness selected through real Edit Style command | A reversible, separate edit with original retained for Reset to original |

Import modes: **Preserve original (default)**; *User-chosen temporary preview override*; *Apply to selected features and save as a new file* (explicit). Never treat the last-used symbology, default layer renderer, application theme, or a generic geometry-type style as an explicit user edit.

## Read + render pipeline

1. Verify ZIP magic/limits and securely inspect files in memory or within a confined temp directory. Keep original KMZ bytes or lossless source member mapping. Reject traversal, unsupported/encrypted entries, unsafe paths, ZIP bombs, DTD/external entities and uncontrolled remote links. **Security decisions do not authorize replacing styling.**
2. Load primary KML; resolve namespace, `Document`/`Folder` hierarchy, `Placemark`, shared `<Style id>`, embedded `<Style>`, `<StyleMap>` `normal`/`highlight`, internal `<styleUrl>#id` and permitted relative references **without flattening or silently discarding shared definitions**. If unresolved, retain original ref and issue a diagnostic.
3. Preserve values and semantics for `IconStyle` (`Icon/href`, `color`, `colorMode`, `scale`, `heading`, `hotSpot`), `LabelStyle` (color/scale), `LineStyle` (color/width, applicable `gx` extensions), `PolyStyle` (color/fill/outline), `BalloonStyle`, `ListStyle`, `ScreenOverlay`/`GroundOverlay`/`PhotoOverlay`, and other applicable user-authored extensions. Preserve label text and `visibility`, folders, metadata, temporal context and geometry/altitude independently of icon style.
4. KML colors are **aabbggrr** hex (alpha-blue-green-red), *not* HTML `#rrggbb`. Convert for rendering only in an adapter; retain original KML hex on save. Preserve alpha, transparent polygons and outline flags. Preserve `normal` vs `highlight` (hover/selected) and inline-vs-shared precedence; default app selection glow must be a visual UI overlay, not an edit to the KML document.
5. For KMZ, bind `Icon/href` and overlays to their original relative asset paths; retain filenames and bytes, do not change extension or resize images. Paths are normally relative to the KML root; model texture `sourceHref` can be relative to the model itself. Check against documented KML rules. Do not infer a new path from the machine user's directory. Remote icons or KML `styleUrl` remain referenced (not fetched automatically), showing a warning/temporary placeholder if not available. Preserve remote URI string; do not embed without explicit opt-in and rights review.
6. Preserve rendering semantics for ZEarth/Cesium/Web via an adapter from KML styles to scene billboards, labels, polylines, polygons and overlays. An internal marker library and Cesium PinBuilder are **only** for new features or user-chosen overrides. Do not recolor original imported PNG via default tint or rewrite original point `pixelOffset` / `hotSpot` when display transforms are applied.
7. Save/export to a **new output** by default. When unchanged, prefer the original ZIP bytes/source members or a structurally equivalent repackage; preserve style IDs/references and original media bytes. User edits must be isolated in an edit history and exported only after user confirms replacing the styling for chosen objects. No silent `styleUrl` deletion, unique-value regeneration, flattening, or imported resource loss.
8. Keep a **compatibility/loss report** for unsupported `gx` constructs, external references, unsupported SVG/font CSS, network links, dynamic refresh, Google Earth extensions. **Do not claim pixel-identical rendering across engines**; equivalent KML content and visual preview are distinct verification levels.

## Test matrix / acceptance

- Two imported points with different PNG icons/colors/scales; one line and polygon with different widths, opacity, fill/outline; label text/color/scale.
- Both shared style and inline style; `StyleMap` `normal` and `highlight` with separate resource references; `IconStyle/hotSpot`, rotation and colorMode.
- Multiple folders, nested names, styling per object; missing and external icon, transparent fill, disabled outline, remote `styleUrl` (no fetch), extra supported and unknown namespaces.
- Re-import original KMZ → view → save copy → reopen (in chosen target and, when feasible, Google Earth); compare **resolved styles**, source style XML or equivalent semantic values, same asset SHA-256, same styleUrl links, folder tree, counts, geometry and overlay behavior; record renderer screenshot diffs separately.
- Select **Edit Style** for one feature: only that feature's explicit override changes; Reset to original restores source without affecting siblings.
- Inject missing image or invalid symbol: show a nonpersistent fallback and warning. Do **not** overwrite source with replacement icon.
- Validate custom new point: licensed Maki/Pinhead PNG can be exported in a *new* KMZ with relative href and attribution when necessary, but imports remain untouched.

The offline [KMZ style regression fixture](../examples/kml-style-preservation/README.md) checks style nodes, `styleUrl` and packed assets of two KMZs. It is a **static regression check**, not a full KML standards validator or proof of visual/runtime rendering.

## Core upstream specifications

- [KML reference](https://developers.google.com/kml/documentation/kmlreference) — color byte order, Style/StyleMap, IconStyle, LabelStyle, LineStyle, PolyStyle.
- [KMZ archive and relative links](https://developers.google.com/kml/documentation/kmzarchives) — packaging, image/icon resources and path resolution.
- [OGC KML standard](https://www.ogc.org/standard/kml/) — baseline and interoperability (check version support).
- [Cesium PinBuilder](https://cesium.com/learn/cesiumjs/ref-doc/PinBuilder.html) — new/custom/generated marker appearances only.
