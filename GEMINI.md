# GEMINI & ANTIGRAVITY PROJECT DIRECTIVE: MR PRIVATE (ELITE ARCHITECTURE)

> **Status:** Active & Enforced  
> **Target Project:** `MR Private` (`com.example.mrprivate`) — Android Jetpack Compose & Firebase  
> **Agent Authorization:** **100% Full Access** Granted to **Antigravity (AI Agent) & Gemini**

---

## 1. تفويض الصلاحيات الكاملة (100% Full Access & Authorization)

يُمنح الوكيل الذكي **Antigravity** الصلاحيات الكاملة بنسبة **100%** لتركيب، بناء، تعديل، وإعادة هيكلة كافة طبقات المشروع البرمجية بدون استثناء:
- **`data/`**: إدارة وتطوير الـ Repositories، واتصالات Firebase Realtime Database، ونماذج البيانات (Data Models)، وتخزين الـ Offline Cache.
- **`services/`**: إدارة خدمات الخلفية، ومعالجة وفحص أوراق الامتحانات الذكية (`PaperScanService`)، وجدولة إشعارات الطلاب والتنبيهات الدقيقة (`StudentNotificationScheduler`).
- **`ui/`**: بناء وتطوير الـ Design System، والثيمات (`Color.kt`, `Theme.kt`, `Type.kt`)، والمكونات المشتركة (`ThemeAndComponents.kt`, `ScheduleComponents.kt`, `NewDesign.kt`)، والرسوم المتحركة.
- **`features/`**: هندسة وبناء شاشات ومنطق الميزات المستقلة (`auth/`, `admin/`, `teacher/`, `student_parent/`, `quiz/`).
- **Root (`com.example.mrprivate/`):** تهيئة التطبيق الأساسية (`MrPrivateApp.kt`) ونقطة الانطلاق والتوجيه الرئيسية (`MainActivity.kt`).

---

## 2. خريطة الهيكلة المعمارية المعتمدة (Target "Elite" Architecture Map)

تلتزم جميع ملفات Kotlin البرمجية داخل المسار `app/src/main/java/com/example/mrprivate/` بالهيكل النظيف التالي:

```text
com.example.mrprivate/
├── MrPrivateApp.kt                      # Application Initialization & Offline Persistence
├── MainActivity.kt                      # Lightweight Entry Point & Root Navigation Router
│
├── data/                                # Data & Backend Layer
│   ├── firebase/
│   │   └── FirebaseManager.kt           # Firebase References & Database Helpers
│   └── repository/
│       └── RoleDataRepository.kt        # State Management, CRUD Operations, Multi-role Queries
│
├── services/                            # Background, Hardware & System Services
│   ├── PaperScanService.kt              # OCR, Quiz Paper Scanner, Webhook Integration
│   └── StudentNotificationScheduler.kt  # AlarmManager, BroadcastReceivers & Push Notifications
│
├── ui/                                  # Design System & Shared UI Building Blocks
│   ├── theme/                           # Color.kt, Type.kt, Theme.kt
│   └── components/                      # ThemeAndComponents.kt, ScheduleComponents.kt, NewDesign.kt
│
└── features/                            # Dedicated Feature & Screen Modules
    ├── auth/                            # AccountAuth.kt (Unified Auth & Secondary Admin Creation)
    ├── admin/                           # AdminScreens.kt (Dashboard, Approvals, Subscriptions)
    ├── teacher/                         # TeacherScreens.kt (Slots, Grades, Student Management)
    ├── student_parent/                  # StudentAndParentScreens.kt, ParentScreens.kt
    └── quiz/                            # QuizSystemComponents.kt (Exam Engine, AI Generation)
```

---

## 3. القواعد الصارمة والخطوط الحمراء (Strict Guardrails & Immutable Rules)

1. **ضمان ثبات منطق الأعمال بنسبة 100% (Zero-Logic-Change Guarantee):**
   - الحفاظ التام على كامل قواعد العمل: احتساب مدة الاشتراكات (120 يوماً للترم)، شروط القبول، الصلاحيات الإدارية، حجز الحصص، تقييمات المعلمين، كشف غياب ولي الأمر، ومعالجة أوراق الامتحانات عبر Webhook.
2. **ثبات مسارات ومفاتيح Firebase (Realtime Database Schema):**
   - يُمنع منعاً باتاً تغيير أو إعادة تسمية عقد ومفاتيح Firebase الحالية:
     `teachers/`, `students/`, `schedules/`, `admins/`, `quizzes/`, `indexes/`, `terms/`
   - ثبات أسماء الحقول (Field Names) داخل كائنات JSON لضمان عدم حدوث أي تراجع أو فقدان لبيانات المستخدمين النشطين.
3. **ثبات واستقرار واجهات Compose واللغتين:**
   - الحفاظ التام على دعم اللغتين (العربية والإنجليزية) عبر دالة `tr(...)`.
   - **قاعدة Compose Hoisting:** عدم استدعاء أي دالة `@Composable` داخل دوال غير كومبوزابل (مثل `onClick`, `Toast.makeText`, `joinToString`)؛ يتم استخراج النصوص دائماً مسبقاً (Hoisted String).
   - تثبيت مفاتيح القوائم (`key = { it.id }`) في جميع عناصر `LazyColumn` و `LazyRow` لمنع الـ Recomposition غير الضروري وضمان أعلى سلاسة في التمرير (60/120 FPS).
4. **تدرج المسؤوليات والاعتماديات النظيفة (Unidirectional Dependencies):**
   - طبقة `features/` تستورد من `ui/` و `data/` و `services/`.
   - طبقة `ui/` لا تستورد إطلاقاً من `features/` لضمان عدم حدوث Circular Dependencies.

---

## 4. قائمة التحقق الإلزامية لسلامة الكود (Mandatory Safety Checklist)

عند إنشاء أو نقل أو فصل أي ملف برمجي (`.kt`):
1. **تحديث الـ Package:** تعديل تعريف الحزمة أعلى الملف بما يطابق موقعه الجديد.
2. **تحديث الـ Imports:** تصحيح كافة مسارات الاستيراد في الملفات المرتبطة تلقائياً.
3. **محددات الرؤية (Visibility Modifiers):** تحويل الدوال والمساعدات المشتركة بين الملفات إلى `internal` عند الحاجة لمنع أخطاء الوصول.
4. **تسجيل المكونات في `AndroidManifest.xml`:** التأكد من مسارات الحزم الصحيحة لكلاس التطبيق والأنشطة والخدمات ومستقبلات البث (`BroadcastReceiver`).
5. **قواعد R8 / ProGuard (`app/proguard-rules.pro` أو `rules.keep`):** حماية نماذج بيانات Firebase من التشويش أو الحذف عند بناء Release.
6. **فحص الترجمة والبناء:** التحقق الصارم من نجاح `./gradlew compileDebugKotlin` بخلو تام من الأخطاء (Zero Compile Errors).
