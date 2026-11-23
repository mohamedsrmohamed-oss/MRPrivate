package com.example.mrprivate

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Calendar

// ==================== الهوية اللونية الجديدة لكل دور ====================
// الطالب: Aurora (سماوي ← نيلي ← موف)
val AuroraDeep = Color(0xFF0369A1)
val AuroraMid = Color(0xFF4F46E5)
val AuroraEnd = Color(0xFF9333EA)
val AuroraGlow = Color(0xFF22D3EE)
val AuroraAccent = Color(0xFF4338CA)
val AuroraSkyTop = Color(0xFFD6F0FF)
val AuroraSkyBottom = Color(0xFFF2FAFF)
val AuroraLilacTop = Color(0xFFEDE4FF)
val AuroraLilacBottom = Color(0xFFFAF6FF)

// ولي الأمر: أزرق نيلي
val ParentDeep = Color(0xFF1E3A8A)
val ParentMid = Color(0xFF3730A3)
val ParentEnd = Color(0xFF4F46E5)
val ParentGlow = Color(0xFF60A5FA)
val ParentAccent = Color(0xFF312E81)

// المعلم: بنفسجي
val TeacherPurple = Color(0xFF7C3AED)
val TeacherPurpleDark = Color(0xFF5B21B6)
val TeacherPurpleEnd = Color(0xFF9333EA)
val TeacherPurpleSoft = Color(0xFFEDE4FF)

// ==================== الأيقونات ثلاثية الأبعاد ====================
/**
 * يرسم أيقونة 3D (PNG بخلفية بيضاء) بنمط Multiply ليذوب الأبيض في خلفية الكارت.
 * يحافظ على نسبة أبعاد الصورة ويوسّطها داخل الحجم المطلوب.
 */
@Composable
fun Icon3D(res: Int, modifier: Modifier = Modifier) {
    val bmp: ImageBitmap = ImageBitmap.imageResource(res)
    Canvas(modifier = modifier) {
        val scale = minOf(size.width / bmp.width, size.height / bmp.height)
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        drawImage(
            image = bmp,
            dstOffset = IntOffset(((size.width - w) / 2f).toInt(), ((size.height - h) / 2f).toInt()),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.High,
            blendMode = BlendMode.Multiply
        )
    }
}

