@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import com.example.mrprivate.data.repository.LoginResult
import com.example.mrprivate.data.repository.RoleDataRepository
import com.example.mrprivate.data.repository.parseAdminSnapshot
import com.example.mrprivate.data.repository.parseStudentSnapshot
import com.example.mrprivate.data.repository.parseTeacherSnapshot
import com.example.mrprivate.services.uriToResizedBase64
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.firebase.database.*
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

val LocalIsArabic = compositionLocalOf { true }
val LocalToggleLanguage = compositionLocalOf { {} }

@Composable
fun tr(ar: String, en: String): String = if (LocalIsArabic.current) ar else en

// ==================== 2. نماذج البيانات (Data Models) ====================
@IgnoreExtraProperties
data class SubjectPlanData(
    var subjectName: String = "",
    var bookPages: Int = 0,
    var lessonsCount: Int = 0,
    var totalUnits: Int = 10,
    var completedUnits: Int = 0,
    var studyDaysPerWeek: Int = 2,
    var academicYearWeeks: Int = 36,
    var completedPages: Int = 0,
    var completedLessons: Int = 0,
    var term: Int = 0,
    @get:PropertyName("isFinished") @set:PropertyName("isFinished") var isFinished: Boolean = false
) {
    @get:Exclude
    val totalYearlySessions: Int
        get() = (studyDaysPerWeek * academicYearWeeks).coerceAtLeast(1)

    @get:Exclude
    val pagesPerSessionTarget: Int
        get() = if (bookPages <= 0) 0 else ceil(bookPages.toDouble() / totalYearlySessions.toDouble()).toInt()

    @get:Exclude
    val completionPercentage: Int
        get() = if (isFinished) 100 else if (totalUnits <= 0) 0 else ((completedUnits * 100) / totalUnits).coerceIn(0, 100)
}

@IgnoreExtraProperties
data class DailyScheduleData(
    var dateKey: String = "",
    @get:PropertyName("isVacation") @set:PropertyName("isVacation") var isVacation: Boolean = false,
    var hourlySlots: Map<String, String> = emptyMap(),
    var completedHours: Map<String, Boolean> = emptyMap()
)

@IgnoreExtraProperties
data class TermSettings(
    var t1Start: String = "2026-09-12",
    var t1End: String = "2027-01-21",
    var t2Start: String = "2027-02-06",
    var t2End: String = "2027-06-24"
)

@IgnoreExtraProperties
data class AdminAccount(
    var id: String = UUID.randomUUID().toString(),
    var username: String = "",
    var canManageAdmins: Boolean = false,
    var canManageUsers: Boolean = true,
    @get:PropertyName("canDeleteAccounts") @set:PropertyName("canDeleteAccounts") var canDeleteAccounts: Boolean = false
)

@IgnoreExtraProperties
data class TeacherProfile(
    var id: String = UUID.randomUUID().toString(),
    var username: String = "",
    var email: String = "",
    var firstName: String = "",
    var secondName: String = "",
    var phone: String = "",
    var photoUriString: String = "",
    var governorate: String = "",
    var area: String = "",
    var extraAreas: List<String> = emptyList(),
    var educationTrack: String = "عربي",
    var stages: List<String> = emptyList(),
    var subject: String = "",
    @get:PropertyName("isApproved") @set:PropertyName("isApproved") var isApproved: Boolean = false,
    @get:PropertyName("isPremium") @set:PropertyName("isPremium") var isPremium: Boolean = false,
    @get:PropertyName("isUltimate") @set:PropertyName("isUltimate") var isUltimate: Boolean = false,
    var subscriptionActivatedAtMillis: Long = 0L,
    var subscriptionExpiresAtMillis: Long = 0L,
    var ratings: List<Int> = listOf(5)
) {
    @get:Exclude
    val fullName: String get() = "أ/ $firstName $secondName"

    @get:Exclude
    val averageRating: Float
        get() = if (ratings.isEmpty()) 0f else ratings.average().toFloat()

    @get:Exclude
    val isSubscriptionExpired: Boolean
        get() = subscriptionExpiresAtMillis > 0L && System.currentTimeMillis() > subscriptionExpiresAtMillis

    @get:Exclude
    val effectiveIsUltimate: Boolean
        get() = isUltimate && !isSubscriptionExpired

    @get:Exclude
    val effectiveIsPremium: Boolean
        get() = (isPremium || isUltimate) && !isSubscriptionExpired

    @get:Exclude
    val isInteractiveActive: Boolean
        get() = effectiveIsPremium || effectiveIsUltimate
}

@IgnoreExtraProperties
data class ScheduleSlot(
    var id: String = UUID.randomUUID().toString(),
    var teacherId: String = "",
    var stage: String = "",
    var grade: String = "",
    var day: String = "",
    var time: String = "",
    var maxStudents: Int = 5,
    var price: String = "",
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate") var isPrivate: Boolean = false,
    @get:PropertyName("isOnline") @set:PropertyName("isOnline") var isOnline: Boolean = false,
    @get:PropertyName("isRecurringYearly") @set:PropertyName("isRecurringYearly") var isRecurringYearly: Boolean = true,
    @get:PropertyName("isExtraSession") @set:PropertyName("isExtraSession") var isExtraSession: Boolean = false,
    var academicYearWeeks: Int = 14,
    var recurringDates: List<String> = emptyList(),
    var enrollments: List<StudentEnrollment> = emptyList()
)

