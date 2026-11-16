# MCP / native candidate routing (fictional offline fixture)

This example contains **simulated declarations**, not connected MCP servers or authenticated host permissions. It exercises selection, native fallback and blocked missing capabilities without executing any code:

```bash
python scripts/mcp_tool_router.py --plan examples/mcp-tool-routing/plan.example.json --inventory examples/mcp-tool-routing/inventory.example.json
python scripts/intent_router.py --plan examples/mcp-tool-routing/plan.example.json --workflow --mcp-inventory examples/mcp-tool-routing/inventory.example.json --format json
```

Expected: repository read uses the declared example MCP candidate; desktop UI uses a declared native fallback; GIS readback remains blocked because no such tool is advertised. All candidate outputs retain `can_execute: false`; they do not prove server availability. See [tool lifecycle](../../references/mcp-integration-workflow.md).
