@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

import java.util.Locale

// ==================== 17. المكونات المساعدة والنوافذ المنبثقة والشارات ====================

@Composable
fun WhatsAppSupportButton() {
    val context = LocalContext.current
    Elkheta3DButton(
        text = tr("تواصل مع دعم المنصة (واتساب: 01118721114)", "Platform Support (WhatsApp: 01118721114)"),
        onClick = { openPlatformSupportWhatsApp(context) },
        modifier = Modifier.fillMaxWidth(),
        mainColor = WhatsAppGreen,
        shadowColor = ElkhetaGreenShadow,
        height = 44
    )
}

@Composable
fun CircularImagePicker(photoUri: Uri?, onPhotoSelected: (Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> onPhotoSelected(uri) }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(Color(0xFFE0F2FE))
            .border(3.dp, ElkhetaCyan, CircleShape)
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (photoUri != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                contentDescription = "اختيار صورة",
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = if (photoUri != null) tr("تم الاختيار ✓", "Selected ✓") else tr("اختر صورة", "Add Photo"),
                fontSize = 12.sp,
                color = Color(0xFF0284C7),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

fun decodeBase64ToBitmap(base64Str: String): android.graphics.Bitmap? {
    return try {
        val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) { null }
}

/** كاش صغير (حوالي 6MB) للصور الشخصية المفكوكة، فلا تُفك من Base64 عند كل إعادة تركيب أو تدوير شاشة. */
internal val avatarBitmapCache = object : android.util.LruCache<Pair<String, Int>, android.graphics.Bitmap>(6 * 1024) {
    override fun sizeOf(key: Pair<String, Int>, value: android.graphics.Bitmap): Int = value.byteCount / 1024
}

/** يفك Base64 بدقة قريبة من الحجم المعروض فقط (inSampleSize) بدل فك الصورة بحجمها الكامل. */
internal fun decodeBase64ToBitmapSampled(base64Str: String, targetPx: Int): android.graphics.Bitmap? {
    return try {
        val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
}

fun loadBitmapFromUriOrBase64(context: Context, photoStr: String, targetPx: Int = 384): android.graphics.Bitmap? {
    if (photoStr.isBlank()) return null
    val cacheKey = photoStr to targetPx
    avatarBitmapCache.get(cacheKey)?.let { return it }
    val fromBase64 = decodeBase64ToBitmapSampled(photoStr, targetPx)
    if (fromBase64 != null) {
        avatarBitmapCache.put(cacheKey, fromBase64)
        return fromBase64
    }
    return try {
        val uri = photoStr.toUri()
        val inputStream = context.contentResolver.openInputStream(uri)
        android.graphics.BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) { null }
}

@Composable
fun ProfileAvatar(
    size: Int,
    isDarkBg: Boolean = true,
    photoUriString: String = "",
    onPhotoChange: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onPhotoChange?.invoke(it) }
    }
    val avatarTargetPx = (size * androidx.compose.ui.platform.LocalDensity.current.density).toInt().coerceAtLeast(64)
    val bitmap = remember(photoUriString, avatarTargetPx) { loadBitmapFromUriOrBase64(context, photoUriString, avatarTargetPx) }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (isDarkBg) PureWhite else DarkPurple)
            .then(if (onPhotoChange != null) Modifier.clickable { launcher.launch("image/*") } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isDarkBg) DarkPurple else PureWhite,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
        if (onPhotoChange != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.34).dp)
                    .clip(CircleShape)
                    .background(DarkPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PureWhite, modifier = Modifier.size((size * 0.18).dp))
            }
        }
    }
}

@Composable
fun StarRatingBar(rating: Float, count: Int, darkText: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = GoldStar,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${String.format(Locale.US, "%.1f", rating)} ($count ${tr("تقييم", "reviews")})",
            color = if (darkText) DarkPurple else PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CustomDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = trValue(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = DarkPurple)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f)) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(trValue(option), fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** تعريب مسميات الباقات: Premium -> بريميوم | Ultimate -> اشتراك شامل */
fun getLocalizedPackageName(tierCode: String, isArabic: Boolean): String {
    return when (tierCode.uppercase()) {
        "ULTIMATE" -> if (isArabic) "اشتراك شامل" else "Ultimate"
        "PREMIUM" -> if (isArabic) "بريميوم (Premium)" else "Premium"
        else -> if (isArabic) "الوضع الأساسي" else "Basic"
    }
}

/** أتمتة إرسال طلب تجديد الاشتراك عبر واتساب الإدارة باسم المعلم ونوع الباقة */
fun openTeacherRenewalWhatsApp(
    context: Context,
    teacherName: String,
    requestedTierCode: String,
    isArabic: Boolean,
    adminPhone: String = "+201118721114"
) {
    val packageLabel = getLocalizedPackageName(requestedTierCode, isArabic)
    val message = if (isArabic) {
        "مرحباً إدارة تطبيق MR Private،\nأرغب في تجديد اشتراكي.\n• اسم المعلم: $teacherName\n• نوع الاشتراك المطلوب: $packageLabel\nيرجى إفادتي بالسعر وتفاصيل التفعيل."
    } else {
        "Hello MR Private Admin,\nI would like to renew my subscription.\n• Teacher Name: $teacherName\n• Requested Package: $packageLabel\nPlease share the price and activation details."
    }
    val url = "https://api.whatsapp.com/send?phone=$adminPhone&text=${Uri.encode(message)}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: Exception) {
        Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}
