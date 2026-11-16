# Geocoding, Place Search & Gazetteer Provider Selection (2026-10-25)

**Scope.** Text/address/place name → coordinates (**forward geocoding**), coordinates → named place (**reverse geocoding**), incremental typing suggestions (**autocomplete**), and authoritative/crowdsourced place-name catalogs (**gazetteers**). This is a curated **provider directory**, not a proxy, bundled API keys, or proof of successful live calls. Full sortable registry: [JSON catalog](free-geospatial-apis.json) filtered by `category=geocoding`.

## Providers ordered by cost/entitlement

| Priority | Provider | Licensing tier | Allowance / restrictions | Best use |
|---|---|---|---|---|
| 1 | [Pelias self-hosted](https://pelias.github.io/pelias/) | **Open-source self-hosted** | MIT software; import/storage/compute costs; **no free public Pelias API implied** | Production-independent geocoder, autocomplete |
| 2 | [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api) | **No-key, noncommercial** | Free within Open-Meteo fair-use quotas; **commercial use needs plan** | Cities, settlements/postcodes |
| 3 | [Nominatim (OSMF hosted)](https://operations.osmfoundation.org/policies/nominatim/) | **Public, heavily restricted** | Max **1 req/sec**, valid identifying User-Agent/Referer, attribution; **no autocomplete or generic white-label geocoding** | Occasional user-initiated searches only |
| 4 | [Photon (komoot demo)](https://github.com/komoot/photon) | **Public demo, or self-host** | Fair use, throttle/ban, **no SLA and no published daily cap** | Autocomplete for evaluation; self-host for production |
| 5 | [GeoNames](https://www.geonames.org/export/) | **Free username / quota** | **10,000 credits/day**, **1,000/hour**, CC BY + attribution, commercial permitted | Multilingual villages, wadis, mountains, administrative names |
| 6 | [Geoapify](https://www.geoapify.com/pricing/) | **Free key / quota** | **3,000 credits/day**, 5 req/sec; commercial Free plan **permitted within terms** | Geocoding, address autocomplete |
| 7 | [LocationIQ](https://locationiq.com/pricing) | **Free key / quota** | **5,000 requests/day**, 2 req/sec; commercial Free plan requires prominent attribution/link | Address/POI geocoding |
| 8 | [geocode.maps.co](https://geocode.maps.co/plans/) | **Free demo account** | Advertises **25,000 requests** at 5 req/sec, subsequently 1 req/sec; **NOT 25,000/day** | Low-volume OSM address geocoding |
| 9 | [Esri ArcGIS World Geocoding](https://developers.arcgis.com/rest/geocode/) | **Free non-stored allowance / metered** | **20,000 non-stored geocodes/month** under Location Platform; stored results billed; count results, not always requests | ArcGIS-compatible place/address lookup and suggests |
| 10 | [MapTiler](https://www.maptiler.com/cloud/pricing/) | **Free key / conditional plan** | Free plan currently lists **1,000 search sessions/month** and **100,000 API calls/month overall**; restrictions apply | MapLibre frontend search |
| 11 | [OpenCage](https://opencagedata.com/pricing) | **Free TEST trial only** | **2,500 requests/day**, 1 req/sec; **not for ongoing production** | Test provider fit; paid for production |
| 12 | [Google Places API (New)](https://developers.google.com/maps/billing-and-pricing/pricing) | **Commercial/metered** | Monthly operation/SKU-specific free usage caps may apply; billing/project key required | Business/POI text searches |
| 13 | [Mapbox Search Box](https://docs.mapbox.com/api/search/search-box/) | **Commercial/metered** | Session pricing/limited free allowance; Search Box results **temporary use only** | UI suggestions + retrieved places |
| 14 | [HERE Geocoding & Search](https://docs.here.com/geocoding-and-search/docs) | **Commercial/plan-based** | Verify account's current free/metered entitlement; different Autosuggest endpoint | Business, roads, addresses |
| 15 | [TomTom Search/Places](https://docs.tomtom.com/search-api/documentation/product-information/introduction) | **Commercial/plan-based** | Check free developer allowance; vendor recommends **Places Search** for new work | Fuzzy geosearch + POI |

**Not every request is one “search”.** An autocomplete box can issue multiple `/suggest` calls for one person's typed query (before debouncing), some services charge per *session*, *returned geocode*, *attribute*, or *SKU*. Daily quota also differs from per-second rate limits, monthly quotas, storage/caching rights, and terms of commercial use. Never pool or ship one developer key across end users against terms.

## World Gazetteer: visual reference names versus search

For ZEarth-style apps, map **Reference Layers > World Gazetteer** to the [scale-aware World Gazetteer guide](world-gazetteer-reference-layer.md) and [offline sample](../examples/gazetteer/README.md). Search can query GeoNames name/alias indices or permitted online services while the independent overlay queries bounded *visible* features at each detail level. No mass API calls triggered by camera movement, no implicit Esri appliance licensing, and no changes to user KML/KMZ symbol styles.

## Recommended routing for agent-generated GIS tools

1. **App and entitlement first:** identify academic/noncommercial vs commercial, offline vs hosted, user-managed credentials, required geographic coverage, latency and data retention.
2. **Search intent:** cities/postcodes → Open-Meteo; natural geographic features/Arabic alternate names → GeoNames; OSM address/POI → Geoapify or LocationIQ; search-as-you-type → a *permitted* autocomplete endpoint (Geoapify, Photon self-host or vendor-specific suggestion API); production without hosted quotas → Pelias/Photon self-host.
3. **Avoid OSMF-hosted Nominatim for autocomplete or auto-generated generic clients** even when the number of *human* searches sounds small. Only explicit, occasional user-requested lookup is acceptable under server policy.
4. **Use explicit priority + opt-in fallback:** do not send every query to all providers. Fallback only after permission/entitlement and terms are satisfied, preserve provider metadata, and avoid ranking cross-provider results as equivalent without source tags.
5. **Normalize for GIS:** `latitude` and `longitude` **WGS84 EPSG:4326**, noting GeoJSON `coordinates: [longitude, latitude]`; preserve provider place ID, original display name and language, feature kind, admin hierarchy, accuracy level, bbox when present, attribution and retrieved timestamp. Do not infer survey-grade precision.
6. **Multilingual Egyptian place names:** support Arabic/English display, transliteration variants (Cairo/القاهرة, Aswan/أسوان), `EG` country filter, optional bbox near Egypt, diacritic-insensitive search *without overwriting source spelling*, and disambiguate same-name locations.
7. **Operate safely:** debounce 300–500 ms for **permitted** autocomplete, minimum 3 typed characters where supported, cancellation and latest-request wins, timeout, bounded retries respecting `Retry-After`, provider-aware cache TTL only if contract allows, user-visible 401/403/429 and quota exhaustion, no secrets in JS bundles, error telemetry without query PII.
8. **Evaluation gate:** official documentation reviewed ≠ network endpoint tested ≠ GIS application integrated ≠ native ArcMap/ArcGIS Pro/Windows acceptance. Use offline fixture tests and a permissioned live smoke test before shipping.

## Request examples (documentation only; no provider endpoint was live-tested by this change)

**Free city search, no key, noncommercial:**

```text
GET https://geocoding-api.open-meteo.com/v1/search?name=Cairo&count=5&language=ar&countryCode=EG
```

**OSMF Nominatim, manual button-triggered lookup ONLY (not autocomplete):**

```text
GET https://nominatim.openstreetmap.org/search?q=Aswan&format=jsonv2&countrycodes=eg&limit=5
User-Agent: YourIdentifiableGISApp/1.0 (contact: project-support@example.org)
```

The sample `User-Agent` above is illustrative; replace with a valid, owned application identity and contact route.

**GeoNames free personal username:**

```text
GET https://secure.geonames.org/searchJSON?q=Aswan&country=EG&lang=ar&maxRows=5&username=YOUR_USERNAME
```

**Geoapify requires an individual key:**

```text
GET https://api.geoapify.com/v1/geocode/search?text=Cairo&filter=countrycode:eg&format=json&apiKey=YOUR_API_KEY
```

Keep secrets out of committed code, README URLs, logs, telemetry and public sites. Query arguments must be URL-encoded, parsed and checked per provider schema.

## ملاحظات بالعربية

- **مفتوح المصدر:** يمكنك تشغيل Pelias بنفسك، لكنه يحتاج بيانات وسيرفر، وليس API سحابيًا مجانيًا جاهزًا.
- **مجاني بشروط:** Nominatim مناسب للبحث اليدوي العرضي فقط، ولا يجوز توصيله بخاصية اقتراح الأسماء أثناء الكتابة على الخادم العام. Photon العام خادم تجريبي وليس خدمة مضمونة.
- **مجاني بحصة:** GeoNames وGeoapify وLocationIQ ومزودون آخرون لهم رصيد أو حد للطلبات. يجب مراجعة الترخيص التجاري ونسبة الاستخدام لكل مزود.
- **تجربة فقط:** OpenCage يمنح ٢٥٠٠ طلب يوميًا **للتجربة** وليس لتشغيل تطبيق إنتاجي مستمر.
- **تجاري:** Google Places وMapbox وHERE وTomTom تعرض خططًا مدفوعة؛ وجود رصيد شهري مجاني لا يعني إلغاء شروط الفوترة والاحتفاظ بالنتائج.

**Last provider-documentation review:** 2026-10-25. All hosted status values remain **unknown** until explicitly tested. Verify official pricing again at implementation/deployment.
