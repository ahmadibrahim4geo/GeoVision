# Archify integration: invocation, evidence and runtime

## Contents

- [Routing and user interaction](#routing-and-user-interaction)
- [Structured handoff](#structured-handoff)
- [Runtime and source provenance](#runtime-and-source-provenance)
- [Authoring and delivery](#authoring-and-delivery)
- [Evidence, language and failure handling](#evidence-language-and-failure-handling)

## Routing and user interaction

Use [GIS Architecture & Workflow Visualization](../skills/gis-architecture-visualization/SKILL.md) as a shared, conditional capability of the [main GIS router](../skills/gis-development/SKILL.md). The [intent orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) interprets the user's goal and relevant visualization purpose. Existing approval already covering the work remains valid; do not manufacture another confirmation loop. Follow the [repository-wide orchestration contract](repository-wide-skill-orchestration.md).

The host agent selects this skill for an explicit architecture/diagram explanation, a reviewed multi-component feature design, cross-component diagnosis that benefits from a map, or a structural change requiring updated documentation. Skip isolated text/color/icon edits and honor text-only requests. The user need not name Archify or prepare JSON. They ask in ordinary language, receive an HTML link and refine the result in the same conversation. A diagram viewer is not a background agent or an executable workflow engine.

Domain owners keep responsibility for software changes and real GIS tests. This specialist owns the diagram artifact. A diagnostic diagram is supporting material unless interactive HTML is a required user outcome. The [intent helper](../scripts/intent_router.py) emits conservative bilingual hints and validates declared handoffs; it cannot understand arbitrary language, launch Archify or authenticate approval.

## Structured handoff

Select `gis-architecture-visualization` in the normalized task's `skills`. Add this optional object to that task; it is required when this specialist is selected:

```json
{
  "visualization": {
    "purpose": "cross-component-diagnosis",
    "diagram_type": "dataflow",
    "evidence_basis": "source-reviewed",
    "language": "en"
  }
}
```

Allowed purposes: `architecture-explanation`, `feature-design`, `cross-component-diagnosis`, `structural-change`. Supported types: `architecture`, `workflow`, `sequence`, `dataflow`, `lifecycle`, `erd`, `class`, `tree`, `timeline`, `waterfall`. Evidence bases: `proposed`, `source-reviewed`, `runtime-observed`; languages: `en`, `ar`. Omit the object on tasks that need no diagram. Attach it to a diagram task depending on relevant findings, or retain the domain owner when visualization supports its existing task.

The compiled workflow carries a `visualization` handoff with renderer `archify`, an unverified runtime flag and the original evidence declaration. It does not add execution, network permissions or successful-test claims. Include request/task/revision, target, domain findings, input references, expected output location and acceptance checks in the host's actual handoff. Pass actual `command_id` and `trace_id` only when they exist.

Example: [reviewable plan](../examples/architecture-visualization/plan.example.json).

```bash
python scripts/intent_router.py --text "ارسملي مخطط يوضح مين مسؤول عن مين في المهارات" --format json
python scripts/intent_router.py --plan examples/architecture-visualization/plan.example.json --workflow --format json
```

The example remains draft/unapproved. `--gate` correctly blocks it; user-provided approval booleans cannot replace actual host authorization.

## Runtime and source provenance

Reviewed source: [tt-a1i/archify](https://github.com/tt-a1i/archify/tree/81c6be8d87090670c45d08a8f97f9d29c5af0bec), version **3.2.0**, commit **81c6be8d87090670c45d08a8f97f9d29c5af0bec**. Read its [skill](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/archify/SKILL.md), [package metadata](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/archify/package.json), [LICENSE](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/LICENSE), [NOTICE](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/NOTICE) and [third-party notices](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/THIRD_PARTY_NOTICES.md) before reuse. The main distribution is Apache-2.0 with historical/component exceptions; original integration instructions here remain MIT. Preserve embedded software notices in generated HTML. This pack does not relicense upstream code or require a paid rendering service/API key.

Archify is an **external runtime dependency**. Installing this GIS skill does not automatically install Node.js, Archify or a browser. First locate an actual installed `archify/bin/archify.mjs` (or `bin/archify.mjs` inside an installed skill); check the package's release/version and dependencies. Upstream declares Node.js >=18. A release version string alone does not verify the commit. Record the actual revision/hash used; review changed upstream instructions before using a different revision.

When no installation is available, the host may fetch the reviewed source into a new, approved tools directory using Git, with no global configuration changes:

```bash
git init <approved-tools-directory>/archify-source
git -C <approved-tools-directory>/archify-source remote add origin https://github.com/tt-a1i/archify.git
git -C <approved-tools-directory>/archify-source fetch --depth 1 origin 81c6be8d87090670c45d08a8f97f9d29c5af0bec
git -C <approved-tools-directory>/archify-source checkout --detach FETCH_HEAD
```

Replace placeholders with real authorized paths; never overwrite a pre-existing tools checkout. The host handles installation and tool permissions, so end users do not need to run these commands. No dependency is silently downloaded by the offline router. Existing approved task scope can cover setup; ask only if a genuinely new permission or scope is required.

## Authoring and delivery

Choose the smallest suitable diagram family. Read the installed upstream's `references/repository-authoring.md`, `references/authoring-defaults.md`, matching schema/example, and `references/delivery-contract.md` as applicable. Keep independent editable JSON and output files in the target project's authorized output directory. Preserve source evidence and label unknown/omitted boundaries. The committed [workflow candidate](../examples/architecture-visualization/skill-routing.workflow.json) is an independently authored explanation of this integration, not imported upstream example content.

For a real invocation, replace paths and type with actual values:

```bash
node <archify-runtime>/bin/archify.mjs finalize workflow <candidate.json> <output.html> --quality showcase --json
```

For code-backed candidates use the upstream's required `--repo-root <target-repository>` and matching repository evidence. `finalize` runs validate, deliver, strict artifact check and browser check. It requires a usable browser for its last stage; inspect the real receipt and nonzero exits. Keep the candidate unchanged while it runs. Retain receipts alongside the corresponding JSON/HTML; a new revision must not reuse stale browser evidence. Do not claim complete delivery on a failed/missing stage.

Respect the host's network policy. For a deliberately offline run, set `ARCHIFY_UPDATE_CHECK_DISABLED=1` in the process environment; upstream documents that this disables optional update-manifest requests and reminder-state writes. Do not silently enable network access or auto-install an update. Never bypass an OS/browser sandbox to satisfy a screenshot check.

## Evidence, language and failure handling

- Mark designs **proposed**, code-derived maps **source-reviewed**, and trace-supported paths **runtime-observed**. In a mixed map label claims individually; missing runtime evidence stays unknown. A source map or viewer path highlight does not prove an edge executed.
- Measure Timeline/Waterfall timings from actual logs or explicitly call them illustrative. Preserve field/CRS/unit semantics, transformations and lossless/lossy distinctions from the domain specialist.
- Retain secrets, private coordinates, credentials and internal paths outside public diagrams. Review sanitized source cards/links with [Security Guardian](../skills/gis-security-guardian/SKILL.md); HTML is shareable and can expose authored content. Distribution needs the user's actual authorization.
- Default to English. Upstream's [Arabic catalog](https://github.com/tt-a1i/archify/blob/81c6be8d87090670c45d08a8f97f9d29c5af0bec/archify/locales/README.md) covers viewer strings, but full RTL chrome/scene support, Arabic plural rules and independent native-language review remain limited. Apply [Arabic i18n](../skills/gis-internationalization-arabic/SKILL.md) and inspect actual requested Arabic output before claiming visual quality.
- Missing Node/runtime/browser: record the specific **BLOCKED / NOT RUN / PARTIAL** state; continue useful domain work when the diagram is optional. Provide a clearly labeled text/Mermaid explanation if helpful, without claiming Archify produced HTML. No unrelated GIS task should fail merely because this optional visualization dependency is missing.
- Keep diagram schema/render/browser/capture checks separate from GIS execution. [DoneProof](../skills/completion-evidence/SKILL.md) and the [Development Journal](../skills/agent-development-journal/SKILL.md) record actual evidence and outstanding tests. A valid diagram is not native ArcGIS/QGIS/Windows/Mobile proof.
