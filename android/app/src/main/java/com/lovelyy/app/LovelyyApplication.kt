package com.lovelyy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lovelyy.app.data.AuthRepository
import com.lovelyy.app.data.PairingRepository
import com.lovelyy.app.data.UserRepository

class LovelyyApplication : Application() {

    /** False until app/google-services.json is added (see README). */
    val isFirebaseConfigured: Boolean by lazy { FirebaseApp.getApps(this).isNotEmpty() }

    // Tiny hand-made "dependency container". Only touch these when Firebase is configured.
    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val userRepository by lazy { UserRepository(FirebaseFirestore.getInstance()) }
    val pairingRepository by lazy { PairingRepository(FirebaseFirestore.getInstance()) }
}
