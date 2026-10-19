package com.example.roadmap_utepsa.presentation.auth
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.auth.PasswordIssue
internal fun PasswordIssue.messageRes(): Int = when (this) {
    PasswordIssue.INVALID_EMAIL -> R.string.access_invalid_email
    PasswordIssue.TOO_SHORT -> R.string.access_weak_password
    PasswordIssue.MISMATCH -> R.string.access_password_mismatch
}
