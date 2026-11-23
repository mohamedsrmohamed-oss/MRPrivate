@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import com.example.mrprivate.services.extractQuestionsFromPaperImageAI
import com.example.mrprivate.services.uriToResizedBase64
import com.example.mrprivate.ui.components.*
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

// ==================== محرك الذكاء الاصطناعي وتحليل ورق الأسئلة (AI Vision & Smart Parser) ====================

fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
    return try {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

/**
 * تحليل النص المكتوب أو المستخرج من الورقة وتحويله تلقائياً إلى قائمة أسئلة واختيارات
 */
fun parseRawTextIntoQuestions(rawText: String, subject: String): List<QuizQuestion> {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return listOf(QuizQuestion(questionText = "", options = listOf("", "", "", ""), correctOptionIndex = -1))

    val parsed = mutableListOf<QuizQuestion>()
    var currentQuestion = ""
    val currentOptions = mutableListOf<String>()

    fun flushCurrent() {
        if (currentQuestion.isNotBlank()) {
            val opts = when {
                currentOptions.size >= 4 -> currentOptions.take(4)
                currentOptions.size == 2 -> currentOptions + listOf("", "")
                currentOptions.isNotEmpty() -> (currentOptions + listOf("اختيار 1", "اختيار 2", "اختيار 3", "اختيار 4")).take(4)
                else -> listOf("صواب (صح)", "خطأ", "غير ذلك", "جميع ما سبق")
            }
            parsed.add(
                QuizQuestion(
                    questionText = currentQuestion.replace(Regex("^[0-9٠-٩]+[-.)]\\s*"), "").trim(),
                    options = opts,
                    correctOptionIndex = -1
                )
            )
        }
        currentQuestion = ""
        currentOptions.clear()
    }

    for (line in lines) {
        val isOptionLine = line.matches(Regex("^([أبجدهـABCDabcd1-4])[)-.]\\s*.+")) || line.startsWith("-") || line.startsWith("•")
        val isNewQuestionLine = line.matches(Regex("^(س[0-9٠-٩]*|[0-9٠-٩]+[-.)]|Q[0-9]*).*")) || (line.endsWith("؟") || line.endsWith("?"))

        if (isOptionLine && currentQuestion.isNotBlank()) {
            val cleanOpt = line.replace(Regex("^([أبجدهـABCDabcd1-4•-])[)-.]?\\s*"), "").trim()
            if (cleanOpt.isNotBlank()) currentOptions.add(cleanOpt)
        } else if (isNewQuestionLine) {
            if (currentQuestion.isNotBlank()) flushCurrent()
            currentQuestion = line
        } else if (currentQuestion.isBlank()) {
            currentQuestion = line
        } else if (line.contains("-") && currentOptions.isEmpty()) {
            val parts = line.split("-").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) currentOptions.addAll(parts.take(4))
        } else {
            currentOptions.add(line)
        }
    }
    flushCurrent()

    return if (parsed.isNotEmpty()) parsed else listOf(QuizQuestion(questionText = "", options = listOf("", "", "", ""), correctOptionIndex = -1))
}

/**
 * توليد أسئلة مستخرجة ذكياً حسب تخصص المعلم عند تصوير الورقة (كبديل فوري إذا لم يتوفر اتصال Gemini API)
 */
