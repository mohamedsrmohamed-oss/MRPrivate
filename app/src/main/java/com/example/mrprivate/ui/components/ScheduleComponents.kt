@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import java.util.Locale

// ==================== مكونات الجدول: التقويم الشهري وشاشة اليوم ====================
internal val monthNamesAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
internal val monthNamesEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
internal val weekHeadersAr = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
internal val weekHeadersEn = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

/** ساعات اليوم في الجدول (08:00 – 23:00) وتُعرض بنظام 12 ساعة */
val timetableHours: List<String> = (8..23).map { String.format(Locale.US, "%02d:00", it) }

fun formatHour12(hour24: String, isArabic: Boolean): String {
    val h = hour24.substringBefore(":").toIntOrNull() ?: return hour24
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val period = if (h >= 12) {
        if (isArabic) "م" else "PM"
    } else {
        if (isArabic) "ص" else "AM"
    }
    return String.format(Locale.US, "%02d:00 %s", h12, period)
}

fun isBreakValue(v: String): Boolean = v == "Break" || v == "راحة"
fun isTeacherSessionValue(v: String): Boolean = v.contains("(أ/")

fun weeksBetween(startKey: String, endKey: String): Int {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val s = fmt.parse(startKey) ?: return 1
        val e = fmt.parse(endKey) ?: return 1
        (((e.time - s.time) / (24L * 3600L * 1000L)).toInt() / 7).coerceAtLeast(1)
    } catch (ex: Exception) { 1 }
}

fun dateKeyTitle(dateKey: String, isArabic: Boolean): String {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.time = fmt.parse(dateKey) ?: return dateKey
        val dayName = daysOfWeek[cal.get(java.util.Calendar.DAY_OF_WEEK) % 7]
        val months = if (isArabic) monthNamesAr else monthNamesEn
        "${trValue(dayName, isArabic)} ${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${months[cal.get(java.util.Calendar.MONTH)]}"
    } catch (ex: Exception) { dateKey }
}

@Composable
fun MonthCalendar(
    timetable: Map<String, DailyScheduleData>,
    selectedDateKey: String?,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalIsArabic.current
    var monthOffset by remember { mutableIntStateOf(0) }
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        add(java.util.Calendar.MONTH, monthOffset)
    }
    val year = cal.get(java.util.Calendar.YEAR)
    val month = cal.get(java.util.Calendar.MONTH)
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val leading = cal.get(java.util.Calendar.DAY_OF_WEEK) % 7 // السبت = 0
    val rows = (leading + daysInMonth + 6) / 7
    val todayKey = getTodayDateKey()
    val months = if (isArabic) monthNamesAr else monthNamesEn
    val heads = if (isArabic) weekHeadersAr else weekHeadersEn

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                Icon(if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack, contentDescription = null, tint = DarkPurple)
            }
            Text("${months[month]} $year", fontWeight = FontWeight.Black, fontSize = 17.sp, color = ElkhetaTextDark)
            IconButton(onClick = { monthOffset++ }) {
                Icon(if (isArabic) Icons.Default.ArrowBack else Icons.Default.ArrowForward, contentDescription = null, tint = DarkPurple)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            heads.forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElkhetaTextGray)
            }
        }
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - leading + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f).height(46.dp))
                    } else {
                        val key = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNum)
                        CalendarDayCell(
                            modifier = Modifier.weight(1f),
                            dayNum = dayNum,
                            day = timetable[key],
                            isToday = key == todayKey,
                            isSelected = key == selectedDateKey,
                            isPast = key < todayKey,
                            onClick = { onDayClick(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalendarDayCell(
    modifier: Modifier,
    dayNum: Int,
    day: DailyScheduleData?,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val dots = day?.hourlySlots?.values
        ?.filter { it.isNotBlank() && !isBreakValue(it) }
        ?.map { getSubjectVisualStyle(it).accentColor }
        ?.distinct()?.take(3) ?: emptyList()
    val vacation = day?.isVacation == true
    val bg = when {
        isSelected -> ElkhetaCyan
        vacation -> Color(0xFFFEF3C7)
        isPast -> Color(0xFFF1F5F9)
        else -> PureWhite
    }
    val textColor = if (isSelected) PureWhite else if (isPast) ElkhetaTextGray else ElkhetaTextDark
    Column(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (isToday) 2.dp else 1.dp, if (isToday) DarkPurple else ElkhetaInputBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$dayNum", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        if (vacation) {
            Text("🌴", fontSize = 9.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
                dots.forEach { Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) PureWhite else it)) }
            }
        }
    }
}

