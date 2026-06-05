package com.geovision.esri.viewer

enum class EsriPackageType {
    MMPK,
    MOBILE_GEODATABASE,
    UNSUPPORTED
}

enum class EsriLayerLoadStatus {
    LOADED,
    FAILED,
    SKIPPED
}

data class EsriLayerReport(
    val name: String,
    val type: String,
    val status: EsriLayerLoadStatus,
    val errorMessage: String? = null,
    val licenseWarning: String? = null,
    val isDisplayed: Boolean = status == EsriLayerLoadStatus.LOADED
)

data class EsriPackageReport(
    val packageName: String,
    val packageType: EsriPackageType,
    val packageVersion: String? = null,
    val totalMaps: Int = 0,
    val totalLayers: Int = 0,
    val layers: List<EsriLayerReport> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    val loadedCount: Int get() = layers.count { it.status == EsriLayerLoadStatus.LOADED }
    val failedCount: Int get() = layers.count { it.status == EsriLayerLoadStatus.FAILED }
    val skippedCount: Int get() = layers.count { it.status == EsriLayerLoadStatus.SKIPPED }
}

data class EsriViewerSettings(
    val cacheLimitBytes: Long = 2L * 1024L * 1024L * 1024L,
    val allowOnlineServices: Boolean = false,
    val readOnly: Boolean = true
)

data class EsriCacheStats(
    val cacheSizeBytes: Long,
    val packagesSizeBytes: Long,
    val tempSizeBytes: Long,
    val reportsSizeBytes: Long,
    val availableDeviceBytes: Long
)

data class EsriRecentPackage(
    val name: String,
    val path: String,
    val packageType: EsriPackageType,
    val openedAt: Long,
    val sizeBytes: Long
)
