@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.student_parent

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.*
import com.example.mrprivate.LocalIsArabic
import com.example.mrprivate.features.admin.EditStudentDialog
import com.example.mrprivate.ui.components.*
import com.example.mrprivate.uriToBase64

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("تذكير بمواعيد الحصص القادمة (${reminders.size})", "Upcoming Sessions Reminder (${reminders.size})"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = ElkhetaTextDark
                        )
                        Text(
                            text = tr("تنبيهات فورية للحصص المجدولة والمؤكدة", "Live reminders for scheduled sessions"),
                            fontSize = 11.sp,
                            color = ElkhetaTextGray
                        )
                    }
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = Color(0xFF0284C7))
                )
            }

            if (remindersEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                if (reminders.isEmpty()) {
                    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = tr("لا توجد حصص مجدولة قريباً للأبناء.", "No upcoming sessions for children soon."),
                            fontSize = 12.sp,
                            color = ElkhetaTextGray,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    val visibleReminders = if (expandedList) reminders else reminders.take(2)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleReminders.forEach { item ->
                            Surface(
                                color = if (item.isToday) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (item.isToday) Color(0xFF93C5FD) else ElkhetaInputBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔔 ${item.childName}: ${item.subjectAndTeacher}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = if (item.isToday) Color(0xFF1D4ED8) else ElkhetaTextDark
                                        )
                                        Text(
                                            text = "${item.dateTitle} • ${item.timeText}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isToday) Color(0xFF2563EB) else ElkhetaTextGray
                                        )
                                    }
                                    if (item.isToday) {
                                        Surface(color = Color(0xFFDBEAFE), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = tr("اليوم", "Today"),
                                                color = Color(0xFF1D4ED8),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (reminders.size > 2) {
                            TextButton(
                                onClick = { expandedList = !expandedList },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = if (expandedList) tr("عرض أقل ▲", "Show Less ▲") else tr("عرض جميع الحصص (${reminders.size}) ▼", "Show All (${reminders.size}) ▼"),
                                    color = Color(0xFF0284C7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

                            ParentHeroCard(
                                children = children,
                                allSchedules = allSchedules,
                                allTeachers = allTeachers,
                                appState = appState
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                val addStudentAdminMsg = tr("لإضافة طالب، يلزم موافقة الإدارة أولاً. تواصل معنا على واتساب.", "Adding a student needs admin approval. Contact us on WhatsApp.")
                                Elkheta3DButton(
                                    text = tr("+ إضافة طالب", "+ Add Student"),
                                    onClick = {
                                        if (!parent.canAddStudentApproved) {
                                            openPlatformSupportWhatsApp(context)
                                            Toast.makeText(context, addStudentAdminMsg, Toast.LENGTH_LONG).show()
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
internal fun ParentAccountTab(
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

        AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("بيانات التواصل والحساب الأساسية", "Primary Account & Contact Fields"), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DarkPurple)

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

        Elkheta3DButton(
            text = tr("✏️ تعديل البيانات الشخصية", "✏️ Edit Personal Details"),
            onClick = onEditData,
            modifier = Modifier.fillMaxWidth(),
            mainColor = DarkPurple,
            shadowColor = ElkhetaPurpleShadow,
            height = 48
        )
        Spacer(modifier = Modifier.height(10.dp))

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
