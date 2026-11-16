# Historical Imagery Timeline — independent GIS implementation contract

Reviewed **2026-10-25**. Design inspiration: [Google Earth Blog: animating historical imagery (2015)](https://www.gearthblog.com/blog/archives/2015/07/animating-historical-imagery-using-google-earth-tours.html). Current feature behavior: [Google Earth historical imagery help](https://developers.google.com/maps/documentation/earth/historical-imagery). This is a specification for new implementations (especially WPF/WebView2 + CesiumJS ZEarth), **not proof of an implemented timeline**.

## Required UI

**Do not replace the globe with an opaque imagery dialog.** Show a narrow dockable/collapsible timeline anchored over or below the map, with previous/next available dates, play/pause, time range, playback speed, current **actual** scene date and provider/credit. A compact floating chip remains when collapsed. Preserve camera, elevation and active layers on open/close. The time control stays keyboard- and screen-reader-accessible, with Arabic RTL and English LTR labels.

A two-date compare mode offers swipe, split or opacity blend with one registered viewport/camera; prevent differing source resolution or georegistration from being presented as a change signal without warning.

## Data model and provider contract

```text
ImageryProvider
  id, name, accessPolicy, attribution, termsUrl, coverageHints
  discoverScenes(aoi, timeWindow, filters, abortSignal) -> SceneSummary[]
  resolvePreview(sceneId, aoi, size, projection, abortSignal) -> LicensedTileOrRaster

SceneSummary
  stableId, providerId, productOrCollection, acquisitionStartUtc, acquisitionEndUtc?
  isMosaic, mosaicTimeWindow?, cloudCover?, footprint?, spatialResolutionMeters?
  crs, bandsOrComposite, qualityFlags[], licenseRef, attribution, previewCapabilities
```

Do **not** conflate: actual sensor acquisition date, date a mosaic was compiled, service processing date, the UI clock and device time. A mosaic can contain pixels from multiple dates; show collection date/range and date uncertainty. Historical imagery is **not universally available for any selected day**: show only discovered scenes; if a day is absent say "no imagery for this date" instead of displaying a newer image with the old date label.

Provider availability changes by AOI, resolution, orbit and date. Recompute valid ticks when pan/zoom changes, but debounce queries and retain/cancel results by request generation ID to avoid stale responses.

## Timeline state machine

```text
closed -> opening -> discovering -> ready
ready -> selecting -> loading -> ready | missing | error
ready -> playing -> loading -> playing | paused | stopped
any -> offline | provider-auth-required | rate-limited
```

- Selecting a date snaps to a **discovered acquisition**, not an interpolated calendar year. Show ISO local display with UTC storage; no implicit timezone conversion of satellite acquisition.
- Play advances among discovered scenes and can use per-frame dwell time; stop/abort in-flight requests when user drags the timeline or closes it. Disable unavailable navigation rather than cycling to phantom dates.
- Cache only as allowed by the provider; memory budget, raster disposal, zoom transitions and low-RAM fallback are mandatory.
- With no scenes, preserve basemap/terrain, explain why, offer other legal sources or broader date ranges; **never show a blank globe**.
- On reopen, restore time window, selected scene ID, compare IDs, display opacity, provider and original camera. If selected IDs no longer resolve, retain request with a warning and offer nearest available date without silently substituting it.

## Source choice: legal and technically honest

| Source class | Appropriate function | Boundary |
| --- | --- | --- |
| [NASA GIBS](https://nasa-gibs.github.io/gibs-api-docs/) | Date-aware visual layers with time-dependent WMS/WMTS support where exposed | Rendering tiles != calibrated raw sensor pixels; query capabilities, dates, resolution and attribution |
| [Copernicus Data Space STAC](https://documentation.dataspace.copernicus.eu/APIs/STAC.html) | Search supported Sentinel acquisitions/metadata | STAC discovery != imagery download; collections, auth and rights vary |
| [Copernicus Sentinel Hub Process API](https://documentation.dataspace.copernicus.eu/APIs/SentinelHub/Process.html) | Authorized image processing/preview for a chosen AOI/time | Auth, quota/processing cost, cloud filter, evalscript and license must be validated |
| [USGS Landsat data](https://www.usgs.gov/landsat-missions/landsat-data-access) | Longer Landsat archive with scene metadata and separately licensed delivery | Landsat pixel sizes and processing/coverage differ from high-resolution commercial imagery |
| User-supplied GeoTIFF/COG/KML GroundOverlay | User's own time-tagged raster series | Respect per-item CRS, extent, timestamps and original KML/KMZ resources |

**Hard boundary:** Google Earth historical satellite imagery is **not** an automatically free downloadable basemap API for ZEarth. Do not scrape, embed, impersonate or cache proprietary Google imagery contrary to Google terms. A provider's imagery display and its STAC catalog might require entirely different endpoints or credentials.

## ZEarth integration seam

- `WPF TimelineViewModel` owns button events, selected time interval, accessibility and progress; it never opens a blocking full-map dialog.
- `GlobeBridge` passes typed messages to CesiumJS: `timeline.open`, `timeline.setScene`, `timeline.compare`, `timeline.close` and cancel requests. An acknowledgment includes success/error and last rendered scene ID; UI must not announce success on command dispatch alone.
- `ImageryRegistry` owns provider metadata/licensing/auth and date-aware sources independently of basemap and terrain.
- `ProjectState` persists scene IDs/source metadata, provider and camera while respecting saved My Places/Temporary Places boundaries.
- Keep current user camera unless explicitly choosing "fly to coverage"; no automatic zoom, pitch reset or globe blanking.

## Runnable acceptance scenarios (T2/T3/T4)

1. **Sparse dates:** fixture offers 2001-01-01 and 2013-05-17 only. Tick list has exactly two available dates; next skips missing years; never calls an imaginary 2005 service.
2. **No coverage:** request over AOI with zero scenes; globe remains visible; "no imagery" is explicit; user can close/collapse timeline.
3. **Camera persistence:** open timeline, compare dates and collapse; longitude/latitude/altitude/heading/pitch and basemap are unchanged within documented tolerances.
4. **Stale replies:** issue requests A then B; B resolves first; late A must not overwrite B. Cancellation disposes stale resources.
5. **Missing provider/auth:** 401/403/404/429, timeouts, NoData, offline and quota exhaustion each show distinguishable recoverable states.
6. **Real provenance:** click "scene details": provider, collection/product, scene ID, actual acquisition time/range, mosaic caveat, resolution, CRS and attribution.
7. **Compare:** swipe, opacity and split reveal same registered extent; mismatched footprint/resolution is flagged.
8. **A11y/i18n:** keyboard previous/next/play/pause, EN/AR labels and RTL are readable; dates are not reversed by bidi rendering.
9. **Packaged application:** click every ZEarth timeline ribbon control in an installed clean Windows build, then save/reopen project. Static web rendering alone is not certification.

## Roadmap

P0: nonblocking, dockable timeline and provider registry; actual availability and honest missing dates. P1: accessible playback and project persistence. P2: two-date swipe/split and custom user overlays. P3: optional imagery export only where provider license and pixel provenance authorize it.

Cross-references: [Google Earth Pro patterns](google-earth-pro-engineering.md), [Earth View architecture](earth-view-architecture.md), [World Gazetteer](world-gazetteer-reference-layer.md), [KML/KMZ source retention](google-kml-kmz-compatibility.md).
