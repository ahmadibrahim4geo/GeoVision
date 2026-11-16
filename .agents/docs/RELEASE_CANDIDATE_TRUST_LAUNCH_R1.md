# Trust Launch r1 — release-candidate notes

Status: **verified feature published to `origin/main`; no tag or GitHub Release created**
Reviewed plans: `GIS-TRUST-LAUNCH-20261029`, revisions `r1` and `r2`
Verification date: 30 October 2026 (Africa/Cairo)

## What changed

- Added a bounded, deterministic self-contained suite builder and archive audit.
- Established one machine-readable T0–T4 evidence-tier registry and drift check.
- Added the first golden QGIS-oriented workflow: an offline EPSG:4326 coordinate
  audit with expected artifacts, negative tests, hash/readback verification and
  a QGIS Processing adapter.
- Moved installation, a copyable request, the golden path and evidence status to
  the opening English/Arabic onboarding surfaces.
- Added canonical/social/structured website metadata, a sitemap, robots policy,
  code of conduct and pull-request template.

## Fresh local verification

| Check | Observed result |
| --- | --- |
| Project catalog | PASS — 188 sources |
| API catalog | PASS — 52 services |
| Skill pack | PASS — 35 skills |
| Evidence-tier consistency | PASS — canonical T0–T4 labels |
| Generated inventory manifest | PASS — 35 skills / 188 references / 52 APIs / 24 data repositories |
| Skill routing graph | PASS — 35 discovered / 33 routed specialists |
| Security Guardian | PASS — 0 findings / 0 blocking; external scanners NOT RUN |
| Self-contained bundle build and archive re-audit | PASS — local temporary build; agent-client install NOT RUN |
| Golden workflow performance sample | PASS — 10,000 generated points in 78.518 ms (127,359 features/second on this run); synthetic T1 only |
| Unit tests | PASS — 391 tests, 1 Windows symlink test skipped |

The first sandboxed unit-test run could not create Windows hard links for four
development-journal tests. Those tests passed when rerun with the required local
filesystem permission, and the complete 391-test command then passed in the
same permitted environment.

## Evidence boundary

- Golden workflow offline fixture, failure paths and artifact readback: **T1**.
- QGIS adapter load in a named QGIS runtime: **T2 NOT RUN**.
- QGIS operation plus independent output reopen/readback: **T3 NOT RUN**.
- Clean-environment packaged install, workflow and teardown: **T4 NOT RUN**.
- Agent-client installation of the generated skill archive: **NOT RUN**.
- External security scanners: **NOT RUN**.

No Git tag, GitHub Release, repository topic, About text, Discussions setting,
Pages source or other administrator setting was changed by this work.

## Remaining administrator publication checkpoint

Branch publication was separately approved and completed under revision `r2`.
The remaining external or administrator actions still require separate explicit
approval:

- [x] Review the diff and the plan-bound DoneProof reports.
- [ ] Install the generated archive in each named supported agent client and
      retain client/version-specific smoke evidence.
- [ ] Run `native_qgis_smoke.py` in the supported QGIS version and retain its
      report if native QGIS support will be advertised.
- [ ] Decide the version/tag and create a GitHub Release with the ZIP and
      `.sha256` generated from the reviewed commit.
- [ ] Set repository About text, website URL and topics such as `gis`, `qgis`,
      `arcgis`, `geospatial`, `agent-skills`, and `open-source` if appropriate.
- [ ] Enable Discussions only with a moderation/response plan.
- [ ] Confirm GitHub Pages uses the intended branch and project subpath.
- [ ] Recheck social preview rendering; SVG preview support varies by platform,
      so prepare a raster preview before relying on link cards.
