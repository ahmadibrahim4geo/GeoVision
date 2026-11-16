# DoneProof: claim-to-evidence verification protocol

Applies to every specialist and general-purpose agent task. **Intent Orchestrator** owns requirements and revision; **GIS Router** owns routing; **Security Guardian** and host own approvals; specialists own execution; **DoneProof** owns the pre-completion evidence check; **Development Journal** records actions. See [DoneProof](../skills/completion-evidence/SKILL.md) and [repository-wide workflow](repository-wide-skill-orchestration.md).

## Decision gate

For **each** required acceptance criterion: identify a sufficient real observation, run/inspect a fresh authorized check against the current revision, preserve a verifiable log/receipt/artifact reference, and classify it as verified, partial, failed, blocked or not_run. Do not accept another agent's assertion or a generated plan as execution proof. If any required criterion is incomplete, do not claim the whole task is complete. After material edits, rerun affected checks.

Native GIS and desktop tests: T0/T1 unit/static work does not prove T2 engine execution, T3 GUI interaction, or T4 packaged clean-machine operation. Continue to use the [GIS acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md) and [GIS evidence validator](../scripts/validate_evidence.py).

## JSON report v1 (optional for complex tasks)

Top-level: `schema_version: "1.0"`, nonempty `task_id`, reviewed plan `request_id`, current `revision`, `overall_status`, and a nonempty `criteria` list. For plan-bound release verification, `request_id` is required; older structure-only reports without it remain readable.

Each criterion: unique `id`, clear `statement`, `status` (`verified|partial|failed|blocked|not_run`), and `evidence` list. For `partial|blocked|not_run`, `reason` is required. For `verified`, passing evidence is required without failed observations; for `failed`, failing evidence is required; `not_run` cannot include observed evidence.

Each observation: `type` (`command_output|artifact_readback|provider_receipt|native_interaction|manual_review`), `reference` (path/commit/log/receipt; no secrets), `observed` (observed facts), `result` (`pass|fail`) and `revision` (matches report). **Overall verified requires every criterion verified.**

| Claim | Appropriate check | Not enough |
| --- | --- | --- |
| File updated | Readback and diff | Agent text |
| Bug fixed | Relevant passing test / real runtime when needed | Linter alone |
| GitHub merged | Observe remote target branch commit | Local commit or open PR |
| Email sent | Provider send result | Draft |
| GUI feature works | Live UI + persisted state/readback | Screenshot or mocks alone |
| Installer ready | Install and run in clean target environment | Package filename |

The [stdlib offline consistency checker](../scripts/verify_completion.py) cannot prove that a claimed command ran or a log is genuine. Host tools must independently verify sources and authorization. No fabricated evidence; never put private credentials in public reports. A simple read-only answer does not need a JSON file.

The [sample report](../templates/doneproof.example.json) is **illustrative**; its passing observation is not a real run. Run `python scripts/verify_completion.py report.json --plan reviewed-plan.json --require-verified` for the release-claim consistency gate. The checker rejects an omitted/reworded/extra criterion or mismatched request/task/revision. Passing the checker is not permission or genuine external evidence. The host must inspect referenced observations.