@IgnoreExtraProperties
data class SessionNotificationItem(
    var id: String = UUID.randomUUID().toString(),
    var teacherId: String = "",
    var slotId: String = "",
    var title: String = "",
    var body: String = "",
    var targetStudentIds: List<String> = emptyList(),
    var createdAtMillis: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class StudentEnrollment(
    var id: String = UUID.randomUUID().toString(),
    var studentId: String = "",
    var studentName: String = "",
    var studentPhone: String = "",
    var studentStage: String = "",
    var studentGrade: String = "",
    var parentName: String = "",
    var parentPhone: String = "",
    @get:PropertyName("isConfirmed") @set:PropertyName("isConfirmed") var isConfirmed: Boolean = false,
    @get:PropertyName("hasRated") @set:PropertyName("hasRated") var hasRated: Boolean = false,
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted") var isCompleted: Boolean = false,
    @get:PropertyName("attended") @set:PropertyName("attended") var attended: Boolean = true,
    var parentExcuseNote: String = "",
    var teacherRatingForStudent: Int = 5,
    var parentOnlyNote: String = ""
)

@IgnoreExtraProperties
data class StudentProfile(
    var id: String = UUID.randomUUID().toString(),
    var username: String = "",
    var email: String = "",
    var name: String = "",
    var phone: String = "",
    var photoUriString: String = "",
    var governorate: String = "",
    var area: String = "",
    var educationTrack: String = "عربي",
    var stage: String = "",
    var grade: String = "",
    @get:PropertyName("isParent") @set:PropertyName("isParent") var isParent: Boolean = false,
    var linkedStudentUsername: String = "",
    var parentId: String = "",
    @get:PropertyName("canAddStudentApproved") @set:PropertyName("canAddStudentApproved") var canAddStudentApproved: Boolean = false,
    var registeredStudentIds: List<String> = emptyList(),
    var subjectPlans: Map<String, SubjectPlanData> = emptyMap(),
    var dailyTimetable: Map<String, DailyScheduleData> = emptyMap()
)

@IgnoreExtraProperties
data class QuizQuestion(
    var id: String = UUID.randomUUID().toString(),
    var questionText: String = "",
    var options: List<String> = listOf("", "", "", ""),
    var correctOptionIndex: Int = -1 // يحدده المعلم عند حل الأسئلة ليكون الـ Model Answer
)

@IgnoreExtraProperties
data class QuizSubmission(
    var studentId: String = "",
    var studentName: String = "",
    var selectedAnswers: List<Int> = emptyList(),
    var score: Int = 0,
    var totalScore: Int = 0,
    var submittedAt: String = ""
)

/** مدة بقاء صورة ورقة الأسئلة في Realtime Database قبل حذفها تلقائياً (ساعة واحدة) */
const val PAPER_RETENTION_MS: Long = 60L * 60L * 1000L
/** فترة فحص الصور المنتهية أثناء فتح التطبيق (5 دقائق) */
const val PAPER_PURGE_INTERVAL_MS: Long = 5L * 60L * 1000L

@IgnoreExtraProperties
data class QuizExam(
    var id: String = UUID.randomUUID().toString(),
    var teacherId: String = "",
    var teacherName: String = "",
    var subject: String = "",
    var slotId: String = "",
    var groupLabel: String = "",
    var title: String = "",
    var durationMinutes: Int = 15,
    var totalScore: Int = 20,
    var createdAt: String = "",
    var paperImageBase64: String = "", // مؤقت: يُنقل إلى quiz_papers/{id} عند الحفظ ولا يُخزَّن داخل الكويز
    var paperExpiresAt: Long = 0L, // وقت انتهاء صورة الورقة (millis)، 0 = لا توجد صورة
    var questions: List<QuizQuestion> = emptyList(),
    var targetStudentIds: List<String> = emptyList(),
    var submissions: Map<String, QuizSubmission> = emptyMap()
)

val stageGradesMap: Map<String, List<String>> = mapOf(
    "ابتدائي" to listOf("الصف الأول الابتدائي", "الصف الثاني الابتدائي", "الصف الثالث الابتدائي", "الصف الرابع الابتدائي", "الصف الخامس الابتدائي", "الصف السادس الابتدائي"),
    "إعدادي" to listOf("الصف الأول الإعدادي", "الصف الثاني الإعدادي", "الصف الثالث الإعدادي"),
    "ثانوي" to listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي"),
    "بكالوريا" to listOf("الصف الأول بكالوريا", "الصف الثاني بكالوريا", "الصف الثالث بكالوريا")
)

// ==================== 3. القوائم الثابتة ودوال المساعدة ====================
val governoratesWithAreas = mapOf(
    "القاهرة" to listOf("مدينة نصر", "المعادي", "مصر الجديدة", "شبرا", "التجمع الخامس"),
    "الجيزة" to listOf("6 أكتوبر", "حدائق أكتوبر", "حدائق الأهرام", "الشيخ زايد", "الدقي", "المهندسين", "الهرم", "فيصل"),
    "الإسكندرية" to listOf("سموحة", "سيدي جابر", "ميامي", "المنتزه", "لوران"),
    "الدقهلية" to listOf("المنصورة", "طلخا", "ميت غمر", "السنبلاوين")
)

val educationTracks = listOf("عربي", "لغات")
val educationalStages = listOf("ابتدائي", "إعدادي", "ثانوي", "بكالوريا")
val arabicSubjects = listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "فيزياء", "كيمياء", "حاسب آلي", "برمجة", "تاريخ", "جغرافيا", "دراسات اجتماعية", "أحياء")
val languageSubjects = listOf("Arabic", "English", "Math", "Science", "Physics", "Chemistry", "Computer Science", "Programming", "History", "Geography", "Social Studies", "Biology")
val daysOfWeek = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

val translationMap = mapOf(
    "القاهرة" to "Cairo", "الجيزة" to "Giza", "الإسكندرية" to "Alexandria", "الدقهلية" to "Dakahlia",
    "مدينة نصر" to "Nasr City", "المعادي" to "Maadi", "مصر الجديدة" to "Heliopolis", "شبرا" to "Shubra", "التجمع الخامس" to "5th Settlement",
    "6 أكتوبر" to "6th of October", "حدائق أكتوبر" to "Hadayek October", "حدائق الأهرام" to "Hadayek Al-Ahram", "الشيخ زايد" to "Sheikh Zayed",
    "الدقي" to "Dokki", "المهندسين" to "Mohandessin", "الهرم" to "Haram", "فيصل" to "Faisal", "سموحة" to "Smouha",
    "سيدي جابر" to "Sidi Gaber", "ميامي" to "Miami", "المنتزه" to "Montazah", "لوران" to "Loran", "المنصورة" to "Mansoura",
    "طلخا" to "Talkha", "ميت غمر" to "Mit Ghamr", "السنبلاوين" to "Sinbillawain", "ابتدائي" to "Primary", "إعدادي" to "Preparatory",
    "ثانوي" to "Secondary", "بكالوريا" to "Baccalaureate", "عربي" to "Arabic", "لغات" to "Languages",
    "السبت" to "Saturday", "الأحد" to "Sunday", "الاثنين" to "Monday", "الثلاثاء" to "Tuesday", "الأربعاء" to "Wednesday",
    "الخميس" to "Thursday", "الجمعة" to "Friday", "لغة عربية" to "Arabic Language", "لغة إنجليزية" to "English Language",
    "رياضيات" to "Math", "علوم" to "Science", "فيزياء" to "Physics", "كيمياء" to "Chemistry",
    "حاسب آلي" to "Computer Science", "برمجة" to "Programming", "تاريخ" to "History", "جغرافيا" to "Geography",
    "دراسات اجتماعية" to "Social Studies", "أحياء" to "Biology"
)

fun translateValue(value: String, isArabic: Boolean): String {
    if (isArabic || value.isBlank()) return value
    return translationMap[value] ?: value
}