// ==================== كارت الحصة الجاية (Hero) ====================
@Composable
fun SessionHeroCard(
    label: String,
    title: String,
    subtitle: String,
    brush: Brush,
    glow: Color,
    actionTextColor: Color,
    iconRes: Int,
    chips: List<String> = emptyList(),
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(brush)
            .drawBehind {
                val c = Offset(size.width * 0.15f, size.height * 0.1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.45f), Color.Transparent),
                        center = c,
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = c
                )
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = PureWhite.copy(alpha = 0.92f), fontSize = 13.sp)
                    Text(title, color = PureWhite, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = PureWhite, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(8.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon3D(iconRes, Modifier.size(68.dp))
                }
            }
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { chip ->
                        Surface(color = PureWhite.copy(alpha = 0.22f), shape = RoundedCornerShape(16.dp)) {
                            Text(
                                chip,
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            if (actionText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PureWhite)
                        .clickable { onAction() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(actionText, color = actionTextColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==================== كارت القسم الباستيل (جدولي / موادي) ====================
@Composable
fun HubTileCard(
    title: String,
    iconRes: Int,
    top: Color,
    bottom: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(190.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(top, bottom)))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Icon3D(iconRes, Modifier.align(Alignment.TopCenter).size(width = 120.dp, height = 104.dp))
        Text(
            text = title,
            modifier = Modifier.align(Alignment.BottomStart),
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            color = ElkhetaTextDark
        )
    }
}

// ==================== منطق اختيار "الحصة الجاية" ====================
data class HeroSession(
    val dateKey: String,
    val hourKey: String,
    val value: String,
    val isBooked: Boolean
) {
    /** اسم المادة: للحصص المحجوزة يكون قبل "(أ/ ..." وللمواد المضافة هو القيمة نفسها */
    val subject: String
        get() = if (isBooked) {
            val i = value.lastIndexOf("(أ/")
            if (i > 0) value.substring(0, i).trim() else value
        } else value

    val teacherName: String
        get() = if (isBooked) {
            val i = value.lastIndexOf("(أ/")
            if (i >= 0) value.substring(i + 1).removeSuffix(")").trim() else ""
        } else ""
}

/**
 * القواعد:
 *  - مادة مضافة في الجدول بدون حجز: تظهر حتى يضغط الطالب "تم الإنجاز".
 *  - حجز مؤكد مع معلم (الحجوزات المؤكدة فقط هي التي تُكتب في جدول الطالب): تظهر وتُزال بعد ميعاد الحجز بساعة.
 */
fun pickNextHeroSession(
    child: StudentProfile,
    nowMillis: Long,
    includeFutureDays: Boolean,
    bookedOnly: Boolean
): HeroSession? {
    val today = getTodayDateKey()
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val dates = if (includeFutureDays) child.dailyTimetable.keys.filter { it >= today }.sorted() else listOf(today)
    for (dKey in dates) {
        val day = child.dailyTimetable[dKey] ?: continue
        if (day.isVacation) continue
        val entries = day.hourlySlots.entries
            .filter { it.value.isNotBlank() && !isBreakValue(it.value) }
            .sortedBy { it.key }
        for (e in entries) {
            val booked = isTeacherSessionValue(e.value)
            if (bookedOnly && !booked) continue
            val startHour = e.key.substringBefore(":").toIntOrNull() ?: continue
            val startMin = startHour * 60
            if (booked) {
                if (dKey == today && nowMin >= startMin + 60) continue
            } else {
                if (day.completedHours[e.key] == true) continue
            }
            return HeroSession(dKey, e.key, e.value, booked)
        }
    }
    return null
}

/** يبحث عن الميعاد والحجز المؤكد المطابقين، ويعتمد على خريطة المعلمين (بدون بحث متكرر داخل القائمة) */
fun findBookedSlot(
    child: StudentProfile,
    hourKey: String,
    label: String,
    allSchedules: List<ScheduleSlot>,
    teachersById: Map<String, TeacherProfile>
): Pair<ScheduleSlot, StudentEnrollment>? {
    for (slot in allSchedules) {
        if (slotHourKey(slot.time) != hourKey) continue
        val t = teachersById[slot.teacherId] ?: continue
        if ("${t.subject} (${t.fullName})" != label) continue
        val enr = slot.enrollments.find {
            (it.studentId == child.id || it.studentName == child.name) && it.isConfirmed
        } ?: continue
        return slot to enr
    }
    return null
}

/** يحدّث الشاشة كل نصف دقيقة ليختفي الكارت بعد انتهاء المهلة */
@Composable
fun rememberNowMillis(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    return now
}

// ==================== Skeleton Loading (حالة التحميل) ====================
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(24.dp)) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing)),
        label = "skeletonShift"
    )
    val x0 = -600f + 1800f * shift
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE5E9F5), Color(0xFFF6F8FE), Color(0xFFE5E9F5)),
                    start = Offset(x0, 0f),
                    end = Offset(x0 + 600f, 0f)
                )
            )
    )
}

@Composable
fun HeroHomeSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonBox(Modifier.fillMaxWidth().height(230.dp), RoundedCornerShape(30.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(190.dp), RoundedCornerShape(28.dp))
            SkeletonBox(Modifier.weight(1f).height(190.dp), RoundedCornerShape(28.dp))
        }
    }
}

/** يعرض Skeleton حتى يصل أول تحميل من Firebase (بحد أقصى 6 ثوانٍ كي لا يعلق عند انقطاع الشبكة) */
@Composable
private fun rememberShowSkeleton(appState: MrPrivateState): Boolean {
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(6_000L)
        timedOut = true
    }
    return !appState.isDataReady && !timedOut
}

// ==================== الشاشة الرئيسية للطالب ====================
@Composable
fun StudentHeroHome(
    student: StudentProfile,
    appState: MrPrivateState,
    onOpenSchedule: () -> Unit,
    onOpenSubjects: () -> Unit
) {
    if (rememberShowSkeleton(appState)) {
        HeroHomeSkeleton()
    } else {
        StudentHeroHomeContent(student, appState, onOpenSchedule, onOpenSubjects)
    }
}

