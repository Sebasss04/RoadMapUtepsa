package com.example.roadmap_utepsa.platform.auth

import androidx.credentials.exceptions.*
import com.example.roadmap_utepsa.domain.auth.AccessFailure
import com.example.roadmap_utepsa.domain.auth.AccessFailureKind
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun credentialFailure(error: Throwable): AccessFailure {
    if (error is CancellationException) throw error
    if (error is AccessFailure) return error
    val causes = generateSequence(error) { it.cause }.take(8).toList()
    val kind = when {
        causes.any { it is GetCredentialCancellationException } -> AccessFailureKind.CANCELLED
        causes.any { it is UnknownHostException || it is ConnectException || it is SocketTimeoutException } -> AccessFailureKind.NETWORK
        causes.any { it is GetCredentialInterruptedException } -> AccessFailureKind.INTERRUPTED
        causes.any { it is NoCredentialException } -> AccessFailureKind.NO_ACCOUNT
        causes.any { it is GetCredentialProviderConfigurationException || it is GetCredentialUnsupportedException } -> AccessFailureKind.UNAVAILABLE
        causes.any { it is ApiException && it.statusCode == 10 || it is GetCredentialUnknownException && it.message.orEmpty().contains("Developer console is not set up correctly", true) } -> AccessFailureKind.CONFIGURATION
        else -> AccessFailureKind.UNKNOWN
    }
    return AccessFailure(kind, error)
}
