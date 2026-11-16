# Synthetic GIS Data Flow Trace

```bash
python scripts/gis_data_flow_diagnostics.py --trace examples/data-flow-diagnostics/trace.example.json --format json
```

This is an **artificial seven-event JSON fixture**, not logs from a real GIS application. It claims a lossless import of two imaginary KML features followed by parsing, engine admission, rendering, saving, exporting and independent readback. The offline validator checks schema, phase ordering, required stages, status and declared feature-count conservation.

**Passing this fixture does not prove a renderer appeared on screen, that CRS/datum are correct, or that an exported GIS file can reopen.** No GIS data file is loaded. For an actual user project, instrument boundaries with redacted `trace_id` and `command_id`, collect a real trace under approved permissions, then independently reopen the exported output with the matching GIS provider to test geometry, fields, time, altitude and styles.

A deliberately failing test can change `READBACK.feature_count` from 2 to 1 in a temporary copy; the checker should exit nonzero and explain the lossless feature-count mismatch. The [Data Flow playbook](../../references/gis-data-flow-diagnostics.md) explains other operations with legitimately different counts.
