package com.geovision.mobile.data.geo

import kotlin.math.abs

data class ParseResult(
    val lat: Double,
    val lon: Double,
    val formatName: String
)

object FormatDetector {

    private val sanitizeRegex = Regex("[\\p{C}\\u200E\\u200F\\u202A-\\u202E\\u2060-\\u2069]")
    private val multiSpaceRegex = Regex("[ \\t]+")
    private val lineNumRegex = Regex("""^\d+\s*[:\)\.](?=\s|$)""")
    private val bulletRegex = Regex("""^[-•*]\s+""")

    /** Normalise Unicode typographic symbols to ASCII equivalents */
    private fun normalise(s: String): String = s
        .replace('\u2032', '\'')   // ′ → '
        .replace('\u2033', '"')   // ″ → "
        .replace('\u02B9', '\'')  // ʹ → '
        .replace('\u02BA', '"')   // ʺ → "
        .replace('\u00B4', '\'')  // ´ → '
        .replace('\u2018', '\'')  // ' → '
        .replace('\u2019', '\'')  // ' → '
        .replace('\u201C', '"')   // " → "
        .replace('\u201D', '"')   // " → "
        .replace("''", "\"")

    fun sanitize(line: String): String {
        val normalised = normalise(line)
        val step1 = normalised
            .replace(sanitizeRegex, "")
            .replace(multiSpaceRegex, " ")
            .trim()
        return step1
            .replace(lineNumRegex, "")
            .replace(bulletRegex, "")
            .trim()
    }

    fun detect(line: String, utmZone: Int = 36, utmHemisphere: Char = 'N'): ParseResult? {
        var cleaned = sanitize(line)
        if (cleaned.isBlank()) return null

        tryUTM(cleaned, utmZone, utmHemisphere)?.let { return it }
        tryDMS(cleaned)?.let { return it }
        tryDDM(cleaned)?.let { return it }
        tryDD(cleaned)?.let { return it }

        val fuzzy = cleaned.replace(Regex("""[^-?\d.,; \t°'\"NESWnesw]"""), " ").replace(multiSpaceRegex, " ").trim()
        if (fuzzy != cleaned) {
            tryUTM(fuzzy, utmZone, utmHemisphere)?.let { return it }
            tryDMS(fuzzy)?.let { return it }
            tryDDM(fuzzy)?.let { return it }
            tryDD(fuzzy)?.let { return it }
        }

        return parseByTokenisation(cleaned, utmZone, utmHemisphere)
    }

    // ─── token-based fallback ───────────────────────────────────────────
    private fun parseByTokenisation(input: String, defaultZone: Int, defaultHemisphere: Char): ParseResult? {
        val tokens = input.split(Regex("""[,;\s]+""")).filter { it.isNotBlank() }
        if (tokens.size < 2) return null

        // try to extract two numeric values ignoring direction suffixes
        fun extractValue(t: String): Pair<Double, String>? {
            val cleaned = t.trim().removeSuffix("°")
            val lastChar = cleaned.lastOrNull()
            val dirSuffix = if (lastChar != null && lastChar.uppercaseChar() in "NESW") {
                lastChar.uppercaseChar().toString()
            } else ""
            val numeric = if (dirSuffix.isNotEmpty()) cleaned.dropLast(1).trim() else cleaned
            val v = numeric.toDoubleOrNull() ?: return null
            return v to dirSuffix
        }

        val vals = tokens.mapNotNull { extractValue(it) }
        if (vals.size < 2) return null

        // if any token has a direction letter, use it
        val dirs = vals.map { it.second }.filter { it.isNotEmpty() }
        if (dirs.size >= 2) {
            val firstDir = dirs[0].first()
            val secondDir = dirs[1].first()
            // try UTM: pattern like 38N 669501.120 2728983.436
            if (tokens.size >= 3) {
                val firstToken = tokens[0].trim()
                val zoneMatch = Regex("""(\d{1,2})\s*([NnSs])""").find(firstToken)
                if (zoneMatch != null) {
                    val zone = zoneMatch.groupValues[1].toIntOrNull()
                    val hemi = zoneMatch.groupValues[2].uppercase().first()
                    val easting = vals[1].first
                    val northing = vals[2].first
                    if (zone != null && zone in 1..60 && easting in 100000.0..999999.0 && northing >= 0) {
                        try {
                            val (lat, lon) = CoordinateConverter.utmToDd(zone, hemi, easting, northing)
                            if (lat in -90.0..90.0 && lon in -180.0..180.0) return ParseResult(lat, lon, "UTM")
                        } catch (_: Exception) {}
                    }
                }
            }

            val isLonFirst = firstDir in "EW" && secondDir in "NS"
            val latVal = if (isLonFirst) vals[1].first else vals[0].first
            val lonVal = if (isLonFirst) vals[0].first else vals[1].first
            val latDir = if (isLonFirst) secondDir else firstDir
            val lonDir = if (isLonFirst) firstDir else secondDir

            val lat = if (latDir in "Ss") -abs(latVal) else abs(latVal)
            val lon = if (lonDir in "Ww") -abs(lonVal) else abs(lonVal)
            if (lat in -90.0..90.0 && lon in -180.0..180.0) return ParseResult(lat, lon, "DD")
        }

        // fallback: two raw numbers
        val v0 = vals[0].first
        val v1 = vals[1].first
        if (v0 in -90.0..90.0 && v1 in -180.0..180.0) return ParseResult(v0, v1, "DD")
        if (v1 in -90.0..90.0 && v0 in -180.0..180.0) return ParseResult(v1, v0, "DD")

        return null
    }

