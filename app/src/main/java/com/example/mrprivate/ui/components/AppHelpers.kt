@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.ui.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.mrprivate.data.model.*
import com.example.mrprivate.features.auth.AccountAuth
import com.example.mrprivate.services.uriToResizedBase64
import java.util.Locale

// ==================== 1. الترجمة وإدارة اللغة المشتركة ====================

val LocalIsArabic = compositionLocalOf { true }
val LocalToggleLanguage = compositionLocalOf { {} }

@Composable
fun tr(ar: String, en: String): String = if (LocalIsArabic.current) ar else en

val stageGradesMap: Map<String, List<String>> = mapOf(
    "ابتدائي" to listOf("الصف الأول الابتدائي", "الصف الثاني الابتدائي", "الصف الثالث الابتدائي", "الصف الرابع الابتدائي", "الصف الخامس الابتدائي", "الصف السادس الابتدائي"),
    "إعدادي" to listOf("الصف الأول الإعدادي", "الصف الثاني الإعدادي", "الصف الثالث الإعدادي"),
    "ثانوي" to listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي"),
    "بكالوريا" to listOf("الصف الأول بكالوريا", "الصف الثاني بكالوريا", "الصف الثالث بكالوريا")
)

// ==================== 2. القوائم الثابتة ودوال المساعدة ====================
val governoratesWithAreas = mapOf(
    "القاهرة" to listOf("مدينة نصر", "المعادي", "مصر الجديدة", "شبرا", "التجمع الخامس"),
    "الجيزة" to listOf("6 أكتوبر", "حدائق أكتوبر", "حدائق الأهرام", "الشيخ زايد", "الدقي", "المهندسين", "الهرم", "فيصل"),
    "الإسكندرية" to listOf("سموحة", "سيدي جابر", "ميامي", "المنتزه", "لوران"),
    "الدقهلية" to listOf("المنصورة", "طلخا", "ميت غمر", "السنبلاوين")
)

val educationTracks = listOf("عربي", "لغات")
val educationalStages = listOf("ابتدائي", "إعدادي", "ثانوي", "بكالوريا")
val arabicSubjects = listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "فيزياء", "كيمياء", "حاسب آلي", "برمجة", "تاريخ", "جغرافيا", "دراسات اجتماعية", "أحياء")
val languageSubjects = listOf("Arabic", "English", "Math", "Science", "Physics", "Chemistry", "Computer Science", "Programming", "History", "Geography", "Social Studies", "Biology")
val daysOfWeek = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

val translationMap = mapOf(
    "القاهرة" to "Cairo", "الجيزة" to "Giza", "الإسكندرية" to "Alexandria", "الدقهلية" to "Dakahlia",
    "مدينة نصر" to "Nasr City", "المعادي" to "Maadi", "مصر الجديدة" to "Heliopolis", "شبرا" to "Shubra", "التجمع الخامس" to "5th Settlement",
    "6 أكتوبر" to "6th of October", "حدائق أكتوبر" to "Hadayek October", "حدائق الأهرام" to "Hadayek Al-Ahram", "الشيخ زايد" to "Sheikh Zayed",
    "الدقي" to "Dokki", "المهندسين" to "Mohandessin", "الهرم" to "Haram", "فيصل" to "Faisal", "سموحة" to "Smouha",
    "سيدي جابر" to "Sidi Gaber", "ميامي" to "Miami", "المنتزه" to "Montazah", "لوران" to "Loran", "المنصورة" to "Mansoura",
    "طلخا" to "Talkha", "ميت غمر" to "Mit Ghamr", "السنبلاوين" to "Sinbillawain", "ابتدائي" to "Primary", "إعدادي" to "Preparatory",
    "ثانوي" to "Secondary", "بكالوريا" to "Baccalaureate", "عربي" to "Arabic", "لغات" to "Languages",
    "السبت" to "Saturday", "الأحد" to "Sunday", "الاثنين" to "Monday", "الثلاثاء" to "Tuesday", "الأربعاء" to "Wednesday",
    "الخميس" to "Thursday", "الجمعة" to "Friday", "لغة عربية" to "Arabic Language", "لغة إنجليزية" to "English Language",
    "رياضيات" to "Math", "علوم" to "Science", "فيزياء" to "Physics", "كيمياء" to "Chemistry",
    "حاسب آلي" to "Computer Science", "برمجة" to "Programming", "تاريخ" to "History", "جغرافيا" to "Geography",
    "دراسات اجتماعية" to "Social Studies", "أحياء" to "Biology"
)

