package com.example.roadmap_utepsa.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AuthIdentity(
    val uid: String,
    val email: String? = null,
    val emailVerified: Boolean = true,
    val providers: Set<String> = setOf("google.com")
) {
    val requiresVerification: Boolean get() = "password" in providers && !emailVerified
}

interface AuthRepository {
    val isAuthenticated: Boolean
    val currentUid: String?
    val authenticationChanges: Flow<Boolean>
    val uidChanges: Flow<String?>
    val currentIdentity: AuthIdentity? get() = currentUid?.let { AuthIdentity(it) }
    val identityChanges: Flow<AuthIdentity?> get() = uidChanges.map { currentIdentity }
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInWithEmail(email: String, password: String) { error("Correo no disponible") }
    suspend fun register(email: String, password: String) { error("Registro no disponible") }
    suspend fun refreshIdentity(): AuthIdentity? = currentIdentity
    suspend fun sendVerification() { error("Verificación no disponible") }
    suspend fun sendPasswordReset(email: String) { error("Recuperación no disponible") }
    suspend fun reauthenticatePassword(password: String) { error("Reautenticación no disponible") }
    suspend fun reauthenticateGoogle(idToken: String) { error("Reautenticación no disponible") }
    suspend fun linkPassword(password: String) { error("Vinculación no disponible") }
    suspend fun linkGoogle(idToken: String) { error("Vinculación no disponible") }
    fun signOut()
}