    // ─── DD ──────────────────────────────────────────────────────────
    private fun tryDD(input: String): ParseResult? {
        val regex = Regex(
            """^\s*([NnSsEeWw]?\s*)?(-?\d{1,3}\.\d*)\s*[°]?\s*([NnSsEeWw])?\s*[,;]\s*""" +
            """([NnSsEeWw]?\s*)?(-?\d{1,3}\.\d*)\s*[°]?\s*([NnSsEeWw])?\s*$"""
        )
        val match = regex.find(input) ?: return null
        val g = match.groupValues
        val dir1 = g[1].takeIf { it.isNotBlank() }?.trim()?.firstOrNull()
            ?: g[3].takeIf { it.isNotBlank() }?.firstOrNull()
        val raw1 = g[2].toDoubleOrNull() ?: return null
        val dir2 = g[4].takeIf { it.isNotBlank() }?.trim()?.firstOrNull()
            ?: g[6].takeIf { it.isNotBlank() }?.firstOrNull()
        val raw2 = g[5].toDoubleOrNull() ?: return null

        val firstIsLon = dir1 != null && (dir1 == 'E' || dir1 == 'e' || dir1 == 'W' || dir1 == 'w')
        val secondIsLat = dir2 != null && (dir2 == 'N' || dir2 == 'n' || dir2 == 'S' || dir2 == 's')

        val latDir: Char?; val rawLat: Double; val lonDir: Char?; val rawLon: Double
        if (firstIsLon && secondIsLat) {
            latDir = dir2; rawLat = raw2; lonDir = dir1; rawLon = raw1
        } else {
            latDir = dir1; rawLat = raw1; lonDir = dir2; rawLon = raw2
        }

        val lat = when {
            latDir != null && (latDir == 'S' || latDir == 's') && rawLat > 0 -> -rawLat
            latDir != null && (latDir == 'N' || latDir == 'n') && rawLat < 0 -> -rawLat
            else -> rawLat
        }
        val lon = when {
            lonDir != null && (lonDir == 'W' || lonDir == 'w') && rawLon > 0 -> -rawLon
            lonDir != null && (lonDir == 'E' || lonDir == 'e') && rawLon < 0 -> -rawLon
            else -> rawLon
        }
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return ParseResult(lat, lon, "DD")
    }

    // ─── DMS ─────────────────────────────────────────────────────────
    private fun tryDMS(input: String): ParseResult? {
        val regex = Regex(
            """([NnSsEeWw]?\s*)?(\d{1,3})°(\d{1,2})['′](\d{1,2}\.?\d*)"\s*([NnSsEeWw])?\s*[,;]?\s*""" +
            """([NnSsEeWw]?\s*)?(\d{1,3})°(\d{1,2})['′](\d{1,2}\.?\d*)"\s*([NnSsEeWw])?"""
        )
        val match = regex.find(input) ?: return null
        val g = match.groupValues
        val dir1 = (g[1].takeIf { it.isNotBlank() }?.trim()?.firstOrNull() ?: g[5].firstOrNull()) ?: return null
        val dir2 = (g[6].takeIf { it.isNotBlank() }?.trim()?.firstOrNull() ?: g[10].firstOrNull()) ?: return null
        val firstIsLon = dir1 == 'E' || dir1 == 'e' || dir1 == 'W' || dir1 == 'w'
        val secondIsLat = dir2 == 'N' || dir2 == 'n' || dir2 == 'S' || dir2 == 's'

        val latDir: Char; val latD: String; val latM: String; val latS: String
        val lonDir: Char; val lonD: String; val lonM: String; val lonS: String
        if (firstIsLon && secondIsLat) {
            latDir = dir2; latD = g[7]; latM = g[8]; latS = g[9]
            lonDir = dir1; lonD = g[2]; lonM = g[3]; lonS = g[4]
        } else {
            latDir = dir1; latD = g[2]; latM = g[3]; latS = g[4]
            lonDir = dir2; lonD = g[7]; lonM = g[8]; lonS = g[9]
        }

        val lat = CoordinateConverter.dmsToDd(latD.toIntOrNull() ?: return null, latM.toIntOrNull() ?: return null, latS.toDoubleOrNull() ?: return null, latDir)
        val lon = CoordinateConverter.dmsToDd(lonD.toIntOrNull() ?: return null, lonM.toIntOrNull() ?: return null, lonS.toDoubleOrNull() ?: return null, lonDir)
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return ParseResult(lat, lon, "DMS")
    }

