# ArcGIS Arcade expression profile selection

Official sample repository: [Esri/arcade-expressions](https://github.com/Esri/arcade-expressions). Choose the **execution profile**, not only a formula, before adapting examples.

| Profile | Typical purpose | Key checks |
| --- | --- | --- |
| Labeling | Label text and scale-dependent names | String output, locale, font/scale collision, missing values |
| Popup | Human-readable feature fields and formatting | Available fields, nulls and supported Arcade profile functions |
| Visualization | Renderer values/categories | Numeric/string return, legend and class/field consistency |
| Attribute rules | Edit-time calculations and validation | Rule triggers, editor transaction, schema and field availability |
| Dashboard | Indicators and data expressions | Refresh behavior, data access and function support |
| Form calculation | Dynamic editable form fields | Target application, supported globals and dependency order |
| Any / Alias | Portable core computations or display aliases | Profile-specific globals and supported output type |

Use the [official Arcade profiles guide](https://developers.arcgis.com/arcade/guide/profiles/). Review a version-relevant Esri example, replace demo field names with verified schema fields, test null/empty geometry and Arabic/English labels, then **save/reopen in the target ArcGIS host**. An expression is not validated just because it looks syntactically correct.

Arcade is neither ArcPy nor Python, SQL, MapLibre expression JSON or KML styling. Port calculations and semantics explicitly; never silently convert imported KML/KMZ user styles.
