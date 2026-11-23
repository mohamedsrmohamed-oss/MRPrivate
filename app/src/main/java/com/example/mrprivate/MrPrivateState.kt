@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import android.content.Context
import androidx.compose.runtime.*
import com.example.mrprivate.data.model.*
import com.example.mrprivate.data.repository.*
import com.example.mrprivate.features.auth.AccountAuth
import com.example.mrprivate.features.auth.AuthError
import com.example.mrprivate.ui.components.*
import com.google.firebase.database.*
import java.util.UUID



class MrPrivateState(val isPreviewMode: Boolean = false) {
    private var adminsRef: DatabaseReference? = null
    private var teachersRef: DatabaseReference? = null
    private var studentsRef: DatabaseReference? = null
    private var schedulesRef: DatabaseReference? = null
    private var quizzesRef: DatabaseReference? = null
    private var settingsRef: DatabaseReference? = null
    private var papersRef: DatabaseReference? = null
    private val purgeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val purgeTask = object : Runnable {
        override fun run() {
            purgeExpiredPapers()
            purgeHandler.postDelayed(this, PAPER_PURGE_INTERVAL_MS)
        }
    }

    var isArabic by mutableStateOf(true)
    var currentScreen by mutableStateOf("login")
    var isLoading by mutableStateOf(false)
    // مؤشرات اكتمال أول تحميل من Firebase (تُستخدم لعرض Skeleton بدل شاشة فارغة)
    var studentsLoaded by mutableStateOf(isPreviewMode)
    var schedulesLoaded by mutableStateOf(isPreviewMode)
    val isDataReady: Boolean get() = studentsLoaded && schedulesLoaded
    /** حالة الاتصال بقاعدة البيانات (.info/connected) — للتحقق قبل العمليات الحساسة أوفلاين */
    var isDbConnected by mutableStateOf(true)
    /** هل توجد صفحة تالية في دليل المعلمين (Cursor-based Pagination) */
    var hasMoreTeachers by mutableStateOf(false)
    private var repo: RoleDataRepository? = null
    private var pendingSession: Pair<String, String>? = null
    var currentThemeIndex by mutableIntStateOf(0)
    var termSettings by mutableStateOf(TermSettings())
    val currentGradientTheme: GradientTheme get() = appGradientThemes.getOrElse(currentThemeIndex) { appGradientThemes[0] }

    val admins = mutableStateListOf<AdminAccount>()
    val teachers = mutableStateListOf<TeacherProfile>()
    val students = mutableStateListOf<StudentProfile>()
    val schedules = mutableStateListOf<ScheduleSlot>()
    val quizzes = mutableStateListOf<QuizExam>()

    // تغيير المستخدم النشط يبدأ/يوقف تحميل البيانات الخاصة بدوره فقط (RoleDataRepository)
    private var _activeAdminId by mutableStateOf<String?>(null)
    private var _activeTeacherId by mutableStateOf<String?>(null)
    private var _activeStudentId by mutableStateOf<String?>(null)
    private var _selectedTeacherIdForStudent by mutableStateOf<String?>(null)

    var activeAdminId: String?
        get() = _activeAdminId
        set(value) { if (_activeAdminId != value) { _activeAdminId = value; onActiveUserChanged("admin", value) } }
    var activeTeacherId: String?
        get() = _activeTeacherId
        set(value) { if (_activeTeacherId != value) { _activeTeacherId = value; onActiveUserChanged("teacher", value) } }
    var activeStudentId: String?
        get() = _activeStudentId
        set(value) { if (_activeStudentId != value) { _activeStudentId = value; onActiveUserChanged("student", value) } }
    /** عند اختيار الطالب لمعلم نجلب مواعيده فقط */
    var selectedTeacherIdForStudent: String?
        get() = _selectedTeacherIdForStudent
        set(value) { _selectedTeacherIdForStudent = value; repo?.listenTeacherSlots(value) }
    var targetStudentForBookingId by mutableStateOf<String?>(null)
    var openChildId by mutableStateOf<String?>(null)
    var openChildTab by mutableIntStateOf(0)

    val activeAdmin: AdminAccount? get() = admins.find { it.id == activeAdminId }
    val activeTeacher: TeacherProfile? get() = teachers.find { it.id == activeTeacherId }
    val activeStudent: StudentProfile? get() = students.find { it.id == activeStudentId }
    val targetStudentForBooking: StudentProfile? get() = students.find { it.id == targetStudentForBookingId } ?: activeStudent
    val selectedTeacherForStudent: TeacherProfile? get() = teachers.find { it.id == selectedTeacherIdForStudent }

    init {
        seedDefaultData()
        if (!isPreviewMode) {
            connectToFirebaseSafely()
        }
    }