fun trValue(value: String, isArabic: Boolean): String = translateValue(value, isArabic)

@Composable
fun trValue(value: String): String = translateValue(value, LocalIsArabic.current)

fun getSubjectsByTrack(track: String): List<String> = if (track == "لغات" || track == "Languages") languageSubjects else arabicSubjects

/** مواد كل مرحلة دراسية (بأسماء المسار العربي، ويُحوَّل للمسار الإنجليزي بنفس الترتيب) */
val stageSubjectsAr: Map<String, List<String>> = mapOf(
    "ابتدائي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "دراسات اجتماعية", "حاسب آلي"),
    "إعدادي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "دراسات اجتماعية", "حاسب آلي"),
    "ثانوي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "فيزياء", "كيمياء", "أحياء", "تاريخ", "جغرافيا", "حاسب آلي", "برمجة"),
    "بكالوريا" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "فيزياء", "كيمياء", "أحياء", "تاريخ", "جغرافيا", "حاسب آلي", "برمجة")
)

/** مواد المسار بعد تصفيتها حسب المرحلة؛ لو المرحلة غير معروفة تُرجع كل مواد المسار */
fun getSubjectsFor(track: String, stage: String): List<String> {
    val all = getSubjectsByTrack(track)
    val allowedAr = stageSubjectsAr[stage] ?: return all
    return allowedAr.mapNotNull { ar ->
        val i = arabicSubjects.indexOf(ar)
        if (i >= 0) all.getOrNull(i) else null
    }
}

// ==================== الترم الدراسي والتوزيع التلقائي ====================
private fun dateKeyFormat() = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun shiftDateKey(key: String, days: Int): String {
    val fmt = dateKeyFormat()
    val d = try { fmt.parse(key) } catch (e: Exception) { null } ?: return key
    val c = java.util.Calendar.getInstance()
    c.time = d
    c.add(java.util.Calendar.DAY_OF_YEAR, days)
    return fmt.format(c.time)
}

/** الترم المطلوب التخطيط له الآن: (رقم الترم، بداية، نهاية) أو null لو انتهت السنة الدراسية */
fun currentTermWindow(ts: TermSettings, todayKey: String = getTodayDateKey()): Triple<Int, String, String>? = when {
    todayKey <= ts.t1End -> Triple(1, maxOf(todayKey, ts.t1Start), ts.t1End)
    todayKey <= ts.t2End -> Triple(2, maxOf(todayKey, ts.t2Start), ts.t2End)
    else -> null
}

private fun dayNameToCalendar(dayName: String): Int = when (dayName) {
    "السبت", "Saturday" -> java.util.Calendar.SATURDAY
    "الأحد", "Sunday" -> java.util.Calendar.SUNDAY
    "الاثنين", "Monday" -> java.util.Calendar.MONDAY
    "الثلاثاء", "Tuesday" -> java.util.Calendar.TUESDAY
    "الأربعاء", "Wednesday" -> java.util.Calendar.WEDNESDAY
    "الخميس", "Thursday" -> java.util.Calendar.THURSDAY
    "الجمعة", "Friday" -> java.util.Calendar.FRIDAY
    else -> java.util.Calendar.SATURDAY
}

/** كل تواريخ يوم معين في الأسبوع داخل [startKey, endKey] */
fun generateTermDates(dayName: String, startKey: String, endKey: String): List<String> {
    val fmt = dateKeyFormat()
    val start = try { fmt.parse(startKey) } catch (e: Exception) { null } ?: return emptyList()
    val target = dayNameToCalendar(dayName)
    val cal = java.util.Calendar.getInstance()
    cal.time = start
    val out = mutableListOf<String>()
    var guard = 0
    while (guard++ < 400) {
        val key = fmt.format(cal.time)
        if (key > endKey) break
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) == target) out.add(key)
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return out
}

/** يحوّل وقت الميعاد مثل "05:00 مساءً" إلى مفتاح ساعة بنظام 24 مثل "17:00" */
fun slotHourKey(time: String): String {
    val m = Regex("(\\d{1,2})\\s*:\\s*(\\d{2})").find(time) ?: return "17:00"
    var h = m.groupValues[1].toIntOrNull() ?: return "17:00"
    val pm = time.contains("مساء") || time.contains("PM", ignoreCase = true)
    val am = time.contains("صباح") || time.contains("AM", ignoreCase = true)
    if (pm && h < 12) h += 12
    if (am && h == 12) h = 0
    return String.format(Locale.US, "%02d:00", h.coerceIn(0, 23))
}

fun getTodayDateKey(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(java.util.Date())
}

fun isDateInPast(dateKey: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val selectedDate = sdf.parse(dateKey) ?: return false
        val todayStr = sdf.format(java.util.Date())
        val todayDate = sdf.parse(todayStr) ?: return false
        selectedDate.before(todayDate)
    } catch (e: Exception) { false }
}

