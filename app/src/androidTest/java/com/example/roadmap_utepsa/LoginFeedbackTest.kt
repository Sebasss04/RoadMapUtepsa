package com.example.roadmap_utepsa

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.AuthRepository
import com.example.roadmap_utepsa.presentation.login.LoginScreen
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException

/** No abre el selector real ni utiliza Firebase: solo verifica feedback y reintento. */
class LoginFeedbackTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>(null)
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        var calls = 0
        override suspend fun signInWithGoogle(idToken: String) { calls++; error("No debe llegar a Firebase") }
        override fun signOut() { uidChanges.value = null }
    }
    private fun start(error: Exception): Auth {
        val auth = Auth()
        compose.setContent {
            val vm = remember { AuthViewModel(auth) }
            val state by vm.state.collectAsState()
            val scope = rememberCoroutineScope()
            RoadMap_UtepsaTheme {
                LoginScreen(state) { scope.launch { vm.signInWithGoogle { Result.failure(error) } } }
            }
        }
        return auth
    }
    private fun pressGoogle() { compose.onNodeWithText("Continuar con Google").performScrollTo().performClick() }
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test fun configurationFailureShowsActionableMessageAndCanRetry() {
        val auth = start(GetCredentialUnknownException("[28444] Developer console is not set up correctly."))
        pressGoogle()
        compose.onNodeWithText(text(R.string.login_google_installation_error)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("[28444] Developer console is not set up correctly.").assertDoesNotExist()
        compose.onNodeWithText("Continuar con Google").performScrollTo().assertIsEnabled()
        assertEquals(0, auth.calls)
    }
    @Test fun userCancellationDoesNotDisplayAnErrorCard() {
        val auth = start(GetCredentialCancellationException())
        pressGoogle()
        compose.onNodeWithText(text(R.string.login_error_title)).assertDoesNotExist()
        compose.onNodeWithText("Continuar con Google").assertIsEnabled()
        assertEquals(0, auth.calls)
    }
    @Test fun networkFailureIsNotReportedAsBadCredentials() {
        val auth = start(UnknownHostException("synthetic-host-not-for-display"))
        pressGoogle()
        compose.onNodeWithText(text(R.string.login_google_network_error)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("synthetic-host-not-for-display").assertDoesNotExist()
        assertEquals(0, auth.calls)
    }
}
