package com.example.mrprivate.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.mrprivate.PaperScanResult
import com.example.mrprivate.QuizQuestion
import com.example.mrprivate.parseRawTextIntoQuestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * خدمة قراءة ورقة الأسئلة عبر Webhook على Make.
 *
 * الطلب:  POST  application/json
 *   { "image": { "name": "image.jpg", "mime": "image/jpeg", "data": "<base64 خام بدون data:image/...>" } }
 *
 * الرد المتوقع من Webhook response (انظر MAKE_SETUP.md):
 *   { "success": true,
 *     "questions": [ { "questionText": "...", "options": ["..","..","..",".."], "correctIndex": 0 } ] }
 * ملاحظة: correctIndex يبدأ من 0، و -1 (أو غيابه) يعني أن المعلم هو من سيحدد الإجابة.
 */
object PaperScanConfig {
    const val WEBHOOK_URL = "https://hook.eu1.make.com/7u3jmq10gutgv4r3fny9o7mnu6rm9kn0"
    const val MAX_IMAGE_SIDE_PX = 1600
    const val JPEG_QUALITY = 80
    const val CONNECT_TIMEOUT_MS = 20_000
    // Make يقطع الرد المتزامن بعد حوالي 40 ثانية، فنعطي هامش بسيط
    const val READ_TIMEOUT_MS = 55_000
    const val MAX_ATTEMPTS = 2
}

// ==================== معالجة الصورة: تصغير + تصحيح الاتجاه + JPEG ====================

/**
 * يقرأ الصورة من الـ Uri بدقة محدودة (الأطول 1600px) ويصحح اتجاهها حسب EXIF
 * ويرجعها Base64 خام (NO_WRAP). استدعها من خيط الخلفية فقط. يرجع "" عند الفشل.
 */
fun uriToResizedBase64(
    context: Context,
    uri: Uri,
    maxSide: Int = PaperScanConfig.MAX_IMAGE_SIDE_PX,
    quality: Int = PaperScanConfig.JPEG_QUALITY,
    webp: Boolean = false
): String {
    val resolver = context.contentResolver
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return ""

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return ""

        val rotation = readExifRotation(context, uri)
        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f

        val finalBmp = if (scale < 1f || rotation != 0) {
            val m = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        } else decoded

        val out = ByteArrayOutputStream()
        finalBmp.compress(if (webp) webpCompressFormat() else Bitmap.CompressFormat.JPEG, quality, out)
        if (finalBmp !== decoded) decoded.recycle()
        finalBmp.recycle()
        android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    } catch (e: OutOfMemoryError) {
        ""
    }
}

/** WEBP_LOSSY متاح من API 30، وقبله نستخدم WEBP القديم (يعمل بنفس الأسلوب). */
@Suppress("DEPRECATION")
private fun webpCompressFormat(): Bitmap.CompressFormat =
    if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

private fun readExifRotation(context: Context, uri: Uri): Int = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0
} catch (e: Exception) {
    0
}

// ==================== الاتصال بالـ Webhook ====================

private sealed class ScanCall {
    data class Ok(val questions: List<QuizQuestion>) : ScanCall()
    data class Fail(val message: String, val retryable: Boolean) : ScanCall()
}

private fun emptyQuestion() = QuizQuestion(questionText = "", options = listOf("", "", "", ""), correctOptionIndex = -1)

private fun scanFailure(msg: String) = PaperScanResult(questions = listOf(emptyQuestion()), errorMessage = msg)

/**
 * استخراج الأسئلة من صورة الورقة. لو مفيش صورة وفيه نص مكتوب يدوياً، يحلل النص محلياً.
 */