fun generateYearlySessionDates(dayName: String, weeksCount: Int = 36): List<String> {
    val targetDayOfWeek = when (dayName) {
        "السبت", "Saturday" -> java.util.Calendar.SATURDAY
        "الأحد", "Sunday" -> java.util.Calendar.SUNDAY
        "الاثنين", "Monday" -> java.util.Calendar.MONDAY
        "الثلاثاء", "Tuesday" -> java.util.Calendar.TUESDAY
        "الأربعاء", "Wednesday" -> java.util.Calendar.WEDNESDAY
        "الخميس", "Thursday" -> java.util.Calendar.THURSDAY
        "الجمعة", "Friday" -> java.util.Calendar.FRIDAY
        else -> java.util.Calendar.SATURDAY
    }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = java.util.Calendar.getInstance()
    while (cal.get(java.util.Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    val result = mutableListOf<String>()
    for (w in 0 until weeksCount) {
        result.add(sdf.format(cal.time))
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
    }
    return result
}

fun autoDistributeSubjectPlanOnTimetable(
    student: StudentProfile,
    subjectName: String,
    studyDaysPerWeek: Int,
    termSettings: TermSettings = TermSettings(),
    finished: Boolean = false
): Map<String, DailyScheduleData> {
    val result = student.dailyTimetable.toMutableMap()
    val todayKey = getTodayDateKey()
    val window = currentTermWindow(termSettings, todayKey) ?: return result
    val startKey = window.second
    val endKey = window.third
    // التوزيع ينتهي قبل امتحانات الترم بأسبوعين (للمراجعة)
    val planEnd = shiftDateKey(endKey, -14).let { if (it > startKey) it else endKey }

    // 1) مسح حصص المذاكرة التلقائية السابقة لنفس المادة من اليوم فصاعداً
    for ((key, day) in result.toMap()) {
        if (key >= todayKey && day.hourlySlots.values.any { it == subjectName }) {
            result[key] = day.copy(hourlySlots = day.hourlySlots.filterValues { it != subjectName })
        }
    }
    if (finished) return result

    // 2) كل أيام الفترة مع رقم يوم الأسبوع
    val fmt = dateKeyFormat()
    val allDates = mutableListOf<Pair<String, Int>>()
    val cal = java.util.Calendar.getInstance()
    cal.time = try { fmt.parse(startKey) } catch (e: Exception) { null } ?: return result
    var guard = 0
    while (guard++ < 400) {
        val key = fmt.format(cal.time)
        if (key > planEnd) break
        allDates.add(key to cal.get(java.util.Calendar.DAY_OF_WEEK))
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }

    // 3) الأيام التي فيها حصص معلم محجوز لنفس المادة تُحسب من أيام المذاكرة الأسبوعية
    val teacherDays = mutableSetOf<Int>()
    allDates.forEach { (key, dow) ->
        if (result[key]?.hourlySlots?.values?.any { it.startsWith("$subjectName (") } == true) teacherDays.add(dow)
    }
    val schoolDays = listOf(
        java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY,
        java.util.Calendar.TUESDAY, java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY
    )
    val wanted = (studyDaysPerWeek.coerceIn(1, 6) - teacherDays.size).coerceAtLeast(0)
    if (wanted == 0) return result
    val pool = schoolDays.filter { it !in teacherDays }
    val picks = (0 until wanted).map { pool[(it * pool.size) / wanted] }.toSet()

    // 4) قالب أسبوعي ثابت: نفس الأيام ونفس الساعة طوال الترم، ولا نكتب فوق أي حصة موجودة
    val targets = allDates.filter { (key, dow) -> dow in picks && result[key]?.isVacation != true }
    val hours = listOf("16:00", "18:00", "20:00", "17:00", "19:00", "21:00", "15:00", "14:00")
    fun free(key: String, h: String) = result[key]?.hourlySlots?.containsKey(h) != true
    val sameHour = hours.firstOrNull { h -> targets.all { (key, _) -> free(key, h) } }
    targets.forEach { (key, _) ->
        val h = sameHour ?: hours.firstOrNull { free(key, it) } ?: return@forEach
        val existing = result[key] ?: DailyScheduleData(dateKey = key)
        result[key] = existing.copy(hourlySlots = existing.hourlySlots + (h to subjectName))
    }
    return result
}

/** الصور الشخصية: أطول ضلع 320px وجودة 70 بصيغة WebP (بدل تخزين بايتات الصورة الأصلية كاملة في قاعدة البيانات). */
const val PROFILE_PHOTO_MAX_SIDE_PX = 320
const val PROFILE_PHOTO_QUALITY = 70

fun uriToBase64(context: Context, uri: Uri): String =
    uriToResizedBase64(context, uri, maxSide = PROFILE_PHOTO_MAX_SIDE_PX, quality = PROFILE_PHOTO_QUALITY, webp = true)

fun openPlatformSupportWhatsApp(context: Context) {
    val supportNumber = "+201118721114"
    val message = "مرحباً دعم منصة MR Private، أحتاج للمساعدة."
    val url = "https://api.whatsapp.com/send?phone=$supportNumber&text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

fun openTeacherWhatsAppForAdmin(context: Context, teacherPhone: String, teacherName: String) {
    val cleanPhone = teacherPhone.trim().replace(" ", "")
    if (cleanPhone.isEmpty()) {
        Toast.makeText(context, "رقم هاتف المعلم غير مسجل", Toast.LENGTH_SHORT).show()
        return
    }
    val formattedPhone = when {
        cleanPhone.startsWith("+") -> cleanPhone
        cleanPhone.startsWith("20") -> "+$cleanPhone"
        cleanPhone.startsWith("0") -> "+2$cleanPhone"
        else -> "+20$cleanPhone"
    }
    val message = "مرحباً $teacherName، معك إدارة منصة MR Private بخصوص مراجعة وتفعيل حسابك."
    val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

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
        // بيانات تجريبية للمعاينة (Preview) فقط؛ الحسابات الحقيقية تُنشأ عبر Firebase Auth
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

            // بيانات الديمو المحلية لا تختلط ببيانات الإنتاج: القوائم تُملأ حسب دور المستخدم فقط
            teachers.clear(); students.clear(); schedules.clear(); quizzes.clear()

            val repository = RoleDataRepository(this, db)
            repo = repository
            repository.startGlobalWatchers()
            pendingSession?.let { (kind, id) ->
                pendingSession = null
                repository.startSession(kind, id)
            }

            // تنظيف دوري لصور الأوراق المنتهية (كل 5 دقائق أثناء فتح التطبيق)
            purgeHandler.removeCallbacks(purgeTask)
            purgeHandler.postDelayed(purgeTask, PAPER_PURGE_INTERVAL_MS)
        } catch (e: Exception) {}
    }

    fun applyTermsSnapshot(snapshot: DataSnapshot) {
        snapshot.getValue(TermSettings::class.java)?.let { termSettings = it }
    }

    /** الصفحة التالية من دليل المعلمين (تُستدعى من الواجهة عند نهاية القائمة) */
    fun loadMoreTeachers() { repo?.loadMoreDirectory() }

    /** كتابة موعد + تحديث فهرس student_slots (يسمح للطالب/ولي الأمر بجلب مواعيدهم فقط) */
    private fun writeSlot(slot: ScheduleSlot) {
        schedulesRef?.child(slot.id)?.setValue(slot)
        repo?.indexSlot(slot)
    }

    fun saveTermSettings(ts: TermSettings) {
        termSettings = ts
        settingsRef?.child("terms")?.setValue(ts)
    }


    /** اليوزرات اللي بتبدأ بـ * محجوزة لدخول الأدمن */
    fun isUsernameReserved(username: String): Boolean = username.trim().startsWith("*")

    /** التحقق من تكرار اسم المستخدم يتم الآن عند إنشاء الحساب نفسه (Firebase Auth يرفض التكرار) */
    fun isUsernameTaken(username: String): Boolean = false

    /** فهرس الهاتف → اسم المستخدم (للدخول برقم الهاتف دون قراءة ملفات المستخدمين) */
    private fun writePhoneIndex(phone: String, username: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 6 || username.isBlank()) return
        FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("phone_index/$digits").setValue(username.trim())
    }

    /**
     * تسجيل الدخول عبر Firebase Auth (إيميل مشتق من اسم المستخدم + كلمة المرور) ثم قراءة ملف المستخدم بالـ uid.
     * الأدمن يدخل بـ "*" قبل اسم المستخدم (أو بإيميله الحقيقي)، ويُتحقق من وجود admins/{uid}.
     */
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
        // الدخول برقم الهاتف: الرقم → اسم المستخدم عبر phone_index (قراءة عامة لمفتاح واحد فقط)
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

    /** تسجيل معلم جديد: حساب Firebase Auth أولاً، ثم ملفه تحت teachers/{uid} (بانتظار موافقة الأدمن) */
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

    /** تسجيل ولي أمر جديد (نفس الآلية: uid = معرّف الملف) */
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

    /** إضافة حساب ابن من ولي الأمر دون إخراج ولي الأمر من جلسته (نسخة Firebase ثانوية) */
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

    /** إنشاء أدمن جديد (حساب Auth + admins/{uid}) من أدمن لديه صلاحية إدارة الأدمن */
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
        // تحديث محلي فوري (Optimistic UI)
        val localUpdated = slot.copy(enrollments = slot.enrollments + newEnrollment)
        val idx = schedules.indexOfFirst { it.id == slot.id }
        if (idx != -1) schedules[idx] = localUpdated

        // كتابة ذرّية (Transaction): لا تُضيّع حجز طالب آخر عند التزامن، وتعمل بأمان مع الكاش أوفلاين
        val ref = schedulesRef ?: return
        ref.child(slot.id).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(ScheduleSlot::class.java)
                    ?: return Transaction.success(currentData) // لا نسخة محلية بعد: يعاد التنفيذ ببيانات الخادم
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

    /**
     * يُرفض الموعد الجديد في حالة واحدة فقط:
     * وجود طالب مؤكد في المجموعة لديه حجز مؤكد في نفس اليوم ضمن نطاق 3 ساعات فقط.
     */
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

        // إضافة الحصة تلقائياً لجدول الطالب طوال الترم الدراسي
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
        // كتابة على مسارات محددة فقط (لا نستبدل ملف الطالب كله): آمن عند العمل أوفلاين وعند تعدد الأجهزة
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

    /**
     * حفظ الكويز. صورة الورقة لا تُحفظ داخل الكويز (حتى لا يحمّلها كل الطلاب)،
     * بل في quiz_papers/{id} مع وقت انتهاء بعد ساعة، وتُحذف تلقائياً.
     */
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
    }

    fun deleteQuizExam(quizId: String) {
        quizzes.removeAll { it.id == quizId }
        quizzesRef?.child(quizId)?.removeValue()
        papersRef?.child(quizId)?.removeValue()
    }

    /** حذف صور الأوراق التي انتهت مدتها (ساعة)، وتنظيف الصور القديمة المخزنة داخل الكويزات سابقاً. */
    fun purgeExpiredPapers() {
        val now = System.currentTimeMillis()
        val today = getTodayDateKey()
        for (q in quizzes.toList()) {
            if (q.paperExpiresAt in 1..now) {
                papersRef?.child(q.id)?.removeValue()?.addOnSuccessListener {
                    quizzesRef?.child(q.id)?.child("paperExpiresAt")?.setValue(0L)
                }
            }
            // نسخ قديمة كانت تحفظ الصورة داخل الكويز نفسه: نحذفها إن كان الكويز من يوم سابق
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

// ==================== 6. نقطة تشغيل التطبيق والخلفيات ====================
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun saveThemePref(context: Context, themeIndex: Int) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit { putInt("theme_index", themeIndex) }
}

fun saveLoginPrefs(context: Context, role: String, userId: String) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putBoolean("is_logged_in", true)
        putString("role", role)
        putString("active_user_id", userId)
    }
}

