@file:Suppress("DEPRECATION", "unused", "UNUSED_VARIABLE", "UNUSED_IMPORT")

package com.example.mrprivate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.mrprivate.features.auth.AppNavigation
import com.example.mrprivate.ui.components.MrPrivateTheme
import com.example.mrprivate.ui.components.restoreLoginState

/**
 * نقطة الدخول الوحيدة للتطبيق — خفيفة بالكامل.
 * - تهيئة [MrPrivateState] مرة واحدة عبر [remember].
 * - استعادة حالة الدخول المحفوظة عبر [restoreLoginState] (من AppHelpers).
 * - تطبيق الثيم ثم تسليم التحكم لـ [AppNavigation] (من AuthScreens).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val appState = remember { MrPrivateState() }
            LaunchedEffect(Unit) { restoreLoginState(context, appState) }
            MrPrivateTheme(appState = appState) { AppNavigation(appState) }
        }
    }
}
