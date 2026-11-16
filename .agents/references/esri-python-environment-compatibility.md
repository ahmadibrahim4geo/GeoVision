# ArcGIS Pro Python environment compatibility: read-only procedure

The official [Esri ArcPy Python distribution file](https://github.com/Esri/arcpy/blob/main/docs/arcgis-dist.json) documents package pins by ArcGIS Pro release. Use its original local JSON and record its revision. It is reference metadata, **not** a command to reinstall or a live installed-environment report.

## Workflow

1. Identify the actual ArcGIS Pro release and the exact Python interpreter used by the target toolbox.
2. Download the official Esri JSON to a local trusted file named arcgis-dist.json. Use that interpreter to run a pip list snapshot, or execute the checker from the interpreter you want to inspect.
3. Compare locally, without a network call or writes to the environment:

    python scripts/check_arcgis_python_env.py --distribution-json arcgis-dist.json --release 3.7 --installed-json installed.json

    python scripts/check_arcgis_python_env.py --distribution-json arcgis-dist.json --release 3.7 --installed-json installed.json --packages numpy,scipy

To obtain the snapshot from the target interpreter: python -m pip list --format=json > installed.json. Replace python with the **actual target executable**. A release key and selected packages must appear in the downloaded Esri JSON; the checker fails closed if they do not.

Exit code 0 means all compared package versions match exactly; 1 means missing/mismatched packages; 2 means invalid input. The default compares all documented packages except the pseudo-package release marker arcgispro. A mismatch may reflect an intentional environment clone. Do not automatically run pip install or overwrite the Esri-managed arcgispro-py3 environment, and never equate a matching report with successful ArcGIS-native toolbox execution. Run host-native import, parameter dialog, and geoprocessing tests separately.
