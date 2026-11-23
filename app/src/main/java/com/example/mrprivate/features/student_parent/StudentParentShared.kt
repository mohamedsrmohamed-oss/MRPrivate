@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.student_parent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import com.example.mrprivate.features.auth.AccountAuth
import com.example.mrprivate.features.auth.AuthError
import com.example.mrprivate.features.auth.rememberAuthErrorTexts
import com.example.mrprivate.ui.components.*

// ==================== شاشات ولي الأمر (شاشة مستقلة عن شاشة الطالب) ====================
fun childrenOf(parent: StudentProfile, all: List<StudentProfile>): List<StudentProfile> =
    all.filter {
        !it.isParent && (
            it.parentId == parent.id ||
            (parent.linkedStudentUsername.isNotBlank() && it.username.equals(parent.linkedStudentUsername.trim(), true)) ||
            parent.registeredStudentIds.contains(it.id)
        )
    }

fun averageCompletion(student: StudentProfile): Int {
    val plans = getSubjectsFor(student.educationTrack, student.stage).mapNotNull { student.subjectPlans[it] }
    return if (plans.isEmpty()) 0 else plans.map { it.completionPercentage }.average().toInt()
}

fun nextSessionText(child: StudentProfile, isArabic: Boolean): String {
    val today = getTodayDateKey()
    for (key in child.dailyTimetable.keys.filter { it >= today }.sorted()) {
        val d = child.dailyTimetable[key] ?: continue
        if (d.isVacation) continue
        val e = d.hourlySlots.entries.filter { it.value.isNotBlank() && !isBreakValue(it.value) }.minByOrNull { it.key } ?: continue
        return "${dateKeyTitle(key, isArabic)} ${formatHour12(e.key, isArabic)} - ${trValue(e.value, isArabic)}"
    }
    return if (isArabic) "لا توجد حصص قادمة" else "No upcoming sessions"
}

fun copyToClipboard(context: Context, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("MR Private", text))
        Toast.makeText(context, "✓", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {}
}

/** اسم المادة بالعربي (لمقارنة مواد المعلمين بالمسارين) */
fun subjectArabicName(subject: String): String {
    val i = languageSubjects.indexOf(subject)
    return if (i >= 0) arabicSubjects[i] else subject
}

fun normalizeSubjectNameAr(subject: String): String {
    val idx = languageSubjects.indexOfFirst { it.equals(subject, ignoreCase = true) }
    return if (idx >= 0) arabicSubjects[idx] else subject
}

@Composable
fun ProgressBar(percent: Int, color: Color = ElkhetaCyan) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)).background(ElkhetaInputBorder)) {
        Box(modifier = Modifier.fillMaxWidth((percent.coerceIn(0, 100)) / 100f).fillMaxHeight().background(color))
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(10.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
    }
}

