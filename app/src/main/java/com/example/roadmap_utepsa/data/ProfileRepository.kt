package com.example.roadmap_utepsa.data

enum class ProfileRegistration { ABSENT, INCOMPLETE, COMPLETE }

data class AssignedStudyPlan(
    val id: String,
    val careerName: String,
    val resolution: String
)

data class ProfileData(
    val names: String,
    val surnames: String,
    val plan: AssignedStudyPlan,
    val registration: ProfileRegistration
)

/** Identity comes from Authentication; implementations must never create another Auth account. */
interface ProfileRepository {
    /** Must distinguish a failed server read from an absent or incomplete registration. */
    suspend fun load(uid: String): ProfileData

    /** Returns only after remote confirmation. Initial registration must be an atomic batch. */
    suspend fun save(uid: String, names: String, surnames: String): ProfileData
}

/** Same limits will also be enforced by Firestore rules; this is immediate form feedback. */
fun validProfileName(value: String): Boolean =
    value.trim().isNotEmpty() && value.trim().length <= 80 &&
        value.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }
