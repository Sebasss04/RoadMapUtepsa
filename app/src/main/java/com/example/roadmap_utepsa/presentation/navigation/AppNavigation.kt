package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.data.ProfileRepository
import com.example.roadmap_utepsa.data.AuthRepository
import com.example.roadmap_utepsa.data.AcademicRepository
import com.example.roadmap_utepsa.data.PreparacionRepository
import com.example.roadmap_utepsa.domain.model.ProfileRegistration
import com.example.roadmap_utepsa.presentation.academic.CurriculumScreen
import com.example.roadmap_utepsa.presentation.viewmodel.AcademicViewModel
import com.example.roadmap_utepsa.presentation.viewmodel.EstadoPreparacionUi
import com.example.roadmap_utepsa.presentation.viewmodel.ProfileLoad
import com.example.roadmap_utepsa.presentation.login.LoginScreen
import com.example.roadmap_utepsa.presentation.profile.ProfileScreen
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import com.example.roadmap_utepsa.presentation.viewmodel.ProfileViewModel
import com.example.roadmap_utepsa.services.GoogleAuthHelper
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    viewModelOwner: ViewModelStoreOwner,
    profileRepository: ProfileRepository,
    academicRepository: AcademicRepository,
    authRepository: AuthRepository,
    preparacionRepository: PreparacionRepository? = null
) {
    val state by authViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val helper = remember(context) { GoogleAuthHelper(context) }

    val uid = state.uid
    if (state.isAuthenticated && uid != null) {
        // Rotation retains the form; a new authentication session gets a fresh server read.
        val profileViewModel = remember(uid, state.sessionVersion, viewModelOwner, profileRepository) {
            ViewModelProvider(viewModelOwner, ProfileViewModel.Factory(uid, profileRepository))
                .get("profile:$uid:${state.sessionVersion}", ProfileViewModel::class.java)
        }
        val profileState by profileViewModel.state.collectAsState()
        var editingProfile by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
        val academicHolder = rememberSaveableStateHolder()
        LaunchedEffect(profileState.saved) {
            if (profileState.saved) editingProfile = false
        }
        val complete = profileState.load == ProfileLoad.READY &&
            profileState.profile?.registration == ProfileRegistration.COMPLETE
        if (complete && !editingProfile) {
            val academicViewModel = remember(uid, state.sessionVersion, viewModelOwner, academicRepository, authRepository) {
                ViewModelProvider(viewModelOwner, AcademicViewModel.Factory(uid, academicRepository, authRepository))
                    .get(AcademicViewModel.key(uid, state.sessionVersion), AcademicViewModel::class.java)
            }
            val academicState by academicViewModel.state.collectAsState()
            val curriculum: @Composable ((() -> Unit)?, EstadoPreparacionUi) -> Unit = { review, pending ->
                academicHolder.SaveableStateProvider(AcademicViewModel.key(uid, state.sessionVersion)) {
                    CurriculumScreen(academicState, academicViewModel::reload,
                        onProfile = { editingProfile = true }, onSignOut = authViewModel::signOut,
                        onOpen = academicViewModel::openSubject, onSelect = academicViewModel::selectState,
                        onSave = academicViewModel::saveState, onClose = academicViewModel::closeSubject,
                        onDismissSaved = academicViewModel::dismissSaved,
                        onReviewConflict = academicViewModel::reviewConflict,
                        onReviewHistory = review?.let { callback -> { academicViewModel.closeSubject(); callback() } },
                        historyStatus = pending)
                }
            }
            if (preparacionRepository == null) curriculum(null, EstadoPreparacionUi.CONFIRMADO)
            else academicHolder.SaveableStateProvider("history:$uid:${state.sessionVersion}") {
                HistorialGate(uid, state.sessionVersion, viewModelOwner, preparacionRepository, authRepository,
                    onProfile = { editingProfile = true }, onSignIn = authViewModel::signOut,
                    onEnterCurriculum = academicViewModel::refreshFromHistory,
                    curriculum = { review, pending -> curriculum(review, pending) })
            }
        } else ProfileScreen(
            state = profileState,
            onNamesChange = profileViewModel::changeNames,
            onSurnamesChange = profileViewModel::changeSurnames,
            onSave = profileViewModel::save,
            onRetryLoad = profileViewModel::retryLoad,
            onDiscard = profileViewModel::discardChanges,
            onSignOut = authViewModel::signOut,
            onReturn = if (complete) ({ editingProfile = false }) else null
        )
    } else {
        LoginScreen(state = state, onSignIn = {
            scope.launch { authViewModel.signInWithGoogle(helper::getGoogleIdToken) }
        })
    }
}
