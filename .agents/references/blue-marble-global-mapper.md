# Blue Marble Geographics / Global Mapper engineering references

Reviewed **2026-10-25**. This guide separates the **commercial Global Mapper software and SDK** from public GitHub forks under the company's account and unrelated community GMS scripts. Documentation review is T0 only; no proprietary runtime, licenses, or sample applications were executed.

## Source ownership and licensing

**Official GitHub:** https://github.com/Blue-Marble-Geographics . The inspected geospatial projects there are **forks**, not proof that Blue Marble originally authored them or that the proprietary Global Mapper source is available. Treat each fork as a separate third-party codebase, preserve its upstream license and notices, and check original development repositories/versions.

**Commercial SDK:** [Global Mapper SDK](https://www.bluemarblegeo.com/global-mapper-sdk/) / [download listing](https://www.bluemarblegeo.com/download-global-mapper-sdk/) / [support license terms](https://support.globalmapper.com/hc/en-us/articles/53119789602324-Global-Mapper-SDK). Requires a developer SDK license; separate distributed DLL and some server licensing obligations may apply. A free trial/download does **not** authorize free redistribution. Do not bundle SDK DLLs into our MIT-licensed skill or present it as a free public web API.

**Python in Global Mapper:** [Official Python reference](https://support.globalmapper.com/hc/en-us/articles/53169757119636-Global-Mapper-Python-reference) and [runtime notes](https://support.globalmapper.com/hc/en-us/articles/53119821844628-Python-in-Global-Mapper). The current support page describes Python 3.11.6 installed with Global Mapper, separately recommending Python 3.10 for some external setups; older SDK guides say 3.9. **Pin edition and SDK/application version before selecting Python**, never hardcode one universal version.

**Legacy and current script language:** Global Mapper Script `.gms` is distinct from Python `import globalmapper`; see official [Global Mapper scripting guide](https://www.bluemarblegeo.com/knowledgebase/global-mapper-26/GlobalMapper_ScriptingReference.pdf) and product version-specific reference. Commands include `IMPORT`, `EXPORT_RASTER`, `EXPORT_ELEVATION`, `GENERATE_CONTOURS`, `GENERATE_WATERSHED`, `GENERATE_RIDGE_LINES`, `LIDAR_CLASSIFY`. Availability and exact parameters change by software edition/version; do not emit unverified production scripts.

## Relevant public GitHub forks (all are forked repositories)

| Blue Marble GitHub fork | Upstream | Intended engineering reference | License caution |
| --- | --- | --- | --- |
| [3d-tiles-tools](https://github.com/Blue-Marble-Geographics/3d-tiles-tools) | [CesiumGS/3d-tiles-tools](https://github.com/CesiumGS/3d-tiles-tools) | Tile merging, processing, optimization and 3D Tiles packing | Apache-2.0 metadata, verify notices |
| [cesium-terrain-builder](https://github.com/Blue-Marble-Geographics/cesium-terrain-builder) | [geo-data/cesium-terrain-builder](https://github.com/geo-data/cesium-terrain-builder) | DEM to Cesium heightmap terrain tiles | GitHub NOASSERTION; fork is older and notes `NoData` is not managed |
| [openMVG-bmg](https://github.com/Blue-Marble-Geographics/openMVG-bmg) | [openMVG/openMVG](https://github.com/openMVG/openMVG) | SFM camera poses and sparse 3D points | MPL-2.0 metadata; review modifications |
| [openMVS-bmg](https://github.com/Blue-Marble-Geographics/openMVS-bmg) | [cdcseacave/openMVS](https://github.com/cdcseacave/openMVS) | Dense clouds, meshing and texture reconstruction | AGPL-3.0 metadata; legal obligations if reusing code |
| [cgal](https://github.com/Blue-Marble-Geographics/cgal) | [CGAL/cgal](https://github.com/CGAL/cgal) | Robust computational geometry and mesh processing | Mixed package-level GPL/LGPL/commercial possible |
| [tinygltf](https://github.com/Blue-Marble-Geographics/tinygltf) | [syoyo/tinygltf](https://github.com/syoyo/tinygltf) | glTF/GLB model input/output | MIT metadata; also check bundled third-party notices |
| [assimp_bmg](https://github.com/Blue-Marble-Geographics/assimp_bmg) | [assimp/assimp](https://github.com/assimp/assimp) | Read many 3D model formats | License detector NOASSERTION; confirm upstream notices |

**Independent community tutorial:** [PeterSanctus/GM-Scripting](https://github.com/PeterSanctus/GM-Scripting), GPL-3.0, Notepad++ highlighting, example raster imports/exports, polygon crop and batch conversion; **not an official Blue Marble repository**. Its last inspected updates are old, so validate names and options on your installed version. Do not copy third-party `.gms` files without respecting GPL.

## Decision table

| User task | Recommended specialist | Key prerequisite / caveat |
| --- | --- | --- |
| Develop .gms automation in Global Mapper | [Global Mapper Development](../skills/global-mapper-development/SKILL.md) | Installed Global Mapper, edition/license/version and official syntax |
| Python scripts inside app or Global Mapper SDK | [Global Mapper Development](../skills/global-mapper-development/SKILL.md) | Exact Python runtime and proprietary SDK licensing |
| Build a standalone alternative application | [Desktop GIS](../skills/gis-desktop-application-development/SKILL.md) | Use independently licensed GDAL/PROJ/other engines, not proprietary GM DLL |
| Generate DEM-derived terrain/stream/contour outputs | [Terrain/Hydrology/LiDAR](../skills/terrain-hydrology-lidar/SKILL.md) | Verify Z datum/NoData and source accuracy |
| Render 3D Tiles and glTF/GLB in a globe | [Geospatial Web Mapping](../skills/geospatial-web-mapping/SKILL.md) | Verify Cesium/plugin versions, transforms, CRS and actual tileset outputs |
| UAV photogrammetry and 3D meshes | [Terrain/Hydrology/LiDAR](../skills/terrain-hydrology-lidar/SKILL.md) | openMVG/OpenMVS are third-party forks, choose license-compatible implementation |

## Acceptance and integration controls

1. Obtain host edition, installed version, Windows architecture, available Python runtime, SDK license and input files; protect originals.
2. Prove a real `.gms` or `.py` operation in licensed Global Mapper with an output placed in a new folder; inspect log, feature count, CRS, units and exported file by re-opening it.
3. For terrain tiles, test valid output URLs/tiling scheme and NoData before placing them into CesiumJS. The terrain builder fork does **not** serve tiles: a separate server may be needed.
4. For photogrammetry, capture camera model, overlap, ground control, processing algorithm, georegistration/scale and true DEM/orthomosaic accuracy.
5. Report T0–T4 evidence truthfully. Catalog/README inspection is not proof of functional Global Mapper scripting, CLI, licensed SDK, or deployable installer.

**No proprietary Blue Marble source code or SDK binary is mirrored or shipped in GIS Development Skills.**
