# GIS Data Flow Diagnostics — end-to-end lineage and fault isolation

The [specialist](../skills/gis-data-flow-diagnostics/SKILL.md) owns data lineage and semantic readback. Use [shared command and trace IDs](gis-ui-quality-coordination.md) and [upstream instrumentation references](gis-ui-quality-upstream-sources.md).

## 1. Build the actual data-flow graph

| Stage | Evidence to capture | Common GIS failure |
| --- | --- | --- |
| IMPORT | Source format, file identity (opaque), origin, import option | file not opened, archive rejected |
| PARSE | Feature count, field schema, nested KML document content, raster dims/NoData | malformed geometry, missing folder/styles |
| CRS | source CRS/datum, target CRS, grid availability, axis/vertical units | layer offscreen, wrong coordinate transformation |
| PROCESS | algorithm/method, parameters, mask, input/output stats | wrong raster resampling, feature dropped |
| ENGINE | provider loaded, engine URI, layer registry ID | UI adapter created no real layer |
| RENDER | visible layer in scene, symbol/layer order and LOD | layer loaded but invisible/incorrect styling |
| SAVE | project model serialized, paths and view-state stored | state lost, no scene/camera persistence |
| EXPORT | artifact type, writer, format options, output identity | empty output, symbols/assets missing |
| READBACK | independent parser reads output; compare invariants | output file exists but is corrupt or differs |

Map only stages used by the operation, but preserve true sequence and failure evidence. A `RENDER` span is not a screenshot or GUI truth. A `SAVE` event is not proof the project can reopen.

## 2. Trace data contract

The portable synthetic JSON format (see [fixture](../examples/data-flow-diagnostics/trace.example.json)) uses:

```json
{
  "schema_version": "1.0",
  "trace_id": "TRACE-DEMO-001",
  "command_id": "import.kml",
  "task_id": "T2",
  "dataset_id": "synthetic-known-file",
  "operation": "lossless",
  "required_phases": ["IMPORT", "PARSE", "ENGINE", "RENDER", "SAVE", "EXPORT", "READBACK"],
  "events": [
    {"sequence": 1, "phase": "IMPORT", "status": "PASS", "feature_count": 2},
    {"sequence": 2, "phase": "PARSE", "status": "PASS", "feature_count": 2}
  ]
}
```

Events allow an optional `crs` (e.g. EPSG:4326), `artifact_id`, `error_code` and `duration_ms`. Never capture passwords, access tokens, raw GPS coordinates, database connection strings, private filenames or user IDs into exported reports. Use redacted opaque identifiers. Tool output must be bounded; protect original files from modifications.

## 3. Deterministic offline validator

```bash
python scripts/gis_data_flow_diagnostics.py --trace examples/data-flow-diagnostics/trace.example.json --format json
```

The standard-library checker identifies: invalid schema, duplicate/out-of-order phases, missing required phases, failures, nonnumeric counts, lossless feature-count mismatch, missing mandatory IMPORT/READBACK spans for lossless operations, and mismatched declared EXPORT/READBACK artifact identities; it does **not** read GIS files or prove geometry/CRS correctness. A phase may be intentionally omitted only when not required in the declared workflow. Real GIS output checks need a host/provider reader.

**Comparable invariant depends on operation:** for lossless copy, feature count and compatible structure should be conserved; for clipping, aggregation or generalization, changes can be correct. Reprojection preserves feature semantics but can alter coordinate bytes and tolerances; a raw hash mismatch alone is not corruption. KML/KMZ style fidelity requires icons/folders/StyleMaps/relative assets and independent roundtrip tests. Raster parity must account for NoData, resampling and georeferencing.

## 4. Instrumentation guidelines

- Pass `trace_id` as explicit scoped context across .NET, Python and JS process/IPC boundaries; on a real target consider OpenTelemetry SDKs *only where needed*.
- Record span elapsed time and provider/operation name; attach numeric counts rather than raw geometries or locations. Default diagnostics local, opt-in network telemetry; use structured sampling and bounded retention.
- Compare the first known-good stage to the first failing stage and record which specialist owns the defect.
- Native acceptance uses real valid/malformed/small/large GIS fixtures; reopen exported outputs independently and compare agreed invariants.

## 5. Hand-off

If geometry/CRS wrong, involve [CRS](../skills/coordinate-reference-systems/SKILL.md) and [Processing](../skills/geospatial-data-processing/SKILL.md). If KML styles wrong, involve [KML/KMZ](../skills/kml-kmz-engineering/SKILL.md). If a button never emitted a trace, hand to [Command Audit](../skills/gis-ui-command-audit/SKILL.md). If data looks right on disk but the map is wrong, E2E and rendering specialists inspect scene/camera and style state. Never silently change GIS engines based solely on one missing event.
