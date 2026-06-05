# Security Policy / سياسة الأمان

## Reporting Security Issues / الإبلاغ عن مشاكل الأمان

**🔒 Do NOT open a public issue for security vulnerabilities!**  
**🔒 لا تنشر مشكلة أمنية علناً!**

If you discover a security vulnerability, please email us privately:

إذا اكتشفت ثغرة أمنية، يرجى إرسال بريد إلكتروني خاص:

📧 **Email**: [maintainer email]  
🔐 **PGP Key**: [if available]  
🕐 **Response Time**: We aim to respond within 48 hours  
🕐 **وقت الاستجابة**: نهدف للرد خلال 48 ساعة

---

## What We Cover / ما نغطيه

✅ **Security Issues We Take Seriously:**
- Memory leaks or exploitable crashes
- Path traversal vulnerabilities
- Unencrypted sensitive data handling
- Buffer overflows
- SQL injection risks
- Improper permission handling

✅ **المشاكل الأمنية التي نأخذها على محمل الجد:**
- تسرب الذاكرة أو الأعطال القابلة للاستغلال
- ثغرات التنقل في المسارات
- التعامل غير المشفر مع البيانات الحساسة
- تجاوزات المخزن المؤقت
- مخاطر حقن SQL
- معالجة الأذونات غير السليمة

---

## What We Don't Cover / ما لا نغطيه

❌ **Not Security Issues:**
- Social engineering
- Theoretical vulnerabilities without proof
- Feature requests disguised as security issues
- Performance issues that don't involve security

❌ **ليست مشاكل أمنية:**
- الهندسة الاجتماعية
- الثغرات النظرية بدون إثبات
- طلبات الميزات المقنعة بمشاكل أمنية
- مشاكل الأداء غير المرتبطة بالأمان

---

## Disclosure Policy / سياسة الكشف

1. **Discover** - Contact us privately / تواصل معنا بشكل خاص
2. **Confirm** - We verify the issue / نحقق من المشكلة
3. **Fix** - We develop a patch / نطور إصلاح
4. **Test** - We test thoroughly / نختبر بعناية
5. **Release** - We release the fix / ننشر الإصلاح
6. **Disclose** - You may disclose after fix / يمكنك الإفصاح بعد الإصلاح

**Timeline**: 90 days max from initial contact  
**الجدول الزمني**: 90 يوم كحد أقصى من الاتصال الأولي

---

## Security Considerations / الاعتبارات الأمنية

### Current Security Status / حالة الأمان الحالية

✅ **Secure Features / الميزات الآمنة:**
- Local-only processing (no cloud upload)
- No telemetry or tracking
- No authentication required
- No network connectivity needed

⚠️ **Areas Under Review / المناطق قيد المراجعة:**
- Local metadata encryption (not implemented yet)
- Path traversal validation (being improved)
- Memory safety checks (being added)

❌ **Known Vulnerabilities / الثغرات المعروفة:**
See [ISSUES.md](../ISSUES.md) for known security issues
راجع [ISSUES.md](../ISSUES.md) للمشاكل الأمنية المعروفة

---

## Best Practices for Users / أفضل الممارسات للمستخدمين

1. **Keep app updated** / حافظ على تحديث التطبيق
2. **Don't share GIS files with sensitive data** / لا تشارك ملفات GIS بيانات حساسة
3. **Use device encryption** / استخدم تشفير الجهاز
4. **Review EXIF data in photos** / راجع بيانات EXIF في الصور
5. **Report suspicious behavior** / أبلغ عن السلوك المريب

---

## Security Updates / تحديثات الأمان

- Subscribe to releases: [GitHub Releases](../../releases)
- تابع الإصدارات: [GitHub Releases](../../releases)
- Follow security advisories
- تابع التنبيهات الأمنية

---

## Contact / التواصل

- 📧 **Security Email**: [maintainer email]
- 💬 **Security Discussions**: [Use GitHub Security Advisory](../../security/advisories)
- 🐛 **Bug Reports**: [GitHub Issues](../../issues) (non-security only)

---

**Thank you for helping keep GEO Vision secure!**  
**شكراً لمساعدتك في الحفاظ على أمان GEO Vision!**

