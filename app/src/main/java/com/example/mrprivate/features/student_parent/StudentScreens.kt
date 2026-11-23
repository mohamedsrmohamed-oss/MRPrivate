@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.student_parent

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.R
import com.example.mrprivate.data.model.*
import com.example.mrprivate.features.quiz.StudentTakeQuizDialog
import com.example.mrprivate.features.quiz.SubjectEducationalLevelSection
import com.example.mrprivate.services.StudentNotificationHelper
import com.example.mrprivate.ui.components.*

@Composable
fun StudentHomeScreen(
    student: StudentProfile,
    allStudents: List<StudentProfile>,
    appState: MrPrivateState,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(-1) }
    var selectedDay by remember { mutableStateOf(getTodayDateKey()) }
    var showProfile by remember { mutableStateOf(false) }
    var activeQuizToTake by remember { mutableStateOf<QuizExam?>(null) }
    val subjects = getSubjectsFor(student.educationTrack, student.stage)
    val scrollState = rememberScrollState()

    LaunchedEffect(student.id, student.dailyTimetable, appState.schedules.size) {
        StudentNotificationHelper.scheduleStudentPushNotifications(
            context = context,
            student = student,
            allTeachers = appState.teachers,
            allSchedules = appState.schedules
        )
    }

    BackHandler(enabled = selectedTab != -1 && !showProfile) { selectedTab = -1 }

    if (showProfile) {
        StudentProfileScreen(
            student = student,
            parent = allStudents.find { it.id == student.parentId },
            appState = appState,
            onBack = { showProfile = false },
            onLogout = onLogout
        )
    } else {
        Scaffold(
            containerColor = ElkhetaSoftBgBottom,
            bottomBar = {
                ElkhetaBottomNavBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    items = listOf(
                        Triple(0, tr("جدولي", "Schedule"), "📅"),
                        Triple(1, tr("موادي", "Subjects"), "📚"),
                        Triple(2, tr("حجوزاتي", "Bookings"), "✅")
                    ),
                    isScrolling = scrollState.isScrollInProgress,
                    iconRes = mapOf(
                        0 to R.drawable.ic3d_calendar_blue,
                        1 to R.drawable.ic3d_cap_blue,
                        2 to R.drawable.ic3d_shield_blue
                    )
                )
            }
        ) { padding ->
            BeautifulAppBackground(isFullDark = false, appState = appState) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                        .statusBarsPadding()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showProfile = true }
                        ) {
                            ProfileAvatar(size = 46, isDarkBg = false, photoUriString = student.photoUriString, onPhotoChange = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student.name, fontSize = 19.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
                                Text("${trValue(student.stage)} ${if (student.grade.isNotBlank()) "· ${student.grade}" else ""} · ${trValue(student.educationTrack)}  ›", fontSize = 12.sp, color = ElkhetaTextGray)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageSwitcherButton(appState = null, isLightMode = true)
                            Surface(shape = RoundedCornerShape(20.dp), color = PureWhite, border = BorderStroke(1.5.dp, AuroraMid), modifier = Modifier.clickable { onLogout() }) {
                                Text(tr("خروج", "Logout"), color = AuroraAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedTab) {
                        -1 -> {
                            val studentSlotIds = remember(appState.schedules, student.id) {
                                appState.schedules
                                    .filter { slot -> slot.enrollments.any { it.studentId == student.id || (it.studentName.isNotBlank() && it.studentName == student.name) } }
                                    .map { it.id }
                                    .toSet()
                            }
                            val pendingQuizzes = appState.quizzes.filter { q ->
                                val isTargeted = q.targetStudentIds.contains(student.id) || q.slotId in studentSlotIds
                                isTargeted && !q.submissions.containsKey(student.id)
                            }

                            if (pendingQuizzes.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.5.dp, GoldStar),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .shadow(3.dp, RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📝", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tr("اختبارات بانتظار الحل (${pendingQuizzes.size})", "Pending Quizzes (${pendingQuizzes.size})"),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFFFDE68A),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = tr("مطلوب حله", "Action Required"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        pendingQuizzes.forEach { q ->
                                            Surface(
                                                color = PureWhite,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = q.title,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = ElkhetaTextDark
                                                        )
                                                        Text(
                                                            text = "${q.teacherName} • ${trValue(q.subject)} • ${q.durationMinutes} ${tr("دقيقة", "min")}",
                                                            fontSize = 11.sp,
                                                            color = ElkhetaTextGray
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { activeQuizToTake = q },
                                                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = tr("ابدأ الكويز", "Start"),
                                                            color = PureWhite,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            StudentHeroHome(
                                student = student,
                                appState = appState,
                                onOpenSchedule = { selectedTab = 0 },
                                onOpenSubjects = { selectedTab = 1 }
                            )
                        }
                        0 -> {
                            Text(tr("جدولك الدراسي (اضغط على أي يوم لعرض مواده ومواعيده).", "Tap any day on the calendar to view its sessions."), fontSize = 12.sp, color = ElkhetaTextGray)
                            Spacer(modifier = Modifier.height(10.dp))
                            AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    MonthCalendar(
                                        timetable = student.dailyTimetable,
                                        selectedDateKey = selectedDay,
                                        onDayClick = { selectedDay = it }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            InlineStudentDayScheduleSection(
                                dateKey = selectedDay,
                                day = student.dailyTimetable[selectedDay],
                                subjects = subjects,
                                studentId = student.id,
                                appState = appState
                            )
                        }
                        1 -> {
                            Text(tr("موادي الدراسية", "My Subjects"), fontSize = 18.sp, fontWeight = FontWeight.Black, color = DarkPurple)
                            Spacer(modifier = Modifier.height(10.dp))
                            subjects.forEach { s ->
                                val plan = student.subjectPlans[s] ?: SubjectPlanData(subjectName = s)
                                val pct = plan.completionPercentage
                                val style = getSubjectVisualStyle(s)
                                val accent = style.accentColor

                                AppCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .shadow(3.dp, RoundedCornerShape(18.dp)),
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
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(Brush.linearGradient(style.tileGradient)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(style.symbolText, color = PureWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = trValue(s),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = ElkhetaTextDark
                                                )
                                            }
                                            Surface(
                                                color = accent.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = if (plan.isFinished) tr("مكتمل 100%", "100% Done") else "$pct%",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = accent,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(9.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(ElkhetaInputBorder)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((pct.coerceIn(0, 100)) / 100f)
                                                    .fillMaxHeight()
                                                    .background(accent)
                                            )
                                        }
                                        SubjectEducationalLevelSection(
                                            student = student,
                                            subjectName = s,
                                            appState = appState,
                                            isStudentView = true
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(tr("حجوزاتي ومواعيدي", "My Bookings"), fontSize = 18.sp, fontWeight = FontWeight.Black, color = DarkPurple)
                            Spacer(modifier = Modifier.height(10.dp))
                            StudentBookingsWithStatusBadges(
                                student = student,
                                allTeachers = appState.teachers,
                                allSchedules = appState.schedules,
                                appState = appState,
                                onTakeQuiz = { activeQuizToTake = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }

        activeQuizToTake?.let { quiz ->
            StudentTakeQuizDialog(
                quiz = quiz,
                student = student,
                onDismiss = { activeQuizToTake = null },
                onSubmitQuiz = { answers ->
                    appState.submitStudentQuizAnswers(quiz, student, answers)
                }
            )
        }
    }
}

@Composable
fun StudentBookingsWithStatusBadges(
    student: StudentProfile,
    allTeachers: List<TeacherProfile>,
    allSchedules: List<ScheduleSlot>,
    appState: MrPrivateState? = null,
    onTakeQuiz: ((QuizExam) -> Unit)? = null
) {
    val mySlots = allSchedules.filter { slot ->
        slot.enrollments.any { it.studentId == student.id || it.studentName == student.name }
    }

    if (mySlots.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Text(
                text = tr("لا توجد حجوزات مسجلة حالياً.", "No bookings found yet."),
                modifier = Modifier.padding(20.dp),
                color = ElkhetaTextGray,
                fontSize = 13.sp
            )
        }
    } else {
        mySlots.forEach { slot ->
            val slotTeacher = allTeachers.find { it.id == slot.teacherId }
            val enroll = slot.enrollments.find { it.studentId == student.id || it.studentName == student.name }
            val isConfirmed = enroll?.isConfirmed == true

            val badgeBgColor = if (isConfirmed) Color(0xFFDCFCE7) else Color(0xFFFEF9C3)
            val badgeBorderColor = if (isConfirmed) Color(0xFF22C55E) else Color(0xFFEAB308)
            val badgeTextColor = if (isConfirmed) Color(0xFF15803D) else Color(0xFFA16207)
            val badgeText = if (isConfirmed) {
                tr("● مؤكد (Confirmed)", "● Confirmed")
            } else {
                tr("● معلق - بانتظار التأكيد (Pending)", "● Pending Approval")
            }

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trValue(slotTeacher?.subject ?: slot.stage),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = DarkPurple
                            )
                            Text(
                                text = slotTeacher?.fullName ?: tr("معلم المنصة", "Platform Teacher"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ElkhetaTextDark
                            )
                        }

                        Surface(
                            color = badgeBgColor,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.2.dp, badgeBorderColor)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${trValue(slot.day)} • ${slot.time}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                        Text(
                            text = if (slot.isOnline) tr("💻 أونلاين", "💻 Online") else tr("🏫 حضوري", "🏫 Offline"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElkhetaTextGray
                        )
                    }

                    if (appState != null) {
                        val slotQuizzes = appState.quizzes.filter { it.slotId == slot.id }
                        if (slotQuizzes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = ElkhetaInputBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Text(
                                text = tr("📝 اختبارات هذه المجموعة (${slotQuizzes.size}):", "📝 Group Quizzes (${slotQuizzes.size}):"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkPurple
                            )
                            slotQuizzes.forEach { q ->
                                val sub = q.submissions[student.id]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = q.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ElkhetaTextDark
                                        )
                                        Text(
                                            text = if (sub != null) "${tr("تم التسليم", "Submitted")}: ${sub.score}/${sub.totalScore}" else tr("لم يتم الحل بعد ⏳", "Pending ⏳"),
                                            fontSize = 10.sp,
                                            color = if (sub != null) Color(0xFF15803D) else Color(0xFFB45309),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (sub == null && onTakeQuiz != null) {
                                        Button(
                                            onClick = { onTakeQuiz(q) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = tr("حل الآن", "Take Quiz"),
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (sub != null) {
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "✓ ${sub.score}/${sub.totalScore}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    student: StudentProfile,
    parent: StudentProfile?,
    appState: MrPrivateState,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val pct = averageCompletion(student)
    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(Brush.linearGradient(listOf(ElkhetaTealHeader, Color(0xFF0F766E))))
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp), color = PureWhite, modifier = Modifier.clickable { onBack() }) {
                        Text(tr("→ الجدول", "→ Schedule"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    LanguageSwitcherButton(appState = null, isLightMode = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(PureWhite).border(4.dp, PureWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(size = 92, isDarkBg = false, photoUriString = student.photoUriString, onPhotoChange = null)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(student.name, fontSize = 21.sp, fontWeight = FontWeight.Black, color = PureWhite)
                Text("${trValue(student.stage)} · ${trValue(student.educationTrack)} · ${trValue(student.governorate)} · ${trValue(student.area)}", fontSize = 12.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
            }
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    tr("كود الحساب", "Account ID") to student.username,
                    tr("ولي الأمر", "Parent") to (parent?.name ?: "-"),
                    tr("إنجاز المنهج", "Syllabus progress") to "$pct%"
                ).forEach { (label, value) ->
                    AppCard(modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 13.sp, color = ElkhetaTextGray)
                            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextDark)
                        }
                    }
                }
                Elkheta3DButton(text = tr("تواصل مع الدعم على واتساب", "Contact Support on WhatsApp"), onClick = { openPlatformSupportWhatsApp(context) }, modifier = Modifier.fillMaxWidth(), mainColor = WhatsAppGreen, shadowColor = ElkhetaGreenShadow, height = 48)
                Elkheta3DButton(text = tr("تسجيل الخروج", "Logout"), onClick = onLogout, modifier = Modifier.fillMaxWidth(), mainColor = Color(0xFFB91C1C), shadowColor = Color(0xFF7F1D1D), height = 48)
                Text(tr("بيانات الحساب بيعدلها ولي الأمر أو الإدارة", "Account details are edited by your parent or the admin"), fontSize = 12.sp, color = ElkhetaTextGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        RightCenterFloatingBackButton(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScheduleForStudentScreen(
    student: StudentProfile,
    teacher: TeacherProfile,
    schedules: List<ScheduleSlot>,
    onBookSlot: (ScheduleSlot) -> Unit,
    onSubmitRating: (ScheduleSlot, String, Int) -> Unit,
    onBack: () -> Unit
) {
    val confirmedSlotAndEnrollment = schedules.firstNotNullOfOrNull { slot ->
        val enrollment = slot.enrollments.find { it.studentId == student.id && it.isConfirmed }
        if (enrollment != null) Pair(slot, enrollment) else null
    }
    var selectedStarsForRating by remember { mutableIntStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(teacher.fullName, color = PureWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = PureWhite) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkPurple), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${teacher.fullName} (${trValue(teacher.educationTrack)})", color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("مادة ${trValue(teacher.subject)} - ${trValue(teacher.governorate)} (${trValue(teacher.area)})", color = LightPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (confirmedSlotAndEnrollment != null) {
                val (confirmedSlot, confirmedEnrollment) = confirmedSlotAndEnrollment
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, GoldStar, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (confirmedEnrollment.hasRated) {
                            Text(tr("شكراً لك! تم تسجيل تقييمك للمعلم بنجاح.", "Thank you! Rating submitted."), fontWeight = FontWeight.Bold, color = DarkPurple)
                        } else {
                            Text(tr("تم قبولك في المجموعة! يمكنك الآن تقييم المعلم:", "Confirmed! Rate your teacher:"), fontWeight = FontWeight.Bold, color = DarkPurple)
                            Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = if (i <= selectedStarsForRating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null, tint = GoldStar,
                                        modifier = Modifier.size(34.dp).clickable { selectedStarsForRating = i }
                                    )
                                }
                            }
                            Elkheta3DButton(
                                text = tr("إرسال التقييم", "Submit Rating"),
                                onClick = { onSubmitRating(confirmedSlot, confirmedEnrollment.id, selectedStarsForRating) },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                height = 42
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(tr("المواعيد السنوية المتاحة للحجز (تتكرر أسبوعياً طوال 36 أسبوع):", "Available Yearly Slots:"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkPurple)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(schedules, key = { it.id }) { slot ->
                    val existingBooking = slot.enrollments.find { it.studentId == student.id }
                    val isFull = slot.enrollments.size >= slot.maxStudents
                    val typeText = if (slot.isPrivate) tr("برايفت", "Private") else tr("جروب (${slot.enrollments.size}/${slot.maxStudents})", "Group (${slot.enrollments.size}/${slot.maxStudents})")
                    val locText = if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")

                    AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${trValue(slot.day)} - ${slot.time}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkPurple)
                                Text("$typeText • $locText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextGray)
                                Text(tr("السعر: ${slot.price} جنيه • مستمر طوال العام الدراسي", "Price: ${slot.price} EGP • Yearly"), fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                            when {
                                existingBooking != null -> {
                                    Surface(color = if (existingBooking.isConfirmed) Color(0xFF2E7D32) else Color(0xFFF57C00), shape = RoundedCornerShape(10.dp)) {
                                        Text(
                                            text = if (existingBooking.isConfirmed) tr("تم التأكيد ✓", "Confirmed ✓") else tr("انتظار قبول المعلم", "Waiting Teacher Approval"),
                                            color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                isFull -> Text(tr("مكتمل", "Full"), color = Color.Red, fontWeight = FontWeight.Bold)
                                else -> {
                                    Elkheta3DButton(
                                        text = tr("حجز الميعاد", "Book"),
                                        onClick = { onBookSlot(slot) },
                                        modifier = Modifier.width(110.dp),
                                        height = 40
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        RightCenterFloatingBackButton(onBack = onBack)
    }
}
