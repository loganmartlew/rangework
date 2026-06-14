package com.loganmartlew.rangework.android.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

sealed interface GoogleIdTokenRequestResult {
    data class Success(
        val idToken: String,
    ) : GoogleIdTokenRequestResult

    data class Cancelled(
        val message: String,
    ) : GoogleIdTokenRequestResult

    data class Failure(
        val message: String,
    ) : GoogleIdTokenRequestResult
}

class AndroidGoogleIdTokenProvider(
    private val activity: ComponentActivity,
    private val webClientId: String,
) {
    suspend fun requestIdToken(): GoogleIdTokenRequestResult {
        if (webClientId.isBlank()) {
            return GoogleIdTokenRequestResult.Failure(
                message = "Google sign-in is not configured. Add a web client ID first.",
            )
        }

        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credential = credentialManager.getCredential(
                context = activity,
                request = request,
            ).credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleIdTokenRequestResult.Success(idToken = googleCredential.idToken)
            } else {
                GoogleIdTokenRequestResult.Failure(
                    message = "Google sign-in returned an unsupported credential payload.",
                )
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdTokenRequestResult.Cancelled(
                message = "Google sign-in was cancelled.",
            )
        } catch (_: NoCredentialException) {
            GoogleIdTokenRequestResult.Failure(
                message = "Google could not provide a sign-in credential. This usually means the device has no eligible Google account, or the app's Google OAuth setup (web client ID / SHA fingerprints) is not configured yet.",
            )
        } catch (exception: GetCredentialException) {
            GoogleIdTokenRequestResult.Failure(
                message = exception.message ?: "Google sign-in failed.",
            )
        }
    }
}
