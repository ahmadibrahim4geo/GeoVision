# GIS Development Skills — detailed engineering updates

These notes were moved from the README to keep the GitHub landing page concise. Historical counts describe the time of each milestone, not the current repository inventory.

## Global Mapper / Blue Marble Geographics research — 2026-10-25

Added a [Global Mapper Development specialist](../skills/global-mapper-development/SKILL.md) and [Blue Marble commercial SDK / GitHub fork reference](../references/blue-marble-global-mapper.md). Curated **7 official-organization forks and 1 independent community GMS scripting repository**, bringing the software catalog to **138 references**. The official organization repositories are forks, **not** source releases of the proprietary Global Mapper engine; use commercial SDK only with required development/redistribution licenses. The bilingual [index.html](../index.html), lightweight router, desktop, terrain and web/3D specialists and derived manifest are synchronized. No commercial SDK binary is included.

## Earth View satellite globe reference — 2026-10-25

Added [Earth View](https://github.com/colincode0/earth-view) to our [software catalog](../references/project-catalog.json), with a [technical architecture review](../references/earth-view-architecture.md) linked from the bilingual website, Web Mapping, Desktop, Remote Sensing and API specialists. NASA GIBS and Copernicus Data Space were **already** indexed as APIs, so we did not duplicate them. The [atomic source intake checklist](../references/source-intake-workflow.md) helps keep future changes synchronized. **Upstream license not found; architecture reference only, no code copied.**

## OpenGeoS advanced engineering and GIS data repositories — 2026-10-25

The toolkit now curates **24 GIS data source directories and portals** separately from its **52 API records** and **129 software references**. It **links** the user's [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026) (its own 2,660+ source master index) rather than duplicating those records. New [data repositories explorer](../index.html), [24-entry JSON catalog](../references/geospatial-data-repositories.json) and [data-provider guide](../references/geospatial-data-repositories.md) distinguish an index, a real STAC API, and the actual downloadable scientific asset.

**New installable specialists:** [Data Repositories](../skills/geospatial-data-catalogs/SKILL.md), [Geospatial AI Agents](../skills/geospatial-ai-agents/SKILL.md) and [Terrain / Hydrology / LiDAR](../skills/terrain-hydrology-lidar/SKILL.md). New [OpenGeoS advanced integration review](../references/opengeos-advanced-integration.md) and an offline [directory query example](../examples/data-catalogs/README.md) show source selection and reproducibility. None of the listed software, data access, or native GIS integration is certified by documentation-only inspection.

## Geocoding & place search — 2026-10-25

The **52-service API directory** now includes **15 cost-ranked geocoding and geographic place-name providers**: Pelias, Photon, Open-Meteo, Nominatim, GeoNames, Geoapify, LocationIQ, geocode.maps.co, Esri, MapTiler, OpenCage, Google Places, Mapbox, HERE and TomTom.

Read the [free-to-commercial selection guide](../references/geocoding-place-search.md), with Arabic usage notes and sample requests; or browse [the live bilingual catalog](https://ahmadibrahim4geo.github.io/GIS-Development-Skills/). Production use rights are independent of daily free credits: OpenCage's 2,500/day tier is **for testing only**; Nominatim's OSMF server **prohibits autocomplete**.

This is a source/skill improvement, not a claim of a tested live API or a completed application integration.

## GIS Agent Operating System

The core skill is now a lightweight router with [the complete previous guidance preserved](../references/gis-development-full-guidance.md) and a [decision tree](../references/agent-decision-tree.md). New resources: [evidence form](../templates/GIS_ACCEPTANCE_REPORT.md), [evidence validation](../scripts/validate_evidence.py), [test fixtures](../fixtures/README.md), [native GIS harness](../native_tests/README.md), [security](../references/geospatial-security.md), [performance](../references/geospatial-performance.md), [before/after examples](../references/gis-antipatterns.md), [generated manifest](../references/repository-manifest.json) and [weekly provider checks](../docs/SERVICE_MONITORING.md).

Previous skills, original guidance, project/API references, brand assets and bilingual content are preserved; native and live provider verification remain separate.

## GIS Project Wizard, KML/KMZ and reliability

- [Arabic and multilingual GIS practical guide](../references/gis-multilingual-arabic-guide.md): Unicode, BiDi, Arabic shaping, localization examples and upstream standards.
- [Multilingual GIS and Arabic-first localization skill](../skills/gis-internationalization-arabic/SKILL.md): RTL/BiDi, Unicode attributes, map labels, localized toolboxes, exports and language-aware search.

- [Launch the bilingual GIS Project Wizard](../project-wizard.html) — free-first architecture choices and downloadable Markdown implementation plans.
- [Project Wizard implementation roadmap](../docs/PROJECT_WIZARD_PLAN.md).
- [KML/KMZ Engineering Skill](../skills/kml-kmz-engineering/SKILL.md) and [open-source repository review](../references/kml-kmz-repository-review.md).
- [Performance, stability and crash-prevention guide](../docs/PERFORMANCE_RELIABILITY.md).

The wizard is a static planning aid; generated plans are not evidence of executed native GIS or stress tests.

## Production GIS workflows and quality benchmark — 2026-10-24

**Historical milestone (2026-10-24):** This upgrade added production workflows and a quality benchmark. Its original counts were **11 skills**, **104 project references** and **52 public API entries**; see the live inventory above for current totals.

- [Spatial Databases](../skills/spatial-databases/SKILL.md): PostGIS/SpatiaLite, spatial indexing and measured query plans.
- [Geospatial Data Processing](../skills/geospatial-data-processing/SKILL.md): vector/raster pipelines, safe coordinate transformations and reproducible checks.
- [Remote Sensing Pipelines](../skills/remote-sensing-pipelines/SKILL.md): STAC/COG, Earth Engine, radiometric QA and aligned raster grids.
- [Reproducible engineering examples](../examples/README.md): topology audit, modeled weather to GeoJSON, batch map PDF export, PostGIS SQL and STAC/COG checklist.
- [GIS Quality Benchmark](../docs/QUALITY_BENCHMARK.md): T0–T4 test-evidence levels and native GIS acceptance requirements.
- [Production workflow reference](../references/production-workflows.md): implementation safeguards, test limits and examples.

Network and weather examples have synthetic offline fixtures for deterministic unit tests. ArcGIS/QGIS/PostGIS/Windows and live-provider acceptance still need real host evidence.

