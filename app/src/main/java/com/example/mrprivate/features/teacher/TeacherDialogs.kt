@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.QuizExam
import com.example.mrprivate.data.model.ScheduleSlot
import com.example.mrprivate.data.model.TeacherProfile
import com.example.mrprivate.ui.components.*

// ==================== 11. شاشة انتظار قبول المعلم ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPendingApprovalScreen(teacher: TeacherProfile, appState: MrPrivateState? = null, onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("حالة حساب المعلم", "Teacher Account Status"), color = PureWhite, fontWeight = FontWeight.Bold) },
                actions = {
                    LanguageSwitcherButton(appState = appState)
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "خروج", tint = PureWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = DarkPurple, modifier = Modifier.size(90.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = tr("مرحباً بك ${teacher.fullName}", "Welcome ${teacher.fullName}"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = LightPurple), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("حسابك قيد المراجعة والاعتماد من قِبل إدارة المنصة (Admin)", "Your account is pending approval by the Platform Admin"),
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkPurple, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            WhatsAppSupportButton()
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text(tr("تسجيل الخروج", "Logout"), color = DarkPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== 13. لوحة تحكم المعلم (مع التكرار على الترم والحضور والغياب وتعديل المواعيد) ====================
@Composable
fun CompleteSessionDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, String) -> Unit
) {
    var attended by remember { mutableStateOf(true) }
    var stars by remember { mutableIntStateOf(5) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(tr("إنهاء الحصة وتقييم الطالب ($studentName)", "Complete Session ($studentName)"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(10.dp))
                Text(tr("تحديد حضور الطالب في الحصة:", "Student Attendance:"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (attended) Color(0xFF2E7D32) else ElkhetaInputBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (attended) Color(0xFF2E7D32) else ElkhetaInputBorder),
                        modifier = Modifier.weight(1f).clickable { attended = true }
                    ) {
                        Text(tr("حضر ✓", "Attended ✓"), color = if (attended) PureWhite else ElkhetaTextDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Surface(
                        color = if (!attended) Color(0xFFB91C1C) else ElkhetaInputBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!attended) Color(0xFFB91C1C) else ElkhetaInputBorder),
                        modifier = Modifier.weight(1f).clickable { attended = false }
                    ) {
                        Text(tr("لم يحضر (غائب) ✗", "Absent ✗"), color = if (!attended) PureWhite else ElkhetaTextDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (attended) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null, tint = GoldStar,
                                modifier = Modifier.size(32.dp).clickable { stars = i }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("ملاحظة خاصة بولي الأمر فقط (سرية)", "Parent Private Note")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
                    Button(onClick = { onSave(attended, stars, note) }, colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)) {
                        Text(tr("حفظ وإنهاء", "Save"), color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    teacherId: String,
    initialSlot: ScheduleSlot? = null,
    appState: MrPrivateState? = null,
    onDismiss: () -> Unit,
    onSave: (ScheduleSlot) -> Unit
) {
    var selectedStage by remember { mutableStateOf(initialSlot?.stage ?: "") }
    val availableGrades = stageGradesMap[selectedStage] ?: emptyList()
    var selectedGrade by remember(selectedStage) {
        mutableStateOf(if (initialSlot?.stage == selectedStage) initialSlot.grade else "")
    }
    var selectedDay by remember { mutableStateOf(initialSlot?.day ?: "") }
    var time by remember { mutableStateOf(initialSlot?.time ?: "") }
    var isPrivate by remember { mutableStateOf(initialSlot?.isPrivate ?: false) }
    var isOnline by remember { mutableStateOf(initialSlot?.isOnline ?: false) }
    var maxStudents by remember { mutableStateOf(initialSlot?.maxStudents?.toString() ?: "") }
    var price by remember { mutableStateOf(initialSlot?.price ?: "") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (initialSlot == null) tr("إضافة موعد حصة (على الترم الدراسي)", "Add Term Schedule") else tr("تعديل موعد الحصة", "Edit Schedule"),
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DarkPurple
                )
                Text(tr("جميع الحقول إجبارية *", "All fields are mandatory *"), fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                CustomDropdown(tr("المرحلة التعليمية *", "Stage *"), educationalStages, selectedStage) {
                    selectedStage = it
                    selectedGrade = ""
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                CustomDropdown(tr("الصف الدراسي *", "Grade *"), availableGrades, selectedGrade) {
                    selectedGrade = it
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                CustomDropdown(tr("اليوم *", "Day *"), daysOfWeek, selectedDay) {
                    selectedDay = it
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it; errorMsg = "" },
                    label = { Text(tr("الساعة * (مثال: 05:00 مساءً)", "Time *")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isPrivate, onClick = { isPrivate = false; if (maxStudents == "1") maxStudents = "" })
                    Text(tr("جروب", "Group"))
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = isPrivate, onClick = { isPrivate = true; maxStudents = "1" })
                    Text(tr("برايفت", "Private"))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isOnline, onClick = { isOnline = false })
                    Text(tr("حضوري", "Offline"))
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = isOnline, onClick = { isOnline = true })
                    Text(tr("أونلاين", "Online"))
                }

                if (!isPrivate) {
                    OutlinedTextField(
                        value = maxStudents,
                        onValueChange = { maxStudents = it.filter { c -> c.isDigit() }; errorMsg = "" },
                        label = { Text(tr("الحد الأقصى للطلاب *", "Max Students *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() }; errorMsg = "" },
                    label = { Text(tr("السعر (بالجنيه) *", "Price (EGP) *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Elkheta3DButton(
                    text = if (initialSlot == null) tr("حفظ وتكرار على الترم الدراسي", "Save Term Schedule") else tr("حفظ تعديل الموعد", "Save Changes"),
                    onClick = {
                        val parsedMax = if (isPrivate) 1 else maxStudents.toIntOrNull()
                        if (selectedStage.isBlank() || selectedGrade.isBlank() || selectedDay.isBlank() || time.isBlank() || price.isBlank() || parsedMax == null || parsedMax <= 0) {
                            errorMsg = "يرجى ملء جميع الحقول الإجبارية أولاً!"
                        } else {
                            val slotObj = (initialSlot ?: ScheduleSlot(teacherId = teacherId)).copy(
                                stage = selectedStage,
                                grade = selectedGrade,
                                day = selectedDay,
                                time = time.trim(),
                                maxStudents = parsedMax,
                                price = price.trim(),
                                isPrivate = isPrivate,
                                isOnline = isOnline,
                                isRecurringYearly = true,
                                academicYearWeeks = 14
                            )
                            val conflict = if (initialSlot != null && appState != null) {
                                appState.checkGroupConflictForSlotEdit(initialSlot, slotObj.day, slotObj.time)
                            } else null
                            if (conflict != null) {
                                errorMsg = conflict
                            } else {
                                onSave(slotObj)
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
internal fun ReassignQuizDialog(
    sourceQuiz: QuizExam,
    schedules: List<ScheduleSlot>,
    appState: MrPrivateState?,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedSlotIds by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("تعيين الاختبار: ${sourceQuiz.title}", "Assign Quiz: ${sourceQuiz.title}"), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(tr("اختر المجموعات المراد إرسال الاختبار إليها:", "Select groups to receive this quiz:"), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                schedules.forEach { slot ->
                    val label = "${slot.grade} (${slot.day} - ${slot.time})"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSlotIds = if (selectedSlotIds.contains(slot.id)) selectedSlotIds - slot.id else selectedSlotIds + slot.id
                        }
                    ) {
                        Checkbox(
                            checked = selectedSlotIds.contains(slot.id),
                            onCheckedChange = { checked ->
                                selectedSlotIds = if (checked) selectedSlotIds + slot.id else selectedSlotIds - slot.id
                            },
                            colors = CheckboxDefaults.colors(checkedColor = TeacherPurple)
                        )
                        Text(label, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            val sendQuizSuccessMsg = tr("تم إرسال الاختبار إلى %d مجموعة بنجاح!", "Quiz assigned to %d groups!")
            Button(
                onClick = {
                    val targets = schedules.filter { selectedSlotIds.contains(it.id) }
                    if (targets.isNotEmpty()) {
                        appState?.reassignSavedQuizToGroups(sourceQuiz, targets)
                        val msg = sendQuizSuccessMsg.replace("%d", targets.size.toString())
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
            ) {
                Text(tr("إرسال للمجموعات المحددة", "Send to Selected"), color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
        }
    )
}

@Composable
internal fun ExtraSessionDialog(
    baseSlot: ScheduleSlot,
    appState: MrPrivateState?,
    context: Context,
    onDismiss: () -> Unit
) {
    var extraDay by remember { mutableStateOf(baseSlot.day) }
    var extraTime by remember { mutableStateOf("") }
    var extraError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("إضافة حصة إضافية (Extra Session)", "Add Extra Session"), fontWeight = FontWeight.Bold, color = TeacherPurpleDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("المجموعة: ${baseSlot.stage} - ${baseSlot.grade}", "Group: ${baseSlot.stage} - ${baseSlot.grade}"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                CustomDropdown(tr("اليوم *", "Day *"), daysOfWeek, extraDay) {
                    extraDay = it
                    extraError = ""
                }
                OutlinedTextField(
                    value = extraTime,
                    onValueChange = { extraTime = it; extraError = "" },
                    label = { Text(tr("الساعة * (مثال: 07:00 مساءً)", "Time *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (extraError.isNotEmpty()) {
                    Text(extraError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            val extraSessionSuccessMsg = tr("تمت إضافة الحصة الإضافية وإرسال إشعار للطلاب", "Extra session added & notified")
            Button(
                onClick = {
                    if (extraDay.isBlank() || extraTime.isBlank()) {
                        extraError = "يرجى إدخال اليوم والساعة للحصة الإضافية"
                    } else {
                        val err = appState?.addExtraSessionForGroup(baseSlot, extraDay, extraTime)
                        if (err != null) {
                            extraError = err
                        } else {
                            Toast.makeText(context, extraSessionSuccessMsg, Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
            ) {
                Text(tr("إضافة وإشعار الطلاب", "Add & Notify"), color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
        }
    )
}

@Composable
internal fun TeacherUltimateSupportDialog(
    teacher: TeacherProfile,
    isArabic: Boolean,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TeacherPurple) },
        title = { Text(tr("ميزة مخصصة لباقة اشتراك شامل", "Ultimate Package Feature"), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                tr(
                    "ميزة إنشاء الاختبارات وتصحيحها بالذكاء الاصطناعي مفعلة لمشتركي باقة (اشتراك شامل). يرجى طلب التفعيل من الدعم الفني.",
                    "AI Quiz Creation is exclusively enabled for Ultimate subscribers. Please request activation from Support."
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    openTeacherRenewalWhatsApp(context, teacher.fullName, "ULTIMATE", isArabic)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
            ) {
                Text(tr("طلب التفعيل من الدعم", "Request Activation"), color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
        }
    )
}