suspend fun extractQuestionsFromPaperImageAI(
    base64Image: String,
    subject: String,
    rawTextHint: String = ""
): PaperScanResult = withContext(Dispatchers.IO) {
    if (base64Image.isBlank() && rawTextHint.isNotBlank()) {
        return@withContext PaperScanResult(parseRawTextIntoQuestions(rawTextHint, subject))
    }
    if (base64Image.isBlank()) {
        return@withContext PaperScanResult(questions = listOf(emptyQuestion()))
    }

    var lastFailure = "تعذر قراءة الصورة. حاول مرة أخرى أو اكتب الأسئلة يدوياً."
    for (attempt in 1..PaperScanConfig.MAX_ATTEMPTS) {
        when (val call = callPaperWebhook(base64Image)) {
            is ScanCall.Ok -> {
                return@withContext if (call.questions.isNotEmpty()) {
                    PaperScanResult(call.questions)
                } else {
                    scanFailure("لم يتم العثور على أسئلة واضحة في الصورة. يرجى تصوير الورقة بإضاءة أوضح وبشكل مستقيم، أو كتابة الأسئلة.")
                }
            }
            is ScanCall.Fail -> {
                lastFailure = call.message
                if (!call.retryable || attempt == PaperScanConfig.MAX_ATTEMPTS) break
                delay(1500L)
            }
        }
    }
    scanFailure(lastFailure)
}

private fun callPaperWebhook(base64Image: String): ScanCall {
    var conn: HttpURLConnection? = null
    return try {
        val body = JSONObject().put(
            "image", JSONObject().apply {
                put("name", "image.jpg")
                put("mime", "image/jpeg")
                put("data", base64Image)
            }
        ).toString().toByteArray(Charsets.UTF_8)

        conn = (URL(PaperScanConfig.WEBHOOK_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            connectTimeout = PaperScanConfig.CONNECT_TIMEOUT_MS
            readTimeout = PaperScanConfig.READ_TIMEOUT_MS
            doOutput = true
            setFixedLengthStreamingMode(body.size)
        }
        conn.outputStream.use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

        when {
            code in 200..299 -> parseWebhookResponse(text)
            code == 429 -> ScanCall.Fail("الخدمة مشغولة حالياً (كود 429). حاول بعد لحظات.", true)
            code >= 500 -> ScanCall.Fail("خطأ مؤقت في خدمة القراءة (كود $code). حاول مرة أخرى.", true)
            code == 413 -> ScanCall.Fail("حجم الصورة كبير جداً (كود 413). صوّر الورقة من مسافة أقرب أو بدقة أقل.", false)
            else -> ScanCall.Fail("تعذر الوصول لخدمة القراءة (كود $code). تأكد أن السيناريو في Make مفعّل (ON).", false)
        }
    } catch (e: SocketTimeoutException) {
        ScanCall.Fail("استغرقت قراءة الصورة وقتاً أطول من المعتاد. حاول مرة أخرى.", true)
    } catch (e: IOException) {
        ScanCall.Fail("تعذر الاتصال بالإنترنت. تأكد من الشبكة وحاول مرة أخرى.", true)
    } catch (e: Exception) {
        ScanCall.Fail("حدث خطأ غير متوقع أثناء قراءة الصورة: ${e.localizedMessage ?: ""}", false)
    } finally {
        conn?.disconnect()
    }
}

// ==================== قراءة الرد (متسامحة مع اختلاف الشكل) ====================

private fun parseWebhookResponse(raw: String): ScanCall {
    val body = raw.trim()
    if (body.isEmpty()) {
        return ScanCall.Fail("الخدمة رجّعت ردّاً فارغاً. تأكد أن آخر خطوة في سيناريو Make هي Webhook response.", false)
    }

    val root: Any = parseLooseJson(body)
        ?: return ScanCall.Fail(
            "رد الخدمة ليس بصيغة JSON (\"${body.take(40)}\"). في Make أضف Webhook response بعد خطوة الذكاء الاصطناعي.",
            false
        )

    if (root is JSONObject && root.has("success") && !root.optBoolean("success", true)) {
        val reason = root.optString("error", root.optString("message", "")).ifBlank { "فشل غير معروف" }
        return ScanCall.Fail("تعذر استخراج الأسئلة: $reason", false)
    }

    val array = findQuestionsArray(root)
        ?: return ScanCall.Fail("شكل رد الخدمة غير مفهوم. المتوقع: { \"questions\": [ ... ] }.", false)

    val questions = mutableListOf<QuizQuestion>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        mapQuestion(item)?.let { questions.add(it) }
    }
    return ScanCall.Ok(questions)
}

