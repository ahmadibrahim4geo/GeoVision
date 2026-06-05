package com.geovision.mobile.data

import org.junit.Test
import org.junit.Assert.*

/**
 * اختبارات شاملة لـ KML Parser - اختبار إصلاحات مشاكل HTML و التكرار
 */
class KmlParserFixesTest {

    /**
     * اختبار 1: تنظيف HTML/XSLT بشكل صحيح
     */
    @Test
    fun stripHtmlTags_removeXSLTAndCSS() {
        val htmlWithXSLT = """
            <html xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:msxsl="urn:schemas-microsoft-com:xslt">
            <head><style>.red { color: red; }</style></head>
            <body><table><tr><td>Key</td><td>Value</td></tr></table></body>
            </html>
        """.trimIndent()
        
        val result = KmlParser::class.java.getDeclaredMethod("stripHtmlTags", String::class.java)
            .apply { isAccessible = true }
            .invoke(null, htmlWithXSLT) as String
        
        // يجب ألا يحتوي على XML namespaces أو tags
        assertFalse(result.contains("xmlns"))
        assertFalse(result.contains("<"))
        assertFalse(result.contains(">"))
        assertTrue(result.contains("Key"))
        assertTrue(result.contains("Value"))
    }

    /**
     * اختبار 2: فك ترميز HTML entities الكاملة
     */
    @Test
    fun decodeHtmlEntities_handleAllTypes() {
        val htmlEntities = """
            &lt;Null&gt; &amp; &quot;test&quot;
            &#160; &#x1F; &#65;
        """.trimIndent()
        
        val result = KmlParser::class.java.getDeclaredMethod("decodeHtmlEntities", String::class.java)
            .apply { isAccessible = true }
            .invoke(null, htmlEntities) as String
        
        // Named entities
        assertTrue(result.contains("<Null>"))
        assertTrue(result.contains("&"))
        assertTrue(result.contains("\"test\""))
        
        // Numeric و Hex entities
        assertTrue(result.contains(" "))  // &#160;
        assertTrue(result.contains("A"))  // &#65;
    }

    /**
     * اختبار 3: معالجة وصف معقد مع جدول HTML
     */
    @Test
    fun parseDescriptionTable_extractPropertyFromHTML() {
        val htmlTable = """
            <table style="font-family:Arial">
            <tr><td>أرقام المطابق</td><td>Channel-OUT</td></tr>
            <tr><td>الارتفاع</td><td>2.5</td></tr>
            <tr><td>المتعهد</td><td>&lt;Null&gt;</td></tr>
            </table>
        """.trimIndent()
        
        val props = mutableMapOf<String, String>()
        
        val parseMethod = KmlParser::class.java.getDeclaredMethod("parseDescriptionTable", String::class.java, MutableMap::class.java)
            .apply { isAccessible = true }
        
        parseMethod.invoke(null, htmlTable, props)
        
        // التحقق من استخراج الخصائص
        assertTrue(props.containsKey("أرقام المطابق") || props.values.contains("Channel-OUT"))
        assertTrue(props.containsKey("الارتفاع") || props.values.contains("2.5"))
        
        // يجب أن تكون البيانات منظفة من HTML
        assertFalse(props.toString().contains("<td>"))
        assertFalse(props.toString().contains("</td>"))
    }

    /**
     * اختبار 4: إنشاء معرفات فريدة للميزات
     */
    @Test
    fun parsePlacemark_createUniqueIds() {
        val kml = """<?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
            <Document>
                <Placemark id="PM_001">
                    <name>Test Feature 1</name>
                    <Point><coordinates>0,0</coordinates></Point>
                </Placemark>
                <Placemark id="PM_002">
                    <name>Test Feature 1</name>
                    <Point><coordinates>1,1</coordinates></Point>
                </Placemark>
            </Document>
            </kml>
        """.trimIndent()
        
        val result = KmlParser.parse(kml, "test.kml")
        
        // التحقق من عدم وجود معرفات مكررة
        val ids = result.features.map { it.id }
        assertEquals(ids.size, ids.toSet().size) // جميع المعرفات فريدة
        
        // يجب أن تحتوي على المعرفات الأصلية أو مشتقات منها
        assertTrue(ids.any { it.contains("PM_001") || it.contains("PM_002") })
    }

