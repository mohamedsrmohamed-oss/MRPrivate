@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import com.example.mrprivate.ui.components.*

// ==================== 2. نافذة عرض نتائج طلاب المجموعة في الكويز (عند المعلم) ====================

@Composable
fun TeacherQuizResultsDialog(
    quiz: QuizExam,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(quiz.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = DarkPurple)
                Text(
                    text = tr("المجموعة: ${quiz.groupLabel} | الدرجة الكلية: ${quiz.totalScore} | المدة: ${quiz.durationMinutes} د", "Group: ${quiz.groupLabel} | Total Score: ${quiz.totalScore}"),
                    fontSize = 12.sp,
                    color = ElkhetaTextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (quiz.submissions.isEmpty()) {
                    Text(tr("لم يقم أي طالب بتسليم إجابات هذا الكويز حتى الآن.", "No student submissions yet."), color = Color.Gray, fontSize = 13.sp)
                } else {
                    quiz.submissions.values.forEach { sub ->
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sub.studentName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ElkhetaTextDark)
                                    Text(tr("تاريخ الحل: ${sub.submittedAt}", "Date: ${sub.submittedAt}"), fontSize = 11.sp, color = ElkhetaTextGray)
                                }
                                Surface(color = DarkPurple, shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        text = "${sub.score} / ${sub.totalScore}",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("إغلاق", "Close"), color = PureWhite)
                }
            }
        }
    }
}

// ==================== 4. قسم المستوى التعليمي ودرجات الكويزات داخل كارت المادة (للطالب وولي الأمر) ====================

@Composable
fun SubjectEducationalLevelSection(
    student: StudentProfile,
    subjectName: String,
    appState: MrPrivateState,
    isStudentView: Boolean
) {
    var quizToTake by remember { mutableStateOf<QuizExam?>(null) }

    // جلب جميع الكويزات المرسلة لمجموعة هذا الطالب في هذه المادة
    val studentEnrolledSlotIds = appState.schedules
        .filter { slot -> slot.enrollments.any { it.studentId == student.id || (it.studentName.isNotBlank() && it.studentName == student.name) } }
        .map { it.id }
        .toSet()

    val subjectQuizzes = appState.quizzes.filter { q ->
        val s1 = normalizeSubjectAr(q.subject).trim()
        val s2 = normalizeSubjectAr(subjectName).trim()
        val sameSubj = s1.equals(s2, ignoreCase = true) ||
            s1.contains(s2, ignoreCase = true) ||
            s2.contains(s1, ignoreCase = true)
        val targetedToStudent = q.targetStudentIds.contains(student.id) || q.slotId in studentEnrolledSlotIds || q.submissions.containsKey(student.id)
        sameSubj && targetedToStudent
    }

    val completedSubmissions = subjectQuizzes.mapNotNull { q -> q.submissions[student.id]?.let { q to it } }
    val pendingQuizzes = subjectQuizzes.filter { q -> !q.submissions.containsKey(student.id) }

    val totalEarned = completedSubmissions.sumOf { it.second.score }
    val totalPossible = completedSubmissions.sumOf { it.second.totalScore }.coerceAtLeast(1)
    val levelPercent = if (completedSubmissions.isEmpty()) null else ((totalEarned * 100) / totalPossible).coerceIn(0, 100)

    val levelLabel = when {
        levelPercent == null -> tr("لم يمتحن بعد", "No exams yet")
        levelPercent >= 85 -> tr("ممتاز ($levelPercent%)", "Excellent ($levelPercent%)")
        levelPercent >= 70 -> tr("جيد جداً ($levelPercent%)", "Very Good ($levelPercent%)")
        levelPercent >= 50 -> tr("متوسط ($levelPercent%)", "Average ($levelPercent%)")
        else -> tr("يحتاج متابعة ($levelPercent%)", "Needs Support ($levelPercent%)")
    }

    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ElkhetaInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("المستوى التعليمي ودرجات الكويزات", "Educational Level & Quiz Scores"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = DarkPurple
                    )
                }
                Surface(
                    color = if (levelPercent != null && levelPercent >= 70) Color(0xFFDCFCE7) else Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = levelLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (levelPercent != null && levelPercent >= 70) Color(0xFF166534) else Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // الكويزات الجديدة المطلوب حلها (تظهر للطالب ليبدأ الامتحان، وتظهر لولي الأمر كإشعار أن هناك كويز بانتظار الحل)
            if (pendingQuizzes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                pendingQuizzes.forEach { q ->
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldStar),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📝 ${q.title}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = ElkhetaTextDark
                                )
                                Text(
                                    text = tr("المدة: ${q.durationMinutes} دقيقة • الدرجة الكلية: ${q.totalScore}", "Time: ${q.durationMinutes}m • Score: ${q.totalScore}"),
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isStudentView) {
                                Button(
                                    onClick = { quizToTake = q },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(tr("ابدأ الكويز الآن", "Start Quiz"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = tr("بانتظار حل الطالب ⏳", "Pending Student ⏳"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            // سجل درجات الامتحانات المحفوظة (يظهر للطالب في المستوى التعليمي ولولي الأمر داخل تاب المواد)
            if (completedSubmissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                completedSubmissions.forEach { (q, sub) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureWhite)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✓ ${q.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ElkhetaTextDark
                            )
                            Text(
                                text = "${q.teacherName} • ${sub.submittedAt}",
                                fontSize = 10.sp,
                                color = ElkhetaTextGray
                            )
                        }
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tr("الدرجة: ${sub.score} / ${sub.totalScore}", "Score: ${sub.score}/${sub.totalScore}"),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    quizToTake?.let { activeQuiz ->
        StudentTakeQuizDialog(
            quiz = activeQuiz,
            student = student,
            onDismiss = { quizToTake = null },
            onSubmitQuiz = { answers ->
                appState.submitStudentQuizAnswers(activeQuiz, student, answers)
            }
        )
    }
}
