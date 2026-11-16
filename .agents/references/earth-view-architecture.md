# Earth View — Satellite globe and remote-sensing explorer architecture

Reviewed: **2026-10-25**. Source: [colincode0/earth-view](https://github.com/colincode0/earth-view). **REFERENCE ONLY: source license absent in inspected repository**; GitHub API returned null license and tree had no LICENSE. Public repository and README saying "open-source" are not a legal grant to redistribute code. Nothing from Earth View has been copied into this skills repository; no live execution or installer test was done.

## Inspected technical architecture

- `README.md`, `README-extended.md`, `package.json`, `desktop/README.md`, `src/providers/GibsProvider.ts`, `SentinelProvider.ts`, four `.codex/skills/*/SKILL.md` agent skills.
- Web frontend: React/TypeScript, Three.js with React Three Fiber, Vite, Zustand and Tailwind CSS.
- `earth-view-globe` describes sphere imagery, camera/zoom, NASA overlays and event markers. `earth-view-imagery-providers` routes base imagery, overlay and regional Sentinel through a typed provider registry. `earth-view-modal` governs regional pan/zoom, date and time-lapse, and saves/restores globe state. `earth-view-server-apis` documents server-side Copernicus OAuth, scene catalog, imagery requests and optional AI provider APIs.
- The Electron Windows shell starts a localhost server to share UI/API behavior with the web app; it is **not** a WPF, CesiumJS or QGIS renderer. Use concepts with new adapters, not code copied verbatim.
- README advertises Ask AI, but upstream `earth-view-modal/SKILL.md` describes a `ASK_VIEW_VISIBLE = false` gate. Treat UI availability as **not confirmed** until inspecting the exact running build.

## Reusable design concepts and mandatory validation

| Component | Engineering lesson | Verification gate |
| --- | --- | --- |
| Globe + detail view | Separate global sphere imagery, close-up raster pass and regional inspection workspace | Preserve viewport/cursor geolocation and prevent globe disappearing |
| NASA GIBS | Use date-aware, layered imagery provider registry and base/overlay distinction | Query actual WMS capabilities; account for WMS 1.3.0 EPSG:4326 **latitude, longitude** BBOX axis order |
| Sentinel-1/2 | Server-side OAuth, distinct Process API and Catalog API, bounded AOI and scene filter | Auth/quotas, pixel QA, original scene date, license and provenance |
| Time-lapse | Fixed bbox, stable frame sizes, real dates; distinguish mosaics from single scenes | Check frame source, missing dates, cancellation/export and memory |
| Modal globe state | Save and restore date, camera, layers and overlays when inspection ends | Open/close, back to globe, persisted project and release smoke test |
| NASA/USGS/EONET events | Separate event feeds from imagery products | Feed date, provider attribution, event and geometry uncertainty |
| Optional image AI | Transmit only consented image and minimal geolocation/date context | Feature actually enabled, no secret in client JS, disclose speculative interpretations |
| Windows Electron | Separate native shell, local HTTP service, frontend and credential manager | Loopback host/origin guard, Windows installer integrity and signed release review |

The existing [GIS API index](free-geospatial-apis.json) **already contains** `nasa-gibs` and `copernicus-dataspace`; do not duplicate services simply because a new software repository uses them. NASA GIBS pre-rendered images are visualization products, **not** calibrated original sensor raster data. NASA source: https://nasa-gibs.github.io/gibs-api-docs/access-basics/ ; Copernicus source: https://documentation.dataspace.copernicus.eu/APIs/SentinelHub.html . Each has separate usage, attribution and operational requirements.

## Security and licensing limits

Do **not** copy upstream code before licensing is confirmed. Do not copy local plaintext `config.json` storage of API keys into a production desktop application; prefer OS-protected credentials (e.g. Windows Credential Manager/DPAPI). Keep secrets server-side, restrict Electron navigation and local HTTP origin/CORS/CSRF, respect imagery credits and actual acquisition dates. Do not advise bypassing Windows SmartScreen as a substitute for validating an unsigned executable's provenance.

## Source-to-skill routing and evidence

- [Web mapping](../skills/geospatial-web-mapping/SKILL.md) — globe, provider registry, layers, modal state and time-lapse UI.
- [Desktop apps](../skills/gis-desktop-application-development/SKILL.md) — secure native packaging, embedded viewer and local server; no WPF/Three.js/Cesium drop-in compatibility.
- [Remote sensing pipelines](../skills/remote-sensing-pipelines/SKILL.md) — satellite collection/version, cloud masks, dates, raster pixel provenance.
- [GIS API integration](../skills/geospatial-api-integration/SKILL.md) — correctly authenticated Copernicus and public NASA GIBS WMS.

This review is **T0 documentary evidence only**. T1 needs a real build and synthetic WMS bbox/date/state tests; T2 requires observed integration with a named browser/Electron renderer or live provider; T3 requires the real imagery/UI operation plus output/state readback; T4 requires clean Windows deployment with representative imagery requests and no credential leaks.
