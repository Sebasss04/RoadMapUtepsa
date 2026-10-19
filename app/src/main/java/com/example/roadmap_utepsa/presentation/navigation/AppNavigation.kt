package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import com.example.roadmap_utepsa.presentation.shared.components.AccountPage
import com.example.roadmap_utepsa.presentation.auth.VerificationScreen
import com.example.roadmap_utepsa.presentation.session.LockScreen
import com.example.roadmap_utepsa.presentation.session.SessionLock
import com.example.roadmap_utepsa.presentation.notification.PendingReminders
import com.example.roadmap_utepsa.domain.notification.ReminderRoute

internal enum class AcademicDestination { CURRICULUM, SUMMARY, OFFER, RECOMMENDATIONS }

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    viewModelOwner: ViewModelStoreOwner,
    profileRepository: ProfileRepository,
    academicRepository: AcademicRepository,
    authRepository: AuthRepository,
    preparacionRepository: PreparacionRepository? = null,
    ofertaRepository: OfertaRepository? = null,
    recomendacionesRepository: RecomendacionesRepository? = null,
    sessionLock: SessionLock? = null,
    onBiometric: (Boolean) -> Unit = {},
    reminders: PendingReminders? = null,
    notificationRoute: ReminderRoute? = null,
    onNotificationHandled: () -> Unit = {},
    foreground: Boolean = true,
    resumeVersion: Long = 0,
    getGoogleToken: suspend () -> Result<String> = {
        Result.failure(com.example.roadmap_utepsa.domain.auth.AccessFailure(
            com.example.roadmap_utepsa.domain.auth.AccessFailureKind.UNAVAILABLE))
    }
) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    val state by authViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val google: suspend () -> Result<String> = {
        sessionLock?.beginExternal()
        try { getGoogleToken() } finally { sessionLock?.endExternal() }
    }
    val lockState = sessionLock?.state?.collectAsState()?.value
    var accessForm by remember { mutableStateOf<String?>(null) }

    val uid = state.uid
    LaunchedEffect(uid, notificationRoute) {
        if (uid != null && notificationRoute != null && notificationRoute.uid != uid) onNotificationHandled()
    }
    if (state.isAuthenticated && uid != null) {
        if (state.identity?.uid != uid) {
            AccountPage(texts.getString(R.string.navigation_app_navigation_1)) {
                androidx.compose.material3.CircularProgressIndicator()
                TextButton(authViewModel::signOut) { Text(texts.getString(R.string.navigation_app_navigation_2)) }
            }
            return
        }
        if (state.identity?.requiresVerification == true) {
            VerificationScreen(state, { scope.launch { authViewModel.verifyEmail() } },
                { scope.launch { authViewModel.checkVerification() } }, authViewModel::signOut)
            return
        }
        // A restored protected session loads no private repositories before its first unlock.
        var entered by remember(uid, state.sessionVersion) { mutableStateOf(false) }
        if (sessionLock == null || (lockState?.uid == uid && !lockState.loading && !lockState.locked)) entered = true
        if (!entered) {
            LockScreen(lockState ?: com.example.roadmap_utepsa.presentation.session.LockState(loading = true),
                { onBiometric(false) }, authViewModel::signOut)
            return
        }
        AcademicFlowNavigation(authViewModel, state, uid, viewModelOwner,
            profileRepository, academicRepository, authRepository, preparacionRepository,
            ofertaRepository, recomendacionesRepository, sessionLock, lockState, onBiometric,
            reminders, notificationRoute, onNotificationHandled, foreground, resumeVersion, google, scope)
    } else {
        EntryNavigation(authViewModel, state, accessForm, { accessForm = it }, google, scope)
    }
}
