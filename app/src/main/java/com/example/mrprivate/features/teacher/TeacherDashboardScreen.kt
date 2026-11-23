@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.LocalIsArabic
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.QuizExam
import com.example.mrprivate.R
import com.example.mrprivate.ScheduleSlot
import com.example.mrprivate.StudentEnrollment
import com.example.mrprivate.TeacherProfile
import com.example.mrprivate.features.quiz.CreateQuizFromPaperDialog
import com.example.mrprivate.features.quiz.TeacherQuizResultsDialog
import com.example.mrprivate.tr
import com.example.mrprivate.trValue
import com.example.mrprivate.translateValue
import com.example.mrprivate.ui.components.*
import com.example.mrprivate.uriToBase64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    teacher: TeacherProfile,
    schedules: List<ScheduleSlot>,
    appState: MrPrivateState? = null,
    onAddSchedule: (ScheduleSlot) -> Unit,
    onConfirmStudent: (ScheduleSlot, String) -> Unit,
    onCompleteStudentSession: (ScheduleSlot, String, Int, String) -> Unit = { _, _, _, _ -> },
    onUpdateAccountPhoto: ((String, String, String) -> Unit)? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = LocalIsArabic.current

    // التحقق من صلاحية اشتراك الترم الدراسي
    val isInteractiveAllowed = teacher.isInteractiveActive
    val isPremiumActive = teacher.effectiveIsPremium
    val isUltimateActive = teacher.effectiveIsUltimate

    // 0: الإدارة والاشتراكات | 1: الكويزات والاختبارات | 2: المواعيد والطلاب
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var showUltimateSupportDialog by remember { mutableStateOf(false) }
    var quizToViewResults by remember { mutableStateOf<QuizExam?>(null) }
    var quizToReassign by remember { mutableStateOf<QuizExam?>(null) }
    var slotToEdit by remember { mutableStateOf<ScheduleSlot?>(null) }
    var slotForExtraSession by remember { mutableStateOf<ScheduleSlot?>(null) }
    var studentToComplete by remember { mutableStateOf<Pair<ScheduleSlot, StudentEnrollment>?>(null) }

    val teacherQuizzes = appState?.quizzes?.filter { it.teacherId == teacher.id } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("لوحة تحكم المعلم", "Teacher Dashboard"), color = PureWhite, fontWeight = FontWeight.Bold) },
                actions = {
                    LanguageSwitcherButton(appState = appState)
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "خروج", tint = PureWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TeacherPurple)
            )
        },
        floatingActionButton = {
            if (isInteractiveAllowed) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TeacherPurple,
                    contentColor = PureWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة موعد", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. اللافتة الرئيسية البارزة في الأعلى (Wide Rectangular Card)
            Card(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(TeacherPurpleDark, TeacherPurpleEnd)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            size = 66,
                            photoUriString = teacher.photoUriString,
                            onPhotoChange = if (onUpdateAccountPhoto != null) { uri ->
                                val b64 = uriToBase64(context, uri)
                                if (b64.isNotEmpty()) onUpdateAccountPhoto("teacher", teacher.id, b64)
                            } else null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${teacher.fullName} (${trValue(teacher.educationTrack)})",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isInteractiveAllowed) {
                                val badgeTitle = if (isUltimateActive) {
                                    tr("👑 بروفايل معلم - اشتراك شامل مفعل", "👑 Ultimate Teacher Active")
                                } else {
                                    tr("👑 بروفايل معلم بريميوم مفعل", "👑 Premium Teacher Active")
                                }
                                Surface(color = PureWhite, shape = RoundedCornerShape(20.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = TeacherPurple, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(badgeTitle, color = TeacherPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                Surface(color = PureWhite.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        text = tr("الوضع الأساسي (بروفايل مرئي فقط)", "Basic Mode (View Only)"),
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${trValue(teacher.subject)} | ${teacher.stages.joinToString(" - ") { translateValue(it, isArabic) }}",
                                color = PureWhite.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            val areasLabel = (listOf(teacher.area) + (if (isPremiumActive) teacher.extraAreas.take(2) else emptyList()))
                                .filter { it.isNotBlank() }
                                .joinToString(" • ") { translateValue(it, isArabic) }
                            Text("${trValue(teacher.governorate)}: $areasLabel", color = Color(0xFFFFE082), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size)
                        }
                    }

                    // لوحة تحفيزية لتجديد الاشتراك عند انتهاء اشتراك الترم
                    if (!isInteractiveAllowed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = tr(
                                        "جدد اشتراكك الآن لتستعيد كامل ميزات Premium أو Ultimate وتبهر طلابك من جديد.",
                                        "Renew your subscription now to restore all Premium or Ultimate features and amaze your students again."
                                    ),
                                    color = TeacherPurpleDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "PREMIUM", isArabic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr("تجديد بريميوم", "Renew Premium"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "ULTIMATE", isArabic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurpleDark),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr("تجديد اشتراك شامل", "Renew Ultimate"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. مربعات دائرية ثلاثية الأبعاد للتنقل بين التبويبات الثلاثة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThreeDRoundedSquareTab(
                    title = tr("الإدارة والاشتراكات", "Admin & Plans"),
                    icon = Icons.Default.AdminPanelSettings,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_shield_purple
                )
                ThreeDRoundedSquareTab(
                    title = tr("الكويزات والاختبارات", "Quizzes & Library"),
                    icon = Icons.Default.Quiz,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_tools_purple
                )
                ThreeDRoundedSquareTab(
                    title = tr("المواعيد والطلاب", "Schedule & Students"),
                    icon = Icons.Default.EventAvailable,
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_calendar_purple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. شبكة أيقونات صغيرة دقيقة للإجراءات السريعة
            AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SmallRedWhiteGlyphAction(
                        title = tr("إضافة موعد", "Add Slot"),
                        icon = Icons.Default.AddCircleOutline,
                        enabled = isInteractiveAllowed,
                        onClick = { showAddDialog = true }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("إنشاء اختبار", "Create Quiz"),
                        icon = Icons.Default.PostAdd,
                        enabled = isInteractiveAllowed,
                        onClick = {
                            if (isUltimateActive) showCreateQuizDialog = true
                            else showUltimateSupportDialog = true
                        }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("الحضور والتقييم", "Attendance"),
                        icon = Icons.Default.FactCheck,
                        enabled = isInteractiveAllowed,
                        onClick = { selectedTab = 2 }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("التقارير والدرجات", "Reports"),
                        icon = Icons.Default.Assessment,
                        enabled = isInteractiveAllowed,
                        onClick = { selectedTab = 1 }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // محتوى التبويبات الثلاثة
            when (selectedTab) {
                0 -> TeacherManagementAndSubscriptionTab(
                    teacher = teacher,
                    isPremiumActive = isPremiumActive,
                    isUltimateActive = isUltimateActive,
                    isInteractiveAllowed = isInteractiveAllowed,
                    appState = appState,
                    context = context,
                    isArabic = isArabic,
                    onCreateQuizClick = {
                        if (isUltimateActive) showCreateQuizDialog = true
                        else showUltimateSupportDialog = true
                    }
                )
                1 -> TeacherQuizzesTab(
                    teacherQuizzes = teacherQuizzes,
                    isInteractiveAllowed = isInteractiveAllowed,
                    schedules = schedules,
                    appState = appState,
                    context = context,
                    onCreateQuizClick = {
                        if (isUltimateActive) showCreateQuizDialog = true
                        else showUltimateSupportDialog = true
                    },
                    onViewResults = { quizToViewResults = it },
                    onReassignQuiz = { quizToReassign = it }
                )
                2 -> TeacherSchedulesAndStudentsTab(
                    schedules = schedules,
                    isInteractiveAllowed = isInteractiveAllowed,
                    appState = appState,
                    context = context,
                    onConfirmStudent = onConfirmStudent,
                    onEditSlot = { slotToEdit = it },
                    onSlotForExtraSession = { slotForExtraSession = it },
                    onStudentToComplete = { studentToComplete = it }
                )
            }
        }
    }

    if (showUltimateSupportDialog) {
        TeacherUltimateSupportDialog(
            teacher = teacher,
            context = context,
            isArabic = isArabic,
            onDismiss = { showUltimateSupportDialog = false }
        )
    }

    quizToReassign?.let { sourceQuiz ->
        ReassignQuizDialog(
            sourceQuiz = sourceQuiz,
            schedules = schedules,
            appState = appState,
            context = context,
            onDismiss = { quizToReassign = null }
        )
    }

    slotForExtraSession?.let { baseSlot ->
        ExtraSessionDialog(
            baseSlot = baseSlot,
            appState = appState,
            context = context,
            onDismiss = { slotForExtraSession = null }
        )
    }

    if (showCreateQuizDialog) {
        val quizSavedMsg = tr("تم حفظ الكويز في مكتبتك وإرساله لطلاب المجموعة!", "Quiz saved to library & sent!")
        CreateQuizFromPaperDialog(
            teacher = teacher,
            schedules = schedules,
            appState = appState,
            onDismiss = { showCreateQuizDialog = false },
            onCreateQuiz = { newQuiz ->
                appState?.createQuizExam(newQuiz)
                Toast.makeText(context, quizSavedMsg, Toast.LENGTH_LONG).show()
                showCreateQuizDialog = false
            }
        )
    }

    quizToViewResults?.let { q ->
        TeacherQuizResultsDialog(
            quiz = q,
            onDismiss = { quizToViewResults = null }
        )
    }

    if (showAddDialog) {
        AddScheduleDialog(
            teacherId = teacher.id,
            initialSlot = null,
            appState = appState,
            onDismiss = { showAddDialog = false },
            onSave = { onAddSchedule(it); showAddDialog = false }
        )
    }

    slotToEdit?.let { editing ->
        val scheduleUpdatedMsg = tr("تم تعديل الموعد وإرسال إشعار وتحديث جداول الطلاب", "Schedule updated & notified")
        AddScheduleDialog(
            teacherId = teacher.id,
            initialSlot = editing,
            appState = appState,
            onDismiss = { slotToEdit = null },
            onSave = { updated ->
                val err = appState?.updateScheduleByTeacher(editing, updated)
                if (err != null) {
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, scheduleUpdatedMsg, Toast.LENGTH_LONG).show()
                    slotToEdit = null
                }
            }
        )
    }

    studentToComplete?.let { (slot, enroll) ->
        CompleteSessionDialog(
            studentName = enroll.studentName,
            onDismiss = { studentToComplete = null },
            onSave = { attended, stars, note ->
                if (appState != null) {
                    appState.completeStudentSession(slot, enroll.id, stars, note, attended)
                } else {
                    onCompleteStudentSession(slot, enroll.id, stars, note)
                }
                studentToComplete = null
            }
        )
    }
}