@Composable
fun DayScheduleScreen(
    dateKey: String,
    day: DailyScheduleData?,
    subjects: List<String>,
    editable: Boolean,
    studentIdForCompletion: String? = null,
    appState: MrPrivateState?,
    onSave: (DailyScheduleData) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val isArabic = LocalIsArabic.current
    val isPast = isDateInPast(dateKey)
    val canEdit = editable && !isPast
    var vacation by remember(dateKey) { mutableStateOf(day?.isVacation == true) }
    val slots = remember(dateKey, day) { mutableStateMapOf<String, String>().apply { putAll(day?.hourlySlots ?: emptyMap()) } }
    fun save() { onSave(DailyScheduleData(dateKey = dateKey, isVacation = vacation, hourlySlots = slots.toMap(), completedHours = day?.completedHours ?: emptyMap())) }

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, ElkhetaInputBorder),
                    modifier = Modifier.clickable { onBack() }
                ) {
                    Text(
                        text = tr("→ الجدول", "→ Calendar"),
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                LanguageSwitcherButton(appState = null, isLightMode = true)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(dateKeyTitle(dateKey, isArabic), fontSize = 22.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
            Spacer(modifier = Modifier.height(10.dp))

            AppCard(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (canEdit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("يوم إجازة", "Day off"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ElkhetaTextDark)
                            Switch(checked = vacation, onCheckedChange = { vacation = it; save() })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (vacation) {
                        Text(tr("🌴 يوم إجازة", "🌴 Day off"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFB45309))
                    } else {
                        val visibleHours = if (canEdit) timetableHours else timetableHours.filter { !slots[it].isNullOrBlank() }
                        if (visibleHours.isEmpty()) {
                            Text(tr("لا توجد مواعيد في هذا اليوم", "No sessions on this day"), color = ElkhetaTextGray, fontSize = 13.sp)
                        }
                        visibleHours.forEach { h ->
                            val isCompleted = day?.completedHours?.get(h) == true
                            HourSlotRow(
                                hour = h,
                                value = slots[h] ?: "",
                                subjects = subjects,
                                editable = canEdit,
                                showStudentCompleteButton = !editable && studentIdForCompletion != null,
                                isCompletedToday = isCompleted,
                                onToggleComplete = {
                                    if (studentIdForCompletion != null && appState != null) {
                                        val subjVal = slots[h] ?: ""
                                        if (subjVal.isNotBlank()) {
                                            appState.toggleStudentDailyStudyCompletion(studentIdForCompletion, dateKey, h, subjVal)
                                        }
                                    }
                                }
                            ) { newValue ->
                                if (newValue.isBlank()) slots.remove(h) else slots[h] = newValue
                                save()
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val note = when {
                canEdit -> tr("التعديل على هذا اليوم فقط", "Changes apply to this day only")
                editable && isPast -> tr("الأيام السابقة للقراءة فقط", "Past days are read-only")
                else -> tr("الجدول يحدده ولي الأمر", "The schedule is set by your parent")
            }
            Text(note, fontSize = 12.sp, color = ElkhetaTextGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        RightCenterFloatingBackButton(onBack = onBack)
    }
}

/** عرض مواد ومواعيد اليوم المختار مباشرة تحت تقويم الأيام (لطالب المنصة) */
@Composable
fun InlineStudentDayScheduleSection(
    dateKey: String,
    day: DailyScheduleData?,
    subjects: List<String>,
    studentId: String,
    appState: MrPrivateState
) {
    val isArabic = LocalIsArabic.current
    val slots = day?.hourlySlots ?: emptyMap()
    val vacation = day?.isVacation == true

    AppCard(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateKeyTitle(dateKey, isArabic),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = DarkPurple
                )
                Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = tr("نظام 12 ساعة", "12-Hour Format"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (vacation) {
                Text(tr("🌴 يوم إجازة", "🌴 Day off"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFB45309))
            } else {
                val visibleHours = timetableHours.filter { !slots[it].isNullOrBlank() }
                if (visibleHours.isEmpty()) {
                    Text(
                        text = tr("لا توجد مواد أو مواعيد مضافة في هذا اليوم.", "No subjects or sessions scheduled on this day."),
                        color = ElkhetaTextGray,
                        fontSize = 13.sp
                    )
                } else {
                    visibleHours.forEach { h ->
                        val isCompleted = day?.completedHours?.get(h) == true
                        HourSlotRow(
                            hour = h,
                            value = slots[h] ?: "",
                            subjects = subjects,
                            editable = false,
                            showStudentCompleteButton = true,
                            isCompletedToday = isCompleted,
                            onToggleComplete = {
                                val subjVal = slots[h] ?: ""
                                if (subjVal.isNotBlank()) {
                                    appState.toggleStudentDailyStudyCompletion(studentId, dateKey, h, subjVal)
                                }
                            },
                            onChange = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HourSlotRow(
    hour: String,
    value: String,
    subjects: List<String>,
    editable: Boolean,
    showStudentCompleteButton: Boolean = false,
    isCompletedToday: Boolean = false,
    onToggleComplete: () -> Unit = {},
    onChange: (String) -> Unit
) {
    val locked = isTeacherSessionValue(value)
    var expanded by remember { mutableStateOf(false) }
    val accent = if (value.isBlank()) ElkhetaInputBorder else getSubjectVisualStyle(value).accentColor
    val isArabic = LocalIsArabic.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatHour12(hour, isArabic), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElkhetaTextDark, modifier = Modifier.width(68.dp))
        Box(modifier = Modifier.width(5.dp).height(34.dp).clip(RoundedCornerShape(3.dp)).background(accent))
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                color = if (locked) Color(0xFFFEF3C7) else ElkhetaInputBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().then(if (editable && !locked) Modifier.clickable { expanded = true } else Modifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (value.isBlank()) tr("فاضي", "Empty") else trValue(value),
                        fontWeight = if (value.isBlank()) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (value.isBlank()) ElkhetaTextGray else ElkhetaTextDark,
                        modifier = Modifier.weight(1f)
                    )
                    if (locked) {
                        Text(tr("معلم محجوز", "Booked"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    } else if (editable) {
                        Text("▾", color = ElkhetaTextGray)
                    }
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(tr("فاضي", "Empty")) }, onClick = { onChange(""); expanded = false })
                subjects.forEach { s ->
                    DropdownMenuItem(text = { Text(trValue(s)) }, onClick = { onChange(s); expanded = false })
                }
                DropdownMenuItem(text = { Text(tr("راحة", "Break")) }, onClick = { onChange("راحة"); expanded = false })
            }
        }
        // زر يومي للطالب لتأكيد إنجاز المواد التي تمت إضافتها من ولي الأمر (وليس المعلم)
        if (showStudentCompleteButton && !locked && value.isNotBlank() && !isBreakValue(value)) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onToggleComplete,
                colors = ButtonDefaults.buttonColors(containerColor = if (isCompletedToday) Color(0xFF2E7D32) else ElkhetaCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isCompletedToday) tr("تم الإنجاز ✓", "Completed ✓") else tr("تم إنجازها", "Mark Done"),
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
