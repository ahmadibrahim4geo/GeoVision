# Visual Asset Discovery & Integration — 27 October 2026

أضفنا مهارة عامة للبحث عن الصور المصغّرة والشعارات الرسمية والصور التوضيحية ولقطات الشاشة والأغلفة والخلفيات، وتجهيزها ودمجها في المشاريع. مثال التضاريس لا يقيّد نطاقها. تعمل من خلال أدوات البحث والتنزيل والتوليد الموجودة فعلًا لدى المضيف.

## Delivered behavior

- [Specialist](../skills/visual-asset-discovery-integration/SKILL.md): choose actual data capture, official marks, representative imagery, screenshots or custom illustration according to the visual brief; inspect originals and preserve source/brand terms.
- [Workflow and metadata](../references/visual-asset-workflow.md): local originals/variants, attribution, representation type, dimensions, hashes and platform ownership. No silent substitution of generic icons for requested imagery.
- [Extensible sources and related skills](../references/visual-asset-sources.md): official media, Openverse, Commons, brand catalogs and optional design/generation skills. Canvas Design is a reviewed composition reference; a platform-specific image-search entry is not assumed to be portable. No external skill, paid service or dependency was installed.
- [Local raster helper](../scripts/prepare_visual_asset.py): non-overwriting PNG/JPEG/WebP preparation with transparent padding, intentional photo cropping, scale variants and JSON readback; Pillow is optional. The helper has no search/download/generation capability.
- Main/lexical routing, UI/UX and Desktop/Web/Mobile handoffs, decision tree, free-first policy, bilingual README/How to Use, homepage, Explorer and generated inventory are synchronized.

## Observed checks

| Check | Observed result | Boundary |
| --- | --- | --- |
| Full `python -m unittest discover -s tests -v` | 356 tests passed after incorporating the current architecture-visualization update | T0/T1 fixtures and source checks |
| New visual asset tests | 11 passed, including actual local raster preparation/readback | Source preservation, padding/alpha, dimensions, hashes, corrupt input, crop/upscale refusal, metadata and bilingual hints |
| Catalog/API/skill-pack/manifest/graph validators | Passed; 34 skills, 188 references, 52 APIs, 24 data repositories | Static inventory and reachability |
| Security Guardian | 0 findings, 0 blocking | Repository scanner only; external scanners NOT RUN |
| Website preservation/syntax | Author/footer content unchanged relative to updated main; both inline JavaScript bodies passed Node syntax checks | No live target application UI claim |
| Isolated realistic asset task | Actual official Openverse compact logo and Commons cloud photo acquired and prepared; six raster variants independently read back | Source acquisition/local files, not application integration |

The isolated task produced light/dark 64/128-pixel logo variants and 96×64/192×128-pixel cloud-photo variants. The parent reviewer independently matched official logo bytes to the canonical Git blob, checked local variant SHA-256/dimensions/alpha-capable format and inspected the contact sheet. See the recorded readback summary in the development log. Original source pages: [Openverse brand resources](https://docs.openverse.org/meta/brand/index.html) and [Commons cloud file](https://commons.wikimedia.org/wiki/File:Cloudsandbluesky.jpg).

The exercise remains isolated from the user's applications. No Z-EARTH terrain model, screenshot, UI or package was modified. Real target UI integration and packaged offline acceptance remain **T3/T4 NOT RUN**; image-generation tools and the optional external Canvas Design skill were not executed. A skill guides the host's actual tools and cannot create internet access or confer permissions itself.
