package com.example.roadmap_utepsa.data

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Boolean
    val currentUid: String?
    val authenticationChanges: Flow<Boolean>
    val uidChanges: Flow<String?>
    suspend fun signInWithGoogle(idToken: String)
    fun signOut()
}
