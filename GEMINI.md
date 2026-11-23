# GEMINI & ANTIGRAVITY PROJECT DIRECTIVE: MR PRIVATE (ELITE ARCHITECTURE)

> **Status:** Active & Enforced  
> **Target Project:** `MR Private` (`com.example.mrprivate`) — Android Jetpack Compose & Firebase  
> **Agent Authorization:** **100% Full Access** Granted to **Antigravity (AI Agent) & Gemini**

---

## 1. تفويض الصلاحيات الكاملة (100% Full Access & Authorization)

يُمنح الوكيل الذكي **Antigravity** الصلاحيات الكاملة بنسبة **100%** لتركيب، بناء، تعديل، وإعادة هيكلة كافة طبقات المشروع البرمجية بدون استثناء:
- **`data/`**: إدارة وتطوير الـ Repositories، واتصالات Firebase Realtime Database، ونماذج البيانات (Data Models)، وتخزين الـ Offline Cache.
- **`services/`**: إدارة خدمات الخلفية، ومعالجة وفحص أوراق الامتحانات الذكية (`PaperScanService`)، وجدولة إشعارات الطلاب والتنبيهات الدقيقة (`StudentNotificationScheduler`).
- **`ui/`**: بناء وتطوير الـ Design System، والثيمات (`Color.kt`, `Theme.kt`, `Type.kt`)، والمكونات المشتركة (`AppCoreComponents.kt`, `ScheduleComponents.kt`, `NewDesign.kt`, `AppHelpers.kt`, `AppDialogsAndBadges.kt`)، والرسوم المتحركة.
- **`features/`**: هندسة وبناء شاشات ومنطق الميزات المستقلة (`auth/`, `admin/`, `teacher/`, `student_parent/`, `quiz/`).
- **Root (`com.example.mrprivate/`):** تهيئة التطبيق الأساسية (`MrPrivateApp.kt`) ونقطة الانطلاق الخفيفة (`MainActivity.kt`) وحالة التطبيق الموحدة (`MrPrivateState.kt`).

---

## 2. خريطة الهيكلة المعمارية المعتمدة — Elite Architecture (Phase 5 — FINAL)

جميع ملفات Kotlin داخل `app/src/main/java/com/example/mrprivate/` تتبع الهيكل النظيف التالي:

```text
com.example.mrprivate/
├── MrPrivateApp.kt          # Application class — Firebase offline persistence
├── MainActivity.kt          # Lightweight entry point ONLY: setContent → MrPrivateTheme → AppNavigation
├── MrPrivateState.kt        # Unified app state: ViewModel-style, Firebase listeners, CRUD ops
│
├── data/
│   ├── firebase/
│   │   └── FirebaseManager.kt           # Firebase DB URL constant & reference helpers
│   ├── model/
│   │   └── AppModels.kt                 # ALL shared data classes & enums (Firebase-safe)
│   │       # SubjectPlanData, DailyScheduleData, TermSettings, AdminAccount,
│   │       # TeacherProfile, ScheduleSlot, SessionNotificationItem, StudentEnrollment,
│   │       # StudentProfile, QuizQuestion, QuizSubmission, QuizExam,
│   │       # PAPER_RETENTION_MS, PAPER_PURGE_INTERVAL_MS
│   └── repository/
│       └── RoleDataRepository.kt        # Firebase real-time listeners, CRUD, role queries, LoginResult enum
│
├── services/
│   ├── PaperScanService.kt              # OCR / quiz paper scanner / Webhook integration
│   └── StudentNotificationScheduler.kt  # AlarmManager, BroadcastReceiver, push notifications
│
├── ui/
│   └── components/
│       ├── AppHelpers.kt                # tr(), trValue(), clearLoginPrefs(), saveLoginPrefs(),
│       │                                # restoreLoginState(), date/schedule helpers, network utils
│       ├── AppCoreComponents.kt         # GradientTheme, appGradientThemes, MrPrivateTheme,
│       │                                # shared Compose building blocks (cards, buttons, top bars)
│       ├── AppDialogsAndBadges.kt       # Shared dialogs and badge components
│       ├── ScheduleComponents.kt        # Schedule slot cards, booking UI
│       └── NewDesign.kt                 # New design components & animations
│
└── features/
    ├── auth/
    │   ├── AccountAuth.kt               # Firebase Auth wrapper (sign-in, create, secondary)
    │   └── AuthScreens.kt               # LoginScreen, RoleSelectionScreen,
    │                                    # TeacherRegistrationScreen, StudentRegistrationScreen,
    │                                    # AppNavigation (root router composable)
    ├── admin/
    │   ├── AdminDashboardScreen.kt      # Admin home tabs
    │   ├── AdminUsersScreen.kt          # Teacher/student management
    │   └── AdminDialogs.kt              # Admin-specific dialogs
    ├── teacher/
    │   ├── TeacherDashboardScreen.kt    # Teacher home tabs
    │   ├── TeacherSessionsScreen.kt     # Session/slot management
    │   ├── TeacherStudentsScreen.kt     # Student list per slot
    │   └── TeacherDialogs.kt            # Teacher-specific dialogs
    ├── student_parent/
    │   ├── ParentDashboardScreen.kt     # Parent home
    │   ├── ParentChildDetailsScreen.kt  # Child booking details
    │   ├── StudentParentShared.kt       # Shared parent/student components
    │   └── StudentScreens.kt            # Student home screen
    └── quiz/
        ├── QuizBuilderScreen.kt         # Teacher quiz creation + AI paper scan
        ├── QuizTakingScreen.kt          # Student quiz-taking flow
        ├── QuizResultScreen.kt          # Results display
        └── QuizComponents.kt            # Shared quiz UI components
```

