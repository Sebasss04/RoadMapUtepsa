package com.example.roadmap_utepsa.account

import com.example.roadmap_utepsa.R

import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.auth.DifferentGoogleEmail
import com.example.roadmap_utepsa.domain.auth.AccessValidation
import com.example.roadmap_utepsa.presentation.auth.accountError
import com.example.roadmap_utepsa.data.auth.firebaseAccessKind
import com.example.roadmap_utepsa.domain.auth.AccessFailure
import com.example.roadmap_utepsa.presentation.account.AccountViewModel
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountAuthenticationTest {
    private val dispatcher = StandardTestDispatcher()
    private class Auth : AuthRepository {
        val identity = MutableStateFlow<AuthIdentity?>(null)
        override val currentIdentity get() = identity.value
        override val identityChanges = identity
        override val currentUid get() = currentIdentity?.uid
        override val isAuthenticated get() = currentUid != null
        override val uidChanges = identity.map { it?.uid }
        override val authenticationChanges = uidChanges.map { it != null }
        var registrations = 0; var emails = 0; var failEmail = false; var reauth = false; var linked = false
        var gate: CompletableDeferred<Unit>? = null
        var linkFailure: Exception? = null
        var refreshFailure = false
        override suspend fun signInWithGoogle(idToken: String) { identity.value = AuthIdentity("a", "a@example.test") }
        override suspend fun register(email: String, password: String) {
            registrations++; identity.value = AuthIdentity("a", email, false, setOf("password"))
        }
        override suspend fun signInWithEmail(email: String, password: String) = register(email, password)
        override suspend fun sendVerification() { emails++; if (failEmail) error("synthetic") }
        override suspend fun sendPasswordReset(email: String) { }
        override suspend fun refreshIdentity(): AuthIdentity? { if (refreshFailure) error("Offline"); return currentIdentity }
        override suspend fun reauthenticatePassword(password: String) { gate?.await(); reauth = true }
        override suspend fun reauthenticateGoogle(idToken: String) { reauth = true }
        override suspend fun linkGoogle(idToken: String) {
            linkFailure?.let { throw it }
            check(reauth); linked = true; identity.value = currentIdentity!!.copy(providers = setOf("password", "google.com"))
        }
        override suspend fun linkPassword(password: String) {
            check(reauth); linked = true; identity.value = currentIdentity!!.copy(providers = setOf("password", "google.com"))
        }
        override fun signOut() { identity.value = null }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun validationRejectsInvalidEmailShortAndMismatchedPasswords() {
        assertNotNull(AccessValidation.password("bad", "12345678", "12345678"))
        assertNotNull(AccessValidation.password("a@example.test", "1234567", "1234567"))
        assertNotNull(AccessValidation.password("a@example.test", "12345678", "87654321"))
        assertNull(AccessValidation.password("a@example.test", "12345678", "12345678"))
        assertFalse(AccessValidation.email("a@x\n.test"))
    }
    @Test fun unverifiedPasswordCannotPassGate() {
        assertTrue(AuthIdentity("a", emailVerified = false, providers = setOf("password")).requiresVerification)
        assertFalse(AuthIdentity("a", emailVerified = true, providers = setOf("password")).requiresVerification)
    }
    @Test fun verificationFailureRetainsCreatedIdentityAndCooldown() = runTest(dispatcher) {
        val auth = Auth().apply { failEmail = true }; val vm = AuthViewModel(auth)
        vm.emailAccess("a@example.test", "12345678", true); runCurrent()
        assertEquals("a", vm.state.value.uid); assertTrue(vm.state.value.identity!!.requiresVerification)
        assertNotNull(vm.state.value.errorMessage); assertEquals(1, auth.registrations)
        vm.verifyEmail(); assertEquals(1, auth.emails)
        vm.emailAccess("a@example.test", "12345678", true); assertEquals(1, auth.registrations)
    }
    @Test fun sameUidProviderAndVerificationRefreshRetainsSessionVersion() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.emailAccess("a@example.test", "12345678", true); runCurrent()
        val version = vm.state.value.sessionVersion
        auth.identity.value = auth.currentIdentity!!.copy(emailVerified = true, providers = setOf("password", "google.com"))
        runCurrent(); vm.checkVerification()
        assertEquals(version, vm.state.value.sessionVersion)
        assertFalse(vm.state.value.identity!!.requiresVerification)
        vm.signOut(); runCurrent(); assertTrue(vm.state.value.sessionVersion > version)
    }
    @Test fun recoveryUsesNeutralResponse() = runTest(dispatcher) {
        val vm = AuthViewModel(Auth()); vm.recover("unknown@example.test")
        assertEquals(R.string.auth_recovery_neutral, vm.state.value.notice); assertNull(vm.state.value.uid)
    }
    @Test fun linkingPasswordReauthenticatesGoogleAndPreservesUid() = runTest(dispatcher) {
        val auth = Auth(); auth.signInWithGoogle("demo")
        val vm = AccountViewModel(auth, "a", 1) { 1 }
        vm.link(null, "12345678") { Result.success("demo") }; runCurrent()
        assertTrue(auth.reauth); assertTrue(auth.linked); assertEquals("a", auth.currentUid)
        assertFalse(vm.state.value.error)
    }
    @Test fun linkDiscardsLateReauthenticationAfterAccountChange() = runTest(dispatcher) {
        val auth = Auth(); auth.register("a@example.test", "12345678")
        val gate = CompletableDeferred<Unit>(); auth.gate = gate
        var version = 1L
        val vm = AccountViewModel(auth, "a", version) { version }
        vm.link("12345678", null) { Result.success("demo") }; runCurrent()
        auth.signOut(); version++; auth.identity.value = AuthIdentity("b")
        gate.complete(Unit); runCurrent(); assertFalse(auth.linked); assertNull(vm.state.value.message)
    }
    @Test fun googleLinkRequiresReauthAndKeepsIdentity() = runTest(dispatcher) {
        val auth = Auth(); auth.register("a@example.test", "12345678")
        val vm = AccountViewModel(auth, "a", 1) { 1 }
        vm.link("12345678", null) { Result.success("demo") }; runCurrent()
        assertTrue(auth.reauth); assertTrue(auth.linked); assertEquals("a", auth.currentUid)
    }
    @Test fun differentEmailHasSpecificSafeFeedback() {
        assertEquals(R.string.access_different_email, accountError(DifferentGoogleEmail()))
        assertEquals(R.string.access_failed, accountError(Exception("secret-password")))
    }
    @Test fun resendRetriesSameIdentityOnlyAfterOneMinute() = runTest(dispatcher) {
        val auth = Auth(); var now = 1L; val vm = AuthViewModel(auth) { now }
        vm.emailAccess("a@example.test", "12345678", true); runCurrent(); assertEquals(1, auth.emails)
        now += 59_999; vm.verifyEmail(); assertEquals(1, auth.emails)
        now++; vm.verifyEmail(); assertEquals(2, auth.emails); assertEquals(1, auth.registrations)
    }
    @Test fun googlePickerResponseAfterSignOutCannotSignIn() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth); val token = CompletableDeferred<Result<String>>()
        val job = launch { vm.signInWithGoogle { token.await() } }; runCurrent()
        vm.signOut(); token.complete(Result.success("late-token")); job.join(); runCurrent()
        assertNull(auth.currentUid); assertFalse(vm.state.value.isAuthenticated)
    }
    @Test fun credentialConflictKeepsUidAndDoesNotMerge() = runTest(dispatcher) {
        val auth = Auth(); auth.register("a@example.test", "12345678")
        auth.linkFailure = Exception("synthetic conflict")
        val vm = AccountViewModel(auth, "a", 1) { 1 }
        vm.link("12345678", null) { Result.success("demo") }; runCurrent()
        assertEquals("a", auth.currentUid); assertFalse(auth.linked)
        assertTrue(vm.state.value.error)
        assertEquals(R.string.access_method_in_use, accountError(AccessFailure(firebaseAccessKind("ERROR_CREDENTIAL_ALREADY_IN_USE"))))
    }
    @Test fun uncertainResultMustBeReconciledBeforeRetry() = runTest(dispatcher) {
        val auth = Auth(); auth.register("a@example.test", "12345678")
        auth.linkFailure = Exception("network"); auth.refreshFailure = true
        val vm = AccountViewModel(auth, "a", 1) { 1 }
        vm.link("12345678", null) { Result.success("demo") }; runCurrent()
        assertFalse(vm.state.value.reconciled)
        auth.linkFailure = null; vm.link("12345678", null) { Result.success("demo") }; runCurrent(); assertFalse(auth.linked)
        auth.refreshFailure = false; vm.refresh(); runCurrent(); assertTrue(vm.state.value.reconciled)
    }
    @Test fun emailExistsDoesNotClaimThatAnotherIdentityOwnsTheCredential() {
        val message = accountError(AccessFailure(firebaseAccessKind("ERROR_EMAIL_ALREADY_IN_USE")))
        assertEquals(R.string.access_email_exists, message)
        assertNotEquals(R.string.access_method_in_use, message)
        assertEquals(R.string.access_method_in_use, accountError(AccessFailure(firebaseAccessKind("ERROR_CREDENTIAL_ALREADY_IN_USE"))))
    }
    @Test fun lateGooglePickerCannotReauthenticateAnotherSession() = runTest(dispatcher) {
        val auth = Auth(); auth.signInWithGoogle("demo"); var version = 1L
        val vm = AccountViewModel(auth, "a", 1) { version }
        val token = CompletableDeferred<Result<String>>()
        vm.link(null, "12345678") { token.await() }; runCurrent()
        auth.signOut(); version++; auth.identity.value = AuthIdentity("b")
        token.complete(Result.success("late")); runCurrent()
        assertFalse(auth.reauth); assertFalse(auth.linked); assertEquals("b", auth.currentUid)
    }
    @Test fun firebase35PasswordPolicyCodeKeepsActionableFeedback() {
        val message = accountError(AccessFailure(firebaseAccessKind("ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS")))
        assertEquals(R.string.access_weak_password, message)
        assertEquals(accountError(AccessFailure(firebaseAccessKind("ERROR_WEAK_PASSWORD"))), message)
    }

}