fun translateValue(value: String, isArabic: Boolean): String {
    if (isArabic || value.isBlank()) return value
    return translationMap[value] ?: value
}

fun trValue(value: String, isArabic: Boolean): String = translateValue(value, isArabic)

@Composable
fun trValue(value: String): String = translateValue(value, LocalIsArabic.current)

fun getSubjectsByTrack(track: String): List<String> = if (track == "لغات" || track == "Languages") languageSubjects else arabicSubjects

val stageSubjectsAr: Map<String, List<String>> = mapOf(
    "ابتدائي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "دراسات اجتماعية", "حاسب آلي"),
    "إعدادي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "علوم", "دراسات اجتماعية", "حاسب آلي"),
    "ثانوي" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "فيزياء", "كيمياء", "أحياء", "تاريخ", "جغرافيا", "حاسب آلي", "برمجة"),
    "بكالوريا" to listOf("لغة عربية", "لغة إنجليزية", "رياضيات", "فيزياء", "كيمياء", "أحياء", "تاريخ", "جغرافيا", "حاسب آلي", "برمجة")
)

fun getSubjectsFor(track: String, stage: String): List<String> {
    val all = getSubjectsByTrack(track)
    val allowedAr = stageSubjectsAr[stage] ?: return all
    return allowedAr.mapNotNull { ar ->
        val i = arabicSubjects.indexOf(ar)
        if (i >= 0) all.getOrNull(i) else null
    }
}

// ==================== 3. الترم الدراسي والتوزيع التلقائي ====================
private fun dateKeyFormat() = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun shiftDateKey(key: String, days: Int): String {
    val fmt = dateKeyFormat()
    val d = try { fmt.parse(key) } catch (e: Exception) { null } ?: return key
    val c = java.util.Calendar.getInstance()
    c.time = d
    c.add(java.util.Calendar.DAY_OF_YEAR, days)
    return fmt.format(c.time)
}

fun currentTermWindow(ts: TermSettings, todayKey: String = getTodayDateKey()): Triple<Int, String, String>? = when {
    todayKey <= ts.t1End -> Triple(1, maxOf(todayKey, ts.t1Start), ts.t1End)
    todayKey <= ts.t2End -> Triple(2, maxOf(todayKey, ts.t2Start), ts.t2End)
    else -> null
}

private fun dayNameToCalendar(dayName: String): Int = when (dayName) {
    "السبت", "Saturday" -> java.util.Calendar.SATURDAY
    "الأحد", "Sunday" -> java.util.Calendar.SUNDAY
    "الاثنين", "Monday" -> java.util.Calendar.MONDAY
    "الثلاثاء", "Tuesday" -> java.util.Calendar.TUESDAY
    "الأربعاء", "Wednesday" -> java.util.Calendar.WEDNESDAY
    "الخميس", "Thursday" -> java.util.Calendar.THURSDAY
    "الجمعة", "Friday" -> java.util.Calendar.FRIDAY
    else -> java.util.Calendar.SATURDAY
}

fun generateTermDates(dayName: String, startKey: String, endKey: String): List<String> {
    val fmt = dateKeyFormat()
    val start = try { fmt.parse(startKey) } catch (e: Exception) { null } ?: return emptyList()
    val target = dayNameToCalendar(dayName)
    val cal = java.util.Calendar.getInstance()
    cal.time = start
    val out = mutableListOf<String>()
    var guard = 0
    while (guard++ < 400) {
        val key = fmt.format(cal.time)
        if (key > endKey) break
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) == target) out.add(key)
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return out
}

