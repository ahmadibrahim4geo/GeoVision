/**
 * ملف نماذج البيانات الخاصّة بالطبقات (Layers).
 * يحتوي على أنواع الملفات الجغرافية المدعومة وهياكل البيانات
 * التي تمثل الطبقة وميزاتها (Features) وتفاصيلها.
 */
package com.geovision.mobile.ui.screens.layers

import androidx.compose.ui.graphics.Color

// ── أنواع الملفات الجغرافية المدعومة ──
/** أنواع الملفات التي يمكن رفعها كطبقة: Shapefile، GeoJSON، KML، GPX، Photo، GeoPackage، GeoDatabase */
enum class FileType(val label: String) {
    SHAPEFILE("Shapefile"),
    GEOJSON("GeoJSON"),
    KML("KML"),
    GPX("GPX"),
    PHOTO("Photo"),
    GEOPACKAGE("GeoPackage"),
    GEODATABASE("GeoDatabase")
}

// ── نموذج بيانات الـ Style للطبقة ──
data class LayerStyle(
    val fillColor: Color = Color(0xFFE65100),
    val strokeColor: Color = Color(0xFFE65100),
    val strokeWidth: Float = 2f,
    val fillAlpha: Float = 0.3f,
    val pointSize: Float = 8f,
    val lineWidth: Float = 4f,
    val pointIcon: String? = null,
    val labelField: String? = null,
    val labelSize: Float = 12f,
    val labelColor: Color = Color.White
)

// ── نموذج بيانات الطبقة (Layer) ──
/**
 * يمثل طبقة جغرافية محمّلة في التطبيق.
 */
data class Layer(
    val id: String,
    val name: String,
    val fileType: FileType,
    val geomType: String? = null,
    val isVisible: Boolean = true,
    val color: Color = Color(0xFFE65100),
    val transparency: Float = 1.0f,
    val pointSize: Float = 8f,
    val lineWidth: Float = 4f,
    val featureCount: Int = 0,
    val progressPercent: Float = 0f,
    var filePath: String? = null,
    val order: Int = 0,
    val style: LayerStyle = LayerStyle(
        fillColor = color,
        strokeColor = color,
        pointSize = pointSize,
        lineWidth = lineWidth
    ),
    val crs: String = "EPSG:4326"
) {
    val extent: String get() = ""
}

// ── فئة LayerNode لتمثيل هرمية الطبقات ──
data class LayerNode(
    val layer: Layer,
    val children: List<LayerNode> = emptyList(),
    val isGroup: Boolean = false
)

// ── نموذج بيانات صفّ الميزة (Feature) ──
data class FeatureRow(
    val id: String,
    val properties: Map<String, String> = emptyMap(),
    val geometryType: String? = null,
    val geometryCoordinates: String? = null,
    val crs: String = "EPSG:4326"
)

// ── نموذج تفاصيل الطبقة الموسّعة ──
/**
 * يحتوي على معلومات تفصيلية عن الطبقة: اسم الملف، المسار،
 * نظام الإسناد المكاني (CRS)، النطاق الجغرافي (Extent)،
 * وقائمة الميزات (Features).
 */
data class LayerDetailInfo(
    val fileName: String,            // اسم ملف الطبقة
    val filePath: String,            // المسار الكامل للملف
    val crs: String,                 // نظام الإسناد المكاني (Coordinate Reference System)
    val extent: String,              // النطاق الجغرافي (bounding box)
    val features: List<FeatureRow>   // قائمة الميزات في الطبقة
)
