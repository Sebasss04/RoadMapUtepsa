package com.example.roadmap_utepsa.data.auth

import com.example.roadmap_utepsa.domain.auth.AccessFailure
import com.example.roadmap_utepsa.domain.auth.AccessFailureKind
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal fun firebaseAccessKind(code: String): AccessFailureKind = when (code) {
    "ERROR_EMAIL_ALREADY_IN_USE" -> AccessFailureKind.EMAIL_EXISTS
    "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AccessFailureKind.METHOD_IN_USE
    "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AccessFailureKind.WEAK_PASSWORD
    "ERROR_REQUIRES_RECENT_LOGIN" -> AccessFailureKind.RECENT_LOGIN
    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_API_KEY", "ERROR_INVALID_OAUTH_CLIENT_ID" -> AccessFailureKind.CONFIGURATION
    "ERROR_NETWORK_REQUEST_FAILED" -> AccessFailureKind.NETWORK
    "ERROR_TOO_MANY_REQUESTS" -> AccessFailureKind.TOO_MANY_ATTEMPTS
    "ERROR_USER_DISABLED" -> AccessFailureKind.DISABLED
    "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_IDP_RESPONSE", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AccessFailureKind.INVALID_ACCESS
    else -> AccessFailureKind.UNKNOWN
}

internal suspend fun <T> Task<T>.awaitAuth(): T = try { await() }
catch (e: CancellationException) { throw e }
catch (e: FirebaseAuthException) { throw AccessFailure(firebaseAccessKind(e.errorCode), e) }
catch (e: FirebaseNetworkException) { throw AccessFailure(AccessFailureKind.NETWORK, e) }
