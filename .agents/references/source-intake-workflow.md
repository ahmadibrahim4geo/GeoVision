# Atomic source intake and full-site synchronization

A new GIS project, dataset directory, API or specialist must update all relevant files **in the same pull request**, not just one source list.

0. **Free-first gate:** identify free execution, paid host, trial, academic/noncommercial or cloud credit requirements using [free-first policy](free-first-policy.md), and keep structured API cost_tier aligned with access.
1. **Identify the resource:** add new code only to [project-catalog.json](project-catalog.json), data portals to [geospatial-data-repositories.json](geospatial-data-repositories.json), and distinct web APIs to [free-geospatial-apis.json](free-geospatial-apis.json). Check existing canonical IDs to prevent duplicates.
**Upstream license audit:** follow [license evidence guide](upstream-license-audit.md). `license_verified: true` requires direct inspection of the license file and `license_scope`, `license_checked_on`, `license_evidence_url`, `license_caveat`; review code, dependencies, data and hosted service terms independently. Use `python scripts/audit_license_catalog.py --summary` to see the unverified backlog.

2. **Verify provenance and license:**2. **Verify provenance and license:** owner/repo and original service, recent README/source, supported platforms, rights, dependencies, authentication, imagery/API terms and whether actual execution occurred. Public code without a LICENSE does not authorize copying it.
3. **Update skills and tools:** amend the relevant domain `skills/*/SKILL.md`, reference guide, safety/QA rules and example only when appropriate. Keep lightweight core router short.
4. **Avoid frozen inventory counts in tests:** every integration regression asserting homepage counters must compute its expected count from the canonical JSON instead of hardcoding a historical total (e.g. 130). New additions must not break previous feature tests merely because the catalog grows.
5. **Update all public surfaces:** both `README.md` and `README.ar.md`, `index.html` (already JSON-driven, including bilingual explorer), relevant references and `CHANGELOG.md`. Keep counters source-derived, do not freeze counts in nonhistorical paragraphs. A notable addition may deserve an EN/AR homepage feature link.
6. **Synchronize manifest:** run `python scripts/build_manifest.py` once to regenerate the authoritative [repository manifest](repository-manifest.json) and main README/HTML counters; use `python scripts/build_manifest.py --check` in CI.
7. **Regression and evidence:** run catalog, API, data repository and skill validators plus `python -m unittest discover -s tests -v`. Assert the new source appears in the project catalog, correct skill/reference and index, with verified-or-unknown license state. Only claim real runtime tests when run.
8. **Publish safely:** PR from fresh `main`, inspect GitHub Actions, merge if passed, confirm main SHA and GitHub Pages source. If squash-merging, a residual recent-push branch banner is not evidence of an unmerged patch.

Never add a new API record merely because a newly listed application consumes an already indexed API. Third-party code stays external until licensing is explicitly permitted. A T0 source review is not T3/T4 native acceptance.
