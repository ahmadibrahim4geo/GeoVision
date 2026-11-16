# Mobile GIS Offline Maps, Edits and Synchronization Contract

Read with the [Mobile GIS skill](../skills/gis-mobile-application-development/SKILL.md) and [mobile sign-in contract](mobile-gis-authentication.md).

## Offline by design

Offline **viewing** and offline **editing** are different capabilities. Download a bounded region and zoom range only with permission and a source license allowing local retention. Record map package CRS, tile attribution, version, byte count, coverage and expiration terms. Do not pre-cache prohibited web tiles or claim offline support because the last online map was visible in memory.

Persist editable geodata in a transaction-capable local store (for example GeoPackage/SQLite when appropriate). Each feature has a stable UUID, schema ID, geometry+CRS, field values, local revision and last-known server revision. A separate append-only outbox records the operation. Unsent changes must survive force-stop, process death, OS suspension and device reboot. Do not purge unsent outbox after sign-out unless the user explicitly authorizes data deletion and receives a safe export option.

## Suggested deterministic operation schema

- op_id: immutable client-generated unique identifier, stable across network retries.
- feature_id / project_id: stable typed identifiers.
- action: create, update or delete (delete is a tombstone until acknowledged).
- base_revision: last server revision used to edit; null only for a new unsynchronized feature.
- payload: validated fields/geometry/CRS; attachments uploaded through independently idempotent steps.
- state: PENDING, IN_FLIGHT, ACKED, CONFLICT or BLOCKED.
- server_revision: populated from a verified acknowledgment; never fabricated locally.
- integrity: validated content hash where useful and a project/schema version.

## Upload state transitions

1. Local edit is atomically committed together with the PENDING outbox record. A UI toast is not evidence of a durable commit.
2. While offline, denied network access, logged out or lacking a valid token, remain PENDING or explicitly BLOCKED. Never silently clear.
3. On authenticated connection, submit op_id and base_revision, server validates authorization, project, schema, feature and version.
4. Server must deduplicate operation IDs; repeat of same op_id returns the prior acknowledgment without creating a duplicate geometry.
5. On matching revision, server commits atomically and returns ACKED plus new server revision; only then mark outbox operation as acknowledged.
6. On revision mismatch, move to CONFLICT, keep both local and remote copies and show who/when/what changed. Resolution (manual merge, keep local or keep remote) must be an explicit audited action.
7. On request timeout, unknown outcome, 5xx or app crash, retry the **same** op_id with bounded backoff. Never assign a new op_id just for a retry.
8. Respect per-project permissions and sign-out/revocation. A revoked session cannot upload; local unsent geodata still follows the user's stated retention choice.

## Pull and conflicts

Pull changes with a server cursor/change token and transactionally apply only after validating schema, CRS, feature IDs and permissions. Never overwrite dirty local records on pull. For delete versus local edit, retain a tombstone conflict and let the user decide. Handle attachment missing/hash mismatch, schema migration, database full, clock skew and multi-device edits. Use server revisions or opaque ETags rather than device wall clock as the concurrency authority.

## Required offline tests

- Airplane mode before launch, during edit and during sync; cancel/kill/reopen and verify local data survived.
- Duplicate op_id replay yields one logical server edit.
- Different op_id with stale base_revision must become CONFLICT; do not use last-write-wins implicitly.
- 401/403, token expiry, session sign-out, provider outage and exhausted storage leave an inspectable queue.
- Arabic attributes, photographs, CRS, vertical metadata and GeoPackage transactions survive export/readback where supported.
- Validate permission denial and partial tile package coverage with proper error UX.

The [offline sync fixture](../examples/mobile-gis/README.md) exercises a small **in-memory simulation** for logic only, never claims network/device testing or durable persistence.
