@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.ScheduleSlot
import com.example.mrprivate.data.model.StudentEnrollment
import com.example.mrprivate.ui.components.*

@Composable
fun TeacherSchedulesAndStudentsTab(
    schedules: List<ScheduleSlot>,
    isInteractiveAllowed: Boolean,
    appState: MrPrivateState?,
    context: Context,
    onConfirmStudent: (ScheduleSlot, String) -> Unit,
    onEditSlot: (ScheduleSlot) -> Unit,
    onSlotForExtraSession: (ScheduleSlot) -> Unit,
    onStudentToComplete: (Pair<ScheduleSlot, StudentEnrollment>) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("المواعيد المتاحة والطلاب المسجلين (${schedules.size})", "Schedules & Enrolled Students (${schedules.size})"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TeacherPurpleDark
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        schedules.forEach { slot ->
            val typeText = if (slot.isPrivate) tr("برايفت", "Private") else tr("جروب (${slot.enrollments.size}/${slot.maxStudents})", "Group (${slot.enrollments.size}/${slot.maxStudents})")
            val locText = if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")

            AppCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).shadow(4.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${trValue(slot.day)} - ${slot.time}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TeacherPurpleDark)
                                if (slot.isExtraSession) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = TeacherPurpleSoft, shape = RoundedCornerShape(6.dp)) {
                                        Text("Extra Session", color = TeacherPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            if (slot.stage.isNotBlank() || slot.grade.isNotBlank()) {
                                Text("${trValue(slot.stage)} • ${slot.grade}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = TeacherPurple, shape = RoundedCornerShape(8.dp)) {
                                Text("$typeText - $locText", color = PureWhite, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            if (isInteractiveAllowed) {
                                IconButton(onClick = { onEditSlot(slot) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الموعد", tint = TeacherPurple)
                                }
                                val cancelNotifMsg = tr("تم إلغاء الحصة وإرسال إشعار للطلاب وأولياء الأمور", "Session cancelled & notified")
                                IconButton(onClick = {
                                    appState?.cancelScheduleByTeacher(slot)
                                    Toast.makeText(context, cancelNotifMsg, Toast.LENGTH_LONG).show()
                                }) {
                                    Icon(Icons.Default.EventBusy, contentDescription = "إلغاء الحصة", tint = Color(0xFFB91C1C))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (slot.isExtraSession)
                            tr("السعر: ${slot.price} جنيه | ⭐ حصة إضافية (Extra Session)", "Price: ${slot.price} EGP | Extra Session")
                        else
                            tr("السعر: ${slot.price} جنيه | 🔄 يتكرر على الترم الدراسي فقط (${slot.recurringDates.size.coerceAtLeast(1)} أسبوع)", "Price: ${slot.price} EGP | Term Recurring (${slot.recurringDates.size.coerceAtLeast(1)} Weeks)"),
                        fontSize = 12.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold
                    )

                    // زر إضافة حصة إضافية (Extra Session) لنفس المجموعة
                    if (isInteractiveAllowed && !slot.isExtraSession) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onSlotForExtraSession(slot) },
                            border = BorderStroke(1.dp, TeacherPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MoreTime, contentDescription = null, tint = TeacherPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("إضافة حصة إضافية (Extra Session) لنفس المجموعة", "Add Extra Session for Group"), color = TeacherPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (slot.enrollments.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        slot.enrollments.forEach { enroll ->
                            val stObj = appState?.students?.find { it.id == enroll.studentId }
                            val parentObj = appState?.students?.find { it.id == stObj?.parentId }
                            val pName = enroll.parentName.ifBlank { parentObj?.name ?: "ولي أمر الطالب" }
                            val pPhone = enroll.parentPhone.ifBlank { parentObj?.phone ?: enroll.studentPhone }
                            val sGrade = enroll.studentGrade.ifBlank { stObj?.grade?.ifBlank { slot.grade.ifBlank { stObj.stage } } ?: slot.grade }

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ElkhetaInputBg, RoundedCornerShape(10.dp)).padding(10.dp)
                            ) {
                                Text(tr("ولي الأمر: $pName  |  📞 $pPhone", "Parent: $pName | 📞 $pPhone"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TeacherPurpleDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(tr("↳ الطالب: ${enroll.studentName}  |  الصف: ${sGrade.ifBlank { "غير محدد" }}", "↳ Student: ${enroll.studentName} | Grade: $sGrade"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                                if (enroll.parentExcuseNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFF97316))) {
                                        Text(
                                            text = tr("✋ اعتذار من ولي الأمر عن الحصة: ${enroll.parentExcuseNote}", "✋ Parent Excuse: ${enroll.parentExcuseNote}"),
                                            color = Color(0xFFC2410C),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!enroll.isConfirmed) {
                                        Button(
                                            onClick = { onConfirmStudent(slot, enroll.id) },
                                            enabled = isInteractiveAllowed,
                                            colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
                                        ) {
                                            Text(tr("تأكيد وقبول", "Confirm"), color = PureWhite, fontSize = 11.sp)
                                        }
                                    } else if (!enroll.isCompleted) {
                                        Button(
                                            onClick = { onStudentToComplete(slot to enroll) },
                                            enabled = isInteractiveAllowed,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                        ) {
                                            Text(tr("إنهاء الحصة (حضور/غياب)", "Complete & Attendance"), color = PureWhite, fontSize = 11.sp)
                                        }
                                    } else {
                                        val statusTxt = if (enroll.attended) tr("حضر ✓ (${enroll.teacherRatingForStudent}★)", "Present ✓") else tr("لم يحضر (غائب) ✗", "Absent ✗")
                                        val statusClr = if (enroll.attended) Color(0xFF2E7D32) else Color(0xFFB91C1C)
                                        Text(statusTxt, color = statusClr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    if (isInteractiveAllowed) {
                                        IconButton(
                                            onClick = {
                                                appState?.removeStudentFromSlot(slot, enroll.id)
                                                Toast.makeText(context, "تم حذف الطالب من المجموعة", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الطالب", tint = Color(0xFFB91C1C))
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
