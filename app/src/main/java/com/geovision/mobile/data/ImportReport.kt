package com.geovision.mobile.data

/**
 * High-level import report shown before/after importing a GIS source.
 * This is the first step toward a real Import Engine without changing the
 * current parser flow all at once.
 */
data class ImportReport(
    val sourceName: String,
    val sourceType: String,
    val layers: List<ImportLayerInfo>,
    val warnings: List<ImportWarning> = emptyList(),
    val errors: List<ImportError> = emptyList()
)

data class ImportLayerInfo(
    val id: String,
    val name: String,
    val geometryType: String?,
    val featureCount: Long?,
    val crs: LayerCrsInfo?,
    val status: ImportStatus,
    val message: String? = null
)

enum class ImportStatus {
    SUPPORTED,
    UNSUPPORTED,
    WARNING,
    FAILED,
    IMPORTED
}

data class LayerCrsInfo(
    val authority: String?,
    val code: String?,
    val name: String?,
    val isKnown: Boolean,
    val requiresUserSelection: Boolean = false
) {
    val displayName: String
        get() = when {
            authority != null && code != null -> "$authority:$code"
            !name.isNullOrBlank() -> name
            else -> "نظام إحداثيات غير معروف"
        }
}

data class ImportWarning(
    val code: String,
    val message: String
)

data class ImportError(
    val code: String,
    val message: String,
    val cause: String? = null
)
