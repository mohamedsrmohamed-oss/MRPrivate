@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.quiz

import android.graphics.Bitmap
import com.example.mrprivate.data.model.*
import com.example.mrprivate.ui.components.*
import java.io.ByteArrayOutputStream

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
    val questions: List<QuizQuestion> = emptyList(),
    val errorMessage: String? = null
)

internal fun normalizeSubjectAr(subject: String): String {
    val idx = languageSubjects.indexOfFirst { it.equals(subject, ignoreCase = true) }
    return if (idx >= 0) arabicSubjects[idx] else subject
}
