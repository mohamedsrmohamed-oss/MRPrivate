package com.example.mrprivate

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

/** عنوان قاعدة بيانات Firebase Realtime Database المستخدمة في المشروع */
const val FIREBASE_DB_URL = "https://mr-private-db3c1-default-rtdb.firebaseio.com/"

/**
 * كلاس التطبيق: يفعّل تخزين القرص (Offline Persistence) مرة واحدة فقط،
 * قبل أي استدعاء لقاعدة البيانات (يجب أن يتم قبل أول getReference).
 */
class MrPrivateApp : Application() {
    companion object {
        lateinit var instance: MrPrivateApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            FirebaseDatabase.getInstance(FIREBASE_DB_URL).setPersistenceEnabled(true)
        } catch (_: Exception) {
            // يُرمى استثناء إن كانت القاعدة قد استُخدمت قبلاً في نفس العملية؛ نتجاهله بأمان
        }
    }
}
