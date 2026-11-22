@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentAdmin: AdminAccount,
    allAdmins: List<AdminAccount>,
    allTeachers: List<TeacherProfile>,
    allStudents: List<StudentProfile>,
    allSchedules: List<ScheduleSlot> = emptyList(),
    appState: MrPrivateState? = null,
    checkUsernameTaken: (String) -> Boolean,
    onApproveTeacher: (String) -> Unit,
    onUpdateTeacher: (TeacherProfile) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onUpdateStudent: (StudentProfile) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onUpdateSchedule: (ScheduleSlot) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onConfirmStudentBooking: (ScheduleSlot, String) -> Unit = { _, _ -> },
    onSaveAdmin: (AdminAccount) -> Unit,
    onCreateAdmin: (AdminAccount, String, (AuthError) -> Unit) -> Unit,
    onDeleteAdmin: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val pendingTeachers = allTeachers.filter { !it.isApproved }
    val canDelete = currentAdmin.canDeleteAccounts || currentAdmin.username.equals("M1", ignoreCase = true)

    val cleanQuery = searchQuery.trim().lowercase()
    val filteredPendingTeachers = if (cleanQuery.isBlank()) pendingTeachers else pendingTeachers.filter {
        it.phone.lowercase().contains(cleanQuery) || it.firstName.lowercase().contains(cleanQuery) || it.secondName.lowercase().contains(cleanQuery) || it.username.lowercase().contains(cleanQuery)
    }
    val filteredAllTeachers = if (cleanQuery.isBlank()) allTeachers else allTeachers.filter {
        it.phone.lowercase().contains(cleanQuery) || it.firstName.lowercase().contains(cleanQuery) || it.secondName.lowercase().contains(cleanQuery) || it.username.lowercase().contains(cleanQuery)
    }
    val filteredAllStudents = if (cleanQuery.isBlank()) allStudents else allStudents.filter {
        it.phone.lowercase().contains(cleanQuery) || it.name.lowercase().contains(cleanQuery) || it.username.lowercase().contains(cleanQuery)
    }

    var teacherToEdit by remember { mutableStateOf<TeacherProfile?>(null) }
    var studentToEdit by remember { mutableStateOf<StudentProfile?>(null) }
    var scheduleToEdit by remember { mutableStateOf<ScheduleSlot?>(null) }
    var adminToEdit by remember { mutableStateOf<AdminAccount?>(null) }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var expandedParents by remember { mutableStateOf(setOf<String>()) }
    var showTermDialog by remember { mutableStateOf(false) }
    val isM1Admin = currentAdmin.username.equals("M1", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("لوحة تحكم الإدارة (${currentAdmin.username})", "Admin Panel (${currentAdmin.username})"), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(if (currentAdmin.canManageAdmins) tr("صلاحية كاملة (Super Admin)", "Super Admin") else tr("صلاحية مراجعة وتعديل البيانات", "Editor Admin"), color = LightPurple, fontSize = 11.sp)
                    }
                },
                actions = {
                    if (appState != null) {
                        TextButton(onClick = { showTermDialog = true }) { Text("📅", fontSize = 20.sp) }
                    }
                    LanguageSwitcherButton(appState = appState)
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "خروج", tint = PureWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureWhite,
                contentColor = DarkPurple,
                edgePadding = 16.dp
            ) {
                val tabTitles = mutableListOf(
                    tr("طلبات الانضمام (${pendingTeachers.size})", "Join Requests (${pendingTeachers.size})"),
                    tr("المعلمون (${allTeachers.size})", "Teachers (${allTeachers.size})"),
                    tr("المواعيد (${allSchedules.size})", "Schedules (${allSchedules.size})"),
                    tr("المستخدمون (${allStudents.size})", "Users (${allStudents.size})")
                )
                if (currentAdmin.canManageAdmins) {
                    tabTitles.add(tr("المديرون (${allAdmins.size})", "Admins (${allAdmins.size})"))
                }
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (selectedTab in 0..3) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(tr("بحث بالاسم، رقم التليفون، أو اليوزر...", "Search by name, phone or username...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkPurple) },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            when (selectedTab) {
                0 -> {
                    if (filteredPendingTeachers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(tr("لا توجد طلبات انضمام جديدة معلقة", "No pending join requests"), color = ElkhetaTextGray)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredPendingTeachers) { teacher ->
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("${teacher.fullName} (${trValue(teacher.educationTrack)})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                                        Text("${tr("المادة:", "Subject:")} ${trValue(teacher.subject)}", fontSize = 14.sp)
                                        val isArabic = LocalIsArabic.current
                                        Text("${tr("المراحل:", "Stages:")} ${teacher.stages.joinToString { trValue(it, isArabic) }}", fontSize = 13.sp, color = ElkhetaTextGray)
                                        Text("${tr("المحافظة:", "Gov:")} ${trValue(teacher.governorate)} - ${trValue(teacher.area)}", fontSize = 13.sp, color = ElkhetaTextGray)
                                        Text("${tr("التليفون:", "Phone:")} ${teacher.phone} | ${tr("اليوزر:", "User:")} ${teacher.username}", fontSize = 13.sp, color = ElkhetaTextGray)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onApproveTeacher(teacher.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(tr("قبول وتفعيل", "Approve"), color = PureWhite)
                                            }
                                            if (canDelete) {
                                                OutlinedButton(
                                                    onClick = { onDeleteTeacher(teacher.id) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(tr("رفض وحذف", "Reject"))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredAllTeachers) { teacher ->
                            val context = LocalContext.current
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${teacher.fullName} (${trValue(teacher.educationTrack)})", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                                            Text("User: ${teacher.username} | ${trValue(teacher.subject)}", fontSize = 12.sp)
                                        }
                                        Row {
                                            IconButton(onClick = { teacherToEdit = teacher }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteTeacher(teacher.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("⭐ ${String.format(Locale.US, "%.1f", teacher.averageRating)}", fontWeight = FontWeight.Bold, color = GoldStar)
                                        Text("(${teacher.ratings.size} ${tr("تقييم", "reviews")})", fontSize = 12.sp, color = ElkhetaTextGray)
                                        Spacer(modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            val uri = Uri.parse("https://wa.me/2${teacher.phone}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }) {
                                            Text(tr("واتساب المعلم", "WhatsApp"), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = teacher.isApproved,
                                            onCheckedChange = { onUpdateTeacher(teacher.copy(isApproved = it)) }
                                        )
                                        Text(if (teacher.isApproved) tr("الحساب مفعّل ونشط", "Active") else tr("الحساب معطّل", "Disabled"), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val schedulesToShow = if (cleanQuery.isBlank()) allSchedules else allSchedules.filter { slot ->
                        val teacher = allTeachers.find { it.id == slot.teacherId }
                        slot.day.lowercase().contains(cleanQuery) || slot.time.lowercase().contains(cleanQuery) || (teacher?.fullName?.lowercase()?.contains(cleanQuery) == true)
                    }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(schedulesToShow) { slot ->
                            val teacher = allTeachers.find { it.id == slot.teacherId }
                            val confirmedCount = slot.enrollments.count { it.isConfirmed }
                            val pendingCount = slot.enrollments.size - confirmedCount
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${slot.day} - ${slot.time}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPurple)
                                            Text(
                                                text = "${tr("المعلم:", "Teacher:")} ${teacher?.fullName ?: tr("غير معروف", "Unknown")}",
                                                fontSize = 13.sp,
                                                color = ElkhetaTextGray
                                            )
                                            Text(
                                                text = "${slot.price} ج.م | ${if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")}",
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = tr("الطلاب: $confirmedCount مؤكد - $pendingCount بانتظار التأكيد", "Students: $confirmedCount confirmed - $pendingCount pending"),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pendingCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { scheduleToEdit = slot }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteSchedule(slot.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                            }
                                        }
                                    }
                                    if (slot.enrollments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(tr("قائمة الحجوزات:", "Enrollments:"), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkPurple)
                                        slot.enrollments.forEach { enrollment ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("• ${enrollment.studentName} (${enrollment.studentPhone})", fontSize = 12.sp)
                                                    if (enrollment.parentName.isNotBlank()) {
                                                        Text("  ${tr("ولي الأمر:", "Parent:")} ${enrollment.parentName} (${enrollment.parentPhone})", fontSize = 11.sp, color = ElkhetaTextGray)
                                                    }
                                                }
                                                if (!enrollment.isConfirmed) {
                                                    Button(
                                                        onClick = { onConfirmStudentBooking(slot, enrollment.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(tr("تأكيد", "Confirm"), fontSize = 11.sp, color = PureWhite)
                                                    }
                                                } else {
                                                    Text(tr("✓ مؤكد", "✓ Confirmed"), fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val shownParents = filteredAllStudents.filter { it.isParent }
                    val orphans = filteredAllStudents.filter { !it.isParent && it.parentId.isBlank() }
                    fun childrenOf(p: StudentProfile, list: List<StudentProfile>): List<StudentProfile> =
                        list.filter { !it.isParent && (it.parentId == p.id || (p.linkedStudentUsername.isNotBlank() && it.username == p.linkedStudentUsername)) }

                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shownParents) { parentSt ->
                            val kids = childrenOf(parentSt, allStudents)
                            val expanded = parentSt.id in expandedParents
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${parentSt.name} (${tr("ولي أمر", "Parent")})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPurple)
                                            Text("User: ${parentSt.username} | Phone: ${parentSt.phone}", fontSize = 12.sp)
                                            Text("${trValue(parentSt.governorate)} · ${trValue(parentSt.area)}", fontSize = 12.sp, color = ElkhetaTextGray)
                                            if (!parentSt.canAddStudentApproved) {
                                                Button(
                                                    onClick = { onUpdateStudent(parentSt.copy(canAddStudentApproved = true)) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    modifier = Modifier.padding(top = 6.dp)
                                                ) {
                                                    Text(tr("تفعيل إضافة طالب جديد", "Approve Adding Student"), color = PureWhite, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { studentToEdit = parentSt }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteStudent(parentSt.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tr("الأبناء (${kids.size}) ${if (expanded) "▴" else "▾"}", "Children (${kids.size}) ${if (expanded) "▴" else "▾"}"),
                                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0369A1),
                                        modifier = Modifier.clickable { expandedParents = if (expanded) expandedParents - parentSt.id else expandedParents + parentSt.id }.padding(vertical = 4.dp)
                                    )
                                    if (expanded) {
                                        kids.forEach { kid ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${kid.name} (${tr("طالب", "Student")})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ElkhetaTextDark)
                                                    Text("User: ${kid.username} | Phone: ${kid.phone}", fontSize = 11.sp)
                                                    Text("${trValue(kid.stage)} · ${trValue(kid.educationTrack)}", fontSize = 11.sp, color = ElkhetaTextGray)
                                                }
                                                Row {
                                                    IconButton(onClick = { studentToEdit = kid }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                                                    if (canDelete) {
                                                        IconButton(onClick = { onDeleteStudent(kid.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (orphans.isNotEmpty()) {
                            item { Text(tr("طلاب بدون ولي أمر", "Students without a parent"), fontWeight = FontWeight.ExtraBold, color = ElkhetaTextGray) }
                            items(orphans) { student ->
                                AppCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${student.name} (${tr("طالب", "Student")})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPurple)
                                            Text("User: ${student.username} | Phone: ${student.phone}", fontSize = 12.sp)
                                        }
                                        Row {
                                            IconButton(onClick = { studentToEdit = student }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteStudent(student.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    if (currentAdmin.canManageAdmins) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Button(onClick = { showAddAdminDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)) {
                                Text(tr("+ إضافة يوزر Admin جديد", "+ Add New Admin"), color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(allAdmins) { adminItem ->
                                    AppCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Admin: ${adminItem.username}", fontWeight = FontWeight.Bold, color = DarkPurple)
                                                Row {
                                                    if (!adminItem.username.equals("M1", true) && canDelete) {
                                                        IconButton(onClick = { onDeleteAdmin(adminItem.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                                    }
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = adminItem.canDeleteAccounts,
                                                    enabled = !adminItem.username.equals("M1", true),
                                                    onCheckedChange = { onSaveAdmin(adminItem.copy(canDeleteAccounts = it)) }
                                                )
                                                Text(tr("صلاحية حذف الحسابات والمواعيد", "Can Delete Accounts"), fontSize = 12.sp)
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

    teacherToEdit?.let { teacher ->
        EditTeacherDialog(teacher = teacher, onDismiss = { teacherToEdit = null }, onSave = { onUpdateTeacher(it); teacherToEdit = null })
    }
    scheduleToEdit?.let { slot ->
        EditScheduleAdminDialog(slot = slot, onDismiss = { scheduleToEdit = null }, onSave = { onUpdateSchedule(it); scheduleToEdit = null })
    }
    studentToEdit?.let { st ->
        EditStudentDialog(
            student = st, onDismiss = { studentToEdit = null }, onSave = { onUpdateStudent(it); studentToEdit = null },
            isAdmin = true, checkUsernameTaken = checkUsernameTaken
        )
    }
    if (showTermDialog && appState != null) {
        TermSettingsDialog(current = appState.termSettings, onDismiss = { showTermDialog = false }, onSave = { appState.saveTermSettings(it); showTermDialog = false })
    }
    if (showAddAdminDialog) {
        AddAdminDialog(onDismiss = { showAddAdminDialog = false }, onCreate = onCreateAdmin)
    }
}
// ==================== 12. نوافذ التعديل والإدارة ====================
@Composable
fun EditTeacherDialog(teacher: TeacherProfile, onDismiss: () -> Unit, onSave: (TeacherProfile) -> Unit) {
    var firstName by remember { mutableStateOf(teacher.firstName) }
    var secondName by remember { mutableStateOf(teacher.secondName) }
    var phone by remember { mutableStateOf(teacher.phone) }
    var selectedTrack by remember { mutableStateOf(teacher.educationTrack.ifEmpty { educationTracks.first() }) }
    val trackSubjects = getSubjectsByTrack(selectedTrack)
    val stageFiltered = trackSubjects.filter { s -> teacher.stages.isEmpty() || teacher.stages.any { st -> s in getSubjectsFor(selectedTrack, st) } }.ifEmpty { trackSubjects }
    val availableSubjects = if (teacher.subject in trackSubjects && teacher.subject !in stageFiltered) stageFiltered + teacher.subject else stageFiltered
    var subject by remember(selectedTrack) {
        mutableStateOf(if (availableSubjects.contains(teacher.subject)) teacher.subject else availableSubjects.first())
    }
    var adminRatingStars by remember { mutableIntStateOf(teacher.averageRating.toInt().coerceIn(1, 5)) }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("تعديل المعلم (${teacher.username})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= adminRatingStars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null, tint = GoldStar,
                            modifier = Modifier.size(30.dp).clickable { adminRatingStars = i }
                        )
                    }
                }
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("الاسم الأول") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = secondName, onValueChange = { secondName = it }, label = { Text("الاسم الثاني") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم التليفون") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown("نوع الدراسة", educationTracks, selectedTrack) { selectedTrack = it }
                Spacer(modifier = Modifier.height(6.dp))
                CustomDropdown("المادة", availableSubjects, subject) { subject = it }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = {
                            onSave(teacher.copy(firstName = firstName.trim(), secondName = secondName.trim(), phone = phone.trim(), educationTrack = selectedTrack, subject = subject, ratings = listOf(adminRatingStars)))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                    ) { Text("حفظ", color = PureWhite) }
                }
            }
        }
    }
}

@Composable
fun EditScheduleAdminDialog(slot: ScheduleSlot, onDismiss: () -> Unit, onSave: (ScheduleSlot) -> Unit) {
    var selectedDay by remember { mutableStateOf(slot.day.ifEmpty { daysOfWeek[0] }) }
    var time by remember { mutableStateOf(slot.time) }
    var price by remember { mutableStateOf(slot.price) }
    var isOnline by remember { mutableStateOf(slot.isOnline) }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("تعديل الموعد", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(10.dp))
                CustomDropdown("اليوم", daysOfWeek, selectedDay) { selectedDay = it }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("الساعة") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("السعر") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isOnline, onCheckedChange = { isOnline = it })
                    Text("حصة أونلاين (Online)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(onClick = { onSave(slot.copy(day = selectedDay, time = time, price = price, isOnline = isOnline)) }, colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)) {
                        Text("حفظ", color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun EditStudentDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
    onSave: (StudentProfile) -> Unit,
    isAdmin: Boolean = false,
    checkUsernameTaken: ((String) -> Boolean)? = null
) {
    var name by remember { mutableStateOf(student.name) }
    var phone by remember { mutableStateOf(student.phone) }
    var username by remember { mutableStateOf(student.username) }
    var governorate by remember { mutableStateOf(student.governorate) }
    var area by remember { mutableStateOf(student.area) }
    var track by remember { mutableStateOf(student.educationTrack.ifEmpty { educationTracks.first() }) }
    var stage by remember { mutableStateOf(student.stage.ifEmpty { educationalStages.first() }) }
    var errorMsg by remember { mutableStateOf("") }
    val takenMsg = tr("اسم المستخدم مسجل بالفعل", "Username already taken")
    val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")
    val emptyMsg = tr("الاسم واسم المستخدم مطلوبان", "Name and username are required")

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("تعديل حساب (${student.username})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(8.dp))
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم (لا يمكن تغييره)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = false)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("التليفون") }, modifier = Modifier.fillMaxWidth())
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDropdown("المحافظة", governoratesWithAreas.keys.toList(), governorate) {
                        governorate = it
                        area = governoratesWithAreas[it]?.firstOrNull() ?: ""
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDropdown("المنطقة", governoratesWithAreas[governorate] ?: emptyList(), area) { area = it }
                    if (!student.isParent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomDropdown("مسار الدراسة", educationTracks, track) { track = it }
                        Spacer(modifier = Modifier.height(8.dp))
                        CustomDropdown("المرحلة", educationalStages, stage) { stage = it }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = {
                            val newUser = username.trim()
                            when {
                                name.isBlank() || newUser.isBlank() -> errorMsg = emptyMsg
                                isAdmin && newUser != student.username && newUser.startsWith("*") -> errorMsg = reservedMsg
                                isAdmin && newUser != student.username && (checkUsernameTaken?.invoke(newUser) == true) -> errorMsg = takenMsg
                                else -> {
                                    var updated = student.copy(name = name, phone = phone)
                                    if (isAdmin) {
                                        updated = updated.copy(username = newUser, governorate = governorate, area = area)
                                        if (!student.isParent) updated = updated.copy(educationTrack = track, stage = stage)
                                    }
                                    onSave(updated)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                    ) {
                        Text("حفظ", color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun TermSettingsDialog(current: TermSettings, onDismiss: () -> Unit, onSave: (TermSettings) -> Unit) {
    val context = LocalContext.current
    var ts by remember { mutableStateOf(current) }
    var errorMsg by remember { mutableStateOf("") }
    val invalidMsg = tr("تأكد أن التواريخ مرتبة: بداية الترم الأول < نهايته < بداية الثاني < نهايته", "Dates must be in order: T1 start < T1 end < T2 start < T2 end")

    fun pick(key: String, onPicked: (String) -> Unit) {
        val parts = key.split("-").mapNotNull { it.toIntOrNull() }
        val y = parts.getOrElse(0) { 2026 }
        val m = parts.getOrElse(1) { 9 } - 1
        val d = parts.getOrElse(2) { 1 }
        android.app.DatePickerDialog(context, { _, yy, mm, dd ->
            onPicked(String.format(Locale.US, "%04d-%02d-%02d", yy, mm + 1, dd))
        }, y, m, d).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(tr("الترم الدراسي", "Academic Terms"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Text(tr("للأدمن فقط. التعديل يؤثر على التوزيع الجديد فقط.", "Admin only. Changes affect new distributions only."), fontSize = 12.sp, color = ElkhetaTextGray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                if (errorMsg.isNotEmpty()) Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(tr("الترم الأول", "Term 1"), fontWeight = FontWeight.ExtraBold, color = Color(0xFF0369A1), modifier = Modifier.padding(top = 6.dp))
                DateFieldRow(tr("البداية", "Start"), ts.t1Start) { pick(ts.t1Start) { v -> ts = ts.copy(t1Start = v); errorMsg = "" } }
                DateFieldRow(tr("النهاية", "End"), ts.t1End) { pick(ts.t1End) { v -> ts = ts.copy(t1End = v); errorMsg = "" } }
                Text(tr("الترم الثاني", "Term 2"), fontWeight = FontWeight.ExtraBold, color = Color(0xFF0369A1), modifier = Modifier.padding(top = 10.dp))
                DateFieldRow(tr("البداية", "Start"), ts.t2Start) { pick(ts.t2Start) { v -> ts = ts.copy(t2Start = v); errorMsg = "" } }
                DateFieldRow(tr("النهاية", "End"), ts.t2End) { pick(ts.t2End) { v -> ts = ts.copy(t2End = v); errorMsg = "" } }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
                    Button(
                        onClick = {
                            if (ts.t1Start < ts.t1End && ts.t1End < ts.t2Start && ts.t2Start < ts.t2End) onSave(ts) else errorMsg = invalidMsg
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                    ) { Text(tr("حفظ", "Save"), color = PureWhite) }
                }
            }
        }
    }
}

@Composable
private fun DateFieldRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp), color = ElkhetaInputBg, border = BorderStroke(1.dp, ElkhetaInputBorder),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = ElkhetaTextGray)
            Text("$value  ▾", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextDark)
        }
    }
}

@Composable
fun AddAdminDialog(onDismiss: () -> Unit, onCreate: (AdminAccount, String, (AuthError) -> Unit) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var canDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val authText = rememberAuthErrorTexts()
    val fieldsMsg = tr("اسم المستخدم وكلمة المرور مطلوبان", "Username and password are required")

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("إضافة Admin جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it; errorMsg = "" }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it; errorMsg = "" }, label = { Text("كلمة المرور (6 أحرف على الأقل)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canDelete, onCheckedChange = { canDelete = it })
                    Text("صلاحية الحذف الكامل")
                }
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = {
                            val user = username.trim()
                            if (user.isBlank() || password.isBlank()) {
                                errorMsg = fieldsMsg
                            } else if (password.length < AccountAuth.MIN_PASSWORD_LENGTH) {
                                errorMsg = authText(AuthError.WEAK_PASSWORD)
                            } else if (!busy) {
                                busy = true
                                errorMsg = ""
                                onCreate(AdminAccount(username = user, canManageUsers = true, canDeleteAccounts = canDelete), password) { err ->
                                    busy = false
                                    if (err == AuthError.NONE) onDismiss() else errorMsg = authText(err)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                    ) { Text("إضافة", color = PureWhite) }
                }
            }
        }
    }
}
