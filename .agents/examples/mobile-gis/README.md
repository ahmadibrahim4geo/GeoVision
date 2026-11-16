# Mobile GIS Offline/Sync Contract — Demonstration Fixture

This is a deliberately small **offline deterministic example**, not an Android or iOS app, map renderer, encrypted database, authentication server, geoprocessing engine, network synchronization client, or publishable package.

- Run from the repository root: python -m unittest tests.test_gis_mobile_application -v
- Verify truthful device evidence: python scripts/validate_mobile_device_evidence.py examples/mobile-gis/device-acceptance.example.json
- Module: offline_sync_contract.py (pure Python standard library).
- Tests: tests/test_gis_mobile_application.py.
- Scope: immutable client operation IDs, revisions, deduplicated replay, server-side conflicts, and rejection of remote writes when disconnected or without verified authentication.
- No real device, OAuth flow, offline persistence, scoped storage or actual GNSS sensor is exercised. The real adapter must implement atomic SQLite transactions, protected authentication and genuine Android/iOS device UI tests separately.

For architecture, [read mobile architecture](../../references/mobile-gis-architecture.md), [offline data and sync](../../references/mobile-gis-offline-sync.md), [login/session privacy](../../references/mobile-gis-authentication.md) and the [device test matrix](../../references/mobile-gis-device-test-matrix.md). Follow the [mobile skill](../../skills/gis-mobile-application-development/SKILL.md).
