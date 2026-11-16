# GIS Development Skills — Technical Audit

**Date:** 2026-10-24  
**Scope:** Repository source tree on `main`: installable agent skills, catalog schema/tools, GIS templates, docs, GitHub Actions, and static bilingual site.  
**Method:** GitHub source/CI inspection, Agent Skills specification review, existing tests and added negative regression tests. No licensed ArcGIS/QGIS, real browser or Windows desktop UI session was available to the reviewer.

## Executive result

The repository contains valuable curated source references and practical developer workflows, but an **offline CI pass does not certify third-party runtime behavior**. The update strengthens verifiable skill installation metadata, cross-file integrity and the overlooked standalone Desktop development pathway.

### Addressed in this update

| Priority | Finding | Action | Proof |
| --- | --- | --- | --- |
| P0 | No repository-wide validation of `SKILL.md` names, description length or local references | Add `scripts/validate_skill_pack.py` and negative-case unit tests; enforce in Actions | `python scripts/validate_skill_pack.py` |
| P0 | Documentation references and site inventory could drift from real files | Check README, reference guides, site assets and project/API/skill counters against files and catalogs | `tests/test_skill_pack.py` |
| P1 | `AGENTS.md` had an obsolete instruction implying only one skill despite specialist skills | Clarify general router versus distinct specialist modules, and version/test/QA requirements | `AGENTS.md` |
| P1 | No focused end-to-end skill for independent GIS Desktop apps or toolbox-to-desktop migration | Add `gis-desktop-application-development` with concrete state/I-O/UI/installer acceptance and desktop engineering guide | `skills/gis-desktop-application-development/SKILL.md`, `references/desktop-application-engineering.md` |
| P1 | CI compiled only two of four templates | Include ArcGIS Pro `.pyt` and QGIS example in Python syntax checks (without claiming host integration) | `.github/workflows/validate.yml` |
| P2 | User-facing counters and new skill references were not centralized in assertions | Check counts against JSON and discovered `SKILL.md` paths; update EN/AR README and site | Local validator and landing-page tests |
| P2 | README duplicated the tagline already contained in the approved logo | Remove redundant subtitle while preserving the exact mission sentence and branded assets | `README.md`, `README.ar.md` |
| P2 | “Release” style labels could imply a published GitHub Release for a documentation revision | Use a **Documentation version** indicator unless a tag/release is actually published | Release page and `README.md` |

## Existing strengths retained

- ArcMap 10.x Python 2.7 vs ArcGIS Pro Python 3 compatibility is explained without mixing `arcpy.mapping` and `arcpy.mp`.
- Coordinate-system instructions distinguish WKID, latestWkid, axis order, vertical datum, datum-grid availability and official Esri group names.
- Data/API index explicitly separates no-key, personal-key, commercial and unavailable services.
- External repositories are source pointers; original third-party licenses and service permissions are not assumed.
- Ahmad Ibrahim's MIRSAD, CreateFlowArrow and geological cross-section tools are linked as source-level examples with explicit native-test evidence tiers, not silently copied.
- Unready reference applications are excluded from the curated catalog. No unrelated app repository is modified.
- Static website uses versioned local catalog JSON and bilingual search/filter/RTL; branding and user-authored graphics are preserved.

## Verification commands

Run from repository root in **developer Python 3.11+**, not the ArcMap Python 2.7 runtime:

```bash
python scripts/validate_catalog.py
python scripts/validate_api_catalog.py
python scripts/validate_skill_pack.py
python -m unittest discover -s tests -v
python -m py_compile templates/arcgis-pro-toolbox.pyt templates/arcmap-script-tool.py templates/qgis-processing-algorithm.py templates/standalone-geojson-summary.py
```

Passing these commands provides **repository/static test evidence**. The ability to parse/compile a `.pyt` or QGIS example using general Python does not validate ArcPy/Processing execution or UI behavior.

## Outstanding work (not implied completed)

1. **P0 — Native GIS acceptance:** test ArcMap 10.8/Python 2.7, licensed ArcGIS Pro and QGIS versions separately with actual fixtures and output readback. The CI runner does not have ArcPy or QGIS.
2. **P0 — Standalone Windows acceptance:** clean-install and uninstall MIRSAD Desktop/other apps, actual UI interaction, offline/online I/O, DXF/GPKG/COG and user-file retention. No clean Windows VM was operated by this audit.
3. **P1 — Live API health:** individually check 18 external providers' current policy, auth/quota, coverage, and machine-readable endpoints. Static API schemas are not real-time service checks; avoid brittle web-dependent CI.
4. **P1 — External dependency auditing:** confirm exact upstream commit or version, source license, archived status and required package versions before production adoption; the 104-link catalog is a discovery/reference set.
5. **P1 — Test real skills installation:** independently verify each skill via the supported skills CLI and at least two intended agent clients; repo-wide file checks are necessary but not full client-install acceptance.
6. **P1 — Browser/runtime accessibility:** test GitHub Pages on narrow mobile and desktop browsers, Arabic RTL, keyboard-only navigation, script errors, CSP/CORS, real JSON loading and visual logo sizing.
7. **P2 — Optional reproducible examples:** add small licensed, deterministic DEM/GeoJSON/STAC fixtures and documented expected outputs to enable cross-engine comparisons.
8. **P2 — Formal release governance:** the revision number in README/changelog is not equivalent to a signed or tagged GitHub Release; publish a release only after accepting that exact tested commit.

## Acceptance/reporting policy

- **T0:** repository/source documentation verified.
- **T1:** syntax, static and headless unit tests, including ArcPy stubs.
- **T2:** native toolbox/GUI loads in target application with expected parameters.
- **T3:** real geoprocessing executes in target GIS and deterministic output inspected.
- **T4:** packaged standalone software is installed/operated/uninstalled on a clean target device.

For every verified feature record the exact OS, app/SDK, Python/toolchain, data fixtures and hashes, output, log, test date and pass/fail/blocked status. Never upgrade T0/T1 to T3 solely because GitHub Actions is green.

## Authoritative agent skill format

- https://agentskills.io/specification
- https://github.com/agentskills/agentskills/blob/main/docs/specification.mdx
- https://www.skills.sh/docs/cli

This is an engineering audit, **not** a claim that external projects, live APIs, native host apps or a distributed executable have been fully regression-tested.
