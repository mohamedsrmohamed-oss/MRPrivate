@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import com.example.mrprivate.ui.components.*
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
        android.widget.Toast.makeText(context, "✓", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {}
}

/** اسم المادة بالعربي (لمقارنة مواد المعلمين بالمسارين) */
private fun subjectArabicName(subject: String): String {
    val i = languageSubjects.indexOf(subject)
    return if (i >= 0) arabicSubjects[i] else subject
}

@Composable
private fun ProgressBar(percent: Int, color: Color = ElkhetaCyan) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)).background(ElkhetaInputBorder)) {
        Box(modifier = Modifier.fillMaxWidth((percent.coerceIn(0, 100)) / 100f).fillMaxHeight().background(color))
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(10.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
    }
}

// ==================== نظام التنبيهات وتذكير بمواعيد الحصص القادمة ====================
data class UpcomingSessionReminder(
    val childName: String,
    val subjectAndTeacher: String,
    val dateTitle: String,
    val timeText: String,
    val isToday: Boolean
)

@Composable
fun UpcomingSessionsRemindersSection(
    children: List<StudentProfile>,
    allSchedules: List<ScheduleSlot>,
    allTeachers: List<TeacherProfile>
) {
    val isArabic = LocalIsArabic.current
    val context = LocalContext.current
    var remindersEnabled by remember { mutableStateOf(true) }
    var expandedList by remember { mutableStateOf(false) }

    val todayKey = getTodayDateKey()
    val tomorrowKey = shiftDateKey(todayKey, 1)

    val reminders = remember(children, allSchedules, allTeachers, isArabic) {
        val list = mutableListOf<UpcomingSessionReminder>()
        children.forEach { child ->
            // 1. من الجدول اليومي (اليوم وغداً)
            listOf(todayKey, tomorrowKey).forEach { dKey ->
                val dayData = child.dailyTimetable[dKey]
                if (dayData != null && !dayData.isVacation) {
                    dayData.hourlySlots.entries
                        .filter { it.value.isNotBlank() && !isBreakValue(it.value) }
                        .sortedBy { it.key }
                        .forEach { (hourKey, label) ->
                            list.add(
                                UpcomingSessionReminder(
                                    childName = child.name,
                                    subjectAndTeacher = trValue(label, isArabic),
                                    dateTitle = if (dKey == todayKey) trValue("اليوم", isArabic) else trValue("غداً", isArabic),
                                    timeText = formatHour12(hourKey, isArabic),
                                    isToday = dKey == todayKey
                                )
                            )
                        }
                }
            }
            // 2. من الحجوزات المؤكدة
            allSchedules.filter { slot ->
                slot.enrollments.any { (it.studentId == child.id || it.studentName == child.name) && it.isConfirmed }
            }.forEach { slot ->
                val t = allTeachers.find { it.id == slot.teacherId }
                val label = "${trValue(t?.subject ?: slot.stage, isArabic)} (${t?.fullName ?: ""})"
                if (list.none { it.childName == child.name && it.subjectAndTeacher == label }) {
                    list.add(
                        UpcomingSessionReminder(
                            childName = child.name,
                            subjectAndTeacher = label,
                            dateTitle = trValue(slot.day, isArabic),
                            timeText = slot.time,
                            isToday = false
                        )
                    )
                }
            }
        }
        list
    }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .border(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (remindersEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("تنبيهات الحصص القادمة (${reminders.size})", "Upcoming Session Alerts (${reminders.size})"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = DarkPurple
                        )
                        Text(
                            text = if (remindersEnabled)
                                tr("التذكير التلقائي قبل الحصة مفعّل ✓", "Auto pre-session reminder active ✓")
                            else
                                tr("التنبيهات متوقفة مؤقتاً", "Reminders paused"),
                            fontSize = 11.sp,
                            color = if (remindersEnabled) Color(0xFF16A34A) else ElkhetaTextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                val remindersEnabledMsg = tr("تم تفعيل تنبيهات تذكير الحصص القادمة", "Session reminders enabled")
                val remindersPausedMsg = tr("تم إيقاف التنبيهات مؤقتاً", "Session reminders paused")
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { enabled ->
                        remindersEnabled = enabled
                        Toast.makeText(
                            context,
                            if (enabled) remindersEnabledMsg else remindersPausedMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            if (remindersEnabled && reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val shown = if (expandedList) reminders else reminders.take(2)
                shown.forEach { rem ->
                    Surface(
                        color = if (rem.isToday) Color(0xFFFEF3C7) else Color(0xFFF0F9FF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⏰ ${rem.subjectAndTeacher}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = ElkhetaTextDark
                                )
                                Text(
                                    text = "${tr("الطالب:", "Student:")} ${rem.childName}",
                                    fontSize = 11.sp,
                                    color = ElkhetaTextGray
                                )
                            }
                            Surface(
                                color = if (rem.isToday) Color(0xFFD97706) else DarkPurple,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${rem.dateTitle} • ${rem.timeText}",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                if (reminders.size > 2) {
                    Text(
                        text = if (expandedList) tr("عرض أقل ▴", "Show less ▴") else tr("عرض كل التنبيهات (${reminders.size}) ▾", "View all alerts (${reminders.size}) ▾"),
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { expandedList = !expandedList }
                    )
                }
            }
        }
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

                Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                                Divider()
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

private fun normalizeSubjectNameAr(subject: String): String {
    val idx = languageSubjects.indexOfFirst { it.equals(subject, ignoreCase = true) }
    return if (idx >= 0) arabicSubjects[idx] else subject
}

@Composable
fun ChildBookingsList(
    child: StudentProfile,
    allTeachers: List<TeacherProfile>,
    allSchedules: List<ScheduleSlot>,
    showParentNote: Boolean,
    appState: MrPrivateState? = null
) {
    val context = LocalContext.current
    var slotToExcuse by remember { mutableStateOf<Pair<ScheduleSlot, StudentEnrollment>?>(null) }
    var bookingForDetails by remember { mutableStateOf<Triple<ScheduleSlot, TeacherProfile?, StudentEnrollment?>?>(null) }

    val mySlots = allSchedules.filter { slot -> slot.enrollments.any { it.studentId == child.id || it.studentName == child.name } }
    if (mySlots.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(tr("لا توجد حصص محجوزة حالياً.", "No bookings yet."), modifier = Modifier.padding(20.dp), color = Color.Gray)
        }
    } else {
        mySlots.forEach { slot ->
            val slotTeacher = allTeachers.find { it.id == slot.teacherId }
            val enroll = slot.enrollments.find { it.studentId == child.id || it.studentName == child.name }
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .clickable { bookingForDetails = Triple(slot, slotTeacher, enroll) },
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${slotTeacher?.fullName ?: ""} (${trValue(slotTeacher?.subject ?: "")})", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkPurple, modifier = Modifier.weight(1f))
                        Text("${trValue(slot.day)} - ${slot.time}", fontSize = 12.sp, color = ElkhetaTextGray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("🔄 موعد أسبوعي متكرر طوال الترم • السعر: ${slot.price} ج.م", "Weekly recurring all term • Price: ${slot.price} EGP"), fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        Text(tr("التفاصيل ›", "Details ›"), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = DarkPurple)
                    }
                    if (enroll != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (enroll.isConfirmed) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (enroll.isConfirmed) tr("الحالة: تم التأكيد ✓", "Status: Confirmed ✓") else tr("الحالة: انتظار قبول المعلم ⏳", "Status: Waiting Teacher Approval ⏳"),
                                color = if (enroll.isConfirmed) Color(0xFF166534) else Color(0xFFB45309),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (enroll.parentExcuseNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFF97316))) {
                                Text(
                                    text = tr("✋ اعتذار عن الحصة: ${enroll.parentExcuseNote} (اشتراكك في المجموعة مستمر ✓)", "✋ Excused from session: ${enroll.parentExcuseNote} (Group enrollment active ✓)"),
                                    color = Color(0xFFC2410C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        // أزرار ولي الأمر فقط: الاعتذار عن الحصة (بدون إلغاء الاشتراك) أو إلغاء الحجز بالكامل
                        if (showParentNote && appState != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { slotToExcuse = slot to enroll },
                                    border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tr("✋ اعتذار عن الحصة", "✋ Apologize / Excuse"),
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                                val bookingCancelledMsg = tr("تم إلغاء الحجز بنجاح", "Booking cancelled")
                                Button(
                                    onClick = {
                                        appState.removeStudentFromSlot(slot, enroll.id)
                                        Toast.makeText(context, bookingCancelledMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tr("✖ إلغاء الحجز", "✖ Cancel Booking"),
                                        color = PureWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    if (enroll != null && enroll.isCompleted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (enroll.attended) tr("حضور الحصة: حضر ✓", "Attendance: Present ✓") else tr("حضور الحصة: لم يحضر (غائب) ✗", "Attendance: Absent ✗"),
                            color = if (enroll.attended) Color(0xFF2E7D32) else Color(0xFFB91C1C),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        if (enroll.attended) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("تقييم المعلم للطالب: ", "Teacher Rating: "), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                StarRatingBar(rating = enroll.teacherRatingForStudent.toFloat(), count = 1, darkText = true)
                            }
                        }
                        if (showParentNote && enroll.parentOnlyNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = LightPurple, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(tr("📝 ملاحظة المعلم السرية لولي الأمر:", "Private Note to Parent:"), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = DarkPurple)
                                    Text(enroll.parentOnlyNote, fontSize = 13.sp, color = ElkhetaTextDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    bookingForDetails?.let { (slot, teacher, enroll) ->
        BookingDetailsDialog(
            slot = slot,
            teacher = teacher,
            enroll = enroll,
            onDismiss = { bookingForDetails = null }
        )
    }

    slotToExcuse?.let { (slot, enroll) ->
        var excuseReason by remember { mutableStateOf(enroll.parentExcuseNote) }
        Dialog(onDismissRequest = { slotToExcuse = null }) {
            AppCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(tr("الاعتذار عن الحصة للمعلم", "Apologize for Session"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = tr("ℹ️ تنويه: الاعتذار يُسجَّل للحصة القادمة فقط ولا يُلغي اشتراك الطالب في المجموعة.", "Note: Excusing applies to the upcoming session only and keeps your group subscription active."),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = excuseReason,
                        onValueChange = { excuseReason = it },
                        label = { Text(tr("سبب الاعتذار (يظهر للمعلم)", "Reason for excuse")) },
                        placeholder = { Text(tr("مثال: ظرف عائلي / مرض الطالب...", "e.g., Family reason / illness")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { slotToExcuse = null }) { Text(tr("إلغاء", "Cancel")) }
                        val excuseSentMsg = tr("تم إرسال الاعتذار للمعلم مع استمرار اشتراككم في المجموعة ✓", "Excuse sent; group subscription remains active ✓")
                        Button(
                            onClick = {
                                val finalNote = excuseReason.trim().ifBlank { "اعتذار من ولي الأمر عن حضور الحصة القادمة" }
                                // يسجل الاعتذار فقط دون حذف الطالب من المجموعة
                                appState?.submitParentExcuseForSession(slot, enroll.id, finalNote)
                                Toast.makeText(context, excuseSentMsg, Toast.LENGTH_LONG).show()
                                slotToExcuse = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                        ) {
                            Text(tr("إرسال الاعتذار", "Submit"), color = PureWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentDashboardScreen(
    parent: StudentProfile,
    allStudents: List<StudentProfile>,
    allTeachers: List<TeacherProfile>,
    allSchedules: List<ScheduleSlot>,
    appState: MrPrivateState,
    onSelectTeacherForChild: (TeacherProfile, StudentProfile) -> Unit,
    onUpdateStudent: (StudentProfile) -> Unit,
    onAddChild: (StudentProfile) -> Unit,
    checkUsernameTaken: (String) -> Boolean,
    onUpdateAccountPhoto: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = LocalIsArabic.current
    val children = childrenOf(parent, allStudents)
    val openChild = appState.openChildId?.let { id -> children.find { it.id == id } }

    var tab by remember { mutableIntStateOf(0) }
    var showAddChild by remember { mutableStateOf(false) }
    var showTeacherDialog by remember { mutableStateOf(false) }
    var showEditMyData by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (openChild != null) {
        ChildManagementScreen(
            child = openChild,
            allTeachers = allTeachers,
            allSchedules = allSchedules,
            appState = appState,
            onSelectTeacher = { teacher -> onSelectTeacherForChild(teacher, openChild) },
            onUpdateStudent = onUpdateStudent,
            onBack = { appState.openChildId = null }
        )
    } else {
        Scaffold(
            containerColor = ElkhetaSoftBgBottom,
            bottomBar = {
                ElkhetaBottomNavBar(
                    selectedTab = tab,
                    onSelectTab = { tab = it },
                    items = listOf(
                        Triple(0, tr("الأبناء", "Children"), "👨‍👧"),
                        Triple(1, tr("الحجوزات", "Bookings"), "📅"),
                        Triple(2, tr("حسابي", "Account"), "🪪")
                    ),
                    isScrolling = scrollState.isScrollInProgress
                )
            }
        ) { padding ->
            BeautifulAppBackground(isFullDark = false, appState = appState) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp)
                ) {
                    when (tab) {
                        0 -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProfileAvatar(
                                        size = 50,
                                        isDarkBg = false,
                                        photoUriString = parent.photoUriString,
                                        onPhotoChange = { uri ->
                                            val b64 = uriToBase64(context, uri)
                                            if (b64.isNotEmpty()) onUpdateAccountPhoto("parent", parent.id, b64)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tr("أهلاً، ${parent.name}", "Welcome, ${parent.name}"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
                                        Text(tr("إدارة الأبناء المضافين (${children.size})", "Managed children (${children.size})"), fontSize = 12.sp, color = ElkhetaTextGray)
                                    }
                                }
                                LanguageSwitcherButton(appState = null, isLightMode = true)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // كارت الحصة الجاية (الحجوزات المؤكدة فقط) مع زر الاعتذار عن الحصة
                            ParentHeroCard(
                                children = children,
                                allSchedules = allSchedules,
                                allTeachers = allTeachers,
                                appState = appState
                            )

                            // التمييز البصري الواضح بين الزر الرئيسي بتعبئة كاملة (إضافة طالب) والزر الثانوي بحدود خارجية فقط (ترشيح معلم)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                val addStudentAdminMsg = tr("لإضافة طالب، يلزم موافقة الإدارة أولاً. تواصل معنا على واتساب.", "Adding a student needs admin approval. Contact us on WhatsApp.")
                                Elkheta3DButton(
                                    text = tr("+ إضافة طالب", "+ Add Student"),
                                    onClick = {
                                        if (!parent.canAddStudentApproved) {
                                            openPlatformSupportWhatsApp(context)
                                            android.widget.Toast.makeText(context, addStudentAdminMsg, android.widget.Toast.LENGTH_LONG).show()
                                        } else showAddChild = true
                                    },
                                    modifier = Modifier.weight(1.15f),
                                    mainColor = DarkPurple,
                                    shadowColor = ElkhetaPurpleShadow,
                                    textColor = PureWhite,
                                    height = 48
                                )
                                OutlinedButton(
                                    onClick = { showTeacherDialog = true },
                                    border = BorderStroke(2.dp, DarkPurple),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = PureWhite,
                                        contentColor = DarkPurple
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.PersonAddAlt, contentDescription = null, tint = DarkPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr("ترشيح معلم", "Suggest Teacher"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = DarkPurple)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // نظام تنبيهات وتذكير بمواعيد الحصص القادمة
                            if (children.isNotEmpty()) {
                                UpcomingSessionsRemindersSection(
                                    children = children,
                                    allSchedules = allSchedules,
                                    allTeachers = allTeachers
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (children.isEmpty()) {
                                AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Text(tr("لا يوجد أبناء مضافون بعد. اضغط (+ إضافة طالب) لتبدأ.", "No children yet. Tap (+ Add Student) to start."), modifier = Modifier.padding(20.dp), color = Color.Gray)
                                }
                            }
                            children.forEach { child ->
                                val pct = averageCompletion(child)
                                AppCard(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).shadow(4.dp, RoundedCornerShape(18.dp)).clickable {
                                        appState.openChildTab = 0
                                        appState.openChildId = child.id
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ProfileAvatar(size = 42, isDarkBg = false, photoUriString = child.photoUriString, onPhotoChange = null)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(child.name, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = DarkPurple)
                                            }
                                            InfoChip("${trValue(child.stage)} · ${trValue(child.educationTrack)}")
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        ProgressBar(pct)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(tr("إنجاز المنهج $pct%", "Progress $pct%"), fontSize = 12.sp, color = ElkhetaTextGray)
                                            Text(nextSessionText(child, isArabic), fontSize = 12.sp, color = ElkhetaTextGray)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(size = 44, isDarkBg = false, photoUriString = parent.photoUriString, onPhotoChange = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(tr("حجوزات الأبناء وسجل الحصص", "Children bookings & sessions"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkPurple)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (children.isEmpty()) {
                                AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Text(tr("لا يوجد أبناء مضافون بعد.", "No children yet."), modifier = Modifier.padding(20.dp), color = Color.Gray)
                                }
                            }
                            children.forEach { child ->
                                Text(child.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ElkhetaTextDark, modifier = Modifier.padding(top = 8.dp))
                                ChildBookingsList(child, allTeachers, allSchedules, showParentNote = true, appState = appState)
                            }
                        }
                        else -> {
                            ParentAccountTab(
                                parent = parent,
                                appState = appState,
                                onEditData = { showEditMyData = true },
                                onUpdateAccountPhoto = onUpdateAccountPhoto,
                                onLogout = { appState.openChildId = null; onLogout() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddChild) {
        AddNewStudentByParentDialog(
            parent = parent,
            checkUsernameTaken = checkUsernameTaken,
            onDismiss = { showAddChild = false },
            onSaveStudent = { newStudent, childPassword, done ->
                appState.addChildStudentAsync(newStudent, childPassword) { created, error ->
                    if (created != null) {
                        onUpdateStudent(parent.copy(canAddStudentApproved = false, registeredStudentIds = parent.registeredStudentIds + created.id))
                        showAddChild = false
                    }
                    done(error)
                }
            }
        )
    }
    if (showTeacherDialog) {
        AddTeacherViaWhatsAppDialog(parent = parent, onDismiss = { showTeacherDialog = false })
    }
    if (showEditMyData) {
        EditStudentDialog(
            student = parent,
            onDismiss = { showEditMyData = false },
            onSave = { onUpdateStudent(it); showEditMyData = false }
        )
    }
}

@Composable
private fun ParentAccountTab(
    parent: StudentProfile,
    appState: MrPrivateState,
    onEditData: () -> Unit,
    onUpdateAccountPhoto: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("الملف الشخصي وحسابي", "My Profile & Account"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = DarkPurple)
            LanguageSwitcherButton(appState = appState, isLightMode = true)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // بطاقة الملف الشخصي الرئيسية
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(22.dp))
                .border(1.dp, DarkPurple.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DarkPurple.copy(alpha = 0.06f), ElkhetaCyan.copy(alpha = 0.08f))))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(88.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(PureWhite).border(3.dp, DarkPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        size = 84,
                        isDarkBg = false,
                        photoUriString = parent.photoUriString,
                        onPhotoChange = { uri ->
                            val b64 = uriToBase64(context, uri)
                            if (b64.isNotEmpty()) onUpdateAccountPhoto("parent", parent.id, b64)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(parent.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
                Text("${trValue(parent.governorate)} • ${trValue(parent.area)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextGray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // إبراز حقلي الاسم / اسم المستخدم ورقم الواتساب كحقول مخصصة واضحة
        AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("بيانات التواصل والحساب الأساسية", "Primary Account & Contact Fields"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DarkPurple)

                // حقل الاسم واسم المستخدم المخصص
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, DarkPurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(LightPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = DarkPurple)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("الاسم / اسم المستخدم", "Name / Username"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextGray)
                                Text("${parent.name} (@${parent.username})", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ElkhetaTextDark)
                            }
                        }
                        IconButton(onClick = { copyToClipboard(context, parent.username) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = DarkPurple, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // حقل رقم الواتساب المخصص
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, WhatsAppGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(WhatsAppGreen.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF15803D))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("رقم الواتساب المسجل", "Registered WhatsApp Number"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                Text(parent.phone, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ElkhetaTextDark)
                            }
                        }
                        IconButton(onClick = { copyToClipboard(context, parent.phone) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // تباين بصري واضح بين الأزرار:
        // 1. زر رئيسي ممتلئ لتعديل البيانات
        Elkheta3DButton(
            text = tr("✏️ تعديل البيانات الشخصية", "✏️ Edit Personal Details"),
            onClick = onEditData,
            modifier = Modifier.fillMaxWidth(),
            mainColor = DarkPurple,
            shadowColor = ElkhetaPurpleShadow,
            height = 48
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 2. زر ثانوي بإطار أخضر وخلفية بيضاء للتواصل مع الدعم
        OutlinedButton(
            onClick = { openPlatformSupportWhatsApp(context) },
            border = BorderStroke(2.dp, WhatsAppGreen),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = PureWhite, contentColor = Color(0xFF15803D)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFF15803D))
            Spacer(modifier = Modifier.width(8.dp))
            Text(tr("تواصل مع الدعم الفني عبر واتساب", "Contact Support on WhatsApp"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF15803D))
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 3. زر تسجيل الخروج بلون تحذيري متباين
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFB91C1C)),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(tr("تسجيل الخروج", "Logout"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

// ==================== إدارة الابن (شاشة كاملة) ====================
@Composable
fun ChildManagementScreen(
    child: StudentProfile,
    allTeachers: List<TeacherProfile>,
    allSchedules: List<ScheduleSlot>,
    appState: MrPrivateState,
    onSelectTeacher: (TeacherProfile) -> Unit,
    onUpdateStudent: (StudentProfile) -> Unit,
    onBack: () -> Unit
) {
    var selectedDay by remember { mutableStateOf<String?>(null) }
    val subjects = getSubjectsFor(child.educationTrack, child.stage)
    val dayKey = selectedDay
    if (dayKey != null) {
        DayScheduleScreen(
            dateKey = dayKey,
            day = child.dailyTimetable[dayKey],
            subjects = subjects,
            editable = true,
            appState = appState,
            onSave = { updatedDay -> onUpdateStudent(child.copy(dailyTimetable = child.dailyTimetable + (dayKey to updatedDay))) },
            onBack = { selectedDay = null }
        )
    } else {
        ChildManagementContent(child, subjects, allTeachers, allSchedules, appState, onSelectTeacher, onUpdateStudent, onBack, onOpenDay = { selectedDay = it })
    }
}

@Composable
private fun ChildManagementContent(
    child: StudentProfile,
    subjects: List<String>,
    allTeachers: List<TeacherProfile>,
    allSchedules: List<ScheduleSlot>,
    appState: MrPrivateState,
    onSelectTeacher: (TeacherProfile) -> Unit,
    onUpdateStudent: (StudentProfile) -> Unit,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var planSubject by remember { mutableStateOf<String?>(null) }
    var bookedSubjectDetails by remember { mutableStateOf<Pair<String, List<ScheduleSlot>>?>(null) }

    // فلاتر وبحث تبويب المعلمون
    var selectedSubjectFilter by remember { mutableStateOf("ALL") }
    var teacherSearchQuery by remember { mutableStateOf("") }
    var modeFilter by remember { mutableStateOf("ALL") } // ALL, ONLINE, OFFLINE
    var classTypeFilter by remember { mutableStateOf("ALL") } // ALL, GROUP, PRIVATE
    var sortByOption by remember { mutableStateOf("PREMIUM_RATING") } // PREMIUM_RATING, RATING_ONLY

    val tabIndex = appState.openChildTab
    val scrollState = rememberScrollState()

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(scrollState).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        size = 52,
                        isDarkBg = false,
                        photoUriString = child.photoUriString,
                        onPhotoChange = { uri ->
                            val b64 = uriToBase64(context, uri)
                            if (b64.isNotEmpty()) onUpdateStudent(child.copy(photoUriString = b64))
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(tr("إدارة: ${child.name}", "Manage: ${child.name}"), fontSize = 19.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            InfoChip(trValue(child.stage))
                            InfoChip(trValue(child.educationTrack))
                        }
                    }
                }
                LanguageSwitcherButton(appState = null, isLightMode = true)
            }
            Text(tr("لتغيير المرحلة تواصل مع الدعم", "To change the stage, contact support"), fontSize = 11.sp, color = ElkhetaTextGray, modifier = Modifier.padding(top = 6.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // بيانات دخول الطالب
            AppCard(modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(tr("بيانات دخول الطالب", "Student login details"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextGray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(tr("اسم المستخدم", "Username"), fontSize = 11.sp, color = ElkhetaTextGray)
                            Text(child.username, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ElkhetaTextDark)
                        }
                        TextButton(onClick = { copyToClipboard(context, child.username) }) { Text(tr("نسخ", "Copy")) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // التابات الأربعة
            val tabTitles = listOf(tr("الجدول", "Schedule"), tr("المواد", "Subjects"), tr("المعلمون", "Teachers"), tr("الحجوزات", "Bookings"))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabTitles.forEachIndexed { i, title ->
                    Surface(
                        color = if (tabIndex == i) ElkhetaCyan else PureWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElkhetaInputBorder),
                        modifier = Modifier.weight(1f).clickable { appState.openChildTab = i }
                    ) {
                        Text(title, modifier = Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (tabIndex == i) PureWhite else ElkhetaTextDark)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (tabIndex) {
                0 -> {
                    val win = currentTermWindow(appState.termSettings)
                    Text(
                        if (win == null) tr("انتهت السنة الدراسية", "School year ended")
                        else tr("الترم ${if (win.first == 1) "الأول" else "الثاني"}: ${win.second} ← ${win.third} (نظام 12 ساعة)", "Term ${win.first}: ${win.second} → ${win.third} (12-Hour Format)"),
                        fontSize = 12.sp, color = ElkhetaTextGray, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            MonthCalendar(timetable = child.dailyTimetable, selectedDateKey = null, onDayClick = onOpenDay)
                        }
                    }
                    Text(tr("اضغط على أي يوم لتقسيم المواد والمواعيد بنظام 12 ساعة", "Tap any day to arrange subjects and times in 12-hour format"), fontSize = 12.sp, color = ElkhetaTextGray, modifier = Modifier.padding(top = 8.dp))
                }
                1 -> {
                    subjects.forEach { s ->
                        val plan = child.subjectPlans[s]
                        val pct = plan?.completionPercentage ?: 0
                        val accent = getSubjectVisualStyle(s).accentColor

                        // التحقق مما إذا كانت المادة محجوزة مع معلم
                        val bookedSlotsForSubject = allSchedules.filter { slot ->
                            val t = allTeachers.find { it.id == slot.teacherId }
                            val isEnrolled = slot.enrollments.any { it.studentId == child.id || it.studentName == child.name }
                            val matchesSubj = t != null && (
                                subjectArabicName(t.subject) == subjectArabicName(s) ||
                                t.subject.equals(s, ignoreCase = true)
                            )
                            isEnrolled && matchesSubj
                        }
                        val bookedTeacherNames = bookedSlotsForSubject.mapNotNull { slot ->
                            allTeachers.find { it.id == slot.teacherId }?.fullName
                        }.distinct()

                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .shadow(3.dp, RoundedCornerShape(16.dp))
                                .then(
                                    if (bookedSlotsForSubject.isNotEmpty())
                                        Modifier
                                            .border(1.5.dp, ElkhetaCyan, RoundedCornerShape(16.dp))
                                            .clickable { bookedSubjectDetails = s to bookedSlotsForSubject }
                                    else Modifier
                                ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(trValue(s), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ElkhetaTextDark)
                                        if (bookedTeacherNames.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp)) {
                                                Text(
                                                    text = tr("👨‍🏫 محجوز مع: ${bookedTeacherNames.joinToString(" • ")} (اضغط للتفاصيل ›)", "👨‍🏫 Booked with: ${bookedTeacherNames.joinToString(" • ")} (Tap for details ›)"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0369A1),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    // تقسيم المنهج بدلاً من ضبط الخطة
                                    TextButton(onClick = { planSubject = s }) {
                                        Text(tr("تقسيم المنهج", "Divide Syllabus"), fontWeight = FontWeight.ExtraBold, color = DarkPurple)
                                    }
                                }
                                ProgressBar(pct, accent)
                                Text(
                                    if (plan?.isFinished == true) tr("✓ خلّص المقرر", "✓ Syllabus finished") else tr("إنجاز $pct%", "Progress $pct%"),
                                    fontSize = 12.sp, color = ElkhetaTextGray, modifier = Modifier.padding(top = 4.dp)
                                )
                                SubjectEducationalLevelSection(
                                    student = child,
                                    subjectName = s,
                                    appState = appState,
                                    isStudentView = false
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // شريط بحث وفلاتر متقدمة لتسهيل العثور على المعلمين
                    OutlinedTextField(
                        value = teacherSearchQuery,
                        onValueChange = { teacherSearchQuery = it },
                        placeholder = { Text(tr("ابحث باسم المعلم أو المادة...", "Search by teacher name or subject..."), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkPurple) },
                        trailingIcon = {
                            if (teacherSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { teacherSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColorsBordered(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // فلاتر متقدمة: نوع الحضور (أونلاين / حضوري) ونوع الحصة (جروب / برايفت) والترتيب
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modeOptions = listOf(
                            "ALL" to tr("الكل (حضور)", "All Modes"),
                            "ONLINE" to tr("💻 أونلاين", "💻 Online"),
                            "OFFLINE" to tr("🏫 حضوري", "🏫 Offline")
                        )
                        modeOptions.forEach { (key, label) ->
                            val active = modeFilter == key
                            FilterChip(
                                selected = active,
                                onClick = { modeFilter = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        val typeOptions = listOf(
                            "GROUP" to tr("👥 جروب", "👥 Group"),
                            "PRIVATE" to tr("👤 برايفت", "👤 Private")
                        )
                        typeOptions.forEach { (key, label) ->
                            val active = classTypeFilter == key
                            FilterChip(
                                selected = active,
                                onClick = { classTypeFilter = if (active) "ALL" else key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        FilterChip(
                            selected = sortByOption == "RATING_ONLY",
                            onClick = { sortByOption = if (sortByOption == "RATING_ONLY") "PREMIUM_RATING" else "RATING_ONLY" },
                            label = { Text(tr("⭐ الأعلى تقييماً", "⭐ Top Rated"), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // شريط فلتر المواد تحت تابة المعلمون
                    Text(tr("فلتر حسب المادة الدراسية:", "Filter by Subject:"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = selectedSubjectFilter == "ALL"
                        Surface(
                            color = if (isAllSelected) DarkPurple else PureWhite,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isAllSelected) DarkPurple else ElkhetaInputBorder),
                            modifier = Modifier.clickable { selectedSubjectFilter = "ALL" }
                        ) {
                            Text(
                                text = tr("الكل", "All"),
                                color = if (isAllSelected) PureWhite else ElkhetaTextDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                        subjects.forEach { subj ->
                            val isSelected = selectedSubjectFilter == subj
                            Surface(
                                color = if (isSelected) DarkPurple else PureWhite,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) DarkPurple else ElkhetaInputBorder),
                                modifier = Modifier.clickable { selectedSubjectFilter = subj }
                            ) {
                                Text(
                                    text = trValue(subj),
                                    color = if (isSelected) PureWhite else ElkhetaTextDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val allowedAr = stageSubjectsAr[child.stage]
                    val cleanSearch = teacherSearchQuery.trim().lowercase()
                    val filteredBase = allTeachers.filter { teacher ->
                        val teacherCoversChildArea = teacher.area == child.area ||
                            (teacher.effectiveIsPremium && teacher.extraAreas.take(2).contains(child.area))
                        val matchesSubjectFilter = selectedSubjectFilter == "ALL" ||
                            subjectArabicName(teacher.subject) == subjectArabicName(selectedSubjectFilter) ||
                            teacher.subject.equals(selectedSubjectFilter, ignoreCase = true)
                        val matchesSearch = cleanSearch.isBlank() ||
                            teacher.fullName.lowercase().contains(cleanSearch) ||
                            teacher.subject.lowercase().contains(cleanSearch) ||
                            subjectArabicName(teacher.subject).lowercase().contains(cleanSearch)

                        val teacherSlots = allSchedules.filter { it.teacherId == teacher.id }
                        val matchesMode = when (modeFilter) {
                            "ONLINE" -> teacherSlots.any { it.isOnline }
                            "OFFLINE" -> teacherSlots.any { !it.isOnline }
                            else -> true
                        }
                        val matchesClassType = when (classTypeFilter) {
                            "GROUP" -> teacherSlots.any { !it.isPrivate }
                            "PRIVATE" -> teacherSlots.any { it.isPrivate }
                            else -> true
                        }

                        teacher.isApproved && teacher.governorate == child.governorate && teacherCoversChildArea &&
                            (teacher.stages.contains(child.stage) || ((child.stage == "ثانوي" || child.stage == "بكالوريا") && (teacher.stages.contains("ثانوي") || teacher.stages.contains("بكالوريا")))) &&
                            (allowedAr == null || subjectArabicName(teacher.subject) in allowedAr) &&
                            matchesSubjectFilter && matchesSearch && matchesMode && matchesClassType
                    }

                    val matched = if (sortByOption == "RATING_ONLY") {
                        filteredBase.sortedByDescending { it.averageRating }
                    } else {
                        filteredBase.sortedWith(compareByDescending<TeacherProfile> { it.effectiveIsPremium }.thenByDescending { it.averageRating })
                    }

                    if (matched.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Text(tr("لا يوجد معلمون مطابقون لخيارات البحث والفلترة الحالية.", "No teachers match your search/filter criteria."), modifier = Modifier.padding(20.dp), color = Color.Gray)
                        }
                    }
                    matched.forEach { teacher ->
                        AppCard(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(size = 52, isDarkBg = false, photoUriString = teacher.photoUriString, onPhotoChange = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${if (teacher.effectiveIsPremium) "⭐ " else ""}${teacher.fullName}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ElkhetaTextDark)
                                    Text(trValue(teacher.subject), fontSize = 12.sp, color = ElkhetaTextGray)
                                    StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size, darkText = true)
                                }
                                Button(onClick = { onSelectTeacher(teacher) }, colors = ButtonDefaults.buttonColors(containerColor = ElkhetaCyan), shape = RoundedCornerShape(12.dp)) {
                                    Text(tr("المواعيد", "Slots"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                else -> {
                    ChildBookingsList(child, allTeachers, allSchedules, showParentNote = true, appState = appState)
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
        // زر الرجوع العائم يتقلص ديناميكياً أثناء التمرير ليظهر السهم فقط، ويتوسع مجدداً عند التوقف
        RightCenterFloatingBackButton(onBack = onBack, isScrolling = scrollState.isScrollInProgress)
    }

    bookedSubjectDetails?.let { (subjName, slots) ->
        BookedSubjectTeacherDetailsDialog(
            child = child,
            subjectName = subjName,
            bookedSlots = slots,
            allTeachers = allTeachers,
            appState = appState,
            onDismiss = { bookedSubjectDetails = null }
        )
    }

    planSubject?.let { sName ->
        EditSubjectPlanDialog(
            subjectName = sName,
            existingPlan = child.subjectPlans[sName] ?: SubjectPlanData(subjectName = sName),
            termSettings = appState.termSettings,
            onDismiss = { planSubject = null },
            onSavePlan = { plan ->
                val timetable = autoDistributeSubjectPlanOnTimetable(child, sName, plan.studyDaysPerWeek, appState.termSettings, plan.isFinished)
                onUpdateStudent(child.copy(subjectPlans = child.subjectPlans + (sName to plan), dailyTimetable = timetable))
                planSubject = null
            }
        )
    }
}
