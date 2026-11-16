# External source references for the whole GIS Development Skills repository

This is a **curated reference list** for repository-wide skill routing, tool interfaces, safety, and verification. Keep GIS-first resources above generic agent frameworks. A linked source is not a required runtime dependency or evidence of integration. Existing skill specialists remain authoritative for their domain; do not fork or duplicate their instructions.

| Rank | Repository | Why it belongs | Reuse boundary |
| --- | --- | --- | --- |
| 1 | [opengeos/GeoAgent](https://github.com/opengeos/GeoAgent) | GIS-context-aware agent, structured geospatial tool registry, package adapters, approval hooks, QGIS/map/STAC context | **Already referenced** by [Geospatial AI Agents](../skills/geospatial-ai-agents/SKILL.md). Learn metadata and confirmation patterns; do not create another generic GeoAgent skill |
| 2 | [nkarasiak/qgis-mcp](https://github.com/nkarasiak/qgis-mcp) | Real QGIS tool bridge, separation between MCP server and QGIS plugin, explicit commands and health diagnostics | QGIS-specific adapter only; do not make its Python/QGIS runtime mandatory elsewhere |
| 3 | [agentskills/agentskills](https://github.com/agentskills/agentskills) | Skill specification, frontmatter validation, progressive disclosure, resources, skill discoverability | Align skill packaging and validation; preserve our specialist navigation and internal validators |
| 4 | [modelcontextprotocol/modelcontextprotocol](https://github.com/modelcontextprotocol/modelcontextprotocol) and [reference servers](https://github.com/modelcontextprotocol/servers) | Defined tool metadata, resource discovery, boundaries and client/server contracts | Optional integration standard; reference servers are illustrative and not production security guarantees |
| 5 | [langchain-ai/langgraph](https://github.com/langchain-ai/langgraph) | Stateful execution graphs, approval interrupts and resume, durable checkpoints, branching and controlled handoffs | Architecture inspiration only; do not impose LangGraph as a mandatory framework or hidden dependency |
| 6 | [promptfoo/promptfoo](https://github.com/promptfoo/promptfoo) | Prompt and agent evaluation, regression cases and adversarial prompt/security tests | Optional tests in a controlled fixture-based environment; protect secrets and manage external model costs |
| 7 | [sparkgeo/geo-mcp-servers](https://github.com/sparkgeo/geo-mcp-servers) | Curated discovery of GIS-focused MCP servers (QGIS, ArcGIS, STAC, PostGIS, geoprocessing) | Discovery index only; independently inspect upstream maintenance, licenses and authentication for each listed server |

## What we should implement in our own repository

1. **Intake contract**: original prompt, normalized intent, language, scope and a reviewed plan revision; do not jump straight to specialist execution.
2. **Skill registry**: for every installed skill, record ID, routing triggers, scope, input/output types, shared dependencies, available host tools, permissions and verified capabilities.
3. **Tool registry**: describe each executable tool using stable ID, input schema, expected side effects, data/CRS requirements, risk, approvals, return schema and failure states. Prefer adapters and existing APIs over rewriting upstream engines.
4. **Dependency graph**: router owns skill selection; shared security, CRS, data processing and verification apply as needed; domain specialists own their own outputs. Resolve collisions once.
5. **Execution checkpoints**: explicit host-side approval and safety preflight for consequential actions, then atomic/rollback-aware execution, test evidence and journal reporting.
6. **Regression cases**: good prompts, ambiguous Arabic/English prompts, multiple valid routing choices, changing approvals, missing engines, dangerous requests, failed tools, interrupted jobs, and false-positive completion checks.

## Keep these roles distinct

- Agent Skills specification defines **how instructions are packaged and discovered**, not native GIS processing.
- GeoAgent and QGIS-MCP demonstrate **GIS tool execution interfaces**; only QGIS-MCP requires the QGIS host for its real operations.
- MCP defines **tool communication**; it does not independently ensure authorization or input safety.
- LangGraph provides an **optional runtime implementation strategy** for the state machine; this repository's written workflow must remain readable and usable without it.
- Promptfoo provides **optional evaluation tooling**, not a permission enforcer.
- Existing specialized GIS skills must remain focused on domain algorithms and formats; avoid duplicating one specialist inside another.

## Upstream adoption review checklist

Before code reuse or installation, verify exact upstream source and release, license and transitive dependencies, maintenance activity, platform/runtime requirements, target user permissions, security audit, API stability, automated tests and clean-machine behavior. Record whether each item is **reference only**, **experimental adapter**, or **verified integration**. Source popularity alone is not evidence of compatibility.

See [the shared repository orchestration contract](repository-wide-skill-orchestration.md), [the GIS router](../skills/gis-development/SKILL.md), [the Agent Skills specialist](../skills/prompt-intent-orchestrator/SKILL.md) and [Geospatial AI Agents](../skills/geospatial-ai-agents/SKILL.md).

## Source verification snapshot — 2026-10-26

The eight distinct repositories named above were checked through GitHub repository metadata: all were publicly accessible and none was archived at review time. This is **availability verification only**, not a test of their tool execution, security posture, binary compatibility or integration with our skill pack.

| Source | Repository license metadata | Adoption status |
| --- | --- | --- |
| GeoAgent | MIT | Reference only; already linked by the GeoAI specialist |
| QGIS MCP | GPL-2.0 | Reference only; check redistribution and derived-work obligations before code reuse |
| Agent Skills | Apache-2.0 | Reference specification only |
| MCP specification and reference servers | NOASSERTION; upstream LICENSE documents a transition from MIT to Apache-2.0, plus CC-BY-4.0 documentation terms | Reference only; inspect exact file and contribution history before reusing code or specification text |
| LangGraph | MIT | Optional runtime architecture reference; not installed |
| Promptfoo | MIT | Optional testing framework; not installed |
| Geo MCP Servers Directory | MIT | Discovery index; each linked server requires its own review |

The license values are upstream **repository-level GitHub metadata**, not a transitive dependency license audit. No upstream code was copied and no package was installed as part of this reference review.

## Skill authoring and completion references — 2026-10-26

[OpenAI Skill Creator and Superpowers](skill-creator-superpowers-review.md) informed the [DoneProof](../skills/completion-evidence/SKILL.md) gate. Both remain **reference only**; no upstream code installed or copied, and no host-level tool permission inferred.
