@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.QuizExam
import com.example.mrprivate.data.model.ScheduleSlot
import com.example.mrprivate.data.model.TeacherProfile
import com.example.mrprivate.ui.components.*

@Composable
fun TeacherManagementAndSubscriptionTab(
    teacher: TeacherProfile,
    isPremiumActive: Boolean,
    isUltimateActive: Boolean,
    isInteractiveAllowed: Boolean,
    appState: MrPrivateState?,
    context: Context,
    isArabic: Boolean,
    onCreateQuizClick: () -> Unit
) {
    Column {
        if (isPremiumActive) {
            val govAreas = (governoratesWithAreas[teacher.governorate] ?: emptyList()).filter { it != teacher.area }
            val noExtraLabel = "بدون منطقة إضافية"
            var extraArea1 by remember(teacher.extraAreas) { mutableStateOf(teacher.extraAreas.getOrNull(0) ?: noExtraLabel) }
            var extraArea2 by remember(teacher.extraAreas) { mutableStateOf(teacher.extraAreas.getOrNull(1) ?: noExtraLabel) }

            AppCard(
                modifier = Modifier.fillMaxWidth().border(1.5.dp, TeacherPurple, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = TeacherPurple)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("اختيار منطقتين إضافيتين داخل محافظة ${teacher.governorate}", "Select 2 Extra Areas in ${teacher.governorate}"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = TeacherPurpleDark
                        )
                    }
                    Text(
                        text = tr("المنطقة الأساسية: ${teacher.area}", "Primary Area: ${teacher.area}"),
                        fontSize = 12.sp,
                        color = ElkhetaTextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val extraAreasUpdatedMsg = tr("تم تحديث المناطق الإضافية", "Extra areas updated")
                    CustomDropdown(
                        label = tr("المنطقة الإضافية (1)", "Extra Area (1)"),
                        options = listOf(noExtraLabel) + govAreas,
                        selectedOption = extraArea1
                    ) { selected ->
                        extraArea1 = selected
                        val updatedExtras = listOfNotNull(
                            extraArea1.takeIf { it != noExtraLabel },
                            extraArea2.takeIf { it != noExtraLabel && it != extraArea1 }
                        )
                        appState?.updateTeacherExtraAreas(teacher, updatedExtras)
                        Toast.makeText(context, extraAreasUpdatedMsg, Toast.LENGTH_SHORT).show()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDropdown(
                        label = tr("المنطقة الإضافية (2)", "Extra Area (2)"),
                        options = listOf(noExtraLabel) + govAreas.filter { it != extraArea1 },
                        selectedOption = extraArea2
                    ) { selected ->
                        extraArea2 = selected
                        val updatedExtras = listOfNotNull(
                            extraArea1.takeIf { it != noExtraLabel },
                            extraArea2.takeIf { it != noExtraLabel && it != extraArea1 }
                        )
                        appState?.updateTeacherExtraAreas(teacher, updatedExtras)
                        Toast.makeText(context, extraAreasUpdatedMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(
                    text = tr(
                        "💡 المنطقة الحالية: ${teacher.area} (${teacher.governorate}). لإضافة منطقتين إضافيتين، جدد باقة بريميوم من الإدارة.",
                        "Current area: ${teacher.area}. Renew Premium via Admin to add 2 extra areas."
                    ),
                    fontSize = 12.sp,
                    color = ElkhetaTextGray,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // بطاقة إنشاء اختبار مع التحقق من باقة اشتراك شامل (Ultimate) والتوجيه للدعم
        AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TeacherPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("إنشاء اختبار ذكي (ميزة اشتراك شامل)", "Smart AI Quiz Creation (Ultimate)"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ElkhetaTextDark
                    )
                }
                Text(
                    text = if (isUltimateActive)
                        tr("مفعّل في حسابك ضمن باقة (اشتراك شامل) لنهاية الترم الدراسي.", "Active in your Ultimate term plan.")
                    else
                        tr("تتطلب هذه الميزة تفعيل باقة (اشتراك شامل) من الدعم الفني.", "Requires Ultimate activation by Support."),
                    fontSize = 12.sp,
                    color = if (isUltimateActive) Color(0xFF2E7D32) else TeacherPurple
                )
                Button(
                    onClick = onCreateQuizClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("إنشاء اختبار", "Create Quiz"), color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // بطاقة الباقات وأتمتة طلب التجديد عبر واتساب
        AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("إدارة الباقات (صالحة لمدة ترم دراسي واحد فقط)", "Subscription Packages (Valid for 1 Academic Term)"),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = ElkhetaTextDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "PREMIUM", isArabic) },
                        border = BorderStroke(1.5.dp, TeacherPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("طلب بريميوم", "Request Premium"), color = TeacherPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "ULTIMATE", isArabic) },
                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("طلب اشتراك شامل", "Request Ultimate"), color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (isInteractiveAllowed) {
                    WhatsAppSupportButton()
                }
            }
        }
    }
}

@Composable
fun TeacherQuizzesTab(
    teacherQuizzes: List<QuizExam>,
    isInteractiveAllowed: Boolean,
    schedules: List<ScheduleSlot>,
    appState: MrPrivateState?,
    context: Context,
    onCreateQuizClick: () -> Unit,
    onViewResults: (QuizExam) -> Unit,
    onReassignQuiz: (QuizExam) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("مكتبة الاختبارات المرسلة (${teacherQuizzes.size})", "Quiz Library & Sent Quizzes (${teacherQuizzes.size})"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TeacherPurpleDark
            )
            Button(
                onClick = onCreateQuizClick,
                colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("+ اختبار جديد", "+ New Quiz"), color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (teacherQuizzes.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(
                    text = tr("لا توجد اختبارات محفوظة في مكتبتك حالياً.", "No saved quizzes in your library yet."),
                    color = ElkhetaTextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            teacherQuizzes.forEach { q ->
                AppCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).shadow(3.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📝 ${q.title}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TeacherPurpleDark)
                                Text(
                                    text = tr("المجموعة: ${q.groupLabel} • الوقت: ${q.durationMinutes} د • الدرجة: ${q.totalScore}", "Group: ${q.groupLabel} • ${q.durationMinutes}m • Score: ${q.totalScore}"),
                                    fontSize = 12.sp,
                                    color = ElkhetaTextGray
                                )
                                Text(
                                    text = tr("عدد الأسئلة: ${q.questions.size} • الطلاب الممتحنون: ${q.submissions.size}", "Questions: ${q.questions.size} • Submissions: ${q.submissions.size}"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { onViewResults(q) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tr("الدرجات", "Scores"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeacherPurple)
                                }
                                val quizDeletedMsg = tr("تم حذف الكويز", "Quiz deleted")
                                IconButton(
                                    onClick = {
                                        appState?.deleteQuizExam(q.id)
                                        Toast.makeText(context, quizDeletedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الكويز", tint = Color(0xFFB91C1C))
                                }
                            }
                        }

                        // زر مكتبة الاختبارات لإعادة الإرسال والتعيين لمجموعات متعددة
                        Button(
                            onClick = { onReassignQuiz(q) },
                            enabled = isInteractiveAllowed && schedules.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("إعادة إرسال وتعيين لمجموعات متعددة", "Re-send & Assign to Multiple Groups"),
                                color = PureWhite,
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
