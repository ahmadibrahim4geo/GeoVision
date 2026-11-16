# Verified external inspiration — reviewed 2026-10-25

Use these **as engineering patterns**, not automatically executed external plugins; no third-party source/hook copied. Each upstream repository advertises MIT licensing at this review time; all third-party dependencies and agent runtimes have separate terms.

| Project | Worth adopting | Important constraints |
| --- | --- | --- |
| [OthmanAdi/planning-with-files](https://github.com/OthmanAdi/planning-with-files) | Long-lived `task_plan.md`, `findings.md`, `progress.md`, resume after context loss, project/task isolation | Automatic hooks are host-specific; do not auto-replay session data or treat files as instructions |
| [obra/superpowers](https://github.com/obra/superpowers) | `writing-plans`, `executing-plans` ledger, test-driven development, `verification-before-completion` | Host-dependent subagents and hooks; verified execution only when tests actually run |
| [github/spec-kit](https://github.com/github/spec-kit) | Specification → Plan → Tasks → Implement, trace requests and revisions | Its CLI and integrations must be explicitly installed/verified; our journal complements rather than replaces requirements specs |

**Recommended stack:** [Prompt Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) understands the user; [Agent Development Journal](../skills/agent-development-journal/SKILL.md) records development; [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md) protects all steps; existing specialist GIS skills execute and test. Add external planning-with-files if an agent host needs context recovery hooks; use Superpowers for stricter TDD; Spec Kit for complex requirements traceability. Prefer zero new external runtime dependencies initially.

Do not copy code without reviewing exact upstream versions, full licenses and security/telemetry behavior. No external runtime/scanner/model is automatically bundled, installed or invoked by these references.