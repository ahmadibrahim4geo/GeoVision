package com.geovision.mobile.ui.screens.terms

import com.geovision.mobile.R

class UserGuideActivity : LegalTextActivity() {
    override fun provideTitleRes() = R.string.user_guide
    override fun provideLogoVisible() = true
    override fun provideSections(): List<Pair<String, String>> {
        return if (currentLang == "ar") arabicSections else englishSections
    }

    private val arabicSections = listOf(
        "GeoVision" to "تطبيق GeoVision هو متصفح بيانات مكانية ونظام معلومات جغرافية (GIS) صمم خصيصاً للأجهزة المحمولة. يتيح لك عرض وقياس وإدارة البيانات الجغرافية مباشرة على هاتفك أو جهازك اللوحي.",
        "ما يمكنك فعله مع GeoVision" to "",
        "عرض الخرائط" to "• OpenStreetMap، صور الأقمار الصناعية (مع أو بدون أسماء)، خرائط طبوغرافية، خرائط النقل، والوضع الليلي.\n• التبديل بين أنواع الخرائط بنقرة واحدة.",
        "استيراد البيانات الجغرافية" to "• فتح ملفات Shapefile (.shp مع .dbf و .shx و .prj)، GeoJSON (.geojson)، KML (.kml)، GPX (.gpx)، GeoPackage (.gpkg)، والصور الجغرافية (GeoPhoto).\n• يتم اكتشاف الملفات المساعدة لـ Shapefile تلقائياً مع دعم تحويل الإسناد (UTM, Web Mercator ← WGS84). يتم استخراج ملفات ZIP التي تحتوي على Shapefiles تلقائياً.\n• يدعم التطبيق ترميز DBF العربي (CP1256) و UTF-8 و ISO-8859-1.",
        "القياس على الخريطة" to "• اضغط على أيقونة المسطرة لبدء القياس.\n• المسافة: انقر على الخريطة لرسم خط؛ يظهر الطول الإجمالي.\n• المساحة: انقر لرسم مضلع؛ يظهر المحيط والمساحة.\n• الدائرة: انقر لتعيين المركز، ثم انقر على حافة الدائرة؛ يظهر نصف القطر والقطر والمحيط والمساحة.\n• التبديل بين الوحدات المترية (متر، كيلومتر، هكتار) والإمبراطورية (قدم، ميل، فدان).\n• يمكن حفظ النتائج كطبقة جديدة للرجوع إليها لاحقاً.",
        "إدارة الطبقات" to "• تظهر البيانات المستوردة في شاشة الطبقات.\n• إظهار/إخفاء الطبقات، تغيير الترتيب، تغيير الألوان والرموز (حجم النقطة، عرض الخط، لون التعبئة).\n• عرض جدول السمات لكل طبقة والبحث داخله.\n• اضغط على طبقة لتكبير الخريطة إلى نطاقها.\n• حذف الطبقات غير المرغوب فيها.",
        "تحديد الموقع (GPS)" to "• اضغط على زر GPS لتوسيط الخريطة على موقعك الحالي.\n• تتحرك النقطة الزرقاء مع حركتك.\n• اضغط مرة أخرى لإيقاف تتبع الموقع.",
        "عرض الإحداثيات" to "• رؤية إحداثيات مركز الخريطة الحالي بتنسيق DD أو DMS أو UTM.\n• تغيير التنسيق من الإعدادات.",
        "شبكة الإحداثيات" to "• تفعيل شبكة إحداثيات على الخريطة للمساعدة في التحديد.",
        "العلامات المرجعية (Bookmarks)" to "• حفظ مواقع الخريطة للتنقل السريع إليها.",
        "البحث" to "• البحث عن المعالم داخل الطبقات المستوردة بالاسم أو قيمة الخاصية.",
        "الإدخال الدقيق" to "• في وضع القياس، يمكنك تحريك الخريطة لتحديد موقع العلامة بدقة وإضافة النقاط يدوياً.",
        "" to "جميع معالجة البيانات تتم محلياً على جهازك. لا يتم رفع أو مشاركة أي بيانات مع أي خادم."
    )

    private val englishSections = listOf(
        "GeoVision" to "GeoVision is a spatial data browser and GIS application designed for mobile devices. It enables you to view, measure, and manage geographic data directly on your phone or tablet.",
        "What You Can Do with GeoVision" to "",
        "View Maps" to "• OpenStreetMap, satellite imagery (with or without labels), topographic maps, transport maps, and dark mode maps.\n• Switch between map types with one tap.",
        "Import Geographic Data" to "• Open Shapefile (.shp with .dbf, .shx, .prj), GeoJSON (.geojson), KML (.kml), GPX (.gpx), GeoPackage (.gpkg), and geotagged photos.\n• Shapefiles are automatically detected with their companion files and support CRS transformation (UTM, Web Mercator → WGS84). ZIP archives containing shapefiles are extracted automatically.\n• The app handles Arabic DBF encoding (CP1256), UTF-8, and ISO-8859-1.",
        "Measure on the Map" to "• Tap the ruler icon to start measuring.\n• Distance: tap points to draw a line; the app shows the total length.\n• Area: tap points to draw a polygon; the app shows the perimeter and area.\n• Circle: tap the center, then tap a point on the edge; the app shows radius, diameter, circumference, and area.\n• Switch between metric (meter, kilometer, hectare) and imperial (feet, mile, acre) units.\n• Results can be saved as a new layer for future reference.",
        "Manage Layers" to "• Imported data appears in the Layers screen.\n• Toggle visibility, reorder, change colors and symbols (point size, line width, fill).\n• View the attribute table for each layer and search within it.\n• Tap a layer to zoom to its extent on the map.\n• Delete layers when no longer needed.",
        "GPS Location" to "• Tap the GPS button to center the map on your current location.\n• The blue dot moves as you move.\n• Tap again to disable GPS tracking.",
        "Coordinate Display" to "• See your current map center coordinates in Degrees Decimal (DD), Degrees Minutes Seconds (DMS), or UTM format.\n• Change the format in Settings.",
        "Grid Overlay" to "• Enable a coordinate grid on the map for reference.",
        "Bookmarks" to "• Save map locations for quick navigation.",
        "Search" to "• Search for features within your imported layers by name or property value.",
        "Precision Input" to "• In measurement mode, you can precisely position the crosshair and add points manually for accurate placement.",
        "" to "All data processing is done locally on your device. No data is uploaded or shared with any server."
    )
}