    private fun seedDefaultData() {
        admins.add(AdminAccount(id = "admin_m1", username = "M1", canManageAdmins = true, canManageUsers = true, canDeleteAccounts = true))

        val todayKey = getTodayDateKey()
        val accA1 = StudentProfile(
            id = "acc_a1", username = "a1", name = "طالب تجريبي a1",
            phone = "01000000001", governorate = "الجيزة", area = "حدائق أكتوبر",
            educationTrack = "عربي", stage = "ثانوي", grade = "الصف الأول الثانوي", isParent = false, parentId = "acc_a2",
            subjectPlans = mapOf(
                "حاسب آلي" to SubjectPlanData("حاسب آلي", bookPages = 180, lessonsCount = 12, totalUnits = 10, completedUnits = 7, studyDaysPerWeek = 3),
                "برمجة" to SubjectPlanData("برمجة", bookPages = 150, lessonsCount = 10, totalUnits = 10, completedUnits = 5, studyDaysPerWeek = 2),
                "لغة عربية" to SubjectPlanData("لغة عربية", bookPages = 220, lessonsCount = 16, totalUnits = 10, completedUnits = 8, studyDaysPerWeek = 3),
                "رياضيات" to SubjectPlanData("رياضيات", bookPages = 200, lessonsCount = 14, totalUnits = 10, completedUnits = 6, studyDaysPerWeek = 3)
            ),
            dailyTimetable = mapOf(
                todayKey to DailyScheduleData(
                    dateKey = todayKey,
                    isVacation = false,
                    hourlySlots = mapOf("08:00" to "لغة عربية", "09:00" to "Break", "16:00" to "حاسب آلي", "18:00" to "برمجة")
                )
            )
        )
        students.add(accA1)

        val accA2 = StudentProfile(
            id = "acc_a2", username = "a2", name = "ولي أمر تجريبي a2",
            phone = "01000000002", governorate = "الجيزة", area = "حدائق أكتوبر",
            educationTrack = "عربي", stage = "ولي أمر", isParent = true, linkedStudentUsername = "a1",
            canAddStudentApproved = true, registeredStudentIds = listOf("acc_a1")
        )
        students.add(accA2)

        val accA3 = TeacherProfile(
            id = "acc_a3", username = "a3", firstName = "معلم", secondName = "معتمد a3",
            phone = "01000000003", governorate = "الجيزة", area = "حدائق أكتوبر", educationTrack = "عربي",
            stages = listOf("إعدادي", "ثانوي", "بكالوريا"), subject = "حاسب آلي", isApproved = true,
            isPremium = true, isUltimate = true,
            subscriptionActivatedAtMillis = System.currentTimeMillis(),
            subscriptionExpiresAtMillis = System.currentTimeMillis() + (120L * 24 * 60 * 60 * 1000),
            ratings = listOf(5, 5)
        )
        teachers.add(accA3)

        val slotA3 = ScheduleSlot(
            id = "slot_a3_1", teacherId = accA3.id, stage = "ثانوي", grade = "الصف الأول الثانوي", day = "السبت", time = "05:00 مساءً",
            maxStudents = 5, price = "200", isPrivate = false, isOnline = true,
            isRecurringYearly = true, academicYearWeeks = 14,
            recurringDates = generateYearlySessionDates("السبت", 14),
            enrollments = listOf(
                StudentEnrollment(
                    id = "enroll_a1_1", studentId = accA1.id, studentName = accA1.name,
                    studentPhone = accA1.phone, studentStage = "ثانوي", studentGrade = "الصف الأول الثانوي",
                    parentName = accA2.name, parentPhone = accA2.phone,
                    isConfirmed = true, isCompleted = true, attended = true,
                    teacherRatingForStudent = 5,
                    parentOnlyNote = "مستوى الطالب ممتاز في البرمجة والتزم بحل التدريبات كاملة."
                )
            )
        )
        schedules.add(slotA3)

        val sampleQuiz = QuizExam(
            id = "quiz_sample_1",
            teacherId = accA3.id,
            teacherName = accA3.fullName,
            subject = accA3.subject,
            slotId = slotA3.id,
            groupLabel = "${slotA3.day} - ${slotA3.time}",
            title = "كويز الوحدة الأولى (حاسب آلي)",
            durationMinutes = 10,
            totalScore = 20,
            createdAt = todayKey,
            questions = listOf(
                QuizQuestion(
                    id = "q1",
                    questionText = "أي مما يلي يُعد من وحدات الإدخال في الحاسب الآلي؟",
                    options = listOf("لوحة المفاتيح (Keyboard)", "الشاشة (Monitor)", "الطابعة (Printer)", "السماعات (Speakers)"),
                    correctOptionIndex = 0
                ),
                QuizQuestion(
                    id = "q2",
                    questionText = "تُقاس سرعة المعالج الدقيق (CPU) بوحدة:",
                    options = listOf("البايت (Byte)", "الهرتز (GHz)", "البكسل (Pixel)", "البوصة (Inch)"),
                    correctOptionIndex = 1
                )
            ),
            targetStudentIds = listOf(accA1.id),
            submissions = emptyMap()
        )
        quizzes.add(sampleQuiz)

        val accA4 = TeacherProfile(
            id = "acc_a4", username = "a4", firstName = "معلم", secondName = "انتظار a4",
            phone = "01000000004", governorate = "الجيزة", area = "حدائق الأهرام", educationTrack = "لغات",
            stages = listOf("ثانوي"), subject = "Programming", isApproved = false
        )
        teachers.add(accA4)
    }

    private fun onActiveUserChanged(kind: String, id: String?) {
        if (isPreviewMode) return
        if (id == null) {
            if (_activeAdminId == null && _activeTeacherId == null && _activeStudentId == null) {
                pendingSession = null
                repo?.stopSession()
            }
            return
        }
        val r = repo
        if (r == null) pendingSession = kind to id else r.startSession(kind, id)
    }

