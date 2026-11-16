# GIS Performance, Reliability and Crash Prevention

Apply to desktop applications, ArcGIS toolboxes, QGIS plugins, web GIS and geoprocessing services.

## Design controls
- Use bounded-memory streaming, spatial indexing, tiling and batch processing for large data.
- Keep UI work responsive: move expensive tasks to worker processes/threads appropriate to the host.
- Provide cancellation, progress reporting, checkpoints and atomic output writes.
- Validate user inputs and corrupted geospatial data before processing.
- Capture structured error logs and actionable diagnostics without leaking personal data.
- Restore recoverable application state; never silently overwrite a valid project after failure.

## Test matrix
| Test | Evidence |
| --- | --- |
| Baseline | Dataset size, runtime, peak RAM, CPU, disk I/O |
| Scale | Multiple feature/raster sizes and scaling curves |
| Stress | Load beyond expected operating range and graceful refusal |
| Soak | Repeated open/process/close cycles and memory trend |
| Failure | Corrupt KML/KMZ, bad CRS, missing layers, interrupted writes |
| Recovery | Cancel, restart, reopen last committed project |
| Regression | Compare to baseline with documented tolerance |
| Compatibility | Native ArcGIS Pro, ArcMap, QGIS, Windows and browser tests where applicable |

Define thresholds per project and hardware; no universal maximum file size or feature count exists. Unit and mock tests are not proof of native runtime behavior.
