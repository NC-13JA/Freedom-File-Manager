Freedom File Manager — النسخة التأسيسية 0.1.0

هذه نسخة تأسيسية قابلة للاستيراد في AndroidIDE على الهاتف.
تتضمن:
- شاشة أولية.
- طلب إذن MANAGE_EXTERNAL_STORAGE عبر إعدادات Android.
- تصفح مجلد التخزين المشترك بعد منح الإذن.
- منتقي SAF لحفظ إذن مجلد يختاره المستخدم.

مهم جداً بشأن Android 14:
إذن MANAGE_EXTERNAL_STORAGE لا يفتح مجلد Android/data أو Android/obb ولا ملفات /data/user/0 الخاصة بالتطبيقات.
لا يوجد تطبيق عادي يستطيع إلغاء هذه الحماية وحده.
سنضيف في المرحلة التالية تكامل Shizuku (يتطلب تفعيل الخدمة من الهاتف عبر Wireless debugging أو Root)، ثم طبقة Root اختيارية.
لا تستخدم هذه النسخة لتعديل ملفات مهمة؛ فهي حالياً نسخة تأسيسية للعرض وطلب الصلاحيات وليست محرر ملفات كاملاً.

البناء على الهاتف:
1. ثبّت AndroidIDE من مصدره الرسمي.
2. افتح مجلد FreedomFileManager في AndroidIDE.
3. اسمح بتنزيل مكونات Gradle/Android SDK عند الطلب.
4. نفّذ Sync ثم Run.

المتطلبات: Android SDK Platform 35، Android Gradle Plugin 8.6.1، Gradle 8.7 أو إصدار متوافق يديره AndroidIDE.