fun clearLoginPrefs(context: Context) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit { clear() }
}

fun restoreLoginState(context: Context, state: MrPrivateState) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    val savedThemeIdx = prefs.getInt("theme_index", 0)
    state.currentThemeIndex = savedThemeIdx.coerceIn(0, appGradientThemes.lastIndex)

    if (prefs.getBoolean("is_logged_in", false)) {
        val role = prefs.getString("role", "") ?: ""
        val userId = prefs.getString("active_user_id", "") ?: ""
        if (role.isNotBlank() && userId.isNotBlank()) {
            // الجلسة صالحة فقط إن كان مستخدم Firebase الحالي هو نفسه صاحب الحساب المحفوظ
            if (AccountAuth.currentUid != userId) {
                clearLoginPrefs(context)
                return
            }
            when (role) {
                "admin" -> { state.activeAdminId = userId; state.currentScreen = "admin_dashboard" }
                "teacher" -> { state.activeTeacherId = userId; state.currentScreen = "teacher_dashboard" }
                "student", "parent" -> { state.activeStudentId = userId; state.currentScreen = "student_dashboard" }
            }
        }
    }
}

@Composable
fun rememberIsNetworkConnected(): Boolean {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(isNetworkAvailable(context)) }
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
        }
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        try { connectivityManager?.registerNetworkCallback(request, callback) } catch (e: Exception) {}
        onDispose { try { connectivityManager?.unregisterNetworkCallback(callback) } catch (e: Exception) {} }
    }
    return isConnected
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val appState = remember { MrPrivateState() }
            LaunchedEffect(Unit) { restoreLoginState(context, appState) }
            MrPrivateTheme(appState = appState) { AppNavigation(appState) }
        }
    }
}

