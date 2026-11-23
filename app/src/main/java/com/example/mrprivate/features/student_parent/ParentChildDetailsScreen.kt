@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.student_parent

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import com.example.mrprivate.features.quiz.SubjectEducationalLevelSection
import com.example.mrprivate.ui.components.*

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
internal fun ChildManagementContent(
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
