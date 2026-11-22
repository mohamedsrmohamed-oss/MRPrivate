package com.example.mrprivate

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

/**
 * Firebase Manager providing centralized access to Firebase Authentication
 * and Cloud Firestore database operations for project mr-private-db3c1.
 */
@Suppress("unused")
object FirebaseManager {

    val auth: FirebaseAuth
        get() = Firebase.auth

    val firestore: FirebaseFirestore
        get() = Firebase.firestore

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserSignedIn: Boolean
        get() = currentUser != null

    /**
     * Sign up a new user with Email and Password using Firebase Authentication.
     */
    fun signUpWithEmail(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(auth.currentUser)
                } else {
                    onFailure(task.exception ?: Exception("Sign-up failed"))
                }
            }
    }

    /**
     * Sign in an existing user with Email and Password using Firebase Authentication.
     */
    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(auth.currentUser)
                } else {
                    onFailure(task.exception ?: Exception("Sign-in failed"))
                }
            }
    }

    /**
     * Sign out the currently authenticated user.
     */
    fun signOut() {
        auth.signOut()
    }
}
