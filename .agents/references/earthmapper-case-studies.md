# EarthMapper repository disambiguation — reviewed 2026-10-24

These projects share a name but address very different GIS needs. Match by **exact repository owner**.

| Repository | What it actually describes | Upstream material checked | How the skill should use it |
|---|---|---|---|
| [AliAlkazraji/EarthMapper](https://github.com/AliAlkazraji/EarthMapper) | Non-commercial Windows desktop mapping product; README describes editing, terrain, imagery, 2D/3D maps and local GeoAI | GitHub root has **only `README.md`**; no published implementation or LICENSE file seen | **Feature and architecture inspiration only**; write our own code from documented upstream open components, never imply its source is available |
| [HIT-SIRS/EarthMapper](https://github.com/HIT-SIRS/EarthMapper) | Research framework for bidirectional satellite–map translation, arXiv:2504.19432 | Root has README, LICENSE (Apache-2.0), and `assets/` only; **no model implementation or pretrained checkpoints seen** | Research citation and method reference, not a ready GIS dependency |
| [rmkemker/EarthMapper](https://github.com/rmkemker/EarthMapper) | Older multispectral/hyperspectral segmentation/classification work | Listed separately in catalog | Historical research reference requiring dependency audit |

**Do not merge, rename or conflate these projects.** A published research article, image, product overview, executable download and source-code library are different deliverables. Availability and project claims should be re-checked before every new release of the skill.

## AliAlkazraji/EarthMapper — features to learn from

Its README describes a .NET/WPF Windows application using WebView2 and mapping frameworks. Its *reported* feature inventory includes 2D/3D map views, vector editing, geometrical constraints, terrain/elevation profiles and DEMs, Sentinel-2 scene discovery and export, historical imagery, map grids, raster styling, and local GeoAI. These features are **product statements** rather than verified implementations here.

When designing an independent app, express each idea as an acceptance test:

1. Open SHP/GPKG/KML/GeoTIFF, retain CRS/attributes, and show errors for unsupported input.
2. Make 2D and 3D commands perform observable camera/map changes.
3. Apply geometry edits on a copy, prove undo/redo and snap tolerance; test metric units.
4. Compare a terrain profile against a source DEM with explicit vertical units and nodata handling.
5. Use STAC catalogs and licensed tile/imagery endpoints, not undocumented scraping.
6. Convert AI masks/detections into spatial outputs with validated pixel-to-ground transform.
7. Verify licensing for **every external service**, dataset, model weight and library.

**Useful upstream component links:** [NetTopologySuite](https://github.com/NetTopologySuite/NetTopologySuite), [ProjNET](https://github.com/NetTopologySuite/ProjNet4GeoAPI), [WebView2Samples](https://github.com/MicrosoftEdge/WebView2Samples), [LibTiff.NET](https://github.com/BitMiracle/libtiff.net), [MapLibre](https://github.com/maplibre/maplibre-gl-js), [ONNX Runtime](https://github.com/microsoft/onnxruntime), [PySTAC Client](https://github.com/stac-utils/pystac-client).

## HIT-SIRS/EarthMapper — research use only

README/paper: *EarthMapper: Visual Autoregressive Models for Controllable Bidirectional Satellite–Map Translation*. It describes geographically conditioned satellite-to-map and map-to-satellite generation, with the CNSatMap dataset of 302,132 aligned pairs across 38 cities (as claimed in the README). Do **not** treat synthetic outputs as authoritative mapping data.

If integrating this research later, first verify whether code/weights/data are **actually released** and licensed separately. Evaluate test-area transferability, geolocation accuracy, consistency across scales, generation artifacts, and agreement with ground-truth vector geometries. For generated maps, an attractive image is not a valid GIS feature layer unless validated and georeferenced.

## Licensing distinction

AliAlkazraji/EarthMapper describes a free non-commercial app, **not an openly licensed codebase**. HIT-SIRS/EarthMapper contains Apache-2.0 LICENSE text but no executable implementation in the checked repository root. Project README claims, hosted services and AI weights may be subject to their own terms. Our catalog only **links** to third-party work.