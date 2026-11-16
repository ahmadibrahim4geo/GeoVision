# How to Use GIS Development Skills

**Start on the website:** [How to Use](../index.html#how-to-use) · [Full Explorer](../explorer.html#get-started) · [العربية](HOW_TO_USE.ar.md)

GIS Development Skills is a collection of guidance files (`SKILL.md`), engineering references, tests and reusable GIS examples. **It is not a GIS program, an LLM subscription or a remotely running AI agent.** GIS products, native runtimes and external API access have separate requirements.

## Route A — Use with a supported coding agent

### 1. Choose a task and the real host
Decide the application and version (ArcGIS Pro/ArcMap, QGIS, desktop/WPF/Cesium, browser GIS, KML/KMZ, geoprocessing or remote sensing), operating system, data format, coordinate reference system (CRS), desired outputs and any licensing constraints. Prepare an existing working project with backup/version control.

### 2. Confirm prerequisites and install the core router
You need Node.js, permission to run the third-party [Skills CLI](https://github.com/vercel-labs/skills), and a **compatible coding agent** that can discover installed Agent Skills and access the authorized project. Run in a terminal inside your project:

```sh
npx skills add ahmadibrahim4geo/GIS-Development-Skills --skill gis-development
```

Check the CLI's version, installation prompts, scope and permissions before approving. Install a specialty only if your task needs it, for example:

```sh
npx skills add ahmadibrahim4geo/GIS-Development-Skills --skill kml-kmz-engineering
```

That convenient source command may copy only the selected nested skill in some
clients. Because the suite also uses shared root-level references and sibling
skills, the repository's verified complete path is to clone the source and
build its self-contained archive:

```sh
python scripts/build_skill_bundle.py --output-dir dist/skill-release
python scripts/build_skill_bundle.py --check dist/skill-release/gis-development-suite.zip
```

See the [distribution contract](SKILL_DISTRIBUTION.md). Installing the archive
in a named client still needs a client-specific smoke test; bundle validation
alone does not prove that integration.

The source repo is [GIS-Development-Skills](https://github.com/ahmadibrahim4geo/GIS-Development-Skills). The install command points to the **published main** branch; additions in an unmerged Pull Request may not yet be installed. Your AI host may have another installation method; consult its own documentation. Running `npx` does not automatically install ArcGIS, QGIS, a model or licenses.

For a fast proof-oriented first run, use the [golden QGIS coordinate audit](../examples/golden-qgis-coordinate-audit/README.md). Its offline result is T1; native QGIS T2–T4 remain NOT RUN until observed in that host.

### Prompt → goal → skill workflow (whole repository)

A compatible AI hosting agent should **understand the objective before picking libraries**: summarize the user's intended outcome, target application, testable success criteria, requirements, constraints and known unknowns. For substantial work, propose numbered tasks and receive scope-specific approval; the [Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) hands the reviewed task graph to the [GIS Router](../skills/gis-development/SKILL.md). Then domain specialists perform preflight, actual implementation and verification, accompanied by Security Guardian and journal evidence. [Workflow protocol](../references/repository-wide-skill-orchestration.md).

The [Project Wizard](../project-wizard.html) offers **local, bilingual goal and success-criteria inputs**, a Markdown plan preview, and a downloadable draft **JSON workflow** compatible with the offline Intent Router. JSON export requires both fields, and it is *not* an AI runtime, an executable command or an authenticated approval. The offline tool can turn a reviewed JSON plan into an ordered, **nonexecuting** preview:

```sh
python scripts/intent_router.py --text "Create a GIS desktop map that opens KMZ" --format json
python scripts/intent_router.py --plan examples/intent-orchestrator/example-plan.json --workflow --format json
```

The first command does not determine a semantic goal. The second consumes one explicitly authored by the host. No command authenticates the user or executes a task.

### Optional GIS UI quality specialists

When the goal is checking GIS software itself, select only the needed skills: [UI Command Audit](../skills/gis-ui-command-audit/SKILL.md) (buttons/handlers/icons), [Desktop E2E](../skills/gis-desktop-e2e-testing/SKILL.md) (real Windows/Qt/WebView2 behavior), [Data Flow Diagnostics](../skills/gis-data-flow-diagnostics/SKILL.md) (import→engine→export integrity), and [UI/UX Design & Testing](../skills/gis-ui-ux-design-testing/SKILL.md) (accessibility/visual/usability). They share [the common evidence contract](../references/gis-ui-quality-coordination.md). The [Project Wizard](../project-wizard.html) has an optional quality-focus choice and exports a draft; do not treat that choice as a native runtime test.

```sh
python scripts/gis_ui_command_audit.py --root examples/ui-command-audit/fixture
python scripts/gis_data_flow_diagnostics.py --trace examples/data-flow-diagnostics/trace.example.json
```

These two tools are **read-only, deterministic, standard-library T0/T1 checks** of synthetic fixtures. Real Windows UI Automation, app WebView2 bridge, GIS engine and installer evidence require suitable hosts, permissions, fixtures and T2–T4 checks.

### 3. Ask in natural language — review before editing
Example for a geospatial desktop application:

> Examine the KML/KMZ importer in my existing GIS project. Preserve imported icons, colors, folders and shared styles. Before changing any files, give me numbered tasks, risks, expected outputs and tests for approval. Then implement only approved changes and show evidence of actual working outputs. Keep a numbered development journal including any amendments, failures and rollback decisions. State which tests you could not run in the native application.

Your host should load [Prompt Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) and [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md) where installed, and route tasks using [GIS Development](../skills/gis-development/SKILL.md). A journal is persisted only if the host actually invokes [Agent Development Journal](../skills/agent-development-journal/SKILL.md). **A Skill file alone cannot authenticate user approval, observe every command or change code.**

### 4. Review deliverables before accepting “done”
Ask for the original requirement, approved plan revision and task IDs; changed paths or commit; test commands with observed pass/fail output; native ArcGIS/QGIS/desktop test results where possible; blocked or untested items; source/license provenance; and journal/handoff links. A green offline test does **not** prove the native GUI launched or a GIS toolbar works. For formal evidence use [GIS Acceptance Report](../templates/GIS_ACCEPTANCE_REPORT.md). Do not allow an agent to delete data, overwrite geometry, share secrets or perform remote actions without real host authorization.

## Route B — Use without any AI agent

1. Browse the [full skill directory](../explorer.html#all-skills), pick a specialist and read its `SKILL.md` and linked references.
2. Inspect [offline GIS examples](../examples/README.md) and starter templates (e.g. [ArcGIS Pro toolbox](../templates/arcgis-pro-toolbox.pyt), [ArcMap script tool](../templates/arcmap-script-tool.py) or [QGIS Processing example](../templates/qgis-processing-algorithm.py)).
3. Read [free-first licensing policy](../references/free-first-policy.md), and check the licenses of upstream code, models, datasets and API providers independently.
4. Work from a safe copy of your project; run the appropriate tests, inspect CRS, units, geometry and output fidelity, then commit changes to version control.

## Frequently asked questions

**I ran the install command; where is the ArcGIS tool?** The command installs instructions for a coding agent, not an executable GIS application. The agent still needs a task, authorized project access, actual tool execution, the relevant host and tests.

**Can I use it in Arabic?** Yes. Request a plan and implementation in Arabic; the examples and landing/explorer pages offer Arabic/English guides.

**Can the agent resume work after I close a conversation?** Only if it has persisted project state and can re-read it. The development journal provides numbered, linked records but requires the host to call the logger and verify source/test claims.

**Can it undo a previous step?** A new journal revision or rollback report records intent/history; actual reversion of code is a separate, authorized and tested action (usually via Git).

**Are all APIs and GIS hosts free?** No. Prefer offline and open-source paths where feasible. ArcGIS, Global Mapper and certain service tiers have vendor terms; a public reference is not a license grant.

**Where do I report an issue?** Open an [issue](https://github.com/ahmadibrahim4geo/GIS-Development-Skills/issues) with platform, version, reproduction steps, observed behavior and redacted test evidence.

This guide covers **how to use** the materials, not claims of live agent execution inside this website.

For a QGIS-based **standalone engine** rather than a hosted plugin, install `qgis-engine-integration` and read the [architecture guide](../references/qgis-engine-integration.md) and [native runtime example](../examples/qgis-engine/README.md). Pin QGIS/Qt/Python and review upstream licensing.

## Completion evidence for a reviewed plan

The [responsibility matrix](../references/repository-wide-skill-orchestration.md) defines who owns intake, routing, security, execution, native checks, DoneProof, and the journal. Check static reachability with `python scripts/audit_skill_graph.py`. For a real multi-step delivery retain the reviewed plan's `request_id`, `revision`, task IDs and verbatim acceptance criteria; DoneProof must match all of them:

```bash
python scripts/verify_completion.py report.json --plan reviewed-plan.json --require-verified
```

A passing consistency report cannot prove external sends, GUI execution or authentic host permissions. Label T2–T4 NOT RUN if the required native environment is unavailable.

## Find and integrate visual assets

Ask: “Find a suitable image for each option, use official logos for providers, broaden the internet search as needed, and integrate local assets with credits and real size checks.” The router selects [Visual Asset Discovery & Integration](../skills/visual-asset-discovery-integration/SKILL.md) and the relevant target UI/platform owner. See [copyable requests](../examples/visual-assets/README.md) and [source/optional skill choices](../references/visual-asset-sources.md). Existing tools perform search/download/generation; a skill is not an internet connection.

## Ask for an interactive diagram when useful

Ask the same coding agent: “Explain my project architecture and command/data paths as an interactive diagram.” The GIS router composes the [visualization specialist](../skills/gis-architecture-visualization/SKILL.md); the domain owner supplies facts, and the agent prepares the external Archify runtime within authorized scope and returns an HTML link. Open it to inspect components/routes and request revisions in the same conversation. Copy bilingual requests from the [homepage](../index.html#architecture-visualization). See [runtime, language and evidence limits](../references/archify-integration.md); diagram checks remain separate from GIS tests.
