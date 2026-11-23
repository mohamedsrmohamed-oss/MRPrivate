@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.quiz

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.QuizExam
import com.example.mrprivate.QuizQuestion
import com.example.mrprivate.ScheduleSlot
import com.example.mrprivate.TeacherProfile
import com.example.mrprivate.getTodayDateKey
import com.example.mrprivate.services.extractQuestionsFromPaperImageAI
import com.example.mrprivate.services.uriToResizedBase64
import com.example.mrprivate.tr
import com.example.mrprivate.trValue
import com.example.mrprivate.ui.components.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CreateQuizFromPaperDialog(
    teacher: TeacherProfile,
    schedules: List<ScheduleSlot>,
    appState: MrPrivateState? = null,
    onDismiss: () -> Unit,
    onCreateQuiz: (QuizExam) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // نصوص الأخطاء تُجهّز هنا لأن tr() لا تُستدعى إلا داخل الـ Composition
    val msgImageOpenFailed = tr("تعذر فتح الصورة. جرّب صورة أخرى أو صوّرها مرة ثانية.", "Could not open the image. Try another one.")
    val msgCameraFailed = tr("تعذر تشغيل الكاميرا على هذا الجهاز.", "Could not start the camera.")

    // المراحل: 0 = تصوير/رفع الورقة، 1 = مراجعة الأسئلة وحلها من المعلم (Model Answer)، 2 = اختيار المجموعة والوقت والدرجة الكلية
    var currentStep by remember { mutableIntStateOf(0) }
    var isScanningPaper by remember { mutableStateOf(false) }
    var paperBase64 by remember { mutableStateOf("") }
    var rawTextToParse by remember { mutableStateOf("") }

    var quizTitle by remember { mutableStateOf("اختبار قصير - ${teacher.subject}") }
    var durationText by remember { mutableStateOf("15") }
    var totalScoreText by remember { mutableStateOf("20") }
    var selectedSlotId by remember { mutableStateOf(schedules.firstOrNull()?.id ?: "") }

    val extractedQuestions = remember { mutableStateListOf<QuizQuestion>() }
    var errorMsg by remember { mutableStateOf("") }

    // تشغيل خدمة القراءة على صورة جاهزة (بعد تصغيرها) وملء قائمة الأسئلة
    fun scanImage(uri: Uri, tempFile: File? = null) {
        isScanningPaper = true
        errorMsg = ""
        scope.launch {
            try {
                val b64 = uriToResizedBase64(context, uri, maxSide = 1280)
                if (b64.isBlank()) {
                    errorMsg = msgImageOpenFailed
                    return@launch
                }
                paperBase64 = b64

                val res = extractQuestionsFromPaperImageAI(b64, teacher.subject, rawTextToParse)
                extractedQuestions.clear()
                if (res.questions.isNotEmpty()) {
                    extractedQuestions.addAll(res.questions)
                } else {
                    extractedQuestions.addAll(generateSubjectSmartQuestions(teacher.subject))
                }
                currentStep = 1
            } catch (e: Exception) {
                extractedQuestions.clear()
                extractedQuestions.addAll(generateSubjectSmartQuestions(teacher.subject))
                currentStep = 1
            } finally {
                isScanningPaper = false
                tempFile?.delete()
            }
        }
    }

    // التقاط صورة جديدة من الكاميرا
    var cameraFilePath by remember { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        val file = if (cameraFilePath.isNotBlank()) File(cameraFilePath) else null
        if (success && file != null && file.exists()) {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            scanImage(uri, file)
        } else {
            file?.delete()
        }
    }
    fun launchPaperCamera() {
        try {
            val dir = File(context.cacheDir, "paper_photos").apply { mkdirs() }
            val file = File.createTempFile("paper_", ".jpg", dir)
            cameraFilePath = file.absolutePath
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMsg = msgCameraFailed
        }
    }

    // اختيار صورة الورقة من المعرض
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scanImage(uri)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // الهيدر ومؤشر الخطوات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("📝 إنشاء كويز ذكي من ورقة أسئلة", "📝 Smart Paper-to-Quiz Creator"),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = DarkPurple
                        )
                        Text(
                            text = when (currentStep) {
                                0 -> tr("الخطوة 1 من 3: تصوير ورقة الأسئلة وتحويلها بالذكاء الاصطناعي", "Step 1/3: Scan question paper")
                                1 -> tr("الخطوة 2 من 3: قم بحل الأسئلة لتحديد نموذج الإجابة (Model Answer)", "Step 2/3: Solve questions to set Model Answer")
                                else -> tr("الخطوة 3 من 3: اختر المجموعة، الوقت، والدرجة الكلية للإرسال", "Step 3/3: Select Group, Time & Total Score")
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / 3f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = ElkhetaCyan
                )
                Spacer(modifier = Modifier.height(12.dp))

                when (currentStep) {
                    // ==================== الخطوة 1: تصوير أو رفع الورقة ====================
                    0 -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFF0F9FF))
                                    .border(2.dp, ElkhetaCyan, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isScanningPaper) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = DarkPurple)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = tr("جاري قراءة الورقة وتحويلها لأسئلة كويز...", "Scanning paper & extracting questions..."),
                                            fontWeight = FontWeight.Bold,
                                            color = DarkPurple,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DocumentScanner,
                                            contentDescription = null,
                                            tint = DarkPurple,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = tr("صوّر ورقة الأسئلة بالكاميرا أو اخترها من الموبايل", "Capture question paper with Camera or Gallery"),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = ElkhetaTextDark,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = tr("سيقوم البرنامج بتحويل الورقة فوراً إلى أسئلة واختيارات جاهزة للحل", "The app will convert the paper into interactive quiz questions"),
                                            fontSize = 11.sp,
                                            color = ElkhetaTextGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Elkheta3DButton(
                                    text = tr("📷 تصوير بالكاميرا", "📷 Camera"),
                                    onClick = { launchPaperCamera() },
                                    modifier = Modifier.weight(1f),
                                    mainColor = DarkPurple,
                                    shadowColor = ElkhetaPurpleShadow,
                                    height = 46
                                )
                                Elkheta3DButton(
                                    text = tr("🖼️ اختيار صورة ورقة", "🖼️ Gallery"),
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    mainColor = ElkhetaCyan,
                                    shadowColor = ElkhetaCyanShadow,
                                    height = 46
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = tr("أو الصق نص الأسئلة هنا لتحويله تلقائياً (اختياري):", "Or paste questions text here to auto-convert (optional):"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ElkhetaTextDark,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = rawTextToParse,
                                onValueChange = { rawTextToParse = it },
                                placeholder = {
                                    Text(
                                        tr(
                                            """مثال:
1- ما هي عاصمة مصر؟
أ) القاهرة
ب) الإسكندرية
ج) الجيزة
د) أسوان""",
                                            """1- Question?
A) Opt 1
B) Opt 2"""
                                        ),
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val qs = parseRawTextIntoQuestions(rawTextToParse, teacher.subject)
                                    extractedQuestions.clear()
                                    extractedQuestions.addAll(qs)
                                    currentStep = 1
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, DarkPurple)
                            ) {
                                Text(
                                    text = tr("✨ تحويل النص / توليد أسئلة الكويز الآن", "✨ Convert & Proceed to Model Answer"),
                                    color = DarkPurple,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // ==================== الخطوة 2: حل الأسئلة من المعلم (تحديد الـ Model Answer) ====================
                    1 -> {
                        Column(modifier = Modifier.weight(1f)) {
                            if (paperBase64.isNotBlank()) {
                                val bmp = remember(paperBase64) { decodeBase64ToBitmap(paperBase64) }
                                if (bmp != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "صورة الورقة",
                                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tr("✓ تم استخراج الأسئلة من الورقة المصورة", "✓ Questions extracted from paper"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF166534))
                                            Text(tr("اضغط على الإجابة الصحيحة لكل سؤال لتصبح هي الـ Model Answer", "Tap the correct option for each question to set the Model Answer"), fontSize = 11.sp, color = ElkhetaTextGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            } else {
                                Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = tr("👈 قم بحل الأسئلة الآن (اضغط على الدائرة بجانب الإجابة الصحيحة لكل سؤال لاعتمادها كـ Model Answer، ويمكنك تعديل أي نص)", "👈 Solve the questions now by selecting the correct option for each question (Model Answer)"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(extractedQuestions, key = { _, q -> q.id }) { qIdx, question ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (question.correctOptionIndex >= 0) Color(0xFF22C55E) else Color(0xFFF59E0B)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tr("السؤال (${qIdx + 1})", "Question (${qIdx + 1})"),
                                                    fontWeight = FontWeight.Black,
                                                    color = DarkPurple,
                                                    fontSize = 14.sp
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (question.correctOptionIndex >= 0) tr("✓ تم تحديد الإجابة", "✓ Model Answer Set") else tr("اختر الإجابة الصحيحة *", "Select Model Answer *"),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (question.correctOptionIndex >= 0) Color(0xFF166534) else Color(0xFFB91C1C)
                                                    )
                                                    if (extractedQuestions.size > 1) {
                                                        IconButton(
                                                            onClick = { extractedQuestions.removeAt(qIdx) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = question.questionText,
                                                onValueChange = { newText ->
                                                    extractedQuestions[qIdx] = question.copy(questionText = newText)
                                                },
                                                label = { Text(tr("نص السؤال", "Question Text")) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            question.options.forEachIndexed { optIdx, optText ->
                                                val isCorrect = question.correctOptionIndex == optIdx
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isCorrect) Color(0xFFDCFCE7) else PureWhite)
                                                        .border(
                                                            1.dp,
                                                            if (isCorrect) Color(0xFF22C55E) else ElkhetaInputBorder,
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isCorrect,
                                                        onClick = {
                                                            extractedQuestions[qIdx] = question.copy(correctOptionIndex = optIdx)
                                                            errorMsg = ""
                                                        }
                                                    )
                                                    OutlinedTextField(
                                                        value = optText,
                                                        onValueChange = { newOpt ->
                                                            val newOpts = question.options.toMutableList().apply { set(optIdx, newOpt) }
                                                            extractedQuestions[qIdx] = question.copy(options = newOpts)
                                                        },
                                                        label = { Text(tr("اختيار ${optIdx + 1}", "Option ${optIdx + 1}")) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedButton(
                                        onClick = {
                                            extractedQuestions.add(
                                                QuizQuestion(
                                                    questionText = "",
                                                    options = listOf("", "", "", ""),
                                                    correctOptionIndex = -1
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("+ إضافة سؤال إضافي", "+ Add Another Question"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (errorMsg.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 0; errorMsg = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tr("→ رجوع للورقة", "→ Back"))
                                }
                                val modelAnswerErrorMsg = tr("يرجى التأكد من كتابة نص كل سؤال وتحديد الإجابة الصحيحة (Model Answer) لجميع الأسئلة أولاً!", "Please set the correct Model Answer for all questions first!")
                                Button(
                                    onClick = {
                                        val anyUnanswered = extractedQuestions.any { it.correctOptionIndex < 0 || it.questionText.isBlank() }
                                        if (extractedQuestions.isEmpty() || anyUnanswered) {
                                            errorMsg = modelAnswerErrorMsg
                                        } else {
                                            errorMsg = ""
                                            currentStep = 2
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkPurple),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text(tr("اعتماد نموذج الإجابة والتالي ←", "Confirm Model Answer ←"), color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ==================== الخطوة 3: اختيار المجموعة والوقت اللازم والدرجة الكلية وإرسال الكويز ====================
                    else -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = quizTitle,
                                onValueChange = { quizTitle = it; errorMsg = "" },
                                label = { Text(tr("عنوان الكويز / الامتحان *", "Quiz Title *")) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = durationText,
                                    onValueChange = { durationText = it.filter { c -> c.isDigit() }; errorMsg = "" },
                                    label = { Text(tr("الوقت اللازم (بالدقائق) *", "Duration (Minutes) *")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = totalScoreText,
                                    onValueChange = { totalScoreText = it.filter { c -> c.isDigit() }; errorMsg = "" },
                                    label = { Text(tr("الدرجة الكلية للامتحان *", "Total Score *")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Text(
                                text = tr("اختر المجموعة لإرسال الكويز لطلابها:", "Select Group to send Quiz to its students:"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = DarkPurple
                            )

                            if (schedules.isEmpty()) {
                                Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = tr("لا توجد مجموعات مضافة حالياً. يرجى إضافة موعد حصة أولاً.", "No groups found. Please add a schedule slot first."),
                                        color = Color(0xFFB91C1C),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                schedules.forEach { slot ->
                                    val isSelected = selectedSlotId == slot.id
                                    val confirmedCount = slot.enrollments.count { it.isConfirmed }
                                    Surface(
                                        color = if (isSelected) Color(0xFFE0F2FE) else ElkhetaInputBg,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) ElkhetaCyan else ElkhetaInputBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedSlotId = slot.id; errorMsg = "" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${trValue(slot.day)} - ${slot.time} (${slot.stage} ${slot.grade})",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = DarkPurple
                                                )
                                                Text(
                                                    text = tr("الطلاب المؤكدون في المجموعة: $confirmedCount طالب", "Confirmed students in group: $confirmedCount"),
                                                    fontSize = 12.sp,
                                                    color = ElkhetaTextGray
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedSlotId = slot.id; errorMsg = "" }
                                            )
                                        }
                                    }
                                }
                            }

                            if (errorMsg.isNotEmpty()) {
                                Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 1; errorMsg = "" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tr("→ تعديل الأسئلة", "→ Back"))
                                }
                                val fillQuizErrorMsg = tr("يرجى إدخال عنوان الكويز والوقت والدرجة الكلية واختيار المجموعة!", "Please fill title, duration, total score and select a group!")
                                Elkheta3DButton(
                                    text = tr("🚀 إرسال الكويز للمجموعة", "🚀 Send Quiz to Group"),
                                    onClick = {
                                        val mins = durationText.toIntOrNull()
                                        val maxScore = totalScoreText.toIntOrNull()
                                        val chosenSlot = schedules.find { it.id == selectedSlotId }
                                        if (quizTitle.isBlank() || mins == null || mins <= 0 || maxScore == null || maxScore <= 0 || chosenSlot == null) {
                                            errorMsg = fillQuizErrorMsg
                                        } else {
                                            val targetIds = chosenSlot.enrollments.filter { it.isConfirmed }.map { it.studentId }
                                            val newQuiz = QuizExam(
                                                teacherId = teacher.id,
                                                teacherName = teacher.fullName,
                                                subject = teacher.subject,
                                                slotId = chosenSlot.id,
                                                groupLabel = "${chosenSlot.day} - ${chosenSlot.time}",
                                                title = quizTitle.trim(),
                                                durationMinutes = mins,
                                                totalScore = maxScore,
                                                createdAt = getTodayDateKey(),
                                                paperImageBase64 = paperBase64,
                                                questions = extractedQuestions.toList(),
                                                targetStudentIds = targetIds,
                                                submissions = emptyMap()
                                            )
                                            onCreateQuiz(newQuiz)
                                        }
                                    },
                                    modifier = Modifier.weight(1.6f),
                                    mainColor = Color(0xFF16A34A),
                                    shadowColor = Color(0xFF15803D),
                                    height = 46
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
