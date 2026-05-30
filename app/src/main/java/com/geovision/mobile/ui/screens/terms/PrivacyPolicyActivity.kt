package com.geovision.mobile.ui.screens.terms

import com.geovision.mobile.R

class PrivacyPolicyActivity : LegalTextActivity() {
    override fun provideTitleRes() = R.string.privacy_title
    override fun provideLogoVisible() = true
    override fun provideSections(): List<Pair<String, String>> {
        return if (currentLang == "ar") arabicSections else englishSections
    }

    private val arabicSections = listOf(
        "سياسة الخصوصية" to "",
        "1. جمع البيانات" to "هذا التطبيق لا يقوم بجمع أي بيانات شخصية من المستخدمين، ولا يطلب معلومات تعريفية مثل الاسم أو البريد الإلكتروني أو الموقع أو أي بيانات حساسة.",
        "2. استخدام البيانات" to "لا يتم تخزين أو معالجة أي بيانات شخصية خارج الجهاز. جميع العمليات داخل التطبيق تتم محليًا بهدف تشغيل الميزات الأساسية فقط مثل القياس وعرض الخرائط.",
        "3. مشاركة البيانات" to "لا يقوم التطبيق بمشاركة أي بيانات مع أي جهة خارجية أو طرف ثالث تحت أي ظرف.",
        "4. خدمات الطرف الثالث" to "قد يعتمد التطبيق على خدمات خرائط خارجية (مثل ESRI أو مصادر خرائط مجانية)، وهذه الخدمات قد يكون لها سياسات خصوصية خاصة بها، ولا يتحكم التطبيق في طريقة تعاملها مع البيانات.",
        "5. الأمان" to "لا يتم تتبع المستخدمين أو إنشاء ملفات تعريف (Profiles)، ولا يتم استخدام أي أدوات تحليل سلوك المستخدم داخل التطبيق.",
        "6. التعديلات على السياسة" to "قد يتم تحديث سياسة الخصوصية من وقت لآخر، وسيتم نشر أي تغييرات داخل التطبيق أو ضمن التحديثات الجديدة.",
        "7. الموافقة" to "باستخدامك للتطبيق فإنك توافق على هذه السياسة بالكامل."
    )

    private val englishSections = listOf(
        "Privacy Policy" to "",
        "1. Data Collection" to "This application does not collect any personal data from users. It does not request any identifying information such as name, email address, location, or any sensitive data.",
        "2. Data Usage" to "No personal data is stored or processed outside the device. All operations within the application are performed locally, solely for the purpose of enabling core features such as measurement and map visualization.",
        "3. Data Sharing" to "The application does not share any data with any external party or third party under any circumstances.",
        "4. Third-Party Services" to "The application may rely on external map services (such as ESRI or free map data sources). These services may have their own privacy policies, and the application has no control over how they handle data.",
        "5. Security" to "Users are not tracked, and no user profiles are created. No user behavior analytics tools are used within the application.",
        "6. Policy Changes" to "This Privacy Policy may be updated from time to time. Any changes will be published within the application or included in new updates.",
        "7. Consent" to "By using this application, you fully agree to this Privacy Policy."
    )
}
