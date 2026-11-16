# Mobile GIS Architecture, Runtime and Engine Selection

**Scope:** Android and iOS field applications; not desktop-shell or responsive-web-only development. Entry point: [Mobile GIS skill](../skills/gis-mobile-application-development/SKILL.md). See the [canonical source catalog](project-catalog.json) and [source registry](gis-software-source-registry.md) for current upstream records.

## Choose independently

| Concern | Options to evaluate | Verification evidence |
| --- | --- | --- |
| OS and UI | Android Kotlin; iOS Swift; Qt/QGIS mobile; Flutter; .NET MAUI | Supported OS versions, build ABI, SDK license, actual device UI |
| 2D render | MapLibre Native, verified native framework, QGIS map canvas | Tiles, styles, offline fallback, projection, GPU, attribution |
| Editing and I/O | Version-matched QGIS/GDAL if deployable; suitable native spatial adapters | Real editable GeoPackage/GeoJSON/KML round-trip; transaction safety |
| CRS | PROJ or version-tested mobile CRS API | Known reference control points, axis order, datum grid availability |
| Storage | GeoPackage/SQLite, OS app sandbox, selected-file URI | WAL/transactions, schema migration, encrypted sensitive data, backup |
| Sensor | Android location services, Apple Core Location, supported external GNSS | Foreground/background permission state, accuracy/fix age, no spoofed live claim |
| Synchronization | API with idempotent outbox, content version and per-feature revisions | Offline/restart/retry/conflict and login expiry acceptance |
| Maps | Licensed self-hosted raster/vector packages or verified downloadable packages | Attribution, bounds, update rules, cache and redistribution rights |

A mobile map renderer alone is **not** a complete GIS engine. A source repository URL is **reference-only** until matching versions, licenses and native builds are independently verified. Evaluate [QField](https://github.com/opengisch/QField), [Mergin Maps](https://github.com/MerginMaps/mobile), [MapLibre Native](https://github.com/maplibre/maplibre-native), and [Mapsui](https://github.com/Mapsui/Mapsui) without assuming commercial cloud access or copying upstream trademark/assets. QGIS integration requires GPL and Qt/plugin/provider packaging review; desktop ABI proof does not prove mobile ABI compatibility.

## Component contract

- Project service: stable project ID, version, layer descriptors, CRS, geometry and field schema, attribution and migrations.
- Data service: import/probe/export/readback, sandboxed documents, explicit format fidelity and typed errors.
- Map service: camera, layers, selection and diagnostics; never silently change original imported KML/KMZ styling.
- Field service: validated forms, local transaction, attachment hash, editor identity and immutable client operation IDs.
- Device service: location permission, GNSS reading age/accuracy, pause/resume, battery/storage policy and precise denial fallback.
- Auth service: keychain/keystore, server verification, refresh and revoked/offline session transitions.
- Sync service: durable outbox, server revision comparison, conflict queue, retry with idempotency, no silent deletion.
- Release service: platform manifests, permission strings, signed package, clean-install/migration and third-party notices.

## Selection and proof

Before coding, record target API/minimum OS; device CPU/ABI; required GPS, camera, storage and BLE; expected maximum offline map size; CRS/vertical accuracy; scope of feature edits; login requirement; retention/deletion; map source rights; transitive licenses; and deployment channel. Build one independently verifiable vertical slice on each target OS. Never declare Android/iOS compatibility based on CI source checks alone.
