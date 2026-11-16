# Golden workflow: verified coordinate audit

This vertical slice shows the repository's intended development contract from
input validation to deterministic artifact and machine-readable evidence.  The
offline core uses only Python's standard library; a QGIS Processing adapter is
included for real native acceptance when QGIS is available.

## Run the verified offline path

From the repository root, choose a **new** output directory:

```powershell
python examples/golden-qgis-coordinate-audit/coordinate_audit.py `
  examples/golden-qgis-coordinate-audit/fixtures/points.geojson `
  .local-output/coordinate-audit
```

Compare `audited_points.geojson` and `summary.json` with [the committed expected
artifacts](expected/).  The command rereads both files and checks the artifact
SHA-256 before returning success.  It never modifies the source and refuses an
existing output directory.

Copyable agent request:

> Use the GIS Development Skills golden coordinate-audit workflow on my
> GeoJSON. Preserve the source, require an explicit CRS, write to a new output
> directory, compare the result with expected acceptance criteria, and report
> T0–T4 evidence without treating offline tests as native QGIS proof.

## What it verifies

- The input is a UTF-8 GeoJSON `FeatureCollection` with no duplicate JSON keys.
- CRS is explicitly `EPSG:4326`; no silent reprojection is attempted.
- Each accepted feature has a unique string ID and finite Point coordinates in
  longitude/latitude bounds.
- Rejected feature indexes and reasons are deterministic.
- Output bytes, hashes and readback match committed expected artifacts.
- Inputs are bounded to 10 MiB and 100,000 features to avoid unbounded memory
  use in this instructional workflow.

The fixture is invented test data, not survey-grade measurement.  This example
does not establish positional accuracy, axis-order suitability for another
format, or fitness for distance/area calculations.  Reproject to an appropriate
projected CRS before metric analysis.

## Evidence matrix

| Tier | Current repository evidence | How to advance |
| --- | --- | --- |
| T0 | Source, adapter and contract are reviewable | Review the linked code and requirements |
| T1 | Deterministic fixture, negative tests, expected files and readback | Run `python -m unittest tests.test_golden_qgis_coordinate_audit -v` |
| T2 | **NOT RUN** | Load the adapter in a named compatible QGIS runtime |
| T3 | **NOT RUN** | Run `native_qgis_smoke.py`, reopen output and retain its report |
| T4 | **NOT RUN** | Install a packaged deliverable in a clean representative environment |

The canonical meanings are in [the evidence-tier registry](../../references/evidence-tiers.json).

## Measure the offline core

```powershell
python examples/golden-qgis-coordinate-audit/benchmark_coordinate_audit.py --features 10000
```

The command reports elapsed milliseconds and features/second after a correctness
check. Results depend on the machine and Python build; they are T1 synthetic
performance evidence, not a QGIS rendering or provider benchmark.

## Native QGIS acceptance

Use the Python environment shipped with the QGIS version being tested.  QGIS
prefix configuration differs by installer and operating system; do not claim a
native result if imports or initialization fail.

```powershell
python examples/golden-qgis-coordinate-audit/native_qgis_smoke.py `
  --work-dir C:\temp\coordinate-audit-qgis `
  --report C:\temp\coordinate-audit-qgis-report.json
```

The native script initializes QGIS Processing, executes the adapter, rereads the
artifact, checks its hash, and records the exact QGIS version.  A successful
report supports T3 for that observed environment; it does not establish clean
installation or packaging (T4).

## Environment and license

| Path | Minimum environment | Network | Current status |
| --- | --- | --- | --- |
| Offline core | Python 3.10+ standard library | None | T1 verified by repository tests |
| QGIS adapter | Compatible QGIS/PyQGIS with Processing | None | Source present; T2–T4 NOT RUN here |

Repository code is provided under the project [MIT License](../../LICENSE).
QGIS has its own GPL licensing terms; review distribution obligations before
packaging this adapter with QGIS or other components.

## العربية

المسار غير المتصل يتحقق من ملف GeoJSON معلن كـ `EPSG:4326`، ويحافظ على المصدر،
ويرفض مجلد الإخراج الموجود مسبقًا، ثم يعيد قراءة الناتج ويقارن بصمته. نجاح هذا
الاختبار هو T1 فقط؛ ولا يُعد إثباتًا للتشغيل داخل QGIS حتى يتم تنفيذ اختبار
PyQGIS الأصلي وتسجيل إصدار البيئة والنتيجة الفعلية.
