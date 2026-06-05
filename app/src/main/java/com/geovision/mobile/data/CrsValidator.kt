package com.geovision.mobile.data

/**
 * Lightweight CRS safety checks for imported layers.
 *
 * The current app still parses many sources directly into memory; this object
 * gives that flow a clear warning signal until the full Import Engine and
 * user-driven CRS selection are implemented.
 */
object CrsValidator {
    private val epsgRegex = Regex("""EPSG[:\s]*(\d+)""", RegexOption.IGNORE_CASE)

    fun inspect(crs: String?): LayerCrsInfo {
        val normalized = crs?.trim().orEmpty()
        if (normalized.isBlank()) {
            return LayerCrsInfo(
                authority = null,
                code = null,
                name = null,
                isKnown = false,
                requiresUserSelection = true
            )
        }

        val epsg = epsgRegex.find(normalized)?.groupValues?.getOrNull(1)
        if (epsg != null) {
            return LayerCrsInfo(
                authority = "EPSG",
                code = epsg,
                name = normalized,
                isKnown = true,
                requiresUserSelection = false
            )
        }

        val upper = normalized.uppercase()
        val isDefaultKnown = upper.contains("KML DEFAULT") ||
            upper.contains("GPX DEFAULT") ||
            upper.contains("GEOJSON DEFAULT") ||
            upper.contains("WGS84") ||
            upper.contains("WGS 84")

        val isCalculated = normalized.contains("محسوب", ignoreCase = true) ||
            upper.contains("CALCULATED") ||
            upper.contains("ASSUMED")

        return LayerCrsInfo(
            authority = if (isDefaultKnown) "EPSG" else null,
            code = if (isDefaultKnown) "4326" else null,
            name = normalized,
            isKnown = isDefaultKnown && !isCalculated,
            requiresUserSelection = !isDefaultKnown || isCalculated
        )
    }

    fun warningFor(crs: String?): ImportWarning? {
        val info = inspect(crs)
        if (info.isKnown && !info.requiresUserSelection) return null

        return ImportWarning(
            code = "CRS_REQUIRES_REVIEW",
            message = if (crs.isNullOrBlank()) {
                "نظام الإحداثيات غير معروف. يجب اختيار CRS قبل الاعتماد على مكان الطبقة."
            } else {
                "نظام الإحداثيات يحتاج مراجعة: ${info.displayName}. لا تعتبر الطبقة مؤكدة الموقع قبل التحقق."
            }
        )
    }

    fun appendWarning(existing: String?, crs: String?): String? {
        val crsWarning = warningFor(crs)?.message ?: return existing
        return listOfNotNull(existing?.takeIf { it.isNotBlank() }, crsWarning).joinToString("\n")
    }
}

