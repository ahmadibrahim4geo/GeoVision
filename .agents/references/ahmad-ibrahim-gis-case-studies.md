# Real-world GIS engineering case studies — Ahmad Ibrahim

Reviewed: 2026-10-24 | Source: public repositories under https://github.com/ahmadibrahim4geo

These are **author-owned projects linked as implementation and QA references**. The GIS Development Skills repository does not copy their programs, datasets, proprietary styles, or executables. A repository's README and even passing headless tests are not proof that every claimed feature runs in a licensed GIS installation.

## 1. MIRSAD — ArcGIS toolboxes and an independent Windows desktop application

- Repository: https://github.com/ahmadibrahim4geo/MIRSAD
- License: MIT for the original repository code; third-party data, basemaps, models and ArcGIS itself keep independent terms.
- Toolbox entry points:
  - `Toolboxes/MIRSAD_Climate_Atlas_Generator_10_8.pyt`: point climate atlas, bilingual.
  - `Toolboxes/MIRSAD_Weather_Stations_Atlas_Generator.pyt`: weather station selection and outputs.
  - `Toolboxes/MIRSAD_Grid_Raster_Climate_Atlas_Generator.pyt`: independent regional grid/raster pipeline; the main raster engine is `Toolboxes/raster_atlas_generator.py`.
- Common support package: `mirsad_core/` (sources, daily climate, calculation, Excel exports, ArcGIS environment safeguards). **Do not distribute an isolated .pyt without its required sibling packages.**
- Independent application: `MIRSAD Desktop/`, with Python + PySide6/Qt source, separate build environment, tests and developer guides. Its own source is bundled at build time; the released runtime does not depend on ArcGIS.
- State: development release `0.4.0.dev8` available; **no approved stable desktop release**. Later source modifications are not necessarily in the existing installer. Consult `MIRSAD Desktop/docs/STATUS.md` for exact scope.
- Evidence: `Documents/TESTING.md` differentiates headless ArcPy doubles, scientific tests, licensed ArcGIS Pro validation and incomplete ArcMap acceptance. `.github/workflows/checks.yml` runs Windows deterministic source and Desktop tests. **Passing this CI does not prove native ArcPy execution.**
- Engineering lessons: single shared calculation core, adapter boundaries, environment restoration even on failure, independent monthly/seasonal/annual outputs, provenance, Arabic/English help, real XLSX/GPKG/FileGDB readback, and separate HG versus FAO-56 PM ET products.

## 2. CreateFlowArrow — linear-network geometry and QA/QC

- Repository: https://github.com/ahmadibrahim4geo/Create-FlowArrow-Tool | License: MIT
- Inspect `CreateFlowArrow.pyt` and `CreateFlowArrow.py`, the user manual and supplied training data (`data/`).
- Described functions: line tangent and midpoint arrow geometry (polygon or polyline), manhole endpoint matching, invert elevation/fall/slope fields, network topology checks and a dedicated `*_QA_Notes` output.
- Important **safety gate**: user-selectable auto-flip changes the input network **in place**, potentially swapping invert attributes. A derived tool or agent MUST keep audit/read-only as the default, require explicit approval and make a backup or transactional copy before any mutation.
- The repository README advertises ArcGIS Pro 2.x/3.x and ArcMap 10.1+ dual runtime. Confirm the versions, encodings and installed ArcPy bindings on real fixtures; there is no evidence here of a CI-native runtime pass for all these versions.
- Test against: straight and curved pipes; digitized uphill/normal direction; mismatched start/end invert values; disconnected nodes; coincident/tolerance-boundary vertices; projected metric CRS; Unicode paths; empty/invalid geometry; no-point layer; auto-flip opt-in; undo/recovery; output attributes and arrow orientation.

## 3. GeologicalCrossSectionToolbox — geology, DEM and multi-format export

- Repository: https://github.com/ahmadibrahim4geo/GeologicalCrossSectionToolbox | License: MIT
- Entry point: `GeologicalCrossSectionToolbox.pyt`; analysis implementation: `geo_cross_section_engine.py`; `requirements.txt`, bilingual HTML/Word user guides and `Data_Test/` fixture data.
- Documented tasks: sampling local or online elevation along profile, geological outcrop intersection, surface/subsurface sections, fault display/dip, vertical exaggeration, multi-section layout, and exports to geodatabase, Shapefile, DXF, CSV/Excel, static and interactive HTML.
- **Do not assume online "free DEM" automatically permits redistribution or has a globally uniform vertical datum.** Verify source, sampling accuracy, NoData, connectivity/timeout and rate limits. Test projected distance units and cross-section sign/orientation.
- Test: flat and steep DEM; reversed section endpoints; fault crossing; zero-distance/empty geology; multi-overlap topology; legend pruning; exported DXF numeric scaling; offline HTML opening; ArcGIS layer files (`.lyr` vs `.lyrx`).
- ArcGIS Desktop 10.8 / Pro 2.x and 3.x are documented targets, not evidence that every installation or dependency version has passed.

## 4. Additional, separate engineering tracks

| Project | What to inspect | Integration boundary |
| --- | --- | --- |
| [ArcMap-Style-Collection](https://github.com/ahmadibrahim4geo/ArcMap-Style-Collection) | Cartographic reference files, Arabic terminology, ArcMap `.style` and Pro `.stylx` handling | Third-party Esri/USGS/styles/fonts retain their source licenses and redistribution terms; **do not copy assets** to the skill |
| [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026) | Searchable open data index, URL audit scripts, exported metadata, bilingual browser UI | Indexed URLs are not proof of current availability, dataset access rights, or provider API stability |

## Repeatable GIS acceptance tiers

1. **T0 – Source check:** repository exists, documented entry point and dependencies identified, upstream license reviewed.
2. **T1 – Static/headless:** syntax, mocked ArcPy parameters, pure numerical calculations, schema tests and CI. Clearly label tests using ArcPy doubles as *non-native*.
3. **T2 – Native GIS parameter load:** ArcMap/Pro loads toolbox and exposes correct inputs/defaults without errors. Still not proof that the processing algorithm ran.
4. **T3 – Native end-to-end:** execute each tool with real license and controlled fixture, verify attribute counts, CRS, NoData, output formats, gradients, and numeric expectations.
5. **T4 – Distributed desktop/mobile:** packaged build/installer round-trip, launch from clean system, data and user-file preservation, external dependency checks, and meaningful UI actions.

Record for each result: target app/version, OS/Python, tool path, fixture hash, parameters, execution command, output hashes/checks, logs, pass/fail, and remaining unsupported features. A static GitHub Actions pass in *this reference repository* is **T0/T1 only** and cannot upgrade external projects to T3.

## Practical integration protocol

- Clone the **original** project and follow its README; never lift individual `.pyt` files away from `mirsad_core` or embedded metadata without checking imports.
- Use a tiny synthetic fixture (a few lines/nodes for flow; a simple DEM and formation polygons for sections; a limited station/time series for MIRSAD).
- Do not allow automatic network service use or destructive input edits without clear user consent.
- Validate required licenses and provider API limits; capture provenance and source coordinate systems.
- Keep ArcMap Python 2.7 tests isolated from ArcGIS Pro Python 3; run integration in *both* to claim dual support.
- Cross-check scientific/engineering outputs with independent expected results. The README is a **feature claim**, not a scientific validation report.

The complete programs remain maintained in their respective repos. Please open issues or contribute fixes upstream, not by embedding large upstream copies here.
