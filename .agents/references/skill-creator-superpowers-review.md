# Upstream review — OpenAI Skill Creator and Superpowers

Reviewed on 2026-10-26. **Reference only:** no external skill installed, executed, vendored or silently enabled. Check licenses and exact upstream versions again before future code reuse.

| Upstream | Useful method | Adopted here | Reuse boundary |
| --- | --- | --- | --- |
| [OpenAI Skill Creator](https://github.com/openai/skills/blob/main/skills/.system/skill-creator/SKILL.md) | Explicit skill triggers; concise SKILL.md; progressive disclosure; examples, validation and iterative evaluations | Precise completion trigger, compact instructions, separate protocol and negative tests | Upstream skill LICENSE.txt: Apache-2.0; ideas only |
| [Superpowers: Verification Before Completion](https://github.com/obra/superpowers/blob/main/skills/verification-before-completion/SKILL.md) | Identify needed check, run it fresh, inspect output, report evidence before claiming success | Per-criterion evidence gate and revision-linked observations | Repository LICENSE: MIT; ideas only |
| [Agent Skills spec](https://agentskills.io/specification) | Frontmatter, names, references and optional scripts | Compatible packaging | Specification reference only |

OpenAI Skill Creator is a **skill authoring process**, while Superpowers supplies a **work verification process**; neither automatically enforces our host permissions. The local [DoneProof protocol](doneproof-verification-protocol.md) integrates with the current Intent Router, Security Guardian, GIS T0–T4 rubric and Journal without a new runtime dependency.

**Evaluation plan:** run paired real agent tasks with/without DoneProof (success, partial, failed, blocked, stale-revision, false GitHub merge, missing native GIS host, and fake tool results); compare unsupported success claims. Tests of a JSON validator are NOT an experiment proving behavior change.
