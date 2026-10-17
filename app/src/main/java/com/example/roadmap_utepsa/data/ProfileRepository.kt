package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.model.ProfileData

/** Identity comes from Authentication; implementations must never create another Auth account. */
interface ProfileRepository {
    /** Must distinguish a failed server read from an absent or incomplete registration. */
    suspend fun load(uid: String): ProfileData

    /** Returns only after remote confirmation. Initial registration must be an atomic batch. */
    suspend fun save(uid: String, names: String, surnames: String): ProfileData
}