private fun parseLooseJson(s: String): Any? {
    val clean = s.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return try {
        when {
            clean.startsWith("[") -> JSONArray(clean)
            clean.startsWith("{") -> JSONObject(clean)
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/** يدوّر على مصفوفة الأسئلة داخل الرد، حتى لو كانت ملفوفة في data/result أو نص JSON. */
private fun findQuestionsArray(node: Any?, depth: Int = 0): JSONArray? {
    if (node == null || depth > 4) return null
    return when (node) {
        is JSONArray -> if (node.length() == 0 || node.opt(0) is JSONObject) node else null
        is JSONObject -> {
            // رد Gemini الخام: candidates[].content.parts[].text (نص JSON بداخله الأسئلة)
            node.optJSONArray("candidates")?.let { cands ->
                for (c in 0 until cands.length()) {
                    val parts = cands.optJSONObject(c)?.optJSONObject("content")?.optJSONArray("parts") ?: continue
                    for (p in 0 until parts.length()) {
                        val t = parts.optJSONObject(p)?.optString("text", "").orEmpty()
                        if (t.isBlank()) continue
                        findQuestionsArray(parseLooseJson(t), depth + 1)?.let { return it }
                    }
                }
            }
            // شكل OpenAI: choices[0].message.content
            node.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")
                ?.takeIf { it.isNotBlank() }
                ?.let { t -> findQuestionsArray(parseLooseJson(t), depth + 1)?.let { return it } }

            val keys = listOf("questions", "data", "result", "output", "items", "response", "text", "content", "json")
            for (k in keys) {
                if (!node.has(k)) continue
                val v = node.opt(k)
                val candidate: Any? = if (v is String) parseLooseJson(v) else v
                findQuestionsArray(candidate, depth + 1)?.let { return it }
            }
            null
        }
        else -> null
    }
}

private fun firstString(obj: JSONObject, vararg keys: String): String {
    for (k in keys) {
        if (obj.has(k) && !obj.isNull(k)) {
            val v = obj.opt(k)
            if (v is String && v.isNotBlank()) return v
            if (v is Number) return v.toString()
        }
    }
    return ""
}

private fun mapQuestion(item: JSONObject): QuizQuestion? {
    val text = firstString(item, "questionText", "question", "text", "q", "title").trim()
    if (text.isBlank()) return null

    val optsArr = listOf("options", "choices", "answers").firstNotNullOfOrNull { item.optJSONArray(it) }
    val opts = mutableListOf<String>()
    if (optsArr != null) {
        for (i in 0 until optsArr.length()) {
            val o = optsArr.opt(i)
            opts.add(
                when (o) {
                    is JSONObject -> firstString(o, "text", "label", "value")
                    null -> ""
                    else -> o.toString()
                }.trim()
            )
        }
    }
    while (opts.size < 4) opts.add("")
    val finalOpts = opts.take(4)
    return QuizQuestion(
        questionText = text,
        options = finalOpts,
        correctOptionIndex = resolveCorrectIndex(item, finalOpts)
    )
}

/** الإجابة الصحيحة: رقم (يبدأ من 0)، أو حرف A-D / أ-د، أو نص الإجابة نفسه. أي شك => -1 ليحددها المعلم. */
private fun resolveCorrectIndex(item: JSONObject, options: List<String>): Int {
    fun valid(i: Int) = i in 0..3 && options[i].isNotBlank()

    for (k in listOf("correctIndex", "correctOptionIndex", "answerIndex", "correct_index")) {
        if (item.has(k) && !item.isNull(k)) {
            val i = item.optInt(k, -1)
            if (valid(i)) return i
        }
    }
    for (k in listOf("correctAnswer", "correct", "answer", "correct_answer")) {
        if (!item.has(k) || item.isNull(k)) continue
        val v = item.opt(k)
        if (v is Number) { if (valid(v.toInt())) return v.toInt(); continue }
        val s = (v as? String)?.trim().orEmpty()
        if (s.isEmpty()) continue
        val letterIdx = when (s.uppercase().take(1)) {
            "A", "أ", "ا" -> 0
            "B", "ب" -> 1
            "C", "ج" -> 2
            "D", "د" -> 3
            else -> -1
        }
        if (s.length <= 2 && valid(letterIdx)) return letterIdx
        val byText = options.indexOfFirst { it.isNotBlank() && it.equals(s, ignoreCase = true) }
        if (valid(byText)) return byText
    }
    return -1
}
