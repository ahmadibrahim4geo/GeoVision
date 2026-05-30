package com.geovision.mobile.core

import com.geovision.mobile.data.GeoJsonParser

object UserFriendlyErrors {

    fun getErrorMessage(errorType: GeoJsonParser.ParseErrorType, fileName: String): String {
        return when (errorType) {
            GeoJsonParser.ParseErrorType.ACCESS_DENIED ->
                "لا يمكن قراءة الملف '$fileName'.\nتأكد من توفر الإذن للوصول إلى الملف."
            GeoJsonParser.ParseErrorType.CORRUPTED ->
                "ملف '$fileName' تالف أو معروض بشكل غير صحيح.\nحاول بملف آخر أو تحقق من صحة الملف."
            GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT ->
                "صيغة الملف غير مدعومة.\nالصيغ المدعومة: GeoJSON, Shapefile, GeoPackage, GPX, KML"
            GeoJsonParser.ParseErrorType.OUT_OF_MEMORY ->
                "الملف كبير جداً ويتجاوز ذاكرة الجهاز.\nحاول بملف أصغر أو أعد تشغيل التطبيق."
            GeoJsonParser.ParseErrorType.TIMEOUT ->
                "انتهت مهلة معالجة الملف '$fileName'.\nحاول مرة أخرى أو استخدم ملف أصغر."
            GeoJsonParser.ParseErrorType.TOO_MANY_FEATURES ->
                "الملف '$fileName' يحتوي على عدد معالم كبير جداً (>100,000).\nقد تؤثر على الأداء."
            GeoJsonParser.ParseErrorType.INVALID_GEOMETRY ->
                "الملف '$fileName' يحتوي على بيانات هندسية غير صالحة.\nتحقق من صحة البيانات."
            GeoJsonParser.ParseErrorType.PARSER_ERROR ->
                "حدث خطأ أثناء معالجة الملف '$fileName'.\nقد يكون الملف تالفاً أو غير متوافق."
            GeoJsonParser.ParseErrorType.NONE -> ""
        }
    }

    fun getSolution(errorType: GeoJsonParser.ParseErrorType): String? {
        return when (errorType) {
            GeoJsonParser.ParseErrorType.CORRUPTED ->
                "💡 محاولات الحل:\n1. تحقق من سلامة الملف\n2. حاول فتح الملف على جهاز آخر\n3. استخدم أداة خارجية للتحقق"
            GeoJsonParser.ParseErrorType.OUT_OF_MEMORY ->
                "💡 محاولات الحل:\n1. أغلق التطبيقات الأخرى\n2. أعد تشغيل الجهاز\n3. استخدم ملف بحجم أصغر"
            GeoJsonParser.ParseErrorType.ACCESS_DENIED ->
                "💡 محاولات الحل:\n1. امنح التطبيق صلاحية الوصول للملفات\n2. انقل الملف إلى مجلد آخر\n3. أعد تشغيل التطبيق"
            GeoJsonParser.ParseErrorType.UNSUPPORTED_FORMAT ->
                "💡 الصيغ المدعومة:\n.geojson, .json, .shp, .gpkg, .gpx, .kml, .kmz"
            else -> null
        }
    }
}