fun slotHourKey(time: String): String {
    val m = Regex("(\\d{1,2})\\s*:\\s*(\\d{2})").find(time) ?: return "17:00"
    var h = m.groupValues[1].toIntOrNull() ?: return "17:00"
    val pm = time.contains("مساء") || time.contains("PM", ignoreCase = true)
    val am = time.contains("صباح") || time.contains("AM", ignoreCase = true)
    if (pm && h < 12) h += 12
    if (am && h == 12) h = 0
    return String.format(Locale.US, "%02d:00", h.coerceIn(0, 23))
}

fun getTodayDateKey(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(java.util.Date())
}

fun isDateInPast(dateKey: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val selectedDate = sdf.parse(dateKey) ?: return false
        val todayStr = sdf.format(java.util.Date())
        val todayDate = sdf.parse(todayStr) ?: return false
        selectedDate.before(todayDate)
    } catch (e: Exception) { false }
}

fun generateYearlySessionDates(dayName: String, weeksCount: Int = 36): List<String> {
    val targetDayOfWeek = when (dayName) {
        "السبت", "Saturday" -> java.util.Calendar.SATURDAY
        "الأحد", "Sunday" -> java.util.Calendar.SUNDAY
        "الاثنين", "Monday" -> java.util.Calendar.MONDAY
        "الثلاثاء", "Tuesday" -> java.util.Calendar.TUESDAY
        "الأربعاء", "Wednesday" -> java.util.Calendar.WEDNESDAY
        "الخميس", "Thursday" -> java.util.Calendar.THURSDAY
        "الجمعة", "Friday" -> java.util.Calendar.FRIDAY
        else -> java.util.Calendar.SATURDAY
    }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = java.util.Calendar.getInstance()
    while (cal.get(java.util.Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    val result = mutableListOf<String>()
    for (w in 0 until weeksCount) {
        result.add(sdf.format(cal.time))
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
    }
    return result
}

fun autoDistributeSubjectPlanOnTimetable(
    student: StudentProfile,
    subjectName: String,
    studyDaysPerWeek: Int,
    termSettings: TermSettings = TermSettings(),
    finished: Boolean = false
): Map<String, DailyScheduleData> {
    val result = student.dailyTimetable.toMutableMap()
    val todayKey = getTodayDateKey()
    val window = currentTermWindow(termSettings, todayKey) ?: return result
    val startKey = window.second
    val endKey = window.third
    val planEnd = shiftDateKey(endKey, -14).let { if (it > startKey) it else endKey }

    for ((key, day) in result.toMap()) {
        if (key >= todayKey && day.hourlySlots.values.any { it == subjectName }) {
            result[key] = day.copy(hourlySlots = day.hourlySlots.filterValues { it != subjectName })
        }
    }
    if (finished) return result

    val fmt = dateKeyFormat()
    val allDates = mutableListOf<Pair<String, Int>>()
    val cal = java.util.Calendar.getInstance()
    cal.time = try { fmt.parse(startKey) } catch (e: Exception) { null } ?: return result
    var guard = 0
    while (guard++ < 400) {
        val key = fmt.format(cal.time)
        if (key > planEnd) break
        allDates.add(key to cal.get(java.util.Calendar.DAY_OF_WEEK))
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }

    val teacherDays = mutableSetOf<Int>()
    allDates.forEach { (key, dow) ->
        if (result[key]?.hourlySlots?.values?.any { it.startsWith("$subjectName (") } == true) teacherDays.add(dow)
    }
    val schoolDays = listOf(
        java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY,
        java.util.Calendar.TUESDAY, java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY
    )
    val wanted = (studyDaysPerWeek.coerceIn(1, 6) - teacherDays.size).coerceAtLeast(0)
    if (wanted == 0) return result
    val pool = schoolDays.filter { it !in teacherDays }
    val picks = (0 until wanted).map { pool[(it * pool.size) / wanted] }.toSet()

    val targets = allDates.filter { (key, dow) -> dow in picks && result[key]?.isVacation != true }
    val hours = listOf("16:00", "18:00", "20:00", "17:00", "19:00", "21:00", "15:00", "14:00")
    fun free(key: String, h: String) = result[key]?.hourlySlots?.containsKey(h) != true
    val sameHour = hours.firstOrNull { h -> targets.all { (key, _) -> free(key, h) } }
    targets.forEach { (key, _) ->
        val h = sameHour ?: hours.firstOrNull { free(key, it) } ?: return@forEach
        val existing = result[key] ?: DailyScheduleData(dateKey = key)
        result[key] = existing.copy(hourlySlots = existing.hourlySlots + (h to subjectName))
    }
    return result
}

// ==================== 4. الصور الشخصية والتواصل والشبكة ====================

const val PROFILE_PHOTO_MAX_SIDE_PX = 320
const val PROFILE_PHOTO_QUALITY = 70

fun uriToBase64(context: Context, uri: Uri): String =
    uriToResizedBase64(context, uri, maxSide = PROFILE_PHOTO_MAX_SIDE_PX, quality = PROFILE_PHOTO_QUALITY, webp = true)

fun openPlatformSupportWhatsApp(context: Context) {
    val supportNumber = "+201118721114"
    val message = "مرحباً دعم منصة MR Private، أحتاج للمساعدة."
    val url = "https://api.whatsapp.com/send?phone=$supportNumber&text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

fun openTeacherWhatsAppForAdmin(context: Context, teacherPhone: String, teacherName: String) {
    val cleanPhone = teacherPhone.trim().replace(" ", "")
    if (cleanPhone.isEmpty()) {
        Toast.makeText(context, "رقم هاتف المعلم غير مسجل", Toast.LENGTH_SHORT).show()
        return
    }
    val formattedPhone = when {
        cleanPhone.startsWith("+") -> cleanPhone
        cleanPhone.startsWith("20") -> "+$cleanPhone"
        cleanPhone.startsWith("0") -> "+2$cleanPhone"
        else -> "+20$cleanPhone"
    }
    val message = "مرحباً $teacherName، معك إدارة منصة MR Private بخصوص مراجعة وتفعيل حسابك."
    val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun saveThemePref(context: Context, themeIndex: Int) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit { putInt("theme_index", themeIndex) }
}

fun saveLoginPrefs(context: Context, role: String, userId: String) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putBoolean("is_logged_in", true)
        putString("role", role)
        putString("active_user_id", userId)
    }
}

fun clearLoginPrefs(context: Context) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    prefs.edit { clear() }
}

