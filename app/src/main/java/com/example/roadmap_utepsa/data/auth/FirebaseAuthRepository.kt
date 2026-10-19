package com.example.roadmap_utepsa.data.auth

import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.auth.DifferentGoogleEmail
import com.example.roadmap_utepsa.domain.auth.StaleAuthSession

import android.util.Base64
import com.google.firebase.auth.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthRepository {
    private var epoch = 0L
    private var recentlyAuthenticated: Pair<String, Long>? = null
    private var refreshingUid: String? = null
    private var refreshGeneration = 0L
    private val signInMutex = Mutex()
    private var signingIn = false
    private fun identity(user: FirebaseUser?) = user?.let {
        AuthIdentity(it.uid, it.email, it.isEmailVerified,
            it.providerData.map { provider -> provider.providerId }.filter { id -> id != "firebase" }.toSet())
    }
    private val identities = MutableStateFlow(identity(auth.currentUser))
    init {
        auth.addIdTokenListener(FirebaseAuth.IdTokenListener { firebase: FirebaseAuth ->
            // During explicit verification, publish only after reload AND token refresh succeed.
            if (!signingIn && (firebase.currentUser?.uid != refreshingUid || refreshingUid == null))
                identities.value = identity(firebase.currentUser)
        })
    }
    override val currentIdentity get() = identities.value?.takeIf { it.uid == auth.currentUser?.uid }
    override val isAuthenticated get() = auth.currentUser != null
    override val currentUid get() = auth.currentUser?.uid
    override val identityChanges: Flow<AuthIdentity?> = identities.asStateFlow()
    override val uidChanges = identityChanges.map { it?.uid }.distinctUntilChanged()
    override val authenticationChanges = uidChanges.map { it != null }
    private suspend fun signIn(operation: suspend () -> AuthResult) {
        val expected = epoch
        // Firebase tasks can finish after a Compose caller is cancelled by rotation.
        // Keep the identity checkpoint alive until the sent authentication resolves.
        withContext(NonCancellable) { signInMutex.withLock {
            if (expected != epoch) throw StaleAuthSession()
            signingIn = true
            try {
                val user = checkNotNull(operation().user)
                if (expected != epoch) {
                    if (auth.currentUser?.uid == user.uid) auth.signOut()
                    throw StaleAuthSession()
                }
                identities.value = identity(user)
            } finally { signingIn = false }
        } }
    }
    override suspend fun signInWithGoogle(idToken: String) = signIn {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitAuth()
    }
    override suspend fun signInWithEmail(email: String, password: String) = signIn {
        auth.signInWithEmailAndPassword(email.trim(), password).awaitAuth()
    }
    // Separate from sending verification: an email failure must never create another identity.
    override suspend fun register(email: String, password: String) = signIn {
        auth.createUserWithEmailAndPassword(email.trim(), password).awaitAuth()
    }
    private fun user() = checkNotNull(auth.currentUser)
    private fun checkSession(user: FirebaseUser, expected: Long) {
        if (epoch != expected || auth.currentUser?.uid != user.uid) throw StaleAuthSession()
    }
    override suspend fun refreshIdentity(): AuthIdentity? {
        val user = auth.currentUser ?: return null
        val expected = epoch
        val refresh = ++refreshGeneration
        refreshingUid = user.uid
        try {
            user.reload().awaitAuth()
            checkSession(user, expected)
            user.getIdToken(true).awaitAuth()
            checkSession(user, expected)
            identities.value = identity(auth.currentUser)
            return identities.value
        } finally { if (refreshGeneration == refresh) refreshingUid = null }
    }
    override suspend fun sendVerification() { user().sendEmailVerification().awaitAuth() }
    override suspend fun sendPasswordReset(email: String) {
        try { auth.sendPasswordResetEmail(email.trim()).awaitAuth() }
        catch (e: com.example.roadmap_utepsa.domain.auth.AccessFailure) {
            if ((e.cause as? FirebaseAuthInvalidUserException)?.errorCode != "ERROR_USER_NOT_FOUND") throw e
        }
    }
    private fun googleCredential(user: FirebaseUser, idToken: String): AuthCredential {
        val email = JSONObject(String(Base64.decode(idToken.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)))
            .optString("email")
        if (email.isBlank() || !email.equals(user.email, ignoreCase = true)) throw DifferentGoogleEmail()
        return GoogleAuthProvider.getCredential(idToken, null)
    }
    private suspend fun reauthenticate(credential: (FirebaseUser) -> AuthCredential) {
        val user = user(); val expected = epoch
        recentlyAuthenticated = null
        user.reauthenticate(credential(user)).awaitAuth()
        checkSession(user, expected)
        recentlyAuthenticated = user.uid to android.os.SystemClock.elapsedRealtime()
    }
    override suspend fun reauthenticatePassword(password: String) = reauthenticate {
        EmailAuthProvider.getCredential(checkNotNull(it.email), password)
    }
    override suspend fun reauthenticateGoogle(idToken: String) = reauthenticate { googleCredential(it, idToken) }
    private suspend fun link(credential: (FirebaseUser) -> AuthCredential) {
        val user = user(); val expected = epoch
        val proof = recentlyAuthenticated
        check(proof != null && proof.first == user.uid && android.os.SystemClock.elapsedRealtime() - proof.second < 120_000)
        recentlyAuthenticated = null
        try {
            user.linkWithCredential(credential(user)).awaitAuth()
            checkSession(user, expected)
        } finally {
            if (epoch == expected && currentUid == user.uid) refreshIdentity()
        }
    }
    override suspend fun linkPassword(password: String) = link {
        EmailAuthProvider.getCredential(checkNotNull(it.email), password)
    }
    override suspend fun linkGoogle(idToken: String) = link { googleCredential(it, idToken) }
    override fun signOut() { epoch++; refreshGeneration++; recentlyAuthenticated = null; refreshingUid = null; auth.signOut(); identities.value = null }
}
