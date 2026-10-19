package com.example.roadmap_utepsa.domain.auth

enum class PasswordIssue { INVALID_EMAIL, TOO_SHORT, MISMATCH }

object AccessValidation {
    fun email(value: String): Boolean = value.trim().let {
        it.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it)
    }
    fun password(email: String, password: String, confirmation: String): PasswordIssue? = when {
        !email(email) -> PasswordIssue.INVALID_EMAIL
        password.length < 8 -> PasswordIssue.TOO_SHORT
        password != confirmation -> PasswordIssue.MISMATCH
        else -> null
    }
}
