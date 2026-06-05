# 📝 Code Comment Standards - معايير التعليقات البرمجية

## Overview / نظرة عامة

All code comments in GEO Vision are written in **Bilingual Format** (English + Arabic).  
جميع التعليقات البرمجية في GEO Vision مكتوبة بصيغة **ثنائية اللغة** (إنجليزي + عربي).

This ensures that all developers, regardless of language preference, can understand the code logic and maintain it effectively.  
هذا يضمن أن جميع المطورين، بغض النظر عن تفضيل اللغة، يمكنهم فهم منطق الكود والحفاظ عليه بفعالية.

---

## 📋 Comment Types & Formats

### 1. File Header Comments / تعليقات رأس الملف

**Format / الصيغة:**
```kotlin
// =============================================================================
// Feature Name (English)
// اسم الميزة (عربي)
// =============================================================================
//
// DESCRIPTION / الوصف:
// English description...
// وصف عربي...
//
// FEATURES / الميزات:
//   - Feature 1 (English) / الميزة 1 (عربي)
//   - Feature 2 (English) / الميزة 2 (عربي)
//
// PERFORMANCE / الأداء:
//   - Optimization 1 (English) / التحسين 1 (عربي)
//   - Optimization 2 (English) / التحسين 2 (عربي)
//
// KNOWN LIMITATIONS / القيود المعروفة:
//   - Limitation 1 (English) / القيد 1 (عربي)
//   - Limitation 2 (English) / القيد 2 (عربي)
// =============================================================================
```

**Example / مثال:**
```kotlin
// =============================================================================
// GeoJSON File Parser (RFC 7946 Compliant)
// محلل ملفات GeoJSON (متوافق مع RFC 7946)
// =============================================================================
//
// DESCRIPTION / الوصف:
// This parser converts GeoJSON files into FeatureRow objects used by the UI.
// يحول هذا المحلل ملفات GeoJSON إلى كائنات FeatureRow التي تستخدمها الواجهة.
//
// FEATURES / الميزات:
//   - Streaming mode for large files / الوضع المتدفق للملفات الكبيرة
//   - UTF-8 character encoding support / دعم ترميز UTF-8
//   - Automatic extent calculation / حساب النطاق الجغرافي التلقائي
// =============================================================================
```

---

### 2. Class/Object Documentation / توثيق الفئات/الكائنات

**Format / الصيغة:**
```kotlin
/**
 * Class Name / اسم الفئة
 *
 * PURPOSE / الغرض:
 * English description of what this class does...
 * وصف عربي لما تفعله هذه الفئة...
 *
 * RESPONSIBILITIES / المسؤوليات:
 * - Responsibility 1 (English) / المسؤولية 1 (عربي)
 * - Responsibility 2 (English) / المسؤولية 2 (عربي)
 *
 * ARCHITECTURE / العمارة:
 * - Uses pattern X for... (English) / يستخدم نمط X لـ... (عربي)
 * - Implements interface Y (English) / يطبق الواجهة Y (عربي)
 *
 * USAGE / الاستخدام:
 * val parser = GeoJsonParser
 * val result = parser.parse(file)
 *
 * @see RelatedClass for more details
 * @see الفئة المرتبطة لمزيد من التفاصيل
 */
```

---

### 3. Function/Method Documentation / توثيق الدوال/الطرق

**Format / الصيغة:**
```kotlin
/**
 * Function Name in English
 * اسم الدالة بالعربية
 *
 * DESCRIPTION / الوصف:
 * What this function does in English...
 * ما تفعله هذه الدالة بالعربية...
 *
 * PARAMETERS / المعاملات:
 * - param1: Description in English / وصف بالعربية
 * - param2: Description in English / وصف بالعربية
 *
 * RETURNS / الإرجاع:
 * English description of return value...
 * وصف عربي لقيمة الإرجاع...
 *
 * THROWS / الأخطاء:
 * - IOException: When file cannot be read / عند عدم قدرة قراءة الملف
 * - ParseException: When format is invalid / عند كون الصيغة غير صحيحة
 *
 * PERFORMANCE / الأداء:
 * O(n) where n is number of features / حيث n هو عدد المعالم
 * Memory: O(k) where k is features in memory / الذاكرة: حيث k هو الميزات المحملة
 *
 * EXAMPLE / مثال:
 * val result = parser.streamParse(reader, "file.geojson")
 * if (result.error == null) {
 *     displayFeatures(result.features)
 * }
 *
 * @param reader The input stream / مصدر الإدخال
 * @param filePath The file path / مسار الملف
 * @return ParseResult containing features / نتيجة التحليل تحتوي على المعالم
 * @see ParseResult for structure details / لمزيد من تفاصيل البنية
 */
```

