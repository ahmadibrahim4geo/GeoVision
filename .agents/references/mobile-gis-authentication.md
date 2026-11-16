# Mobile GIS Sign-in, Offline Session and Privacy

Read with [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md) and [Mobile GIS Application Development](../skills/gis-mobile-application-development/SKILL.md).

## Authentication choices

Do not make sign-in mandatory for a local-only offline mapper that does not need a remote account. For projects requiring a remote backend, select a documented identity provider/organization, access model and data ownership. Standard mobile flow should use system browser-based OAuth 2.0 authorization code with PKCE for public clients, an exact registered redirect/deep-link scheme or verified universal/app link, server-side token validation, TLS, short-lived access tokens and refresh-token rotation or other provider-supported session renewal. Do not put a client secret in an APK/IPA, intercept other apps' callback URLs, or implement an embedded password-collecting web view without a reviewed security justification.

Secrets and refresh tokens belong in the platform's protected storage (Android Keystore-backed solutions, Apple Keychain); never in GeoPackage metadata, plain shared preferences, exported backups, analytics, logs, source tree, KML or KMZ. PIN/biometric local unlock does **not** authenticate against a remote server.

## State machine

| State | Allowed operations | Disallowed operations |
| --- | --- | --- |
| GUEST_LOCAL | Local project and offline editing where policy allows | Private remote sync |
| SIGNED_OUT | Guest-safe data access and explicit login | Remote mutation |
| AUTHENTICATING | Browser login handoff and cancellable progress | Claim authenticated before server confirms |
| AUTHENTICATED | Scoped server requests and permitted project sync | Other tenant/project data |
| OFFLINE_UNLOCKED | Previously authorized local workspace subject to retention/device policy | Claim online verification or upload while offline |
| EXPIRED | Preserve local outbox, present reauthentication | Automatic remote mutation with invalid access token |
| REVOKED | Lock protected remote data where policy requires, keep user-authorized export/recovery path | Refresh/upload after revocation |

No automatic conversion from OFFLINE_UNLOCKED to AUTHENTICATED without actual server verification. Capture server 401 vs 403 distinctly, redact URLs containing tokens, and limit account metadata retained on device. If offline access is not allowed for a sensitive project, deny local opening rather than pretending the old session remains verified.

## GIS-specific threats

- Protect sensitive locations, field observations, photos, device identifiers and role-linked geodata. Do not silently upload GNSS tracks.
- Scope every API query by account, tenant, project, layer and feature authorization; test cross-tenant rejection and server-side rules.
- Define attachment storage/encryption, retention, secure export and sign-out/device-loss procedures.
- Token expiry during edit or upload must not delete unsynchronized edits; show a blocking state and explicit repair/retry.
- Keep a readable local audit trail with hashed pseudonymous actor identifiers where suitable, never raw credentials.

## Test evidence

T1: deterministic tests for sign-in states, offline unlocked versus authenticated, expire/revoke and token-safe logging. T2: real auth server with test identity, fail/refresh/revoke/cross-project authorization. T3: device browser callback, background/resume, airplane mode, local biometric prompt as separately implemented. T4: signed package and deep-link validation, app update, backup/restore, logout/reinstall on both OSes. Record the provider, audience/scopes and exact app/OS versions; mark unperformed checks NOT RUN.
