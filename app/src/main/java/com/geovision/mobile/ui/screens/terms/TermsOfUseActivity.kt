package com.geovision.mobile.ui.screens.terms

import com.geovision.mobile.R

class TermsOfUseActivity : LegalTextActivity() {
    override fun provideTitleRes() = R.string.terms_of_use
    override fun provideLogoVisible() = true
    override fun provideSections(): List<Pair<String, String>> {
        return if (currentLang == "ar") arabicSections else englishSections
    }

    private val arabicSections = listOf(
        "شروط الاستخدام" to "يرجى قراءة شروط الاستخدام التالية بعناية قبل استخدام التطبيق. باستخدامك للتطبيق فإنك توافق على الالتزام بهذه الشروط.",
        "1. التعريف بالخدمة" to "يوفر هذا التطبيق خدمات عرض الخرائط، وقياس المسافات والمساحات، وإدارة الطبقات الجغرافية، واستخدام مصادر بيانات خرائطية مختلفة بهدف دعم الأعمال الجغرافية وتحليل البيانات المكانية.",
        "2. استخدام التطبيق" to "يُسمح باستخدام التطبيق للأغراض المشروعة فقط، ويُمنع استخدامه في أي نشاط مخالف للقوانين أو يهدف إلى الإضرار بالنظام أو انتهاك حقوق الآخرين.",
        "3. دقة البيانات" to "يعتمد التطبيق على مصادر بيانات مختلفة مثل الخرائط وبيانات الأقمار الصناعية. لا نضمن دقة البيانات بشكل مطلق، وقد تختلف النتائج حسب المصدر أو إعدادات الجهاز.",
        "4. مصادر البيانات" to "قد يستخدم التطبيق مصادر بيانات خارجية مثل خرائط ESRI أو مصادر خرائط مجانية أخرى، ويخضع استخدام هذه المصادر لشروط كل مزود خدمة على حدة.",
        "5. القياسات" to "نتائج قياس المسافات والمساحات تقريبية وتعتمد على دقة البيانات ونظام الإحداثيات المستخدم، ولا يُعتد بها كقياسات هندسية دقيقة في الحالات الرسمية إلا إذا تم التحقق منها.",
        "6. الملكية الفكرية" to "جميع عناصر التطبيق من تصميم وواجهة برمجية وخصائص تقنية هي ملك لمطور التطبيق، ولا يجوز نسخها أو إعادة توزيعها دون إذن.",
        "7. التعديلات على الخدمة" to "نحتفظ بالحق في تحديث أو تعديل أو إيقاف أي جزء من التطبيق في أي وقت دون إشعار مسبق بهدف تحسين الأداء أو إضافة ميزات جديدة.",
        "8. حدود المسؤولية" to "لا نتحمل أي مسؤولية عن أي خسائر أو أضرار ناتجة عن استخدام التطبيق أو الاعتماد على نتائجه بشكل مباشر أو غير مباشر.",
        "9. معلومات المطور والتواصل" to "تم تطوير هذا التطبيق بواسطة المهندس/ أحمد إبراهيم، ويمكن التواصل عبر الرقم: +20 111 191 5925.",
        "10. قبول الشروط والخصوصية" to "باستخدامك للتطبيق فإنك تقر بأنك قرأت وفهمت ووافقت على جميع الشروط المذكورة أعلاه. كما تقر بأن استخدامك للتطبيق يتم بمحض إرادتك، وأن التطبيق لا يقوم بجمع أي بيانات شخصية، ولا يقوم بعمليات تتبع للمستخدمين أو تخزين أي معلومات تعريفية أو حساسة تخص المستخدمين، ويتم التعامل مع البيانات داخل التطبيق فقط لغرض تشغيل الميزات الأساسية دون مشاركتها مع أي طرف ثالث."
    )

    private val englishSections = listOf(
        "Terms of Use" to "Please read the following Terms of Use carefully before using the application. By using this application, you agree to comply with these terms.",
        "1. Service Description" to "This application provides map viewing services, distance and area measurement tools, geographic layer management, and the use of various map data sources to support geospatial work and spatial data analysis.",
        "2. Use of the Application" to "The application may only be used for lawful purposes. It is prohibited to use it for any activity that violates laws, disrupts systems, or infringes upon the rights of others.",
        "3. Data Accuracy" to "The application relies on various data sources such as maps and satellite data. We do not guarantee absolute data accuracy, and results may vary depending on the source or device settings.",
        "4. Data Sources" to "The application may use external data sources such as ESRI maps or other free mapping services. The use of these sources is subject to the terms and conditions of each respective provider.",
        "5. Measurements" to "Distance and area measurement results are approximate and depend on data accuracy and the coordinate system used. They should not be considered precise engineering measurements for official use unless independently verified.",
        "6. Intellectual Property" to "All elements of the application, including design, user interface, programming structure, and technical features, are the property of the application developer. They may not be copied or redistributed without permission.",
        "7. Service Modifications" to "We reserve the right to update, modify, or discontinue any part of the application at any time without prior notice, in order to improve performance or add new features.",
        "8. Limitation of Liability" to "We are not responsible for any losses or damages arising from the use of the application or reliance on its results, whether direct or indirect.",
        "9. Developer Information and Contact" to "This application was developed by Engineer/ Ahmed Ibrahim. You can contact via phone number: +20 111 191 5925.",
        "10. Acceptance of Terms and Privacy" to "By using the application, you acknowledge that you have read, understood, and agreed to all the above terms. You also acknowledge that your use of the application is entirely voluntary, and that the application does not collect any personal data, perform user tracking, or store any identifying or sensitive information. All data processing within the application is strictly limited to enabling core functionalities and is not shared with any third party."
    )
}
