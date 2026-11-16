# Production-grade GIS Workflows

The established [general GIS skill](../skills/gis-development/SKILL.md) routes work across GIS hosts; three **additional** specialties cover [spatial databases](../skills/spatial-databases/SKILL.md), [independent data processing](../skills/geospatial-data-processing/SKILL.md), and [remote sensing](../skills/remote-sensing-pipelines/SKILL.md). No existing tool or reference is replaced.

## Documented examples
- [Network QA](../examples/network-topology/README.md): synthetic LineStrings, read-only duplicates and endpoint dangles; not complete topology.
- [API ingestion](../examples/api-ingestion/README.md): Open-Meteo **synthetic fixture**, WGS84/UTC/units and opt-in live use.
- [ArcGIS Pro batch export](../examples/batch-export/README.md): controlled layout-to-PDF outputs; real ArcPy test required.
- [PostGIS SQL](../examples/spatial-sql/postgis_network_index.sql): sandbox GiST/geometry query plan reference.
- [STAC/COG workflow](../examples/remote-sensing/STAC_COG_WORKFLOW.md): remote-sensing reproducibility and pixel checks.

Start with the [examples index](../examples/README.md). Each example must preserve source data, declare environment and licensing, avoid hardcoded paths, validate CRS/units, and report the highest evidenced test tier from the [quality benchmark](../docs/QUALITY_BENCHMARK.md).

## GIS Agent runtime entry points

Read the [short decision tree](agent-decision-tree.md) instead of loading all specializations. Implement [security](geospatial-security.md), [performance](geospatial-performance.md), [before/after examples](gis-antipatterns.md), [test fixtures](../fixtures/README.md), [native acceptance](../native_tests/README.md) and [machine-checkable evidence reports](../templates/GIS_ACCEPTANCE_REPORT.md). All existing case studies, tools and reference catalogs remain available.
