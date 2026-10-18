package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.data.OfertaRepository
import com.example.roadmap_utepsa.data.RecomendacionesRepository
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesScreen
import com.example.roadmap_utepsa.presentation.viewmodel.RecomendacionesViewModel
import com.example.roadmap_utepsa.presentation.offer.OfertaScreen
import com.example.roadmap_utepsa.presentation.viewmodel.OfertaViewModel
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
    preparacionRepository: PreparacionRepository? = null,
    ofertaRepository: OfertaRepository? = null,
    recomendacionesRepository: RecomendacionesRepository? = null
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
        var offerOpened by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
        var showingOffer by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
        var showingRecommendations by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
        var discardOfferOnSignOut by remember { mutableStateOf(false) }
        val academicHolder = rememberSaveableStateHolder()
        LaunchedEffect(profileState.saved) {
            if (profileState.saved) editingProfile = false
        }
        val complete = profileState.load == ProfileLoad.READY &&
            profileState.profile?.registration == ProfileRegistration.COMPLETE
        val offerVm = if (offerOpened && ofertaRepository != null) remember(uid, state.sessionVersion, viewModelOwner, ofertaRepository) {
            ViewModelProvider(viewModelOwner, OfertaViewModel.Factory(
                uid, state.sessionVersion, ofertaRepository, authRepository, { authViewModel.state.value.sessionVersion }))
                .get(OfertaViewModel.key(uid, state.sessionVersion),
                    OfertaViewModel::class.java)
        } else null
        val offerDraftPresent = offerVm?.state?.collectAsState()?.value?.dirty == true
        val recommendationsVm = if (recomendacionesRepository != null) remember(uid, state.sessionVersion, viewModelOwner, recomendacionesRepository) {
            ViewModelProvider(viewModelOwner, RecomendacionesViewModel.Factory(uid, state.sessionVersion,
                recomendacionesRepository, authRepository) { authViewModel.state.value.sessionVersion })
                .get(RecomendacionesViewModel.key(uid, state.sessionVersion), RecomendacionesViewModel::class.java)
        } else null
        if (complete && !editingProfile) {
            val academicViewModel = remember(uid, state.sessionVersion, viewModelOwner, academicRepository, authRepository) {
                ViewModelProvider(viewModelOwner, AcademicViewModel.Factory(uid, academicRepository, authRepository))
                    .get(AcademicViewModel.key(uid, state.sessionVersion), AcademicViewModel::class.java)
            }
            val academicState by academicViewModel.state.collectAsState()
            val curriculum: @Composable ((() -> Unit)?, EstadoPreparacionUi) -> Unit = { review, pending ->
                academicHolder.SaveableStateProvider(AcademicViewModel.key(uid, state.sessionVersion)) {
                    if (showingRecommendations && recommendationsVm != null) {
                        DisposableEffect(recommendationsVm) {
                            recommendationsVm.reload()
                            onDispose { recommendationsVm.leave() }
                        }
                        val recommendationsState by recommendationsVm.state.collectAsState()
                        RecomendacionesScreen(recommendationsState, recommendationsVm::reload,
                            onMalla = { showingRecommendations = false; showingOffer = false },
                            onOffer = { showingRecommendations = false; offerOpened = true; showingOffer = true },
                            onHistory = { showingRecommendations = false; showingOffer = false; review?.invoke() },
                            onProfile = { editingProfile = true }, onSignOut = authViewModel::signOut,
                            signOutWarning = if (offerDraftPresent) R.string.offer_discard_detail else R.string.academic_sign_out_detail)
                    } else if (showingOffer && offerVm != null) {
                        LaunchedEffect(offerVm) { offerVm.reload() }
                        val offerState by offerVm.state.collectAsState()
                        OfertaScreen(offerState,
                            offerVm::reload, offerVm::edit, offerVm::query, offerVm::toggle,
                            offerVm::save, offerVm::cancel, offerVm::resolveConflict,
                            onMalla = { showingOffer = false }, onProfile = { editingProfile = true },
                            onSignOut = authViewModel::signOut,
                            onRecommendations = if (recommendationsVm != null) ({ showingRecommendations = true }) else null)
                    } else CurriculumScreen(academicState, academicViewModel::reload,
                        onProfile = { editingProfile = true }, onSignOut = authViewModel::signOut,
                        onOpen = academicViewModel::openSubject, onSelect = academicViewModel::selectState,
                        onSave = academicViewModel::saveState, onClose = academicViewModel::closeSubject,
                        onDismissSaved = academicViewModel::dismissSaved,
                        onReviewConflict = academicViewModel::reviewConflict,
                        onReviewHistory = review?.let { callback -> { academicViewModel.closeSubject(); callback() } },
                        historyStatus = pending, onOffer = if (ofertaRepository != null) ({ offerOpened = true; showingOffer = true }) else null,
                        signOutWarning = if (offerDraftPresent) R.string.offer_discard_detail else R.string.academic_sign_out_detail,
                        onRecommendations = if (recommendationsVm != null) ({ showingRecommendations = true }) else null)
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
            onSignOut = { if (offerDraftPresent) discardOfferOnSignOut = true else authViewModel.signOut() },
            onReturn = if (complete) ({ editingProfile = false; showingOffer = false }) else null
        )
        if (discardOfferOnSignOut) AlertDialog(
            onDismissRequest = { discardOfferOnSignOut = false },
            title = { Text(stringResource(R.string.offer_sign_out)) },
            text = { Text(stringResource(R.string.offer_discard_detail)) },
            confirmButton = { TextButton({ discardOfferOnSignOut = false; authViewModel.signOut() }) {
                Text(stringResource(R.string.offer_confirm)) } },
            dismissButton = { TextButton({ discardOfferOnSignOut = false }) {
                Text(stringResource(R.string.offer_stay)) } })
    } else {
        LoginScreen(state = state, onSignIn = {
            scope.launch { authViewModel.signInWithGoogle(helper::getGoogleIdToken) }
        })
    }
}
