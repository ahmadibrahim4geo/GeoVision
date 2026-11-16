# Visual assets: source to shipped resource

Use [Visual Asset Discovery & Integration](../skills/visual-asset-discovery-integration/SKILL.md) for thumbnails, photographs, official marks, illustrations and screenshots. Apply the [global execution contract](repository-wide-skill-orchestration.md) and [free-first policy](free-first-policy.md); available host tools perform the actual search/download/render.

## Execution contract

1. Receive the approved visual brief: subjects, roles, exact slots, platform, density, theme and protected assets. Do not constrain the workflow to GIS or terrain.
2. Discover actual tools and choose retrieval, official assets, data rendering, screenshot capture or illustration. Build source candidates dynamically from this project, with no fixed provider list or mandatory search order. Inspect images embedded/linked in the current source website; use a suitable one directly after source/terms review. Search any other relevant sites when needed. Named services in the guide are optional examples. Inspect originals and images, not just search snippets; broaden sources and query languages when useful.
3. Retain the chosen original locally and record its origin, terms, review date and selection reason. A provider's documentation picture is not automatically reusable. Search/index licensing metadata needs original-source confirmation.
4. Prepare a new asset directory and variants; inspect the result at the real target dimensions. Use the helper below for PNG/JPEG/WebP rasters. For SVG, use the target platform's reviewed vector pipeline, exclude scripts/external references and verify the saved asset. This helper does not sanitize SVG.
5. Integrate the local asset through the platform owner: web relative imports; WPF/Qt resource/build inclusion; mobile density/resources. Preserve semantic alt labels, focus and selection behavior. Ship applicable attribution with the project.
6. Read the integrated files back, then run real target UI and packaged/offline checks where available. Preserve T2–T4 NOT RUN if not observed. Never equate a source link, mockup or unit test with native execution.

## Metadata contract

Create one JSON object per selected asset with:

- `asset_id`: stable lowercase kebab-case identifier.
- `role`: `thumbnail`, `photo`, `logo`, `screenshot`, `illustration`, `scientific-preview`, `cover` or `background`.
- `representation`: `actual-preview`, `illustrative`, `brand-mark`, `photograph`, `screenshot` or `generated`.
- `source_page` and `license_url`: original HTTPS pages without tokens/credentials; `creator`, `license`, `attribution`, `alt` and `intended_use`: nonempty factual text.
- `source_verified` and `license_verified`: host-observed reviews, never assertions inferred by the helper.
- Recommended: `source_file_url`, `retrieved_at`, provider/version, search terms, selection reason, attribution placement and capture/render settings.
- For `generated`: `generator` and `generation_reference` identify the actual tool/result. It must remain distinguishable from observed source data.
- For `scientific-preview`: `representation` must be `actual-preview` and `data_reference` must identify the actual dataset/provider and render/capture context.

Source-review flags are bookkeeping, **not authenticated consent or a license adjudication**. Record concrete evidence outside the helper. Do not put confidential data, device location or access tokens in public manifests. An original author's specified credit must survive processing; do not relicense external imagery as the repository's MIT code.

## Optional local raster helper

[prepare_visual_asset.py](../scripts/prepare_visual_asset.py) performs local preparation only. It has **no network or generator**. Requires Pillow in the active Python environment; inspect/install that dependency only under the host's existing authorization. Missing Pillow produces an actionable error and leaves source files alone.

Prepare reviewed metadata based on the contract above, then run from the skills repository:

```bash
python scripts/prepare_visual_asset.py --input /path/to/selected-original.png --metadata /path/to/asset-metadata.json --output-dir /path/to/project/assets/visual/model-preview --width 64 --height 64 --scales 1 2 --fit contain
```

The destination must not already exist. The helper rejects malformed required metadata, unreviewed source/license fields, corrupt/oversized/animated inputs, invalid dimensions, a logo/scientific-preview crop and an upscaled cover. It creates lossless PNG variants and `asset.json` containing source/variant SHA-256 hashes and dimensions; it strips EXIF while retaining alpha and an existing ICC profile. Contain never enlarges the source; it pads unused space. Use a sufficiently large original for high DPI.

For a thumbnail photograph, `--fit cover` is available with explicit cropping intent. It is not valid for a logo or factual scientific preview. SVG, GIF and video are handled by a suitable separate host pipeline.

## Failure and completion contract

| Situation | Required behavior |
| --- | --- |
| Search unavailable | Inspect authorized project assets; report the missing tool and a manual source path, without inventing results |
| Original license or brand identity uncertain | Leave candidate unselected; continue to an appropriate source |
| Only generic icons found for a requested thumbnail | Continue searching/rendering, or report blocked; no silent icon substitution |
| Actual model data unavailable | Explicit illustrative image only if that satisfies the approved brief; no factual preview claim |
| Download/generation failed | Record failure and preserve originals; no phantom local paths |
| UI/native host missing | Deliver source assets and preparation evidence with native acceptance NOT RUN |

The [source/skill guide](visual-asset-sources.md) separates image providers, host tools and optional installable skills.