    private fun connectToFirebaseSafely() {
        try {
            val db = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
            adminsRef = db.getReference("admins")
            teachersRef = db.getReference("teachers")
            studentsRef = db.getReference("students")
            schedulesRef = db.getReference("schedules")
            quizzesRef = db.getReference("quizzes")
            settingsRef = db.getReference("settings")
            papersRef = db.getReference("quiz_papers")

            teachers.clear(); students.clear(); schedules.clear(); quizzes.clear()

            val repository = RoleDataRepository(this, db)
            repo = repository
            repository.startGlobalWatchers()
            pendingSession?.let { (kind, id) ->
                pendingSession = null
                repository.startSession(kind, id)
            }

            purgeHandler.removeCallbacks(purgeTask)
            purgeHandler.postDelayed(purgeTask, PAPER_PURGE_INTERVAL_MS)
        } catch (e: Exception) {}
    }

    fun applyTermsSnapshot(snapshot: DataSnapshot) {
        snapshot.getValue(TermSettings::class.java)?.let { termSettings = it }
    }

    fun loadMoreTeachers() { repo?.loadMoreDirectory() }

    private fun writeSlot(slot: ScheduleSlot) {
        schedulesRef?.child(slot.id)?.setValue(slot)
        repo?.indexSlot(slot)
    }

    fun saveTermSettings(ts: TermSettings) {
        termSettings = ts
        settingsRef?.child("terms")?.setValue(ts)
    }

    fun isUsernameReserved(username: String): Boolean = username.trim().startsWith("*")

    fun isUsernameTaken(username: String): Boolean = false

