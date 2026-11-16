# PR #12 — per-commit CI forensic audit (25 October 2026)

**Scope:** [PR #12](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/12), 41 commits. This report was prepared using the actual GitHub check-runs and representative failed job logs for **each of the 41 commits**. The listed reason is the **observed failing gate**, not necessarily every defect in that historical snapshot. Where multiple checks/retries exist, at least one run was inspected per SHA.

**Outcome:** 40 intermediate commits show failing historical checks; final head `de0a776` passed both `validate` and `security-preflight`, and [PR #12 was merged](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/pull/12). Later `main` commit `21de626` passed 223 general unit tests, 9 dedicated security tests, and GitHub Pages deployment. These results concern CI, **not native GIS compatibility or visual parity**.

## Failure codebook and verified resolution on main

| Code | Historic gate failure | Current mitigation |
| --- | --- | --- |
| A | Homepage skill-counter drift (16 or 17 discovered skills) | Generated inventory and counters synchronized; `build_manifest.py --check` passes |
| B | README/Arabic README counter or derived manifest stale | Generated inventory and counters synchronized; `build_manifest.py --check` passes |
| C | KML skill missing mandatory free-first policy reference | Core router/policy matrix, Arabic/KML links and README coverage fixed; relevant unit tests pass |
| D | Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete | Core router/policy matrix, Arabic/KML links and README coverage fixed; relevant unit tests pass |
| E | Both KML and Arabic skills missing from free-first policy matrix | Core router/policy matrix, Arabic/KML links and README coverage fixed; relevant unit tests pass |
| F | Policy matrix gaps plus missing required README/example/Earth View/Blue Marble links | Core router/policy matrix, Arabic/KML links and README coverage fixed; relevant unit tests pass |
| G | Landing page referenced premium-layout.css before file existed | Stylesheet committed and local-link validation passes |
| H | Merge conflict: missing project-wizard.html plus counter drift (also security inventory) | Wizard file restored and link + security validators pass |
| I | Homepage 22-skill count drift (security inventory affected) | Generated inventory and counters synchronized; `build_manifest.py --check` passes |
| J | Core router 5,851 characters; validation failed, security passed | Routing and budget fixed; relevant test passes |
| PASS | Both checks succeeded | Final PR #12 head verified |

## Commit-by-commit checks (chronological)

| # | Commit | Historical check | Observed primary failure |
| ---: | --- | --- | --- |
| 1 | [`b4e8f3e`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/b4e8f3e) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 2 | [`42020b0`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/42020b0) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 3 | [`7608a7c`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/7608a7c) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 4 | [`e3ed03b`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/e3ed03b) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 5 | [`65c3d3a`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/65c3d3a) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 6 | [`41d2e97`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/41d2e97) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 7 | [`ab79fcf`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/ab79fcf) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 8 | [`bba53fe`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/bba53fe) | ❌ Failed | B: README/Arabic README counter or derived manifest stale |
| 9 | [`8affcb3`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/8affcb3) | ❌ Failed | B: README/Arabic README counter or derived manifest stale |
| 10 | [`917e618`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/917e618) | ❌ Failed | B: README/Arabic README counter or derived manifest stale |
| 11 | [`db103bb`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/db103bb) | ❌ Failed | C: KML skill missing mandatory free-first policy reference |
| 12 | [`9d7411b`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/9d7411b) | ❌ Failed | C: KML skill missing mandatory free-first policy reference |
| 13 | [`2a3a850`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/2a3a850) | ❌ Failed | C: KML skill missing mandatory free-first policy reference |
| 14 | [`5816b1f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/5816b1f) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 15 | [`b23b92f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/b23b92f) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 16 | [`e0d6b13`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/e0d6b13) | ❌ Failed | A: Homepage skill-counter drift (16 or 17 discovered skills) |
| 17 | [`1601a0f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/1601a0f) | ❌ Failed | B: README/Arabic README counter or derived manifest stale |
| 18 | [`f6a85ec`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/f6a85ec) | ❌ Failed | B: README/Arabic README counter or derived manifest stale |
| 19 | [`b41797f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/b41797f) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 20 | [`3aa46bd`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/3aa46bd) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 21 | [`817ec07`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/817ec07) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 22 | [`60230ea`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/60230ea) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 23 | [`3f69b4b`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/3f69b4b) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 24 | [`847adae`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/847adae) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 25 | [`5c49da8`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/5c49da8) | ❌ Failed | D: Core router exceeded 5,500 characters; KML/Arabic policy coverage incomplete |
| 26 | [`9793902`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/9793902) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 27 | [`e4c37b0`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/e4c37b0) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 28 | [`4028890`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/4028890) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 29 | [`1e16673`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/1e16673) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 30 | [`eccab87`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/eccab87) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 31 | [`c62abf9`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/c62abf9) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 32 | [`1f21985`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/1f21985) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 33 | [`8f21a76`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/8f21a76) | ❌ Failed | E: Both KML and Arabic skills missing from free-first policy matrix |
| 34 | [`b9d5603`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/b9d5603) | ❌ Failed | F: Policy matrix gaps plus missing required README/example/Earth View/Blue Marble links |
| 35 | [`8b811b3`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/8b811b3) | ❌ Failed | G: Landing page referenced premium-layout.css before file existed |
| 36 | [`8b3458d`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/8b3458d) | ❌ Failed | F: Policy matrix gaps plus missing required README/example/Earth View/Blue Marble links |
| 37 | [`02816db`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/02816db) | ❌ Failed | F: Policy matrix gaps plus missing required README/example/Earth View/Blue Marble links |
| 38 | [`536ec77`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/536ec77) | ❌ Failed | H: Merge conflict: missing project-wizard.html plus counter drift (also security inventory) |
| 39 | [`7fc753f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/7fc753f) | ❌ Failed | I: Homepage 22-skill count drift (security inventory affected) |
| 40 | [`bd1f73f`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/bd1f73f) | ❌ Failed | J: Core router 5,851 characters; validation failed, security passed |
| 41 | [`de0a776`](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/de0a776) | ✅ Passed | PASS: Both validation and security checks passed |

## Representative raw evidence

- [First counter drift](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40169035060) (`b4e8f3e`): 16 skills discovered while `index.html` counter was outdated.
- [README and manifest drift](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40169178011) (`bba53fe`): English/Arabic README counters and derived manifest not yet updated.
- [Arabic + KML policy misses and router budget](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/commit/817ec07) (`817ec07`): router 5,542 characters and absent policy/manifest registrations.
- [Missing Wizard after integration](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40210508494) (`536ec77`): local links to `project-wizard.html` broken after merge.
- [Final passing validation](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40210788304) and [security](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40210788300) (`de0a776`).
- [Latest passing main validation](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40211668251) and [security](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/actions/runs/40211668259) (`21de626`).

## What can and cannot be fixed retroactively

GitHub checks are tied to **immutable commit trees**, not just the current branch. Fixing `main` does not turn a failed check at an older commit green. Re-running old jobs without modifying those historical trees will generally reproduce their old failures; rewriting a merged PR's history just to hide X marks destroys traceability and is **not** advised. Treat `main` and the latest PR head as the authoritative release status, while preserving this incident audit.

There was **no evidence that the repository's MIT LICENSE needed to be changed** to fix these failures: the problematic 'free-first policy' test checked whether newly registered specialists were named in a documentation matrix. The `KML`/Arabic skills and `free-first-policy.md` now align. Third-party KML libraries have independent licenses; see [verified license and redistribution reference](../references/kml-kmz-repository-review.md).

**Prevention:** run `python scripts/build_manifest.py` immediately after adding/removing a skill; update the free-first matrix and all required README/entry-point references in the **same commit**; run `python scripts/validate_skill_pack.py`, `python scripts/build_manifest.py --check`, `python -m unittest discover -s tests -v` and JS syntax/interaction checks locally before pushing. Keep security workflows mandatory; never disable tests to make a check green. Native KML/KMZ round-trip and stress testing remain separate acceptance tasks.