@Composable
fun rememberIsNetworkConnected(): Boolean {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(isNetworkAvailable(context)) }
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
        }
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        try { connectivityManager?.registerNetworkCallback(request, callback) } catch (e: Exception) {}
        onDispose { try { connectivityManager?.unregisterNetworkCallback(callback) } catch (e: Exception) {} }
    }
    return isConnected
}

// ==================== 5. استعادة حالة تسجيل الدخول عند بدء التطبيق ====================

/**
 * يستعيد حالة تسجيل الدخول المحفوظة من SharedPreferences.
 * يُستدعى مرة واحدة عبر [LaunchedEffect] في MainActivity.
 * المنطق محمي بالكامل — لا تعديل على مفاتيح SharedPreferences.
 */
fun restoreLoginState(context: Context, state: com.example.mrprivate.MrPrivateState) {
    val prefs = context.getSharedPreferences("mr_private_prefs", Context.MODE_PRIVATE)
    val savedThemeIdx = prefs.getInt("theme_index", 0)
    state.currentThemeIndex = savedThemeIdx.coerceIn(0, appGradientThemes.lastIndex)

    if (prefs.getBoolean("is_logged_in", false)) {
        val role = prefs.getString("role", "") ?: ""
        val userId = prefs.getString("active_user_id", "") ?: ""
        if (role.isNotBlank() && userId.isNotBlank()) {
            if (AccountAuth.currentUid != userId) {
                clearLoginPrefs(context)
                return
            }
            when (role) {
                "admin"             -> { state.activeAdminId  = userId; state.currentScreen = "admin_dashboard" }
                "teacher"           -> { state.activeTeacherId = userId; state.currentScreen = "teacher_dashboard" }
                "student", "parent" -> { state.activeStudentId = userId; state.currentScreen = "student_dashboard" }
            }
        }
    }
}
