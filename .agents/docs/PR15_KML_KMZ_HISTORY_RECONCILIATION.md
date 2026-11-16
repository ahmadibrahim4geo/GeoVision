# PR #15 — KML/KMZ integration history reconciliation

**Reconciled:** 2026-10-26  
**Scope:** historical pull-request bookkeeping and traceability only. No GIS runtime or symbol logic is changed.

## Why this record exists

[PR #15](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/15) originally proposed open map-icon sources and preservation of imported KML/KMZ styles, but was closed without merging when security and documentation updates landed concurrently on `main`.

All intended functional documentation, fixtures, and style-preservation tests from #15 were carried forward into [PR #16](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/16), which **merged on 2026-10-25**. The work must not be replayed from the outdated #15 snapshot because doing so could overwrite newer security, website, or GIS-skill work.

## Safe historical reconciliation

The reconciliation commit on PR #15 is an ancestry merge of its original work and `main` at `9b7490bb0a4f06378175d8a928b659069dcbfbce`. Its file tree starts from that current `main` snapshot and adds only this explanatory document. No superseded source files, HTML, tests, security controls, or catalog data are restored from the old branch.

PR #15 can therefore be merged as a historical reconciliation without changing the KML/KMZ implementation already delivered in PR #16. GitHub's eventual **Merged** badge on PR #15 records this reconciliation, **not** a second independent implementation.

## Validation and provenance

- [PR #15 original validation](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40179539645/job/121127419853): succeeded.
- [PR #16](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/16): delivered the original map-marker and lossless KML/KMZ policy.
- [PR #30](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/30): separately repaired later Project Wizard/free-first CI regressions and merged on 2026-10-26.
- The PR #15 reconciliation must show only this documentation file in its final comparison against `main`, and must pass relevant checks before merging.
