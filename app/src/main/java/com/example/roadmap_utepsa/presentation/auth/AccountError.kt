package com.example.roadmap_utepsa.presentation.auth

import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.auth.*

fun accountError(error: Exception): Int = when {
    error is DifferentGoogleEmail -> R.string.access_different_email
    error is AccessFailure -> when (error.kind) {
        AccessFailureKind.CANCELLED -> R.string.access_cancelled
        AccessFailureKind.DIFFERENT_EMAIL -> R.string.access_different_email
        AccessFailureKind.NETWORK -> R.string.access_network
        AccessFailureKind.EMAIL_EXISTS -> R.string.access_email_exists
        AccessFailureKind.METHOD_IN_USE -> R.string.access_method_in_use
        AccessFailureKind.WEAK_PASSWORD -> R.string.access_weak_password
        AccessFailureKind.TOO_MANY_ATTEMPTS -> R.string.login_google_too_many_requests
        AccessFailureKind.RECENT_LOGIN -> R.string.access_recent_login
        AccessFailureKind.CONFIGURATION, AccessFailureKind.UNAVAILABLE -> R.string.access_unavailable
        else -> R.string.access_failed
    }
    else -> R.string.access_failed
}
