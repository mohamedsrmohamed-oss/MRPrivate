@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import com.example.mrprivate.features.auth.AuthError
import com.example.mrprivate.ui.components.*

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
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showTermDialog by remember { mutableStateOf(false) }

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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(tr("البحث برقم الهاتف أو الاسم أو اليوزر...", "Search by phone, name, or username...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkPurple) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = MediumPurple, contentColor = PureWhite, edgePadding = 8.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(tr("طلبات (${filteredPendingTeachers.size})", "Pending (${filteredPendingTeachers.size})"), fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(tr("المعلمين (${filteredAllTeachers.size})", "Teachers (${filteredAllTeachers.size})"), fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(tr("المواعيد (${allSchedules.size})", "Schedules (${allSchedules.size})"), fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(tr("الطلاب (${filteredAllStudents.size})", "Students (${filteredAllStudents.size})"), fontWeight = FontWeight.Bold) })
                if (currentAdmin.canManageAdmins) {
                    Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text(tr("الأدمن (${allAdmins.size})", "Admins (${allAdmins.size})"), fontWeight = FontWeight.Bold) })
                }
            }

            when (selectedTab) {
                0 -> AdminPendingTeachersTab(
                    filteredPendingTeachers = filteredPendingTeachers,
                    canDelete = canDelete,
                    onApproveTeacher = onApproveTeacher,
                    onUpdateTeacher = onUpdateTeacher,
                    onDeleteTeacher = onDeleteTeacher,
                    onEditTeacher = { teacherToEdit = it }
                )
                1 -> AdminTeachersTab(
                    filteredAllTeachers = filteredAllTeachers,
                    canDelete = canDelete,
                    onUpdateTeacher = onUpdateTeacher,
                    onDeleteTeacher = onDeleteTeacher,
                    onEditTeacher = { teacherToEdit = it }
                )
                2 -> AdminSchedulesTab(
                    allSchedules = allSchedules,
                    allTeachers = allTeachers,
                    canDelete = canDelete,
                    onDeleteSchedule = onDeleteSchedule,
                    onEditSchedule = { scheduleToEdit = it }
                )
                3 -> AdminStudentsAndParentsTab(
                    filteredAllStudents = filteredAllStudents,
                    allStudents = allStudents,
                    canDelete = canDelete,
                    onUpdateStudent = onUpdateStudent,
                    onDeleteStudent = onDeleteStudent,
                    onEditStudent = { studentToEdit = it }
                )
                4 -> if (currentAdmin.canManageAdmins) {
                    AdminAdminsTab(
                        allAdmins = allAdmins,
                        canDelete = canDelete,
                        onSaveAdmin = onSaveAdmin,
                        onDeleteAdmin = onDeleteAdmin,
                        onOpenAddAdmin = { showAddAdminDialog = true }
                    )
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