    private fun writePhoneIndex(phone: String, username: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 6 || username.isBlank()) return
        FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("phone_index/$digits").setValue(username.trim())
    }

    fun loginAsync(context: Context, role: String, identifier: String, pass: String, onResult: (LoginResult) -> Unit) {
        val clean = identifier.trim()
        fun proceed(usernameOrEmail: String) {
            AccountAuth.signIn(usernameOrEmail, pass) { r ->
                val uid = r.uid
                if (uid == null) {
                    onResult(if (r.error == AuthError.NETWORK) LoginResult.NETWORK_ERROR else LoginResult.WRONG_CREDENTIALS)
                    return@signIn
                }
                val db = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
                fun reject(networkIssue: Boolean) {
                    AccountAuth.signOut()
                    onResult(if (networkIssue) LoginResult.NETWORK_ERROR else LoginResult.WRONG_CREDENTIALS)
                }
                when (role) {
                    "admin" -> db.getReference("admins/$uid").get().addOnCompleteListener { t ->
                        val admin = if (t.isSuccessful) parseAdminSnapshot(t.result) else null
                        if (admin == null) { reject(!t.isSuccessful); return@addOnCompleteListener }
                        activeAdminId = uid
                        repo?.preloadAdmin(admin)
                        currentScreen = "admin_dashboard"
                        saveLoginPrefs(context, role, uid)
                        onResult(LoginResult.SUCCESS)
                    }
                    "teacher" -> db.getReference("teachers/$uid").get().addOnCompleteListener { t ->
                        val teacher = if (t.isSuccessful) parseTeacherSnapshot(t.result) else null
                        if (teacher == null) { reject(!t.isSuccessful); return@addOnCompleteListener }
                        activeTeacherId = uid
                        repo?.preloadTeacher(teacher)
                        currentScreen = "teacher_dashboard"
                        saveLoginPrefs(context, role, uid)
                        onResult(LoginResult.SUCCESS)
                    }
                    else -> db.getReference("students/$uid").get().addOnCompleteListener { t ->
                        val st = if (t.isSuccessful) parseStudentSnapshot(t.result) else null
                        if (st == null || st.isParent != (role == "parent")) { reject(!t.isSuccessful); return@addOnCompleteListener }
                        activeStudentId = uid
                        repo?.preloadStudent(st)
                        currentScreen = "student_dashboard"
                        saveLoginPrefs(context, role, uid)
                        onResult(LoginResult.SUCCESS)
                    }
                }
            }
        }
        if (role != "admin" && clean.isNotEmpty() && clean.all { it.isDigit() }) {
            FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("phone_index/$clean").get()
                .addOnCompleteListener { t ->
                    val mapped = if (t.isSuccessful) t.result.getValue(String::class.java) else null
                    proceed(mapped ?: clean)
                }
        } else {
            proceed(clean)
        }
    }

    fun updateAccountPhoto(role: String, userId: String, base64Photo: String) {
        when (role) {
            "teacher" -> {
                val idx = teachers.indexOfFirst { it.id == userId }
                if (idx != -1) {
                    val updated = teachers[idx].copy(photoUriString = base64Photo)
                    teachers[idx] = updated
                    teachersRef?.child(userId)?.setValue(updated)
                }
            }
            "student", "parent" -> {
                val idx = students.indexOfFirst { it.id == userId }
                if (idx != -1) {
                    val updated = students[idx].copy(photoUriString = base64Photo)
                    students[idx] = updated
                    studentsRef?.child(userId)?.setValue(updated)
                }
            }
        }
    }

    fun registerTeacherAsync(teacher: TeacherProfile, password: String, onResult: (AuthError) -> Unit) {
        AccountAuth.createAccount(teacher.username, password) { r ->
            val uid = r.uid
            if (uid == null) { onResult(r.error); return@createAccount }
            val pendingTeacher = teacher.copy(id = uid, isApproved = false)
            teachersRef?.child(uid)?.setValue(pendingTeacher)
            activeTeacherId = uid
            repo?.preloadTeacher(pendingTeacher)
            currentScreen = "teacher_dashboard"
            onResult(AuthError.NONE)
        }
    }

    fun registerStudentAsync(student: StudentProfile, password: String, onResult: (AuthError) -> Unit) {
        AccountAuth.createAccount(student.username, password) { r ->
            val uid = r.uid
            if (uid == null) { onResult(r.error); return@createAccount }
            val profile = student.copy(id = uid)
            studentsRef?.child(uid)?.setValue(profile)
            writePhoneIndex(profile.phone, profile.username)
            activeStudentId = uid
            repo?.preloadStudent(profile)
            currentScreen = "student_dashboard"
            onResult(AuthError.NONE)
        }
    }

    fun addChildStudentAsync(child: StudentProfile, password: String, onResult: (StudentProfile?, AuthError) -> Unit) {
        AccountAuth.createAccountSecondary(child.username, password) { r ->
            val uid = r.uid
            if (uid == null) { onResult(null, r.error); return@createAccountSecondary }
            val profile = child.copy(id = uid)
            studentsRef?.child(uid)?.setValue(profile)
            writePhoneIndex(profile.phone, profile.username)
            if (students.none { it.id == uid }) students.add(profile)
            onResult(profile, AuthError.NONE)
        }
    }

    fun createAdminAsync(admin: AdminAccount, password: String, onResult: (AuthError) -> Unit) {
        AccountAuth.createAccountSecondary(admin.username, password) { r ->
            val uid = r.uid
            if (uid == null) { onResult(r.error); return@createAccountSecondary }
            adminsRef?.child(uid)?.setValue(admin.copy(id = uid))
            onResult(AuthError.NONE)
        }
    }

    fun addSchedule(slot: ScheduleSlot) {
        val window = currentTermWindow(termSettings)
        val dates = if (window != null) generateTermDates(slot.day, window.second, window.third) else generateYearlySessionDates(slot.day, 14)
        val finalSlot = slot.copy(academicYearWeeks = dates.size.coerceAtLeast(1), recurringDates = dates)
        schedules.add(finalSlot)
        writeSlot(finalSlot)
    }

    fun updateSchedule(updatedSlot: ScheduleSlot) {
        val idx = schedules.indexOfFirst { it.id == updatedSlot.id }
        if (idx != -1) schedules[idx] = updatedSlot
        writeSlot(updatedSlot)
    }

    fun deleteSchedule(slotId: String) {
        schedules.find { it.id == slotId }?.let { repo?.unindexSlot(it) }
        schedules.removeAll { it.id == slotId }
        schedulesRef?.child(slotId)?.removeValue()
    }

    fun bookSlot(slot: ScheduleSlot, student: StudentProfile) {
        val parent = students.find { it.id == student.parentId } ?: activeStudent?.takeIf { it.isParent }
        val newEnrollment = StudentEnrollment(
            studentId = student.id,
            studentName = student.name,
            studentPhone = student.phone,
            studentStage = student.stage,
            studentGrade = student.grade.ifBlank { slot.grade.ifBlank { student.stage } },
            parentName = parent?.name ?: "",
            parentPhone = parent?.phone ?: student.phone,
            isConfirmed = false,
            hasRated = false
        )
        val localUpdated = slot.copy(enrollments = slot.enrollments + newEnrollment)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = localUpdated

        val ref = schedulesRef ?: return
        ref.child(slot.id).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(ScheduleSlot::class.java)
                    ?: return Transaction.success(currentData)
                if (current.enrollments.any { it.studentId == student.id }) return Transaction.abort()
                currentData.value = current.copy(enrollments = current.enrollments + newEnrollment)
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error == null) repo?.indexSlot(localUpdated)
            }
        })
    }

    private fun parseHour24(time: String): Int {
        val m = Regex("(\\d{1,2})\\s*:\\s*(\\d{2})").find(time) ?: return 17
        var h = m.groupValues[1].toIntOrNull() ?: 17
        val pm = time.contains("مساء") || time.contains("PM", ignoreCase = true)
        val am = time.contains("صباح") || time.contains("AM", ignoreCase = true)
        if (pm && h < 12) h += 12
        if (am && h == 12) h = 0
        return h.coerceIn(0, 23)
    }

    fun checkGroupConflictForSlotEdit(slot: ScheduleSlot, newDay: String, newTime: String): String? {
        val proposedHour = parseHour24(newTime)
        val confirmedEnrollments = slot.enrollments.filter { it.isConfirmed }

        for (enroll in confirmedEnrollments) {
            val st = students.find { it.id == enroll.studentId } ?: continue
            val conflictOtherSlot = schedules.firstOrNull { other ->
                other.id != slot.id &&
                other.day == newDay &&
                kotlin.math.abs(parseHour24(other.time) - proposedHour) < 3 &&
                other.enrollments.any { it.studentId == st.id && it.isConfirmed }
            }
            if (conflictOtherSlot != null) {
                return "لا يمكن اعتماد هذا الموعد! الطالب المؤكد (${st.name}) لديه حجز مؤكد آخر يوم $newDay الساعة ${conflictOtherSlot.time} (ضمن نطاق 3 ساعات)."
            }
        }
        return null
    }

    fun pushSessionNotification(
        teacherId: String,
        slotId: String,
        title: String,
        body: String,
        studentIds: List<String>
    ) {
        if (studentIds.isEmpty()) return
        val notif = SessionNotificationItem(
            teacherId = teacherId,
            slotId = slotId,
            title = title,
            body = body,
            targetStudentIds = studentIds
        )
        try {
            FirebaseDatabase.getInstance().getReference("notifications").child(notif.id).setValue(notif)
        } catch (_: Exception) { }
    }

    fun updateScheduleByTeacher(oldSlot: ScheduleSlot, updatedSlot: ScheduleSlot): String? {
        val conflictMsg = checkGroupConflictForSlotEdit(oldSlot, updatedSlot.day, updatedSlot.time)
        if (conflictMsg != null) return conflictMsg

        val window = currentTermWindow(termSettings)
        val oldDates = if (window != null) generateTermDates(oldSlot.day, window.second, window.third) else oldSlot.recurringDates
        val newDates = if (window != null) generateTermDates(updatedSlot.day, window.second, window.third) else generateYearlySessionDates(updatedSlot.day, 14)
        val oldHourKey = slotHourKey(oldSlot.time)
        val newHourKey = slotHourKey(updatedSlot.time)
        val slotTeacher = teachers.find { it.id == updatedSlot.teacherId }
        val confirmedStudentIds = oldSlot.enrollments.filter { it.isConfirmed }.map { it.studentId }

        if (slotTeacher != null) {
            val label = "${slotTeacher.subject} (${slotTeacher.fullName})"
            oldSlot.enrollments.filter { it.isConfirmed }.forEach { enroll ->
                val stIdx = students.indexOfFirst { it.id == enroll.studentId }
                if (stIdx != -1) {
                    val st = students[stIdx]
                    val map = st.dailyTimetable.toMutableMap()
                    oldDates.forEach { dKey ->
                        val dayData = map[dKey] ?: return@forEach
                        if (dayData.hourlySlots[oldHourKey] == label) {
                            map[dKey] = dayData.copy(hourlySlots = dayData.hourlySlots - oldHourKey)
                        }
                    }
                    newDates.forEach { dKey ->
                        val existingDay = map[dKey] ?: DailyScheduleData(dateKey = dKey)
                        if (!existingDay.isVacation) {
                            map[dKey] = existingDay.copy(hourlySlots = existingDay.hourlySlots + (newHourKey to label))
                        }
                    }
                    val newSt = st.copy(dailyTimetable = map)
                    students[stIdx] = newSt
                    studentsRef?.child(newSt.id)?.setValue(newSt)
                }
            }
            pushSessionNotification(
                teacherId = updatedSlot.teacherId,
                slotId = updatedSlot.id,
                title = "تحديث موعد حصة - ${slotTeacher.subject}",
                body = "قام ${slotTeacher.fullName} بتعديل موعد الحصة إلى يوم ${updatedSlot.day} الساعة ${updatedSlot.time}.",
                studentIds = confirmedStudentIds
            )
        }

        val finalSlot = updatedSlot.copy(academicYearWeeks = newDates.size.coerceAtLeast(1), recurringDates = newDates)
        val idx = schedules.indexOfFirst { it.id == finalSlot.id }
        if (idx != -1) schedules[idx] = finalSlot
        writeSlot(finalSlot)
        return null
    }

    fun cancelScheduleByTeacher(slot: ScheduleSlot) {
        val slotTeacher = teachers.find { it.id == slot.teacherId }
        val confirmedStudentIds = slot.enrollments.filter { it.isConfirmed }.map { it.studentId }

        if (slotTeacher != null) {
            val label = "${slotTeacher.subject} (${slotTeacher.fullName})"
            val hourKey = slotHourKey(slot.time)
            confirmedStudentIds.forEach { stId ->
                val stIdx = students.indexOfFirst { it.id == stId }
                if (stIdx != -1) {
                    val st = students[stIdx]
                    val updatedMap = st.dailyTimetable.mapValues { (_, dayData) ->
                        if (dayData.hourlySlots[hourKey] == label) {
                            dayData.copy(hourlySlots = dayData.hourlySlots - hourKey)
                        } else dayData
                    }
                    val updatedSt = st.copy(dailyTimetable = updatedMap)
                    students[stIdx] = updatedSt
                    studentsRef?.child(updatedSt.id)?.setValue(updatedSt)
                }
            }
            pushSessionNotification(
                teacherId = slot.teacherId,
                slotId = slot.id,
                title = "إلغاء حصة - ${slotTeacher.subject}",
                body = "قام ${slotTeacher.fullName} بإلغاء حصة يوم ${slot.day} الساعة ${slot.time}.",
                studentIds = confirmedStudentIds
            )
        }

        repo?.unindexSlot(slot)
        schedules.removeAll { it.id == slot.id }
        schedulesRef?.child(slot.id)?.removeValue()
    }

    fun addExtraSessionForGroup(baseSlot: ScheduleSlot, extraDay: String, extraTime: String): String? {
        val conflictMsg = checkGroupConflictForSlotEdit(baseSlot, extraDay, extraTime)
        if (conflictMsg != null) return conflictMsg

        val extraDates = generateYearlySessionDates(extraDay, 1)
        val extraSlot = baseSlot.copy(
            id = UUID.randomUUID().toString(),
            day = extraDay,
            time = extraTime.trim(),
            isExtraSession = true,
            isRecurringYearly = false,
            academicYearWeeks = 1,
            recurringDates = extraDates
        )
        schedules.add(extraSlot)
        writeSlot(extraSlot)

        val slotTeacher = teachers.find { it.id == baseSlot.teacherId }
        val confirmedStudentIds = baseSlot.enrollments.filter { it.isConfirmed }.map { it.studentId }
        if (slotTeacher != null) {
            val label = "${slotTeacher.subject} (${slotTeacher.fullName})"
            val hourKey = slotHourKey(extraTime)
            confirmedStudentIds.forEach { stId ->
                val stIdx = students.indexOfFirst { it.id == stId }
                if (stIdx != -1) {
                    val st = students[stIdx]
                    val map = st.dailyTimetable.toMutableMap()
                    extraDates.forEach { dKey ->
                        val existingDay = map[dKey] ?: DailyScheduleData(dateKey = dKey)
                        if (!existingDay.isVacation) {
                            map[dKey] = existingDay.copy(hourlySlots = existingDay.hourlySlots + (hourKey to label))
                        }
                    }
                    val updatedSt = st.copy(dailyTimetable = map)
                    students[stIdx] = updatedSt
                    studentsRef?.child(updatedSt.id)?.setValue(updatedSt)
                }
            }
            pushSessionNotification(
                teacherId = baseSlot.teacherId,
                slotId = extraSlot.id,
                title = "حصة إضافية (Extra Session) - ${slotTeacher.subject}",
                body = "أضاف ${slotTeacher.fullName} حصة إضافية للمجموعة يوم $extraDay الساعة $extraTime.",
                studentIds = confirmedStudentIds
            )
        }
        return null
    }

    fun reassignSavedQuizToGroups(sourceQuiz: QuizExam, targetSlots: List<ScheduleSlot>) {
        targetSlots.forEach { slot ->
            val targetIds = slot.enrollments.filter { it.isConfirmed }.map { it.studentId }
            val cloned = sourceQuiz.copy(
                id = UUID.randomUUID().toString(),
                slotId = slot.id,
                groupLabel = "${slot.stage} - ${slot.grade} (${slot.day} ${slot.time})",
                createdAt = getTodayDateKey(),
                targetStudentIds = targetIds,
                submissions = emptyMap()
            )
            createQuizExam(cloned)
        }
    }

    fun submitParentExcuseForSession(slot: ScheduleSlot, enrollmentId: String, excuseText: String) {
        val updatedList = slot.enrollments.map {
            if (it.id == enrollmentId) it.copy(parentExcuseNote = excuseText.trim()) else it
        }
        val updatedSlot = slot.copy(enrollments = updatedList)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = updatedSlot
        writeSlot(updatedSlot)
    }

    fun updateTeacherExtraAreas(teacher: TeacherProfile, extraAreas: List<String>) {
        val cleaned = if (teacher.effectiveIsPremium) extraAreas.filter { it.isNotBlank() && it != teacher.area }.distinct().take(2) else emptyList()
        updateTeacherProfile(teacher.copy(extraAreas = cleaned))
    }

    fun removeStudentFromSlot(slot: ScheduleSlot, enrollmentId: String) {
        val enrollToRemove = slot.enrollments.find { it.id == enrollmentId } ?: return
        val updatedList = slot.enrollments.filterNot { it.id == enrollmentId }
        val updatedSlot = slot.copy(enrollments = updatedList)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = updatedSlot
        writeSlot(updatedSlot)
        repo?.unindexStudent(slot.id, enrollToRemove.studentId)

        val slotTeacher = teachers.find { it.id == slot.teacherId }
        val stIdx = students.indexOfFirst { it.id == enrollToRemove.studentId }
        if (slotTeacher != null && stIdx != -1) {
            val st = students[stIdx]
            val label = "${slotTeacher.subject} (${slotTeacher.fullName})"
            val hourKey = slotHourKey(slot.time)
            val updatedMap = st.dailyTimetable.mapValues { (_, dayData) ->
                if (dayData.hourlySlots[hourKey] == label) {
                    dayData.copy(hourlySlots = dayData.hourlySlots - hourKey)
                } else dayData
            }
            val updatedSt = st.copy(dailyTimetable = updatedMap)
            students[stIdx] = updatedSt
            studentsRef?.child(updatedSt.id)?.setValue(updatedSt)
        }
    }

    fun confirmStudentBooking(slot: ScheduleSlot, enrollmentId: String) {
        val updatedList = slot.enrollments.map {
            if (it.id == enrollmentId) it.copy(isConfirmed = true) else it
        }
        val updatedSlot = slot.copy(enrollments = updatedList)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = updatedSlot
        writeSlot(updatedSlot)

        val confirmedEnroll = updatedList.find { it.id == enrollmentId }
        val slotTeacher = teachers.find { it.id == slot.teacherId }
        if (confirmedEnroll != null && slotTeacher != null) {
            val stIdx = students.indexOfFirst { it.id == confirmedEnroll.studentId }
            if (stIdx != -1) {
                val st = students[stIdx]
                val window = currentTermWindow(termSettings)
                val hourKey = slotHourKey(slot.time)
                val datesToPopulate = if (window == null) generateYearlySessionDates(slot.day, 14) else generateTermDates(slot.day, window.second, window.third)
                val updatedMap = st.dailyTimetable.toMutableMap()
                datesToPopulate.forEach { dKey ->
                    val existingDay = updatedMap[dKey] ?: DailyScheduleData(dateKey = dKey)
                    if (existingDay.isVacation) return@forEach
                    val slotsMap = existingDay.hourlySlots.toMutableMap()
                    slotsMap[hourKey] = "${slotTeacher.subject} (${slotTeacher.fullName})"
                    updatedMap[dKey] = existingDay.copy(hourlySlots = slotsMap)
                }
                var updatedStudent = st.copy(dailyTimetable = updatedMap)
                val plan = st.subjectPlans[slotTeacher.subject]
                if (plan != null && !plan.isFinished) {
                    updatedStudent = updatedStudent.copy(
                        dailyTimetable = autoDistributeSubjectPlanOnTimetable(updatedStudent, slotTeacher.subject, plan.studyDaysPerWeek, termSettings)
                    )
                }
                students[stIdx] = updatedStudent
                studentsRef?.child(updatedStudent.id)?.setValue(updatedStudent)
            }
        }
    }

    fun completeStudentSession(slot: ScheduleSlot, enrollmentId: String, stars: Int, note: String, attended: Boolean = true) {
        val updatedList = slot.enrollments.map {
            if (it.id == enrollmentId) {
                it.copy(
                    isCompleted = true,
                    attended = attended,
                    teacherRatingForStudent = if (attended) stars else 0,
                    parentOnlyNote = if (attended) note.trim() else note.trim().ifBlank { "الطالب لم يحضر الحصة" }
                )
            } else it
        }
        val updatedSlot = slot.copy(enrollments = updatedList)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = updatedSlot
        writeSlot(updatedSlot)
    }

    fun toggleStudentDailyStudyCompletion(studentId: String, dateKey: String, hourKey: String, subjectName: String) {
        val idx = students.indexOfFirst { it.id == studentId }
        if (idx == -1) return
        val st = students[idx]
        val dayData = st.dailyTimetable[dateKey] ?: DailyScheduleData(dateKey = dateKey)
        val currentDone = dayData.completedHours[hourKey] == true
        val newDone = !currentDone
        val updatedCompletedHours = dayData.completedHours.toMutableMap().apply { put(hourKey, newDone) }
        val updatedDay = dayData.copy(completedHours = updatedCompletedHours)

        val updatedPlans = st.subjectPlans.toMutableMap()
        val plan = updatedPlans[subjectName] ?: SubjectPlanData(subjectName = subjectName)
        val newCompletedLessons = if (newDone) plan.completedLessons + 1 else (plan.completedLessons - 1).coerceAtLeast(0)
        val newCompletedUnits = if (newDone) (plan.completedUnits + 1).coerceAtMost(plan.totalUnits.coerceAtLeast(1)) else (plan.completedUnits - 1).coerceAtLeast(0)
        updatedPlans[subjectName] = plan.copy(completedLessons = newCompletedLessons, completedUnits = newCompletedUnits)

        val updatedStudent = st.copy(
            dailyTimetable = st.dailyTimetable + (dateKey to updatedDay),
            subjectPlans = updatedPlans
        )
        students[idx] = updatedStudent
        studentsRef?.child(updatedStudent.id)?.updateChildren(
            mapOf(
                "dailyTimetable/$dateKey/dateKey" to dateKey,
                "dailyTimetable/$dateKey/completedHours/$hourKey" to newDone,
                "subjectPlans/$subjectName" to updatedPlans[subjectName]
            )
        )
    }

    fun submitTeacherRating(teacher: TeacherProfile, slot: ScheduleSlot, enrollmentId: String, stars: Int) {
        val updatedRatings = teacher.ratings.toMutableList().apply { add(stars) }
        val updatedTeacher = teacher.copy(ratings = updatedRatings)
        val tIdx = teachers.indexOfFirst { it.id == teacher.id }
        if (tIdx != -1) teachers[tIdx] = updatedTeacher
        teachersRef?.child(teacher.id)?.setValue(updatedTeacher)

        val updatedEnrollments = slot.enrollments.map {
            if (it.id == enrollmentId) it.copy(hasRated = true) else it
        }
        val updatedSlot = slot.copy(enrollments = updatedEnrollments)
        val sIdx = schedules.indexOfFirst { it.id == slot.id }
        if (sIdx != -1) schedules[sIdx] = updatedSlot
        writeSlot(updatedSlot)
    }

    fun approveTeacher(teacherId: String) {
        val idx = teachers.indexOfFirst { it.id == teacherId }
        if (idx != -1) {
            val updatedTeacher = teachers[idx].copy(isApproved = true)
            teachers[idx] = updatedTeacher
            teachersRef?.child(teacherId)?.setValue(updatedTeacher)
            teachersRef?.child(teacherId)?.child("isApproved")?.setValue(true)
            teachersRef?.child(teacherId)?.child("approved")?.setValue(true)
        }
    }

    fun updateTeacherProfile(updatedTeacher: TeacherProfile) {
        val idx = teachers.indexOfFirst { it.id == updatedTeacher.id }
        if (idx != -1) teachers[idx] = updatedTeacher
        teachersRef?.child(updatedTeacher.id)?.setValue(updatedTeacher)
        teachersRef?.child(updatedTeacher.id)?.child("isPremium")?.setValue(updatedTeacher.isPremium)
        teachersRef?.child(updatedTeacher.id)?.child("premium")?.setValue(updatedTeacher.isPremium)
    }

    fun deleteTeacher(teacherId: String) {
        teachers.removeAll { it.id == teacherId }
        teachersRef?.child(teacherId)?.removeValue()
    }

    fun updateStudentProfile(updatedStudent: StudentProfile) {
        val idx = students.indexOfFirst { it.id == updatedStudent.id }
        if (idx != -1) students[idx] = updatedStudent
        studentsRef?.child(updatedStudent.id)?.setValue(updatedStudent)
    }

    fun deleteStudent(studentId: String) {
        students.removeAll { it.id == studentId }
        studentsRef?.child(studentId)?.removeValue()
    }

    fun addOrUpdateAdmin(admin: AdminAccount) {
        val idx = admins.indexOfFirst { it.id == admin.id }
        if (idx != -1) admins[idx] = admin else admins.add(admin)
        adminsRef?.child(admin.id)?.setValue(admin)
    }

    fun deleteAdmin(adminId: String) {
        admins.removeAll { it.id == adminId }
        adminsRef?.child(adminId)?.removeValue()
    }

    fun createQuizExam(quiz: QuizExam) {
        val now = System.currentTimeMillis()
        val image = quiz.paperImageBase64
        val hasPaper = image.isNotBlank()
        val expiresAt = if (hasPaper) now + PAPER_RETENTION_MS else 0L
        val slim = quiz.copy(paperImageBase64 = "", paperExpiresAt = expiresAt)

        quizzes.add(0, slim)
        quizzesRef?.child(slim.id)?.setValue(slim)
        if (hasPaper) {
            papersRef?.child(slim.id)?.setValue(
                mapOf(
                    "quizId" to slim.id,
                    "teacherId" to slim.teacherId,
                    "imageBase64" to image,
                    "createdAt" to now,
                    "expiresAt" to expiresAt
                )
            )
        }
        if (slim.targetStudentIds.isNotEmpty()) {
            pushSessionNotification(
                teacherId = slim.teacherId,
                slotId = slim.slotId,
                title = "📝 اختبار جديد: ${slim.title}",
                body = "أرسل لك المعلم ${slim.teacherName} اختباراً جديداً في مادة ${slim.subject}",
                studentIds = slim.targetStudentIds
            )
        }
    }

    fun deleteQuizExam(quizId: String) {
        quizzes.removeAll { it.id == quizId }
        quizzesRef?.child(quizId)?.removeValue()
        papersRef?.child(quizId)?.removeValue()
    }

    fun purgeExpiredPapers() {
        val now = System.currentTimeMillis()
        val today = getTodayDateKey()
        for (q in quizzes.toList()) {
            if (q.paperExpiresAt in 1..now) {
                papersRef?.child(q.id)?.removeValue()?.addOnSuccessListener {
                    quizzesRef?.child(q.id)?.child("paperExpiresAt")?.setValue(0L)
                }
            }
            if (q.paperImageBase64.isNotBlank() && q.paperExpiresAt == 0L && q.createdAt.isNotBlank() && q.createdAt < today) {
                quizzesRef?.child(q.id)?.child("paperImageBase64")?.setValue("")
            }
        }
    }

    fun submitStudentQuizAnswers(quiz: QuizExam, student: StudentProfile, selectedAnswers: List<Int>): QuizSubmission {
        val totalQ = quiz.questions.size.coerceAtLeast(1)
        var correctCount = 0
        quiz.questions.forEachIndexed { index, q ->
            val ans = selectedAnswers.getOrElse(index) { -1 }
            if (ans == q.correctOptionIndex && q.correctOptionIndex >= 0) {
                correctCount++
            }
        }
        val calculatedScore = ((correctCount.toDouble() / totalQ.toDouble()) * quiz.totalScore.toDouble()).toInt()
        val submission = QuizSubmission(
            studentId = student.id,
            studentName = student.name,
            selectedAnswers = selectedAnswers,
            score = calculatedScore,
            totalScore = quiz.totalScore,
            submittedAt = getTodayDateKey()
        )
        val updatedSubmissions = quiz.submissions.toMutableMap().apply {
            put(student.id, submission)
        }
        val updatedQuiz = quiz.copy(submissions = updatedSubmissions)
        val idx = quizzes.indexOfFirst { it.id == quiz.id }
        if (idx != -1) quizzes[idx] = updatedQuiz
        quizzesRef?.child(quiz.id)?.setValue(updatedQuiz)
        return submission
    }
}
