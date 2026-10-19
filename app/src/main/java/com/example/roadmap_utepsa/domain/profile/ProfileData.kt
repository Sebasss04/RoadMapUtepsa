package com.example.roadmap_utepsa.domain.profile

data class ProfileData(
    val names: String,
    val surnames: String,
    val plan: AssignedStudyPlan,
    val registration: ProfileRegistration
)