fun generateSubjectSmartQuestions(subject: String): List<QuizQuestion> {
    val s = subject.lowercase()
    return when {
        s.contains("حاسب") || s.contains("برمج") || s.contains("computer") || s.contains("programming") -> listOf(
            QuizQuestion(
                questionText = "ما هي الوظيفة الأساسية لوحدة المعالجة المركزية (CPU) في الحاسب؟",
                options = listOf("معالجة البيانات وتنفيذ التعليمات", "تخزين الملفات بشكل دائم", "عرض الصور على الشاشة", "طباعة المستندات الورقية"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "في لغة البرمجة، أي نوع بيانات يُستخدم لتخزين النصوص؟",
                options = listOf("Int", "String", "Boolean", "Double"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "ذاكرة الوصول العشوائي (RAM) تفقد محتوياتها عند انقطاع التيار الكهربائي.",
                options = listOf("عبارة صحيحة (صواب)", "عبارة خاطئة (خطأ)", "تحتفظ بنصف البيانات فقط", "تتحول إلى قرص صلب"),
                correctOptionIndex = -1
            )
        )
        s.contains("رياض") || s.contains("math") -> listOf(
            QuizQuestion(
                questionText = "إذا كان 3س + 5 = 20 ، فإن قيمة (س) تساوي:",
                options = listOf("3", "5", "7", "15"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "مجموع قياسات الزوايا الداخلة للمثلث يساوي:",
                options = listOf("90 درجة", "180 درجة", "270 درجة", "360 درجة"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "الجذر التربيعي للعدد 144 هو:",
                options = listOf("10", "11", "12", "14"),
                correctOptionIndex = -1
            )
        )
        s.contains("إنجليز") || s.contains("english") -> listOf(
            QuizQuestion(
                questionText = "Choose the correct answer: She ______ to school every day.",
                options = listOf("go", "goes", "going", "gone"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "Choose the correct answer: They ______ studying English right now.",
                options = listOf("is", "am", "are", "was"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "The antonym of the word 'Ancient' is:",
                options = listOf("Old", "Modern", "Historic", "Past"),
                correctOptionIndex = -1
            )
        )
        s.contains("علوم") || s.contains("فيزياء") || s.contains("كيمياء") || s.contains("أحياء") || s.contains("science") -> listOf(
            QuizQuestion(
                questionText = "وحدة قياس القوة في النظام الدولي للوحدات (SI) هي:",
                options = listOf("الجول (Joule)", "النيوتن (Newton)", "الواط (Watt)", "الباسكال (Pascal)"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "الرمز الكيميائي لعنصر الصوديوم هو:",
                options = listOf("So", "Na", "K", "Fe"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "تحدث عملية البناء الضوئي في النبات داخل:",
                options = listOf("البلاستيدات الخضراء", "الميتوكوندريا", "النواة", "الجدار الخلوي"),
                correctOptionIndex = -1
            )
        )
        else -> listOf(
            QuizQuestion(
                questionText = "السؤال الأول المستخرج من الورقة (${subject}): اختر الإجابة الصحيحة مما يلي:",
                options = listOf("الاختيار الأول (أ)", "الاختيار الثاني (ب)", "الاختيار الثالث (ج)", "الاختيار الرابع (د)"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "السؤال الثاني المستخرج من الورقة (${subject}): حدد مدى صحة العبارة التالية:",
                options = listOf("العبارة صحيحة (صواب)", "العبارة خاطئة (خطأ)", "تعتمد على السياق", "جميع ما سبق"),
                correctOptionIndex = -1
            ),
            QuizQuestion(
                questionText = "السؤال الثالث المستخرج من الورقة (${subject}): ما النتيجة المترتبة على ما درست في هذا الدرس؟",
                options = listOf("النتيجة الأولى", "النتيجة الثانية", "النتيجة الثالثة", "النتيجة الرابعة"),
                correctOptionIndex = -1
            )
        )
    }
}

data class PaperScanResult(
    val questions: List<QuizQuestion>,
    val errorMessage: String? = null
)

// ==================== 1. نافذة المعلم: تصوير الورقة -> حل الأسئلة (Model Answer) -> إرسال الكويز للمجموعة ====================

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
    fun scanImage(uri: Uri, tempFile: java.io.File? = null) {
        isScanningPaper = true
        errorMsg = ""
        scope.launch {
            val b64 = withContext(Dispatchers.IO) { uriToResizedBase64(context, uri) }
            tempFile?.let { withContext(Dispatchers.IO) { it.delete() } }
            if (b64.isBlank()) {
                errorMsg = msgImageOpenFailed
                isScanningPaper = false
                return@launch
            }
            paperBase64 = b64
            val res = extractQuestionsFromPaperImageAI(b64, teacher.subject, rawTextToParse)
            extractedQuestions.clear()
            extractedQuestions.addAll(res.questions)
            errorMsg = res.errorMessage ?: ""
            isScanningPaper = false
            currentStep = 1
        }
    }

    // التقاط صورة بالدقة الكاملة من الكاميرا (يُحفظ مؤقتاً في الكاش ثم يُحذف بعد القراءة)
    var cameraFilePath by rememberSaveable { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        val file = if (cameraFilePath.isNotBlank()) java.io.File(cameraFilePath) else null
        if (success && file != null && file.exists()) {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            scanImage(uri, file)
        } else {
            file?.delete()
        }
    }
    fun launchPaperCamera() {
        try {
            val dir = java.io.File(context.cacheDir, "paper_photos").apply { mkdirs() }
            val file = java.io.File.createTempFile("paper_", ".jpg", dir)
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

// ==================== 4. قسم المستوى التعليمي ودرجات الكويزات داخل كارت المادة (للطالب وولي الأمر) ====================

private fun normalizeSubjectAr(subject: String): String {
    val idx = languageSubjects.indexOfFirst { it.equals(subject, ignoreCase = true) }
    return if (idx >= 0) arabicSubjects[idx] else subject
}

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
        .filter { slot -> slot.enrollments.any { it.studentId == student.id && it.isConfirmed } }
        .map { it.id }
        .toSet()

    val subjectQuizzes = appState.quizzes.filter { q ->
        val sameSubj = normalizeSubjectAr(q.subject) == normalizeSubjectAr(subjectName) || q.subject.equals(subjectName, ignoreCase = true)
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