@Composable
private fun StudentHeroHomeContent(
    student: StudentProfile,
    appState: MrPrivateState,
    onOpenSchedule: () -> Unit,
    onOpenSubjects: () -> Unit
) {
    val isArabic = LocalIsArabic.current
    val now = rememberNowMillis()
    val minuteKey = now / 60_000L
    val hero = remember(student.dailyTimetable, minuteKey) {
        pickNextHeroSession(student, now, includeFutureDays = false, bookedOnly = false)
    }
    // تخزين مؤقت في الذاكرة: خريطة المعلمين ونتيجة المطابقة يُعاد حسابهما فقط عند تغيّر البيانات
    val teachersById by remember(appState.teachers) {
        derivedStateOf { appState.teachers.associateBy { it.id } }
    }
    val match by remember(student.id, hero?.hourKey, hero?.value) {
        derivedStateOf {
            if (hero != null && hero.isBooked) {
                findBookedSlot(student, hero.hourKey, hero.value, appState.schedules, teachersById)
            } else null
        }
    }
    val nextLabel = tr("حصتك الجاية", "Your next session")
    val doneLabel = tr("تم الإنجاز", "Done")
    val confirmedLabel = tr("● مؤكد (Confirmed)", "● Confirmed")
    val onlineLabel = tr("أونلاين", "Online")
    val offlineLabel = tr("حضوري", "Offline")

    Column(modifier = Modifier.fillMaxWidth()) {
        if (hero != null) {
            val time = formatHour12(hero.hourKey, isArabic)
            val subtitle = if (hero.isBooked && hero.teacherName.isNotBlank()) "$time · ${hero.teacherName}" else time
            val chips = if (hero.isBooked) {
                listOfNotNull(
                    confirmedLabel,
                    match?.first?.let { if (it.isOnline) onlineLabel else offlineLabel }
                )
            } else emptyList()
            SessionHeroCard(
                label = nextLabel,
                title = trValue(hero.subject),
                subtitle = subtitle,
                brush = Brush.linearGradient(listOf(AuroraDeep, AuroraMid, AuroraEnd)),
                glow = AuroraGlow,
                actionTextColor = AuroraAccent,
                iconRes = R.drawable.ic3d_calendar_blue,
                chips = chips,
                actionText = if (hero.isBooked) null else doneLabel,
                onAction = {
                    appState.toggleStudentDailyStudyCompletion(student.id, hero.dateKey, hero.hourKey, hero.value)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HubTileCard(
                title = tr("جدولي", "Schedule"),
                iconRes = R.drawable.ic3d_calendar_blue,
                top = AuroraSkyTop,
                bottom = AuroraSkyBottom,
                onClick = onOpenSchedule,
                modifier = Modifier.weight(1f)
            )
            HubTileCard(
                title = tr("موادي", "Subjects"),
                iconRes = R.drawable.ic3d_cap_blue,
                top = AuroraLilacTop,
                bottom = AuroraLilacBottom,
                onClick = onOpenSubjects,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==================== كارت الحصة الجاية لولي الأمر ====================
@Composable
fun ParentHeroCard(
    children: List<StudentProfile>,
    allSchedules: List<ScheduleSlot>,
    allTeachers: List<TeacherProfile>,
    appState: MrPrivateState
) {
    if (rememberShowSkeleton(appState)) {
        SkeletonBox(Modifier.fillMaxWidth().height(230.dp), RoundedCornerShape(30.dp))
        Spacer(modifier = Modifier.height(12.dp))
    } else if (children.isNotEmpty()) {
        ParentHeroCardContent(children, allSchedules, allTeachers, appState)
    }
}

@Composable
private fun ParentHeroCardContent(
    children: List<StudentProfile>,
    allSchedules: List<ScheduleSlot>,
    allTeachers: List<TeacherProfile>,
    appState: MrPrivateState
) {
    val isArabic = LocalIsArabic.current
    val now = rememberNowMillis()
    val minuteKey = now / 60_000L
    val pick = remember(children, minuteKey) {
        children
            .mapNotNull { c ->
                pickNextHeroSession(c, now, includeFutureDays = true, bookedOnly = true)?.let { c to it }
            }
            .minWithOrNull(
                compareBy<Pair<StudentProfile, HeroSession>>({ it.second.dateKey }, { it.second.hourKey })
            )
    }
    val teachersById by remember(allTeachers) {
        derivedStateOf { allTeachers.associateBy { it.id } }
    }
    val match by remember(pick?.first?.id, pick?.second?.hourKey, pick?.second?.value) {
        derivedStateOf {
            if (pick != null) {
                findBookedSlot(pick.first, pick.second.hourKey, pick.second.value, allSchedules, teachersById)
            } else null
        }
    }
    var showExcuse by remember { mutableStateOf(false) }
    val nextLabel = tr("الحصة الجاية", "Next session")
    val excuseLabel = tr("الاعتذار عن الحصة للمعلم", "Apologize for Session")
    val confirmedLabel = tr("● مؤكد (Confirmed)", "● Confirmed")
    val onlineLabel = tr("أونلاين", "Online")
    val offlineLabel = tr("حضوري", "Offline")

    if (pick != null) {
        val child = pick.first
        val hero = pick.second
        val time = formatHour12(hero.hourKey, isArabic)
        val dayPart = if (hero.dateKey == getTodayDateKey()) "" else dateKeyTitle(hero.dateKey, isArabic) + " · "
        val subtitle = dayPart + time + if (hero.teacherName.isNotBlank()) " · ${hero.teacherName}" else ""
        val chips = listOfNotNull(
            confirmedLabel,
            match?.first?.let { if (it.isOnline) onlineLabel else offlineLabel }
        )
        SessionHeroCard(
            label = "$nextLabel · ${child.name}",
            title = trValue(hero.subject),
            subtitle = subtitle,
            brush = Brush.linearGradient(listOf(ParentDeep, ParentMid, ParentEnd)),
            glow = ParentGlow,
            actionTextColor = ParentAccent,
            iconRes = R.drawable.ic3d_calendar_blue,
            chips = chips,
            actionText = if (match != null) excuseLabel else null,
            onAction = { showExcuse = true }
        )
        Spacer(modifier = Modifier.height(12.dp))
        val m = match
        if (showExcuse && m != null) {
            ParentExcuseDialog(
                slot = m.first,
                enroll = m.second,
                appState = appState,
                onDismiss = { showExcuse = false }
            )
        }
    }
}

/** نفس نافذة الاعتذار الحالية في ChildBookingsList بنفس النصوص والمنطق */
@Composable
fun ParentExcuseDialog(
    slot: ScheduleSlot,
    enroll: StudentEnrollment,
    appState: MrPrivateState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var excuseReason by remember { mutableStateOf(enroll.parentExcuseNote) }
    val excuseSentMsg = tr("تم إرسال الاعتذار للمعلم مع استمرار اشتراككم في المجموعة ✓", "Excuse sent; group subscription remains active ✓")
    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(tr("الاعتذار عن الحصة للمعلم", "Apologize for Session"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tr(
                            "ℹ️ تنويه: الاعتذار يُسجَّل للحصة القادمة فقط ولا يُلغي اشتراك الطالب في المجموعة.",
                            "Note: Excusing applies to the upcoming session only and keeps your group subscription active."
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = excuseReason,
                    onValueChange = { excuseReason = it },
                    label = { Text(tr("سبب الاعتذار (يظهر للمعلم)", "Reason for excuse")) },
                    placeholder = { Text(tr("مثال: ظرف عائلي / مرض الطالب...", "e.g., Family reason / illness")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
                    Button(
                        onClick = {
                            val finalNote = excuseReason.trim().ifBlank { "اعتذار من ولي الأمر عن حضور الحصة القادمة" }
                            appState.submitParentExcuseForSession(slot, enroll.id, finalNote)
                            Toast.makeText(
                                context,
                                excuseSentMsg,
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)
                    ) {
                        Text(tr("إرسال الاعتذار", "Submit"), color = PureWhite)
                    }
                }
            }
        }
    }
}
