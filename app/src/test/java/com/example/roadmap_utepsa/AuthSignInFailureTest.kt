package com.example.roadmap_utepsa

import androidx.credentials.exceptions.*
import com.example.roadmap_utepsa.data.AuthRepository
import com.example.roadmap_utepsa.presentation.viewmodel.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSignInFailureTest {
    private val dispatcher = StandardTestDispatcher()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>(null)
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        var calls = 0
        override suspend fun signInWithGoogle(idToken: String) {
            calls++
            check(idToken == "synthetic-token")
            uidChanges.value = "synthetic-user"
        }
        override fun signOut() { uidChanges.value = null }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun closingGooglePickerIsNotAnAuthenticationError() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.signInWithGoogle { Result.failure(GetCredentialCancellationException("synthetic cancellation")) }
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertFalse(vm.state.value.isAuthenticated)
        assertNull(vm.state.value.errorMessageRes)
        assertNull(vm.state.value.errorMessage)
        assertEquals(0, auth.calls)
    }
    @Test fun providerFailureNeverSendsCredentialToFirebase() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.signInWithGoogle { Result.failure(Exception("synthetic-secret-not-for-display")) }
        assertEquals(0, auth.calls)
        assertNull(vm.state.value.errorMessage)
        assertEquals(R.string.login_google_generic_error, vm.state.value.errorMessageRes)
    }
    @Test fun networkErrorGetsConnectionMessageAndStopsLoading() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.signInWithGoogle { Result.failure(UnknownHostException("synthetic-host")) }
        assertEquals(R.string.login_google_network_error, vm.state.value.errorMessageRes)
        assertFalse(vm.state.value.isLoading)
        assertEquals(0, auth.calls)
    }
    @Test fun onlyKnownDeveloperConsoleFailureIsIdentifiedAsConfiguration() {
        assertEquals(R.string.login_google_installation_error,
            googleSignInErrorMessage(GetCredentialUnknownException("[28444] Developer console is not set up correctly.")))
        assertEquals(R.string.login_google_generic_error,
            googleSignInErrorMessage(GetCredentialUnknownException("Some other unknown error")))
    }
    @Test fun missingAccountIsDifferentFromUnavailableProvider() {
        assertEquals(R.string.login_google_no_account, googleSignInErrorMessage(NoCredentialException()))
        assertEquals(R.string.login_google_unavailable, googleSignInErrorMessage(GetCredentialProviderConfigurationException()))
        assertEquals(R.string.login_google_unavailable, googleSignInErrorMessage(GetCredentialUnsupportedException()))
        assertEquals(R.string.login_google_interrupted, googleSignInErrorMessage(GetCredentialInterruptedException()))
    }
    @Test fun coroutineCancellationPropagatesAndDoesNotContactRepository() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        try {
            vm.signInWithGoogle { throw CancellationException("synthetic coroutine cancelled") }
            fail("La cancelación debe propagarse")
        } catch (_: CancellationException) { }
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessageRes)
        assertEquals(0, auth.calls)
    }
    @Test fun successfulSignInKeepsStableUidAndSessionVersion() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        runCurrent()
        vm.signInWithGoogle { Result.success("synthetic-token") }
        runCurrent()
        assertEquals("synthetic-user", vm.state.value.uid)
        assertEquals(1L, vm.state.value.sessionVersion)
        assertTrue(vm.state.value.isAuthenticated)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.errorMessageRes)
    }
    @Test fun secondTapDoesNotStartAnotherProviderRequest() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        val gate = CompletableDeferred<Result<String>>()
        var providerCalls = 0
        val first = launch { vm.signInWithGoogle { providerCalls++; gate.await() } }
        runCurrent()
        vm.signInWithGoogle { providerCalls++; Result.success("synthetic-token") }
        assertEquals(1, providerCalls)
        gate.complete(Result.success("synthetic-token"))
        first.join()
        assertEquals(1, auth.calls)
    }
    @Test fun retryAfterProviderErrorClearsPreviousMessage() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.signInWithGoogle { Result.failure(NoCredentialException()) }
        assertNotNull(vm.state.value.errorMessageRes)
        vm.signInWithGoogle { Result.success("synthetic-token") }
        assertNull(vm.state.value.errorMessageRes)
        assertTrue(vm.state.value.isAuthenticated)
        assertEquals(1, auth.calls)
    }
    @Test fun developerErrorStatusTenHasSpecificMessage() {
        assertEquals(R.string.login_google_installation_error, googleSignInErrorMessage(ApiException(Status(10))))
        assertEquals(R.string.login_google_generic_error, googleSignInErrorMessage(ApiException(Status(8))))
    }
    @Test fun firebaseErrorCodesHaveSafeActionableMessages() {
        val cases = mapOf(
            "ERROR_OPERATION_NOT_ALLOWED" to R.string.login_google_installation_error,
            "ERROR_APP_NOT_AUTHORIZED" to R.string.login_google_installation_error,
            "ERROR_INVALID_API_KEY" to R.string.login_google_installation_error,
            "ERROR_INVALID_OAUTH_CLIENT_ID" to R.string.login_google_installation_error,
            "ERROR_NETWORK_REQUEST_FAILED" to R.string.login_google_network_error,
            "ERROR_TOO_MANY_REQUESTS" to R.string.login_google_too_many_requests,
            "ERROR_USER_DISABLED" to R.string.login_google_disabled_account,
            "ERROR_INVALID_CREDENTIAL" to R.string.login_google_retry_account,
            "ERROR_INVALID_IDP_RESPONSE" to R.string.login_google_retry_account,
            "ERROR_USER_TOKEN_EXPIRED" to R.string.login_google_retry_account,
            "ERROR_INVALID_USER_TOKEN" to R.string.login_google_retry_account,
            "OTHER" to R.string.login_google_generic_error)
        cases.forEach { (code, expected) -> assertEquals(code, expected, firebaseSignInErrorMessage(code)) }
    }
    @Test fun wrappedProviderErrorsAreRecognizedWithoutExposingRawMessage() {
        assertNull(googleSignInErrorMessage(Exception("wrapper", GetCredentialCancellationException())))
        assertEquals(R.string.login_google_network_error, googleSignInErrorMessage(Exception("wrapper", UnknownHostException())))
    }
    @Test fun signOutClearsTheVisibleError() = runTest(dispatcher) {
        val auth = Auth(); val vm = AuthViewModel(auth)
        vm.signInWithGoogle { Result.failure(NoCredentialException()) }
        vm.signOut()
        assertNull(vm.state.value.errorMessageRes)
        assertFalse(vm.state.value.isAuthenticated)
        assertNull(vm.state.value.uid)
    }
}
