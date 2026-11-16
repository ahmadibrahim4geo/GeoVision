# ArcGIS Pro tool development

Official reference: https://pro.arcgis.com/en/pro-app/latest/arcpy/geoprocessing_and_python/defining-a-tool-in-a-python-toolbox.htm

- Target **Python 3 supplied with the exact ArcGIS Pro release**, unless the user provides a validated clone environment. Do not install random GDAL/PyQt wheels into the managed ArcGIS Pro environment without checking binary compatibility.
- Python toolboxes use `Toolbox`, tool classes, `getParameterInfo`, `updateParameters`, `updateMessages` and `execute`; keep top-level code side-effect-free. See the official `.pyt` creator docs at https://pro.arcgis.com/en/pro-app/3.6/arcpy/geoprocessing_and_python/creating-a-new-python-toolbox.htm .
- `arcpy.Parameter` definitions must match expected path/datatype, direction, required/optional type, supported output types and filters.
- Validate parameter `altered` / `hasBeenValidated` behavior when implementing dynamic menus.
- Respect geoprocessing `arcpy.env` and license requirements, messages, cancellation patterns and workspace locks.
- Use `arcpy.mp` for ArcGIS Pro map and layout automation; never mechanically rename an `arcpy.mapping` operation.
- Unit/integration test outside ArcGIS Pro where possible, then run the actual `.pyt` in ArcGIS Pro before reporting it works.

**Safe starter:** `templates/arcgis-pro-toolbox.pyt` copies feature geometries/attributes to a new destination without mutating the source.

**Official resources:** https://github.com/Esri/arcpy ; https://github.com/Esri/arcgis-python-api

## Additional official Esri implementation examples

- [Esri ArcPy](https://github.com/Esri/arcpy) contains debugging guidance and official Python package versions per ArcGIS Pro release. Compare a local release JSON with installed packages via [the read-only checker](esri-python-environment-compatibility.md).
- [Pro SDK code snippets](https://github.com/Esri/arcgis-pro-sdk-snippets) provide C# add-in patterns targeting Pro 3.7/.NET 10; they are distinct from ArcPy Python toolbox APIs. Inspect actual SDK, QueuedTask and editing requirements.
- [Arcade examples](https://github.com/Esri/arcade-expressions) use their own [profile semantics](arcgis-arcade-profiles.md), not Python.
- [Parallel network analysis](https://github.com/Esri/large-network-analysis-tools), [CAD/BIM](https://github.com/Esri/cad-bim-scripts) and [elevation GP](https://github.com/Esri/elevation-gp-python) demonstrate useful algorithms but require ArcGIS host licenses and native validation.
