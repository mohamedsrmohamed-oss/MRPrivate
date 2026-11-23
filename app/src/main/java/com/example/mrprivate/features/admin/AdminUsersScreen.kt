@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.data.model.*
import com.example.mrprivate.ui.components.*

internal fun adminChildrenOf(parent: StudentProfile, all: List<StudentProfile>): List<StudentProfile> =
    all.filter {
        !it.isParent && (
            it.parentId == parent.id ||
            (parent.linkedStudentUsername.isNotBlank() && it.username.equals(parent.linkedStudentUsername.trim(), true)) ||
            parent.registeredStudentIds.contains(it.id)
        )
    }

@Composable
fun AdminPendingTeachersTab(
    filteredPendingTeachers: List<TeacherProfile>,
    canDelete: Boolean,
    onApproveTeacher: (String) -> Unit,
    onUpdateTeacher: (TeacherProfile) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onEditTeacher: (TeacherProfile) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filteredPendingTeachers, key = { it.id }) { teacher ->
            val context = LocalContext.current
            AppCard(modifier = Modifier.fillMaxWidth().border(1.dp, DarkPurple, RoundedCornerShape(12.dp))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${teacher.fullName} (${trValue(teacher.educationTrack)})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkPurple)
                    Text("اليوزر: ${teacher.username} | المادة: ${trValue(teacher.subject)} | المنطقة: ${trValue(teacher.area)}", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(LightPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("تليفون المعلم: ${teacher.phone}", "Phone: ${teacher.phone}"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkPurple)
                        Button(
                            onClick = { openTeacherWhatsAppForAdmin(context, teacher.phone, teacher.fullName) },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(tr("تواصل واتساب", "WhatsApp"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val termMillis = 120L * 24 * 60 * 60 * 1000
                        Checkbox(
                            checked = teacher.effectiveIsPremium,
                            onCheckedChange = { checked ->
                                val now = System.currentTimeMillis()
                                onUpdateTeacher(
                                    teacher.copy(
                                        isPremium = checked,
                                        isUltimate = if (!checked) false else teacher.isUltimate,
                                        subscriptionActivatedAtMillis = if (checked) now else 0L,
                                        subscriptionExpiresAtMillis = if (checked) now + termMillis else 0L
                                    )
                                )
                            }
                        )
                        Text(tr("بريميوم (Premium)", "Premium"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = teacher.effectiveIsUltimate,
                            onCheckedChange = { checked ->
                                val now = System.currentTimeMillis()
                                onUpdateTeacher(
                                    teacher.copy(
                                        isUltimate = checked,
                                        isPremium = if (checked) true else teacher.isPremium,
                                        subscriptionActivatedAtMillis = if (checked) now else teacher.subscriptionActivatedAtMillis,
                                        subscriptionExpiresAtMillis = if (checked) now + termMillis else teacher.subscriptionExpiresAtMillis
                                    )
                                )
                            }
                        )
                        Text(tr("اشتراك شامل (Ultimate)", "Ultimate"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MrRedPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onApproveTeacher(teacher.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.weight(1f)) {
                            Text(tr("قبول ورفع البروفايل", "Approve"), color = PureWhite, fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = { onEditTeacher(teacher) }) { Text(tr("تعديل", "Edit"), color = DarkPurple) }
                        if (canDelete) {
                            IconButton(onClick = { onDeleteTeacher(teacher.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTeachersTab(
    filteredAllTeachers: List<TeacherProfile>,
    canDelete: Boolean,
    onUpdateTeacher: (TeacherProfile) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onEditTeacher: (TeacherProfile) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filteredAllTeachers, key = { it.id }) { teacher ->
            val context = LocalContext.current
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${teacher.fullName} (${trValue(teacher.educationTrack)})", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                            Text("User: ${teacher.username} | ${trValue(teacher.subject)}", fontSize = 12.sp)
                        }
                        Row {
                            IconButton(onClick = { onEditTeacher(teacher) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                            if (canDelete) {
                                IconButton(onClick = { onDeleteTeacher(teacher.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(LightPurple.copy(alpha = 0.35f), RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("تليفون: ${teacher.phone}", "Phone: ${teacher.phone}"), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkPurple)
                        Button(
                            onClick = { openTeacherWhatsAppForAdmin(context, teacher.phone, teacher.fullName) },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(tr("واتساب", "WhatsApp"), color = PureWhite, fontSize = 11.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val termMillis = 120L * 24 * 60 * 60 * 1000
                        Checkbox(
                            checked = teacher.effectiveIsPremium,
                            onCheckedChange = { checked ->
                                val now = System.currentTimeMillis()
                                onUpdateTeacher(
                                    teacher.copy(
                                        isPremium = checked,
                                        isUltimate = if (!checked) false else teacher.isUltimate,
                                        subscriptionActivatedAtMillis = if (checked) now else 0L,
                                        subscriptionExpiresAtMillis = if (checked) now + termMillis else 0L
                                    )
                                )
                            }
                        )
                        Text(tr("بريميوم (Premium)", "Premium"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                        Spacer(modifier = Modifier.width(4.dp))
                        Checkbox(
                            checked = teacher.effectiveIsUltimate,
                            onCheckedChange = { checked ->
                                val now = System.currentTimeMillis()
                                onUpdateTeacher(
                                    teacher.copy(
                                        isUltimate = checked,
                                        isPremium = if (checked) true else teacher.isPremium,
                                        subscriptionActivatedAtMillis = if (checked) now else teacher.subscriptionActivatedAtMillis,
                                        subscriptionExpiresAtMillis = if (checked) now + termMillis else teacher.subscriptionExpiresAtMillis
                                    )
                                )
                            }
                        )
                        Text(tr("اشتراك شامل", "Ultimate"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MrRedPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size, darkText = true)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSchedulesTab(
    allSchedules: List<ScheduleSlot>,
    allTeachers: List<TeacherProfile>,
    canDelete: Boolean,
    onDeleteSchedule: (String) -> Unit,
    onEditSchedule: (ScheduleSlot) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(allSchedules, key = { it.id }) { slot ->
            val slotTeacher = allTeachers.find { it.id == slot.teacherId }
            val typeText = if (slot.isPrivate) tr("برايفت", "Private") else tr("جروب (${slot.enrollments.size}/${slot.maxStudents})", "Group (${slot.enrollments.size}/${slot.maxStudents})")
            val locText = if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(slotTeacher?.fullName ?: "معلم", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkPurple)
                            Text("${trValue(slot.day)} - ${slot.time} ($typeText - $locText)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("السعر: ${slot.price} جنيه | تكرار سنوي (36 أسبوع)", fontSize = 12.sp, color = MediumPurple)
                        }
                        Row {
                            IconButton(onClick = { onEditSchedule(slot) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
                            if (canDelete) {
                                IconButton(onClick = { onDeleteSchedule(slot.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStudentsAndParentsTab(
    filteredAllStudents: List<StudentProfile>,
    allStudents: List<StudentProfile>,
    canDelete: Boolean,
    onUpdateStudent: (StudentProfile) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onEditStudent: (StudentProfile) -> Unit
) {
    var expandedParents by remember { mutableStateOf(setOf<String>()) }
    val matchedIds = filteredAllStudents.map { it.id }.toSet()
    val parentsAll = allStudents.filter { it.isParent }
    val shownParents = parentsAll.filter { p -> p.id in matchedIds || adminChildrenOf(p, allStudents).any { it.id in matchedIds } }
    val linkedChildIds = parentsAll.flatMap { p -> adminChildrenOf(p, allStudents).map { it.id } }.toSet()
    val orphans = filteredAllStudents.filter { !it.isParent && it.id !in linkedChildIds }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(shownParents, key = { it.id }) { parentSt ->
            val kids = adminChildrenOf(parentSt, allStudents)
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
                            IconButton(onClick = { onEditStudent(parentSt) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
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
                                    IconButton(onClick = { onEditStudent(kid) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
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
            items(orphans, key = { it.id }) { student ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${student.name} (${tr("طالب", "Student")})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkPurple)
                            Text("User: ${student.username} | Phone: ${student.phone}", fontSize = 12.sp)
                        }
                        Row {
                            IconButton(onClick = { onEditStudent(student) }) { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkPurple) }
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

@Composable
fun AdminAdminsTab(
    allAdmins: List<AdminAccount>,
    canDelete: Boolean,
    onSaveAdmin: (AdminAccount) -> Unit,
    onDeleteAdmin: (String) -> Unit,
    onOpenAddAdmin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onOpenAddAdmin, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)) {
            Text(tr("+ إضافة يوزر Admin جديد", "+ Add New Admin"), color = PureWhite, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(allAdmins, key = { it.id }) { adminItem ->
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
