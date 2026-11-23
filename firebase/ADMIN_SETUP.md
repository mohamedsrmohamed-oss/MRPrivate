# إنشاء أول أدمن (مرة واحدة من Firebase Console)

1. Authentication ← Users ← Add user: بريد حقيقي (مثل admin@yourdomain.com) وكلمة مرور قوية، ثم انسخ الـ User UID.
2. Realtime Database ← أضف العقدة: `admins/<UID>` بالقيم:
   { "username": "M1", "canManageAdmins": true, "canManageUsers": true, "canDeleteAccounts": true }
3. تسجيل دخول الأدمن في التطبيق: اكتب `*admin@yourdomain.com` في خانة اسم المستخدم + كلمة المرور.
4. بعدها يضيف الأدمن باقي الأدمن من شاشة الأدمن (يُنشأ لهم حساب Auth تلقائياً، ويدخلون بـ `*username`).

# ملاحظات
- تفعيل Email/Password في Authentication ← Sign-in method.
- المستخدمون العاديون يسجلون باسم مستخدم (يُشتق منه بريد ثابت) ولا يحتاجون بريداً حقيقياً.
- كلمة المرور حد أدنى 6 أحرف (شرط Firebase).
- حذف مستخدم من التطبيق يحذف ملفه من القاعدة فقط؛ حساب Auth يبقى (لا يمكن حذفه من التطبيق)، فلإعادة استخدام نفس اسم المستخدم احذف الحساب من Authentication ← Users.
- لا يوجد استرجاع كلمة مرور (الإيميلات مشتقة وليست حقيقية): المستخدم الذي ينسى كلمة مرورك يحتاج حساباً جديداً أو تدخلاً يدوياً من Console.
