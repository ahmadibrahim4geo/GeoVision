# PR #27 — GIS Software Source Registry reconciliation

**Reviewed:** 2026-10-26

[PR #27](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/27) originally expanded the GIS source catalog from 148 to **188 unique repositories**, created the software source registry guide, and provided HTML/Explorer links and catalog validation tests.

## Why only a history record is merged

The registry guide, the 188-project JSON catalog, the validation script, and the catalog regression tests are already present in current `main` at identical Git blob hashes to PR #27. The bilingual website entrypoints and most added documentation have also been carried forward. Some legacy PR lines, including a 23-skill README count, are intentionally **not** restored: newer work in `main` updated the skill inventory, UI and routing documentation. Full restoration of the old branch could regress that newer content.

The original PR branch remains an important historical contribution but its changes were already propagated to main via subsequent changes. Its history is reconciled by a two-parent commit retaining the current main tree and adding only this audit record. No sources or libraries from external repositories are bundled or executed by this documentation-only change.

## Verified contents

- [GIS software source registry](../references/gis-software-source-registry.md)
- [Canonical GIS project catalog](../references/project-catalog.json): 188 projects at reconciliation time
- [Catalog validation tests](../tests/test_gis_software_source_registry.py)
- [Project Explorer](../explorer.html)

**Limitations:** upstream native builds, transitive licenses, device deployment, offline datasets and sensor permissions remain subject to later per-project review. Passing static GitHub Actions CI does not certify real Android, iOS or Windows builds.

Reconciliation acceptance: the final branch comparison against `main` must show only this document, CI validation and security checks must pass, and the PR may then merge for accurate GitHub history.