// ==================== نافذة تفاصيل الحجز عند النقر عليه ====================
@Composable
fun BookingDetailsDialog(
    slot: ScheduleSlot,
    teacher: TeacherProfile?,
    enroll: StudentEnrollment?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("📋 تفاصيل الحجز والمعلم", "📋 Booking & Teacher Details"),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = DarkPurple
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                // بطاقة بيانات المعلم
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ElkhetaInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            size = 54,
                            isDarkBg = false,
                            photoUriString = teacher?.photoUriString ?: "",
                            onPhotoChange = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teacher?.fullName ?: tr("معلم المنصة", "Platform Teacher"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = DarkPurple
                            )
                            Text(
                                text = "${trValue(teacher?.subject ?: "")} • ${trValue(teacher?.educationTrack ?: "")}",
                                fontSize = 12.sp,
                                color = ElkhetaTextGray,
                                fontWeight = FontWeight.Bold
                            )
                            if (teacher != null) {
                                Text(
                                    text = "${trValue(teacher.governorate)} - ${trValue(teacher.area)}",
                                    fontSize = 12.sp,
                                    color = ElkhetaTextGray
                                )
                                StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size, darkText = true)
                            }
                        }
                    }
                }

                // تفاصيل الموعد والاشتراك
                Surface(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val typeLabel = if (slot.isPrivate) tr("برايفت (فردي)", "Private") else tr("مجموعة (${slot.enrollments.size}/${slot.maxStudents})", "Group")
                        val locLabel = if (slot.isOnline) tr("أونلاين (Online)", "Online") else tr("حضوري (Offline)", "Offline")
                        Text(tr("📅 الموعد: ${trValue(slot.day)} - ${slot.time}", "📅 Schedule: ${trValue(slot.day)} - ${slot.time}"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                        Text(tr("🎓 الصف: ${trValue(slot.stage)} • ${slot.grade}", "🎓 Grade: ${trValue(slot.stage)} • ${slot.grade}"), fontSize = 13.sp, color = ElkhetaTextDark)
                        Text(tr("📍 النظام: $typeLabel • $locLabel", "📍 Mode: $typeLabel • $locLabel"), fontSize = 13.sp, color = ElkhetaTextDark)
                        Text(tr("💰 تكلفة الحصة: ${slot.price} ج.م", "💰 Price: ${slot.price} EGP"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                    }
                }

                // حالة الحضور والتقييم والملاحظات
                if (enroll != null) {
                    Surface(
                        color = LightPurple.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tr("📝 سجل المتابعة والملاحظات:", "📝 Attendance & Notes:"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DarkPurple)
                            Text(
                                text = if (enroll.isConfirmed) tr("حالة الاشتراك: مؤكد ومستمر في المجموعة ✓", "Subscription: Confirmed & Active in Group ✓")
                                else tr("حالة الاشتراك: في انتظار قبول المعلم ⏳", "Subscription: Pending Teacher Approval ⏳"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enroll.isConfirmed) Color(0xFF166534) else Color(0xFFB45309)
                            )
                            if (enroll.isCompleted) {
                                Text(
                                    text = if (enroll.attended) tr("سجل الحضور: حضر الحصة ✓ (تقييم المعلم: ${enroll.teacherRatingForStudent}/5 ★)", "Attendance: Present ✓ (${enroll.teacherRatingForStudent}/5 ★)")
                                    else tr("سجل الحضور: غائب عن الحصة ✗", "Attendance: Absent ✗"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (enroll.attended) Color(0xFF2E7D32) else Color(0xFFB91C1C)
                                )
                            }
                            if (enroll.parentExcuseNote.isNotBlank()) {
                                Text(
                                    text = tr("✋ اعتذارك المسجل: ${enroll.parentExcuseNote} (الاشتراك سارٍ في المجموعة)", "✋ Your Excuse: ${enroll.parentExcuseNote}"),
                                    fontSize = 12.sp,
                                    color = Color(0xFFC2410C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (enroll.parentOnlyNote.isNotBlank())
                                    tr("💬 ملاحظة المعلم السرية لك: ${enroll.parentOnlyNote}", "💬 Teacher Private Note: ${enroll.parentOnlyNote}")
                                else
                                    tr("💬 ملاحظات المعلم: لا توجد ملاحظات إضافية مسجلة بعد.", "💬 Teacher Notes: No additional notes yet."),
                                fontSize = 13.sp,
                                color = ElkhetaTextDark
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { openPlatformSupportWhatsApp(context) },
                        border = BorderStroke(1.5.dp, WhatsAppGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("استفسار للإدارة", "Ask Support"), color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("إغلاق", "Close"), color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== نافذة/صفحة تفاصيل المادة المحجوزة مع المعلم ====================
@Composable
fun BookedSubjectTeacherDetailsDialog(
    child: StudentProfile,
    subjectName: String,
    bookedSlots: List<ScheduleSlot>,
    allTeachers: List<TeacherProfile>,
    appState: MrPrivateState,
    onDismiss: () -> Unit
) {
    val subjectQuizzes = appState.quizzes.filter { q ->
        normalizeSubjectNameAr(q.subject) == normalizeSubjectNameAr(subjectName) ||
        q.subject.equals(subjectName, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("📚 تفاصيل مادة: ${trValue(subjectName)}", "📚 Subject Details: ${trValue(subjectName)}"),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = DarkPurple
                        )
                        Text(
                            text = tr("الطالب: ${child.name}", "Student: ${child.name}"),
                            fontSize = 12.sp,
                            color = ElkhetaTextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. حصص المعلم المحجوزة وسجل الحضور والغياب والتقييم وملاحظات المعلم
                    bookedSlots.forEach { slot ->
                        val teacher = allTeachers.find { it.id == slot.teacherId }
                        val enroll = slot.enrollments.find { it.studentId == child.id || it.studentName == child.name }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, ElkhetaCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProfileAvatar(size = 48, isDarkBg = false, photoUriString = teacher?.photoUriString ?: "")
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = teacher?.fullName ?: tr("معلم المادة", "Subject Teacher"),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = DarkPurple
                                        )
                                        Text(
                                            text = "${trValue(slot.day)} - ${slot.time} • ${if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF0369A1),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // مواعيد الحصص المجدولة للمعلم في الترم
                                if (slot.recurringDates.isNotEmpty()) {
                                    Text(
                                        text = tr("🗓️ حصص المعلم القادمة (${slot.recurringDates.size} حصة في الترم):", "🗓️ Scheduled Teacher Sessions (${slot.recurringDates.size}):"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ElkhetaTextDark
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        slot.recurringDates.take(8).forEach { d ->
                                            Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp)) {
                                                Text(d, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }

                                // سجل الحضور والغياب وتقييم الطالب
                                HorizontalDivider()
                                Text(tr("✅ سجل الحضور والغياب وتقييم الطالب:", "✅ Attendance & Student Evaluation:"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = DarkPurple)
                                if (enroll != null) {
                                    val attendanceText = when {
                                        !enroll.isCompleted -> tr("الحصة القادمة مجدولة (لم تُنهَ بعد)", "Upcoming session scheduled")
                                        enroll.attended -> tr("حضر الحصة ✓", "Attended ✓")
                                        else -> tr("غائب عن الحصة ✗", "Absent ✗")
                                    }
                                    val attendanceColor = when {
                                        !enroll.isCompleted -> Color(0xFF0369A1)
                                        enroll.attended -> Color(0xFF16A34A)
                                        else -> Color(0xFFB91C1C)
                                    }
                                    Text(attendanceText, color = attendanceColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)

                                    if (enroll.isCompleted && enroll.attended) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(tr("تقييم المعلم للطالب: ", "Teacher Rating: "), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            StarRatingBar(rating = enroll.teacherRatingForStudent.toFloat(), count = 1, darkText = true)
                                        }
                                    }

                                    if (enroll.parentExcuseNote.isNotBlank()) {
                                        Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFF97316))) {
                                            Text(
                                                text = tr("✋ اعتذار مسجل عن الحصة: ${enroll.parentExcuseNote} (الاشتراك مستمر في المجموعة)", "✋ Session Excuse: ${enroll.parentExcuseNote}"),
                                                color = Color(0xFFC2410C),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    // ملاحظات المعلم السرية لولي الأمر
                                    Surface(color = LightPurple, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(tr("📝 ملاحظات المعلم لولي الأمر:", "📝 Teacher Notes for Parent:"), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = DarkPurple)
                                            Text(
                                                text = enroll.parentOnlyNote.ifBlank { tr("لا توجد ملاحظات مسجلة من المعلم حتى الآن.", "No notes recorded by teacher yet.") },
                                                fontSize = 13.sp,
                                                color = ElkhetaTextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. الدرجات والكويزات الخاصة بهذه المادة
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr("🏆 درجات الاختبارات والكويزات في المادة:", "🏆 Quiz & Exam Scores:"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF166534))
                            val submissions = subjectQuizzes.mapNotNull { q -> q.submissions[child.id]?.let { q to it } }
                            if (submissions.isEmpty()) {
                                Text(tr("لم يتم تسليم اختبارات في هذه المادة بعد.", "No quiz scores recorded yet."), fontSize = 12.sp, color = ElkhetaTextGray)
                            } else {
                                submissions.forEach { (q, sub) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PureWhite)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                                            Text(sub.submittedAt, fontSize = 11.sp, color = ElkhetaTextGray)
                                        }
                                        Surface(color = DarkPurple, shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                text = "${sub.score} / ${sub.totalScore}",
                                                color = PureWhite,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Elkheta3DButton(
                    text = tr("إغلاق التفاصيل", "Close Details"),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = DarkPurple,
                    shadowColor = ElkhetaPurpleShadow,
                    height = 46
                )
            }
        }
    }
}

// ==================== 14. نوافذ الطالب وولي الأمر ====================
@Composable
fun EditSubjectPlanDialog(
    subjectName: String,
    existingPlan: SubjectPlanData,
    termSettings: TermSettings = TermSettings(),
    onDismiss: () -> Unit,
    onSavePlan: (SubjectPlanData) -> Unit
) {
    var bookPages by remember { mutableStateOf(existingPlan.bookPages.toString()) }
    var lessonsCount by remember { mutableStateOf(existingPlan.lessonsCount.toString()) }
    var totalUnits by remember { mutableStateOf(existingPlan.totalUnits.toString()) }
    var completedUnits by remember { mutableStateOf(existingPlan.completedUnits.toString()) }
    var studyDays by remember { mutableStateOf(existingPlan.studyDaysPerWeek.coerceIn(1, 6).toString()) }
    var isFinished by remember { mutableStateOf(existingPlan.isFinished) }
    val window = currentTermWindow(termSettings)
    val termLabel = if (window == null) tr("انتهت السنة الدراسية", "School year ended")
    else tr("الترم ${if (window.first == 1) "الأول" else "الثاني"}: ${window.second} ← ${window.third}", "Term ${window.first}: ${window.second} → ${window.third}")
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(tr("تقسيم المنهج: ${trValue(subjectName)}", "Divide Syllabus: ${trValue(subjectName)}"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(termLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = bookPages, onValueChange = { bookPages = it.filter { c -> c.isDigit() } }, label = { Text(tr("عدد صفحات الكتاب (الترم)", "Book pages (term)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = numberKeyboard)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = lessonsCount, onValueChange = { lessonsCount = it.filter { c -> c.isDigit() } }, label = { Text(tr("عدد الدروس", "Lessons")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = numberKeyboard)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = totalUnits, onValueChange = { totalUnits = it.filter { c -> c.isDigit() } }, label = { Text(tr("إجمالي وحدات الترم", "Total units (term)")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = numberKeyboard)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = completedUnits, onValueChange = { completedUnits = it.filter { c -> c.isDigit() } }, label = { Text(tr("الوحدات المنجزة", "Completed units")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = numberKeyboard)
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown(tr("أيام المذاكرة أسبوعياً", "Study days per week"), listOf("1", "2", "3", "4", "5", "6"), studyDays) { studyDays = it }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("الطالب خلّص المقرر", "Student finished the syllabus"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ElkhetaTextDark)
                    Switch(checked = isFinished, onCheckedChange = { isFinished = it })
                }
                Text(
                    tr("التوزيع تلقائي ومتكرر أسبوعياً على الترم الحالي، حول حصص المعلم المحجوزة.", "Automatic weekly distribution over the current term, around booked teacher sessions."),
                    fontSize = 11.sp, color = ElkhetaTextGray, modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Elkheta3DButton(
                    text = tr("حفظ تقسيم المنهج على الترم", "Save Syllabus Division"),
                    onClick = {
                        onSavePlan(
                            existingPlan.copy(
                                subjectName = subjectName,
                                bookPages = bookPages.toIntOrNull() ?: 0,
                                lessonsCount = lessonsCount.toIntOrNull() ?: 0,
                                totalUnits = (totalUnits.toIntOrNull() ?: 10).coerceAtLeast(1),
                                completedUnits = (completedUnits.toIntOrNull() ?: 0).coerceAtLeast(0),
                                studyDaysPerWeek = studyDays.toIntOrNull() ?: 2,
                                academicYearWeeks = if (window == null) existingPlan.academicYearWeeks else weeksBetween(window.second, window.third),
                                term = window?.first ?: existingPlan.term,
                                isFinished = isFinished
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AddNewStudentByParentDialog(
    parent: StudentProfile,
    checkUsernameTaken: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSaveStudent: (StudentProfile, String, (AuthError) -> Unit) -> Unit
) {
    var busy by remember { mutableStateOf(false) }
    val authText = rememberAuthErrorTexts()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("") }
    val gradesForStage = stageGradesMap[stage] ?: emptyList()
    var grade by remember(stage) { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val fieldsMsg = tr("يرجى ملء جميع الحقول واختيار المرحلة والمسار", "Please fill all fields and choose the stage and track")
    val takenMsg = tr("اسم المستخدم مسجل بالفعل", "Username already taken")
    val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(tr("إضافة حساب طالب جديد", "Add New Student"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(8.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("اسم الطالب الثلاثي", "Student full name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(tr("اسم المستخدم للطالب", "Student username")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(tr("كلمة المرور", "Password")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("رقم تليفون الطالب", "Student phone")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown(tr("المرحلة الدراسية", "Education stage"), educationalStages, stage) { stage = it; grade = "" }
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown(tr("الصف الدراسي", "Grade"), gradesForStage, grade) { grade = it }
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown(tr("مسار الدراسة", "Study track"), educationTracks, selectedTrack) { selectedTrack = it }
                Text(tr("المرحلة والمسار لا يتغيران بعد الحفظ", "Stage and track cannot be changed after saving"), fontSize = 11.sp, color = Color(0xFFB45309), modifier = Modifier.padding(top = 6.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Elkheta3DButton(
                    text = tr("حفظ الطالب", "Save Student"),
                    onClick = {
                        val cleanUser = username.trim()
                        if (name.isBlank() || cleanUser.isBlank() || password.isBlank() || phone.isBlank() || stage.isBlank() || selectedTrack.isBlank()) {
                            errorMessage = fieldsMsg
                        } else if (cleanUser.startsWith("*")) {
                            errorMessage = reservedMsg
                        } else if (checkUsernameTaken(cleanUser)) {
                            errorMessage = takenMsg
                        } else if (password.trim().length < AccountAuth.MIN_PASSWORD_LENGTH) {
                            errorMessage = authText(AuthError.WEAK_PASSWORD)
                        } else if (!busy) {
                            busy = true
                            errorMessage = ""
                            onSaveStudent(
                                StudentProfile(
                                    parentId = parent.id, name = name.trim(), username = cleanUser,
                                    phone = phone.trim(),
                                    governorate = parent.governorate, area = parent.area,
                                    educationTrack = selectedTrack, stage = stage, grade = grade.ifBlank { stage }, isParent = false
                                ),
                                password.trim()
                            ) { err ->
                                busy = false
                                if (err != AuthError.NONE) errorMessage = authText(err)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AddTeacherViaWhatsAppDialog(parent: StudentProfile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var teacherName by remember { mutableStateOf("") }
    var teacherPhone by remember { mutableStateOf("") }
    var teacherSubject by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(tr("ترشيح معلم جديد عبر واتساب المنصة", "Propose Teacher via WhatsApp"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = teacherName, onValueChange = { teacherName = it }, label = { Text("اسم المعلم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teacherPhone, onValueChange = { teacherPhone = it }, label = { Text("رقم تليفون المعلم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teacherSubject, onValueChange = { teacherSubject = it }, label = { Text("المادة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(14.dp))
                Elkheta3DButton(
                    text = tr("إرسال عبر واتساب", "Send via WhatsApp"),
                    onClick = {
                        if (teacherName.isNotBlank() && teacherPhone.isNotBlank()) {
                            val msg = "ترشيح معلم جديد:\nالاسم: $teacherName\nالتليفون: $teacherPhone\nالمادة: $teacherSubject"
                            val url = "https://api.whatsapp.com/send?phone=+201118721114&text=${Uri.encode(msg)}"
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } catch (e: Exception) {}
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = WhatsAppGreen,
                    shadowColor = ElkhetaGreenShadow
                )
            }
        }
    }
}
