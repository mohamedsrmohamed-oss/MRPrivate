@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrprivate.MrPrivateState

// ==================== 1. الهوية البصرية ونظام التصميم الحديث (Elkheta Infographic UI) ====================
val DarkPurple = Color(0xFF4A148C)
val MediumPurple = Color(0xFF6A1B9A)
val LightPurple = Color(0xFFF3E5F5)
val PureWhite = Color(0xFFFFFFFF)
val GoldStar = Color(0xFFFFB300)
val WhatsAppGreen = Color(0xFF25D366)

val ElkhetaCyan = Color(0xFF00A3FF)
val ElkhetaCyanShadow = Color(0xFF0077BA)
val ElkhetaGreenShadow = Color(0xFF1DA851)
val ElkhetaPurpleShadow = Color(0xFF2E0854)
val ElkhetaTealHeader = Color(0xFF1E9E99)
val ElkhetaSoftBgTop = Color(0xFFF2F8FC)
val ElkhetaSoftBgBottom = Color(0xFFFFFDF4)
val ElkhetaInputBg = Color(0xFFF5F7FA)
val ElkhetaInputBorder = Color(0xFFE2E8F0)
val ElkhetaTextDark = Color(0xFF1E293B)
val ElkhetaTextGray = Color(0xFF64748B)

val MrRedPrimary = Color(0xFFD32F2F)
val MrRedDark = Color(0xFF9A0007)
val MrRedSoftBg = Color(0xFFFFEBEE)

data class SubjectVisualStyle(
    val accentColor: Color,
    val tileGradient: List<Color>,
    val symbolText: String,
    val icon: ImageVector
)

fun getSubjectVisualStyle(subjectName: String): SubjectVisualStyle {
    return when {
        subjectName.contains("عرب") || subjectName.equals("Arabic", true) -> SubjectVisualStyle(
            accentColor = Color(0xFF6366F1),
            tileGradient = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
            symbolText = "ع",
            icon = Icons.Default.MenuBook
        )
        subjectName.contains("إنجليز") || subjectName.equals("English", true) -> SubjectVisualStyle(
            accentColor = Color(0xFF0284C7),
            tileGradient = listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
            symbolText = "EN",
            icon = Icons.Default.MenuBook
        )
        subjectName.contains("علوم") || subjectName.contains("فيزياء") || subjectName.contains("كيمياء") || subjectName.contains("أحياء") ||
                subjectName.equals("Biology", true) || subjectName.equals("Science", true) || subjectName.equals("Physics", true) || subjectName.equals("Chemistry", true) -> SubjectVisualStyle(
            accentColor = Color(0xFF22C55E),
            tileGradient = listOf(Color(0xFF16A34A), Color(0xFF15803D)),
            symbolText = "🧪",
            icon = Icons.Default.Science
        )
        subjectName.contains("رياض") || subjectName.equals("Math", true) -> SubjectVisualStyle(
            accentColor = Color(0xFFF59E0B),
            tileGradient = listOf(Color(0xFFD97706), Color(0xFFB45309)),
            symbolText = "±",
            icon = Icons.Default.Calculate
        )
        subjectName.contains("برمج") || subjectName.contains("حاسب") ||
                subjectName.equals("Programming", true) || subjectName.equals("Computer Science", true) -> SubjectVisualStyle(
            accentColor = Color(0xFF8B5CF6),
            tileGradient = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
            symbolText = "</>",
            icon = Icons.Default.Computer
        )
        else -> SubjectVisualStyle(
            accentColor = Color(0xFFEC4899),
            tileGradient = listOf(Color(0xFF9D4EDD), Color(0xFF6A1B9A)),
            symbolText = "🌍",
            icon = Icons.Default.Public
        )
    }
}

// ==================== 4. إدارة الحالة والربط الهجين الكامل مع Firebase ====================
data class GradientTheme(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val colors: List<Color>,
    val primaryColor: Color
)

val appGradientThemes = listOf(
    GradientTheme(0, "الأزرق والبنفسجي الحديث", "Modern Cyan & Purple", listOf(Color(0xFF2E0854), Color(0xFF4A148C), Color(0xFF00A3FF)), Color(0xFF4A148C)),
    GradientTheme(1, "الكحلي الليلي", "Night Navy", listOf(Color(0xFF0A1128), Color(0xFF1C2541), Color(0xFF3A506B)), Color(0xFF1C2541)),
    GradientTheme(2, "الزمردي الفيروزي", "Teal Emerald", listOf(Color(0xFF062C30), Color(0xFF05595B), Color(0xFF1E9E99)), Color(0xFF1E9E99)),
    GradientTheme(3, "العنابي الملكي", "Royal Burgundy", listOf(Color(0xFF370617), Color(0xFF6A040F), Color(0xFF9D0208)), Color(0xFF6A040F))
)

