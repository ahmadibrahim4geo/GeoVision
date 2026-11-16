# Practical guide: multilingual GIS and Arabic text

This guide explains **what an AI coding agent must know before changing text behavior**. Use it with [the internationalization skill](../skills/gis-internationalization-arabic/SKILL.md).

## Core concepts, in plain language

| Concept | Meaning | GIS example |
| --- | --- | --- |
| Internationalization (i18n) | Design the app so any language can be added without changing algorithms | Replace hard-coded toolbar text with translation keys |
| Localization (l10n) | Translate and adapt a specific locale | ar-EG Arabic interface, French decimal formatting |
| Unicode | Standard representation of characters | Preserve `القاهرة` in station attributes |
| UTF-8 | Byte encoding for Unicode | Save GeoJSON/KML and source files in UTF-8 |
| RTL/LTR | Right-to-left versus left-to-right layout | Arabic panel alignment versus coordinate entry |
| BiDi | Algorithm handling mixed writing directions | `المحطة A-12 (31.2, 30.1)` |
| Arabic shaping | Contextual glyph forms and joined letters | Properly joined map labels, not isolated letters |
| Locale | Language plus optional region | `ar-EG`, `en-US`, `fr-FR` |
| Field identifier versus alias | Stable storage key versus localized label | `Station_Name` / `اسم المحطة` |
| Normalization | Canonical Unicode representation | Consistent search comparisons without changing source |
| Collation | Language-aware sorting | Arabic place-name order |
| Transliteration | Rendering words in another script | Optional `Al Qahirah` alias, not a replacement for `القاهرة` |

## Why Arabic needs special attention

Arabic uses connected letters, contextual glyph shaping, combining diacritics and RTL layout. Mixing Arabic with Latin file names, GIS coordinates, numbers and parentheses invokes the Unicode Bidirectional Algorithm. A font supporting Arabic glyphs is necessary but may not be sufficient: the renderer must support shaping and BiDi. **Never reverse Arabic text manually.** Some mapping engines render text differently from HTML, and PDF export may require font embedding and a shaping-capable renderer.

### Good defaults

- Store source code, translation dictionaries and text interchange in UTF-8.
- Use `lang="ar"` and `dir="rtl"` on Arabic UI sections; isolate code/coordinates with `<bdi>` or `dir="ltr"` as appropriate.
- Keep internal GIS field names stable and ASCII-compatible where interoperability requires it; translate only aliases, labels and help text.
- Store original Arabic names unchanged; create optional normalized search keys separately.
- Test font fallback and actual renderer output for labels, layouts, legends and PDF exports.

## Example: stable field, localized label

```json
{
  "field": "Station_Name",
  "value": "محطة القاهرة",
  "aliases": {
    "ar-EG": "اسم المحطة",
    "en": "Station name",
    "fr": "Nom de la station"
  }
}
```

`value` is the source data. Changing UI language must not translate the underlying value.

## Example: safe HTML mixed-direction text

```html
<section lang="ar" dir="rtl">
  <label>اسم الملف</label>
  <bdi dir="auto">Cairo_Stations.kmz</bdi>
  <p>الإحداثيات: <bdi dir="ltr">31.2357, 30.0444</bdi></p>
</section>
```

## GIS file-format rules

- **GeoPackage / PostGIS:** prefer Unicode-capable storage; check client and driver settings.
- **GeoJSON:** encode UTF-8 and round-trip attribute values.
- **KML/KMZ:** escape XML; preserve Arabic names, descriptions, folder hierarchy and ExtendedData. Validate in the target GIS host.
- **Shapefile:** DBF text encoding and field-name limits can cause corruption; use explicit encoding and test `.cpg`, or choose GeoPackage.
- **CSV/Excel:** UTF-8 CSV import may need an explicit encoding/BOM workflow; XLSX should preserve Unicode strings.
- **Map labels and PDF:** verify joining, font fallback, punctuation and actual export visually.

## Arabic search without corrupting data

Search-only transformations can remove tatweel/optional diacritics and optionally fold alef variants or ya/alef-maqsura. These are *not* always semantically equivalent. Keep original data and make normalization rules configurable. Test French accented characters and other scripts too.

## AI-agent decision procedure

1. Ask which locales, platforms, formats and renderers are in scope.
2. Separate immutable identifiers, source attributes and translated presentation.
3. Choose appropriate localization frameworks (web Intl/i18n libraries, Qt translations, WPF localization, GIS-host alias support).
4. Check Unicode, encoding, shaping, BiDi, fonts, sorting and search.
5. Test real Arabic labels in the UI, attribute tables, KML/KMZ, GeoJSON, XLSX and exported maps.
6. Document known host/format limitations and measured performance; do not claim native compatibility from unit tests alone.

## Suggested acceptance strings

- `القاهرة`, `الإسكندرية`, `الأقصر`, `أسوان`
- `محطة A-12 (31.2357, 30.0444)`
- `إحداثيات الموقع: EPSG:4326`
- `مُحَمَّد` (combining diacritics)
- `français – Élévation`
- Long Arabic field descriptions, Arabic numerals and mixed punctuation

## Free-first upstream references

- [Unicode Bidirectional Algorithm — UAX #9](https://www.unicode.org/reports/tr9/)
- [Unicode Normalization — UAX #15](https://www.unicode.org/reports/tr15/)
- [Unicode CLDR](https://cldr.unicode.org/)
- [W3C Arabic Layout Requirements](https://www.w3.org/TR/alreq/)
- [W3C Internationalization](https://www.w3.org/International/)
- [ICU project](https://github.com/unicode-org/icu)
- [HarfBuzz shaping engine](https://github.com/harfbuzz/harfbuzz)
- [FriBidi bidirectional library](https://github.com/fribidi/fribidi)
- [GNU gettext](https://www.gnu.org/software/gettext/)
- [Qt internationalization](https://doc.qt.io/qt-6/internationalization.html)

These are technical references and candidates, not a claim that their libraries are installed or tested here.