    // ─── DDM ─────────────────────────────────────────────────────────
    private fun tryDDM(input: String): ParseResult? {
        val regex = Regex(
            """([NnSsEeWw]?\s*)?(\d{1,3})°([\d.]+)'\s*([NnSsEeWw])?\s*[,;]?\s*""" +
            """([NnSsEeWw]?\s*)?(\d{1,3})°([\d.]+)'\s*([NnSsEeWw])?"""
        )
        val match = regex.find(input) ?: return null
        val g = match.groupValues
        val dir1 = (g[1].takeIf { it.isNotBlank() }?.trim()?.firstOrNull() ?: g[4].firstOrNull()) ?: return null
        val dir2 = (g[5].takeIf { it.isNotBlank() }?.trim()?.firstOrNull() ?: g[8].firstOrNull()) ?: return null
        val firstIsLon = dir1 == 'E' || dir1 == 'e' || dir1 == 'W' || dir1 == 'w'
        val secondIsLat = dir2 == 'N' || dir2 == 'n' || dir2 == 'S' || dir2 == 's'

        val latDir: Char; val latD: String; val latM: String
        val lonDir: Char; val lonD: String; val lonM: String
        if (firstIsLon && secondIsLat) {
            latDir = dir2; latD = g[6]; latM = g[7]
            lonDir = dir1; lonD = g[2]; lonM = g[3]
        } else {
            latDir = dir1; latD = g[2]; latM = g[3]
            lonDir = dir2; lonD = g[6]; lonM = g[7]
        }

        val lat = CoordinateConverter.ddmToDd(latD.toIntOrNull() ?: return null, latM.toDoubleOrNull() ?: return null, latDir)
        val lon = CoordinateConverter.ddmToDd(lonD.toIntOrNull() ?: return null, lonM.toDoubleOrNull() ?: return null, lonDir)
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return ParseResult(lat, lon, "DDM")
    }

    // ─── UTM ─────────────────────────────────────────────────────────
    private fun tryUTM(input: String, defaultZone: Int = 36, defaultHemisphere: Char = 'N'): ParseResult? {
        // Pattern: 38N 669501.120E 2728983.436N  (or with spaces, with/without E/N suffix)
        val fullRegex = Regex(
            """(\d{1,2})\s*([NnSs])\s+([\d.]+)\s*[Ee]?\s+([\d.]+)\s*[NnSs]?"""
        )
        val match = fullRegex.find(input)
        if (match != null) {
            val g = match.groupValues
            val zone = g[1].toIntOrNull() ?: return null
            val hemisphere = g[2].uppercase().first()
            val easting = g[3].toDoubleOrNull() ?: return null
            val northing = g[4].toDoubleOrNull() ?: return null
            if (zone !in 1..60 || easting !in 100000.0..999999.0 || northing < 0) return null
            try {
                val (lat, lon) = CoordinateConverter.utmToDd(zone, hemisphere, easting, northing)
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
                return ParseResult(lat, lon, "UTM")
            } catch (_: Exception) {
                return null
            }
        }

        val shortRegex = Regex("""^\s*([\d.]+)\s+([\d.]+)\s*$""")
        val shortMatch = shortRegex.find(input)
        if (shortMatch != null) {
            val easting = shortMatch.groupValues[1].toDoubleOrNull() ?: return null
            val northing = shortMatch.groupValues[2].toDoubleOrNull() ?: return null
            if (easting !in 100000.0..999999.0 || northing < 0) return null
            try {
                val (lat, lon) = CoordinateConverter.utmToDd(defaultZone, defaultHemisphere, easting, northing)
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
                return ParseResult(lat, lon, "UTM")
            } catch (_: Exception) {
                return null
            }
        }

        return null
    }
}