// ==================== 5. مكونات التصميم الحديث (Elkheta 3D Buttons & Infographic Cards) ====================
@Composable
fun Elkheta3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mainColor: Color = ElkhetaCyan,
    shadowColor: Color = ElkhetaCyanShadow,
    textColor: Color = PureWhite,
    height: Int = 50,
    cornerRadius: Int = 16,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height((height + 4).dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(shadowColor)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(mainColor)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ElkhetaInfographicSubjectCard(
    subjectName: String,
    teacherName: String,
    teacherPhotoUri: String = "",
    progressPercent: Int = 0,
    subtitleInfo: String = "",
    onClick: () -> Unit
) {
    val style = getSubjectVisualStyle(subjectName)
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(6.dp)
                    .height(114.dp)
                    .background(style.accentColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(style.tileGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = style.symbolText, color = PureWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = trValue(subjectName), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = ElkhetaTextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, style.accentColor, CircleShape)
                            ) {
                                ProfileAvatar(size = 28, isDarkBg = false, photoUriString = teacherPhotoUri)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = teacherName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElkhetaTextGray)
                        }
                        if (subtitleInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = subtitleInfo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = style.accentColor)
                        }
                    }
                    Surface(color = style.accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = "$progressPercent%",
                            color = style.accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEFF3F8))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((progressPercent / 100f).coerceIn(0.05f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(style.accentColor)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(35, 70).forEach { milestone ->
                            val reached = progressPercent >= milestone
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (reached) style.accentColor else PureWhite)
                                    .border(1.5.dp, if (reached) style.accentColor else Color(0xFFD8E0EA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (milestone == 35) "🏛️" else "🎁", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ElkhetaBottomNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    items: List<Triple<Int, String, String>>? = null,
    isScrolling: Boolean = false,
    iconRes: Map<Int, Int> = emptyMap()
) {
    val navItems = items ?: listOf(
        Triple(0, tr("جدولي", "Schedule"), "📅"),
        Triple(1, tr("دروسي", "Lessons"), "📕"),
        Triple(2, tr("أستاذك", "Teachers"), "💬"),
        Triple(3, tr("حجوزاتي", "Bookings"), "💰"),
        Triple(4, tr("حسابي", "Account"), "🪪")
    )
    // شريط تنقل سفلي مثبت بوضع الالتصاق التام (Sticky Bottom Navigation) وبخلفية متطابقة مع واجهة التطبيق لمنع أي فراغات بيضاء
    Surface(
        color = ElkhetaSoftBgBottom,
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PureWhite.copy(alpha = 0.96f),
                            ElkhetaSoftBgBottom
                        )
                    )
                )
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ElkhetaInputBorder)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { (index, label, emoji) ->
                    val isSelected = selectedTab == index
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) ElkhetaCyan.copy(alpha = 0.16f) else Color.Transparent
                            )
                            .clickable { onSelectTab(index) }
                            .padding(
                                horizontal = if (isScrolling) 12.dp else 14.dp,
                                vertical = 7.dp
                            )
                            .animateContentSize()
                    ) {
                        val iconResId = iconRes[index]
                        if (iconResId != null) {
                            Icon3D(iconResId, Modifier.size(if (isSelected) 30.dp else 26.dp))
                        } else {
                            Text(text = emoji, fontSize = if (isSelected) 20.sp else 18.sp)
                        }
                        AnimatedVisibility(
                            visible = !isScrolling,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0284C7) else ElkhetaTextDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** زر رجوع عائم في أقصى اليمين بمنتصف الشاشة يتقلص ديناميكياً أثناء التمرير ليظهر السهم فقط */
@Composable
fun RightCenterFloatingBackButton(
    onBack: () -> Unit,
    isScrolling: Boolean = false
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = DarkPurple.copy(alpha = 0.92f),
                shadowElevation = 10.dp,
                border = BorderStroke(1.5.dp, GoldStar),
                modifier = Modifier
                    .padding(end = 0.dp)
                    .animateContentSize()
                    .clickable { onBack() }
            ) {
                Row(
                    modifier = Modifier
                        .padding(
                            horizontal = if (isScrolling) 10.dp else 13.dp,
                            vertical = 10.dp
                        )
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "رجوع",
                        tint = PureWhite,
                        modifier = Modifier.size(19.dp)
                    )
                    AnimatedVisibility(
                        visible = !isScrolling,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tr("رجوع", "Back"),
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BeautifulAppBackground(
    isFullDark: Boolean = false,
    appState: MrPrivateState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = appState?.currentGradientTheme ?: appGradientThemes[0]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isFullDark) {
                    Brush.verticalGradient(theme.colors)
                } else {
                    Brush.verticalGradient(listOf(ElkhetaSoftBgTop, PureWhite, ElkhetaSoftBgBottom))
                }
            )
    ) {
        content()
    }
}

