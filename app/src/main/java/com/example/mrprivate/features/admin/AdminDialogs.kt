@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.admin

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mrprivate.AdminAccount
import com.example.mrprivate.ScheduleSlot
import com.example.mrprivate.StudentProfile
import com.example.mrprivate.TeacherProfile
import com.example.mrprivate.TermSettings
import com.example.mrprivate.daysOfWeek
import com.example.mrprivate.educationTracks
import com.example.mrprivate.educationalStages
import com.example.mrprivate.features.auth.AccountAuth
import com.example.mrprivate.features.auth.AuthError
import com.example.mrprivate.features.auth.rememberAuthErrorTexts
import com.example.mrprivate.getSubjectsByTrack
import com.example.mrprivate.getSubjectsFor
import com.example.mrprivate.governoratesWithAreas
import com.example.mrprivate.tr
import com.example.mrprivate.ui.components.*
import java.util.Locale

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
internal fun DateFieldRow(label: String, value: String, onClick: () -> Unit) {
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
