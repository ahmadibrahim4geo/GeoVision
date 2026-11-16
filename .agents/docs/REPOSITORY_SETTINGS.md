# Repository profile and publication settings

This file records the recommended public profile. **GitHub repository About / Settings are not automatically set by a committed Markdown file.** An administrator must apply the values in GitHub's UI or by a separately authorized repository-management API.

## About (repository sidebar)

- **Description:** Open-source GIS agent skill & developer toolkit for ArcGIS Pro, ArcMap, QGIS, desktop/3D mapping, remote sensing and public geospatial APIs.
- **Website:** https://ahmadibrahim4geo.github.io/GIS-Development-Skills/
- **Topics** (select each as a separate tag): `gis`, `geospatial`, `gis-development`, `agent-skills`, `arcgis-pro`, `arcpy`, `arcmap`, `qgis`, `pyqgis`, `geospatial-api`, `remote-sensing`, `maplibre`, `cesiumjs`, `rustgis`, `open-source`
- **Releases:** use semantic version tags only after the tests and host-app limitations are documented.
- **About blurb / skill focus:** An installable AI coding-agent skill, verified source directory and starter templates for GIS development. It helps implement and test; it is not a substitute for ArcGIS, QGIS, GDAL or commercial data rights.

> Website availability must be verified after GitHub Pages deployment. Do not label the website live until the Pages build succeeds and it opens in a browser.

## How to apply the About settings

1. Visit https://github.com/ahmadibrahim4geo/GIS-Development-Skills .
2. On the right side of the repository, find **About** and click the gear icon.
3. Paste the Description above. Enter the Website URL. Add the topics individually.
4. Save changes.

These sidebar settings cannot be replaced by editing `README.md`, `SKILL.md` or `index.html`; those files have been updated independently.

## GitHub Pages (static index.html site)

The site is a static, dependency-free `index.html`. It uses `references/project-catalog.json` and `references/free-geospatial-apis.json` as its source data.

- Visit https://github.com/ahmadibrahim4geo/GIS-Development-Skills/settings/pages .
- Choose **Deploy from a branch** under **Build and deployment → Source**.
- Set **Branch: main**, **Folder: /(root)**, then **Save** if these options are not already set.
- Open the published URL and test that both directory tabs load their data; use the Arabic language switch, search and category filtering.
- For failures, inspect the repository's **Actions** tab for a workflow named **pages build and deployment**, or the current Pages workflow.
- Do not set a custom domain unless you own it and have set the required DNS records.
- Avoid switching to **GitHub Actions** as the source unless a custom deployment workflow is intentionally added and configured.

The presence of `has_pages: true` in GitHub repository metadata alone is **not** verification that the public URL is responding correctly.

## Social preview

A 1280 × 640 or compatible PNG/JPEG project banner can be uploaded manually through **Settings → General → Social preview → Edit**. Choose a clean version of the approved logo with a short label, `GIS Development Skills`. The `assets/logo.svg` file in the repo is the official vector reference; committing an image file alone does not change the social preview setting.

## Community / quality

- **Issues:** keep enabled for bug reports, GIS source suggestions and enhancement requests.
- **Discussions:** optional. Enable in **Settings → General → Features → Discussions** if you want an open community question and proposals space; only then add a Discussions link.
- **License:** MIT applies to this repository's own content; third-party components, data and hosted APIs keep their individual licensing.
- **CI:** keep `.github/workflows/validate.yml` enabled and require green tests before release.
