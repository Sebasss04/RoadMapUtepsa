package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesScreen
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesViewModel
import com.example.roadmap_utepsa.presentation.offer.OfertaScreen
import com.example.roadmap_utepsa.presentation.offer.OfertaViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.presentation.academic.ResumenScreen
import com.example.roadmap_utepsa.presentation.academic.CurriculumScreen
import com.example.roadmap_utepsa.presentation.academic.AcademicViewModel
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi
import com.example.roadmap_utepsa.presentation.profile.ProfileLoad
import com.example.roadmap_utepsa.presentation.profile.ProfileScreen
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.profile.ProfileViewModel
import kotlinx.coroutines.launch
import com.example.roadmap_utepsa.presentation.auth.EmailFormScreen
import com.example.roadmap_utepsa.presentation.account.AccountScreen
import com.example.roadmap_utepsa.presentation.session.LockScreen
import com.example.roadmap_utepsa.presentation.account.AccountViewModel
import com.example.roadmap_utepsa.presentation.session.SessionLock
import com.example.roadmap_utepsa.presentation.notification.PendingReminders
import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute

/** Session-scoped owners remain shared across all four tabs and Account. */
@Composable
internal fun AcademicFlowNavigation(
    authViewModel: AuthViewModel,
    state: com.example.roadmap_utepsa.presentation.auth.AuthUiState,
    uid: String,
    viewModelOwner: ViewModelStoreOwner,
    profileRepository: ProfileRepository,
    academicRepository: AcademicRepository,
    authRepository: AuthRepository,
    preparacionRepository: PreparacionRepository?,
    ofertaRepository: OfertaRepository?,
    recomendacionesRepository: RecomendacionesRepository?,
    sessionLock: SessionLock?,
    lockState: com.example.roadmap_utepsa.presentation.session.LockState?,
    onBiometric: (Boolean) -> Unit,
    reminders: PendingReminders?,
    notificationRoute: ReminderRoute?,
    onNotificationHandled: () -> Unit,
    foreground: Boolean,
    resumeVersion: Long,
    google: suspend () -> Result<String>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val texts = androidx.compose.ui.platform.LocalResources.current
    // Rotation retains the form; a new authentication session gets a fresh server read.
    val profileViewModel = remember(uid, state.sessionVersion, viewModelOwner, profileRepository) {
        ViewModelProvider(viewModelOwner, ProfileViewModel.Factory(uid, profileRepository))
            .get("profile:$uid:${state.sessionVersion}", ProfileViewModel::class.java)
    }
    val profileState by profileViewModel.state.collectAsState()
    var editingProfile by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
    var showingAccount by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
    var recovering by remember { mutableStateOf(false) }
    val accountVm = remember(uid, state.sessionVersion, viewModelOwner, authRepository) {
        ViewModelProvider(viewModelOwner, AccountViewModel.Factory(authRepository, uid, state.sessionVersion) {
            authViewModel.state.value.sessionVersion
        }).get("account:$uid:${state.sessionVersion}", AccountViewModel::class.java)
    }
    val accountState by accountVm.state.collectAsState()
    var offerOpened by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(false) }
    var destination by rememberSaveable(uid, state.sessionVersion) { mutableStateOf(AcademicDestination.CURRICULUM) }
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
        val reminderSettings = reminders?.state?.collectAsState()?.value
        val routeAllowed = foreground && lockState?.locked != true && lockState?.loading != true
        val openCurriculum = { destination = AcademicDestination.CURRICULUM }
        val openSummary = { academicViewModel.closeSubject(); destination = AcademicDestination.SUMMARY }
        val openOffer: (() -> Unit)? = if (ofertaRepository != null) ({
            academicViewModel.closeSubject(); offerOpened = true; destination = AcademicDestination.OFFER
        }) else null
        val openRecommendations: (() -> Unit)? = if (recommendationsVm != null) ({
            academicViewModel.closeSubject(); destination = AcademicDestination.RECOMMENDATIONS
        }) else null
        val curriculum: @Composable ((() -> Unit)?, EstadoPreparacionUi) -> Unit = { review, pending ->
                DisposableEffect(reminders) { onDispose { reminders?.pause() } }
                LaunchedEffect(destination, pending, showingAccount, routeAllowed, resumeVersion,
                    reminderSettings?.loading, reminderSettings?.enabled, reminderSettings?.ready) {
                    if (routeAllowed && !showingAccount && !editingProfile &&
                        pending != EstadoPreparacionUi.CARGANDO && pending != EstadoPreparacionUi.ERROR &&
                        pending != EstadoPreparacionUi.GUARDADO_PENDIENTE)
                        reminders?.evaluate(if (destination == AcademicDestination.OFFER) ReminderKind.OFFER else null)
                    else reminders?.pause()
                }
                AcademicNavigation(destination,
                    recommendationsAvailable = recommendationsVm != null,
                    offerAvailable = offerVm != null,
                    recommendations = { if (recommendationsVm != null) {
                    DisposableEffect(recommendationsVm) {
                        recommendationsVm.reload()
                        onDispose { recommendationsVm.leave() }
                    }
                    val recommendationsState by recommendationsVm.state.collectAsState()
                    RecomendacionesScreen(recommendationsState, recommendationsVm::reload,
                        onMalla = openCurriculum,
                        onOffer = { openOffer?.invoke() },
                        onHistory = { openCurriculum(); review?.invoke() },
                        onProfile = { showingAccount = true },
                        onSummary = openSummary)
                    } },
                    offer = { if (offerVm != null) {
                    LaunchedEffect(offerVm) { offerVm.reload() }
                    val offerState by offerVm.state.collectAsState()
                    OfertaScreen(offerState,
                        offerVm::reload, offerVm::edit, offerVm::query, offerVm::toggle,
                        offerVm::save, offerVm::cancel, offerVm::resolveConflict,
                        onMalla = openCurriculum, onProfile = { showingAccount = true },
                        onRecommendations = openRecommendations, onSummary = openSummary)
                    } },
                    summary = {
                    ResumenScreen(academicState, pending, academicViewModel::reload,
                        onProfile = { showingAccount = true }, onCurriculum = openCurriculum,
                        onHistory = review,
                        onOffer = openOffer, onRecommendations = openRecommendations)
                    },
                    curriculum = {
                        academicHolder.SaveableStateProvider(AcademicViewModel.key(uid, state.sessionVersion)) {
                    CurriculumScreen(academicState, academicViewModel::reload,
                    onProfile = { showingAccount = true },
                    onOpen = academicViewModel::openSubject, onSelect = academicViewModel::selectState,
                    onSave = academicViewModel::saveState, onClose = academicViewModel::closeSubject,
                    onDismissSaved = academicViewModel::dismissSaved,
                    onReviewConflict = academicViewModel::reviewConflict,
                    onReviewHistory = review?.let { callback -> { academicViewModel.closeSubject(); callback() } },
                    historyStatus = pending, onOffer = openOffer,
                    onRecommendations = openRecommendations, onSummary = openSummary)
            }
                    })
        }
        if (preparacionRepository == null) curriculum(null, EstadoPreparacionUi.CONFIRMADO)
        else academicHolder.SaveableStateProvider("history:$uid:${state.sessionVersion}") {
            HistorialGate(uid, state.sessionVersion, viewModelOwner, preparacionRepository, authRepository,
                onProfile = { showingAccount = true }, onSignIn = authViewModel::signOut,
                onEnterCurriculum = academicViewModel::refreshFromHistory,
                curriculum = { review, pending -> curriculum(review, pending) },
                notificationRoute = notificationRoute?.takeIf { it.uid == uid && routeAllowed },
                onNotificationDestination = { kind ->
                    showingAccount = false; recovering = false
                    if (kind == ReminderKind.OFFER) { offerOpened = true; destination = AcademicDestination.OFFER }
                    else destination = AcademicDestination.CURRICULUM
                    onNotificationHandled()
                })
        }
        if (showingAccount && state.identity != null) AccountNavigation({ showingAccount = false }) {
            if (recovering) EmailFormScreen(false, state, {
                recovering = false; authViewModel.clearFeedback()
            }) { email, _ -> scope.launch { authViewModel.recover(email) } }
            else AccountScreen(state.identity, profileState.profile, accountState, lockState,
                when (destination) {
                    AcademicDestination.CURRICULUM -> texts.getString(R.string.navigation_app_navigation_3)
                    AcademicDestination.SUMMARY -> texts.getString(R.string.navigation_app_navigation_4)
                    AcademicDestination.OFFER -> texts.getString(R.string.navigation_app_navigation_5)
                    AcademicDestination.RECOMMENDATIONS -> texts.getString(R.string.navigation_app_navigation_6)
                }, onBack = { showingAccount = false },
                onEdit = { profileViewModel.discardChanges(); editingProfile = true },
                onSignOut = { discardOfferOnSignOut = true },
                onRecover = { authViewModel.clearFeedback(); recovering = true },
                onLink = { password, newPassword -> accountVm.link(password, newPassword, google) },
                onBiometric = { enable -> if (enable) onBiometric(true) else sessionLock?.disable() },
                onRefresh = accountVm::refresh, reminders = reminders)
        }
    } else ProfileScreen(
        state = profileState,
        onNamesChange = profileViewModel::changeNames,
        onSurnamesChange = profileViewModel::changeSurnames,
        onSave = profileViewModel::save,
        onRetryLoad = profileViewModel::retryLoad,
        onDiscard = { profileViewModel.discardChanges(); if (complete) editingProfile = false },
        onSignOut = { discardOfferOnSignOut = true },
        returnLabel = texts.getString(R.string.navigation_app_navigation_7),
        onReturn = if (complete) ({
            editingProfile = false
        }) else null
    )
    if (discardOfferOnSignOut) AlertDialog(
        onDismissRequest = { discardOfferOnSignOut = false },
        title = { Text(stringResource(R.string.offer_sign_out)) },
        text = { Text(if (offerDraftPresent || profileState.hasChanges)
            texts.getString(R.string.navigation_app_navigation_8) else texts.getString(R.string.navigation_app_navigation_9)) },
        confirmButton = { TextButton({ discardOfferOnSignOut = false; authViewModel.signOut() }) {
            Text(stringResource(R.string.offer_confirm)) } },
        dismissButton = { TextButton({ discardOfferOnSignOut = false }) {
            Text(stringResource(R.string.offer_stay)) } })
    // Modal covering keeps navigation, active writes and in-memory drafts mounted.
    if (lockState?.locked == true || lockState?.loading == true) LockScreen(lockState,
        { onBiometric(false) }, authViewModel::signOut, offerDraftPresent || profileState.hasChanges)
}
