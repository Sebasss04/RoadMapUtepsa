package com.example.roadmap_utepsa.presentation.viewmodel

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.roadmap_utepsa.R
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Solo mensajes controlados: nunca muestra mensajes del SDK, tokens o datos de cuenta. */
internal fun googleSignInErrorMessage(error: Throwable): Int? {
    val causes = generateSequence(error) { it.cause }.take(8).toList()
    if (causes.any { it is GetCredentialCancellationException }) return null
    if (causes.any { it is FirebaseNetworkException || it is UnknownHostException ||
            it is ConnectException || it is SocketTimeoutException }) return R.string.login_google_network_error
    if (causes.any { it is GetCredentialInterruptedException }) return R.string.login_google_interrupted
    if (causes.any { it is NoCredentialException }) return R.string.login_google_no_account
    if (causes.any { it is GetCredentialProviderConfigurationException || it is GetCredentialUnsupportedException })
        return R.string.login_google_unavailable
    if (causes.any { it is ApiException && it.statusCode == 10 ||
            it is GetCredentialUnknownException && it.message.orEmpty()
                .contains("Developer console is not set up correctly", ignoreCase = true) })
        return R.string.login_google_installation_error
    val firebase = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
    return firebase?.let { firebaseSignInErrorMessage(it.errorCode) } ?: R.string.login_google_generic_error
}

internal fun firebaseSignInErrorMessage(code: String): Int = when (code) {
    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_API_KEY",
    "ERROR_INVALID_OAUTH_CLIENT_ID" -> R.string.login_google_installation_error
    "ERROR_NETWORK_REQUEST_FAILED" -> R.string.login_google_network_error
    "ERROR_TOO_MANY_REQUESTS" -> R.string.login_google_too_many_requests
    "ERROR_USER_DISABLED" -> R.string.login_google_disabled_account
    "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_IDP_RESPONSE", "ERROR_USER_TOKEN_EXPIRED",
    "ERROR_INVALID_USER_TOKEN" -> R.string.login_google_retry_account
    else -> R.string.login_google_generic_error
}
