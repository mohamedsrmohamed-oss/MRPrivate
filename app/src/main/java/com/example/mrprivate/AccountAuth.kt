package com.example.mrprivate

import androidx.compose.runtime.Composable
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.security.MessageDigest

enum class AuthError { NONE, USERNAME_TAKEN, WEAK_PASSWORD, WRONG_CREDENTIALS, NETWORK, OTHER }

data class AuthResult(val uid: String? = null, val error: AuthError = AuthError.NONE)

/**
 * حسابات المستخدمين عبر Firebase Authentication (إيميل + كلمة مرور).
 * المستخدمون يسجلون بـ "اسم مستخدم"، فنشتق منه بريداً ثابتاً (SHA-256) لا يتغير:
 *   username → u<40 hex>@mrprivate.app
 * ومن يدخل بإيميل حقيقي (يحتوي @) يُستخدم كما هو — ويُستعمل لحسابات الأدمن الأولى.
 * كلمة المرور لا تُخزَّن في قاعدة البيانات إطلاقاً.
 */
object AccountAuth {
    const val MIN_PASSWORD_LENGTH = 6 // حد Firebase الأدنى
    private const val EMAIL_DOMAIN = "mrprivate.app"
    private const val SECONDARY_APP_NAME = "account_creator"

    fun identifierToEmail(identifier: String): String {
        val clean = identifier.trim().lowercase()
        if (clean.contains("@")) return clean
        val digest = MessageDigest.getInstance("SHA-256").digest(clean.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "u${hex.take(40)}@$EMAIL_DOMAIN"
    }

    private fun toError(e: Exception?): AuthError = when (e) {
        is FirebaseAuthUserCollisionException -> AuthError.USERNAME_TAKEN
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> AuthError.WRONG_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.OTHER
    }

    fun signIn(identifier: String, password: String, onResult: (AuthResult) -> Unit) {
        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(identifierToEmail(identifier), password)
                .addOnCompleteListener { task ->
                    val uid = if (task.isSuccessful) task.result?.user?.uid else null
                    onResult(if (uid != null) AuthResult(uid) else AuthResult(error = toError(task.exception)))
                }
        } catch (e: Exception) {
            onResult(AuthResult(error = toError(e)))
        }
    }

    /** إنشاء حساب ويدخل به مباشرة (للتسجيل الذاتي: ولي أمر / معلم) */
    fun createAccount(identifier: String, password: String, onResult: (AuthResult) -> Unit) {
        try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(identifierToEmail(identifier), password)
                .addOnCompleteListener { task ->
                    val uid = if (task.isSuccessful) task.result?.user?.uid else null
                    onResult(if (uid != null) AuthResult(uid) else AuthResult(error = toError(task.exception)))
                }
        } catch (e: Exception) {
            onResult(AuthResult(error = toError(e)))
        }
    }

    /**
     * إنشاء حساب لشخص آخر (ابن من ولي أمر / أدمن جديد) بدون إخراج المستخدم الحالي من جلسته:
     * نستخدم نسخة Firebase ثانوية ثم نسجّل خروجها فوراً.
     */
    fun createAccountSecondary(identifier: String, password: String, onResult: (AuthResult) -> Unit) {
        try {
            val auth = secondaryAuth()
            auth.createUserWithEmailAndPassword(identifierToEmail(identifier), password)
                .addOnCompleteListener { task ->
                    val uid = if (task.isSuccessful) task.result?.user?.uid else null
                    val error = toError(task.exception)
                    auth.signOut()
                    onResult(if (uid != null) AuthResult(uid) else AuthResult(error = error))
                }
        } catch (e: Exception) {
            onResult(AuthResult(error = toError(e)))
        }
    }

    fun signOut() {
        try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
    }

    val currentUid: String? get() = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }

    private fun secondaryAuth(): FirebaseAuth {
        val app = try {
            FirebaseApp.getInstance(SECONDARY_APP_NAME)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(MrPrivateApp.instance, FirebaseApp.getInstance().options, SECONDARY_APP_NAME)
        }
        return FirebaseAuth.getInstance(app)
    }
}

/** نصوص أخطاء المصادقة (تُحسب داخل الـ Composable ثم تُستعمل داخل الـ callbacks) */
@Composable
fun rememberAuthErrorTexts(): (AuthError) -> String {
    val taken = tr("اسم المستخدم مسجل بالفعل، اختر اسماً آخر", "Username already taken")
    val weak = tr("كلمة المرور يجب ألا تقل عن 6 أحرف", "Password must be at least 6 characters")
    val net = tr("تعذر الاتصال بالإنترنت، تأكد من الشبكة وحاول مرة أخرى", "No internet connection, please try again")
    val wrong = tr("اسم المستخدم أو كلمة المرور غير صحيحة!", "Invalid username or password!")
    val other = tr("حدث خطأ غير متوقع، حاول مرة أخرى", "Unexpected error, please try again")
    return { e ->
        when (e) {
            AuthError.USERNAME_TAKEN -> taken
            AuthError.WEAK_PASSWORD -> weak
            AuthError.NETWORK -> net
            AuthError.WRONG_CREDENTIALS -> wrong
            else -> other
        }
    }
}
