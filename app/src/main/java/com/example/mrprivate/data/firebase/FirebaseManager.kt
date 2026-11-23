package com.example.mrprivate.data.firebase

import com.example.mrprivate.FIREBASE_DB_URL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * مدير مركزي لاتصالات وقواعد بيانات Firebase Realtime Database والمصادقة (Firebase Auth).
 */
object FirebaseManager {
    val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(FIREBASE_DB_URL)

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val teachersRef: DatabaseReference
        get() = database.getReference("teachers")

    val studentsRef: DatabaseReference
        get() = database.getReference("students")

    val schedulesRef: DatabaseReference
        get() = database.getReference("schedules")

    val quizzesRef: DatabaseReference
        get() = database.getReference("quizzes")

    val adminsRef: DatabaseReference
        get() = database.getReference("admins")

    val settingsRef: DatabaseReference
        get() = database.getReference("settings")

    val studentSlotsRef: DatabaseReference
        get() = database.getReference("student_slots")
}
