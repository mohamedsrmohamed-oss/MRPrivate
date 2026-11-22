@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.firebase.database.*
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

// ==================== 11. شاشة انتظار قبول المعلم ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPendingApprovalScreen(teacher: TeacherProfile, appState: MrPrivateState? = null, onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("حالة حساب المعلم", "Teacher Account Status"), color = PureWhite, fontWeight = FontWeight.Bold) },
                actions = {
                    LanguageSwitcherButton(appState = appState)
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "خروج", tint = PureWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = DarkPurple, modifier = Modifier.size(90.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = tr("مرحباً بك ${teacher.fullName}", "Welcome ${teacher.fullName}"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = LightPurple), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tr("حسابك قيد المراجعة والاعتماد من قِبل إدارة المنصة (Admin)", "Your account is pending approval by the Platform Admin"),
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkPurple, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            WhatsAppSupportButton()
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text(tr("تسجيل الخروج", "Logout"), color = DarkPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== 13. لوحة تحكم المعلم (مع التكرار على الترم والحضور والغياب وتعديل المواعيد) ====================
@Composable
fun CompleteSessionDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, String) -> Unit
) {
    var attended by remember { mutableStateOf(true) }
    var stars by remember { mutableIntStateOf(5) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(tr("إنهاء الحصة وتقييم الطالب ($studentName)", "Complete Session ($studentName)"), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkPurple)
                Spacer(modifier = Modifier.height(10.dp))
                Text(tr("تحديد حضور الطالب في الحصة:", "Student Attendance:"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (attended) Color(0xFF2E7D32) else ElkhetaInputBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (attended) Color(0xFF2E7D32) else ElkhetaInputBorder),
                        modifier = Modifier.weight(1f).clickable { attended = true }
                    ) {
                        Text(tr("حضر ✓", "Attended ✓"), color = if (attended) PureWhite else ElkhetaTextDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Surface(
                        color = if (!attended) Color(0xFFB91C1C) else ElkhetaInputBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!attended) Color(0xFFB91C1C) else ElkhetaInputBorder),
                        modifier = Modifier.weight(1f).clickable { attended = false }
                    ) {
                        Text(tr("لم يحضر (غائب) ✗", "Absent ✗"), color = if (!attended) PureWhite else ElkhetaTextDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (attended) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null, tint = GoldStar,
                                modifier = Modifier.size(32.dp).clickable { stars = i }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("ملاحظة خاصة بولي الأمر فقط (سرية)", "Parent Private Note")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("إلغاء", "Cancel")) }
                    Button(onClick = { onSave(attended, stars, note) }, colors = ButtonDefaults.buttonColors(containerColor = DarkPurple)) {
                        Text(tr("حفظ وإنهاء", "Save"), color = PureWhite)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    teacher: TeacherProfile,
    schedules: List<ScheduleSlot>,
    appState: MrPrivateState? = null,
    onAddSchedule: (ScheduleSlot) -> Unit,
    onConfirmStudent: (ScheduleSlot, String) -> Unit,
    onCompleteStudentSession: (ScheduleSlot, String, Int, String) -> Unit = { _, _, _, _ -> },
    onUpdateAccountPhoto: ((String, String, String) -> Unit)? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = LocalIsArabic.current

    // التحقق من صلاحية اشتراك الترم الدراسي
    val isInteractiveAllowed = teacher.isInteractiveActive
    val isPremiumActive = teacher.effectiveIsPremium
    val isUltimateActive = teacher.effectiveIsUltimate

    // 0: الإدارة والاشتراكات | 1: الكويزات والاختبارات | 2: المواعيد والطلاب
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var showUltimateSupportDialog by remember { mutableStateOf(false) }
    var quizToViewResults by remember { mutableStateOf<QuizExam?>(null) }
    var quizToReassign by remember { mutableStateOf<QuizExam?>(null) }
    var slotToEdit by remember { mutableStateOf<ScheduleSlot?>(null) }
    var slotForExtraSession by remember { mutableStateOf<ScheduleSlot?>(null) }
    var studentToComplete by remember { mutableStateOf<Pair<ScheduleSlot, StudentEnrollment>?>(null) }

    val teacherQuizzes = appState?.quizzes?.filter { it.teacherId == teacher.id } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("لوحة تحكم المعلم", "Teacher Dashboard"), color = PureWhite, fontWeight = FontWeight.Bold) },
                actions = {
                    LanguageSwitcherButton(appState = appState)
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = "خروج", tint = PureWhite) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TeacherPurple)
            )
        },
        floatingActionButton = {
            if (isInteractiveAllowed) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = TeacherPurple,
                    contentColor = PureWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة موعد", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. اللافتة الرئيسية البارزة في الأعلى (Wide Rectangular Card)
            Card(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(TeacherPurpleDark, TeacherPurpleEnd)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            size = 66,
                            photoUriString = teacher.photoUriString,
                            onPhotoChange = if (onUpdateAccountPhoto != null) { uri ->
                                val b64 = uriToBase64(context, uri)
                                if (b64.isNotEmpty()) onUpdateAccountPhoto("teacher", teacher.id, b64)
                            } else null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${teacher.fullName} (${trValue(teacher.educationTrack)})",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isInteractiveAllowed) {
                                val badgeTitle = if (isUltimateActive) {
                                    tr("👑 بروفايل معلم - اشتراك شامل مفعل", "👑 Ultimate Teacher Active")
                                } else {
                                    tr("👑 بروفايل معلم بريميوم مفعل", "👑 Premium Teacher Active")
                                }
                                Surface(color = PureWhite, shape = RoundedCornerShape(20.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = TeacherPurple, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(badgeTitle, color = TeacherPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                Surface(color = PureWhite.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        text = tr("الوضع الأساسي (بروفايل مرئي فقط)", "Basic Mode (View Only)"),
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${trValue(teacher.subject)} | ${teacher.stages.joinToString(" - ") { translateValue(it, isArabic) }}",
                                color = PureWhite.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            val areasLabel = (listOf(teacher.area) + (if (isPremiumActive) teacher.extraAreas.take(2) else emptyList()))
                                .filter { it.isNotBlank() }
                                .joinToString(" • ") { translateValue(it, isArabic) }
                            Text("${trValue(teacher.governorate)}: $areasLabel", color = Color(0xFFFFE082), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            StarRatingBar(rating = teacher.averageRating, count = teacher.ratings.size)
                        }
                    }

                    // لوحة تحفيزية لتجديد الاشتراك عند انتهاء اشتراك الترم
                    if (!isInteractiveAllowed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = tr(
                                        "جدد اشتراكك الآن لتستعيد كامل ميزات Premium أو Ultimate وتبهر طلابك من جديد.",
                                        "Renew your subscription now to restore all Premium or Ultimate features and amaze your students again."
                                    ),
                                    color = TeacherPurpleDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "PREMIUM", isArabic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr("تجديد بريميوم", "Renew Premium"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { openTeacherRenewalWhatsApp(context, teacher.fullName, "ULTIMATE", isArabic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurpleDark),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr("تجديد اشتراك شامل", "Renew Ultimate"), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. مربعات دائرية ثلاثية الأبعاد (3D Rounded Squares) للتنقل بين التبويبات الثلاثة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThreeDRoundedSquareTab(
                    title = tr("الإدارة والاشتراكات", "Admin & Plans"),
                    icon = Icons.Default.AdminPanelSettings,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_shield_purple
                )
                ThreeDRoundedSquareTab(
                    title = tr("الكويزات والاختبارات", "Quizzes & Library"),
                    icon = Icons.Default.Quiz,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_tools_purple
                )
                ThreeDRoundedSquareTab(
                    title = tr("المواعيد والطلاب", "Schedule & Students"),
                    icon = Icons.Default.EventAvailable,
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f),
                    accent = TeacherPurple,
                    accentDark = TeacherPurpleDark,
                    softBg = TeacherPurpleSoft,
                    iconRes = R.drawable.ic3d_calendar_purple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. شبكة أيقونات صغيرة دقيقة (Small Glyph Icons) بالأحمر والأبيض للإجراءات السريعة
            AppCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SmallRedWhiteGlyphAction(
                        title = tr("إضافة موعد", "Add Slot"),
                        icon = Icons.Default.AddCircleOutline,
                        enabled = isInteractiveAllowed,
                        onClick = { showAddDialog = true }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("إنشاء اختبار", "Create Quiz"),
                        icon = Icons.Default.PostAdd,
                        enabled = isInteractiveAllowed,
                        onClick = {
                            if (isUltimateActive) showCreateQuizDialog = true
                            else showUltimateSupportDialog = true
                        }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("الحضور والتقييم", "Attendance"),
                        icon = Icons.Default.FactCheck,
                        enabled = isInteractiveAllowed,
                        onClick = { selectedTab = 2 }
                    )
                    SmallRedWhiteGlyphAction(
                        title = tr("التقارير والدرجات", "Reports"),
                        icon = Icons.Default.Assessment,
                        enabled = isInteractiveAllowed,
                        onClick = { selectedTab = 1 }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== محتوى التبويبات الثلاثة ====================
            when (selectedTab) {
                // التبويب الأول: الإدارة والاشتراكات
                0 -> {
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
                                onClick = {
                                    if (isUltimateActive) showCreateQuizDialog = true
                                    else showUltimateSupportDialog = true
                                },
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

                // التبويب الثاني: الكويزات والاختبارات ومكتبة الاختبارات
                1 -> {
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
                            onClick = {
                                if (isUltimateActive) showCreateQuizDialog = true
                                else showUltimateSupportDialog = true
                            },
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
                                                onClick = { quizToViewResults = q },
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
                                        onClick = { quizToReassign = q },
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

                // التبويب الثالث: المواعيد والطلاب والحصص الإضافية
                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("المواعيد المتاحة والطلاب المسجلين (${schedules.size})", "Schedules & Enrolled Students (${schedules.size})"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TeacherPurpleDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    schedules.forEach { slot ->
                        val typeText = if (slot.isPrivate) tr("برايفت", "Private") else tr("جروب (${slot.enrollments.size}/${slot.maxStudents})", "Group (${slot.enrollments.size}/${slot.maxStudents})")
                        val locText = if (slot.isOnline) tr("أونلاين", "Online") else tr("حضوري", "Offline")

                        AppCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).shadow(4.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${trValue(slot.day)} - ${slot.time}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TeacherPurpleDark)
                                            if (slot.isExtraSession) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(color = TeacherPurpleSoft, shape = RoundedCornerShape(6.dp)) {
                                                    Text("Extra Session", color = TeacherPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        if (slot.stage.isNotBlank() || slot.grade.isNotBlank()) {
                                            Text("${trValue(slot.stage)} • ${slot.grade}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = TeacherPurple, shape = RoundedCornerShape(8.dp)) {
                                            Text("$typeText - $locText", color = PureWhite, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                        if (isInteractiveAllowed) {
                                            IconButton(onClick = { slotToEdit = slot }) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل الموعد", tint = TeacherPurple)
                                            }
                                            val cancelNotifMsg = tr("تم إلغاء الحصة وإرسال إشعار للطلاب وأولياء الأمور", "Session cancelled & notified")
                                            IconButton(onClick = {
                                                appState?.cancelScheduleByTeacher(slot)
                                                Toast.makeText(context, cancelNotifMsg, Toast.LENGTH_LONG).show()
                                            }) {
                                                Icon(Icons.Default.EventBusy, contentDescription = "إلغاء الحصة", tint = Color(0xFFB91C1C))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (slot.isExtraSession)
                                        tr("السعر: ${slot.price} جنيه | ⭐ حصة إضافية (Extra Session)", "Price: ${slot.price} EGP | Extra Session")
                                    else
                                        tr("السعر: ${slot.price} جنيه | 🔄 يتكرر على الترم الدراسي فقط (${slot.recurringDates.size.coerceAtLeast(1)} أسبوع)", "Price: ${slot.price} EGP | Term Recurring (${slot.recurringDates.size.coerceAtLeast(1)} Weeks)"),
                                    fontSize = 12.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.Bold
                                )

                                // زر إضافة حصة إضافية (Extra Session) لنفس المجموعة
                                if (isInteractiveAllowed && !slot.isExtraSession) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { slotForExtraSession = slot },
                                        border = BorderStroke(1.dp, TeacherPurple),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.MoreTime, contentDescription = null, tint = TeacherPurple, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("إضافة حصة إضافية (Extra Session) لنفس المجموعة", "Add Extra Session for Group"), color = TeacherPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (slot.enrollments.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    slot.enrollments.forEach { enroll ->
                                        val stObj = appState?.students?.find { it.id == enroll.studentId }
                                        val parentObj = appState?.students?.find { it.id == stObj?.parentId }
                                        val pName = enroll.parentName.ifBlank { parentObj?.name ?: "ولي أمر الطالب" }
                                        val pPhone = enroll.parentPhone.ifBlank { parentObj?.phone ?: enroll.studentPhone }
                                        val sGrade = enroll.studentGrade.ifBlank { stObj?.grade?.ifBlank { slot.grade.ifBlank { stObj.stage } } ?: slot.grade }

                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(ElkhetaInputBg, RoundedCornerShape(10.dp)).padding(10.dp)
                                        ) {
                                            Text(tr("ولي الأمر: $pName  |  📞 $pPhone", "Parent: $pName | 📞 $pPhone"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TeacherPurpleDark)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(tr("↳ الطالب: ${enroll.studentName}  |  الصف: ${sGrade.ifBlank { "غير محدد" }}", "↳ Student: ${enroll.studentName} | Grade: $sGrade"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextDark)
                                            if (enroll.parentExcuseNote.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFF97316))) {
                                                    Text(
                                                        text = tr("✋ اعتذار من ولي الأمر عن الحصة: ${enroll.parentExcuseNote}", "✋ Parent Excuse: ${enroll.parentExcuseNote}"),
                                                        color = Color(0xFFC2410C),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!enroll.isConfirmed) {
                                                    Button(
                                                        onClick = { onConfirmStudent(slot, enroll.id) },
                                                        enabled = isInteractiveAllowed,
                                                        colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
                                                    ) {
                                                        Text(tr("تأكيد وقبول", "Confirm"), color = PureWhite, fontSize = 11.sp)
                                                    }
                                                } else if (!enroll.isCompleted) {
                                                    Button(
                                                        onClick = { studentToComplete = slot to enroll },
                                                        enabled = isInteractiveAllowed,
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                                    ) {
                                                        Text(tr("إنهاء الحصة (حضور/غياب)", "Complete & Attendance"), color = PureWhite, fontSize = 11.sp)
                                                    }
                                                } else {
                                                    val statusTxt = if (enroll.attended) tr("حضر ✓ (${enroll.teacherRatingForStudent}★)", "Present ✓") else tr("لم يحضر (غائب) ✗", "Absent ✗")
                                                    val statusClr = if (enroll.attended) Color(0xFF2E7D32) else Color(0xFFB91C1C)
                                                    Text(statusTxt, color = statusClr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }

                                                if (isInteractiveAllowed) {
                                                    IconButton(
                                                        onClick = {
                                                            appState?.removeStudentFromSlot(slot, enroll.id)
                                                            Toast.makeText(context, "تم حذف الطالب من المجموعة", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف الطالب", tint = Color(0xFFB91C1C))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة توجيه المعلم لطلب التفعيل من الدعم (ميزة اشتراك شامل / Ultimate)
    if (showUltimateSupportDialog) {
        AlertDialog(
            onDismissRequest = { showUltimateSupportDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TeacherPurple) },
            title = { Text(tr("ميزة مخصصة لباقة اشتراك شامل", "Ultimate Package Feature"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    tr(
                        "ميزة إنشاء الاختبارات وتصحيحها بالذكاء الاصطناعي مفعلة لمشتركي باقة (اشتراك شامل). يرجى طلب التفعيل من الدعم الفني.",
                        "AI Quiz Creation is exclusively enabled for Ultimate subscribers. Please request activation from Support."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUltimateSupportDialog = false
                        openTeacherRenewalWhatsApp(context, teacher.fullName, "ULTIMATE", isArabic)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
                ) {
                    Text(tr("طلب التفعيل من الدعم", "Request Activation"), color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUltimateSupportDialog = false }) { Text(tr("إغلاق", "Close")) }
            }
        )
    }

    // نافذة إعادة إرسال الاختبار المحفوظ وتعيينه لمجموعات متعددة
    quizToReassign?.let { sourceQuiz ->
        var selectedSlotIds by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(
            onDismissRequest = { quizToReassign = null },
            title = { Text(tr("تعيين الاختبار: ${sourceQuiz.title}", "Assign Quiz: ${sourceQuiz.title}"), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(tr("اختر المجموعات المراد إرسال الاختبار إليها:", "Select groups to receive this quiz:"), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    schedules.forEach { slot ->
                        val label = "${slot.grade} (${slot.day} - ${slot.time})"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedSlotIds = if (selectedSlotIds.contains(slot.id)) selectedSlotIds - slot.id else selectedSlotIds + slot.id
                            }
                        ) {
                            Checkbox(
                                checked = selectedSlotIds.contains(slot.id),
                                onCheckedChange = { checked ->
                                    selectedSlotIds = if (checked) selectedSlotIds + slot.id else selectedSlotIds - slot.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = TeacherPurple)
                            )
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                val sendQuizSuccessMsg = tr("تم إرسال الاختبار إلى %d مجموعة بنجاح!", "Quiz assigned to %d groups!")
                Button(
                    onClick = {
                        val targets = schedules.filter { selectedSlotIds.contains(it.id) }
                        if (targets.isNotEmpty()) {
                            appState?.reassignSavedQuizToGroups(sourceQuiz, targets)
                            val msg = sendQuizSuccessMsg.replace("%d", targets.size.toString())
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        quizToReassign = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
                ) {
                    Text(tr("إرسال للمجموعات المحددة", "Send to Selected"), color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { quizToReassign = null }) { Text(tr("إلغاء", "Cancel")) }
            }
        )
    }

    // نافذة إضافة حصة إضافية (Extra Session) لنفس المجموعة
    slotForExtraSession?.let { baseSlot ->
        var extraDay by remember { mutableStateOf(baseSlot.day) }
        var extraTime by remember { mutableStateOf("") }
        var extraError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { slotForExtraSession = null },
            title = { Text(tr("إضافة حصة إضافية (Extra Session)", "Add Extra Session"), fontWeight = FontWeight.Bold, color = TeacherPurpleDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("المجموعة: ${baseSlot.stage} - ${baseSlot.grade}", "Group: ${baseSlot.stage} - ${baseSlot.grade}"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    CustomDropdown(tr("اليوم *", "Day *"), daysOfWeek, extraDay) {
                        extraDay = it
                        extraError = ""
                    }
                    OutlinedTextField(
                        value = extraTime,
                        onValueChange = { extraTime = it; extraError = "" },
                        label = { Text(tr("الساعة * (مثال: 07:00 مساءً)", "Time *")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (extraError.isNotEmpty()) {
                        Text(extraError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                val extraSessionSuccessMsg = tr("تمت إضافة الحصة الإضافية وإرسال إشعار للطلاب", "Extra session added & notified")
                Button(
                    onClick = {
                        if (extraDay.isBlank() || extraTime.isBlank()) {
                            extraError = "يرجى إدخال اليوم والساعة للحصة الإضافية"
                        } else {
                            val err = appState?.addExtraSessionForGroup(baseSlot, extraDay, extraTime)
                            if (err != null) {
                                extraError = err
                            } else {
                                Toast.makeText(context, extraSessionSuccessMsg, Toast.LENGTH_LONG).show()
                                slotForExtraSession = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeacherPurple)
                ) {
                    Text(tr("إضافة وإشعار الطلاب", "Add & Notify"), color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { slotForExtraSession = null }) { Text(tr("إلغاء", "Cancel")) }
            }
        )
    }

    if (showCreateQuizDialog) {
        val quizSavedMsg = tr("تم حفظ الكويز في مكتبتك وإرساله لطلاب المجموعة!", "Quiz saved to library & sent!")
        CreateQuizFromPaperDialog(
            teacher = teacher,
            schedules = schedules,
            appState = appState,
            onDismiss = { showCreateQuizDialog = false },
            onCreateQuiz = { newQuiz ->
                appState?.createQuizExam(newQuiz)
                Toast.makeText(context, quizSavedMsg, Toast.LENGTH_LONG).show()
                showCreateQuizDialog = false
            }
        )
    }
    quizToViewResults?.let { q ->
        TeacherQuizResultsDialog(
            quiz = q,
            onDismiss = { quizToViewResults = null }
        )
    }
    if (showAddDialog) {
        AddScheduleDialog(
            teacherId = teacher.id,
            initialSlot = null,
            appState = appState,
            onDismiss = { showAddDialog = false },
            onSave = { onAddSchedule(it); showAddDialog = false }
        )
    }
    slotToEdit?.let { editing ->
        val scheduleUpdatedMsg = tr("تم تعديل الموعد وإرسال إشعار وتحديث جداول الطلاب", "Schedule updated & notified")
        AddScheduleDialog(
            teacherId = teacher.id,
            initialSlot = editing,
            appState = appState,
            onDismiss = { slotToEdit = null },
            onSave = { updated ->
                val err = appState?.updateScheduleByTeacher(editing, updated)
                if (err != null) {
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, scheduleUpdatedMsg, Toast.LENGTH_LONG).show()
                    slotToEdit = null
                }
            }
        )
    }
    studentToComplete?.let { (slot, enroll) ->
        CompleteSessionDialog(
            studentName = enroll.studentName,
            onDismiss = { studentToComplete = null },
            onSave = { attended, stars, note ->
                if (appState != null) {
                    appState.completeStudentSession(slot, enroll.id, stars, note, attended)
                } else {
                    onCompleteStudentSession(slot, enroll.id, stars, note)
                }
                studentToComplete = null
            }
        )
    }
}

@Composable
fun AddScheduleDialog(
    teacherId: String,
    initialSlot: ScheduleSlot? = null,
    appState: MrPrivateState? = null,
    onDismiss: () -> Unit,
    onSave: (ScheduleSlot) -> Unit
) {
    // عند الإضافة الجديدة تكون جميع الحقول فارغة تماماً وإجبارية
    var selectedStage by remember { mutableStateOf(initialSlot?.stage ?: "") }
    val availableGrades = stageGradesMap[selectedStage] ?: emptyList()
    var selectedGrade by remember(selectedStage) {
        mutableStateOf(if (initialSlot?.stage == selectedStage) initialSlot.grade else "")
    }
    var selectedDay by remember { mutableStateOf(initialSlot?.day ?: "") }
    var time by remember { mutableStateOf(initialSlot?.time ?: "") }
    var isPrivate by remember { mutableStateOf(initialSlot?.isPrivate ?: false) }
    var isOnline by remember { mutableStateOf(initialSlot?.isOnline ?: false) }
    var maxStudents by remember { mutableStateOf(initialSlot?.maxStudents?.toString() ?: "") }
    var price by remember { mutableStateOf(initialSlot?.price ?: "") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (initialSlot == null) tr("إضافة موعد حصة (على الترم الدراسي)", "Add Term Schedule") else tr("تعديل موعد الحصة", "Edit Schedule"),
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DarkPurple
                )
                Text(tr("جميع الحقول إجبارية *", "All fields are mandatory *"), fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                CustomDropdown(tr("المرحلة التعليمية *", "Stage *"), educationalStages, selectedStage) {
                    selectedStage = it
                    selectedGrade = ""
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                CustomDropdown(tr("الصف الدراسي *", "Grade *"), availableGrades, selectedGrade) {
                    selectedGrade = it
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                CustomDropdown(tr("اليوم *", "Day *"), daysOfWeek, selectedDay) {
                    selectedDay = it
                    errorMsg = ""
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it; errorMsg = "" },
                    label = { Text(tr("الساعة * (مثال: 05:00 مساءً)", "Time *")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isPrivate, onClick = { isPrivate = false; if (maxStudents == "1") maxStudents = "" })
                    Text(tr("جروب", "Group"))
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = isPrivate, onClick = { isPrivate = true; maxStudents = "1" })
                    Text(tr("برايفت", "Private"))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isOnline, onClick = { isOnline = false })
                    Text(tr("حضوري", "Offline"))
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(selected = isOnline, onClick = { isOnline = true })
                    Text(tr("أونلاين", "Online"))
                }

                if (!isPrivate) {
                    OutlinedTextField(
                        value = maxStudents,
                        onValueChange = { maxStudents = it.filter { c -> c.isDigit() }; errorMsg = "" },
                        label = { Text(tr("الحد الأقصى للطلاب *", "Max Students *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() }; errorMsg = "" },
                    label = { Text(tr("السعر (بالجنيه) *", "Price (EGP) *")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Elkheta3DButton(
                    text = if (initialSlot == null) tr("حفظ وتكرار على الترم الدراسي", "Save Term Schedule") else tr("حفظ تعديل الموعد", "Save Changes"),
                    onClick = {
                        val parsedMax = if (isPrivate) 1 else maxStudents.toIntOrNull()
                        if (selectedStage.isBlank() || selectedGrade.isBlank() || selectedDay.isBlank() || time.isBlank() || price.isBlank() || parsedMax == null || parsedMax <= 0) {
                            errorMsg = "يرجى ملء جميع الحقول الإجبارية أولاً!"
                        } else {
                            val slotObj = (initialSlot ?: ScheduleSlot(teacherId = teacherId)).copy(
                                stage = selectedStage,
                                grade = selectedGrade,
                                day = selectedDay,
                                time = time.trim(),
                                maxStudents = parsedMax,
                                price = price.trim(),
                                isPrivate = isPrivate,
                                isOnline = isOnline,
                                isRecurringYearly = true,
                                academicYearWeeks = 14
                            )
                            val conflict = if (initialSlot != null && appState != null) {
                                appState.checkGroupConflictForSlotEdit(initialSlot, slotObj.day, slotObj.time)
                            } else null
                            if (conflict != null) {
                                errorMsg = conflict
                            } else {
                                onSave(slotObj)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
