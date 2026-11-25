package com.lovelyy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lovelyy.app.data.AuthRepository
import com.lovelyy.app.data.CatRepository
import com.lovelyy.app.data.EventRepository
import com.lovelyy.app.data.MoodRepository
import com.lovelyy.app.data.NoteRepository
import com.lovelyy.app.data.QuestionRepository
import com.lovelyy.app.data.SignalRepository
import com.lovelyy.app.signals.SignalCenter
import com.lovelyy.app.data.PairingRepository
import com.lovelyy.app.data.UserRepository

class LovelyyApplication : Application() {

    /** False until app/google-services.json is added (see README). */
    val isFirebaseConfigured: Boolean by lazy { FirebaseApp.getApps(this).isNotEmpty() }

    // Tiny hand-made "dependency container". Only touch these when Firebase is configured.
    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val userRepository by lazy { UserRepository(FirebaseFirestore.getInstance()) }
    val pairingRepository by lazy { PairingRepository(FirebaseFirestore.getInstance()) }
    val catRepository by lazy { CatRepository(FirebaseFirestore.getInstance()) }
    val signalRepository by lazy { SignalRepository(FirebaseFirestore.getInstance()) }
    val moodRepository by lazy { MoodRepository(FirebaseFirestore.getInstance()) }
    val questionRepository by lazy { QuestionRepository(FirebaseFirestore.getInstance()) }
    val noteRepository by lazy { NoteRepository(FirebaseFirestore.getInstance()) }
    val eventRepository by lazy { EventRepository(FirebaseFirestore.getInstance()) }
    val signalCenter by lazy { SignalCenter(this) }
}