---

### 4. Inline Comments / التعليقات في السطر

**Format / الصيغة:**
```kotlin
// English comment / تعليق عربي
var counter = 0  // Simple counter / عداد بسيط

// IMPORTANT: English explanation (CRITICAL)
// مهم: شرح عربي (حرج)
if (memoryUsage > threshold) {
    clearCache()
}

// TODO: Feature description needed
// TODO: مطلوب شرح الميزة
fun implementFeature() { }

// FIXME: Issue description
// FIXME: وصف المشكلة
val result = performOperation()
```

---

### 5. Complex Logic Comments / تعليقات المنطق المعقد

**Format / الصيغة:**
```kotlin
// ALGORITHM / الخوارزمية:
// English step-by-step explanation...
// شرح عربي خطوة بخطوة...
//
// 1. Read header (English) / اقرأ الرأس (عربي)
// 2. Parse features (English) / حلل المعالم (عربي)
// 3. Calculate extent (English) / احسب النطاق (عربي)

// EDGE CASES / حالات الحدود:
// - Empty file (English) / ملف فارغ (عربي)
// - Invalid encoding (English) / ترميز غير صحيح (عربي)
// - Memory exhaustion (English) / استنزاف الذاكرة (عربي)

// OPTIMIZATION / التحسين:
// Using buffered reader instead of readBytes()
// استخدام مقرئ مخزن مؤقتاً بدلاً من readBytes()
// Reduces memory usage from O(n) to O(1)
// يقلل استخدام الذاكرة من O(n) إلى O(1)
```

---

### 6. Warning/Important Notes / ملاحظات تحذيرية/مهمة

**Format / الصيغة:**
```kotlin
// ⚠️ WARNING: Description (English) / وصف عربي
// This operation may consume significant memory
// قد تستهلك هذه العملية ذاكرة كبيرة
if (fileSize > 100_000_000) {  // > 100MB
    logWarning("Large file detected")
}

// ✅ VERIFIED: This approach works across all Android versions
// تم التحقق: هذا النهج يعمل على جميع إصدارات Android
val layoutDirection = LocalLayoutDirection.current

// ❌ DO NOT: Delete this without updating cache logic
// لا تحذف هذا دون تحديث منطق الذاكرة المؤقتة
val CACHE_KEY = "layer_metadata"
```

---

### 7. Data Class/Model Documentation / توثيق فئات البيانات

**Format / الصيغة:**
```kotlin
/**
 * Data model for GeoJSON features
 * نموذج البيانات لميزات GeoJSON
 *
 * FIELDS / الحقول:
 * - id: Unique identifier (English) / معرف فريد (عربي)
 * - geometryType: Type of geometry (English) / نوع الهندسة (عربي)
 * - geometryCoordinates: WKT format (English) / صيغة WKT (عربي)
 * - properties: Feature attributes (English) / خصائص الميزة (عربي)
 *
 * NOTES / ملاحظات:
 * - geometryCoordinates is in WKT format for consistency
 *   geometryCoordinates بصيغة WKT للاتساق
 * - properties map keys are case-sensitive
 *   مفاتيح خريطة properties حساسة لحالة الأحرف
 */
```

---

## 🎯 Best Practices / أفضل الممارسات

### DO / افعل:
✅ Write comments that explain WHY, not just WHAT  
✅ اكتب تعليقات تشرح لماذا، وليس فقط ماذا  

✅ Use both English and Arabic for clarity  
✅ استخدم الإنجليزية والعربية للوضوح  

✅ Update comments when code changes  
✅ حدّث التعليقات عند تغيير الكود  

