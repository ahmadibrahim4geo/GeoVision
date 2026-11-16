# ArcMap legacy development

**ArcMap / ArcGIS Desktop 10.x is retired** (Esri lists retirement effective 2026-03-01). Use for maintenance or controlled legacy environments, not as a default for new installations. Source: https://desktop.arcgis.com/en/arcmap/latest/tools/data-management-toolbox/analyzetoolsforpro.htm

## Compatibility rules

- Typical ArcMap 10.x scripting environment is **Python 2.7 + ArcPy**. Verify the exact installed ArcMap release and Python environment.
- Script tools typically use `arcpy.GetParameterAsText(index)`, `arcpy.AddMessage` and legacy geoprocessing functions.
- For MXD layouts/map automation, `arcpy.mapping` is legacy; **ArcGIS Pro uses `arcpy.mp`**, not the same API.
- Avoid f-strings, type annotations, `pathlib`, dataclasses and modern Python 3-only packages in a Python 2.7 target.
- A `.pyt` file can be used on supported legacy versions, but **always test it in the actual release**. Do not assume an ArcGIS Pro `.pyt` runs in ArcMap.
- Keep new algorithmic logic and data contracts documented separately to support a later migration.

## Runbook

1. Record ArcMap version, interpreter path, installed extensions and tool license levels.
2. Define parameters and data types in the Script Tool properties in ArcCatalog/ArcToolbox.
3. Adapt `templates/arcmap-script-tool.py` and test one small fixture.
4. Review geoprocessing environment: overwriteOutput, extent, mask, scratchWorkspace, outputCoordinateSystem.
5. Use Esri's Analyze Tools For Pro migration guidance, then separately retest in ArcGIS Pro.

**Upstream samples:** https://github.com/arcpy/sample-gp-tools