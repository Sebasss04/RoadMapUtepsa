package com.example.roadmap_utepsa.domain.auth

enum class AccessFailureKind {
    CANCELLED, NETWORK, INTERRUPTED, NO_ACCOUNT, UNAVAILABLE, CONFIGURATION,
    TOO_MANY_ATTEMPTS, DISABLED, INVALID_ACCESS, EMAIL_EXISTS, METHOD_IN_USE,
    WEAK_PASSWORD, RECENT_LOGIN, DIFFERENT_EMAIL, UNKNOWN
}

/** Keeps the original cause for internal diagnosis, never for display. */
class AccessFailure(val kind: AccessFailureKind, cause: Throwable? = null) : Exception(kind.name, cause)
