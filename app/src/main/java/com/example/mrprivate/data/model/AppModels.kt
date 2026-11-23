@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.util.UUID
import kotlin.math.ceil

// ==================== نماذج البيانات المشتركة (Shared Data Models) ====================

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
    var correctOptionIndex: Int = -1
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
    var paperImageBase64: String = "",
    var paperExpiresAt: Long = 0L,
    var questions: List<QuizQuestion> = emptyList(),
    var targetStudentIds: List<String> = emptyList(),
    var submissions: Map<String, QuizSubmission> = emptyMap()
)

/** مدة بقاء صورة ورقة الأسئلة في Realtime Database قبل حذفها تلقائياً (ساعة واحدة) */
const val PAPER_RETENTION_MS: Long = 60L * 60L * 1000L
/** فترة فحص الصور المنتهية أثناء فتح التطبيق (5 دقائق) */
const val PAPER_PURGE_INTERVAL_MS: Long = 5L * 60L * 1000L
