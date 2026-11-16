# GIS Development Skills — End-to-End Release Readiness Audit
**Review date:** 2026-10-26  
**Scope:** the GIS Development Skills repository on GitHub, its installed skill pack, offline helpers and public website.  
**Review method:** file inventory, source inspection, adversarial fixtures, GitHub Actions regression checks, no external native GIS runtime.

## Inventory and system boundary

- **30 installable Agent Skills**, all reachable directly from the core router except its own entrypoint and the cross-cutting Development Journal, which the entrypoints require.
- **188 curated project references, 52 public API catalog records and 24 data repository entries** are directory records, not guarantees of service availability or licensing.
- Browser `index.html` and `explorer.html` are **documentation/frontends**, not a GIS engine or an agent executor. The Project Wizard exports unapproved draft plans. The Python intent compiler creates a **non-executing** dependency-ordered preview. Native actions require a compatible authenticated hosting agent.
- The read-only [route graph auditor](../scripts/audit_skill_graph.py) inventories router reachability, lexical domain hints, free-first policy, and shared owners. It cannot run software or attest third-party behavior.

## Command, data and evidence ownership

| Phase | Owner | Receives → emits | Verification / limit |
| --- | --- | --- | --- |
| 1. Intent & authorization plan | [Prompt Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) | Original user request → task IDs, goal, plan revision, user-reviewed acceptance criteria | Local JSON records approval declarations, **not authenticated consent** |
| 2. Task routing | [GIS Development](../skills/gis-development/SKILL.md) | Reviewable task graph → explicit `owner_skill`, ordered dependencies | Missing domain owner is rejected; no execution from keyword hints |
| 3. Security and host preflight | [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md) | Data/provenance, side effects, candidate tools → scoped authorization checks and blockers | The real agent host enforces permissions |
| 4. Domain execution | Appropriate target specialist | Inputs, CRS/units, environment, reviewed task → changed code/files/artifacts | Must run via actual available tools and preserve originals |
| 5. Command and data diagnostics (conditional) | [UI Command Audit](../skills/gis-ui-command-audit/SKILL.md) + [GIS Data Flow Diagnostics](../skills/gis-data-flow-diagnostics/SKILL.md) | `command_id` and `trace_id`, import → engine → render → save/export → readback | Static/synthetic checks ≠ live GIS rendering |
| 6. Native UI, mobile and packaged tests (conditional) | [Desktop E2E](../skills/gis-desktop-e2e-testing/SKILL.md) / [Mobile](../skills/gis-mobile-application-development/SKILL.md) and relevant host owner | Compiled application and fixtures → observed GUI/device tests or explicit NOT RUN | Requires actual Windows/Android/iOS and licensed hosts as applicable |
| 7. Completion claim gate | [DoneProof](../skills/completion-evidence/SKILL.md) | Original plan + current-revision test observations → per-criterion status and gaps | Offline checker now rejects missing/rewritten criteria; does not authenticate logs |
| 8. History and handoff | [Agent Development Journal](../skills/agent-development-journal/SKILL.md) | Real step references, outcomes, errors → append-only records and recovery context | A journal entry is not evidence of execution |

See the canonical [repository-wide contract](../references/repository-wide-skill-orchestration.md) and [GIS UI quality handoffs](../references/gis-ui-quality-coordination.md).

## Weaknesses identified and corrected in this audit

| Risk | Before | Treatment |
| --- | --- | --- |
| Missing mobile keyword coverage | Lexical intent hints could miss a native mobile GIS request | Add Android/iOS/GNSS/offline-sync and Arabic mobile route hints |
| Conditional system permission routing | Device capabilities were present as a specialist but not directly hinted | Add limited permission/device text hints; host still confirms semantics |
| Malformed JSON plan data | Unsupported list/object types in status, risk and side-effect fields could cause Python TypeError | Validate types before membership checks; return actionable errors |
| Ambiguous task ownership | Workflow compiler silently selected the first eligible specialist if `owner_skill` was omitted | Require an explicit selected accountable owner; disallow shared-only owner |
| DoneProof report omission | Reports could say fully verified while silently dropping acceptance criteria from the original plan | Add strict `--plan` binding of request, revision, task and full criterion set; require plan for `--require-verified` |
| DoneProof unsafe/misleading JSON | Duplicate keys, non-finite JSON or impossible aggregate status could be accepted | Detect duplicate keys, reject non-finite values and oversized files, enforce aggregate status |
| Lossless trace could skip readback | A sender could omit READBACK from both events and required phases and still receive a structural PASS | Lossless reports must explicitly require IMPORT and READBACK; exported and reopened artifact IDs must agree when present |
| Routing drift | Existing validations checked links but not coherent domain hint, policy and shared-role coverage | Add read-only whole-pack ownership audit and invoke it in CI |
| Public-facing workflow outdated | Homepage compressed execute, test and report into one step; evidence gates were hard to find | Separate Execute, Prove and Handoff stages; update bilingual docs and clear navigation |

## Reproducible checks (offline, no account or paid API)

```bash
python scripts/audit_skill_graph.py
python scripts/build_manifest.py --check
python scripts/validate_catalog.py
python scripts/validate_api_catalog.py
python scripts/validate_skill_pack.py
python scripts/security_audit.py --root .
python scripts/verify_completion.py templates/doneproof.example.json
python -m unittest discover -s tests -v
```

To check a **real** completed task, the host must first perform relevant checks and independently inspect results, then run:

```bash
python scripts/verify_completion.py report.json --plan reviewed-plan.json --require-verified
```

Passing this Python command proves **structural consistency with a stated plan**, not that a user approved the plan, a reported log is genuine, an email was delivered, a native GIS renderer worked or the public website was deployed. Do not re-label T0/T1 as T2–T4.

## Publication verdict and unresolved validation boundaries

The repository's skills, documentation, example fixtures and static website are designed for **public source publication and guided use** after successful GitHub CI. This does **not** certify production operation in every supported runtime. Keep these distinct:

- **Offline structural CI:** observable through the matching GitHub Actions run for the released commit. Any red check blocks the merge.
- **Agent behavior:** paired live tests with and without DoneProof across compatible agent hosts are still required to measure behavioral improvements.
- **Host/device acceptance:** native ArcGIS Pro, ArcMap, QGIS desktop, Windows WebView2/UI Automation, Android/iOS device, offline sync, login, package installer and provider-side integrations require separate real target tests. Until performed, label each scenario **NOT RUN**.
- **Website release:** validate actual GitHub Pages deployment, keyboard/RTL/LTR behavior, mobile layout and catalog network loading in a browser; CI syntax checks alone cannot attest a deployment.
- **Licenses and external services:** upstream code and service terms must be reviewed for the exact version and use case. Catalog presence is not an execution proof or unrestricted license.

**No source GIS data, branding assets, footer/author identity or existing specialist implementation was intentionally replaced by this audit.**
