package com.example.roadmap_utepsa.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    override val isAuthenticated: Boolean
        get() = auth.currentUser != null
    override val currentUid: String?
        get() = auth.currentUser?.uid

    override val uidChanges: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val authenticationChanges: Flow<Boolean> = uidChanges.map { it != null }

    override suspend fun signInWithGoogle(idToken: String) {
        val result = auth.signInWithCredential(
            GoogleAuthProvider.getCredential(idToken, null)
        ).await()
        checkNotNull(result.user) { "Firebase no devolvió un usuario autenticado" }
    }

    override fun signOut() = auth.signOut()
}
