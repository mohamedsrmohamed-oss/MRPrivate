# CLAUDE.md — MRPrivate Project Rules (Elite Architecture — Phase 5 Final)

## 1. Project Identity & Tech Stack
- **App Name:** MRPrivate (`com.example.mrprivate`)
- **Platform:** Native Android (Kotlin, Jetpack Compose, Material 3)
- **Backend:** Firebase Realtime Database, Firebase Auth
- **Primary Language & Direction:** Arabic UI (Full RTL layout support) with English/Arabic data fields.

## 2. Official "Elite" Directory Structure (STRICT — Phase 5 Final)
All Kotlin source files live under `app/src/main/java/com/example/mrprivate/` and MUST strictly follow this hierarchy:

- **Root (`com.example.mrprivate/`):**
  - `MrPrivateApp.kt` — Application class registered in `AndroidManifest.xml`. Enables Firebase offline persistence. **Keep minimal.**
  - `MainActivity.kt` — **Lightweight entry point ONLY.** Calls `restoreLoginState()` then delegates to `AppNavigation()`. **Never add screens, models, or business logic here.**
  - `MrPrivateState.kt` — Unified app state (Compose-observable ViewModel-style). Contains all CRUD, Firebase listeners, and business logic.

- **`data/model/`:**
  - `AppModels.kt` — **ALL** shared data classes and enums used by Firebase. Annotated with `@IgnoreExtraProperties` / `@PropertyName`. **Never rename fields.**

- **`data/firebase/`:**
  - `FirebaseManager.kt` — Firebase DB URL and reference helpers.

- **`data/repository/`:**
  - `RoleDataRepository.kt` — Firebase real-time listeners, role-based CRUD ops, `LoginResult` enum.

- **`services/`:**
  - `PaperScanService.kt` — Paper scanning, OCR, Webhook integration.
  - `StudentNotificationScheduler.kt` — AlarmManager, BroadcastReceiver, push notifications.

- **`ui/components/`:**
  - `AppHelpers.kt` — `tr()`, `trValue()`, `clearLoginPrefs()`, `saveLoginPrefs()`, **`restoreLoginState()`**, date/schedule helpers, network utils, `uriToBase64()`.
  - `AppCoreComponents.kt` — `GradientTheme`, `appGradientThemes`, `MrPrivateTheme`, shared Compose building blocks.
  - `AppDialogsAndBadges.kt` — Shared dialogs and badge components.
  - `ScheduleComponents.kt` — Schedule slot cards, booking UI.
  - `NewDesign.kt` — New design components & animations.

- **`features/auth/`:**
  - `AccountAuth.kt` — Firebase Auth wrapper (sign-in, sign-up, secondary account creation).
  - `AuthScreens.kt` — `LoginScreen`, `RoleSelectionScreen`, `TeacherRegistrationScreen`, `StudentRegistrationScreen`, **`AppNavigation`** (the root router `@Composable`).

- **`features/admin/`:** `AdminDashboardScreen.kt`, `AdminUsersScreen.kt`, `AdminDialogs.kt`
- **`features/teacher/`:** `TeacherDashboardScreen.kt`, `TeacherSessionsScreen.kt`, `TeacherStudentsScreen.kt`, `TeacherDialogs.kt`
- **`features/student_parent/`:** `ParentDashboardScreen.kt`, `ParentChildDetailsScreen.kt`, `StudentParentShared.kt`, `StudentScreens.kt`
- **`features/quiz/`:** `QuizBuilderScreen.kt`, `QuizTakingScreen.kt`, `QuizResultScreen.kt`, `QuizComponents.kt`

## 3. Your Role & Strict Boundaries (Claude Code)
Your mission is **Core Backend, Database Operations, Business Logic, and New Services**.

### ✅ ALLOWED:
1. Build and maintain repositories, Firebase queries, data models, and offline caching inside `data/`.
2. Create or update background services, schedulers, and hardware/file services inside `services/`.
3. Wire data states, callbacks, and repositories into `features/*` screens using existing `ui/components/*` building blocks.
4. Update `firebase/database.rules.v2.json` and `firebase/functions/index.js` when explicitly requested.

### 🚫 STRICTLY FORBIDDEN:
1. **DO NOT** modify `ui/components/*` visual styles, colors, typography, or icons — managed by Figma/UI.
2. **DO NOT** add new screens, helper functions, or data classes directly inside `MainActivity.kt` or `MrPrivateApp.kt`.
3. **DO NOT** rename or restructure existing Firebase Realtime Database node keys or break existing role permissions (`admin`, `teacher`, `student`, `parent`) unless explicitly instructed.
4. **DO NOT** reorganize folder structures or move files (Architecture refactoring is handled by Gemini/Antigravity).
5. **DO NOT** commit or push directly to `main`. Always work on a dedicated branch (e.g., `feat/<feature-name>` or `fix/<issue-name>`).

## 4. Android Build, Manifest & Release Protection
- **ProGuard / R8 (`app/src/main/keepRules/rules.keep`):** Covers `data.**`, `services.**`, `ui.components.**`, `features.**` and all Firebase model fields. If you add a new data model, ensure it falls under `data.model.**`.
- **Manifest (`app/src/main/AndroidManifest.xml`):** Register every new `Service`, `BroadcastReceiver`, `Activity`, or permission with its full package path.
- **Verification:** Before finishing any task, verify all imports, package headers, and ensure `./gradlew assembleDebug` compiles cleanly with zero errors.

## 5. Dependency Flow (Unidirectional)
```
MainActivity  →  features/auth/AppNavigation
features/**   →  ui/components/** + data/** + services/**
ui/components →  data/model/** + services/**   (NEVER imports from features/)
data/**       →  (no internal cross-dependencies)
services/**   →  data/model/**
```
