package com.example.roadmap_utepsa.presentation.auth

import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.auth.AccessFailure
import com.example.roadmap_utepsa.domain.auth.AccessFailureKind

internal fun googleSignInErrorMessage(error: Throwable): Int? = when ((error as? AccessFailure)?.kind) {
    AccessFailureKind.CANCELLED -> null
    AccessFailureKind.NETWORK -> R.string.login_google_network_error
    AccessFailureKind.INTERRUPTED -> R.string.login_google_interrupted
    AccessFailureKind.NO_ACCOUNT -> R.string.login_google_no_account
    AccessFailureKind.UNAVAILABLE -> R.string.login_google_unavailable
    AccessFailureKind.CONFIGURATION -> R.string.login_google_installation_error
    AccessFailureKind.TOO_MANY_ATTEMPTS -> R.string.login_google_too_many_requests
    AccessFailureKind.DISABLED -> R.string.login_google_disabled_account
    AccessFailureKind.INVALID_ACCESS -> R.string.login_google_retry_account
    else -> R.string.login_google_generic_error
}
