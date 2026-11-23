# CLAUDE.md — MRPrivate Project Rules (Elite Architecture)

## 1. Project Identity & Tech Stack
- **App Name:** MRPrivate (`com.example.mrprivate`)
- **Platform:** Native Android (Kotlin, Jetpack Compose, Material 3)
- **Backend:** Firebase Realtime Database, Firebase Auth, Firebase Cloud Functions (`firebase/`)
- **Primary Language & Direction:** Arabic UI (Full RTL layout support) with English/Arabic data fields.

## 2. Official "Elite" Directory Structure (STRICT)
All Kotlin source files live under `app/src/main/java/com/example/mrprivate/` and MUST strictly follow this hierarchy:

- **Root (`com.example.mrprivate/`):**
  - `MrPrivateApp.kt` (Application class registered in `AndroidManifest.xml` — keep minimal)
  - `MainActivity.kt` (Entry point & top-level routing ONLY — NEVER dump screens, models, or business logic here)
- **`data/` (Data & Backend Layer — YOUR PRIMARY DOMAIN):**
  - `data/firebase/FirebaseManager.kt` (Firebase initialization, database references, path helpers)
  - `data/repository/RoleDataRepository.kt` (Data repositories, CRUD operations, role-based queries)
- **`services/` (Background & System Services — YOUR PRIMARY DOMAIN):**
  - `services/PaperScanService.kt` (Paper scanning & OCR/image processing service)
  - `services/StudentNotificationScheduler.kt` (Scheduled student notifications & alarms)
- **`ui/` (Design System & Shared UI — READ-ONLY FOR YOU):**
  - `ui/theme/` (`Color.kt`, `Type.kt`, `Theme.kt`)
  - `ui/components/` (`ThemeAndComponents.kt`, `ScheduleComponents.kt`, `NewDesign.kt`)
- **`features/` (Role & Feature Screens):**
  - `features/auth/AccountAuth.kt`
  - `features/admin/AdminScreens.kt`
  - `features/teacher/TeacherScreens.kt`
  - `features/student_parent/StudentAndParentScreens.kt`
  - `features/student_parent/ParentScreens.kt`
  - `features/quiz/QuizSystemComponents.kt`

## 3. Your Role & Strict Boundaries (Claude Code)
Your mission is **Core Backend, Database Operations, Business Logic, and New Services**.

### ✅ ALLOWED:
1. Build and maintain repositories, Firebase queries, data models, and offline caching inside `data/`.
2. Create or update background services, schedulers, and hardware/file services inside `services/`.
3. Update `firebase/database.rules.v2.json` and `firebase/functions/index.js` when explicitly requested for new backend features.
4. Wire data states, ViewModels/repositories, and callbacks into `features/*` screens using existing `ui/components/*` building blocks.

### 🚫 STRICTLY FORBIDDEN:
1. **DO NOT** modify `ui/theme/*` (`Color.kt`, `Type.kt`, `Theme.kt`), `ui/components/*`, or `app/src/main/res/*` (colors, typography, icons, and visual styles are managed by Figma/UI).
2. **DO NOT** add new screens, helper functions, or data classes directly inside `MainActivity.kt` or `MrPrivateApp.kt`.
3. **DO NOT** rename or restructure existing Firebase Realtime Database node keys or break existing role permissions (`admin`, `teacher`, `student`, `parent`) unless explicitly instructed.
4. **DO NOT** reorganize folder structures or move unrelated files (Architecture refactoring is handled by Gemini).
5. **DO NOT** commit or push directly to `main`. Always work on a dedicated branch (e.g., `feat/<feature-name>` or `fix/<issue-name>`).

## 4. Android Build, Manifest & Release Protection
- **ProGuard / R8 (`app/src/main/keepRules/rules.keep`):** Whenever you add or modify any data model serialized/deserialized with Firebase, ensure its package/class is covered by `rules.keep` so Release builds do not break.
- **Manifest (`app/src/main/AndroidManifest.xml`):** Whenever you add a new `Service`, `BroadcastReceiver`, `Activity`, or permission, register it with its full package path in `AndroidManifest.xml`.
- **Visibility Modifiers:** Respect `internal` and `private` scopes across packages; avoid exposing internal repository helpers publicly without reason.
- **Verification:** Before finishing any task, verify all imports, package headers, and ensure `./gradlew assembleDebug` compiles cleanly with zero errors.
