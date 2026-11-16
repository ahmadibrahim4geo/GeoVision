# GitHub Actions failure triage — GIS Development Skills

**Purpose:** Distinguish a failed repository check from broken ArcGIS, QGIS or the GIS agent itself. Email is a notification for a **specific run/commit**, not proof that every version is failing.

## Duplicate and obsolete notifications

- Previous configuration ran workflows on every feature `push` and again on `pull_request`, causing duplicate validations and often two failed-run emails per proposed change.
- Updated `validate.yml` and `security.yml`: run for **pushes to main**, **pull request events**, and manual `workflow_dispatch`.
- Each workflow uses `concurrency` with `cancel-in-progress: true` scoped to its branch/PR. New commits cancel in-progress obsolete runs, but historical completed failures and their already-delivered emails cannot be retroactively removed.
- Do NOT disable validation, mark expected failures successful, or turn security checks off to avoid notifications. Check the newest SHA and PR status before merging.

## What failure symptoms mean

| Failure step | Typical cause | Fix without hiding the test |
| --- | --- | --- |
| Validate skill pack and local links | New skill not in catalog, links missing or website skill counter drift | Regenerate manifest, synchronize bilingual counters and repair paths |
| Check generated manifest and counters | Files and `references/repository-manifest.json` disagree | Run `python scripts/build_manifest.py`, commit outputs and recheck |
| Run offline unit tests | Website structure or business-rule test incompatible with changed markup, or a real code regression | Read **FAIL/ERROR** details, update implementation and legitimate test contract together |
| Validate inline website JavaScript syntax | Invalid inline JS or absent `<script>` block | Correct script and test against actual site before commit |
| Security Guardian | Security policy, archive checks or metadata tests fail | Treat as a release blocker; never bypass to green CI |

## Workflow for an engineer or agent

1. Open **Actions → Validate GIS Skill → latest run** for the **correct branch and commit**, not a historical email.
2. Inspect **Jobs → validate → failed step**. Open the log around `FAIL:`, `ERROR:`, `AssertionError`, and process exit messages.
3. Verify if another PR has since fixed the issue. Group identical failures by *test identifier and offending file*, not by total message count.
4. Fix the smallest root cause, run local tests, publish to an isolated PR, and wait for both `Validate GIS Skill` and `GIS Security Guardian` to be green **for the updated SHA**.
5. Confirm changes to `index.html` and `explorer.html` preserve bilingual layout and existing functional controls; T0–T1 tests are not native T2–T4 GIS acceptance.
6. Keep old failed runs as useful historical diagnostics. Repo's latest successful CI state matters for merging.

## Important limitations

GitHub notifications may be sent to email for historical runs even after code is fixed. Completed job failures cannot be turned into successes by editing documentation. GitHub Pages builds/deployments are separate workflows and normal `cancelled` statuses for superseded site builds are not failures.

Any user-specific email or notification delivery preferences must be changed by that user in GitHub notification settings; a repository workflow change does not necessarily eliminate every email. Keep repository logs redacted, avoid sharing email addresses and secrets, and always use read-only least-privilege CI where possible.
