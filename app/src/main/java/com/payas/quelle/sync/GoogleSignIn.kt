package com.payas.quelle.sync

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.payas.quelle.R
import kotlinx.coroutines.tasks.await

sealed interface SignInResult {
    data object Success : SignInResult
    data object Cancelled : SignInResult
    data class Failed(val message: String) : SignInResult
}

/** Google hesap seçiciyi açar ve seçilen hesapla Firebase'e giriş yapar. */
suspend fun signInWithGoogle(context: Context): SignInResult {
    return try {
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            SignInResult.Success
        } else {
            SignInResult.Failed("Google hesabı alınamadı.")
        }
    } catch (e: GetCredentialCancellationException) {
        SignInResult.Cancelled
    } catch (e: Exception) {
        SignInResult.Failed(e.localizedMessage ?: "Giriş yapılamadı.")
    }
}

suspend fun signOut(context: Context) {
    FirebaseAuth.getInstance().signOut()
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
}
