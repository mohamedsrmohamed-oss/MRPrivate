@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mrprivate.data.model.*
import com.example.mrprivate.ui.components.*
import kotlinx.coroutines.delay
import java.util.Locale

// ==================== 3. نافذة امتحان الطالب المؤقتة بالتايمر التنازلي والتصحيح الآلي ====================

@Composable
fun StudentTakeQuizDialog(
    quiz: QuizExam,
    student: StudentProfile,
    onDismiss: () -> Unit,
    onSubmitQuiz: (List<Int>) -> QuizSubmission
) {
    val selectedAnswers = remember {
        mutableStateListOf<Int>().apply {
            repeat(quiz.questions.size) { add(-1) }
        }
    }
    var remainingSeconds by remember { mutableIntStateOf((quiz.durationMinutes.coerceAtLeast(1)) * 60) }
    var submissionResult by remember { mutableStateOf<QuizSubmission?>(null) }

    // عداد تنازلي للوقت المحدد من المعلم؛ يسلم الامتحان تلقائياً عند انتهاء الوقت
    LaunchedEffect(submissionResult) {
        if (submissionResult == null) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            if (submissionResult == null) {
                submissionResult = onSubmitQuiz(selectedAnswers.toList())
            }
        }
    }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)

    Dialog(
        onDismissRequest = { if (submissionResult != null) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            val result = submissionResult
            if (result != null) {
                // شاشة النتيجة الفورية بعد التصحيح التلقائي مقارنة بـ Model Answer المعلم
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tr("تم تسليم الكويز وتصحيحه تلقائياً!", "Quiz Submitted & Graded Automatically!"),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = DarkPurple,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${quiz.title} (${trValue(quiz.subject)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ElkhetaTextGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(2.dp, Color(0xFF22C55E))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tr("درجتك في الامتحان", "Your Score"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Text(
                                text = "${result.score} / ${result.totalScore}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr("✓ تم حفظ الدرجة في المستوى التعليمي داخل موادي وفي حساب ولي الأمر.", "✓ Score saved to your Educational Level and Parent's Subjects tab."),
                        fontSize = 12.sp,
                        color = ElkhetaTextGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Elkheta3DButton(
                        text = tr("العودة إلى موادي", "Back to Subjects"),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // شاشة حل الأسئلة مع التايمر
                Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(quiz.title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = DarkPurple)
                            Text("${quiz.teacherName} • ${tr("الدرجة الكلية:", "Total:")} ${quiz.totalScore}", fontSize = 12.sp, color = ElkhetaTextGray)
                        }
                        Surface(
                            color = if (remainingSeconds < 60) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.5.dp, if (remainingSeconds < 60) Color.Red else GoldStar)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (remainingSeconds < 60) Color.Red else Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timerText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (remainingSeconds < 60) Color.Red else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(quiz.questions, key = { _, q -> q.id }) { qIdx, question ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${qIdx + 1}. ${question.questionText}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ElkhetaTextDark
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    question.options.filter { it.isNotBlank() }.forEachIndexed { optIdx, optText ->
                                        val isPicked = selectedAnswers.getOrElse(qIdx) { -1 } == optIdx
                                        Surface(
                                            color = if (isPicked) Color(0xFFE0F2FE) else PureWhite,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                if (isPicked) 2.dp else 1.dp,
                                                if (isPicked) ElkhetaCyan else ElkhetaInputBorder
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { selectedAnswers[qIdx] = optIdx }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isPicked,
                                                    onClick = { selectedAnswers[qIdx] = optIdx }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(optText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ElkhetaTextDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Elkheta3DButton(
                        text = tr("✅ تسليم الإجابات وإظهار الدرجة فوراً", "✅ Submit Answers & View Score"),
                        onClick = {
                            submissionResult = onSubmitQuiz(selectedAnswers.toList())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        mainColor = Color(0xFF16A34A),
                        shadowColor = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}