@Composable
fun AppNavigation(state: MrPrivateState) {
    val context = LocalContext.current

    when (state.currentScreen) {
        "login" -> LoginScreen(
            isLoading = state.isLoading,
            appState = state,
            onLogin = { role, user, pass, done -> state.loginAsync(context, role, user, pass, done) },
            onFirstTimeRegisterClick = { state.currentScreen = "role_selection" }
        )
        "role_selection" -> {
            BackHandler { state.currentScreen = "login" }
            RoleSelectionScreen(
                appState = state,
                onSelectRole = { role ->
                    state.currentScreen = if (role == "teacher") "teacher_reg" else "parent_reg"
                },
                onBackToLogin = { state.currentScreen = "login" }
            )
        }
        "admin_dashboard" -> state.activeAdmin?.let { admin ->
            BackHandler { clearLoginPrefs(context); state.activeAdminId = null; state.currentScreen = "login" }
            AdminDashboardScreen(
                currentAdmin = admin, allAdmins = state.admins, allTeachers = state.teachers,
                allStudents = state.students, allSchedules = state.schedules, appState = state,
                checkUsernameTaken = { state.isUsernameTaken(it) }, onApproveTeacher = { state.approveTeacher(it) },
                onUpdateTeacher = { state.updateTeacherProfile(it) }, onDeleteTeacher = { state.deleteTeacher(it) },
                onUpdateStudent = { state.updateStudentProfile(it) }, onDeleteStudent = { state.deleteStudent(it) },
                onUpdateSchedule = { state.updateSchedule(it) }, onDeleteSchedule = { state.deleteSchedule(it) },
                onConfirmStudentBooking = { slot, enrollId -> state.confirmStudentBooking(slot, enrollId) },
                onSaveAdmin = { state.addOrUpdateAdmin(it) }, onCreateAdmin = { admin, pass, done -> state.createAdminAsync(admin, pass, done) }, onDeleteAdmin = { state.deleteAdmin(it) },
                onLogout = { clearLoginPrefs(context); state.activeAdminId = null; state.currentScreen = "login" }
            )
        }
        "teacher_reg" -> {
            BackHandler { state.currentScreen = "role_selection" }
            TeacherRegistrationScreen(
                appState = state, checkUsernameTaken = { state.isUsernameTaken(it) },
                onRegistered = { teacher, pass, done -> state.registerTeacherAsync(teacher, pass, done) }, onBack = { state.currentScreen = "role_selection" }
            )
        }
        "teacher_dashboard" -> state.activeTeacher?.let { teacher ->
            BackHandler { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" }
            if (!teacher.isApproved) {
                TeacherPendingApprovalScreen(teacher = teacher, appState = state, onLogout = { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" })
            } else {
                TeacherDashboardScreen(
                    teacher = teacher, schedules = state.schedules.filter { it.teacherId == teacher.id }, appState = state,
                    onAddSchedule = { state.addSchedule(it) }, onConfirmStudent = { slot, enrollmentId -> state.confirmStudentBooking(slot, enrollmentId) },
                    onCompleteStudentSession = { slot, enrollmentId, stars, note -> state.completeStudentSession(slot, enrollmentId, stars, note) },
                    onUpdateAccountPhoto = { role, userId, photo -> state.updateAccountPhoto(role, userId, photo) },
                    onLogout = { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" }
                )
            }
        }
        "parent_reg" -> {
            BackHandler { state.currentScreen = "role_selection" }
            StudentRegistrationScreen(
                isParent = true, appState = state, checkUsernameTaken = { state.isUsernameTaken(it) },
                onRegistered = { parent, pass, done -> state.registerStudentAsync(parent, pass, done) }, onBack = { state.currentScreen = "role_selection" }
            )
        }
        "student_dashboard" -> state.activeStudent?.let { student ->
            BackHandler { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
            val approvedTeachers = state.teachers.filter { it.isApproved }
            if (student.isParent) {
                ParentDashboardScreen(
                    parent = student, allStudents = state.students, allTeachers = approvedTeachers, allSchedules = state.schedules, appState = state,
                    onSelectTeacherForChild = { teacher, child ->
                        state.targetStudentForBookingId = child.id
                        state.selectedTeacherIdForStudent = teacher.id
                        state.currentScreen = "teacher_details_for_student"
                    },
                    onUpdateStudent = { state.updateStudentProfile(it) },
                    onAddChild = { },
                    checkUsernameTaken = { state.isUsernameTaken(it) },
                    onUpdateAccountPhoto = { role, userId, photo -> state.updateAccountPhoto(role, userId, photo) },
                    onLogout = { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
                )
            } else {
                StudentHomeScreen(
                    student = student, allStudents = state.students, appState = state,
                    onLogout = { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
                )
            }
        }
        "teacher_details_for_student" -> {
            BackHandler { state.currentScreen = "student_dashboard" }
            val bookingStudent = state.targetStudentForBooking
            val teacher = state.selectedTeacherForStudent
            if (bookingStudent != null && teacher != null) {
                TeacherScheduleForStudentScreen(
                    student = bookingStudent, teacher = teacher, schedules = state.schedules.filter { it.teacherId == teacher.id },
                    onBookSlot = { slot -> state.bookSlot(slot, bookingStudent) },
                    onSubmitRating = { slot, enrollmentId, stars -> state.submitTeacherRating(teacher, slot, enrollmentId, stars) },
                    onBack = { state.currentScreen = "student_dashboard" }
                )
            }
        }
    }
}

// ==================== 7. شاشة الدخول الحديثة (مطابقة لتصميم الصور المرفقة) ====================
@Composable
fun LoginScreen(
    isLoading: Boolean,
    appState: MrPrivateState? = null,
    onLogin: (String, String, String, (LoginResult) -> Unit) -> Unit,
    onFirstTimeRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("student") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // الصف الأول: شعار المنصة (MR Private دايماً بنفس الترتيب) + زر اللغة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DarkPurple, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.5.dp, GoldStar)) {
                            Text("MR", color = PureWhite, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Private", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkPurple)
                    }
                }
                LanguageSwitcherButton(appState = null, isLightMode = true)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Elkheta3DButton(
                text = tr("كلّمنا على واتساب", "WhatsApp Support"),
                onClick = { openPlatformSupportWhatsApp(context) },
                modifier = Modifier.fillMaxWidth(),
                mainColor = WhatsAppGreen,
                shadowColor = ElkhetaGreenShadow,
                height = 40,
                cornerRadius = 20
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = tr("ذاكر بانبساط،\nوحسّن مستواك الدراسي", "Study Happily,\nImprove Your Level"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = ElkhetaTextDark,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // الكارت الأبيض العائم لتسجيل الدخول (مثل صورة "ابدأ بحسابك")
            FloatingCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔐", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr("ابدأ بحسابك", "Sign In to Your Account"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // كروت اختيار الدور (طالب / معلم / ولي أمر)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            Triple("student", tr("طالب", "Student"), "🎓"),
                            Triple("teacher", tr("معلم", "Teacher"), "👨‍🏫"),
                            Triple("parent", tr("ولي أمر", "Parent"), "👨‍👩‍👦")
                        )
                        roles.forEach { (roleKey, roleLabel, emoji) ->
                            val isSelected = selectedRole == roleKey
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedRole = roleKey
                                        errorMessage = ""
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ElkhetaCyan else ElkhetaInputBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE0F2FE).copy(alpha = 0.5f) else ElkhetaInputBg
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                    Text(
                                        text = roleLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF0369A1) else ElkhetaTextDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = tr("اسم المستخدم أو رقم الموبايل", "Username or Phone"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ElkhetaTextDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = "" },
                        placeholder = { Text("a1 / a2 / a3", color = Color.Gray) },
                        trailingIcon = { Text("📱", fontSize = 18.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColorsBordered()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = tr("الباسورد", "Password"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ElkhetaTextDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        placeholder = { Text(tr("اكتب الباسورد (للتجربة: 0)", "Password (demo: 0)"), color = Color.Gray) },
                        leadingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = ElkhetaTextGray
                                )
                            }
                        },
                        trailingIcon = { Text("🔑", fontSize = 18.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColorsBordered()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val emptyErrorMsg = tr("يرجى إدخال اسم المستخدم وكلمة المرور", "Please enter username and password")
                    val invalidErrorMsg = tr("اسم المستخدم أو كلمة المرور غير صحيحة!", "Invalid username or password!")
                    val networkErrorMsg = tr("تعذر الاتصال بالإنترنت، تأكد من الشبكة وحاول مرة أخرى", "No internet connection, please try again")

                    Elkheta3DButton(
                        text = if (busy) tr("جارٍ تسجيل الدخول...", "Signing in...") else tr("تسجيل الدخول ←", "Sign In ←"),
                        onClick = {
                            // دخول الأدمن: علامة * قبل اسم المستخدم فقط
                            val isAdminLogin = username.trim().startsWith("*")
                            val loginName = if (isAdminLogin) username.trim().removePrefix("*").trim() else username
                            if (loginName.isBlank() || password.isBlank()) {
                                errorMessage = emptyErrorMsg
                            } else {
                                val roleToLogin = if (isAdminLogin) "admin" else selectedRole
                                if (!busy) {
                                    busy = true
                                    errorMessage = ""
                                    onLogin(roleToLogin, loginName, password) { result ->
                                        busy = false
                                        when (result) {
                                            LoginResult.SUCCESS -> Unit
                                            LoginResult.NETWORK_ERROR -> errorMessage = networkErrorMsg
                                            LoginResult.WRONG_CREDENTIALS -> errorMessage = invalidErrorMsg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        mainColor = ElkhetaCyan,
                        shadowColor = ElkhetaCyanShadow
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Elkheta3DButton(
                text = tr("🚀 سجل مجاناً لأول مرة", "🚀 Register Free"),
                onClick = onFirstTimeRegisterClick,
                modifier = Modifier.fillMaxWidth(),
                mainColor = DarkPurple,
                shadowColor = ElkhetaPurpleShadow
            )
        }
    }
}

// ==================== 8. شاشة التسجيل لأول مرة (أهلاً بيك! معلم أو ولي أمر فقط بدون طالب) ====================
@Composable
fun RoleSelectionScreen(
    appState: MrPrivateState? = null,
    onSelectRole: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTopBar(appState = appState, backText = tr("→ عندي حساب", "→ I have an account"), onBack = { onBackToLogin() })

            Spacer(modifier = Modifier.height(20.dp))

            // الكارت الأبيض العائم مطابق لصورة "أهلاً بيك! قبل ما نبدأ قول لنا إنت مين"
            FloatingCard {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋", fontSize = 34.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tr("أهلاً بيك!", "Welcome!"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Text(
                        text = tr("قبل ما نبدأ – قول لنا إنت مين", "Before we start - tell us who you are"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElkhetaTextGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // الزر الأول: ولي أمر
                    RoleCard(
                        emoji = "👨‍👩‍👦",
                        title = tr("ولي أمر", "Parent"),
                        description = tr("هتابع تقدم ابني، أضيف حساب الطالب، وأضبط جدوله ومواده", "Track my child's progress, add student account & schedule"),
                        onClick = { onSelectRole("parent") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // الزر الثاني: معلم
                    RoleCard(
                        emoji = "👨‍🏫",
                        title = tr("معلم", "Teacher"),
                        description = tr("هسجل مواعيد حصصي السنوية وأتابع طلابي وتقييماتهم", "Manage yearly sessions, groups, and student evaluations"),
                        onClick = { onSelectRole("teacher") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = tr("عندك حساب بالفعل؟ .. سجل دخولك", "Already have an account? Sign In"),
                        color = ElkhetaCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
            }
        }
    }
}

// ==================== 9. شاشة تسجيل ولي الأمر المبسطة (صورة + اسم مستخدم + باسورد + رقم واتساب فقط) ====================
@Composable
fun StudentRegistrationScreen(
    isParent: Boolean,
    appState: MrPrivateState? = null,
    checkUsernameTaken: (String) -> Boolean,
    onRegistered: (StudentProfile, String, (AuthError) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val authText = rememberAuthErrorTexts()
    var phone by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("") }
    val availableAreas = governoratesWithAreas[governorate] ?: emptyList()
    var area by remember(governorate) { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTopBar(appState = appState, backText = tr("→ رجوع", "→ Back"), onBack = { onBack() })

            Spacer(modifier = Modifier.height(14.dp))

            FloatingCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr("ابدأ بحساب ولي الأمر", "Create Parent Account"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Text(
                        text = tr("اختر الصورة واكتب اسم المستخدم والباسورد ورقم الواتساب فقط", "Select photo, username, password & WhatsApp phone"),
                        fontSize = 12.sp,
                        color = ElkhetaTextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    // 1. الصورة الشخصية
                    CircularImagePicker(photoUri = photoUri, onPhotoSelected = { photoUri = it; errorMsg = "" })
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. اسم المستخدم
                    Text(tr("اسم المستخدم", "Username"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = "" },
                        placeholder = { Text(tr("اكتب اسم المستخدم", "Enter username")) },
                        trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. الباسورد
                    Text(tr("الباسورد", "Password"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        placeholder = { Text(tr("اكتب الباسورد", "Enter password")) },
                        leadingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        trailingIcon = { Text("🔑", fontSize = 18.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. رقم هاتف واتساب
                    Text(tr("رقم الموبايل (واتساب)", "WhatsApp Phone Number"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = "" },
                        placeholder = { Text("01xxxxxxxxx") },
                        trailingIcon = { Text("📱", fontSize = 18.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. المحافظة والمنطقة (قوائم منسدلة، مطلوبة)
                    CustomDropdown(tr("المحافظة", "Governorate"), governoratesWithAreas.keys.toList(), governorate) { governorate = it; errorMsg = "" }
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomDropdown(tr("المنطقة", "Area"), availableAreas, area) { area = it; errorMsg = "" }

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val photoErrorMsg = tr("يرجى اختيار الصورة الشخصية أولاً", "Please select a profile photo")
                    val fieldsErrorMsg = tr("يرجى ملء اسم المستخدم والباسورد ورقم الواتساب", "Please fill all fields")
                    val usernameTakenMsg = tr("اسم المستخدم مسجل بالفعل، اختر اسماً آخر", "Username already taken")
                    val locationErrorMsg = tr("يرجى اختيار المحافظة والمنطقة", "Please choose governorate and area")
                    val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")

                    Elkheta3DButton(
                        text = tr("تسجيل", "Register"),
                        onClick = {
                            when {
                                photoUri == null -> errorMsg = photoErrorMsg
                                username.isBlank() || password.isBlank() || phone.isBlank() -> errorMsg = fieldsErrorMsg
                                password.length < AccountAuth.MIN_PASSWORD_LENGTH -> errorMsg = authText(AuthError.WEAK_PASSWORD)
                                governorate.isBlank() || area.isBlank() -> errorMsg = locationErrorMsg
                                username.trim().startsWith("*") -> errorMsg = reservedMsg
                                checkUsernameTaken(username) -> errorMsg = usernameTakenMsg
                                else -> {
                                    if (!busy) {
                                    busy = true
                                    errorMsg = ""
                                    onRegistered(
                                        StudentProfile(
                                            username = username.trim(),
                                            name = username.trim(),
                                            phone = phone.trim(),
                                            photoUriString = uriToBase64(context, photoUri ?: Uri.EMPTY),
                                            governorate = governorate,
                                            area = area,
                                            educationTrack = "عربي",
                                            stage = "ولي أمر",
                                            isParent = true,
                                            canAddStudentApproved = false
                                        ),
                                        password
                                    ) { err ->
                                        busy = false
                                        if (err != AuthError.NONE) errorMsg = authText(err)
                                    }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        mainColor = ElkhetaCyan,
                        shadowColor = ElkhetaCyanShadow
                    )
                }
            }
        }
    }
}

// ==================== 10. شاشة تسجيل المعلم الكاملة ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRegistrationScreen(
    appState: MrPrivateState? = null,
    checkUsernameTaken: (String) -> Boolean,
    onRegistered: (TeacherProfile, String, (AuthError) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val authText = rememberAuthErrorTexts()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var secondName by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf(educationTracks[0]) }
    var selectedGov by remember { mutableStateOf(governoratesWithAreas.keys.first()) }
    val availableAreas = governoratesWithAreas[selectedGov] ?: emptyList()
    var selectedArea by remember(selectedGov) { mutableStateOf(availableAreas.first()) }
    var stage1 by remember { mutableStateOf(educationalStages[0]) }
    var stage2 by remember { mutableStateOf("بدون مرحلة إضافية") }
    val selectedStages = listOfNotNull(stage1, stage2.takeIf { it != "بدون مرحلة إضافية" && it != stage1 })
    val availableSubjects = getSubjectsByTrack(selectedTrack).filter { s -> selectedStages.any { st -> s in getSubjectsFor(selectedTrack, st) } }
    var subject by remember(selectedTrack, stage1, stage2) { mutableStateOf(availableSubjects.first()) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("تسجيل بروفايل معلم", "Teacher Registration"), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = PureWhite) } },
                actions = { LanguageSwitcherButton(appState = null) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularImagePicker(photoUri = photoUri, onPhotoSelected = { photoUri = it })
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = username, onValueChange = { username = it; errorMsg = "" }, label = { Text(tr("اسم المستخدم (User Name)", "User Name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = email, onValueChange = { email = it; errorMsg = "" }, label = { Text(tr("البريد الإلكتروني (Email)", "Email")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it; errorMsg = "" }, label = { Text(tr("رقم التليفون واتساب (للإدارة فقط)", "WhatsApp Phone (Admin Only)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; errorMsg = "" }, label = { Text(tr("كلمة المرور (Password)", "Password")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it; errorMsg = "" }, label = { Text(tr("الاسم الأول", "First Name")) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = secondName, onValueChange = { secondName = it; errorMsg = "" }, label = { Text(tr("الاسم الثاني", "Second Name")) }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("نوع الدراسة (عربي / لغات)", "Education Track"), educationTracks, selectedTrack) { newTrack ->
                selectedTrack = newTrack
                subject = getSubjectsByTrack(newTrack).first()
            }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المحافظة", "Governorate"), governoratesWithAreas.keys.toList(), selectedGov) { selectedGov = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المنطقة", "Area"), availableAreas, selectedArea) { selectedArea = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المرحلة التعليمية (1)", "Stage 1"), educationalStages, stage1) { stage1 = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المرحلة التعليمية (2 - اختياري)", "Stage 2"), listOf("بدون مرحلة إضافية") + educationalStages, stage2) { stage2 = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المادة التعليمية", "Subject"), availableSubjects, subject) { subject = it }

            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))

            val photoErrorMsg = tr("يرجى اختيار صورة شخصية", "Please select a photo")
            val fieldsErrorMsg = tr("يرجى ملء جميع الحقول المطلوبة", "Please fill all required fields")
            val usernameTakenMsg = tr("اسم المستخدم مسجل بالفعل", "Username already taken")
            val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")

            Elkheta3DButton(
                text = tr("تسجيل وإرسال للمراجعة", "Register & Submit"),
                onClick = {
                    when {
                        photoUri == null -> errorMsg = photoErrorMsg
                        username.isBlank() || phone.isBlank() || password.isBlank() || firstName.isBlank() || secondName.isBlank() -> errorMsg = fieldsErrorMsg
                        password.length < AccountAuth.MIN_PASSWORD_LENGTH -> errorMsg = authText(AuthError.WEAK_PASSWORD)
                        username.trim().startsWith("*") -> errorMsg = reservedMsg
                        checkUsernameTaken(username) -> errorMsg = usernameTakenMsg
                        else -> {
                            if (!busy) {
                            busy = true
                            errorMsg = ""
                            val stagesList = mutableListOf(stage1)
                            if (stage2 != "بدون مرحلة إضافية" && stage2 != stage1) stagesList.add(stage2)
                            onRegistered(
                                TeacherProfile(
                                    username = username.trim(), email = email.trim(), phone = phone.trim(),
                                    firstName = firstName.trim(), secondName = secondName.trim(),
                                    photoUriString = uriToBase64(context, photoUri ?: Uri.EMPTY),
                                    governorate = selectedGov, area = selectedArea, educationTrack = selectedTrack,
                                    stages = stagesList, subject = subject, isApproved = false
                                ),
                                password
                            ) { err ->
                                busy = false
                                if (err != AuthError.NONE) errorMsg = authText(err)
                            }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                mainColor = DarkPurple,
                shadowColor = ElkhetaPurpleShadow
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "1. شاشة تسجيل الدخول الحديثة")
@Composable
fun PreviewLoginScreen() {
    val state = remember { MrPrivateState(isPreviewMode = true) }
    MrPrivateTheme(appState = state) {
        LoginScreen(isLoading = false, appState = state, onLogin = { _, _, _, done -> done(LoginResult.SUCCESS) }, onFirstTimeRegisterClick = {})
    }
}
