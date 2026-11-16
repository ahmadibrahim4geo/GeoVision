# Visual Asset Discovery & Integration — Professional Agent Skill

[![Benchmark](https://img.shields.io/badge/Benchmark-6%2F6%20Passed%20(100%25)-35D592?style=for-the-badge&logo=pytest&logoColor=white)](#benchmark-evaluation-suite)
[![Evidence](https://img.shields.io/badge/Evidence-DoneProof%20T0--T4-2582ED?style=for-the-badge&logo=shield&logoColor=white)](#test-evidence-boundary)
[![Zero-Hotlinking](https://img.shields.io/badge/Zero--Hotlinking-Enforced-172B3C?style=for-the-badge)](#the-solution-architecture)

> **Find, inspect, prepare and integrate authentic images, thumbnails, official logos, and factual previews into desktop, web, and mobile projects with verified licenses and local assets.**

---

## The Problem: Why Coding Agents Fail at Visual Assets

Standard LLM coding assistants (Cursor, Claude, Copilot, ChatGPT) struggle when asked to integrate visual assets, resulting in broken UIs and legal risks:

| Common AI Agent Failure | What Happens in Production | How This Skill Solves It |
| :--- | :--- | :--- |
| **Hotlinked CDN URLs** | Uses external URLs that fail when offline or hit 404/rate limits. | **Zero-Hotlinking:** Enforces local bundling and offline-first asset staging. |
| **Hallucinated Logos** | Prompts an image generator to draw an AI cartoon instead of an official brand mark. | **Official Integrity:** Strictly restricts logos to authoritative brand marks (`fit: contain`), disallowing AI approximations. |
| **Clipart as Scientific Data** | Uses a stock picture of generic mountains for a DEM elevation selector. | **Factual Previews:** Requires `actual-preview` with explicit `data_reference` (dataset/CRS/provider). |
| **License Blindness** | Copies uncredited images from Google Images or Pinterest without usage terms. | **Provenance Tracking:** Mandates creator, source page, license URL, and attribution in `asset.json`. |
| **Aspect Ratio & DPI Distortion** | Stretches images or dumps 5MB unoptimized JPEGs into 64px UI slots. | **Deterministic Raster Helper:** Automatically generates exact 1x & 2x Retina PNGs, strips EXIF, and keeps alpha. |

---

## 5-Second Quickstart for Any Agent

### Cursor IDE
Copy [`agents/cursor.mdc`](../../skills/visual-asset-discovery-integration/agents/cursor.mdc) to `.cursor/rules/visual-assets.mdc` in your target repository.

### Claude Desktop & Claude Code
Add the system prompt from [`agents/claude.md`](../../skills/visual-asset-discovery-integration/agents/claude.md) to your Claude Project instructions.

### CLI Raster Preparation Helper
Prepare lossless local variants, strip EXIF metadata, and emit `asset.json` with SHA-256 hashes:
```bash
python scripts/prepare_visual_asset.py \
  --input /path/to/source.png \
  --metadata /path/to/metadata.json \
  --output-dir assets/visual/my-preview \
  --width 64 --height 64 --scales 1 2 --fit contain
```

---

## Benchmark Evaluation Suite

The skill includes an automated, synthetic quantitative evaluation suite that can be run with zero network access:

```bash
python scripts/evaluate_visual_skill.py --strict
```

### Verified Benchmark Results

| Scenario ID | Test Scope | Verification Rule | Result |
| :--- | :--- | :--- | :---: |
| `EVAL-01` | **Brand Logo Integrity** | Enforces `representation: brand-mark`; forbids destructive cropping (`--fit cover`). | **PASS (100%)** |
| `EVAL-02` | **Scientific Preview** | Enforces `representation: actual-preview` and non-empty `data_reference`. | **PASS (100%)** |
| `EVAL-03` | **License Verification** | Rejects unverified terms (`license_verified: false`) and unknown licenses. | **PASS (100%)** |
| `EVAL-04` | **Density Scaling (1x/2x)** | Generates exact 64px (1x) and 128px (2x Retina) variants losslessly. | **PASS (100%)** |
| `EVAL-05` | **Cryptographic Readback** | SHA-256 hashes in `asset.json` strictly match generated file bytes. | **PASS (100%)** |
| `EVAL-06` | **JSON Injection Safety** | AST-level duplicate key rejection prevents parser discrepancy attacks. | **PASS (100%)** |

---

## Copyable Prompts

### English
> Find and integrate a suitable image for each option in this interface. Inspect the provider's own resources and broaden internet/image searches as needed. Use official logos for brand identities, actual data previews when required, and explicitly identified illustrations otherwise. Deliver local assets, the correct small-size/density variants, source credits and target UI loading evidence.

### العربية
> ابحث عن صورة مناسبة لكل اختيار في الواجهة وادمجها داخل المشروع. افحص صور المصادر الرسمية ووسّع البحث على الإنترنت حسب الحاجة. استخدم الشعارات الرسمية للجهات، ومعاينات البيانات الفعلية عند طلبها، وميّز الصور التوضيحية. جهّز الملفات محليًا بالأحجام المناسبة وسجّل المصادر والتراخيص واختبر ظهور الصور داخل البرنامج.

---

## Acceptance Scenarios

| Request | Expected Route and Output | Failure Caught |
| :--- | :--- | :--- |
| **Unlisted project provider with website images** | Inspect original on provider's site, review terms, and use directly. | Rejecting a source solely because it is unlisted in a static catalog. |
| **Small thumbnail for terrain models** | Actual model/data capture if factual; otherwise clearly labelled illustrative thumbnail. | Repeated generic mountain icons presented as real DEM model previews. |
| **Logos for service cards** | Official verified brand assets, contain/padding, credits, and correct local binding. | Invented/recolored marks, cropped lettering, or wrong organizations. |
| **Illustrations for non-GIS apps** | Topic-appropriate web search or available generation tools with explicit representation type. | Hardcoded terrain-only source selection. |
| **64-pixel thumbnail on 2× display** | Suitable original and 64/128-pixel variants; real size review. | Distortion, unreadable detail, or unsupported upscaling. |
| **Offline packaged project** | Shipped local resources and credit text; real load/readback test. | A hotlinked URL that only worked in a development browser. |
| **No search tool or uncertain license** | Honest missing capability, alternative reviewed source, or blocked asset. | Fabricated downloads, false success flags, or automatic paid subscriptions. |

---

## Test Evidence Boundary

* **Unit Tests & Integrity:** Run `python -m unittest discover -s tests -p 'test_visual_assets.py' -v` to verify deterministic local preparation, metadata rejection, and the benchmark evaluation suite.
* **T0–T4 Evidence:** Unit tests and benchmark passes represent T0/T1 local evidence. Live external provider downloads (T2), target UI rendering (T3), and packaged offline builds (T4) must be observed in the target host environment and recorded with [DoneProof](../../skills/completion-evidence/SKILL.md).
