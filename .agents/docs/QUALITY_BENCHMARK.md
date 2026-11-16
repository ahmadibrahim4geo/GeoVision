# GIS Skill Quality Benchmark

An evidence-based verification standard for installable agent skills and GIS workflow examples. The machine-readable source of truth is [evidence-tiers.json](../references/evidence-tiers.json). A repository link, syntax check, mock or screenshot alone is not proof of native correctness.

| Tier | Required evidence | Not established |
| --- | --- | --- |
| T0 | Source and documented procedure inspected | Executable operation |
| T1 | Repeatable offline/static unit tests using fixtures | Licensed ArcGIS/QGIS or live API |
| T2 | Feature loads or integrates in a named compatible native GIS client, engine, database or live-provider adapter with a controlled fixture | End-to-end operation and independent output readback |
| T3 | Actual GIS/DB/provider operation completes and produced outputs are independently re-opened or re-read and compared | Clean packaged distribution |
| T4 | Packaged software installs or provisions in a clean representative environment, runs the user workflow, persists, exports/reopens and recovers/uninstalls where applicable | Future live-provider uptime or untested versions |

An offline, static, mocked or synthetic fixture is **T1 at most**, even when it produces a real file. T2 begins only when the named native/runtime/provider integration was actually observed. Unavailable T2–T4 checks are `NOT RUN`, never inferred from CI.

## Five quality gates
1. **Geospatial accuracy:** CRS, true coordinate transformation, horizontal/vertical units, geometry validity, raster pixel alignment and nodata.
2. **Reproducibility:** fixture and expected results, versions, environment, exact commands, hashes, source dates and traceable provenance.
3. **Safety:** no overwrite/deletion by default, secure credentials, API provider conditions, licensing, opt-in for destructive geometry modifications.
4. **Integration:** proof of commands actually wired and run inside specified ArcMap, ArcGIS Pro, QGIS, PostGIS or desktop application.
5. **Documentation:** inputs, outputs, edge cases, known limits, failure statuses and discoverable bilingual website links.

Mark each gate `pass`, `fail`, `blocked` or `not tested`. Use the actual highest evidence tier. Native GIS readiness requires T3 for each claimed target, not just a green GitHub workflow.

**Audit record template:** Git commit; tool/skill path; date/reviewer; OS and GIS/database/Python versions; fixture names/hashes; run commands; expected vs observed output geometry/counts/attributes/pixel samples; side effects; performance; license; failures; unresolved limits.

The new offline network/weather examples are suitable for T1. The Pro batch export example has **no native runtime evidence**; PostGIS and Earth Engine examples are review procedures. This preserves the limitations documented in [the existing technical audit](TECHNICAL_AUDIT_2026-10-24.md).

## Machine-checkable reporting (required for implementation claims)

The agent must complete [the acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md) and machine-readable fields of [the example](../templates/gis-acceptance.example.json), then run `python scripts/validate_evidence.py <report.json>`. Record OS, target app/version, runtime, CRS/units, executed commands, fixtures, expected versus observed output, not-tested items, safety, licensing and evidence links.

An agent may describe partial implementation truthfully at T0/T1; **claiming completed native ArcGIS/QGIS processing requires actual target runtime testing and output read-back**. [Native test harnesses](../native_tests/README.md) provide narrow optional examples, not proof of every supported tool.
