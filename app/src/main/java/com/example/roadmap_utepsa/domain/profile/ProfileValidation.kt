package com.example.roadmap_utepsa.domain.profile

/** Same limits will also be enforced by Firestore rules; this is immediate form feedback. */
fun validProfileName(value: String): Boolean =
    value.trim().isNotEmpty() && value.trim().length <= 80 &&
        value.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }
