package com.example.roadmap_utepsa.domain.session

interface LockPreferences {
    suspend fun enabled(uid: String): Boolean
    suspend fun setEnabled(uid: String, enabled: Boolean)
}
