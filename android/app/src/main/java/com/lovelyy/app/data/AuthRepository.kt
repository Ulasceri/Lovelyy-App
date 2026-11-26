package com.lovelyy.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the signed-in user, or null when signed out. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String) = wrap {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun sendPasswordReset(email: String) = wrap {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String) = wrap {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
    }

    /**
     * Google sign-in through Android Credential Manager.
     * Needs the Google provider enabled in Firebase, which makes google-services.json
     * contain a "default_web_client_id" (see README). [activityContext] must be an Activity.
     */
    suspend fun signInWithGoogle(activityContext: Context) = wrap {
        val webClientId = webClientId(activityContext)
            ?: throw AppException(AppError.GoogleNotConfigured)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AppException(AppError.GoogleCancelled, e)
        } catch (e: GetCredentialException) {
            throw AppException(AppError.Unknown, e)
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) throw AppException(AppError.Unknown)

        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        // Widgets keep showing the couple otherwise.
        com.lovelyy.app.widget.WidgetData.clear(context)
        // An unsealed letter belongs to the signed-out account.
        CapsuleDraft.clear(context)
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    // The google-services plugin generates this string from google-services.json.
    // Looked up by name so the app still compiles before Firebase is set up.
    private fun webClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId == 0) null else context.getString(resId).takeIf { it.isNotBlank() }
    }

    private suspend fun wrap(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: AppException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthWeakPasswordException) {
            throw AppException(AppError.WeakPassword, e)
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AppException(AppError.EmailInUse, e)
        } catch (e: FirebaseAuthInvalidUserException) {
            throw AppException(AppError.InvalidCredentials, e)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            val err = if (e.errorCode == "ERROR_INVALID_EMAIL") AppError.InvalidEmail else AppError.InvalidCredentials
            throw AppException(err, e)
        } catch (e: FirebaseNetworkException) {
            throw AppException(AppError.Network, e)
        } catch (e: Exception) {
            throw AppException(AppError.Unknown, e)
        }
    }
}
