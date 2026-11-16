# GIS Engineering Acceptance Report

Fill all fields with tested facts; write `not tested` for tasks lacking host access. A missing native ArcGIS/QGIS/Windows environment must never be disguised as success.

| Field | Value |
| --- | --- |
| Target | |
| OS | |
| Application/version | |
| Python/runtime | |
| GIS/CRS/units/vertical datum | |
| Evidence tier (T0–T4) | |
| Commands executed | |
| Fixtures used (paths/hashes) | |
| Expected result | |
| Observed result | |
| Not tested | |
| Known limitations | |
| Changes to user data | |
| Provenance/licensing | |
| Overall status (pass / partial / blocked / fail / not tested) | |

For an auditable machine-readable record populate [the JSON example](gis-acceptance.example.json) and validate it with:
```bash
python scripts/validate_evidence.py templates/gis-acceptance.example.json
```

If a task was only coded or unit-tested without target application runtime, report T0/T1; do not claim T2, T3 or T4. See the [machine-readable tier registry](../references/evidence-tiers.json) and [quality benchmark](../docs/QUALITY_BENCHMARK.md).