@Composable
fun NoConnectionBanner() {
    Surface(color = Color.Red, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = tr("لا يوجد اتصال بالإنترنت - No Connection", "No Internet Connection"), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun MrPrivateTheme(appState: MrPrivateState, content: @Composable () -> Unit) {
    val isConnected = rememberIsNetworkConnected()
    val focusManager = LocalFocusManager.current
    val direction = if (appState.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val primaryThemeColor = appState.currentGradientTheme.primaryColor

    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalIsArabic provides appState.isArabic,
        LocalToggleLanguage provides { appState.isArabic = !appState.isArabic }
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(primary = primaryThemeColor, onPrimary = PureWhite, surface = PureWhite, background = ElkhetaSoftBgTop)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
                color = ElkhetaSoftBgTop
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isConnected) { NoConnectionBanner() }
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}

@Composable
fun LanguageSwitcherButton(appState: MrPrivateState? = null, isLightMode: Boolean = false) {
    val context = LocalContext.current
    val isArabic = LocalIsArabic.current
    val onToggle = LocalToggleLanguage.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (appState != null) {
            IconButton(onClick = {
                val nextIdx = (appState.currentThemeIndex + 1) % appGradientThemes.size
                appState.currentThemeIndex = nextIdx
                saveThemePref(context, nextIdx)
            }) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "تغيير الثيم",
                    tint = if (isLightMode) DarkPurple else PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        OutlinedButton(
            onClick = onToggle,
            border = BorderStroke(1.5.dp, if (isLightMode) DarkPurple else PureWhite),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isLightMode) PureWhite else MediumPurple.copy(alpha = 0.8f),
                contentColor = if (isLightMode) DarkPurple else PureWhite
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isArabic) "English" else "العربية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==================== مكونات مشتركة (تم توحيد التكرار) ====================

/** كارت أبيض بالتشكيل الافتراضي للتطبيق؛ يمرّر الـ modifier والـ shape كما هما. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = shape,
        content = content
    )
}

/** الكارت الأبيض العائم المستخدم في شاشات الدخول والتسجيل. */
@Composable
fun FloatingCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(26.dp),
        content = content
    )
}

/** كارت اختيار الدور (ولي أمر / معلم). */
@Composable
fun RoleCard(emoji: String, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.5.dp, ElkhetaInputBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(PureWhite),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = ElkhetaTextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = ElkhetaTextGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** الشريط العلوي: زر الرجوع ومبدّل اللغة. */
@Composable
fun ScreenTopBar(appState: MrPrivateState?, backText: String, onBack: () -> Unit) {
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
                text = backText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = ElkhetaTextDark,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        LanguageSwitcherButton(appState = null, isLightMode = true)
    }
}

/** ألوان حقول الإدخال (بدون حدود مخصصة). */
@Composable
fun elkhetaFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = ElkhetaInputBg,
    focusedContainerColor = PureWhite
)

/** ألوان حقول الإدخال مع حدود مخصصة. */
@Composable
fun elkhetaFieldColorsBordered() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = ElkhetaInputBg,
    focusedContainerColor = PureWhite,
    unfocusedBorderColor = ElkhetaInputBorder,
    focusedBorderColor = ElkhetaCyan
)

// ==================== 8. مكونات الهوية البصرية المحدثة (الأحمر والأبيض + 3D Rounded Squares + Wide Cards + Small Glyph Icons) ====================

/** 1. مربعات دائرية ثلاثية الأبعاد (3D Rounded Squares) للأقسام الرئيسية والتنقل بين التبويبات */
@Composable
fun ThreeDRoundedSquareTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MrRedPrimary,
    accentDark: Color = MrRedDark,
    softBg: Color = MrRedSoftBg,
    iconRes: Int? = null
) {
    val bgBrush = if (selected) {
        Brush.verticalGradient(listOf(accent, accentDark))
    } else {
        Brush.verticalGradient(listOf(PureWhite, Color(0xFFF8F9FA)))
    }
    Box(
        modifier = modifier
            .shadow(if (selected) 10.dp else 5.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PureWhite.copy(alpha = 0.65f) else accent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (iconRes != null) 54.dp else 44.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        if (iconRes != null) PureWhite else if (selected) PureWhite.copy(alpha = 0.22f) else softBg,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Icon3D(iconRes, Modifier.size(40.dp))
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (selected) PureWhite else accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (selected) PureWhite else ElkhetaTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** 2. أيقونات صغيرة دقيقة (Small Glyph Icons) بالأحمر والأبيض للإجراءات السريعة */
@Composable
fun SmallRedWhiteGlyphAction(
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.45f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(MrRedPrimary.copy(alpha = alpha), MrRedDark.copy(alpha = alpha))),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = PureWhite, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElkhetaTextDark.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
