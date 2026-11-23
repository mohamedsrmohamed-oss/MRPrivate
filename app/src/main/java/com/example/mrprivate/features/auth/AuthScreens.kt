@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.features.auth

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.mrprivate.MrPrivateState
import com.example.mrprivate.data.model.*
import com.example.mrprivate.data.repository.LoginResult
import com.example.mrprivate.features.admin.AdminDashboardScreen
import com.example.mrprivate.features.student_parent.ParentDashboardScreen
import com.example.mrprivate.features.student_parent.StudentHomeScreen
import com.example.mrprivate.features.student_parent.TeacherScheduleForStudentScreen
import com.example.mrprivate.features.teacher.TeacherDashboardScreen
import com.example.mrprivate.features.teacher.TeacherPendingApprovalScreen
import com.example.mrprivate.ui.components.*

// ==================== 7. شاشة الدخول الحديثة ====================
@Composable
fun LoginScreen(
    isLoading: Boolean,
    appState: MrPrivateState? = null,
    onLogin: (String, String, String, (LoginResult) -> Unit) -> Unit,
    onFirstTimeRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("student") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DarkPurple, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.5.dp, GoldStar)) {
                            Text("MR", color = PureWhite, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Private", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkPurple)
                    }
                }
                LanguageSwitcherButton(appState = null, isLightMode = true)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Elkheta3DButton(
                text = tr("كلّمنا على واتساب", "WhatsApp Support"),
                onClick = { openPlatformSupportWhatsApp(context) },
                modifier = Modifier.fillMaxWidth(),
                mainColor = WhatsAppGreen,
                shadowColor = ElkhetaGreenShadow,
                height = 40,
                cornerRadius = 20
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = tr("ذاكر بانبساط،\nوحسّن مستواك الدراسي", "Study Happily,\nImprove Your Level"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = ElkhetaTextDark,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            FloatingCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔐", fontSize = 30.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr("ابدأ بحسابك", "Sign In to Your Account"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            Triple("student", tr("طالب", "Student"), "🎓"),
                            Triple("teacher", tr("معلم", "Teacher"), "👨‍🏫"),
                            Triple("parent", tr("ولي أمر", "Parent"), "👨‍👩‍👦")
                        )
                        roles.forEach { (roleKey, roleLabel, emoji) ->
                            val isSelected = selectedRole == roleKey
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedRole = roleKey
                                        errorMessage = ""
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) ElkhetaCyan else ElkhetaInputBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE0F2FE).copy(alpha = 0.5f) else ElkhetaInputBg
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                    Text(
                                        text = roleLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF0369A1) else ElkhetaTextDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = tr("اسم المستخدم أو رقم الموبايل", "Username or Phone"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ElkhetaTextDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = "" },
                        placeholder = { Text("a1 / a2 / a3", color = Color.Gray) },
                        trailingIcon = { Text("📱", fontSize = 18.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColorsBordered()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = tr("الباسورد", "Password"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ElkhetaTextDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        placeholder = { Text(tr("اكتب الباسورد (للتجربة: 0)", "Password (demo: 0)"), color = Color.Gray) },
                        leadingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = ElkhetaTextGray
                                )
                            }
                        },
                        trailingIcon = { Text("🔑", fontSize = 18.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColorsBordered()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val emptyErrorMsg = tr("يرجى إدخال اسم المستخدم وكلمة المرور", "Please enter username and password")
                    val invalidErrorMsg = tr("اسم المستخدم أو كلمة المرور غير صحيحة!", "Invalid username or password!")
                    val networkErrorMsg = tr("تعذر الاتصال بالإنترنت، تأكد من الشبكة وحاول مرة أخرى", "No internet connection, please try again")

                    Elkheta3DButton(
                        text = if (busy) tr("جارٍ تسجيل الدخول...", "Signing in...") else tr("تسجيل الدخول ←", "Sign In ←"),
                        onClick = {
                            val isAdminLogin = username.trim().startsWith("*")
                            val loginName = if (isAdminLogin) username.trim().removePrefix("*").trim() else username
                            if (loginName.isBlank() || password.isBlank()) {
                                errorMessage = emptyErrorMsg
                            } else {
                                val roleToLogin = if (isAdminLogin) "admin" else selectedRole
                                if (!busy) {
                                    busy = true
                                    errorMessage = ""
                                    onLogin(roleToLogin, loginName, password) { result ->
                                        busy = false
                                        when (result) {
                                            LoginResult.SUCCESS -> Unit
                                            LoginResult.NETWORK_ERROR -> errorMessage = networkErrorMsg
                                            LoginResult.WRONG_CREDENTIALS -> errorMessage = invalidErrorMsg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        mainColor = ElkhetaCyan,
                        shadowColor = ElkhetaCyanShadow
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Elkheta3DButton(
                text = tr("🚀 سجل مجاناً لأول مرة", "🚀 Register Free"),
                onClick = onFirstTimeRegisterClick,
                modifier = Modifier.fillMaxWidth(),
                mainColor = DarkPurple,
                shadowColor = ElkhetaPurpleShadow
            )
        }
    }
}

// ==================== 8. شاشة التسجيل لأول مرة ====================
@Composable
fun RoleSelectionScreen(
    appState: MrPrivateState? = null,
    onSelectRole: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTopBar(appState = appState, backText = tr("→ عندي حساب", "→ I have an account"), onBack = { onBackToLogin() })

            Spacer(modifier = Modifier.height(20.dp))

            FloatingCard {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋", fontSize = 34.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = tr("أهلاً بيك!", "Welcome!"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Text(
                        text = tr("قبل ما نبدأ – قول لنا إنت مين", "Before we start - tell us who you are"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElkhetaTextGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    RoleCard(
                        emoji = "👨‍👩‍👦",
                        title = tr("ولي أمر", "Parent"),
                        description = tr("هتابع تقدم ابني، أضيف حساب الطالب، وأضبط جدوله ومواده", "Track my child's progress, add student account & schedule"),
                        onClick = { onSelectRole("parent") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RoleCard(
                        emoji = "👨‍🏫",
                        title = tr("معلم", "Teacher"),
                        description = tr("هسجل مواعيد حصصي السنوية وأتابع طلابي وتقييماتهم", "Manage yearly sessions, groups, and student evaluations"),
                        onClick = { onSelectRole("teacher") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = tr("عندك حساب بالفعل؟ .. سجل دخولك", "Already have an account? Sign In"),
                        color = ElkhetaCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
            }
        }
    }
}

// ==================== 9. شاشة تسجيل ولي الأمر المبسطة ====================
@Composable
fun StudentRegistrationScreen(
    isParent: Boolean,
    appState: MrPrivateState? = null,
    checkUsernameTaken: (String) -> Boolean,
    onRegistered: (StudentProfile, String, (AuthError) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val authText = rememberAuthErrorTexts()
    var phone by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("") }
    val availableAreas = governoratesWithAreas[governorate] ?: emptyList()
    var area by remember(governorate) { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    BeautifulAppBackground(isFullDark = false, appState = appState) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenTopBar(appState = appState, backText = tr("→ رجوع", "→ Back"), onBack = { onBack() })

            Spacer(modifier = Modifier.height(14.dp))

            FloatingCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr("ابدأ بحساب ولي الأمر", "Create Parent Account"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ElkhetaTextDark
                    )
                    Text(
                        text = tr("اختر الصورة واكتب اسم المستخدم والباسورد ورقم الواتساب فقط", "Select photo, username, password & WhatsApp phone"),
                        fontSize = 12.sp,
                        color = ElkhetaTextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    CircularImagePicker(photoUri = photoUri, onPhotoSelected = { photoUri = it; errorMsg = "" })
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(tr("اسم المستخدم", "Username"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = "" },
                        placeholder = { Text(tr("اكتب اسم المستخدم", "Enter username")) },
                        trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(tr("الباسورد", "Password"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        placeholder = { Text(tr("اكتب الباسورد", "Enter password")) },
                        leadingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        trailingIcon = { Text("🔑", fontSize = 18.sp) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(tr("رقم الموبايل (واتساب)", "WhatsApp Phone Number"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = ElkhetaTextDark, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = "" },
                        placeholder = { Text("01xxxxxxxxx") },
                        trailingIcon = { Text("📱", fontSize = 18.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = elkhetaFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomDropdown(tr("المحافظة", "Governorate"), governoratesWithAreas.keys.toList(), governorate) { governorate = it; errorMsg = "" }
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomDropdown(tr("المنطقة", "Area"), availableAreas, area) { area = it; errorMsg = "" }

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val photoErrorMsg = tr("يرجى اختيار الصورة الشخصية أولاً", "Please select a profile photo")
                    val fieldsErrorMsg = tr("يرجى ملء اسم المستخدم والباسورد ورقم الواتساب", "Please fill all fields")
                    val usernameTakenMsg = tr("اسم المستخدم مسجل بالفعل، اختر اسماً آخر", "Username already taken")
                    val locationErrorMsg = tr("يرجى اختيار المحافظة والمنطقة", "Please choose governorate and area")
                    val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")

                    Elkheta3DButton(
                        text = tr("تسجيل", "Register"),
                        onClick = {
                            when {
                                photoUri == null -> errorMsg = photoErrorMsg
                                username.isBlank() || password.isBlank() || phone.isBlank() -> errorMsg = fieldsErrorMsg
                                password.length < AccountAuth.MIN_PASSWORD_LENGTH -> errorMsg = authText(AuthError.WEAK_PASSWORD)
                                governorate.isBlank() || area.isBlank() -> errorMsg = locationErrorMsg
                                username.trim().startsWith("*") -> errorMsg = reservedMsg
                                checkUsernameTaken(username) -> errorMsg = usernameTakenMsg
                                else -> {
                                    if (!busy) {
                                        busy = true
                                        errorMsg = ""
                                        onRegistered(
                                            StudentProfile(
                                                username = username.trim(),
                                                name = username.trim(),
                                                phone = phone.trim(),
                                                photoUriString = uriToBase64(context, photoUri ?: Uri.EMPTY),
                                                governorate = governorate,
                                                area = area,
                                                educationTrack = "عربي",
                                                stage = "ولي أمر",
                                                isParent = true,
                                                canAddStudentApproved = false
                                            ),
                                            password
                                        ) { err ->
                                            busy = false
                                            if (err != AuthError.NONE) errorMsg = authText(err)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        mainColor = ElkhetaCyan,
                        shadowColor = ElkhetaCyanShadow
                    )
                }
            }
        }
    }
}

// ==================== 10. شاشة تسجيل المعلم الكاملة ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRegistrationScreen(
    appState: MrPrivateState? = null,
    checkUsernameTaken: (String) -> Boolean,
    onRegistered: (TeacherProfile, String, (AuthError) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val authText = rememberAuthErrorTexts()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var secondName by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf(educationTracks[0]) }
    var selectedGov by remember { mutableStateOf(governoratesWithAreas.keys.first()) }
    val availableAreas = governoratesWithAreas[selectedGov] ?: emptyList()
    var selectedArea by remember(selectedGov) { mutableStateOf(availableAreas.first()) }
    var stage1 by remember { mutableStateOf(educationalStages[0]) }
    var stage2 by remember { mutableStateOf("بدون مرحلة إضافية") }
    val selectedStages = listOfNotNull(stage1, stage2.takeIf { it != "بدون مرحلة إضافية" && it != stage1 })
    val availableSubjects = getSubjectsByTrack(selectedTrack).filter { s -> selectedStages.any { st -> s in getSubjectsFor(selectedTrack, st) } }
    var subject by remember(selectedTrack, stage1, stage2) { mutableStateOf(availableSubjects.first()) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("تسجيل بروفايل معلم", "Teacher Registration"), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = PureWhite) } },
                actions = { LanguageSwitcherButton(appState = null) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularImagePicker(photoUri = photoUri, onPhotoSelected = { photoUri = it })
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = username, onValueChange = { username = it; errorMsg = "" }, label = { Text(tr("اسم المستخدم (User Name)", "User Name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = email, onValueChange = { email = it; errorMsg = "" }, label = { Text(tr("البريد الإلكتروني (Email)", "Email")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it; errorMsg = "" }, label = { Text(tr("رقم التليفون واتساب (للإدارة فقط)", "WhatsApp Phone (Admin Only)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; errorMsg = "" }, label = { Text(tr("كلمة المرور (Password)", "Password")) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it; errorMsg = "" }, label = { Text(tr("الاسم الأول", "First Name")) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = secondName, onValueChange = { secondName = it; errorMsg = "" }, label = { Text(tr("الاسم الثاني", "Second Name")) }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("نوع الدراسة (عربي / لغات)", "Education Track"), educationTracks, selectedTrack) { newTrack ->
                selectedTrack = newTrack
                subject = getSubjectsByTrack(newTrack).first()
            }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المحافظة", "Governorate"), governoratesWithAreas.keys.toList(), selectedGov) { selectedGov = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المنطقة", "Area"), availableAreas, selectedArea) { selectedArea = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المرحلة التعليمية (1)", "Stage 1"), educationalStages, stage1) { stage1 = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المرحلة التعليمية (2 - اختياري)", "Stage 2"), listOf("بدون مرحلة إضافية") + educationalStages, stage2) { stage2 = it }
            Spacer(modifier = Modifier.height(10.dp))
            CustomDropdown(tr("المادة التعليمية", "Subject"), availableSubjects, subject) { subject = it }

            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))

            val photoErrorMsg = tr("يرجى اختيار صورة شخصية", "Please select a photo")
            val fieldsErrorMsg = tr("يرجى ملء جميع الحقول المطلوبة", "Please fill all required fields")
            val usernameTakenMsg = tr("اسم المستخدم مسجل بالفعل", "Username already taken")
            val reservedMsg = tr("اسم المستخدم لا يجوز أن يبدأ بعلامة *", "Username cannot start with *")

            Elkheta3DButton(
                text = tr("تسجيل وإرسال للمراجعة", "Register & Submit"),
                onClick = {
                    when {
                        photoUri == null -> errorMsg = photoErrorMsg
                        username.isBlank() || phone.isBlank() || password.isBlank() || firstName.isBlank() || secondName.isBlank() -> errorMsg = fieldsErrorMsg
                        password.length < AccountAuth.MIN_PASSWORD_LENGTH -> errorMsg = authText(AuthError.WEAK_PASSWORD)
                        username.trim().startsWith("*") -> errorMsg = reservedMsg
                        checkUsernameTaken(username) -> errorMsg = usernameTakenMsg
                        else -> {
                            if (!busy) {
                                busy = true
                                errorMsg = ""
                                val stagesList = mutableListOf(stage1)
                                if (stage2 != "بدون مرحلة إضافية" && stage2 != stage1) stagesList.add(stage2)
                                onRegistered(
                                    TeacherProfile(
                                        username = username.trim(), email = email.trim(), phone = phone.trim(),
                                        firstName = firstName.trim(), secondName = secondName.trim(),
                                        photoUriString = uriToBase64(context, photoUri ?: Uri.EMPTY),
                                        governorate = selectedGov, area = selectedArea, educationTrack = selectedTrack,
                                        stages = stagesList, subject = subject, isApproved = false
                                    ),
                                    password
                                ) { err ->
                                    busy = false
                                    if (err != AuthError.NONE) errorMsg = authText(err)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                mainColor = DarkPurple,
                shadowColor = ElkhetaPurpleShadow
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "1. شاشة تسجيل الدخول الحديثة")
@Composable
fun PreviewLoginScreen() {
    val state = remember { MrPrivateState(isPreviewMode = true) }
    MrPrivateTheme(appState = state) {
        LoginScreen(isLoading = false, appState = state, onLogin = { _, _, _, done -> done(LoginResult.SUCCESS) }, onFirstTimeRegisterClick = {})
    }
}

// ==================== موجِّه التطبيق الرئيسي (App Navigation Router) ====================
// يُستدعى من MainActivity بعد تهيئة الثيم — يوجِّه إلى الشاشة المناسبة بناءً على state.currentScreen

@Composable
fun AppNavigation(state: MrPrivateState) {
    val context = LocalContext.current

    when (state.currentScreen) {
        "login" -> LoginScreen(
            isLoading = state.isLoading,
            appState = state,
            onLogin = { role, user, pass, done -> state.loginAsync(context, role, user, pass, done) },
            onFirstTimeRegisterClick = { state.currentScreen = "role_selection" }
        )
        "role_selection" -> {
            BackHandler { state.currentScreen = "login" }
            RoleSelectionScreen(
                appState = state,
                onSelectRole = { role ->
                    state.currentScreen = if (role == "teacher") "teacher_reg" else "parent_reg"
                },
                onBackToLogin = { state.currentScreen = "login" }
            )
        }
        "admin_dashboard" -> state.activeAdmin?.let { admin ->
            BackHandler { clearLoginPrefs(context); state.activeAdminId = null; state.currentScreen = "login" }
            AdminDashboardScreen(
                currentAdmin = admin, allAdmins = state.admins, allTeachers = state.teachers,
                allStudents = state.students, allSchedules = state.schedules, appState = state,
                checkUsernameTaken = { state.isUsernameTaken(it) }, onApproveTeacher = { state.approveTeacher(it) },
                onUpdateTeacher = { state.updateTeacherProfile(it) }, onDeleteTeacher = { state.deleteTeacher(it) },
                onUpdateStudent = { state.updateStudentProfile(it) }, onDeleteStudent = { state.deleteStudent(it) },
                onUpdateSchedule = { state.updateSchedule(it) }, onDeleteSchedule = { state.deleteSchedule(it) },
                onConfirmStudentBooking = { slot, enrollId -> state.confirmStudentBooking(slot, enrollId) },
                onSaveAdmin = { state.addOrUpdateAdmin(it) }, onCreateAdmin = { a, pass, done -> state.createAdminAsync(a, pass, done) }, onDeleteAdmin = { state.deleteAdmin(it) },
                onLogout = { clearLoginPrefs(context); state.activeAdminId = null; state.currentScreen = "login" }
            )
        }
        "teacher_reg" -> {
            BackHandler { state.currentScreen = "role_selection" }
            TeacherRegistrationScreen(
                appState = state, checkUsernameTaken = { state.isUsernameTaken(it) },
                onRegistered = { teacher, pass, done -> state.registerTeacherAsync(teacher, pass, done) }, onBack = { state.currentScreen = "role_selection" }
            )
        }
        "teacher_dashboard" -> state.activeTeacher?.let { teacher ->
            BackHandler { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" }
            if (!teacher.isApproved) {
                TeacherPendingApprovalScreen(teacher = teacher, appState = state, onLogout = { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" })
            } else {
                TeacherDashboardScreen(
                    teacher = teacher, schedules = state.schedules.filter { it.teacherId == teacher.id }, appState = state,
                    onAddSchedule = { state.addSchedule(it) }, onConfirmStudent = { slot, enrollmentId -> state.confirmStudentBooking(slot, enrollmentId) },
                    onCompleteStudentSession = { slot, enrollmentId, stars, note -> state.completeStudentSession(slot, enrollmentId, stars, note) },
                    onUpdateAccountPhoto = { role, userId, photo -> state.updateAccountPhoto(role, userId, photo) },
                    onLogout = { clearLoginPrefs(context); state.activeTeacherId = null; state.currentScreen = "login" }
                )
            }
        }
        "parent_reg" -> {
            BackHandler { state.currentScreen = "role_selection" }
            StudentRegistrationScreen(
                isParent = true, appState = state, checkUsernameTaken = { state.isUsernameTaken(it) },
                onRegistered = { parent, pass, done -> state.registerStudentAsync(parent, pass, done) }, onBack = { state.currentScreen = "role_selection" }
            )
        }
        "student_dashboard" -> state.activeStudent?.let { student ->
            BackHandler { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
            val approvedTeachers = state.teachers.filter { it.isApproved }
            if (student.isParent) {
                ParentDashboardScreen(
                    parent = student, allStudents = state.students, allTeachers = approvedTeachers, allSchedules = state.schedules, appState = state,
                    onSelectTeacherForChild = { teacher, child ->
                        state.targetStudentForBookingId = child.id
                        state.selectedTeacherIdForStudent = teacher.id
                        state.currentScreen = "teacher_details_for_student"
                    },
                    onUpdateStudent = { state.updateStudentProfile(it) },
                    onAddChild = { },
                    checkUsernameTaken = { state.isUsernameTaken(it) },
                    onUpdateAccountPhoto = { role, userId, photo -> state.updateAccountPhoto(role, userId, photo) },
                    onLogout = { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
                )
            } else {
                StudentHomeScreen(
                    student = student, allStudents = state.students, appState = state,
                    onLogout = { clearLoginPrefs(context); state.activeStudentId = null; state.currentScreen = "login" }
                )
            }
        }
        "teacher_details_for_student" -> {
            BackHandler { state.currentScreen = "student_dashboard" }
            val bookingStudent = state.targetStudentForBooking
            val teacher = state.selectedTeacherForStudent
            if (bookingStudent != null && teacher != null) {
                TeacherScheduleForStudentScreen(
                    student = bookingStudent, teacher = teacher, schedules = state.schedules.filter { it.teacherId == teacher.id },
                    onBookSlot = { slot -> state.bookSlot(slot, bookingStudent) },
                    onSubmitRating = { slot, enrollmentId, stars -> state.submitTeacherRating(teacher, slot, enrollmentId, stars) },
                    onBack = { state.currentScreen = "student_dashboard" }
                )
            }
        }
    }
}