    /**
     * اختبار 5: معالجة Placemarks بنفس الاسم
     */
    @Test
    fun parsePlacemark_handleDuplicateNames() {
        val kml = """<?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
            <Document>
                <Placemark id="ID_10000">
                    <name>مخطط 1379</name>
                    <Point><coordinates>511932.7,2413230.8</coordinates></Point>
                </Placemark>
                <Placemark id="ID_10001">
                    <name>مخطط 1379</name>
                    <Point><coordinates>511933.7,2413231.8</coordinates></Point>
                </Placemark>
                <Placemark id="ID_10002">
                    <name>مخطط 1379</name>
                    <Point><coordinates>511934.7,2413232.8</coordinates></Point>
                </Placemark>
            </Document>
            </kml>
        """.trimIndent()
        
        val result = KmlParser.parse(kml, "test.kml")
        
        // يجب أن تكون لدينا 3 ميزات بدون تكرار
        assertEquals(3, result.features.size)
        
        // جميع المعرفات يجب أن تكون فريدة
        val ids = result.features.map { it.id }
        assertEquals(3, ids.toSet().size)
        
        // يجب أن تحتوي على معرفات من placemarkIds
        assertTrue(ids.any { it.contains("ID_10000") })
        assertTrue(ids.any { it.contains("ID_10001") })
        assertTrue(ids.any { it.contains("ID_10002") })
        
        // يجب أن تحتوي الخصائص على معرفات إضافية
        result.features.forEach { feature ->
            assertTrue(feature.properties.containsKey("_placemark_id"))
            assertTrue(feature.properties.containsKey("_geometry_index"))
        }
    }

    /**
     * اختبار 6: معالجة MultiGeometry بدون تكرار
     */
    @Test
    fun parsePlacemark_multiGeometryNoDuplicates() {
        val kml = """<?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
            <Document>
                <Placemark id="PM_001">
                    <name>Multi Geometry Test</name>
                    <MultiGeometry>
                        <Point><coordinates>0,0</coordinates></Point>
                        <Point><coordinates>1,1</coordinates></Point>
                        <LineString><coordinates>0,0 1,1</coordinates></LineString>
                    </MultiGeometry>
                </Placemark>
            </Document>
            </kml>
        """.trimIndent()
        
        val result = KmlParser.parse(kml, "test.kml")
        
        // يجب أن نحصل على 3 ميزات (نقطتان وخط واحد)
        assertEquals(3, result.features.size)
        
        // جميع الميزات يجب أن تكون من نفس الـ Placemark
        result.features.forEach { feature ->
            assertEquals("PM_001", feature.properties["_placemark_id"])
        }
        
        // يجب أن تكون geometry indices مختلفة
        val indices = result.features.map { it.properties["_geometry_index"] }
        assertEquals(listOf("0", "1", "2"), indices)
    }

    /**
     * اختبار 7: معالجة الأحرف الخاصة والعربية
     */
    @Test
    fun parsePlacemark_handleArabicAndSpecialChars() {
        val kml = """<?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
            <Document>
                <Placemark id="PM_001">
                    <name>محطة الصرف الصحي رقم 1</name>
                    <description>
                        &lt;table&gt;
                        &lt;tr&gt;&lt;td&gt;الموقع&lt;/td&gt;&lt;td&gt;شارع النيل&lt;/td&gt;&lt;/tr&gt;
                        &lt;/table&gt;
                    </description>
                    <Point><coordinates>31.2,30.0</coordinates></Point>
                </Placemark>
            </Document>
            </kml>
        """.trimIndent()
        
        val result = KmlParser.parse(kml, "test.kml")
        
        assertEquals(1, result.features.size)
        val feature = result.features[0]
        
        // التحقق من الاسم العربي
        assertTrue(feature.properties["_name"]?.contains("محطة") ?: false)
        
        // التحقق من معالجة الوصف
        val description = feature.properties["description"] ?: ""
        assertFalse(description.contains("&lt;"))
        assertFalse(description.contains("</td>"))
        assertTrue(description.isNotEmpty())
    }

    /**
     * اختبار 8: معالجة ملفات KML الكبيرة بدون مشاكل أداء
     */
    @Test
    fun parsePlacemark_largeBatchPerformance() {
        // محاكاة ملف KML كبير مع 100 Placemark
        val placemarks = (1..100).joinToString("\n") { i ->
            """
            <Placemark id="ID_$i">
                <name>Feature $i</name>
                <description>
                    &lt;html&gt;&lt;table&gt;
                    &lt;tr&gt;&lt;td&gt;Property 1&lt;/td&gt;&lt;td&gt;Value 1&lt;/td&gt;&lt;/tr&gt;
                    &lt;/table&gt;&lt;/html&gt;
                </description>
                <Point><coordinates>${i},${i}</coordinates></Point>
            </Placemark>
            """.trimIndent()
        }
        
        val kml = """<?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
            <Document>
                $placemarks
            </Document>
            </kml>
        """.trimIndent()
        
        val startTime = System.currentTimeMillis()
        val result = KmlParser.parse(kml, "test.kml")
        val elapsed = System.currentTimeMillis() - startTime
        
        // التحقق من النتائج
        assertEquals(100, result.features.size)
        
        // يجب أن تكون جميع المعرفات فريدة
        val ids = result.features.map { it.id }
        assertEquals(100, ids.toSet().size)
        
        // يجب أن تكون الأداء معقولة (أقل من 5 ثواني لـ 100 Placemark)
        assertTrue("Performance check failed. Elapsed: ${elapsed}ms", elapsed < 5000)
        
        println("Performance: 100 Placemarks parsed in ${elapsed}ms")
    }
}