---

## 3. القواعد الصارمة والخطوط الحمراء (Strict Guardrails & Immutable Rules)

1. **ضمان ثبات منطق الأعمال بنسبة 100% (Zero-Logic-Change Guarantee):**
   - الحفاظ التام على كامل قواعد العمل: احتساب مدة الاشتراكات (120 يوماً للترم)، شروط القبول، الصلاحيات الإدارية، حجز الحصص، تقييمات المعلمين، كشف غياب ولي الأمر، ومعالجة أوراق الامتحانات عبر Webhook.
2. **ثبات مسارات ومفاتيح Firebase (Realtime Database Schema):**
   - يُمنع منعاً باتاً تغيير أو إعادة تسمية عقد ومفاتيح Firebase الحالية:
     `teachers/`, `students/`, `schedules/`, `admins/`, `quizzes/`, `indexes/`, `terms/`, `phone_index/`, `quiz_papers/`, `notifications/`
   - ثبات أسماء الحقول (Field Names) داخل كائنات JSON لضمان عدم حدوث أي تراجع أو فقدان لبيانات المستخدمين النشطين.
3. **ثبات واستقرار واجهات Compose واللغتين:**
   - الحفاظ التام على دعم اللغتين (العربية والإنجليزية) عبر دالة `tr(...)` في `AppHelpers.kt`.
   - **قاعدة Compose Hoisting:** عدم استدعاء أي دالة `@Composable` داخل دوال غير كومبوزابل (مثل `onClick`, `Toast.makeText`, `joinToString`)؛ يتم استخراج النصوص دائماً مسبقاً (Hoisted String).
   - تثبيت مفاتيح القوائم (`key = { it.id }`) في جميع عناصر `LazyColumn` و `LazyRow` لمنع الـ Recomposition غير الضروري وضمان أعلى سلاسة في التمرير (60/120 FPS).
4. **تدرج المسؤوليات والاعتماديات النظيفة (Unidirectional Dependencies):**
   - `features/` ← تستورد من `ui/components/` و `data/` و `services/`.
   - `ui/components/` ← تستورد من `data/model/` و `services/` فقط. **لا تستورد من `features/`.**
   - `MainActivity` ← تستورد من `features/auth/AppNavigation` و `ui/components/MrPrivateTheme` و `ui/components/restoreLoginState` فقط.

---

## 4. قائمة التحقق الإلزامية لسلامة الكود (Mandatory Safety Checklist)

عند إنشاء أو نقل أو فصل أي ملف برمجي (`.kt`):
1. **تحديث الـ Package:** تعديل تعريف الحزمة أعلى الملف بما يطابق موقعه الجديد.
2. **تحديث الـ Imports:** تصحيح كافة مسارات الاستيراد في الملفات المرتبطة تلقائياً.
3. **محددات الرؤية (Visibility Modifiers):** تحويل الدوال والمساعدات المشتركة بين الملفات إلى `internal` عند الحاجة.
4. **تسجيل المكونات في `AndroidManifest.xml`:** التأكد من المسارات الكاملة للأنشطة والخدمات ومستقبلات البث.
5. **قواعد R8 / ProGuard (`app/src/main/keepRules/rules.keep`):** تغطي `data.**`, `services.**`, `ui.components.**`, `features.**` وحقول كائنات Firebase.
6. **فحص الترجمة والبناء:** التحقق من نجاح `./gradlew compileDebugKotlin compileDebugAndroidTestKotlin assembleDebug` بـ 0 errors.

---

## 5. حالة المشروع الحالية (Architecture Milestone)
- **Phase 5B Completed:** تم اكتمال الهيكلة المعمارية المعتمدة (Elite Architecture) بنسبة 100%.
- **All Layers Verified:** الكود مقسم ونظيف بالكامل عبر `data/`, `services/`, `ui/components/`, `features/`, و `Root`.
- **R8 / ProGuard Protection:** ملف `app/src/main/keepRules/rules.keep` مفعل ويحمي كافة الحزم الجديدة وموديلات Firebase.
- **Build Status:** نجاح البناء والترجمة الكاملة واختبارات الـ AndroidTest وصدور ملف `app-debug.apk` بنجاح بدون أخطاء.