✅ Use meaningful variable names to reduce comment need  
✅ استخدم أسماء متغيرات ذات معنى لتقليل الحاجة للتعليقات  

✅ Document assumptions and constraints  
✅ وثّق الافتراضات والقيود  

### DON'T / لا تفعل:
❌ Avoid obvious comments that restate the code  
❌ تجنب التعليقات الواضحة التي تعيد ذكر الكود  

```kotlin
// ❌ BAD - this comment is obvious
// سيئ - هذا التعليق واضح جداً
i = i + 1  // Increment i

// ✅ GOOD - this comment explains why
// جيد - هذا التعليق يشرح لماذا
i = i + 1  // Move to next batch of 50 features for UI responsiveness
            // الانتقال إلى المجموعة التالية من 50 ميزة لاستجابة الواجهة
```

❌ Don't leave commented-out code  
❌ لا تترك كود معلقاً  

❌ Don't write comments in only one language  
❌ لا تكتب التعليقات بلغة واحدة فقط  

---

## 🔍 Comment Review Checklist / قائمة مراجعة التعليقات

Before committing code / قبل الحفظ:

- [ ] All public classes have documentation comments
      جميع الفئات العامة لها تعليقات توثيق
- [ ] All public methods have documentation comments
      جميع الطرق العامة لها تعليقات توثيق
- [ ] Comments are in both English and Arabic
      التعليقات بالإنجليزية والعربية
- [ ] Comments explain the "why" not just the "what"
      التعليقات تشرح "لماذا" وليس فقط "ماذا"
- [ ] No commented-out code remains
      لا يوجد كود معلق متبقي
- [ ] Warnings and important notes are clearly marked
      التحذيرات والملاحظات المهمة محددة بوضوح
- [ ] Documentation is up-to-date with code
      التوثيق محدث مع الكود

---

## 📚 Examples from Codebase / أمثلة من قاعدة الأكواد

### Example 1: GeoJsonParser Header
```kotlin
// =============================================================================
// GeoJSON File Parser (RFC 7946 Compliant)
// محلل ملفات GeoJSON (متوافق مع RFC 7946)
// =============================================================================
//
// DESCRIPTION / الوصف:
// This parser converts GeoJSON files into FeatureRow objects used by the UI.
// يحول هذا المحلل ملفات GeoJSON إلى كائنات FeatureRow التي تستخدمها الواجهة.
// ... [more details]
```

### Example 2: Function Documentation
```kotlin
/**
 * Parse GeoJSON with streaming for memory efficiency
 * تحليل GeoJSON بالوضع المتدفق لكفاءة الذاكرة
 *
 * @param reader Input stream containing GeoJSON
 *               مصدر الإدخال يحتوي على GeoJSON
 * @return ParseResult with features and metadata
 *         نتيجة التحليل تحتوي على الميزات والبيانات الوصفية
 */
```

### Example 3: Inline Comment
```kotlin
// Check memory before parsing large file
// تحقق من الذاكرة قبل تحليل ملف كبير
if (!checkAvailableMemory(requiredSize)) {
    throw OutOfMemoryError("Insufficient memory")
}
```

---

## 🌐 Language Guidelines / إرشادات اللغة

### English Rules / قواعد الإنجليزية:
- Use clear, technical terminology
- Use present tense
- Be concise and professional

### Arabic Rules / قواعد العربية:
- استخدم المصطلحات التقنية الواضحة
- استخدم الفعل المضارع
- كن مختصراً واحترافياً

### Formatting / التنسيق:
- English first, then Arabic (if same line)
- الإنجليزية أولاً، ثم العربية (إن كان نفس السطر)
- Use `//` for single-line comments
- Use `/** */` for documentation comments

---

## 📞 Questions / أسئلة

If unclear about how to comment something:  
إذا كان هناك غموض حول كيفية التعليق على شيء ما:

1. Check similar code in the project for patterns
   تحقق من كود مشابه في المشروع للأنماط
2. Refer to this document
   الرجوع إلى هذا المستند
3. Ask in GitHub Issues or Discussions
   اسأل في GitHub Issues أو Discussions

---

**Last Updated**: January 2026  
**Status**: Active  
**Applied To**: All future code contributions

